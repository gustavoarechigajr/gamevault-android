package com.gamevault.android.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.gamevault.android.ui.secondscreen.SecondScreenState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    platformId: String,
    onBack: () -> Unit,
    vm: GamesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(platformId) { vm.load(context, platformId) }

    DisposableEffect(Unit) {
        onDispose { SecondScreenState.selectGame(null) }
    }

    LaunchedEffect(state.verifyMessage) {
        val msg = state.verifyMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.clearVerifyMessage()
    }

    LaunchedEffect(searchActive) {
        if (searchActive) try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    val displayedGames = remember(state.games, searchQuery, state.sortOrder, state.localFiles) {
        val filtered = if (searchQuery.isBlank()) state.games
                       else state.games.filter {
                           val title = it.meta?.title ?: it.name
                           title.contains(searchQuery, ignoreCase = true) ||
                               it.name.contains(searchQuery, ignoreCase = true)
                       }
        when (state.sortOrder) {
            SortOrder.Alphabetical -> filtered.sortedBy { (it.meta?.title ?: it.name).lowercase() }
            SortOrder.Size         -> filtered.sortedByDescending { it.size ?: 0L }
            SortOrder.Year         -> filtered.sortedByDescending { it.meta?.year ?: "" }
            SortOrder.Downloaded   -> filtered.sortedWith(
                compareByDescending<GameItem> { if (it.name.lowercase() in state.localFiles) 1 else 0 }
                    .thenBy { (it.meta?.title ?: it.name).lowercase() }
            )
            SortOrder.Rating       -> filtered.sortedByDescending { it.meta?.rating?.toFloatOrNull() ?: 0f }
        }
    }

    val groupedSearchResults = remember(displayedGames, searchQuery) {
        if (searchQuery.isBlank()) emptyMap()
        else displayedGames.groupBy { game ->
            val first = (game.meta?.title ?: game.name).firstOrNull()?.uppercaseChar()
            if (first?.isLetter() == true) first.toString() else "#"
        }
    }

    // Index of list positions where a new leading character starts — used for L/R letter jumping
    val letterBoundaries = remember(displayedGames) {
        val list = mutableListOf<Int>()
        var lastChar: Char? = null
        displayedGames.forEachIndexed { idx, game ->
            val c = (game.meta?.title ?: game.name).firstOrNull()?.uppercaseChar()
            val norm = if (c?.isLetter() == true) c else '#'
            if (norm != lastChar) { list.add(idx); lastChar = norm }
        }
        list
    }

    fun jumpRight() {
        val first = listState.firstVisibleItemIndex
        letterBoundaries.firstOrNull { it > first }?.let {
            scope.launch { listState.animateScrollToItem(it) }
        }
    }

    fun jumpLeft() {
        val first = listState.firstVisibleItemIndex
        val idx = letterBoundaries.indexOfLast { it < first }
        scope.launch { listState.animateScrollToItem(if (idx >= 0) letterBoundaries[idx] else 0) }
    }

    Scaffold(
        containerColor = GVBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                // Search: fake focusable row when idle (D-pad safe), real TextField when active
                if (searchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search ${state.platformName}…", color = Color(0xFF7090B8)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7090B8)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color(0xFF7090B8))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            searchActive = false
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .focusRequester(searchFocusRequester)
                            .onFocusChanged { if (!it.isFocused) keyboardController?.hide() }
                            .onKeyEvent { ev ->
                                if (ev.type == KeyEventType.KeyDown && (
                                    ev.key == Key.Escape ||
                                    ev.key == Key.Back ||
                                    ev.key == Key.DirectionDown ||
                                    ev.key == Key.DirectionUp
                                )) {
                                    searchActive = false
                                    focusManager.clearFocus()
                                    true
                                } else false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = GVRed,
                            unfocusedBorderColor    = Color(0xFF1C2A44),
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color(0xFFDCE8FF),
                            unfocusedContainerColor = GVSurface,
                            focusedContainerColor   = GVSurface,
                        ),
                    )
                } else {
                    val fakeInteraction = remember { MutableInteractionSource() }
                    val fakeFocused by fakeInteraction.collectIsFocusedAsState()
                    val fakeBorder by animateColorAsState(
                        targetValue = if (fakeFocused) GVRed.copy(alpha = 0.7f) else Color(0xFF1C2A44),
                        animationSpec = tween(150), label = "searchBorder",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GVSurface)
                            .border(1.5.dp, fakeBorder, RoundedCornerShape(12.dp))
                            .focusable(interactionSource = fakeInteraction)
                            .clickable { searchActive = true }
                            .onKeyEvent { ev ->
                                if (ev.type == KeyEventType.KeyDown &&
                                    (ev.key == Key.Enter || ev.key == Key.ButtonA)) {
                                    searchActive = true; true
                                } else false
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF7090B8), modifier = Modifier.size(20.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Search ${state.platformName}…" else searchQuery,
                            color = if (searchQuery.isBlank()) Color(0xFF7090B8) else Color(0xFFDCE8FF),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                Icons.Default.Close, "Clear",
                                tint = Color(0xFF7090B8),
                                modifier = Modifier.size(16.dp).clickable { searchQuery = "" },
                            )
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = state.sortOrder.ordinal,
                    containerColor = GVBackground,
                    contentColor = GVRed,
                    edgePadding = 12.dp,
                    divider = {},
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    SortOrder.entries.forEach { order ->
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
        },
    ) { padding ->
        // onPreviewKeyEvent fires BEFORE the focus system processes the event, so
        // we can intercept directional keys without the focus traversal stealing them.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (ev.key) {
                        Key.ButtonX -> {
                            if (!searchActive) { searchActive = true; true } else false
                        }
                        Key.ButtonY -> {
                            val game = SecondScreenState.selectedGame.value ?: return@onPreviewKeyEvent false
                            val isOnDevice    = game.name.lowercase() in state.localFiles
                            val activeDownload = state.downloads[game.name]?.done == false
                            val isQueued      = game.name in state.queuedKeys
                            if (!isOnDevice && !activeDownload && !isQueued) {
                                vm.download(context, game, platformId)
                            }
                            true
                        }
                        Key.DirectionLeft  -> { jumpLeft();  true }
                        Key.DirectionRight -> { jumpRight(); true }
                        else -> false
                    }
                },
        ) {
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
                    LazyColumn(
                        state = listState,
                        // Extra bottom padding so last item isn't hidden behind the hint bar
                        contentPadding = PaddingValues(start = 12.dp, end = 36.dp, top = 12.dp, bottom = 56.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (searchQuery.isNotBlank() && groupedSearchResults.isNotEmpty()) {
                            groupedSearchResults.entries.sortedBy { it.key }.forEach { (letter, games) ->
                                stickyHeader(key = "alpha:$letter") {
                                    Text(
                                        text = letter,
                                        color = GVRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(GVBackground)
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                    )
                                }
                                items(games, key = { "s:${it.path}" }) { game ->
                                    GameRow(
                                        game           = game,
                                        downloadStatus = state.downloads[game.name],
                                        isOnDevice     = game.name.lowercase() in state.localFiles,
                                        isQueued       = game.name in state.queuedKeys,
                                        onDownload     = { vm.download(context, game, platformId) },
                                        onDelete       = { vm.deleteGame(context, game, platformId) },
                                        onCancel       = { vm.cancelDownload(game) },
                                        onVerify       = { vm.verifyFile(context, game, platformId) },
                                        onSelect       = { SecondScreenState.selectGame(game) },
                                    )
                                }
                            }
                        } else {
                            items(displayedGames, key = { it.path }) { game ->
                                GameRow(
                                    game           = game,
                                    downloadStatus = state.downloads[game.name],
                                    isOnDevice     = game.name.lowercase() in state.localFiles,
                                    isQueued       = game.name in state.queuedKeys,
                                    onDownload     = { vm.download(context, game, platformId) },
                                    onDelete       = { vm.deleteGame(context, game, platformId) },
                                    onCancel       = { vm.cancelDownload(game) },
                                    onVerify       = { vm.verifyFile(context, game, platformId) },
                                    onSelect       = { SecondScreenState.selectGame(game) },
                                )
                            }
                        }
                    }

                    if (searchQuery.isBlank()) {
                        FastScrollBar(
                            games     = displayedGames,
                            listState = listState,
                            modifier  = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }

            // Hint bar overlaid at the bottom — list has extra padding so it isn't hidden
            ControllerHintBar(modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun ControllerHintBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xEE0A1020))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControllerHint("A", "Select")
        ControllerHint("B", "Back")
        ControllerHint("X", "Search")
        ControllerHint("Y", "Download")
        Spacer(Modifier.weight(1f))
        ControllerHint("◀▶", "Jump letter")
    }
}

