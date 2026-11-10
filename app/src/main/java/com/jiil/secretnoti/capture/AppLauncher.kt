package com.jiil.secretnoti.capture

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * 원래 앱(카톡·Teams·문자)에서 대화방을 여는 일.
 * 알림의 contentIntent는 DB에 넣을 수 없어서 메모리에 두고, 앱이 다시 시작될 때를 위해
 * [RoomIntentStash]에도 맡겨 둔다.
 */
object AppLauncher {
    private val contentIntents = ConcurrentHashMap<Long, PendingIntent>()

    fun remember(conversationId: Long, intent: PendingIntent, pkg: String? = null, stash: Boolean = true,
                 context: Context? = null) {
        contentIntents[conversationId] = intent
        if (stash && pkg != null && context != null) RoomIntentStash.put(context, conversationId, pkg, intent)
    }

    /**
     * ROOM: 방을 열었다. STASHED: 맡겨 둔 것으로 여는 중 (읽음 처리는 [RoomIntentStash]가 한다).
     * APP: 방은 못 열고 앱만 열었다. FAILED: 앱이 없다.
     */
    enum class Opened { ROOM, STASHED, APP, FAILED }

    fun sendRoom(context: Context, pi: PendingIntent): Boolean = runCatching {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 36) {
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        pi.send(context, 0, null, null, null, null, options.toBundle())
    }.isSuccess

    /** 방을 열 수 없을 때: 앱만 열고 그렇다고 알려 준다. */
    fun openAppOnly(context: Context, packageName: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        RoomIntentStash.showAppOnlyNotice(context, AppLabels.label(context, packageName))
        return true
    }

    /** 대화방 알림이 있으면 그 방으로([Opened.ROOM]), 없으면 앱만 연다. 방이 열리면 원래 앱에서 읽음 처리된다. */
    fun openConversation(context: Context, conversationId: Long, packageName: String): Opened {
        contentIntents[conversationId]?.let { pi ->
            if (sendRoom(context, pi)) return Opened.ROOM
            contentIntents.remove(conversationId)
        }
        // 앱이 다시 시작돼 메모리에 없으면 맡겨 둔 것을 찾는다.
        if (RoomIntentStash.open(context, conversationId)) return Opened.STASHED
        return if (openAppOnly(context, packageName)) Opened.APP else Opened.FAILED
    }
}
