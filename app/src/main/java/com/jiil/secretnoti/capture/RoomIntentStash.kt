package com.jiil.secretnoti.capture

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ReadElsewhereAction
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 대화방 알림의 "방 열기" PendingIntent를 앱이 다시 시작돼도 쓸 수 있게 맡겨 두는 곳.
 *
 * PendingIntent는 파일에 저장할 수 없지만, 우리 PendingIntent의 extra에 넣어 두면 시스템이 함께 들고 있는다.
 * 앱이 다시 시작된 뒤에는 우리 PendingIntent를 찾아 보내고, 여기서 extra를 꺼내 그 방을 연다.
 */
class RoomIntentStash : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return
        val room = IntentCompat.getParcelableExtra(intent, EXTRA_ROOM, PendingIntent::class.java)
        val result = goAsync()
        if (room != null && AppLauncher.sendRoom(context, room)) {
            AppLauncher.remember(id, room, stash = false)
            scope.launch {
                runCatching {
                    NotiApp.instance.chat.markOpenedInApp(id, Prefs.readElsewhere.value == ReadElsewhereAction.REMOVE_FROM_PANEL)
                }
                WidgetUpdater.request(context)
                result.finish()
            }
        } else {
            AppLauncher.openAppOnly(context, pkg)
            result.finish()
        }
    }

    companion object {
        private const val EXTRA_ID = "conversation_id"
        private const val EXTRA_PKG = "package"
        private const val EXTRA_ROOM = "room"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun key(context: Context, id: Long) =
            Intent(context, RoomIntentStash::class.java).setAction("com.jiil.secretnoti.ROOM.$id")

        fun put(context: Context, id: Long, pkg: String, room: PendingIntent) {
            val intent = key(context, id).putExtra(EXTRA_ID, id).putExtra(EXTRA_PKG, pkg).putExtra(EXTRA_ROOM, room)
            runCatching {
                PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        }

        /** 맡겨 둔 것이 있으면 꺼내 보내고 true. 실제로 방을 여는 일은 [onReceive]에서 한다. */
        fun open(context: Context, id: Long): Boolean {
            val stash = PendingIntent.getBroadcast(context, 0, key(context, id),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return false
            return runCatching { stash.send() }.isSuccess
        }

        fun showAppOnlyNotice(context: Context, appLabel: String) {
            Toast.makeText(context, context.getString(R.string.room_open_app_only, appLabel), Toast.LENGTH_LONG).show()
        }
    }
}
