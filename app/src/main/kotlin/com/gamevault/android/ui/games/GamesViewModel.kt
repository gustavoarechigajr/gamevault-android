package com.gamevault.android.ui.games

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.GameMeta
import com.gamevault.android.util.PlatformMapper
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder(val label: String) {
    Alphabetical("A–Z"),
    Size("Size"),
    Year("Year"),
    Downloaded("Downloaded"),
    Rating("Rating"),
}

data class DownloadStatus(
    val fileName: String,
    val progress: Int,
    val done: Boolean = false,
    val error: String? = null,
)

data class GamesState(
    val platformName: String = "",
    val games: List<GameItem> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
    val downloads: Map<String, DownloadStatus> = emptyMap(),
    // Stored as lowercase for case-insensitive matching against server filenames
    val localFiles: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.Alphabetical,
    val queuedKeys: Set<String> = emptySet(),
    val verifyMessage: String? = null,
)

class GamesViewModel : ViewModel() {
    private val _state = MutableStateFlow(GamesState())
    val state = _state.asStateFlow()

    fun load(context: Context, platformId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "") }
            try {
                val prefs  = context.dataStore.data.first()
                val remote = prefs[Prefs.SERVER_URL] ?: ""
                val local  = prefs[Prefs.LOCAL_URL]  ?: ""
                val (api, baseUrl) = ApiClient.getApiSmart(remote, local)

                val gamesResp = api.getPlatform(platformId)
                if (!gamesResp.isSuccessful) {
                    _state.update { it.copy(loading = false, error = "Error ${gamesResp.code()}") }
                    return@launch
                }
                val body  = gamesResp.body()!!
                val games = body.items.filter { it.type == "file" }

                val metaMap = try {
                    val r = api.getPlatformMetadata(platformId)
                    if (r.isSuccessful) r.body() ?: emptyMap() else emptyMap()
                } catch (_: Exception) { emptyMap() }

                val enriched = games.map { game ->
                    val key  = game.metaKey ?: game.name
                    val meta = metaMap[key] ?: metaMap[game.name]
                    game.copy(
                        meta = meta?.let {
                            GameMeta(
                                title       = it.title,
                                description = it.description,
                                coverUrl    = it.boxArtPath?.let { p -> "$baseUrl/static/$p" },
                                year        = it.year,
                                rating      = it.rating,
                            )
                        }
                    )
                }

                val localFiles = scanLocalFiles(context, platformId)
                _state.update {
                    it.copy(loading = false, platformName = body.platform.name, games = enriched, localFiles = localFiles)
                }

                observeRepoDownloads(enriched.map { it.name }.toSet())
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun observeRepoDownloads(gameNames: Set<String>) {
        viewModelScope.launch {
            combine(
                DownloadRepository.downloads,
                DownloadRepository.queue,
                DownloadRepository.globalLocalFiles,
            ) { allDownloads, queue, globalFiles -> Triple(allDownloads, queue, globalFiles) }
            .collect { (allDownloads, queue, globalFiles) ->
                val relevant   = allDownloads.filterKeys { it in gameNames }
                val queuedKeys = queue.map { it.key }.filter { it in gameNames }.toSet()

                _state.update { s ->
                    var newDownloads  = emptyMap<String, DownloadStatus>()
                    // Seed from the global scan (files found via Settings → Rescan)
                    var newLocalFiles = s.localFiles + globalFiles.filter { f ->
                        gameNames.any { name -> name.lowercase() == f }
                    }
                    for ((key, entry) in relevant) {
                        newDownloads = newDownloads + (key to DownloadStatus(key, entry.progress, entry.done, entry.error))
                        if (entry.done && entry.error == null) {
                            newLocalFiles = newLocalFiles + key.lowercase()
                        }
                    }
                    s.copy(downloads = newDownloads, localFiles = newLocalFiles, queuedKeys = queuedKeys)
                }
            }
        }
    }

    // Returns lowercase filenames for case-insensitive matching against server filenames
    private suspend fun scanLocalFiles(context: Context, platformId: String): Set<String> =
        withContext(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: return@withContext emptySet()
            if (romsRootUri.isBlank()) return@withContext emptySet()

            val folderName = PlatformMapper.getFolderName(platformId)
            val root = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
                ?: return@withContext emptySet()
            val platformDir = root.findFile(folderName)
                ?: return@withContext emptySet()

            platformDir.listFiles().mapNotNull { it.name?.lowercase() }.toSet()
        }

    fun download(context: Context, game: GameItem, platformId: String) {
        if (DownloadRepository.downloads.value[game.name]?.done == false) return
        if (DownloadRepository.queue.value.any { it.key == game.name }) return

        viewModelScope.launch {
            val prefs       = context.dataStore.data.first()
            val remote      = prefs[Prefs.SERVER_URL]    ?: ""
            val local       = prefs[Prefs.LOCAL_URL]     ?: ""
            val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: ""

            if (romsRootUri.isBlank()) {
                _state.update {
                    it.copy(downloads = it.downloads + (game.name to DownloadStatus(game.name, 0, true, "Set a ROMs folder in Settings first")))
                }
                return@launch
            }

            val (_, baseUrl) = ApiClient.getApiSmart(remote, local)
            DownloadRepository.enqueue(
                context       = context,
                game          = game,
                platformId    = platformId,
                platformName  = _state.value.platformName,
                serverBaseUrl = baseUrl,
                romsRootUri   = romsRootUri,
            )
        }
    }

    fun cancelDownload(game: GameItem) {
        DownloadRepository.cancelDownload(game.name)
    }

    fun setSortOrder(order: SortOrder) = _state.update { it.copy(sortOrder = order) }

    fun deleteGame(context: Context, game: GameItem, platformId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val prefs = context.dataStore.data.first()
                val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: return@withContext
                if (romsRootUri.isBlank()) return@withContext

                val folderName = PlatformMapper.getFolderName(platformId)
                val root = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
                    ?: return@withContext
                val platformDir = root.findFile(folderName) ?: return@withContext
                // Case-insensitive delete: find by lowercased name
                platformDir.listFiles().firstOrNull { it.name?.lowercase() == game.name.lowercase() }?.delete()
            }
            DownloadRepository.remove(game.name)
            _state.update { s ->
                s.copy(
                    localFiles = s.localFiles - game.name.lowercase(),
                    downloads  = s.downloads  - game.name,
                )
            }
        }
    }

    fun verifyFile(context: Context, game: GameItem, platformId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val prefs = context.dataStore.data.first()
                val romsRootUri = prefs[Prefs.ROMS_ROOT_URI]
                    ?: return@withContext "ROMs folder not configured in Settings"
                if (romsRootUri.isBlank()) return@withContext "ROMs folder not configured in Settings"

                val folderName = PlatformMapper.getFolderName(platformId)
                val root = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
                    ?: return@withContext "Cannot access ROMs folder"
                val platformDir = root.findFile(folderName)
                    ?: return@withContext "Platform folder not found"

                val file = platformDir.listFiles()
                    .firstOrNull { it.name?.lowercase() == game.name.lowercase() }
                    ?: return@withContext "File not found on device"

                val localSize    = file.length()
                val expectedSize = game.size

                if (expectedSize != null && expectedSize > 0 && localSize != expectedSize) {
                    return@withContext "Size mismatch — got ${localSize}B, expected ${expectedSize}B (${game.sizeHuman}). File may be corrupted."
                }

                "File OK — ${game.sizeHuman ?: "${localSize} bytes"}"
            }
            _state.update { it.copy(verifyMessage = result) }
        }
    }

    fun clearVerifyMessage() = _state.update { it.copy(verifyMessage = null) }
}
