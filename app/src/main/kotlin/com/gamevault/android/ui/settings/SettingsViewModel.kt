package com.gamevault.android.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val serverUrl: String = "",
    val romsRootUri: String = "",
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            val url = Prefs.serverUrl(context).first()
            val roms = Prefs.romsRootUri(context).first()
            _state.update { it.copy(serverUrl = url, romsRootUri = roms) }
        }
    }

    fun onServerUrlChange(v: String) = _state.update { it.copy(serverUrl = v) }

    fun saveServerUrl(context: Context) {
        viewModelScope.launch {
            Prefs.setServerUrl(context, _state.value.serverUrl.trim())
        }
    }

    fun onRomsRootPicked(context: Context, uri: String) {
        // Persist persistent URI permission so we can write across reboots
        context.contentResolver.takePersistableUriPermission(
            android.net.Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        _state.update { it.copy(romsRootUri = uri) }
        viewModelScope.launch { Prefs.setRomsRootUri(context, uri) }
    }
}
