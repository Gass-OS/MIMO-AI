package ai.mimo.chat

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads the official MIMO model artifacts into app-specific storage.
 * DownloadManager owns transfer/retry/resume; a model is not marked verified
 * until its complete file passes the pinned SHA-256 check.
 */
class ModelInstaller(private val context: Context) {
    enum class Model(val displayName: String, val fileName: String, val url: String, val sha256: String) {
        BRAIN(
            "MIMO Brain",
            "Qwen3-4B-Q4_K_M.gguf",
            "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf",
            "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5"
        ),
        STUDIO(
            "MIMO Studio",
            "sd_xl_base_1.0.safetensors",
            "https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0/resolve/main/sd_xl_base_1.0.safetensors",
            "31e35c80fc4829d14f90153f4c74cd59c90b779f6afe05a74cd6120b893f7e5b"
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

    private val manager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("mimo_model_installer", Context.MODE_PRIVATE)

    fun availableBytes(): Long {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return StatFs(root.absolutePath).availableBytes
    }

    fun hasRequiredSpace(minimumBytes: Long = 15_000_000_000L): Boolean =
        availableBytes() >= minimumBytes

    fun existingVerifiedFile(model: Model): File? {
        val file = modelFile(model)
        return file.takeIf { it.isFile && it.length() > 0L &&
            prefs.getBoolean("verified_${model.name}", false) }
    }

    fun enqueue(model: Model, wifiOnly: Boolean = true): Long {
        val existing = existingVerifiedFile(model)
        if (existing != null) return COMPLETED_ALREADY_INSTALLED

        val previousId = prefs.getLong("download_${model.name}", INVALID_ID)
        if (previousId != INVALID_ID) {
            val current = query(previousId)
            if (current != null && (current.status == DownloadManager.STATUS_PENDING ||
                    current.status == DownloadManager.STATUS_RUNNING ||
                    current.status == DownloadManager.STATUS_PAUSED)) return previousId
        }

        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val modelDirectory = File(root, "mimo-models")
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IllegalStateException("Unable to create the model download directory")
        }

        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle(model.displayName)
            .setDescription("Downloading and verifying MIMO components")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "mimo-models/${model.fileName}"
            )

        val networkTypes = if (wifiOnly) DownloadManager.Request.NETWORK_WIFI
            else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
        request.setAllowedNetworkTypes(networkTypes)

        val id = manager.enqueue(request)
        prefs.edit()
            .putLong("download_${model.name}", id)
            .putBoolean("verified_${model.name}", false)
            .apply()
        return id
    }

    fun describeReason(transfer: Transfer): String {
        if (transfer.reason == 0) return ""
        val label = when (transfer.reason) {
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for an allowed network"
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            DownloadManager.PAUSED_UNKNOWN -> "Paused by Android"
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume this transfer"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Download storage is unavailable"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "A file with this name already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage file error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many server redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server rejected the download (HTTP error)"
            DownloadManager.ERROR_UNKNOWN -> "Unknown Android download error"
            else -> "Android download reason ${transfer.reason}"
        }
        return label
    }

    fun query(id: Long): Transfer? {
        if (id == COMPLETED_ALREADY_INSTALLED || id == INVALID_ID) return null
        var cursor: Cursor? = null
        return try {
            cursor = manager.query(DownloadManager.Query().setFilterById(id))
            if (cursor == null || !cursor.moveToFirst()) return null
            Transfer(
                id = id,
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            )
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Resumes an existing DownloadManager transfer when possible, waits for the
     * real transfer state, and verifies the final file before returning true.
     * Cancelling this coroutine does not cancel the persisted DownloadManager job.
     */
    suspend fun downloadAndVerify(
        model: Model,
        wifiOnly: Boolean = true,
        onProgress: (Transfer?) -> Unit = {}
    ): Boolean {
        if (isVerified(model)) {
            onProgress(null)
            return true
        }

        var id = savedDownloadId(model)
        var previous = if (id != INVALID_ID) query(id) else null
        if (previous?.status == DownloadManager.STATUS_SUCCESSFUL) {
            if (verify(model, id)) {
                onProgress(previous)
                return true
            }
            id = INVALID_ID
        }
        if (previous == null || (previous.status != DownloadManager.STATUS_PENDING &&
                    previous.status != DownloadManager.STATUS_RUNNING &&
                    previous.status != DownloadManager.STATUS_PAUSED)) {
            id = enqueue(model, wifiOnly)
        }

        if (id == COMPLETED_ALREADY_INSTALLED) return isVerified(model)
        while (true) {
            val transfer = query(id) ?: return false
            onProgress(transfer)
            when (transfer.status) {
                DownloadManager.STATUS_SUCCESSFUL -> return verify(model, id)
                DownloadManager.STATUS_FAILED -> return false
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED -> kotlinx.coroutines.delay(1000)
            }
        }
    }

    suspend fun verify(model: Model, id: Long): Boolean = withContext(Dispatchers.IO) {
        val transfer = query(id)
        if (transfer?.status != DownloadManager.STATUS_SUCCESSFUL) return@withContext false
        // The request writes directly to this app-specific destination. Do not
        // copy from getUriForDownloadedFile(): that URI points to the same file.
        if (manager.getUriForDownloadedFile(id) == null) return@withContext false
        val file = modelFile(model)
        if (!file.isFile || file.length() <= 0L) return@withContext false
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
        val valid = actual.equals(model.sha256, ignoreCase = true)
        if (valid) {
            prefs.edit().putBoolean("verified_${model.name}", true).apply()
        } else {
            file.delete()
            prefs.edit().putBoolean("verified_${model.name}", false).apply()
            manager.remove(id)
        }
        valid
    }

    fun isVerified(model: Model): Boolean = existingVerifiedFile(model) != null

    fun savedDownloadId(model: Model): Long =
        prefs.getLong("download_${model.name}", INVALID_ID)

    fun modelFile(model: Model): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(root, "mimo-models/${model.fileName}")
    }

    companion object {
        const val INVALID_ID = -1L
        const val COMPLETED_ALREADY_INSTALLED = -2L
    }
}
