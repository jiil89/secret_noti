package com.jiil.secretnoti.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 명세의 디자인 토큰. 정식 테마·컴포넌트는 M3에서 다듬는다.
private val NotiColors = lightColorScheme(
    primary = Color(0xFF2558E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEFF),
    onPrimaryContainer = Color(0xFF1F4FD1),
    background = Color.White,
    onBackground = Color(0xFF16181D),
    surface = Color.White,
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF4F5F7),
    onSurfaceVariant = Color(0xFF5B6270),
    outline = Color(0xFFD5D8DE),
    outlineVariant = Color(0xFFE6E8EC),
)

@Composable
fun NotiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NotiColors, content = content)
}
