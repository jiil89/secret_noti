package com.jiil.secretnoti.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.AppLabels
import com.jiil.secretnoti.capture.DeletionNotifier
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.data.BackupManager
import com.jiil.secretnoti.data.WrongPasswordException
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.AppSettingUi
import com.jiil.secretnoti.ui.model.HandleSize
import com.jiil.secretnoti.ui.model.SettingsActions
import com.jiil.secretnoti.ui.model.SettingsUi
import com.jiil.secretnoti.ui.model.ThemeMode
import com.jiil.secretnoti.ui.screens.SettingsScreen
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 설정 화면과 Prefs·저장소를 잇는다. */
@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenPermissions: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chat = remember { NotiApp.instance.chat }
    val backup = remember { BackupManager(context.applicationContext, NotiApp.instance.db) }

    var permissions by remember { mutableStateOf(readPermissionSummary(context)) }
    var canNotify by remember { mutableStateOf(DeletionNotifier.canNotify(context)) }
    var deviceSecure by remember { mutableStateOf(AppLock.isDeviceSecure(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = readPermissionSummary(context)
        canNotify = DeletionNotifier.canNotify(context)
        deviceSecure = AppLock.isDeviceSecure(context)
        onPauseOrDispose { }
    }

    val enabledApps by Prefs.enabledApps.flow.collectAsStateWithLifecycle()
    val seenApps by remember { chat.observeSeenApps() }.collectAsStateWithLifecycle(emptyList())
    val hidden by remember { chat.observeHiddenConversations() }.collectAsStateWithLifecycle(emptyList())
    val handleEnabled by Prefs.overlayEnabled.flow.collectAsStateWithLifecycle()
    val handleOnLeft by Prefs.handleOnLeft.flow.collectAsStateWithLifecycle()
    val handleY by Prefs.handleY.flow.collectAsStateWithLifecycle()
    val handleSize by Prefs.handleSize.flow.collectAsStateWithLifecycle()
    val exclusion by Prefs.gestureExclusion.flow.collectAsStateWithLifecycle()
    val retention by Prefs.retentionDays.flow.collectAsStateWithLifecycle()
    val lockEnabled by Prefs.lockEnabled.flow.collectAsStateWithLifecycle()
    val hidePreviews by Prefs.hidePreviews.flow.collectAsStateWithLifecycle()
    val deletionAlerts by Prefs.deletionAlerts.flow.collectAsStateWithLifecycle()
    val savePhotos by Prefs.savePhotos.flow.collectAsStateWithLifecycle()
    val keywords by Prefs.keywords.flow.collectAsStateWithLifecycle()
    val themeMode by Prefs.themeMode.flow.collectAsStateWithLifecycle()
    val panelWidth by Prefs.panelWidthDp.flow.collectAsStateWithLifecycle()

    val launcherApps = remember { installedLauncherApps(context) }
    val apps = remember(enabledApps, seenApps, launcherApps) {
        buildAppList(context, enabledApps, seenApps, launcherApps)
    }

    // 백업·복원: 파일을 고른 다음 미리 받아 둔 비밀번호로 처리한다.
    var pendingPassword by remember { mutableStateOf<String?>(null) }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pw = pendingPassword.also { pendingPassword = null }
        if (uri == null || pw == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { backup.export(uri, pw) } }
            toast(result.fold({ context.resources.getQuantityString(R.plurals.backup_done, it, it) }, { context.getString(R.string.backup_failed) }))
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val pw = pendingPassword.also { pendingPassword = null }
        if (uri == null || pw == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { backup.import(uri, pw) } }
            WidgetUpdater.request(context)
            toast(
                result.fold(
                    { context.resources.getQuantityString(R.plurals.restore_done, it, it) },
                    {
                        if (it is WrongPasswordException) context.getString(R.string.restore_wrong_password)
                        else context.getString(R.string.restore_failed)
                    },
                ),
            )
        }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = DeletionNotifier.canNotify(context)
    }

    val state = SettingsUi(
        apps = apps,
        handleEnabled = handleEnabled,
        handleOnLeft = handleOnLeft,
        handlePosition = handleY,
        handleSize = handleSize,
        gestureExclusion = exclusion,
        retentionDays = retention,
        permissions = permissions,
        lockEnabled = lockEnabled,
        lockAvailable = deviceSecure,
        hidePreviews = hidePreviews,
        deletionAlerts = deletionAlerts,
        notificationPermission = canNotify,
        savePhotos = savePhotos,
        keywords = keywords.sorted(),
        hiddenConversations = hidden,
        themeMode = themeMode,
        panelWidthDp = panelWidth,
    )

    val actions = remember {
        object : SettingsActions {
            override fun back() = onBack()
            override fun toggleApp(packageName: String, enabled: Boolean) = Prefs.setAppEnabled(packageName, enabled)
            override fun setHandleEnabled(enabled: Boolean) { Prefs.overlayEnabled.value = enabled }
            override fun setHandleSide(onLeft: Boolean) { Prefs.handleOnLeft.value = onLeft }
            override fun setHandlePosition(position: Float) { Prefs.handleY.value = position }
            override fun setHandleSize(size: HandleSize) { Prefs.handleSize.value = size }
            override fun setGestureExclusion(enabled: Boolean) { Prefs.gestureExclusion.value = enabled }
            override fun setRetention(days: Int) {
                Prefs.retentionDays.value = days
                // 바뀐 기간을 바로 적용한다 (0 = 계속 보관이면 아무것도 지우지 않음).
                scope.launch(Dispatchers.IO) { chat.deleteOlderThan(days) }
            }
            override fun openPermissions() = onOpenPermissions()
            override fun openDiagnostics() = onOpenDiagnostics()
            override fun setLock(enabled: Boolean) {
                Prefs.lockEnabled.value = enabled
                AppLock.onSettingChanged(enabled)
            }
            override fun setHidePreviews(enabled: Boolean) {
                Prefs.hidePreviews.value = enabled
                WidgetUpdater.request(context)
            }
            override fun setDeletionAlerts(enabled: Boolean) { Prefs.deletionAlerts.value = enabled }
            override fun requestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            override fun setSavePhotos(enabled: Boolean) { Prefs.savePhotos.value = enabled }
            override fun addKeyword(keyword: String) {
                val k = keyword.trim()
                if (k.isNotEmpty()) Prefs.keywords.value = Prefs.keywords.value + k
            }
            override fun removeKeyword(keyword: String) { Prefs.keywords.value = Prefs.keywords.value - keyword }
            override fun unhideConversation(id: Long) {
                scope.launch { chat.setHidden(id, false); WidgetUpdater.request(context) }
            }
            override fun setThemeMode(mode: ThemeMode) { Prefs.themeMode.value = mode }
            override fun setPanelWidth(dp: Int) { Prefs.panelWidthDp.value = dp }
            override fun backup(password: String) {
                pendingPassword = password
                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                createBackup.launch("secretnoti-backup-$stamp.snb")
            }
            override fun restore(password: String) {
                pendingPassword = password
                openBackup.launch(arrayOf("*/*"))
            }
        }
    }

    SettingsScreen(state = state, actions = actions)
}

