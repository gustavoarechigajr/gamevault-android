package com.gamevault.android.data

import android.content.Context
import android.content.Intent
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.service.DownloadService
import com.gamevault.android.util.DownloadHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections

// A pending download waiting in the queue
data class QueuedDownload(
    val key: String,
    val game: GameItem,
    val platformId: String,
    val platformName: String,
    val serverBaseUrl: String,
    val romsRootUri: String,
)

// An active or completed download entry (shown in the downloads screen)
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

    private const val MAX_CONCURRENT = 1

    private lateinit var appContext: Context

    // App-lifetime scope — outlives any ViewModel or Activity
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _queue     = MutableStateFlow<List<QueuedDownload>>(emptyList())
    private val _downloads = MutableStateFlow<Map<String, DownloadEntry>>(emptyMap())

    val queue     = _queue.asStateFlow()
    val downloads = _downloads.asStateFlow()

    // Active download jobs, keyed by fileName
    private val jobs      = ConcurrentHashMap<String, Job>()
    // Keys that were force-started (bypass MAX_CONCURRENT limit)
    private val forceKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val queueMutex = Mutex()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun enqueue(
        context: Context,
        game: GameItem,
        platformId: String,
        platformName: String,
        serverBaseUrl: String,
        romsRootUri: String,
        forceStart: Boolean = false,
    ) {
        val key = game.name
        if (jobs.containsKey(key)) return
        if (_queue.value.any { it.key == key }) return

        val entry = QueuedDownload(key, game, platformId, platformName, serverBaseUrl, romsRootUri)

        // Start the foreground service to keep the process alive
        context.startForegroundService(Intent(context, DownloadService::class.java))

        if (forceStart) {
            forceKeys.add(key)
            scope.launch { startDownload(entry) }
        } else {
            _queue.update { it + entry }
            scope.launch { processQueue() }
        }
    }

    fun cancelDownload(key: String) {
        val job = jobs[key]
        if (job != null) {
            job.cancel()
            // CancellationException handler in startDownload removes the entry
            // DownloadHelper's handler deletes the partial file
        } else {
            // Only queued, not started yet — just remove from queue
            _queue.update { it.filterNot { q -> q.key == key } }
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        _queue.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices) return@update list
            list.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    fun forceStartQueued(key: String) {
        val entry = _queue.value.firstOrNull { it.key == key } ?: return
        _queue.update { it.filterNot { q -> q.key == key } }
        forceKeys.add(key)
        scope.launch { startDownload(entry) }
    }

    fun upsert(entry: DownloadEntry) {
        _downloads.update { map ->
            val preserved = entry.copy(timestamp = map[entry.key]?.timestamp ?: entry.timestamp)
            map + (entry.key to preserved)
        }
    }

    fun remove(key: String) {
        _downloads.update { it - key }
    }

    fun clearCompleted() {
        _downloads.update { map -> map.filterValues { !it.done } }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private suspend fun processQueue() {
        queueMutex.withLock {
            while (true) {
                val activeQueued = jobs.keys.count { it !in forceKeys }
                if (activeQueued >= MAX_CONCURRENT) break
                val next = _queue.value.firstOrNull() ?: break
                _queue.update { it.drop(1) }
                // Launch without holding the mutex so it doesn't block
                scope.launch { startDownload(next) }
            }
        }
    }

    private suspend fun startDownload(entry: QueuedDownload) {
        val key       = entry.key
        val gameTitle = entry.game.meta?.title ?: entry.game.name

        upsert(DownloadEntry(key, key, gameTitle, entry.platformName, -1))

        val job = scope.launch {
            try {
                DownloadHelper.download(
                    context       = appContext,
                    serverBaseUrl = entry.serverBaseUrl,
                    filePath      = entry.game.path,
                    platformId    = entry.platformId,
                    romsRootUri   = entry.romsRootUri,
                    onProgress    = { pct ->
                        upsert(DownloadEntry(key, key, gameTitle, entry.platformName, pct))
                    },
                )
                upsert(DownloadEntry(key, key, gameTitle, entry.platformName, 100, done = true))
            } catch (e: CancellationException) {
                remove(key)
                throw e
            } catch (e: Exception) {
                upsert(DownloadEntry(key, key, gameTitle, entry.platformName, 0, done = true, error = e.message))
            } finally {
                jobs.remove(key)
                forceKeys.remove(key)
                // NonCancellable so processQueue() runs even when this coroutine is being cancelled
                withContext(NonCancellable) { processQueue() }
            }
        }
        jobs[key] = job
    }
}
