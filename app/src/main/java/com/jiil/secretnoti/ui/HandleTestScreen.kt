package com.jiil.secretnoti.ui

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
            "가장자리 스와이프가 시스템 '뒤로 가기' 제스처와 겹치는지 확인하는 시험입니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!connected) Warning("손잡이는 알림 리스너 안에서 실행돼요. '권한' 탭에서 알림 접근 권한을 먼저 허용하세요.")
        if (!canDraw) Warning("'다른 앱 위에 표시' 권한이 필요해요.")

        PrefSwitch("손잡이 표시", Prefs.overlayEnabled)
        PrefSwitch("시스템 제스처 예외 영역 사용 (Android 10+)", Prefs.gestureExclusion)
        PrefSwitch("왼쪽 가장자리에 두기", Prefs.handleOnLeft)
        Text("세로 위치")
        Slider(value = handleY, onValueChange = { Prefs.handleY.value = it })

        Text("시험 순서", fontWeight = FontWeight.Bold)
        Text(
            "1. 제스처 내비게이션에서 다른 앱(유튜브 등)을 띄우고 손잡이를 안쪽으로 밀기\n" +
                "2. 손잡이를 탭하기\n" +
                "3. '예외 영역'을 끄고 1~2 반복\n" +
                "4. 버튼 내비게이션으로 바꿔서 반복\n" +
                "CANCEL이 찍히면 시스템이 제스처를 가져간 것입니다.",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("제스처 기록", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = GestureLog::clear) { Text("지우기") }
        }
        if (log.isEmpty()) Text("아직 기록이 없어요.", style = MaterialTheme.typography.bodySmall)
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
