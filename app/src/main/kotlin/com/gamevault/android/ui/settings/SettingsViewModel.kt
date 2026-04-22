package com.gamevault.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.util.PlatformMapper
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val serverUrl: String = "",
    val localUrl: String = "",
    val romsRootUri: String = "",
    val folderStatus: String = "",
    val urlsSaved: Boolean = false,
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _state.update {
                it.copy(
                    serverUrl = prefs[Prefs.SERVER_URL] ?: "",
                    localUrl  = prefs[Prefs.LOCAL_URL]  ?: "",
                    romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: "",
                )
            }
        }
    }

    fun onServerUrlChange(v: String) = _state.update { it.copy(serverUrl = v) }
    fun onLocalUrlChange(v: String)  = _state.update { it.copy(localUrl = v) }

    fun saveUrls(context: Context) {
        viewModelScope.launch {
            Prefs.setServerUrl(context, _state.value.serverUrl.trim())
            Prefs.setLocalUrl(context, _state.value.localUrl.trim())
            _state.update { it.copy(urlsSaved = true) }
            delay(2000)
            _state.update { it.copy(urlsSaved = false) }
        }
    }

    fun onRomsRootPicked(context: Context, uri: String) {
        context.contentResolver.takePersistableUriPermission(
            Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        _state.update { it.copy(romsRootUri = uri, folderStatus = "Creating folders…") }
        viewModelScope.launch {
            Prefs.setRomsRootUri(context, uri)
            createEsFolders(context, uri)
        }
    }

    private fun createEsFolders(context: Context, rootUri: String) {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(rootUri)) ?: run {
            _state.update { it.copy(folderStatus = "Could not access folder") }
            return
        }
        val folders = PlatformMapper.allFolderNames()
        var created = 0
        for (name in folders) {
            if (root.findFile(name) == null) {
                root.createDirectory(name)
                created++
            }
        }
        _state.update {
            it.copy(folderStatus = if (created > 0) "Created $created folders" else "All folders already exist")
        }
    }
}
