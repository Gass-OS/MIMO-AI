package ai.mimo.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * User-started foreground service keeps the approved model transfer alive when
 * MIMO's screen changes or the activity is backgrounded. Files remain in filesDir.
 */
class MimoDownloadService : Service() {
    companion object {
        const val ACTION_START = "ai.mimo.chat.action.START_MODEL_DOWNLOAD"
        const val EXTRA_WIFI_ONLY = "wifi_only"
        const val STATE_PREFS = "mimo_download_state"
        private const val CHANNEL_ID = "mimo_model_downloads"
        private const val NOTIFICATION_ID = 4107
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    @Volatile private var latestRequest: Int = 0
    private lateinit var state: android.content.SharedPreferences
    private lateinit var installer: ModelInstaller

    override fun onCreate() {
        super.onCreate()
        state = getSharedPreferences(STATE_PREFS, MODE_PRIVATE)
        installer = ModelInstaller(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Preparing MIMO Brain", 0, false))
        if (intent?.action == ACTION_START || intent == null) {
            val request = ++latestRequest
            val wifiOnly = intent?.getBooleanExtra(EXTRA_WIFI_ONLY, true) ?: true
            scope.launch {
                downloadJob?.cancelAndJoin()
                if (request != latestRequest) return@launch
                val task = launch { runBrainDownload(wifiOnly) }
                downloadJob = task
                task.join()
                if (request == latestRequest) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun runBrainDownload(wifiOnly: Boolean) {
        state.edit().putString("status", "Downloading MIMO Brain in the background…").apply()
        updateNotification("Downloading MIMO Brain", 0, false)
        try {
            if (!installer.hasRequiredSpace()) {
                val message = "At least 3 GB of free internal app storage is required before starting MIMO Brain."
                state.edit().putString("status", message).apply()
                updateNotification(message, 0, false)
                return
            }
            val success = installer.downloadAndVerify(ModelInstaller.Model.BRAIN, wifiOnly) { transfer ->
                if (transfer != null) {
                    val percent = (transfer.progress * 100f).toInt().coerceIn(0, 100)
                    val status = when {
                        transfer.status == android.app.DownloadManager.STATUS_PAUSED ->
                            "MIMO Brain paused: " + installer.describeReason(transfer)
                        transfer.status == android.app.DownloadManager.STATUS_FAILED ->
                            "MIMO Brain download failed: " + installer.describeReason(transfer)
                        transfer.totalBytes <= 0L -> "MIMO Brain connected · waiting for download data…"
                        else -> "MIMO Brain · $percent% · " +
                            formatBytes(transfer.downloadedBytes) + " / " + formatBytes(transfer.totalBytes)
                    }
                    state.edit()
                        .putLong("brain_downloaded", transfer.downloadedBytes)
                        .putLong("brain_total", transfer.totalBytes)
                        .putString("status", status)
                        .apply()
                    updateNotification(status, percent, transfer.totalBytes > 0L)
                }
            }
            if (success) {
                val message = "MIMO Brain downloaded and SHA-256 verified in private app storage."
                state.edit().putLong("brain_downloaded", installer.modelFile(ModelInstaller.Model.BRAIN).length())
                    .putLong("brain_total", installer.modelFile(ModelInstaller.Model.BRAIN).length())
                    .putString("status", message).putBoolean("brain_complete", true).apply()
                updateNotification("MIMO Brain verified · setup complete", 100, true)
            } else {
                val message = installer.query(installer.savedDownloadId(ModelInstaller.Model.BRAIN))
                    ?.let { "MIMO Brain download failed: " + installer.describeReason(it) }
                    ?: "MIMO Brain download failed. Check your connection and retry."
                state.edit().putString("status", message).apply()
                updateNotification(message, 0, false)
            }
        } catch (cancelled: CancellationException) {
            state.edit().putString("status", "MIMO Brain download paused. Reopen MIMO to resume.").apply()
            throw cancelled
        } catch (error: Exception) {
            val message = "Download service error: " + (error.message ?: error.javaClass.simpleName)
            state.edit().putString("status", message).apply()
            updateNotification(message, 0, false)
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "Unknown size"
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1.0) String.format(java.util.Locale.US, "%.2f GB", gb)
        else String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "MIMO model downloads", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Progress and completion for MIMO's approved model downloads" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String, progress: Int, determinate: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("MIMO AI · Model setup")
            .setContentText(message)
            .setOngoing(progress < 100 && !message.contains("failed", true))
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (determinate) builder.setProgress(100, progress, false)
        else builder.setProgress(0, 0, true)
        return builder.build()
    }

    private fun updateNotification(message: String, progress: Int, determinate: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message, progress, determinate))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
