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
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameVaultTheme {
                val navController = rememberNavController()
                var startDest by remember { mutableStateOf<String?>(null) }

                // Verify session on every launch instead of trusting saved username alone
                LaunchedEffect(Unit) {
                    val serverUrl = dataStore.data.map { it[Prefs.SERVER_URL] ?: "" }.first()
                    val username = dataStore.data.map { it[Prefs.USERNAME] ?: "" }.first()

                    startDest = if (serverUrl.isBlank() || username.isBlank()) {
                        "login"
                    } else {
                        try {
                            val response = ApiClient.getApi(serverUrl).me()
                            if (response.isSuccessful) "platforms" else "login"
                        } catch (e: Exception) {
                            // Server unreachable — go to login so the user can see the error
                            // and retry, rather than silently failing on the platforms screen
                            "login"
                        }
                    }
                }

                if (startDest == null) {
                    // Splash while we check the session
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
                                onPlatformClick = { platformId -> navController.navigate("games/$platformId") },
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
