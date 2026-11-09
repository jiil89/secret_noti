package com.jiil.secretnoti.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.ui.design.*
import kotlinx.coroutines.launch

private val INTRO_PAGES = listOf(
    R.string.intro_1_title to R.string.intro_1_body,
    R.string.intro_2_title to R.string.intro_2_body,
    R.string.intro_3_title to R.string.intro_3_body,
)

/** 처음 켤 때 한 번 나오는 3장 소개. 끝나면 권한 설정으로 이어진다. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { INTRO_PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == INTRO_PAGES.lastIndex
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.intro_skip), color = NotiColors.Secondary)
            }
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(NotiColors.DetailBackground).clearAndSetSemantics {}) {
                        when (page) {
                            0 -> IntroUnread()
                            1 -> IntroEdge()
                            else -> IntroOpen()
                        }
                    }
                    Text(stringResource(INTRO_PAGES[page].first), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = NotiColors.Text)
                    Text(stringResource(INTRO_PAGES[page].second), style = MaterialTheme.typography.bodyLarge,
                        color = NotiColors.Body)
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(INTRO_PAGES.size) { i ->
                    Box(Modifier.height(6.dp).width(if (i == pager.currentPage) 18.dp else 6.dp)
                        .background(if (i == pager.currentPage) NotiColors.Accent else NotiColors.Divider, CircleShape))
                }
            }
            Button(onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                shape = NotiShapes.banner, modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                Text(stringResource(if (last) R.string.intro_start else R.string.intro_next), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 1장: 상대방 화면처럼 말풍선 옆 1이 그대로 남아 있다. */
@Composable
private fun BoxScope.IntroUnread() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(R.string.intro_sample_1, R.string.intro_sample_2).forEach { text ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(text), Modifier.background(NotiColors.Background, NotiShapes.bubble)
                    .padding(horizontal = 12.dp, vertical = 8.dp), color = NotiColors.Body,
                    style = MaterialTheme.typography.bodyMedium)
                Text("1", color = Color(0xFFE0A800), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Box(Modifier.align(Alignment.BottomEnd).padding(20.dp).size(56.dp).clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF16181D)), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_noti_foreground), null, Modifier.requiredSize(92.dp))
    }
}

/** 2장: 화면 가장자리 손잡이와 열린 패널. */
@Composable
private fun BoxScope.IntroEdge() {
    Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.76f).fillMaxWidth(0.58f)
        .shadow(8.dp, NotiShapes.panel(onLeftSide = false)).background(NotiColors.Background, NotiShapes.panel(onLeftSide = false))
        .padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.conversations_recent), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = NotiColors.Text)
            repeat(4) { i ->
                Box(Modifier.fillMaxWidth(if (i % 2 == 0) 1f else 0.6f).height(8.dp)
                    .background(NotiColors.DetailBackground, CircleShape))
            }
        }
    }
    Text(stringResource(R.string.intro_2_swipe), Modifier.align(Alignment.CenterStart).padding(start = 20.dp),
        color = NotiColors.NoticeForeground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
}

/** 3장: 답장할 때만 원래 앱으로. */
@Composable
private fun BoxScope.IntroOpen() {
    Text(stringResource(R.string.intro_3_tag), Modifier.align(Alignment.TopStart).padding(20.dp)
        .background(NotiColors.Background, NotiShapes.banner).padding(horizontal = 10.dp, vertical = 6.dp),
        color = NotiColors.Body, style = MaterialTheme.typography.labelLarge)
    OutlinedButton(onClick = {}, enabled = false, shape = NotiShapes.banner,
        border = BorderStroke(1.dp, NotiColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = NotiColors.Background,
            disabledContainerColor = NotiColors.Background, disabledContentColor = NotiColors.Text),
        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth()) {
        Text(stringResource(R.string.room_screen_open, stringResource(R.string.source_kakao)),
            style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.room_screen_marks_read), color = NotiColors.Secondary,
            style = MaterialTheme.typography.labelLarge)
    }
}
