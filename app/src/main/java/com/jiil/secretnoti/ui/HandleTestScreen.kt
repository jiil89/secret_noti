package com.jiil.secretnoti.ui

import com.jiil.secretnoti.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.capture.ListenerStatus
import com.jiil.secretnoti.overlay.GestureLog
import com.jiil.secretnoti.settings.Pref
import com.jiil.secretnoti.settings.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HandleTestScreen() {
    val context = LocalContext.current
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LifecycleResumeEffect(Unit) {
        canDraw = Settings.canDrawOverlays(context)
        onPauseOrDispose { }
    }
    val connected by ListenerStatus.connected.collectAsStateWithLifecycle()
    val handleY by Prefs.handleY.flow.collectAsStateWithLifecycle()
    val log by GestureLog.entries.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.KOREA) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.route_handle_test_help),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!connected) Warning(stringResource(R.string.route_handle_listener_warning))
        if (!canDraw) Warning(stringResource(R.string.route_handle_overlay_warning))

        PrefSwitch(stringResource(R.string.route_show_handle), Prefs.overlayEnabled)
        PrefSwitch(stringResource(R.string.route_gesture_exclusion), Prefs.gestureExclusion)
        PrefSwitch(stringResource(R.string.route_handle_left), Prefs.handleOnLeft)
        Text(stringResource(R.string.route_vertical_position))
        Slider(value = handleY, onValueChange = { Prefs.handleY.value = it })

        Text(stringResource(R.string.route_test_steps), fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.route_handle_test_steps),
            style = MaterialTheme.typography.bodySmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.route_gesture_log), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = GestureLog::clear) { Text(stringResource(R.string.route_clear)) }
        }
        if (log.isEmpty()) Text(stringResource(R.string.route_empty_log), style = MaterialTheme.typography.bodySmall)
        log.forEach { Text("${timeFormat.format(Date(it.at))}  ${it.message}", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PrefSwitch(label: String, pref: Pref<Boolean>) {
    val checked by pref.flow.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { pref.value = it })
    }
}

@Composable
private fun Warning(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
}
