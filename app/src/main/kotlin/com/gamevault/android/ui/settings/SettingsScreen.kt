package com.gamevault.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamevault.android.ui.theme.GVBackground
import com.gamevault.android.ui.theme.GVRed
import com.gamevault.android.ui.theme.GVSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load(context) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm.onRomsRootPicked(context, it.toString()) } }

    Scaffold(
        containerColor = GVBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GVBackground),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Server ────────────────────────────────────────────────────────
            SettingSection(title = "Server") {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = vm::onServerUrlChange,
                    label = { Text("Remote URL") },
                    placeholder = { Text("https://yourserver.duckdns.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = gvFieldColors(),
                )
                OutlinedTextField(
                    value = state.localUrl,
                    onValueChange = vm::onLocalUrlChange,
                    label = { Text("Local URL (optional)") },
                    placeholder = { Text("http://10.0.0.10:5000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = gvFieldColors(),
                )
                Text(
                    text = "When on your home network the app will try the local URL first, then fall back to the remote URL automatically.",
                    color = Color(0xFF7090B8),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Button(
                    onClick = { vm.saveUrls(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = GVRed),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("Save") }
            }

            HorizontalDivider(color = Color(0xFF1C2A44))

            // ── ROMs Folder ───────────────────────────────────────────────────
            SettingSection(title = "ROMs Folder") {
                Text(
                    text = if (state.romsRootUri.isBlank()) "No folder selected" else state.romsRootUri,
                    color = if (state.romsRootUri.isBlank()) Color(0xFF7090B8) else Color(0xFFDCE8FF),
                    fontSize = 13.sp,
                    maxLines = 3,
                )
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(GVRed)),
                ) {
                    Icon(Icons.Default.Folder, null, tint = GVRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick ROMs Root Folder", color = GVRed)
                }
                if (state.folderStatus.isNotBlank()) {
                    Text(
                        text = state.folderStatus,
                        color = if (state.folderStatus.startsWith("Could")) GVRed else Color(0xFF22C55E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    text = "Select your Emulation/roms directory. All platform subfolders will be created automatically.",
                    color = Color(0xFF7090B8),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GVSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = Color(0xFF7090B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun gvFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GVRed,
    unfocusedBorderColor = Color(0xFF1C2A44),
    focusedLabelColor = GVRed,
    unfocusedLabelColor = Color(0xFF7090B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFDCE8FF),
    unfocusedContainerColor = Color(0xFF111B35),
    focusedContainerColor = Color(0xFF111B35),
)
