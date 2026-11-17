package com.jiil.secretnoti.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.ui.design.NotiColors
import com.jiil.secretnoti.ui.design.NotiIcons
import com.jiil.secretnoti.ui.design.NotiShapes

/**
 * 카톡 알림 표시가 '표시 안 함'이라 카톡 내용이 저장되지 않을 때의 안내 (앱 첫 화면·설정 맨 위).
 * [onSnooze]가 있으면 "오늘은 그만 보기"도 보여 준다.
 */
@Composable
fun KakaoHiddenNotice(onOpenKakao: () -> Unit, modifier: Modifier = Modifier, onSnooze: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().background(NotiColors.NoticeBackground, NotiShapes.banner)
        .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(NotiIcons.info, null, Modifier.size(18.dp), tint = NotiColors.NoticeForeground)
            Text(stringResource(R.string.kakao_hidden_title), color = NotiColors.NoticeForeground,
                style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(R.string.kakao_hidden_body), color = NotiColors.NoticeForeground,
            style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onOpenKakao, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.kakao_hidden_open), color = NotiColors.NoticeForeground,
                    fontWeight = FontWeight.Bold)
            }
            if (onSnooze != null) TextButton(onClick = onSnooze, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.kakao_hidden_snooze), color = NotiColors.NoticeForeground.copy(alpha = 0.75f))
            }
        }
    }
}

/** 엣지 패널용 한 줄 안내. 누르면 앱 첫 화면으로 간다. */
@Composable
fun KakaoHiddenNoticeSlim(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(NotiShapes.banner).background(NotiColors.NoticeBackground)
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(NotiIcons.info, null, Modifier.size(16.dp), tint = NotiColors.NoticeForeground)
        Text(stringResource(R.string.kakao_hidden_slim), Modifier.weight(1f), color = NotiColors.NoticeForeground,
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.kakao_hidden_fix), color = NotiColors.NoticeForeground,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** 카카오톡 앱만 연다 (방을 여는 게 아니라 읽음 처리되지 않는다). */
fun openKakaoApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(TargetApps.KAKAO) ?: return
    context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
