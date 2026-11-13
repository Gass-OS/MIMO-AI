package ai.mimo.chat

import android.content.Context
import android.app.ActivityManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import android.app.DownloadManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Downloads model weights directly from inside MIMO into private internal app storage.
 * No Android Downloads folder and no system DownloadManager notification are used.
 * Partial files are retained so a later retry can resume with HTTP Range requests.
 */
class ModelInstaller(private val context: Context) {
    enum class Model(val displayName: String, val fileName: String, val url: String, val sha256: String, val requiredBytes: Long) {
        BRAIN(
            "MIMO Brain",
            "Qwen3-4B-Q4_K_M.gguf",
            "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/bc640142c66e1fdd12af0bd68f40445458f3869b/Qwen3-4B-Q4_K_M.gguf",
            "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5",
            3_000_000_000L
        ),
        STUDIO(
            "MIMO Studio",
            "sd_xl_base_1.0.safetensors",
            "https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0/resolve/fa787a693dcc986199df6f759d0039f36d777169/sd_xl_base_1.0.safetensors",
            "31e35c80fc4829d14f90153f4c74cd59c90b779f6afe05a74cd6120b893f7e5b",
            7_500_000_000L
        )
    }

    data class Transfer(
        val id: Long,
        val status: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val reason: Int
    ) {
        val progress: Float
            get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    private val prefs = context.getSharedPreferences("mimo_model_installer", Context.MODE_PRIVATE)
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val transferLock = Any()
    private val transfers = mutableMapOf<Long, Transfer>()
    private val errors = mutableMapOf<Long, String>()

    private fun transferId(model: Model) = model.ordinal.toLong() + 1L
    private fun partFile(model: Model) = File(modelFile(model).parentFile, model.fileName + ".part")

    fun availableBytes(): Long = StatFs(context.filesDir.absolutePath).availableBytes

    fun recommendedPerformanceProfile(): String {
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val ramGb = memory.totalMem / 1_073_741_824.0
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            ramGb >= 8.0 -> "high"
            ramGb < 2.0 || cores <= 2 -> "low"
            else -> "medium"
        }
    }

    fun deviceResources(): String {
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val ramGb = memory.totalMem / 1_073_741_824.0
        val freeGb = availableBytes() / 1_000_000_000.0
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val tier = when {
            ramGb >= 8.0 -> "Fort · 8 GB+"
            ramGb < 2.0 || cores <= 2 -> "Low · 2 GB"
            else -> "Medium · 4 GB"
        }
        return String.format(Locale.US, "%s device | %.1f GB RAM | %d CPU cores | %s | %.2f GB free app storage", tier, ramGb, cores, abi, freeGb)
    }

