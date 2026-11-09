package com.jiil.secretnoti.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.R

@Composable
fun PeekBubble(title: String, text: String, packageName: String, onLeftSide: Boolean, onClick: () -> Unit) {
    val density = LocalDensity.current
    val pointer = with(density) { 6.dp.toPx() }
    val radius = with(density) { 12.dp.toPx() }
    val shape = remember(onLeftSide, pointer, radius) {
        GenericShape { size, _ ->
            val left = if (onLeftSide) pointer else 0f
            val right = if (onLeftSide) size.width else size.width - pointer
            val r = minOf(radius, size.height / 2, (right - left) / 2)
            moveTo(left + r, 0f)
            lineTo(right - r, 0f)
            quadraticBezierTo(right, 0f, right, r)
            if (!onLeftSide) {
                lineTo(right, size.height / 2 - pointer)
                lineTo(size.width, size.height / 2)
                lineTo(right, size.height / 2 + pointer)
            }
            lineTo(right, size.height - r)
            quadraticBezierTo(right, size.height, right - r, size.height)
            lineTo(left + r, size.height)
            quadraticBezierTo(left, size.height, left, size.height - r)
            if (onLeftSide) {
                lineTo(left, size.height / 2 + pointer)
                lineTo(0f, size.height / 2)
                lineTo(left, size.height / 2 - pointer)
            }
            lineTo(left, r)
            quadraticBezierTo(left, 0f, left + r, 0f)
            close()
        }
    }
    val inspection = LocalInspectionMode.current
    val appear = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(180)) }
    val description = stringResource(R.string.peek_bubble_open_panel, title, text)
    Row(
        Modifier.widthIn(max = 260.dp).padding(6.dp)
            .graphicsLayer {
                alpha = appear.value
                translationX = (1f - appear.value) * pointer * if (onLeftSide) -1f else 1f
            }
            .shadow(6.dp, shape).clip(shape).background(Color(0xFF16181D))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .absolutePadding(left = if (onLeftSide) 16.dp else 10.dp,
                right = if (onLeftSide) 10.dp else 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(packageName, 22.dp)
        Column(Modifier.weight(1f, fill = false).clearAndSetSemantics {}) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
