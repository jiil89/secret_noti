package com.jiil.secretnoti.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NotiPalette(
    val Text: Color,
    val Secondary: Color,
    val Body: Color,
    val Accent: Color,
    val NoticeBackground: Color,
    val NoticeForeground: Color,
    val Background: Color,
    val DetailBackground: Color,
    val Divider: Color,
    val Outline: Color,
    val Avatar: Color,
    val DeletedBackground: Color,
    val DeletedOutline: Color,
    val OnAccent: Color,
    val Scrim: Color,
)

val NotiLightPalette = NotiPalette(
    Text = Color(0xFF16181D),
    Secondary = Color(0xFF5B6270),
    Body = Color(0xFF3B3F47),
    Accent = Color(0xFF2558E8),
    NoticeBackground = Color(0xFFE8EEFF),
    NoticeForeground = Color(0xFF1F4FD1),
    Background = Color(0xFFFFFFFF),
    DetailBackground = Color(0xFFF4F5F7),
    Divider = Color(0xFFE6E8EC),
    Outline = Color(0xFF747C8A),
    Avatar = Color(0xFFEEF0F3),
    DeletedBackground = Color(0xFFFAFAFB),
    DeletedOutline = Color(0xFF848B98),
    OnAccent = Color(0xFFFFFFFF),
    Scrim = Color(0x800A0C10),
)

val NotiDarkPalette = NotiPalette(
    Text = Color(0xFFF2F3F7),
    Secondary = Color(0xFFB8BFCC),
    Body = Color(0xFFD7DBE4),
    Accent = Color(0xFFA9BFFF),
    NoticeBackground = Color(0xFF25365B),
    NoticeForeground = Color(0xFFC5D4FF),
    Background = Color(0xFF181B22),
    DetailBackground = Color(0xFF101319),
    Divider = Color(0xFF303640),
    Outline = Color(0xFF828C9E),
    Avatar = Color(0xFF2B303A),
    DeletedBackground = Color(0xFF20242D),
    DeletedOutline = Color(0xFF929CAC),
    OnAccent = Color(0xFF10265A),
    Scrim = Color(0xB3000000),
)

val LocalNotiPalette = staticCompositionLocalOf { NotiLightPalette }

object NotiColors {
    val Text: Color @Composable get() = LocalNotiPalette.current.Text
    val Secondary: Color @Composable get() = LocalNotiPalette.current.Secondary
    val Body: Color @Composable get() = LocalNotiPalette.current.Body
    val Accent: Color @Composable get() = LocalNotiPalette.current.Accent
    val NoticeBackground: Color @Composable get() = LocalNotiPalette.current.NoticeBackground
    val NoticeForeground: Color @Composable get() = LocalNotiPalette.current.NoticeForeground
    val Background: Color @Composable get() = LocalNotiPalette.current.Background
    val DetailBackground: Color @Composable get() = LocalNotiPalette.current.DetailBackground
    val Divider: Color @Composable get() = LocalNotiPalette.current.Divider
    val Outline: Color @Composable get() = LocalNotiPalette.current.Outline
    val Avatar: Color @Composable get() = LocalNotiPalette.current.Avatar
    val DeletedBackground: Color @Composable get() = LocalNotiPalette.current.DeletedBackground
    val DeletedOutline: Color @Composable get() = LocalNotiPalette.current.DeletedOutline
    val OnAccent: Color @Composable get() = LocalNotiPalette.current.OnAccent
    val Scrim: Color @Composable get() = LocalNotiPalette.current.Scrim
}

fun notiColorScheme(dark: Boolean): ColorScheme {
    val p = if (dark) NotiDarkPalette else NotiLightPalette
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.Accent, onPrimary = p.OnAccent,
        primaryContainer = p.NoticeBackground, onPrimaryContainer = p.NoticeForeground,
        secondary = p.Accent, onSecondary = p.OnAccent,
        secondaryContainer = p.NoticeBackground, onSecondaryContainer = p.NoticeForeground,
        background = p.Background, onBackground = p.Text,
        surface = p.Background, onSurface = p.Text,
        surfaceVariant = p.DetailBackground, onSurfaceVariant = p.Secondary,
        surfaceDim = p.DetailBackground, surfaceBright = p.Background,
        surfaceContainerLowest = p.DetailBackground, surfaceContainerLow = p.Background,
        surfaceContainer = p.Background, surfaceContainerHigh = p.Avatar,
        surfaceContainerHighest = p.Avatar,
        inverseSurface = p.Text, inverseOnSurface = p.Background,
        outline = p.Outline, outlineVariant = p.Divider, scrim = Color.Black,
    )
}

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
