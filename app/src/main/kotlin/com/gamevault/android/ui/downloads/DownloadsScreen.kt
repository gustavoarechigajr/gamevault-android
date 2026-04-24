package com.gamevault.android.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val allDownloads by DownloadRepository.downloads.collectAsState()

    val active    = allDownloads.values.filter { !it.done }.sortedByDescending { it.timestamp }
    val completed = allDownloads.values.filter { it.done }.sortedByDescending { it.timestamp }

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
        if (allDownloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No downloads yet", color = Color(0xFF7090B8), fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active.isNotEmpty()) {
                    item {
                        SectionLabel("In Progress")
                    }
                    items(active, key = { it.key }) { entry ->
                        DownloadRow(entry = entry, onDismiss = null)
                    }
                }
                if (completed.isNotEmpty()) {
                    item {
                        if (active.isNotEmpty()) Spacer(Modifier.height(4.dp))
                        SectionLabel("Completed")
                    }
                    items(completed, key = { it.key }) { entry ->
                        DownloadRow(
                            entry = entry,
                            onDismiss = { DownloadRepository.remove(entry.key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF7090B8),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun DownloadRow(entry: DownloadEntry, onDismiss: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status icon
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when {
                !entry.done -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = GVRed,
                    strokeWidth = 2.5.dp,
                )
                entry.error != null -> Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Failed",
                    tint = GVRed,
                    modifier = Modifier.size(24.dp),
                )
                else -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.gameTitle,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.platformName,
                color = Color(0xFF7090B8),
                fontSize = 12.sp,
            )
            when {
                !entry.done && entry.progress >= 0 -> {
                    LinearProgressIndicator(
                        progress = { entry.progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                    Text("${entry.progress}%", color = Color(0xFF7090B8), fontSize = 11.sp)
                }
                !entry.done -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color = GVRed,
                        trackColor = Color(0xFF1C2A44),
                    )
                }
                entry.error != null -> {
                    Text(entry.error, color = GVRed, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF4A5A7A), modifier = Modifier.size(18.dp))
            }
        }
    }
}
