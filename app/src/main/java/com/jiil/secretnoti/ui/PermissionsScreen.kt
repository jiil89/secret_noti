package com.jiil.secretnoti.ui

import com.jiil.secretnoti.R
import androidx.compose.ui.res.stringResource
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.capture.ListenerStatus
import com.jiil.secretnoti.capture.NotiListenerService
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.PermissionSummaryUi

private data class PermissionState(
    val listener: Boolean,
    val overlay: Boolean,
    val battery: Boolean,
    val kakaoInstalled: Boolean,
    val teamsInstalled: Boolean,
    val smsPackage: String?,
) {
    companion object {
        fun read(context: Context): PermissionState {
            val pm = context.getSystemService(PowerManager::class.java)
            return PermissionState(
                listener = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
                overlay = Settings.canDrawOverlays(context),
                battery = pm.isIgnoringBatteryOptimizations(context.packageName),
                kakaoInstalled = context.isInstalled(TargetApps.KAKAO),
                teamsInstalled = context.isInstalled(TargetApps.TEAMS),
                smsPackage = TargetApps.defaultSmsPackage(context),
            )
        }
    }
}

fun readPermissionSummary(context: Context): PermissionSummaryUi =
    PermissionState.read(context).let { PermissionSummaryUi(it.listener, it.overlay, it.battery) }

internal fun Context.isPackageInstalled(pkg: String) = isInstalled(pkg)

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleResumeEffect(Unit) {
        state = PermissionState.read(context)
        onPauseOrDispose { }
    }
    val connected by ListenerStatus.connected.collectAsState()
    val pkgUri = Uri.parse("package:${context.packageName}")

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PermissionCard(
                title = stringResource(R.string.route_permission_listener_title),
                body = stringResource(R.string.route_permission_listener_body),
                granted = state.listener,
                note = when {
                    !state.listener ->
                        stringResource(R.string.route_permission_restricted)
                    connected -> stringResource(R.string.route_listener_connected)
                    else -> stringResource(R.string.route_listener_waiting)
                },
                actions = listOf(
                    stringResource(R.string.route_open_settings) to { context.openListenerSettings() },
                    stringResource(R.string.route_app_info) to { context.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) },
                ),
            )
        }
        item {
            PermissionCard(
                title = stringResource(R.string.route_permission_overlay_title),
                body = stringResource(R.string.route_permission_overlay_body),
                granted = state.overlay,
                actions = listOf(
                    stringResource(R.string.route_open_settings) to { context.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)) },
                ),
            )
        }
        item {
            PermissionCard(
                title = stringResource(R.string.route_permission_battery_title),
                body = stringResource(R.string.route_permission_battery_body),
                granted = state.battery,
                note = stringResource(R.string.route_permission_battery_note),
                actions = listOf(
                    stringResource(R.string.route_request_exemption) to {
                        context.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
                    },
                ),
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.route_target_apps), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.route_target_apps_help))
                    Text(
                        stringResource(R.string.route_kakao_preview_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    note: String? = null,
    actions: List<Pair<String, () -> Unit>>,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (granted) stringResource(R.string.route_permission_granted) else stringResource(R.string.route_permission_required),
                    color = if (granted) Color(0xFF1B7F3B) else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEachIndexed { i, (label, onClick) ->
                    if (i == 0) FilledTonalButton(onClick = onClick) { Text(label) }
                    else OutlinedButton(onClick = onClick) { Text(label) }
                }
            }
        }
    }
}

private fun Context.isInstalled(pkg: String): Boolean = try {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(pkg, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

private fun Context.launch(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent)
    } catch (_: Exception) {
        fallback?.let { runCatching { startActivity(it) } }
    }
}

private fun Context.openListenerSettings() {
    val general = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(this, NotiListenerService::class.java).flattenToString(),
        )
        launch(detail, fallback = general)
    } else {
        launch(general)
    }
}
