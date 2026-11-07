package com.jiil.secretnoti.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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

    val overlayEnabled = bool("overlay_enabled", false)
    val gestureExclusion = bool("gesture_exclusion", true)
    val handleOnLeft = bool("handle_on_left", false)
    val handleY = float("handle_y", 0.35f)
    val dumpAllApps = bool("dump_all_apps", false)
    val lastPanelOpenedAt = long("last_panel_opened_at", 0L)
    val retentionDays = int("retention_days", 30)
    val lastCleanupAt = long("last_cleanup_at", 0L)

    private val all = listOf(
        overlayEnabled, gestureExclusion, handleOnLeft, handleY, dumpAllApps, lastPanelOpenedAt, retentionDays, lastCleanupAt,
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
