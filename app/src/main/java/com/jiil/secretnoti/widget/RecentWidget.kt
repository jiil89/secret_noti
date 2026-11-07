package com.jiil.secretnoti.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.ui.MainActivity
import com.jiil.secretnoti.ui.components.formatRelativeTime
import com.jiil.secretnoti.ui.model.ConversationUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** 홈 화면 위젯: 최근 대화방 몇 개. 내용 가리기를 켜면 미리보기 대신 "잠금 중"을 보여준다. */
class RecentWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = NotiApp.instance.chat.observeConversations().first().take(4)
        provideContent { GlanceTheme { WidgetBody(items) } }
    }
}

class RecentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentWidget()
}

@Composable
private fun WidgetBody(items: List<ConversationUi>) {
    val context = LocalContext.current
    Column(
        GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(14.dp),
    ) {
        Text(
            context.getString(R.string.app_name),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GlanceTheme.colors.onSurface),
            modifier = GlanceModifier.clickable(actionStartActivity(MainActivity.intent(context, MainActivity.ROUTE_LIST))),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        items.forEach { c ->
            Row(
                GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                    .clickable(actionStartActivity(MainActivity.intent(context, MainActivity.routeRoom(c.id)))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        c.title,
                        maxLines = 1,
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                    )
                    Text(
                        if (c.previewHidden) context.getString(R.string.preview_locked) else c.preview,
                        maxLines = 1,
                        style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
                Text(
                    formatRelativeTime(context, c.lastAt),
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        }
    }
}

/** 새 메시지가 연달아 와도 위젯은 잠깐 모았다가 한 번만 새로 그린다. */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pending = AtomicBoolean(false)

    fun request(context: Context) {
        if (!pending.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch {
            delay(1500)
            pending.set(false)
            runCatching { RecentWidget().updateAll(app) }
        }
    }
}
