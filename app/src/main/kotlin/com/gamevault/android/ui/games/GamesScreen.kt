package com.gamevault.android.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gamevault.android.data.model.GameItem
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    platformId: String,
    onBack: () -> Unit,
    vm: GamesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(platformId) { vm.load(context, platformId) }

    val displayedGames = remember(state.games, searchQuery) {
        if (searchQuery.isBlank()) state.games
        else state.games.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = GVBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            state.platformName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GVBackground),
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ${state.platformName}…", color = Color(0xFF7090B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7090B8)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GVRed,
                        unfocusedBorderColor = Color(0xFF1C2A44),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFDCE8FF),
                        unfocusedContainerColor = GVSurface,
                        focusedContainerColor = GVSurface,
                    ),
                )
            }
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
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayedGames, key = { it.path }) { game ->
                        GameRow(
                            game = game,
                            downloadStatus = state.downloads[game.name],
                            onDownload = { vm.download(context, game, platformId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameRow(
    game: GameItem,
    downloadStatus: DownloadStatus?,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GVSurface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val coverUrl = game.meta?.coverUrl
        if (coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF182240)),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.meta?.title ?: game.name,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (game.sizeHuman != null) {
                Text(
                    text = game.sizeHuman,
                    color = Color(0xFF7090B8),
                    fontSize = 12.sp,
                )
            }
            // Show progress bar while downloading
            if (downloadStatus != null && !downloadStatus.done) {
                Spacer(Modifier.height(4.dp))
                if (downloadStatus.progress >= 0) {
                    LinearProgressIndicator(
                        progress = { downloadStatus.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                    Text(
                        text = "${downloadStatus.progress}%",
                        color = Color(0xFF7090B8),
                        fontSize = 11.sp,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                }
            }
            if (downloadStatus?.done == true && downloadStatus.error != null) {
                Text(
                    text = downloadStatus.error,
                    color = GVRed,
                    fontSize = 11.sp,
                )
            }
        }

        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when {
                downloadStatus != null && !downloadStatus.done -> {
                    // progress is shown in the column; nothing here
                }
                downloadStatus?.done == true && downloadStatus.error == null -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50))
                }
                downloadStatus?.done == true && downloadStatus.error != null -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Retry", tint = GVRed)
                    }
                }
                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = GVRed)
                    }
                }
            }
        }
    }
}
