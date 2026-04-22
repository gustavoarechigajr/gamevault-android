package com.gamevault.android.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.LoginRequest
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val loading: Boolean = false,
    val error: String = "",
    val loggedIn: Boolean = false,
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onServerUrlChange(v: String) = _state.update { it.copy(serverUrl = v, error = "") }
    fun onUsernameChange(v: String)  = _state.update { it.copy(username = v, error = "") }
    fun onPasswordChange(v: String)  = _state.update { it.copy(password = v, error = "") }
    fun onRememberMeChange(v: Boolean) = _state.update { it.copy(rememberMe = v) }

    fun init(context: Context) {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            _state.update {
                it.copy(
                    serverUrl  = prefs[Prefs.SERVER_URL]     ?: "",
                    username   = prefs[Prefs.USERNAME]       ?: "",
                    password   = prefs[Prefs.SAVED_PASSWORD] ?: "",
                    rememberMe = prefs[Prefs.REMEMBER_ME]    ?: false,
                )
            }
        }
    }

    fun login(context: Context) {
        val s = _state.value
        if (s.serverUrl.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "") }
            try {
                val prefs    = context.dataStore.data.first()
                val localUrl = prefs[Prefs.LOCAL_URL] ?: ""
                val api      = ApiClient.getApiSmart(s.serverUrl.trim(), localUrl)
                val response = api.login(LoginRequest(s.username.trim(), s.password))
                if (response.isSuccessful && response.body()?.ok == true) {
                    Prefs.setServerUrl(context, s.serverUrl.trim())
                    Prefs.setUser(context, s.username.trim(), "user")
                    Prefs.saveCredentials(context, s.password, s.rememberMe)
                    _state.update { it.copy(loading = false, loggedIn = true) }
                } else {
                    val msg = response.body()?.error ?: "Login failed (${response.code()})"
                    _state.update { it.copy(loading = false, error = msg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Cannot reach server: ${e.message}") }
            }
        }
    }
}
