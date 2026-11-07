package com.jiil.secretnoti.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NotiColors {
    val Text = Color(0xFF16181D)
    val Secondary = Color(0xFF5B6270)
    val Body = Color(0xFF3B3F47)
    val Accent = Color(0xFF2558E8)
    val NoticeBackground = Color(0xFFE8EEFF)
    val NoticeForeground = Color(0xFF1F4FD1)
    val Background = Color.White
    val DetailBackground = Color(0xFFF4F5F7)
    val Divider = Color(0xFFE6E8EC)
    val Outline = Color(0xFFD5D8DE)
    val Avatar = Color(0xFFEEF0F3)
    val DeletedBackground = Color(0xFFFAFAFB)
    val DeletedOutline = Color(0xFFB8BDC6)
}

val NotiColorScheme = lightColorScheme(
    primary = NotiColors.Accent, onPrimary = Color.White,
    primaryContainer = NotiColors.NoticeBackground, onPrimaryContainer = NotiColors.NoticeForeground,
    background = NotiColors.Background, onBackground = NotiColors.Text,
    surface = NotiColors.Background, onSurface = NotiColors.Text,
    surfaceVariant = NotiColors.DetailBackground, onSurfaceVariant = NotiColors.Secondary,
    outline = NotiColors.Outline, outlineVariant = NotiColors.Divider,
)

object NotiShapes {
    fun panel(onLeftSide: Boolean) = if (onLeftSide)
        RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    else RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    val bubble = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    val mine = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    val pill = RoundedCornerShape(50)
    val banner = RoundedCornerShape(12.dp)
    val material = Shapes(small = RoundedCornerShape(10.dp), medium = banner,
        large = RoundedCornerShape(18.dp), extraLarge = RoundedCornerShape(28.dp))
}

object NotiSpacing {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val content = 16.dp
    val panel = 20.dp
    val button = 48.dp
    val panelWidth = 316.dp
}

// Replace this family with bundled Pretendard Font resources in one place.
private val NotiFontFamily = FontFamily.SansSerif
private fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = NotiFontFamily, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight)

val NotiTypography = Typography(
    displayLarge = style(57, 64), displayMedium = style(45, 52), displaySmall = style(36, 44),
    headlineLarge = style(32, 40), headlineMedium = style(28, 36), headlineSmall = style(24, 32),
    titleLarge = style(20, 28, FontWeight.Bold), titleMedium = style(17, 24, FontWeight.Bold),
    titleSmall = style(15, 22, FontWeight.Bold),
    bodyLarge = style(15, 22), bodyMedium = style(13, 19), bodySmall = style(12, 18),
    labelLarge = style(15, 22, FontWeight.Medium),
    labelMedium = style(12, 18, FontWeight.Medium), labelSmall = style(11, 16),
)
