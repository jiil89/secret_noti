package com.jiil.secretnoti.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jiil.secretnoti.R
import com.jiil.secretnoti.data.MessageEntity
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.MainActivity

/** 상대가 메시지를 삭제하면 "원문을 보관했다"고 알려준다. */
object DeletionNotifier {
    private const val CHANNEL = "deleted_messages"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, conversationId: Long, roomTitle: String, appLabel: String, deleted: List<MessageEntity>) {
        if (deleted.isEmpty() || !canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_deleted), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        val title = context.getString(R.string.deleted_alert_title, roomTitle)
        val body = if (Prefs.hidePreviews.value) {
            context.getString(R.string.deleted_alert_hidden, deleted.size)
        } else {
            deleted.joinToString("\n") { m ->
                val who = m.sender ?: context.getString(R.string.me)
                "$who: ${m.originalText ?: m.text}"
            }
        }
        val open = PendingIntent.getActivity(
            context,
            conversationId.toInt(),
            MainActivity.intent(context, MainActivity.routeRoom(conversationId)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val public = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_secret)
            .setContentTitle(context.getString(R.string.deleted_alert_public))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_secret)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText(appLabel)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify("deleted", conversationId.toInt(), notification)
    }
}
