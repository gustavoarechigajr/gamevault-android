package com.gamevault.android.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.LoginRequest
import com.gamevault.android.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String = "",
    val loggedIn: Boolean = false,
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onServerUrlChange(v: String) = _state.update { it.copy(serverUrl = v, error = "") }
    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = "") }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = "") }

    fun login(context: Context) {
        val s = _state.value
        if (s.serverUrl.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "") }
            try {
                val savedUrl = Prefs.serverUrl(context).first()
                val url = s.serverUrl.ifBlank { savedUrl }
                val api = ApiClient.getApi(url)
                val response = api.login(LoginRequest(s.username.trim(), s.password))
                if (response.isSuccessful && response.body()?.ok == true) {
                    Prefs.setServerUrl(context, url)
                    Prefs.setUser(context, s.username.trim(), "user")
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
