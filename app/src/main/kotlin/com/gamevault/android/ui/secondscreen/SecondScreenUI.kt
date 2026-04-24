package com.gamevault.android.ui.secondscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gamevault.android.data.DownloadEntry
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.QueuedDownload
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.Platform
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface
import com.gamevault.android.util.PlatformMapper

@Composable
fun SecondScreenUI() {
    val activeTab       by SecondScreenState.activeTab.collectAsState()
    val selectedGame    by SecondScreenState.selectedGame.collectAsState()
    val selectedPlatform by SecondScreenState.selectedPlatform.collectAsState()
    val downloads       by DownloadRepository.downloads.collectAsState()
    val queue           by DownloadRepository.queue.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GVBackground),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                SecondTab.GameInfo -> GameInfoContent(selectedGame, selectedPlatform)
                SecondTab.Downloads -> DownloadsContent(downloads, queue)
            }
        }

        NavigationBar(containerColor = GVSurface, tonalElevation = 0.dp) {
            NavigationBarItem(
                selected = activeTab == SecondTab.GameInfo,
                onClick  = { SecondScreenState.switchTab(SecondTab.GameInfo) },
                icon     = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                label    = { Text("Game Info", fontSize = 11.sp) },
                colors   = navBarItemColors(),
            )
            NavigationBarItem(
                selected = activeTab == SecondTab.Downloads,
                onClick  = { SecondScreenState.switchTab(SecondTab.Downloads) },
                icon     = { Icon(Icons.Default.Download, contentDescription = null) },
                label    = { Text("Downloads", fontSize = 11.sp) },
                colors   = navBarItemColors(),
            )
        }
    }
}

@Composable
private fun navBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = GVRed,
    selectedTextColor   = GVRed,
    indicatorColor      = Color(0xFF1A2540),
    unselectedIconColor = Color(0xFF7090B8),
    unselectedTextColor = Color(0xFF7090B8),
)

// ── Game Info tab ─────────────────────────────────────────────────────────────

@Composable
private fun GameInfoContent(game: GameItem?, platform: Platform?) {
    when {
        game     != null -> GameDetail(game)
        platform != null -> PlatformDetail(platform)
        else             -> EmptyInfoHint()
    }
}

@Composable
private fun GameDetail(game: GameItem) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val coverUrl = game.meta?.coverUrl
        if (coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GVSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = Color(0xFF3A4A6A), modifier = Modifier.size(52.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text       = game.meta?.title ?: game.name,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (game.meta?.year != null) {
                Text(game.meta.year, color = Color(0xFF7090B8), fontSize = 11.sp)
            }
            val ratingStr = game.meta?.rating?.toFloatOrNull()?.let { "★ %.1f".format(it / 2f) }
            if (ratingStr != null) {
                Text(ratingStr, color = GVRed, fontSize = 11.sp)
            }
            if (game.sizeHuman != null) {
                Text(game.sizeHuman, color = Color(0xFF7090B8), fontSize = 11.sp)
            }
        }

        if (!game.meta?.description.isNullOrBlank()) {
            Text(
                text     = game.meta!!.description!!,
                color    = Color(0xFFDCE8FF),
                fontSize = 11.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun PlatformDetail(platform: Platform) {
    val drawableRes = PlatformMapper.getDrawableRes(platform.id)
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(platform.color)) }
        .getOrDefault(Color(0xFF4ADE80))

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = null,
                modifier = Modifier.size(88.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = accentColor, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text       = platform.name,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
        )
        Text(
            text     = "${platform.count} games",
            color    = Color(0xFF7090B8),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyInfoHint() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.SportsEsports, null, tint = Color(0xFF1E2E48), modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(10.dp))
        Text("Select a game or console", color = Color(0xFF3A4A6A), fontSize = 13.sp)
    }
}

// ── Downloads tab ─────────────────────────────────────────────────────────────

@Composable
private fun DownloadsContent(
    downloads: Map<String, DownloadEntry>,
    queue: List<QueuedDownload>,
) {
    val active  = downloads.values.filter { !it.done }.sortedByDescending { it.timestamp }
    val recent  = downloads.values.filter { it.done && it.error == null }.sortedByDescending { it.timestamp }
    val failed  = downloads.values.filter { it.done && it.error != null }.sortedByDescending { it.timestamp }

    if (downloads.isEmpty() && queue.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Download, null, tint = Color(0xFF1E2E48), modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(8.dp))
            Text("No downloads yet", color = Color(0xFF3A4A6A), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (active.isNotEmpty()) {
            item { DlSectionLabel("In Progress") }
            items(active, key = { "a:${it.key}" }) { entry ->
                ActiveCompactRow(entry)
            }
        }

        if (queue.isNotEmpty()) {
            item { DlSectionLabel("Queue (${queue.size})") }
            items(queue.take(5), key = { "q:${it.key}" }) { item ->
                DlSimpleRow(
                    icon  = Icons.Default.Schedule,
                    tint  = Color(0xFF4A5A7A),
                    title = item.game.meta?.title ?: item.game.name,
                )
            }
        }

        if (recent.isNotEmpty()) {
            item { DlSectionLabel("Downloaded") }
            items(recent.take(5), key = { "r:${it.key}" }) { entry ->
                DlSimpleRow(
                    icon  = Icons.Default.CheckCircle,
                    tint  = Color(0xFF4CAF50),
                    title = entry.gameTitle,
                )
            }
        }

        if (failed.isNotEmpty()) {
            item { DlSectionLabel("Failed (${failed.size})") }
            items(failed.take(3), key = { "f:${it.key}" }) { entry ->
                DlSimpleRow(
                    icon  = Icons.Default.ErrorOutline,
                    tint  = GVRed,
                    title = entry.gameTitle,
                )
            }
        }
    }
}

@Composable
private fun DlSectionLabel(text: String) {
    Text(
        text       = text,
        color      = Color(0xFF7090B8),
        fontSize   = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
    )
}

@Composable
private fun DlSimpleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(title, color = Color(0xFFDCE8FF), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActiveCompactRow(entry: DownloadEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier    = Modifier.size(12.dp),
                color       = GVRed,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                entry.gameTitle,
                color    = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (entry.progress >= 0) {
                Text("${entry.progress}%", color = GVRed, fontSize = 11.sp)
            }
        }
        if (entry.progress >= 0) {
            LinearProgressIndicator(
                progress   = { entry.progress / 100f },
                modifier   = Modifier.fillMaxWidth(),
                color      = GVRed,
                trackColor = Color(0xFF1C2A44),
            )
        } else {
            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth(),
                color      = GVRed,
                trackColor = Color(0xFF1C2A44),
            )
        }
    }
}
