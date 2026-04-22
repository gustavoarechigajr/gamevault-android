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

data class GamesState(
    val platformName: String = "",
    val games: List<GameItem> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
)

class GamesViewModel : ViewModel() {
    private val _state = MutableStateFlow(GamesState())
    val state = _state.asStateFlow()

    fun load(context: Context, platformId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "") }
            try {
                val prefs    = context.dataStore.data.first()
                val remote   = prefs[Prefs.SERVER_URL] ?: ""
                val local    = prefs[Prefs.LOCAL_URL]  ?: ""
                val api      = ApiClient.getApiSmart(remote, local)

                // Determine which base URL actually worked (for building cover URLs)
                val baseUrl  = (prefs[Prefs.LOCAL_URL] ?: "").ifBlank { remote }.trimEnd('/')

                val gamesResp = api.getPlatform(platformId)
                if (!gamesResp.isSuccessful) {
                    _state.update { it.copy(loading = false, error = "Error ${gamesResp.code()}") }
                    return@launch
                }
                val body = gamesResp.body()!!
                val games = body.items.filter { it.type == "file" }

                // Fetch metadata and attach cover URLs
                val metaResp = api.getPlatformMetadata(platformId)
                val metaMap: Map<String, GameMeta> = if (metaResp.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    (metaResp.body() as? Map<String, Map<String, Any?>>)?.mapValues { (_, v) ->
                        GameMeta(
                            title      = v["title"] as? String,
                            description = v["description"] as? String,
                            boxArtPath = v["box_art_path"] as? String,
                            coverUrl   = (v["box_art_path"] as? String)?.let { "$baseUrl/$it" },
                            rating     = v["rating"]?.toString(),
                            genre      = v["genre"] as? String,
                            year       = v["year"]?.toString(),
                        )
                    } ?: emptyMap()
                } else emptyMap()

                val enriched = games.map { game ->
                    val key = game.metaKey ?: game.name
                    game.copy(meta = metaMap[key] ?: metaMap[game.name])
                }

                _state.update {
                    it.copy(loading = false, platformName = body.platform.name, games = enriched)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun download(context: Context, game: GameItem, platformId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val remote = prefs[Prefs.SERVER_URL] ?: ""
            val local  = prefs[Prefs.LOCAL_URL]  ?: ""
            val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: ""
            if (romsRootUri.isBlank()) {
                onResult("Set a ROMs root folder in Settings first")
                return@launch
            }
            // Use local URL for download if available (faster on home network)
            val baseUrl = local.ifBlank { remote }
            val id = DownloadHelper.enqueue(
                context      = context,
                serverBaseUrl = baseUrl,
                filePath     = game.path,
                platformId   = platformId,
                romsRootUri  = romsRootUri,
                sessionCookie = "",
            )
            if (id == -1L) onResult("Failed to start download")
            else onResult("Downloading ${game.name}…")
        }
    }
}