private fun installedLauncherApps(context: android.content.Context): List<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    return context.packageManager.queryIntentActivities(intent, 0)
        .map { it.activityInfo.packageName }
        .filter { it != context.packageName }
        .distinct()
}

/** 추천(알려진 메신저·메시지 알림을 보낸 적 있는 앱·이미 켠 앱)을 먼저, 나머지 설치 앱을 뒤에. */
private fun buildAppList(
    context: android.content.Context,
    enabled: Set<String>,
    seen: List<String>,
    launcher: List<String>,
): List<AppSettingUi> {
    val sms = TargetApps.defaultSmsPackage(context)
    val smsRow = AppSettingUi(
        packageName = TargetApps.SMS_TOKEN,
        label = context.getString(R.string.sms_label),
        enabled = TargetApps.SMS_TOKEN in enabled,
        detail = sms?.let { AppLabels.label(context, it) } ?: context.getString(R.string.sms_not_found),
        suggested = true,
    )
    fun isSms(p: String) = TargetApps.isSmsPackage(p, sms)
    val known = TargetApps.KNOWN_MESSENGERS.keys.filter { AppLabels.isInstalled(context, it) }
    val suggested = (known + seen + enabled.filter { it != TargetApps.SMS_TOKEN })
        .distinct().filter { !isSms(it) && it != context.packageName && AppLabels.isInstalled(context, it) }
    val others = launcher.filter { it !in suggested && !isSms(it) }

    fun row(pkg: String, suggestedRow: Boolean) = AppSettingUi(
        packageName = pkg,
        label = AppLabels.label(context, pkg),
        enabled = pkg in enabled,
        detail = when {
            pkg in seen -> context.getString(R.string.detail_seen)
            pkg in TargetApps.KNOWN_MESSENGERS -> context.getString(R.string.detail_messenger)
            else -> context.getString(R.string.detail_installed)
        },
        suggested = suggestedRow,
    )
    return listOf(smsRow) +
        suggested.map { row(it, true) }.sortedWith(compareByDescending<AppSettingUi> { it.enabled }.thenBy { it.label }) +
        others.map { row(it, false) }.sortedBy { it.label }
}
