package com.gamevault.android.ui.platforms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.Platform
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlatformsState(
    val platforms: List<Platform> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
)

class PlatformsViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlatformsState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "") }
            try {
                val prefs  = context.dataStore.data.first()
                val remote = prefs[Prefs.SERVER_URL] ?: ""
                val local  = prefs[Prefs.LOCAL_URL]  ?: ""
                val (api, _) = ApiClient.getApiSmart(remote, local)
                val response = api.getPlatforms()
                if (response.isSuccessful) {
                    _state.update { it.copy(loading = false, platforms = response.body() ?: emptyList()) }
                } else {
                    _state.update { it.copy(loading = false, error = "Error ${response.code()}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun logout(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val prefs  = context.dataStore.data.first()
                val remote = prefs[Prefs.SERVER_URL] ?: ""
                val local  = prefs[Prefs.LOCAL_URL]  ?: ""
                val (api, _) = ApiClient.getApiSmart(remote, local)
                api.logout()
            } catch (_: Exception) {}
            ApiClient.clearSession()
            Prefs.clearUser(context)
            onDone()
        }
    }
}
