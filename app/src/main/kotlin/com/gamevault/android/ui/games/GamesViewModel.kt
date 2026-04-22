package com.gamevault.android.ui.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.util.DownloadHelper
import com.gamevault.android.util.Prefs
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
                val url = Prefs.serverUrl(context).first()
                val api = ApiClient.getApi(url)
                val response = api.getPlatform(platformId)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _state.update {
                        it.copy(
                            loading = false,
                            platformName = body.platform.name,
                            games = body.items.filter { g -> g.type == "file" },
                        )
                    }
                } else {
                    _state.update { it.copy(loading = false, error = "Error ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun download(context: Context, game: GameItem, platformId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val url = Prefs.serverUrl(context).first()
            val romsRootUri = Prefs.romsRootUri(context).first()
            if (romsRootUri.isBlank()) {
                onResult("Set a ROMs root folder in Settings first")
                return@launch
            }
            val id = DownloadHelper.enqueue(
                context = context,
                serverBaseUrl = url,
                filePath = game.path,
                platformId = platformId,
                romsRootUri = romsRootUri,
                sessionCookie = "",
            )
            if (id == -1L) {
                onResult("Failed to start download")
            } else {
                onResult("Downloading ${game.name}…")
            }
        }
    }
}
