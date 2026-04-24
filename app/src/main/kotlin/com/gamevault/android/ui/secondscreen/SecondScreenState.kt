package com.gamevault.android.ui.secondscreen

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.Platform
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SecondTab { GameInfo, Downloads }

object SecondScreenState {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()

    private val _hasSecondDisplay = MutableStateFlow(false)
    val hasSecondDisplay = _hasSecondDisplay.asStateFlow()

    private val _selectedGame = MutableStateFlow<GameItem?>(null)
    val selectedGame = _selectedGame.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<Platform?>(null)
    val selectedPlatform = _selectedPlatform.asStateFlow()

    private val _activeTab = MutableStateFlow(SecondTab.GameInfo)
    val activeTab = _activeTab.asStateFlow()

    fun init(context: Context, hasSecondDisplay: Boolean) {
        _hasSecondDisplay.value = hasSecondDisplay
        scope.launch {
            val prefs = context.dataStore.data.first()
            _enabled.value = prefs[Prefs.DUAL_SCREEN_ENABLED] ?: false
        }
    }

    fun setHasSecondDisplay(has: Boolean) {
        _hasSecondDisplay.value = has
    }

    fun toggle(context: Context) {
        val newVal = !_enabled.value
        _enabled.value = newVal
        scope.launch {
            context.dataStore.edit { it[Prefs.DUAL_SCREEN_ENABLED] = newVal }
        }
    }

    fun selectGame(game: GameItem?) { _selectedGame.value = game }
    fun selectPlatform(platform: Platform?) { _selectedPlatform.value = platform }
    fun switchTab(tab: SecondTab) { _activeTab.value = tab }
}
