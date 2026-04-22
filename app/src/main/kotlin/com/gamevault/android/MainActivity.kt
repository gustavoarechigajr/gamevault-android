package com.gamevault.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamevault.android.ui.games.GamesScreen
import com.gamevault.android.ui.login.LoginScreen
import com.gamevault.android.ui.platforms.PlatformsScreen
import com.gamevault.android.ui.settings.SettingsScreen
import com.gamevault.android.ui.theme.GameVaultTheme
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameVaultTheme {
                val navController = rememberNavController()
                val serverUrl by dataStore.data
                    .map { it[Prefs.SERVER_URL] ?: "" }
                    .collectAsState(initial = "")
                val username by dataStore.data
                    .map { it[Prefs.USERNAME] ?: "" }
                    .collectAsState(initial = "")

                val startDest = if (serverUrl.isNotBlank() && username.isNotBlank()) "platforms" else "login"

                NavHost(navController = navController, startDestination = startDest) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { navController.navigate("platforms") { popUpTo("login") { inclusive = true } } }
                        )
                    }
                    composable("platforms") {
                        PlatformsScreen(
                            onPlatformClick = { platformId -> navController.navigate("games/$platformId") },
                            onSettingsClick = { navController.navigate("settings") },
                            onLogout = { navController.navigate("login") { popUpTo("platforms") { inclusive = true } } },
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
