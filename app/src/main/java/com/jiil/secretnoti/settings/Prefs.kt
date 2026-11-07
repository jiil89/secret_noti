package com.jiil.secretnoti.settings

import android.content.Context
import android.content.SharedPreferences
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.model.HandleSize
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class Pref<T>(
    private val key: String,
    private val default: T,
    private val read: SharedPreferences.(String, T) -> T,
    private val write: SharedPreferences.Editor.(String, T) -> Unit,
) {
    private val state = MutableStateFlow(default)
    val flow: StateFlow<T> get() = state

    var value: T
        get() = state.value
        set(v) {
            Prefs.sp.edit { write(key, v) }
            state.value = v
        }

    internal fun load(sp: SharedPreferences) {
        state.value = sp.read(key, default)
    }
}

object Prefs {
    internal lateinit var sp: SharedPreferences

    val overlayEnabled = bool("overlay_enabled", false)
    val gestureExclusion = bool("gesture_exclusion", true)
    val handleOnLeft = bool("handle_on_left", false)
    val handleY = float("handle_y", 0.35f)
    val handleSize = Pref(
        "handle_size", HandleSize.MEDIUM,
        { k, d -> runCatching { HandleSize.valueOf(getString(k, d.name)!!) }.getOrDefault(d) },
        { k, v -> putString(k, v.name) },
    )
    val dumpAllApps = bool("dump_all_apps", false)
    val lastPanelOpenedAt = long("last_panel_opened_at", 0L)
    /** 보관 기간(일). 0이면 지우지 않고 계속 보관. */
    val retentionDays = int("retention_days", 30)
    val lastCleanupAt = long("last_cleanup_at", 0L)

    /** 설정에서 고른 대상 앱. 꺼진 앱의 알림은 저장하지 않고 목록에서도 숨긴다. */
    val sourceKakao = bool("source_kakao", true)
    val sourceSms = bool("source_sms", true)
    val sourceTeams = bool("source_teams", true)

    fun sourcePref(source: Source): Pref<Boolean> = when (source) {
        Source.KAKAO -> sourceKakao
        Source.SMS -> sourceSms
        Source.TEAMS -> sourceTeams
    }

    fun isSourceEnabled(source: Source): Boolean = sourcePref(source).value

    val enabledSources: Flow<Set<Source>> =
        combine(sourceKakao.flow, sourceSms.flow, sourceTeams.flow) { _, _, _ ->
            Source.entries.filter { isSourceEnabled(it) }.toSet()
        }

    private val all = listOf(
        overlayEnabled, gestureExclusion, handleOnLeft, handleY, handleSize, dumpAllApps, lastPanelOpenedAt, retentionDays, lastCleanupAt,
        sourceKakao, sourceSms, sourceTeams,
    )

    fun init(context: Context) {
        sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        all.forEach { it.load(sp) }
    }

    private fun bool(key: String, default: Boolean) =
        Pref(key, default, { k, d -> getBoolean(k, d) }, { k, v -> putBoolean(k, v) })

    private fun int(key: String, default: Int) =
        Pref(key, default, { k, d -> getInt(k, d) }, { k, v -> putInt(k, v) })

    private fun float(key: String, default: Float) =
        Pref(key, default, { k, d -> getFloat(k, d) }, { k, v -> putFloat(k, v) })

    private fun long(key: String, default: Long) =
        Pref(key, default, { k, d -> getLong(k, d) }, { k, v -> putLong(k, v) })
}
