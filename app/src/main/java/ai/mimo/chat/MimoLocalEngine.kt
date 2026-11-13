package ai.mimo.chat

import android.content.Context
import com.tensai.llamakt.LlamaEngine
import com.tensai.llamakt.SamplingParams
import com.tensai.llamakt.chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Owns the native llama.cpp session for MIMO Brain.
 *
 * The GGUF remains in ModelInstaller's existing filesDir/mimo-models folder.
 * Updating the APK does not copy, replace, or download model weights.
 */
class MimoLocalEngine(context: Context) {
    private val installer = ModelInstaller(context.applicationContext)

    @Volatile
    private var engine: LlamaEngine? = null

    suspend fun load() = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext

        val model = ModelInstaller.Model.BRAIN
        // If an earlier install left a complete model but its verification flag was
        // not persisted, verify the existing bytes in place. Never download here.
        if (!installer.isVerified(model)) {
            val valid = installer.verify(model, 1L)
            if (!valid) {
                throw IllegalStateException(
                    "The existing MIMO Brain file is missing or failed SHA-256 verification. No model download was started."
                )
            }
        }

        val modelFile = installer.modelFile(model)
        if (!modelFile.isFile || modelFile.length() <= 0L) {
            throw IllegalStateException("MIMO Brain was not found in its existing private storage.")
        }

        val runtime = LlamaEngine()
        try {
            // CPU-first is the safe baseline. llama.cpp detects the fast ARM cores
            // automatically; GPU offload can be enabled after device validation.
            runtime.load(
                path = modelFile.absolutePath,
                nGpuLayers = 0,
                nCtx = 4096,
                nThreads = 0
            )
            engine = runtime
        } catch (error: Exception) {
            runCatching { runtime.free() }
            throw error
        }
    }

    suspend fun generate(
        history: List<ChatMessage>,
        onToken: (String) -> Unit
    ) {
        val runtime = engine ?: throw IllegalStateException("MIMO Brain is still loading.")
        val promptMessages = buildList {
            add(
                com.tensai.llamakt.ChatMessage(
                    "system",
                    "You are MIMO AI, a helpful private on-device assistant. Reply naturally in the language used by the user. You understand English, French, Arabic, and Tunisian Arabic (Derja). Be clear, accurate, friendly, and concise. Do not claim to use the internet or external services."
                )
            )
            history.forEach { message ->
                if (message.text.isNotBlank()) {
                    add(
                        com.tensai.llamakt.ChatMessage(
                            if (message.fromUser) "user" else "assistant",
                            message.text
                        )
                    )
                }
            }
        }

        withContext(Dispatchers.Default) {
            runtime.chat(
                messages = promptMessages,
                params = SamplingParams(
                    nPredict = 384,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.9f,
                    repeatPenalty = 1.08f
                ),
                enableThinking = false
            ).collect { token ->
                withContext(Dispatchers.Main.immediate) { onToken(token) }
            }
        }
    }

    fun close() {
        val active = engine
        engine = null
        runCatching { active?.free() }
    }
}
