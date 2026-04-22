package com.gamevault.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.LoginRequest
import com.gamevault.android.ui.games.GamesScreen
import com.gamevault.android.ui.login.LoginScreen
import com.gamevault.android.ui.platforms.PlatformsScreen
import com.gamevault.android.ui.settings.SettingsScreen
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GameVaultTheme
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameVaultTheme {
                val navController = rememberNavController()
                var startDest by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val prefs    = dataStore.data.first()
                    val remote   = prefs[Prefs.SERVER_URL]     ?: ""
                    val local    = prefs[Prefs.LOCAL_URL]      ?: ""
                    val username = prefs[Prefs.USERNAME]       ?: ""
                    val password = prefs[Prefs.SAVED_PASSWORD] ?: ""
                    val remember = prefs[Prefs.REMEMBER_ME]    ?: false

                    if (remote.isBlank() || username.isBlank()) {
                        startDest = "login"
                        return@LaunchedEffect
                    }

                    try {
                        val (api, _) = ApiClient.getApiSmart(remote, local)

                        // Session still valid — go straight in
                        if (api.me().isSuccessful) {
                            startDest = "platforms"
                            return@LaunchedEffect
                        }

                        // Session expired — auto-login if remember me is on
                        if (remember && password.isNotBlank()) {
                            val login = api.login(LoginRequest(username, password))
                            if (login.isSuccessful && login.body()?.ok == true) {
                                startDest = "platforms"
                                return@LaunchedEffect
                            }
                        }
                    } catch (_: Exception) {}

                    startDest = "login"
                }

                if (startDest == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(GVBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = GVRed)
                    }
                } else {
                    NavHost(navController = navController, startDestination = startDest!!) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("platforms") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("platforms") {
                            PlatformsScreen(
                                onPlatformClick = { navController.navigate("games/$it") },
                                onSettingsClick = { navController.navigate("settings") },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("platforms") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("games/{platformId}") { back ->
                            val platformId = back.arguments?.getString("platformId") ?: return@composable
                            GamesScreen(
                                platformId = platformId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
