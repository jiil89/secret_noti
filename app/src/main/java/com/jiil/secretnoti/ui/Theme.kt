package com.jiil.secretnoti.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.ThemeMode

@Composable
fun NotiTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalNotiPalette provides if (dark) NotiDarkPalette else NotiLightPalette) {
        MaterialTheme(colorScheme = notiColorScheme(dark), typography = NotiTypography,
            shapes = NotiShapes.material, content = content)
    }
}
