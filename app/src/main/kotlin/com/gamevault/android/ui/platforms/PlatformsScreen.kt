package com.gamevault.android.ui.platforms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.model.Platform
import com.gamevault.android.ui.secondscreen.SecondScreenState
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface
import com.gamevault.android.util.PlatformMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformsScreen(
    onPlatformClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onLogout: () -> Unit,
    vm: PlatformsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state   by vm.state.collectAsState()
    val activeDownloadCount by DownloadRepository.downloads.collectAsState()
    val dualScreenEnabled   by SecondScreenState.enabled.collectAsState()
    val hasSecondDisplay    by SecondScreenState.hasSecondDisplay.collectAsState()

    BackHandler { /* consume back press — prevents black-screen flash on AYN Thor */ }

    LaunchedEffect(Unit) { vm.load(context) }

    Scaffold(
        containerColor = GVBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GameVault",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White,
                    )
                },
                actions = {
                    val activeCount = activeDownloadCount.values.count { !it.done }

                    // Dual-screen toggle — only shown when a second display is physically present
                    if (hasSecondDisplay) {
                        IconButton(onClick = { SecondScreenState.toggle(context) }) {
                            Icon(
                                Icons.Default.Splitscreen,
                                contentDescription = "Toggle dual screen",
                                tint = if (dualScreenEnabled) GVRed else Color(0xFF7090B8),
                            )
                        }
                    }

                    BadgedBox(
                        badge = {
                            if (activeCount > 0) {
                                Badge(containerColor = GVRed) {
                                    Text(activeCount.toString(), color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onDownloadsClick) {
                            Icon(Icons.Default.Download, contentDescription = "Downloads", tint = Color(0xFF7090B8))
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF7090B8))
                    }
                    IconButton(onClick = { vm.logout(context, onLogout) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFF7090B8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GVBackground),
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GVRed,
                )
                state.error.isNotBlank() -> Text(
                    state.error,
                    color = GVRed,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.platforms) { platform ->
                        PlatformCard(
                            platform = platform,
                            onClick  = {
                                SecondScreenState.selectGame(null)
                                SecondScreenState.selectPlatform(platform)
                                onPlatformClick(platform.id)
                            },
                            onFocus  = { SecondScreenState.selectPlatform(platform) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(
    platform: Platform,
    onClick: () -> Unit,
    onFocus: () -> Unit,
) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(platform.color)) }
        .getOrDefault(Color(0xFF4ADE80))
    val drawableRes = PlatformMapper.getDrawableRes(platform.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GVSurface)
            .onFocusChanged { if (it.hasFocus) onFocus() }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(4.dp, 36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(2.dp))
        }

        Column {
            Text(
                text = platform.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
            )
            Text(
                text = "${platform.count} games",
                color = Color(0xFF7090B8),
                fontSize = 12.sp,
            )
        }
    }
}
