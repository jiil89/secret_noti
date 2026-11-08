package com.jiil.secretnoti.overlay

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.ConcurrentHashMap

/** 새 메시지가 저장됐다는 신호. 손잡이 반짝임·미리보기 말풍선에 쓴다. */
data class NewMessageEvent(
    val conversationId: Long,
    val packageName: String,
    val title: String,
    val text: String,
)

object OverlayEvents {
    private val messages = MutableSharedFlow<NewMessageEvent>(extraBufferCapacity = 16)
    val newMessages: SharedFlow<NewMessageEvent> get() = messages

    private val previews = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** 설정의 "반짝임 미리 보기". */
    val glowPreviews: SharedFlow<Unit> get() = previews

    fun newMessage(event: NewMessageEvent) = messages.tryEmit(event)
    fun previewGlow() = previews.tryEmit(Unit)
}

/** 시크릿노티 화면이 앞에 떠 있는지. 떠 있으면 미리보기 말풍선을 띄우지 않는다. */
object AppVisibility {
    @Volatile
    var mainVisible = false
}

/** "보낸 앱 색" 반짝임: 잘 알려진 앱은 정해진 색, 나머지는 앱 아이콘의 평균 색. */
object AppColors {
    private val known = mapOf(
        "com.kakao.talk" to 0xFFFEE500.toInt(),
        "com.microsoft.teams" to 0xFF6264A7.toInt(),
        "com.samsung.android.messaging" to 0xFF3E8EF7.toInt(),
        "com.google.android.apps.messaging" to 0xFF1A73E8.toInt(),
        "jp.naver.line.android" to 0xFF06C755.toInt(),
        "com.whatsapp" to 0xFF25D366.toInt(),
        "org.telegram.messenger" to 0xFF229ED9.toInt(),
        "com.instagram.android" to 0xFFE1306C.toInt(),
        "com.facebook.orca" to 0xFF0084FF.toInt(),
        "com.discord" to 0xFF5865F2.toInt(),
        "com.Slack" to 0xFF611F69.toInt(),
        "com.nhn.android.band" to 0xFF00C73C.toInt(),
    )
    private val cache = ConcurrentHashMap<String, Int>()

    fun of(context: Context, packageName: String, fallback: Int): Int = known[packageName] ?: cache.getOrPut(packageName) {
        runCatching {
            val bmp = context.packageManager.getApplicationIcon(packageName).toBitmap(24, 24)
            var r = 0L; var g = 0L; var b = 0L; var n = 0
            for (x in 0 until bmp.width) for (y in 0 until bmp.height) {
                val c = bmp.getPixel(x, y)
                // 투명하거나 거의 흰색·검은색인 픽셀은 빼고 색이 있는 부분만 평균
                if (Color.alpha(c) < 128) continue
                val max = maxOf(Color.red(c), Color.green(c), Color.blue(c))
                val min = minOf(Color.red(c), Color.green(c), Color.blue(c))
                if (max - min < 30) continue
                r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
            }
            if (n == 0) fallback else Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
        }.getOrDefault(fallback)
    }
}
