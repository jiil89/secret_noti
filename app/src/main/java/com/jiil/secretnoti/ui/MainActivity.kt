package com.jiil.secretnoti.ui

import com.jiil.secretnoti.R
import androidx.compose.ui.res.stringResource
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
import com.jiil.secretnoti.overlay.AppVisibility
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.screens.LockScreen
import com.jiil.secretnoti.ui.model.ContactKind
import androidx.compose.ui.platform.LocalContext
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
            val textSize by Prefs.textSize.flow.collectAsState()
            val previewLines by Prefs.previewLines.flow.collectAsState()
            NotiTheme(theme, textSize, previewLines) {
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
        AppVisibility.mainVisible = true
        AppLock.onAppStart(this)
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.mainVisible = false
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
        const val ROUTE_INTRO = "intro"
        /** 처음 소개 뒤의 권한 설정. 뒤로 가면 설정이 아니라 목록으로. */
        const val ROUTE_SETUP = "setup"
        const val ROUTE_HELP = "help"
        fun routeRoom(conversationId: Long) = "room:$conversationId"
        fun routeContact(kind: ContactKind) = "contact:${kind.name}"

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
    val context = LocalContext.current
    // 처음 켤 때만 소개를 보여 준다. 알림 접근을 이미 켜 둔 사용자(업데이트)는 건너뛴다.
    val firstRun = remember { !Prefs.onboardingDone.value && !readPermissionSummary(context).notificationAccess }
    var route by rememberSaveable { mutableStateOf(if (firstRun) MainActivity.ROUTE_INTRO else MainActivity.ROUTE_LIST) }
    var introFromHelp by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { Prefs.onboardingDone.value = true }
    val requested by requestedRoute.collectAsState()
    LaunchedEffect(requested) {
        requested?.let {
            route = it
            requestedRoute.value = null
        }
    }
    // 권한·진단·도움말·문의 화면은 설정으로, 나머지는 목록으로 돌아간다.
    val parent = when {
        route == MainActivity.ROUTE_PERMISSIONS || route == MainActivity.ROUTE_DIAGNOSTICS ||
            route == MainActivity.ROUTE_HELP || route.startsWith("contact:") -> MainActivity.ROUTE_SETTINGS
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
            onOpenHelp = { route = MainActivity.ROUTE_HELP },
            onOpenContact = { route = MainActivity.routeContact(it) },
        )
        route == MainActivity.ROUTE_INTRO -> OnboardingScreen(onFinish = {
            route = when {
                !readPermissionSummary(context).allGranted -> MainActivity.ROUTE_SETUP
                introFromHelp -> MainActivity.ROUTE_HELP
                else -> MainActivity.ROUTE_LIST
            }
            introFromHelp = false
        })
        route == MainActivity.ROUTE_SETUP -> BackScaffold(stringResource(R.string.route_permissions), onBack = goBack) { PermissionsScreen() }
        route == MainActivity.ROUTE_HELP -> HelpScreen(
            onBack = goBack,
            onReplayIntro = { introFromHelp = true; route = MainActivity.ROUTE_INTRO },
            onOpenPermissions = { route = MainActivity.ROUTE_PERMISSIONS },
            onContact = { route = MainActivity.routeContact(it) },
        )
        route.startsWith("contact:") -> {
            val kind = ContactKind.entries.find { it.name == route.removePrefix("contact:") } ?: ContactKind.OTHER
            ContactScreen(kind, onBack = goBack)
        }
        route == MainActivity.ROUTE_PERMISSIONS -> BackScaffold(stringResource(R.string.route_permissions), onBack = goBack) { PermissionsScreen() }
        route == MainActivity.ROUTE_DIAGNOSTICS -> DiagnosticsScreen(onBack = goBack)
        else -> ConversationListRoute(
            onOpenConversation = { route = MainActivity.routeRoom(it) },
            onOpenSettings = { route = MainActivity.ROUTE_SETTINGS },
            onOpenSearch = { route = MainActivity.ROUTE_SEARCH },
        )
    }
}
