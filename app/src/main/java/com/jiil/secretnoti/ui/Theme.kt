package com.jiil.secretnoti.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jiil.secretnoti.ui.design.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.jiil.secretnoti.ui.model.PreviewLines
import com.jiil.secretnoti.ui.model.TextSize
import com.jiil.secretnoti.ui.model.ThemeMode

@Composable
fun NotiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textSize: TextSize = TextSize.NORMAL,
    previewLines: PreviewLines = PreviewLines.AUTO,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // 글자 크기는 휴대폰 설정(fontScale)에 곱한다. 아이콘·버튼(dp)은 그대로.
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalNotiPalette provides if (dark) NotiDarkPalette else NotiLightPalette,
        LocalDensity provides Density(density.density, density.fontScale * textSize.scale),
        LocalPreviewLines provides previewLines.lines,
    ) {
        MaterialTheme(colorScheme = notiColorScheme(dark), typography = NotiTypography,
            shapes = NotiShapes.material, content = content)
    }
}
