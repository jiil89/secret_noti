package com.jiil.secretnoti.capture

import android.content.Context
import android.net.Uri
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ReadElsewhereAction
import com.jiil.secretnoti.widget.WidgetUpdater

/**
 * 삼성 배지 저장소(앱 아이콘 숫자). 카톡은 여기에 전체 안 읽은 수를 쓴다.
 * 알림창에서 "모두 지우기"를 한 뒤 카톡에서 읽으면 알림으로는 알 수 없으므로, 배지가 0이 된 것으로 알아챈다.
 * 갤럭시가 아니거나 읽을 수 없으면 아무 일도 하지 않는다.
 */
object BadgeReader {
    private val URI: Uri = Uri.parse("content://com.sec.badge/apps")

    /** 패키지 → 배지 숫자. */
    fun read(context: Context): Result<Map<String, Int>> = runCatching {
        val out = linkedMapOf<String, Int>()
        context.contentResolver.query(URI, null, null, null, null)?.use { c ->
            val pkg = c.getColumnIndex("package")
            val count = c.getColumnIndex("badgecount")
            check(pkg >= 0 && count >= 0) { "columns=" + c.columnNames.joinToString() }
            while (c.moveToNext()) {
                val p = c.getString(pkg) ?: continue
                out[p] = (out[p] ?: 0) + c.getInt(count)
            }
        } ?: error("query returned null")
        out
    }

    /** 진단 화면용: 카톡 배지 숫자. */
    fun kakao(context: Context): String = read(context).fold(
        { map -> map.filterKeys { it.startsWith("com.kakao") }.entries.joinToString { "${it.key}=${it.value}" }
            .ifEmpty { "kakao 없음 (전체 ${map.size}개 앱)" } },
        { "오류: ${it.javaClass.simpleName}: ${it.message}" },
    )

    /** 카톡 배지가 0이면 카톡 대화방을 모두 읽음으로 한다. IO 스레드에서 부른다. */
    suspend fun syncKakao(context: Context) {
        if (!TargetApps.isEnabled(TargetApps.KAKAO, Prefs.enabledApps.value, null)) return
        val badge = read(context).getOrNull()?.get(TargetApps.KAKAO) ?: return
        if (badge != 0) return
        runCatching {
            NotiApp.instance.chat.markAppAllRead(TargetApps.KAKAO,
                Prefs.readElsewhere.value == ReadElsewhereAction.REMOVE_FROM_PANEL)
        }
        WidgetUpdater.request(context)
    }
}
