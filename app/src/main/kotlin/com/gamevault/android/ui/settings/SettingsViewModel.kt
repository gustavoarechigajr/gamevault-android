package com.gamevault.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.BuildConfig
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.util.PlatformMapper
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.UpdateHelper
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle        : UpdateState()
    object Checking    : UpdateState()
    object UpToDate    : UpdateState()
    data class Available(val version: String, val downloadUrl: String) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()  // -1 = indeterminate
    data class Error(val message: String) : UpdateState()
}

data class SettingsState(
    val serverUrl: String = "",
    val localUrl: String = "",
    val romsRootUri: String = "",
    val folderStatus: String = "",
    val urlsSaved: Boolean = false,
    val updateState: UpdateState = UpdateState.Idle,
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            val prefs    = context.dataStore.data.first()
            val savedUri = prefs[Prefs.ROMS_ROOT_URI] ?: ""

            var validUri     = savedUri
            var folderStatus = ""
            if (savedUri.isNotBlank()) {
                val parsedUri      = Uri.parse(savedUri)
                val hasPermission  = context.contentResolver.persistedUriPermissions.any {
                    it.uri == parsedUri && it.isReadPermission && it.isWritePermission
                }
                if (!hasPermission) {
                    validUri     = ""
                    folderStatus = "Folder access lost — please re-select"
                    Prefs.setRomsRootUri(context, "")
                }
            }

            _state.update {
                it.copy(
                    serverUrl    = prefs[Prefs.SERVER_URL] ?: "",
                    localUrl     = prefs[Prefs.LOCAL_URL]  ?: "",
                    romsRootUri  = validUri,
                    folderStatus = folderStatus,
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

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.update { it.copy(updateState = UpdateState.Checking) }
            try {
                val release = UpdateHelper.fetchLatestRelease()
                if (UpdateHelper.isNewer(release.tagName, BuildConfig.VERSION_NAME)) {
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        _state.update { it.copy(updateState = UpdateState.Available(release.tagName, apkAsset.downloadUrl)) }
                    } else {
                        _state.update { it.copy(updateState = UpdateState.Error("No APK found in release ${release.tagName}")) }
                    }
                } else {
                    _state.update { it.copy(updateState = UpdateState.UpToDate) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(updateState = UpdateState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun downloadAndInstall(context: Context, downloadUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(updateState = UpdateState.Downloading(-1)) }
            try {
                val apkFile = UpdateHelper.downloadApk(
                    context     = context,
                    downloadUrl = downloadUrl,
                    onProgress  = { pct ->
                        _state.update { it.copy(updateState = UpdateState.Downloading(pct)) }
                    },
                )
                UpdateHelper.installApk(context, apkFile)
                _state.update { it.copy(updateState = UpdateState.Idle) }
            } catch (e: Exception) {
                _state.update { it.copy(updateState = UpdateState.Error(e.message ?: "Download failed")) }
            }
        }
    }

    fun rescanLocalFiles(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(folderStatus = "Scanning…") }
            val count = withContext(Dispatchers.IO) {
                val prefs = context.dataStore.data.first()
                val romsRootUri = prefs[Prefs.ROMS_ROOT_URI] ?: return@withContext 0
                if (romsRootUri.isBlank()) return@withContext 0

                val root = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
                    ?: return@withContext 0
                val allFiles = mutableSetOf<String>()
                root.listFiles().forEach { dir ->
                    if (dir.isDirectory) {
                        dir.listFiles().forEach { file ->
                            file.name?.lowercase()?.let { allFiles.add(it) }
                        }
                    }
                }
                DownloadRepository.addGlobalLocalFiles(allFiles)
                allFiles.size
            }
            _state.update {
                it.copy(folderStatus = if (count > 0) "Found $count game files" else "No files found in ROMs folder")
            }
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
