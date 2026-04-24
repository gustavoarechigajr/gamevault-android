package com.gamevault.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.service.DownloadService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadRepository.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            DownloadService.CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "GameVault download progress" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
