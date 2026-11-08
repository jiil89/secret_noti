package com.jiil.secretnoti.capture

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.net.Uri
import android.service.notification.StatusBarNotification

/** StatusBarNotification → [NotificationSnapshot]. */
object SnapshotExtractor {

    fun from(sbn: StatusBarNotification, shortcutLabel: String? = null): NotificationSnapshot {
        val n = sbn.notification
        val e = n.extras
        return NotificationSnapshot(
            packageName = sbn.packageName,
            tag = sbn.tag,
            shortcutId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) n.shortcutId else null,
            locusId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) n.locusId?.id else null,
            postTime = sbn.postTime,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0,
            category = n.category,
            title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            conversationTitle = e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
            hiddenConversationTitle = e.getCharSequence("android.hiddenConversationTitle")?.toString(),
            isGroupConversation = if (e.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                e.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
            } else {
                null
            },
            selfKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                e.parcelable<Person>(Notification.EXTRA_MESSAGING_PERSON)?.key
            } else {
                null
            },
            messages = messages(e),
            shortcutLabel = shortcutLabel,
        )
    }

    private fun messages(e: Bundle): List<RawMessage> {
        val array: Array<out Parcelable> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            e.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            e.getParcelableArray(Notification.EXTRA_MESSAGES)
        } ?: return emptyList()

        return array.mapNotNull { item ->
            val b = item as? Bundle ?: return@mapNotNull null
            val text = b.getCharSequence("text")?.toString() ?: return@mapNotNull null
            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) b.parcelable<Person>("sender_person") else null
            RawMessage(
                sender = person?.name?.toString() ?: b.getCharSequence("sender")?.toString(),
                senderKey = person?.key,
                text = text,
                time = b.getLong("time"),
                messageId = b.getBundle("extras")?.let { x ->
                    @Suppress("DEPRECATION")
                    x.get("chatLogId")?.toString()
                },
                dataMimeType = b.getString("type"),
                dataUri = b.parcelable<Uri>("uri")?.toString(),
            )
        }
    }

    private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key) as? T
        }
}