    private fun networkStatus(wifiOnly: Boolean): Pair<Boolean, String> {
        val networks = connectivity.allNetworks.toList()
        val activeCaps = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        val caps = networks.mapNotNull { connectivity.getNetworkCapabilities(it) } + listOfNotNull(activeCaps)
        val hasInternet = caps.any { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
        val hasWifi = caps.any { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
        val hasCellular = caps.any { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) }
        // Respect the explicit Wi-Fi-only preference when Android positively identifies
        // a cellular route. Do not reject a connection merely because Android's
        // capability snapshot is incomplete (VPNs, captive portals and OEM network
        // reporting can temporarily hide INTERNET/VALIDATED). Let the HTTPS request
        // be the final connectivity test so MIMO never waits forever at 0%.
        if (wifiOnly && !hasWifi && hasCellular) {
            return false to "Wi-Fi only is ON | mobile data detected but blocked. Turn Wi-Fi only OFF to use mobile data."
        }
        val transport = when {
            hasWifi -> "Wi-Fi"
            hasCellular -> "Mobile data"
            else -> "Network"
        }
        val validated = caps.any { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
        return true to when {
            !hasInternet -> "Android network status is incomplete | testing the secure model connection directly..."
            !validated -> "$transport detected | testing the secure model connection directly..."
            else -> "$transport connected | contacting the model server..."
        }
    }

    // The first model needs about 2.5 GB plus temporary/metadata headroom.
    // The larger Studio model is checked separately immediately before it starts.
    fun hasRequiredSpace(minimumBytes: Long = 3_000_000_000L): Boolean =
        availableBytes() >= minimumBytes

    fun existingVerifiedFile(model: Model): File? {
        val file = modelFile(model)
        return file.takeIf { it.isFile && file.length() > 0L &&
            prefs.getBoolean("verified_${model.name}", false) }
    }

    fun enqueue(model: Model, wifiOnly: Boolean = true): Long = transferId(model)

    fun describeReason(transfer: Transfer): String =
        errors[transfer.id] ?: when (transfer.reason) {
            1 -> "Waiting for Wi-Fi"
            2 -> "No network connection"
            3 -> "Server returned an HTTP error"
            4 -> "Not enough private app storage"
            else -> if (transfer.reason == 0) "" else "Download error (${transfer.reason})"
        }

    fun query(id: Long): Transfer? = synchronized(transferLock) { transfers[id] }

    suspend fun downloadAndVerify(
        model: Model,
        wifiOnly: Boolean = true,
        onProgress: (Transfer?) -> Unit = {}
    ): Boolean {
        if (isVerified(model)) {
            onProgress(null)
            return true
        }

        val id = transferId(model)
        val directory = modelFile(model).parentFile!!
        if (!directory.exists() && !directory.mkdirs()) {
            publish(Transfer(id, DownloadManager.STATUS_FAILED, 0L, -1L, 4), "Cannot create MIMO's private model folder")
            onProgress(query(id))
            return false
        }
        if (availableBytes() < model.requiredBytes) {
            publish(Transfer(id, DownloadManager.STATUS_FAILED, 0L, -1L, 4), "MIMO needs ${model.requiredBytes / 1_000_000_000L} GB of free internal app storage for ${model.displayName}")
            onProgress(query(id))
            return false
        }

        val destination = modelFile(model)
        val partial = partFile(model)
        prefs.edit().putLong("download_${model.name}", id).putBoolean("verified_${model.name}", false).apply()

        return try {
            withContext(Dispatchers.IO) {
                var lastReported = 0L
                while (true) {
                    val (connected, networkMessage) = networkStatus(wifiOnly)
                    if (!connected) {
                        val reason = if (networkMessage.contains("Wi-Fi only")) 1 else 2
                        val waiting = Transfer(id, DownloadManager.STATUS_PAUSED, partial.length(), -1L, reason)
                        publish(waiting, networkMessage)
                        withContext(Dispatchers.Main) { onProgress(waiting) }
                        delay(2500)
                        continue
                    }
                    val connecting = Transfer(id, DownloadManager.STATUS_RUNNING, partial.length(), -1L, 0)
                    publish(connecting, networkMessage)
                    withContext(Dispatchers.Main) { onProgress(connecting) }

                    val offset = partial.takeIf { it.exists() }?.length() ?: 0L
                    val connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 45_000
                        readTimeout = 120_000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "MIMO-AI-Android/0.1")
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("Accept", "application/octet-stream")
                        if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
                    }

                    try {
                        val response = connection.responseCode
                        if (response == 416 && offset > 0L) {
                            partial.delete()
                            continue
                        }
                        if (response !in 200..299) {
                            val message = "Model server returned HTTP $response"
                            publish(Transfer(id, DownloadManager.STATUS_FAILED, offset, -1L, 3), message)
                            withContext(Dispatchers.Main) { onProgress(query(id)) }
                            return@withContext false
                        }

                        val append = offset > 0L && response == HttpURLConnection.HTTP_PARTIAL
                        val startAt = if (append) offset else 0L
                        val responseLength = connection.getHeaderFieldLong("Content-Length", -1L)
                        val rangeTotal = connection.getHeaderField("Content-Range")
                            ?.substringAfter("/")?.toLongOrNull()
                        val total = rangeTotal ?: if (responseLength > 0L) startAt + responseLength else -1L
                        if (!append && partial.exists()) partial.delete()

                        var downloaded = startAt
                        publish(Transfer(id, DownloadManager.STATUS_RUNNING, downloaded, total, 0))
                        withContext(Dispatchers.Main) { onProgress(query(id)) }
                        connection.inputStream.use { input ->
                            FileOutputStream(partial, append).use { output ->
                                val buffer = ByteArray(256 * 1024)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                    downloaded += count
                                    if (downloaded != lastReported) {
                                        lastReported = downloaded
                                        val progress = Transfer(id, DownloadManager.STATUS_RUNNING, downloaded, total, 0)
                                        publish(progress)
                                        withContext(Dispatchers.Main) { onProgress(progress) }
                                    }
                                }
                                output.fd.sync()
                            }
                        }
                        if (total > 0L && partial.length() != total) {
                            throw java.io.IOException("Incomplete response: received ${partial.length()} of $total bytes")
                        }
                    } finally {
                        connection.disconnect()
                    }

                    val finalTransfer = Transfer(id, DownloadManager.STATUS_RUNNING, partial.length(), partial.length(), 0)
                    publish(finalTransfer)
                    val valid = verifyFile(model, partial)
                    if (!valid) {
                        partial.delete()
                        publish(Transfer(id, DownloadManager.STATUS_FAILED, 0L, -1L, 3), "SHA-256 verification failed; the partial model was discarded. Retry the download.")
                        withContext(Dispatchers.Main) { onProgress(query(id)) }
                        return@withContext false
                    }
                    if (!partial.renameTo(destination)) {
                        destination.outputStream().use { output -> partial.inputStream().use { it.copyTo(output) } }
                        partial.delete()
                    }
                    prefs.edit().putBoolean("verified_${model.name}", true).apply()
                    val done = Transfer(id, DownloadManager.STATUS_SUCCESSFUL, destination.length(), destination.length(), 0)
                    publish(done)
                    withContext(Dispatchers.Main) { onProgress(done) }
                    return@withContext true
                }
                @Suppress("UNREACHABLE_CODE")
                false
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            val message = error.message ?: error.javaClass.simpleName
            val failed = Transfer(id, DownloadManager.STATUS_FAILED, partial.length(), -1L, 3)
            publish(failed, message)
            withContext(Dispatchers.Main) { onProgress(failed) }
            false
        }
    }

