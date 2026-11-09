package com.jiil.secretnoti.ui

import com.jiil.secretnoti.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.design.NotiIcons

/** 진단 기록: 알림 원본 기록과 내보내기 (테스터 문제 확인용). */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    BackScaffold(stringResource(R.string.route_diagnostics), onBack) { DumpScreen() }
}

/** 뒤로 가기와 제목만 있는 단순한 화면 틀. */
@Composable
fun BackScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.statusBarsPadding().padding(start = 4.dp, top = 4.dp),
            ) {
                IconButton(onClick = onBack) { Icon(NotiIcons.chevronLeft, contentDescription = stringResource(R.string.route_back)) }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}
