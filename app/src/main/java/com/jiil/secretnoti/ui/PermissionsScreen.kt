package com.jiil.secretnoti.ui

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
                title = "1. 알림 접근 권한",
                body = "카카오톡·문자·Teams 알림을 읽어서 이 폰 안에만 저장합니다.",
                granted = state.listener,
                note = when {
                    !state.listener ->
                        "스위치가 회색으로 눌리지 않으면: '앱 정보' → 오른쪽 위 ⋮ → '제한된 설정 허용'을 누른 뒤 다시 시도하세요."
                    connected -> "리스너 연결됨"
                    else -> "권한은 있지만 리스너가 아직 연결되지 않았어요. 잠시 후 다시 확인하세요."
                },
                actions = listOf(
                    "설정 열기" to { context.openListenerSettings() },
                    "앱 정보" to { context.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) },
                ),
            )
        }
        item {
            PermissionCard(
                title = "2. 다른 앱 위에 표시",
                body = "화면 가장자리 손잡이와 엣지패널을 띄우는 데 필요합니다.",
                granted = state.overlay,
                actions = listOf(
                    "설정 열기" to { context.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)) },
                ),
            )
        }
        item {
            PermissionCard(
                title = "3. 배터리 최적화 제외",
                body = "절전 기능이 알림 수집을 멈추지 않게 합니다.",
                granted = state.battery,
                note = "갤럭시는 추가로: 설정 → 배터리 → 백그라운드 사용 한도 → '절전 예외 앱'에 시크릿노티를 넣어 주세요.",
                actions = listOf(
                    "제외 요청" to {
                        context.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
                    },
                ),
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("대상 앱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("어떤 앱의 알림을 저장할지는 설정 → 대상 앱에서 고를 수 있어요.")
                    Text(
                        "카카오톡 설정 → 알림에서 '메시지 미리보기'가 켜져 있어야 내용이 저장됩니다. " +
                            "알림을 끈 채팅방, PC 사용 중 알림이 꺼지는 경우는 저장되지 않아요.",
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
                    if (granted) "허용됨" else "필요",
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
