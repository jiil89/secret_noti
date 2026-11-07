package com.jiil.secretnoti.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.jiil.secretnoti.ui.design.NotiColors
import com.jiil.secretnoti.ui.design.NotiIcons

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val packageManager = LocalContext.current.packageManager
    val pixels = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val icon = remember(packageName, size, pixels, packageManager) {
        try {
            packageManager.getApplicationIcon(packageName).toBitmap(pixels, pixels).asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    Box(modifier.size(size).clip(CircleShape).background(NotiColors.Avatar),
        contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(size))
        } else {
            Icon(NotiIcons.message, contentDescription = null,
                modifier = Modifier.size(size * 0.65f), tint = NotiColors.Secondary)
        }
    }
}