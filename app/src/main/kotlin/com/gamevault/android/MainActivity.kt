package com.gamevault.android

import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.Display
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamevault.android.data.api.ApiClient
import com.gamevault.android.data.model.LoginRequest
import com.gamevault.android.ui.downloads.DownloadsScreen
import com.gamevault.android.ui.games.GamesScreen
import com.gamevault.android.ui.login.LoginScreen
import com.gamevault.android.ui.platforms.PlatformsScreen
import com.gamevault.android.ui.secondscreen.SecondScreenState
import com.gamevault.android.ui.secondscreen.SecondTab
import com.gamevault.android.ui.secondscreen.SecondaryPresentation
import com.gamevault.android.ui.settings.SettingsScreen
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GameVaultTheme
import com.gamevault.android.util.Prefs
import com.gamevault.android.util.dataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = ApiClient.invalidateUrlCache()
        override fun onLost(network: Network)      = ApiClient.invalidateUrlCache()
    }

    private var secondPresentation: SecondaryPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                SecondScreenState.setHasSecondDisplay(true)
            }
        }
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                SecondScreenState.setHasSecondDisplay(false)
            }
        }
        override fun onDisplayChanged(displayId: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(networkCallback)

        val dm = getSystemService(DisplayManager::class.java)
        dm.registerDisplayListener(displayListener, null)

        // Detect second display and load saved toggle state
        val hasSecond = dm.displays.any { it.displayId != Display.DEFAULT_DISPLAY }
        SecondScreenState.init(applicationContext, hasSecond)

        // Reactively create or dismiss the Presentation whenever the toggle or display changes
        lifecycleScope.launch {
            combine(SecondScreenState.enabled, SecondScreenState.hasSecondDisplay) { on, hasDisplay ->
                on && hasDisplay
            }.collect { shouldShow ->
                if (shouldShow) {
                    if (secondPresentation == null) {
                        val secondDisplay = dm.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
                        if (secondDisplay != null) {
                            secondPresentation = SecondaryPresentation(this@MainActivity, secondDisplay)
                                .also { it.show() }
                        }
                    }
                } else {
                    secondPresentation?.dismiss()
                    secondPresentation = null
                }
            }
        }

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

                        if (api.me().isSuccessful) {
                            startDest = "platforms"
                            return@LaunchedEffect
                        }

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
                                onPlatformClick  = { navController.navigate("games/$it") },
                                onSettingsClick  = { navController.navigate("settings") },
                                onDownloadsClick = {
                                    // When dual-screen is active, redirect downloads to second screen
                                    if (SecondScreenState.enabled.value && SecondScreenState.hasSecondDisplay.value) {
                                        SecondScreenState.switchTab(SecondTab.Downloads)
                                    } else {
                                        navController.navigate("downloads")
                                    }
                                },
                                onLogout = {
                                    SecondScreenState.selectPlatform(null)
                                    SecondScreenState.selectGame(null)
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
                        composable("downloads") {
                            DownloadsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        secondPresentation?.dismiss()
        secondPresentation = null
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
    }
}
