package com.gamevault.android.data

import android.content.Context
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.util.DownloadHelper
import com.gamevault.android.util.PlatformMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadEntry(
    val key: String,
    val fileName: String,
    val gameTitle: String,
    val platformName: String,
    val progress: Int,         // 0-100, or -1 for indeterminate
    val done: Boolean = false,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

object DownloadRepository {
    private val _downloads = MutableStateFlow<Map<String, DownloadEntry>>(emptyMap())
    val downloads = _downloads.asStateFlow()

    // Outlives any individual ViewModel — downloads continue across navigation
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startDownload(
        context: Context,
        game: GameItem,
        platformId: String,
        platformName: String,
        serverBaseUrl: String,
        romsRootUri: String,
    ) {
        val fileName  = game.name
        val gameTitle = game.meta?.title ?: game.name

        if (_downloads.value[fileName]?.done == false) return

        upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, -1))

        scope.launch {
            try {
                DownloadHelper.download(
                    context       = context.applicationContext,
                    serverBaseUrl = serverBaseUrl,
                    filePath      = game.path,
                    platformId    = platformId,
                    romsRootUri   = romsRootUri,
                    onProgress    = { pct ->
                        upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, pct))
                    },
                )
                upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, 100, done = true))
            } catch (e: CancellationException) {
                remove(fileName)
                throw e
            } catch (e: Exception) {
                upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, 0, done = true, error = e.message))
            }
        }
    }

    fun upsert(entry: DownloadEntry) {
        _downloads.update { it + (entry.key to entry) }
    }

    fun remove(key: String) {
        _downloads.update { it - key }
    }

    fun clearCompleted() {
        _downloads.update { map -> map.filterValues { !it.done } }
    }
}
