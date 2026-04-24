package com.gamevault.android.ui.games

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.DownloadEntry
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.GameMeta
import com.gamevault.android.util.DownloadHelper
import com.gamevault.android.util.PlatformMapper
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadStatus(
    val fileName: String,
    val progress: Int,   // 0-100, or -1 for indeterminate
    val done: Boolean = false,
    val error: String? = null,
)

data class GamesState(
    val platformName: String = "",
    val games: List<GameItem> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
    val downloads: Map<String, DownloadStatus> = emptyMap(),
    val localFiles: Set<String> = emptySet(),
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
                            )
                        }
                    )
                }

                val localFiles = scanLocalFiles(context, platformId)
                _state.update { it.copy(loading = false, platformName = body.platform.name, games = enriched, localFiles = localFiles) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

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

            platformDir.listFiles()
                .mapNotNull { it.name }
                .toSet()
        }

    fun download(context: Context, game: GameItem, platformId: String) {
        val fileName = game.name
        if (_state.value.downloads[fileName]?.done == false) return

        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val remote      = prefs[Prefs.SERVER_URL]    ?: ""
            val local       = prefs[Prefs.LOCAL_URL]     ?: ""
            val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: ""

            if (romsRootUri.isBlank()) {
                _state.update {
                    it.copy(downloads = it.downloads + (fileName to DownloadStatus(fileName, 0, true, "Set a ROMs folder in Settings first")))
                }
                return@launch
            }

            val (_, baseUrl) = ApiClient.getApiSmart(remote, local)
            val platformName = _state.value.platformName
            val gameTitle    = game.meta?.title ?: game.name

            _state.update { it.copy(downloads = it.downloads + (fileName to DownloadStatus(fileName, 0))) }
            DownloadRepository.upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, 0))

            try {
                DownloadHelper.download(
                    context       = context,
                    serverBaseUrl = baseUrl,
                    filePath      = game.path,
                    platformId    = platformId,
                    romsRootUri   = romsRootUri,
                    onProgress    = { pct ->
                        _state.update { s ->
                            s.copy(downloads = s.downloads + (fileName to DownloadStatus(fileName, pct)))
                        }
                        DownloadRepository.upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, pct))
                    },
                )
                _state.update { s ->
                    s.copy(
                        downloads  = s.downloads + (fileName to DownloadStatus(fileName, 100, done = true)),
                        localFiles = s.localFiles + fileName,
                    )
                }
                DownloadRepository.upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, 100, done = true))
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(downloads = s.downloads + (fileName to DownloadStatus(fileName, 0, done = true, error = e.message)))
                }
                DownloadRepository.upsert(DownloadEntry(fileName, fileName, gameTitle, platformName, 0, done = true, error = e.message))
            }
        }
    }

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
                platformDir.findFile(game.name)?.delete()
            }
            _state.update { s ->
                s.copy(
                    localFiles = s.localFiles - game.name,
                    downloads  = s.downloads - game.name,
                )
            }
        }
    }
}
