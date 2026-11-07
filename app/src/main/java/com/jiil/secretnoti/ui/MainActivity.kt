package com.jiil.secretnoti.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jiil.secretnoti.capture.ListenerStatus
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** 엣지패널 등 바깥에서 "이 화면을 열어 달라"고 들어온 요청. */
    private val requestedRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        setContent { NotiTheme { AppRoot(requestedRoute) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onResume() {
        super.onResume()
        ListenerStatus.requestRefresh()
    }

    companion object {
        private const val EXTRA_ROUTE = "route"
        const val ROUTE_LIST = "list"
        const val ROUTE_SETTINGS = "settings"
        fun routeRoom(conversationId: Long) = "room:$conversationId"

        fun intent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

@Composable
private fun AppRoot(requestedRoute: MutableStateFlow<String?>) {
    var route by rememberSaveable { mutableStateOf(MainActivity.ROUTE_LIST) }
    val requested by requestedRoute.collectAsState()
    LaunchedEffect(requested) {
        requested?.let {
            route = it
            requestedRoute.value = null
        }
    }
    BackHandler(enabled = route != MainActivity.ROUTE_LIST) { route = MainActivity.ROUTE_LIST }

    val roomId = route.removePrefix("room:").toLongOrNull()
    when {
        roomId != null -> RoomRoute(roomId, onBack = { route = MainActivity.ROUTE_LIST })
        route == MainActivity.ROUTE_SETTINGS -> DiagnosticsScreen(onBack = { route = MainActivity.ROUTE_LIST })
        else -> ConversationListRoute(
            onOpenConversation = { route = MainActivity.routeRoom(it) },
            onOpenSettings = { route = MainActivity.ROUTE_SETTINGS },
        )
    }
}
