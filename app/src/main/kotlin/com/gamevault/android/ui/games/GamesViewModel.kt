package com.gamevault.android.ui.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.GameMeta
import com.gamevault.android.util.DownloadHelper
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

                // Fetch metadata keyed by filename
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
                                title     = it.title,
                                description = it.description,
                                // box_art_path is "boxart/{platform}/{file}.jpg"
                                // served under /static/ on the Flask server
                                coverUrl  = it.boxArtPath?.let { p -> "$baseUrl/static/$p" },
                            )
                        }
                    )
                }

                _state.update { it.copy(loading = false, platformName = body.platform.name, games = enriched) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun download(context: Context, game: GameItem, platformId: String) {
        val fileName = game.name
        if (_state.value.downloads[fileName]?.done == false) return // already in progress

        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val remote      = prefs[Prefs.SERVER_URL]   ?: ""
            val local       = prefs[Prefs.LOCAL_URL]    ?: ""
            val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: ""

            if (romsRootUri.isBlank()) {
                _state.update {
                    it.copy(downloads = it.downloads + (fileName to DownloadStatus(fileName, 0, true, "Set a ROMs folder in Settings first")))
                }
                return@launch
            }

            val (_, baseUrl) = ApiClient.getApiSmart(remote, local)
            _state.update { it.copy(downloads = it.downloads + (fileName to DownloadStatus(fileName, 0))) }

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
                    },
                )
                _state.update { s ->
                    s.copy(downloads = s.downloads + (fileName to DownloadStatus(fileName, 100, done = true)))
                }
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(downloads = s.downloads + (fileName to DownloadStatus(fileName, 0, done = true, error = e.message)))
                }
            }
        }
    }
}
