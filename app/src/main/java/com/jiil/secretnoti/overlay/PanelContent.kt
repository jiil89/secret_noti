package com.jiil.secretnoti.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.capture.TargetApps

/** M1 시험용 엣지패널. 최근 저장된 알림 10개를 그대로 보여준다. 정식 디자인은 M4. */
@Composable
fun PanelContent(onLeft: Boolean, onClose: () -> Unit) {
    val dao = remember { NotiApp.instance.db.dumpDao() }
    val items by remember { dao.observeRecentPosted(10) }.collectAsState(initial = emptyList())
    val shape = if (onLeft) {
        RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    } else {
        RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x800A0C10))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(if (onLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(vertical = 96.dp)
                .width(316.dp)
                .fillMaxHeight()
                // 카드 안을 눌렀을 때는 닫히지 않도록 탭을 소비한다.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("최근 메시지", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "M1 시험용 패널 · 바깥을 누르거나 뒤로 가기로 닫기",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (items.isEmpty()) {
                    Text("아직 저장된 알림이 없어요.", style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { dump ->
                        Column {
                            Text(
                                dump.title ?: "(제목 없음)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                dump.text ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                TargetApps.classify(dump.packageName, null)?.label ?: dump.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
