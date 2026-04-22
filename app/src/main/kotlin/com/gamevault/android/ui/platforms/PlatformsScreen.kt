package com.gamevault.android.ui.platforms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamevault.android.data.model.Platform
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformsScreen(
    onPlatformClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    vm: PlatformsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

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
                    color = Color(0xFFE4000F),
                )
                state.error.isNotBlank() -> Text(
                    state.error,
                    color = Color(0xFFE4000F),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.platforms) { platform ->
                        PlatformCard(platform = platform, onClick = { onPlatformClick(platform.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(platform: Platform, onClick: () -> Unit) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(platform.color)) }
        .getOrDefault(Color(0xFF4ADE80))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GVSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
                .align(Alignment.CenterStart)
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
        ) {
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
