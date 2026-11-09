package com.jiil.secretnoti.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.ui.model.BadgeStyle
import com.jiil.secretnoti.ui.model.HandleSize
import com.jiil.secretnoti.ui.model.PanelWidth
import com.jiil.secretnoti.ui.model.PreviewLines
import com.jiil.secretnoti.ui.model.TextSize
import com.jiil.secretnoti.ui.model.ReadElsewhereAction
import com.jiil.secretnoti.ui.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    val overlayEnabled = bool("overlay_enabled", true)
    val gestureExclusion = bool("gesture_exclusion", true)
    val handleOnLeft = bool("handle_on_left", false)
    val handleY = float("handle_y", 0.35f)
    val handleSize = enum("handle_size", HandleSize.MEDIUM)
    /** 엣지패널 폭(dp). */
    val panelWidthDp = int("panel_width_dp", PanelWidth.NORMAL.dp)
    val dumpAllApps = bool("dump_all_apps", false)
    val lastPanelOpenedAt = long("last_panel_opened_at", 0L)

    /** 보관 기간(일). 0이면 지우지 않고 계속 보관. */
    val retentionDays = int("retention_days", 30)
    val lastCleanupAt = long("last_cleanup_at", 0L)

    /** 대상 앱 패키지 목록. 기본 문자 앱은 [TargetApps.SMS_TOKEN]. */
    val enabledApps = stringSet("enabled_apps", TargetApps.DEFAULT_ENABLED)

    val lockEnabled = bool("lock_enabled", false)
    val hidePreviews = bool("hide_previews", false)
    val deletionAlerts = bool("deletion_alerts", true)
    val savePhotos = bool("save_photos", true)
    val keywords = stringSet("keywords", emptySet())
    val themeMode = enum("theme_mode", ThemeMode.SYSTEM)
    val readElsewhere = enum("read_elsewhere", ReadElsewhereAction.REMOVE_FROM_PANEL)
    val autoMuteAds = bool("auto_mute_ads", false)
    val autoMuteChannels = bool("auto_mute_channels", false)
    val channelBackfillDone = bool("channel_backfill_done", false)
    val indicatorBadge = bool("indicator_badge", true)
    val badgeStyle = enum("badge_style", BadgeStyle.NUMBER)
    val indicatorGlow = bool("indicator_glow", true)
    val glowUsesAppColor = bool("glow_uses_app_color", false)
    val glowColor = int("glow_color", 0xFF2558E8.toInt())
    val peekBubble = bool("peek_bubble", false)
    val textSize = enum("text_size", TextSize.NORMAL)
    val previewLines = enum("preview_lines", PreviewLines.AUTO)

    private val all = listOf(
        overlayEnabled, gestureExclusion, handleOnLeft, handleY, handleSize, panelWidthDp, dumpAllApps, lastPanelOpenedAt,
        retentionDays, lastCleanupAt, enabledApps, lockEnabled, hidePreviews, deletionAlerts, savePhotos,
        keywords, themeMode, readElsewhere, autoMuteAds, autoMuteChannels, channelBackfillDone, indicatorBadge, badgeStyle, indicatorGlow, glowUsesAppColor, glowColor,
        peekBubble, textSize, previewLines,
    )

    fun init(context: Context) {
        sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        migrateSourceSwitches()
        all.forEach { it.load(sp) }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        enabledApps.value = if (enabled) enabledApps.value + packageName else enabledApps.value - packageName
    }

    /** 이전 버전의 카톡·문자·Teams 스위치를 대상 앱 목록으로 옮긴다. */
    private fun migrateSourceSwitches() {
        if (sp.contains("enabled_apps") || !sp.contains("source_kakao")) return
        val set = buildSet {
            if (sp.getBoolean("source_kakao", true)) add(TargetApps.KAKAO)
            if (sp.getBoolean("source_sms", true)) add(TargetApps.SMS_TOKEN)
            if (sp.getBoolean("source_teams", true)) add(TargetApps.TEAMS)
        }
        sp.edit { putStringSet("enabled_apps", set) }
    }

    private fun bool(key: String, default: Boolean) =
        Pref(key, default, { k, d -> getBoolean(k, d) }, { k, v -> putBoolean(k, v) })

    private fun int(key: String, default: Int) =
        Pref(key, default, { k, d -> getInt(k, d) }, { k, v -> putInt(k, v) })

    private fun float(key: String, default: Float) =
        Pref(key, default, { k, d -> getFloat(k, d) }, { k, v -> putFloat(k, v) })

    private fun long(key: String, default: Long) =
        Pref(key, default, { k, d -> getLong(k, d) }, { k, v -> putLong(k, v) })

    private fun stringSet(key: String, default: Set<String>) =
        Pref(key, default, { k, d -> getStringSet(k, d)?.toSet() ?: d }, { k, v -> putStringSet(k, v) })

    private inline fun <reified E : Enum<E>> enum(key: String, default: E) = Pref(
        key, default,
        { k, d -> runCatching { enumValueOf<E>(getString(k, d.name)!!) }.getOrDefault(d) },
        { k, v -> putString(k, v.name) },
    )
}
