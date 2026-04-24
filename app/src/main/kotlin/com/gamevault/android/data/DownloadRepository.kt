package com.gamevault.android.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DownloadEntry(
    val key: String,           // unique — same as fileName used in GamesViewModel
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
