package com.jiil.secretnoti.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.design.NotiIcons

private val tabs = listOf("권한", "알림 덤프", "손잡이 시험")

/** 진단 도구: 권한 상태·알림 덤프·손잡이 제스처 기록. */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    IconButton(onClick = onBack) { Icon(NotiIcons.chevronLeft, contentDescription = "뒤로") }
                    Text("진단 도구", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                PrimaryTabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> PermissionsScreen()
                1 -> DumpScreen()
                else -> HandleTestScreen()
            }
        }
    }
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
                IconButton(onClick = onBack) { Icon(NotiIcons.chevronLeft, contentDescription = "뒤로") }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}
