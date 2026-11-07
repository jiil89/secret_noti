package com.jiil.secretnoti.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.jiil.secretnoti.capture.ListenerStatus
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.screens.LockScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    /** 엣지패널·위젯·알림에서 "이 화면을 열어 달라"고 들어온 요청. */
    private val requestedRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)

        // 잠금을 켜면 최근 앱 화면·스크린샷에 내용이 남지 않게 한다.
        lifecycleScope.launch {
            Prefs.lockEnabled.flow.collect { on ->
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        setContent {
            val theme by Prefs.themeMode.flow.collectAsState()
            NotiTheme(theme) {
                val locked by AppLock.locked.collectAsState()
                if (locked && AppLock.isActive(this)) {
                    LockGate(this)
                } else {
                    AppRoot(requestedRoute)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppLock.onAppStart(this)
    }

    override fun onStop() {
        super.onStop()
        AppLock.onAppStop()
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
        const val ROUTE_PERMISSIONS = "permissions"
        const val ROUTE_DIAGNOSTICS = "diagnostics"
        const val ROUTE_SEARCH = "search"
        fun routeRoom(conversationId: Long) = "room:$conversationId"

        fun intent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

@Composable
private fun LockGate(activity: FragmentActivity) {
    var error by remember { mutableStateOf<String?>(null) }
    // 잠금 화면이 뜨면 바로 인증 창을 띄운다.
    LaunchedEffect(Unit) { AppLock.authenticate(activity) { error = it } }
    LockScreen(onUnlock = { AppLock.authenticate(activity) { error = it } }, error = error)
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
    // 권한·진단 화면은 설정으로, 나머지는 목록으로 돌아간다.
    val parent = when (route) {
        MainActivity.ROUTE_PERMISSIONS, MainActivity.ROUTE_DIAGNOSTICS -> MainActivity.ROUTE_SETTINGS
        else -> MainActivity.ROUTE_LIST
    }
    val goBack = { route = parent }
    BackHandler(enabled = route != MainActivity.ROUTE_LIST, onBack = goBack)

    val roomId = route.removePrefix("room:").toLongOrNull()
    when {
        roomId != null -> RoomRoute(roomId, onBack = goBack)
        route == MainActivity.ROUTE_SEARCH -> SearchRoute(
            onOpenConversation = { route = MainActivity.routeRoom(it) },
            onBack = goBack,
        )
        route == MainActivity.ROUTE_SETTINGS -> SettingsRoute(
            onBack = goBack,
            onOpenPermissions = { route = MainActivity.ROUTE_PERMISSIONS },
            onOpenDiagnostics = { route = MainActivity.ROUTE_DIAGNOSTICS },
        )
        route == MainActivity.ROUTE_PERMISSIONS -> BackScaffold("권한", onBack = goBack) { PermissionsScreen() }
        route == MainActivity.ROUTE_DIAGNOSTICS -> DiagnosticsScreen(onBack = goBack)
        else -> ConversationListRoute(
            onOpenConversation = { route = MainActivity.routeRoom(it) },
            onOpenSettings = { route = MainActivity.ROUTE_SETTINGS },
            onOpenSearch = { route = MainActivity.ROUTE_SEARCH },
        )
    }
}
