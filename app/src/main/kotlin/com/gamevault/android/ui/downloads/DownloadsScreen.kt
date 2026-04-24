package com.gamevault.android.ui.downloads

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamevault.android.data.DownloadEntry
import com.gamevault.android.data.DownloadRepository
import com.gamevault.android.data.QueuedDownload
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val allDownloads by DownloadRepository.downloads.collectAsState()
    val queue        by DownloadRepository.queue.collectAsState()

    val active    = allDownloads.values.filter { !it.done }.sortedByDescending { it.timestamp }
    val completed = allDownloads.values.filter { it.done }.sortedByDescending { it.timestamp }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey   = to.key   as? String ?: return@rememberReorderableLazyListState
        if (fromKey.startsWith("q:") && toKey.startsWith("q:")) {
            val fromGameKey = fromKey.removePrefix("q:")
            val toGameKey   = toKey.removePrefix("q:")
            val fromIdx = queue.indexOfFirst { it.key == fromGameKey }
            val toIdx   = queue.indexOfFirst { it.key == toGameKey }
            if (fromIdx != -1 && toIdx != -1) {
                DownloadRepository.moveInQueue(fromIdx, toIdx)
            }
        }
    }

    Scaffold(
        containerColor = GVBackground,
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (completed.isNotEmpty()) {
                        TextButton(onClick = { DownloadRepository.clearCompleted() }) {
                            Text("Clear completed", color = Color(0xFF7090B8), fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GVBackground),
            )
        }
    ) { padding ->
        if (allDownloads.isEmpty() && queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No downloads yet", color = Color(0xFF7090B8), fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active.isNotEmpty()) {
                    item(key = "header:active") { SectionLabel("In Progress") }
                    items(active, key = { "a:${it.key}" }) { entry ->
                        ActiveRow(entry)
                    }
                }

                if (queue.isNotEmpty()) {
                    item(key = "header:queue") { SectionLabel("Queue") }
                    items(queue, key = { "q:${it.key}" }) { item ->
                        ReorderableItem(reorderableState, key = "q:${item.key}") { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
                            QueueRow(
                                item      = item,
                                elevation = elevation,
                                dragHandle = {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Reorder",
                                        tint     = Color(0xFF4A5A7A),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .draggableHandle(),
                                    )
                                },
                                onForceStart = { DownloadRepository.forceStartQueued(item.key) },
                                onCancel     = { DownloadRepository.cancelDownload(item.key) },
                            )
                        }
                    }
                }

                if (completed.isNotEmpty()) {
                    item(key = "header:completed") {
                        if (active.isNotEmpty() || queue.isNotEmpty()) Spacer(Modifier.height(4.dp))
                        SectionLabel("Completed")
                    }
                    items(completed, key = { "c:${it.key}" }) { entry ->
                        CompletedRow(entry, onDismiss = { DownloadRepository.remove(entry.key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        color      = Color(0xFF7090B8),
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun ActiveRow(entry: DownloadEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier    = Modifier.size(24.dp),
            color       = GVRed,
            strokeWidth = 2.5.dp,
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text       = entry.gameTitle,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(text = entry.platformName, color = Color(0xFF7090B8), fontSize = 12.sp)
            when {
                entry.progress >= 0 -> {
                    LinearProgressIndicator(
                        progress   = { entry.progress / 100f },
                        modifier   = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color      = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                    Text("${entry.progress}%", color = Color(0xFF7090B8), fontSize = 11.sp)
                }
                else -> {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color      = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                }
            }
        }

        IconButton(
            onClick  = { DownloadRepository.cancelDownload(entry.key) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancel",
                tint     = Color(0xFF4A5A7A),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun QueueRow(
    item: QueuedDownload,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandle: @Composable () -> Unit,
    onForceStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val gameTitle = item.game.meta?.title ?: item.game.name

    Surface(
        shape     = RoundedCornerShape(12.dp),
        color     = GVSurface,
        shadowElevation = elevation,
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier  = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            dragHandle()

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = gameTitle,
                    color      = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(text = item.platformName, color = Color(0xFF7090B8), fontSize = 12.sp)
            }

            // Force-start: bypass queue and download immediately
            IconButton(onClick = onForceStart, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Force start",
                    tint     = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint     = Color(0xFF4A5A7A),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CompletedRow(entry: DownloadEntry, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            if (entry.error != null) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Failed",
                    tint     = GVRed,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint     = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text       = entry.gameTitle,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(text = entry.platformName, color = Color(0xFF7090B8), fontSize = 12.sp)
            if (entry.error != null) {
                Text(entry.error, color = GVRed, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint     = Color(0xFF4A5A7A),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
