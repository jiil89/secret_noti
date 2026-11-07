package com.jiil.secretnoti.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jiil.secretnoti.ui.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun matchRanges(text: String, terms: List<String>): List<IntRange> = buildList {
    terms.filter { it.isNotBlank() }.distinct().forEach { term ->
        var start = text.indexOf(term, ignoreCase = true)
        while (start >= 0) {
            add(start until start + term.length)
            start = text.indexOf(term, start + 1, ignoreCase = true)
        }
    }
}

@Composable
fun highlightedText(text: String, terms: List<String>): AnnotatedString {
    val foreground = NotiColors.NoticeForeground
    val background = NotiColors.NoticeBackground
    return remember(text, terms, foreground, background) {
        buildAnnotatedString {
            append(text)
            matchRanges(text, terms).forEach { range ->
                addStyle(SpanStyle(color = foreground, background = background, fontWeight = FontWeight.Bold),
                    range.first, range.last + 1)
            }
        }
    }
}

private data class PhotoState(val bitmap: ImageBitmap? = null, val loading: Boolean = true)

@Composable
private fun filePhoto(path: String, maxPixels: Int): PhotoState {
    val state by produceState(PhotoState(), path, maxPixels) {
        value = PhotoState()
        value = withContext(Dispatchers.IO) {
            val bitmap = try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxPixels) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
            PhotoState(bitmap, loading = false)
        }
    }
    return state
}

@Composable
fun MessagePhoto(path: String) {
    val pixels = with(LocalDensity.current) { 220.dp.roundToPx() }.coerceIn(1, 1024)
    val photo = filePhoto(path, pixels)
    var expanded by remember(path) { mutableStateOf(false) }
    val bitmap = photo.bitmap
    if (bitmap != null) {
        Image(bitmap, "사진 크게 보기", Modifier.width(220.dp).heightIn(max = 220.dp)
            .aspectRatio((bitmap.width.toFloat() / bitmap.height).coerceIn(0.5f, 2f))
            .clip(NotiShapes.banner).clickable { expanded = true }, contentScale = ContentScale.Fit)
    } else {
        Text(if (photo.loading) "사진 불러오는 중" else "사진을 불러올 수 없어요",
            Modifier.padding(vertical = 16.dp), color = NotiColors.Secondary,
            style = MaterialTheme.typography.bodyMedium)
    }
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            val full = filePhoto(path, 2048)
            Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
                val display = full.bitmap ?: bitmap
                if (display != null) Image(display, "저장된 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { expanded = false }, modifier = Modifier.align(Alignment.TopEnd)
                    .padding(8.dp).background(Color.Black, NotiShapes.pill)) {
                    Icon(NotiIcons.close, "닫기", tint = Color.White)
                }
            }
        }
    }
}
