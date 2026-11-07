package com.jiil.secretnoti.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jiil.secretnoti.ui.design.NotiColorScheme
import com.jiil.secretnoti.ui.design.NotiShapes
import com.jiil.secretnoti.ui.design.NotiTypography

@Composable
fun NotiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NotiColorScheme, typography = NotiTypography,
        shapes = NotiShapes.material, content = content)
}
