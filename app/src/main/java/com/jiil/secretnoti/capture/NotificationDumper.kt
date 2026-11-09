package com.jiil.secretnoti.capture

import android.app.Notification
import android.app.Person
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

/**
 * 알림 하나를 extras 전체까지 JSON으로 풀어낸다.
 * 카톡·Teams·문자 앱의 실제 알림 구조를 확인해 파서를 확정하기 위한 M1 디버그 도구.
 */
object NotificationDumper {

    fun toJson(sbn: StatusBarNotification, event: String, removalReason: Int?): JSONObject {
        val n = sbn.notification
        return JSONObject().apply {
            put("event", event)
            removalReason?.let { put("removalReason", it) }
            // postTime은 지운 알림에도 "올린 시각"이 그대로라, 언제 지워졌는지 알려면 기록 시각이 따로 필요하다.
            put("recordedAt", System.currentTimeMillis())
            put("package", sbn.packageName)
            put("id", sbn.id)
            put("tag", sbn.tag ?: JSONObject.NULL)
            put("key", sbn.key)
            put("groupKey", sbn.groupKey)
            put("postTime", sbn.postTime)
            put("isOngoing", sbn.isOngoing)
            put("isClearable", sbn.isClearable)
            put("flags", n.flags)
            put("isGroupSummary", n.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            put("category", n.category ?: JSONObject.NULL)
            put("group", n.group ?: JSONObject.NULL)
            put("sortKey", n.sortKey ?: JSONObject.NULL)
            put("when", n.`when`)
            put("number", n.number)
            put("tickerText", n.tickerText?.toString() ?: JSONObject.NULL)
            put("hasContentIntent", n.contentIntent != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                put("channelId", n.channelId ?: JSONObject.NULL)
                put("shortcutId", n.shortcutId ?: JSONObject.NULL)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put("locusId", n.locusId?.id ?: JSONObject.NULL)
            }
            put("actions", JSONArray().apply {
                n.actions?.forEach { a ->
                    put(JSONObject().apply {
                        put("title", a.title?.toString() ?: JSONObject.NULL)
                        put("hasRemoteInputs", !a.remoteInputs.isNullOrEmpty())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) put("semanticAction", a.semanticAction)
                    })
                }
            })
            put("extras", bundleToJson(n.extras))
        }
    }

    private fun bundleToJson(bundle: Bundle?): Any {
        if (bundle == null) return JSONObject.NULL
        val out = JSONObject()
        for (key in bundle.keySet().sorted()) {
            val value = try {
                @Suppress("DEPRECATION")
                valueToJson(bundle.get(key), depth = 0)
            } catch (t: Throwable) {
                "<읽기 실패: ${t.javaClass.simpleName}>"
            }
            out.put(key, value)
        }
        return out
    }

    private fun valueToJson(value: Any?, depth: Int): Any {
        if (depth > 6) return "<너무 깊음>"
        return when (value) {
            null -> JSONObject.NULL
            is Boolean, is Int, is Long, is Double -> value
            is Float -> value.toDouble()
            is CharSequence -> value.toString()
            is Bundle -> bundleToJson(value)
            is Bitmap -> "<Bitmap ${value.width}x${value.height}>"
            is Icon -> "<Icon>"
            is IntArray -> JSONArray(value.toList())
            is LongArray -> JSONArray(value.toList())
            is BooleanArray -> JSONArray(value.toList())
            is Array<*> -> JSONArray().apply { value.forEach { put(valueToJson(it, depth + 1)) } }
            is Iterable<*> -> JSONArray().apply { value.forEach { put(valueToJson(it, depth + 1)) } }
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && value is Person) {
                JSONObject().apply {
                    put("_type", "Person")
                    put("name", value.name?.toString() ?: JSONObject.NULL)
                    put("key", value.key ?: JSONObject.NULL)
                    put("uri", value.uri ?: JSONObject.NULL)
                    put("isBot", value.isBot)
                    put("isImportant", value.isImportant)
                }
            } else {
                "<${value.javaClass.name}> ${value.toString().take(300)}"
            }
        }
    }
}
