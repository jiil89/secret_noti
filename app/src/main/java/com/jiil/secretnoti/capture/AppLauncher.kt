package com.jiil.secretnoti.capture

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * 원래 앱(카톡·Teams·문자)에서 대화방을 여는 일.
 * 알림의 contentIntent는 DB에 넣을 수 없어서, 앱이 살아 있는 동안 메모리에만 보관한다.
 */
object AppLauncher {
    private val contentIntents = ConcurrentHashMap<Long, PendingIntent>()

    fun remember(conversationId: Long, intent: PendingIntent) {
        contentIntents[conversationId] = intent
    }

    /** 대화방 알림이 있으면 그 방으로, 없으면 앱만 연다. 원래 앱이 열리면 거기서 읽음 처리된다. */
    fun openConversation(context: Context, conversationId: Long, packageName: String): Boolean {
        contentIntents[conversationId]?.let { pi ->
            val sent = runCatching {
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
            if (sent) return true
            contentIntents.remove(conversationId)
        }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