@Composable
private fun ControllerHint(button: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C2A44))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(button, color = GVRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Color(0xFF7090B8), fontSize = 11.sp)
    }
}

@Composable
private fun FastScrollBar(
    games: List<GameItem>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (games.isEmpty()) return

    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    // Normalize non-letter first chars (numbers, symbols) to '#' so the bar never
    // exceeds 27 slots regardless of how many numbered titles a library has.
    val letterIndex = remember(games) {
        val map = LinkedHashMap<Char, Int>()
        games.forEachIndexed { idx, game ->
            val raw = (game.meta?.title ?: game.name).firstOrNull()?.uppercaseChar()
            val c   = if (raw?.isLetter() == true) raw else '#'
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

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
                .background(Color(0x55FFFFFF), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
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
                    val isActive = i == activeIdx
                    Text(
                        text       = letter.toString(),
                        fontSize   = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isActive) GVRed else Color(0xFF7090B8),
                    )
                }
            }
        }

        // Floating letter bubble shown while dragging — appears to the left of the bar
        if (activeIdx in letters.indices) {
            val yOffsetDp = with(density) {
                ((activeIdx + 0.5f) / letters.size * barHeightPx).toDp() - 16.dp
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .absoluteOffset(x = (-36).dp, y = yOffsetDp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GVRed)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = letters[activeIdx].toString(),
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
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
    onVerify: () -> Unit,
    onSelect: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) GVRed.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(150),
        label = "focusBorder",
    )

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
                }) { Text("Delete", color = GVRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF7090B8))
                }
            },
            containerColor = GVSurface,
        )
    }

    val activeDownload = downloadStatus != null && !downloadStatus.done

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GVSurface)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .onFocusChanged { if (it.hasFocus) onSelect() }
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onSelect)
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
                isQueued -> Text("Queued", color = Color(0xFF7090B8), fontSize = 11.sp)
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
                downloadStatus?.done == true && downloadStatus.error != null ->
                    Text(text = downloadStatus.error, color = GVRed, fontSize = 11.sp)
            }
        }

        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when {
                activeDownload || isQueued -> {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel", tint = Color(0xFF7090B8))
                    }
                }
                isOnDevice && downloadStatus?.error == null -> {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.CheckCircle, "On device", tint = Color(0xFF4CAF50))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = GVSurface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Verify file", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.VerifiedUser, null, tint = Color(0xFF7090B8))
                                },
                                onClick = { showMenu = false; onVerify() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete game", color = GVRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = GVRed) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                            )
                        }
                    }
                }
                downloadStatus?.done == true && downloadStatus.error != null -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.ErrorOutline, "Retry", tint = GVRed)
                    }
                }
                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, "Download", tint = GVRed)
                    }
                }
            }
        }
    }
}
