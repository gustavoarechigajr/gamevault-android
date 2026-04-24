package com.gamevault.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamevault.android.R
import com.gamevault.android.data.DownloadEntry
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.QueuedDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "gamevault_downloads"
        const val NOTIF_ID   = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitoring = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Starting…"))

        if (!monitoring) {
            monitoring = true
            scope.launch {
                DownloadRepository.downloads
                    .combine(DownloadRepository.queue) { dl: Map<String, DownloadEntry>, q: List<QueuedDownload> ->
                        Pair(dl, q)
                    }
                    .collect { result ->
                        val downloads: Map<String, DownloadEntry> = result.first
                        val queue: List<QueuedDownload> = result.second
                        val active = downloads.values.filter { !it.done }
                        val queued = queue.size

                        if (active.isEmpty() && queued == 0) {
                            stopSelf()
                            return@collect
                        }

                        val text = when {
                            active.size == 1 && queued == 0 ->
                                "${active[0].gameTitle} — ${active[0].progress.coerceAtLeast(0)}%"
                            active.size == 1 ->
                                "${active[0].gameTitle} — ${active[0].progress.coerceAtLeast(0)}% · $queued queued"
                            active.isNotEmpty() && queued > 0 ->
                                "${active.size} downloading · $queued queued"
                            active.isNotEmpty() ->
                                "${active.size} game${if (active.size > 1) "s" else ""} downloading…"
                            else ->
                                "$queued game${if (queued > 1) "s" else ""} queued"
                        }
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIF_ID, buildNotification(text))
                    }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        monitoring = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GameVault")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notif_download)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