    suspend fun verify(model: Model, id: Long): Boolean = withContext(Dispatchers.IO) {
        val file = modelFile(model)
        val valid = file.isFile && verifyFile(model, file)
        if (valid) prefs.edit().putBoolean("verified_${model.name}", true).apply()
        valid
    }

    private fun verifyFile(model: Model, file: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(model.sha256, ignoreCase = true)
    }

    private fun publish(transfer: Transfer, error: String? = null) {
        synchronized(transferLock) {
            transfers[transfer.id] = transfer
            if (error != null) errors[transfer.id] = error else if (transfer.status == DownloadManager.STATUS_RUNNING) errors.remove(transfer.id)
        }
    }

    fun isVerified(model: Model): Boolean = existingVerifiedFile(model) != null

    fun resetTransferForNetworkChange(model: Model) {
        if (isVerified(model)) return
        val id = transferId(model)
        // Preserve partial bytes when the user changes network policy.
        prefs.edit().remove("download_${model.name}").putBoolean("verified_${model.name}", false).apply()
        synchronized(transferLock) { transfers.remove(id); errors.remove(id) }
    }

    fun savedDownloadId(model: Model): Long =
        prefs.getLong("download_${model.name}", transferId(model))

    fun modelFile(model: Model): File =
        File(File(context.filesDir, "mimo-models"), model.fileName)
}
