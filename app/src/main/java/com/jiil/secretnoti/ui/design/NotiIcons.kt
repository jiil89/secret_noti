package com.jiil.secretnoti.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object NotiIcons {
    private fun line(name: String, data: String) = ImageVector.Builder(
        name, 24.dp, 24.dp, 24f, 24f,
    ).addPath(PathParser().parsePathString(data).toNodes(),
        stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

    val users = line("Users", "M16 21V19A4 4 0 0 0 12 15H6A4 4 0 0 0 2 19V21M13 7A4 4 0 1 1 5 7A4 4 0 1 1 13 7M22 21V19A4 4 0 0 0 19 15.13M16 3.13A4 4 0 0 1 16 10.87")
    val saveOff = line("SaveOff", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M5.64 5.64L18.36 18.36")
    val lock = line("Lock", "M5 10H19V21H5ZM8 10V7A4 4 0 0 1 16 7V10M12 14V17")
    val pin = line("Pin", "M9 3H15L14 9L18 13V15H6V13L10 9ZM12 15V22")
    val eyeOff = line("EyeOff", "M17.94 17.94A10.07 10.07 0 0 1 12 20C5 20 1 12 1 12A18.45 18.45 0 0 1 6.06 6.06M9.9 4.24A9.12 9.12 0 0 1 12 4C19 4 23 12 23 12A18.5 18.5 0 0 1 20.84 15.19M14.12 14.12A3 3 0 1 1 9.88 9.88M1 1L23 23")
    val chevronLeft = line("ChevronLeft", "M15 18L9 12L15 6")
    val sliders = line("Sliders", "M4 21V14M4 10V3M12 21V12M12 8V3M20 21V16M20 12V3M1 14H7M9 8H15M17 16H23")
    val search = line("Search", "M19 11A8 8 0 1 1 3 11A8 8 0 1 1 19 11M21 21L16.65 16.65")
    val externalLink = line("ExternalLink", "M15 3H21V9M10 14L21 3M21 14V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3H10")
    val info = line("Info", "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12M12 16V12M12 8H12.01")
    val more = line("More", "M12 5H12.01M12 12H12.01M12 19H12.01")
    val tag = line("Tag", "M20.6 13.4L13.4 20.6A2 2 0 0 1 10.6 20.6L2 12V2H12L20.6 10.6A2 2 0 0 1 20.6 13.4ZM7 7H7.01")
    val trash = line("Trash", "M3 6H21M19 6V20A2 2 0 0 1 17 22H7A2 2 0 0 1 5 20V6M8 6V4A2 2 0 0 1 10 2H14A2 2 0 0 1 16 4V6M10 11V17M14 11V17")
    val close = line("Close", "M18 6L6 18M6 6L18 18")
    val message = line("Message", "M21 11.5A8.38 8.38 0 0 1 20.1 15.3A8.5 8.5 0 0 1 12.5 20A8.38 8.38 0 0 1 8.7 19.1L3 21L4.9 15.3A8.38 8.38 0 0 1 4 11.5A8.5 8.5 0 0 1 8.7 3.9A8.38 8.38 0 0 1 12.5 3H13A8.48 8.48 0 0 1 21 11V11.5")
}
