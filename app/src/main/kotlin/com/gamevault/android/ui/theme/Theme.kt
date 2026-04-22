package com.gamevault.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GVRed = Color(0xFFE4000F)
val GVBackground = Color(0xFF06091A)
val GVSurface = Color(0xFF0C1228)

private val DarkColorScheme = darkColorScheme(
    primary = GVRed,
    background = GVBackground,
    surface = GVSurface,
    onPrimary = Color.White,
    onBackground = Color(0xFFDCE8FF),
    onSurface = Color(0xFFDCE8FF),
)

@Composable
fun GameVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
