package com.gamevault.android.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.launch

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

    val displayedGames = remember(state.games, searchQuery, state.sortOrder, state.localFiles) {
        val filtered = if (searchQuery.isBlank()) state.games
                       else state.games.filter { it.name.contains(searchQuery, ignoreCase = true) }
        when (state.sortOrder) {
            SortOrder.Alphabetical -> filtered.sortedBy { (it.meta?.title ?: it.name).lowercase() }
            SortOrder.Size         -> filtered.sortedByDescending { it.size ?: 0L }
            SortOrder.Year         -> filtered.sortedByDescending { it.meta?.year ?: "" }
            SortOrder.Downloaded   -> filtered.sortedWith(
                compareByDescending<GameItem> { if (it.name in state.localFiles) 1 else 0 }
                    .thenBy { (it.meta?.title ?: it.name).lowercase() }
            )
            SortOrder.Rating       -> filtered.sortedByDescending { it.meta?.rating?.toFloatOrNull() ?: 0f }
        }
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
                        focusedBorderColor    = GVRed,
                        unfocusedBorderColor  = Color(0xFF1C2A44),
                        focusedTextColor      = Color.White,
                        unfocusedTextColor    = Color(0xFFDCE8FF),
                        unfocusedContainerColor = GVSurface,
                        focusedContainerColor   = GVSurface,
                    ),
                )
                // Sort chips
                ScrollableTabRow(
                    selectedTabIndex = state.sortOrder.ordinal,
                    containerColor = GVBackground,
                    contentColor = GVRed,
                    edgePadding = 12.dp,
                    divider = {},
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    SortOrder.entries.forEachIndexed { _, order ->
                        Tab(
                            selected = state.sortOrder == order,
                            onClick  = { vm.setSortOrder(order) },
                            text = {
                                Text(
                                    order.label,
                                    fontSize = 13.sp,
                                    color = if (state.sortOrder == order) GVRed else Color(0xFF7090B8),
                                )
                            },
                        )
                    }
                }
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
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 12.dp, end = 36.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(displayedGames, key = { it.path }) { game ->
                            GameRow(
                                game           = game,
                                downloadStatus = state.downloads[game.name],
                                isOnDevice     = game.name in state.localFiles,
                                isQueued       = game.name in state.queuedKeys,
                                onDownload     = { vm.download(context, game, platformId) },
                                onDelete       = { vm.deleteGame(context, game, platformId) },
                                onCancel       = { vm.cancelDownload(game) },
                            )
                        }
                    }

                    // Alphabetical fast-scroll bar (only when not searching)
                    if (state.sortOrder == SortOrder.Alphabetical && searchQuery.isBlank()) {
                        FastScrollBar(
                            games     = displayedGames,
                            listState = listState,
                            modifier  = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FastScrollBar(
    games: List<GameItem>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (games.isEmpty()) return

    val scope = rememberCoroutineScope()

    // Map from first letter -> first index in games list
    val letterIndex = remember(games) {
        val map = LinkedHashMap<Char, Int>()
        games.forEachIndexed { idx, game ->
            val c = (game.meta?.title ?: game.name).firstOrNull()?.uppercaseChar() ?: return@forEachIndexed
            if (c !in map) map[c] = idx
        }
        map
    }
    val letters = letterIndex.keys.toList()
    if (letters.isEmpty()) return

    var barHeightPx by remember { mutableStateOf(1f) }
    var activeIdx   by remember { mutableStateOf(-1) }

    fun scrollToY(y: Float) {
        val idx = (y / barHeightPx * letters.size).toInt().coerceIn(0, letters.size - 1)
        activeIdx = idx
        val target = letterIndex[letters[idx]] ?: 0
        scope.launch { listState.scrollToItem(target) }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .background(Color(0x22000000), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            .onSizeChanged { barHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    scrollToY(down.position.y)
                    do {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            change.consume()
                            scrollToY(change.position.y)
                        }
                    } while (event.changes.any { it.pressed })
                    activeIdx = -1
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { i, letter ->
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = letter.toString(),
                    fontSize   = 9.sp,
                    fontWeight = if (i == activeIdx) FontWeight.Bold else FontWeight.Normal,
                    color      = if (i == activeIdx) GVRed else Color(0xFF7090B8),
                )
            }
        }
    }
}

@Composable
private fun GameRow(
    game: GameItem,
    downloadStatus: DownloadStatus?,
    isOnDevice: Boolean,
    isQueued: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete game?", color = Color.White) },
            text = {
                Text(
                    "This will remove \"${game.meta?.title ?: game.name}\" from your device.",
                    color = Color(0xFFDCE8FF),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = GVRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF7090B8))
                }
            },
            containerColor = GVSurface,
        )
    }

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

        val activeDownload = downloadStatus != null && !downloadStatus.done

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = game.meta?.title ?: game.name,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            if (game.sizeHuman != null) {
                Text(text = game.sizeHuman, color = Color(0xFF7090B8), fontSize = 12.sp)
            }
            when {
                isQueued -> {
                    Text("Queued", color = Color(0xFF7090B8), fontSize = 11.sp)
                }
                activeDownload -> {
                    Spacer(Modifier.height(4.dp))
                    if (downloadStatus!!.progress >= 0) {
                        LinearProgressIndicator(
                            progress   = { downloadStatus.progress / 100f },
                            modifier   = Modifier.fillMaxWidth(),
                            color      = GVRed,
                            trackColor = Color(0xFF1C2A44),
                        )
                        Text(text = "${downloadStatus.progress}%", color = Color(0xFF7090B8), fontSize = 11.sp)
                    } else {
                        LinearProgressIndicator(
                            modifier   = Modifier.fillMaxWidth(),
                            color      = GVRed,
                            trackColor = Color(0xFF1C2A44),
                        )
                    }
                }
                downloadStatus?.done == true && downloadStatus.error != null -> {
                    Text(text = downloadStatus.error, color = GVRed, fontSize = 11.sp)
                }
            }
        }

        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when {
                activeDownload || isQueued -> {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF7090B8))
                    }
                }
                isOnDevice && downloadStatus?.error == null -> {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "On device — tap to delete",
                            tint = Color(0xFF4CAF50),
                        )
                    }
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
