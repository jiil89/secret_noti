package com.jiil.secretnoti.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.R
import kotlin.math.roundToInt

private val glowPresets = listOf(
    0xFF2558E8.toInt() to R.string.glow_color_blue,
    0xFF7C4DFF.toInt() to R.string.glow_color_purple,
    0xFFE91E63.toInt() to R.string.glow_color_pink,
    0xFFF44336.toInt() to R.string.glow_color_red,
    0xFFFF9800.toInt() to R.string.glow_color_orange,
    0xFF22C55E.toInt() to R.string.glow_color_green,
    0xFF14B8A6.toInt() to R.string.glow_color_teal,
    0xFFFFFFFF.toInt() to R.string.glow_color_white,
)

internal fun parseGlowHex(value: String): Int? =
    if (value.matches(Regex("#[0-9a-fA-F]{6}"))) {
        0xFF000000.toInt() or value.substring(1).toInt(16)
    } else null

private fun glowHex(color: Int) = "#" + (color and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

@Composable
fun GlowColorPicker(argb: Int, onColorChange: (Int) -> Unit) {
    val opaqueColor = argb or 0xFF000000.toInt()
    var hex by remember(opaqueColor) { mutableStateOf(glowHex(opaqueColor)) }
    val ringColor = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            glowPresets.forEach { (preset, nameRes) ->
                val name = stringResource(nameRes)
                val selected = opaqueColor == preset
                Canvas(Modifier.size(40.dp).selectable(selected, role = Role.RadioButton,
                    onClick = { hex = glowHex(preset); onColorChange(preset) })
                    .semantics { contentDescription = name }) {
                    val color = Color(preset)
                    drawCircle(color, 14.dp.toPx())
                    drawCircle(ringColor.copy(alpha = 0.35f), 14.dp.toPx(), style = Stroke(1.dp.toPx()))
                    if (selected) {
                        drawCircle(ringColor, 18.dp.toPx(), style = Stroke(2.dp.toPx()))
                        val ink = if (color.luminance() > 0.45f) Color.Black else Color.White
                        drawLine(ink, center + Offset(-6.dp.toPx(), 0f),
                            center + Offset(-1.dp.toPx(), 5.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(ink, center + Offset(-1.dp.toPx(), 5.dp.toPx()),
                            center + Offset(7.dp.toPx(), -5.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }
        listOf(R.string.glow_channel_red to 16, R.string.glow_channel_green to 8,
            R.string.glow_channel_blue to 0).forEach { (labelRes, shift) ->
            val channel = (opaqueColor ushr shift) and 255
            val label = stringResource(labelRes, channel)
            Text(label, style = MaterialTheme.typography.labelLarge)
            Slider(value = channel.toFloat(), valueRange = 0f..255f, steps = 254,
                onValueChange = { value ->
                    val next = (opaqueColor and (255 shl shift).inv()) or
                        (value.roundToInt().coerceIn(0, 255) shl shift) or 0xFF000000.toInt()
                    hex = glowHex(next)
                    onColorChange(next)
                }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label })
        }
        val invalid = parseGlowHex(hex) == null
        OutlinedTextField(value = hex, onValueChange = { value ->
            hex = value
            parseGlowHex(value)?.let(onColorChange)
        }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.glow_hex_label)) },
            isError = invalid,
            supportingText = { if (invalid) Text(stringResource(R.string.glow_hex_error)) })
        val description = stringResource(R.string.glow_live_preview, glowHex(opaqueColor))
        Canvas(Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16181D)).semantics { contentDescription = description }) {
            val color = Color(opaqueColor)
            val width = 6.dp.toPx()
            val height = 56.dp.toPx()
            val origin = Offset((size.width - width) / 2, (size.height - height) / 2)
            for (spread in 12 downTo 1) {
                val halo = spread.dp.toPx()
                drawRoundRect(color.copy(alpha = 0.025f), origin - Offset(halo, halo),
                    Size(width + halo * 2, height + halo * 2), CornerRadius(width + halo))
            }
            drawRoundRect(color, origin, Size(width, height), CornerRadius(width / 2))
        }
    }
}

