package com.jiil.secretnoti.ui

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
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.SettingsUi
import com.jiil.secretnoti.ui.model.SourceSettingUi
import com.jiil.secretnoti.ui.screens.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 설정 화면과 Prefs·저장소를 잇는다. */
@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenPermissions: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissions by remember { mutableStateOf(readPermissionSummary(context)) }
    var smsPackage by remember { mutableStateOf(TargetApps.defaultSmsPackage(context)) }
    LifecycleResumeEffect(Unit) {
        permissions = readPermissionSummary(context)
        smsPackage = TargetApps.defaultSmsPackage(context)
        onPauseOrDispose { }
    }

    val kakao by Prefs.sourceKakao.flow.collectAsStateWithLifecycle()
    val sms by Prefs.sourceSms.flow.collectAsStateWithLifecycle()
    val teams by Prefs.sourceTeams.flow.collectAsStateWithLifecycle()
    val handleEnabled by Prefs.overlayEnabled.flow.collectAsStateWithLifecycle()
    val handleOnLeft by Prefs.handleOnLeft.flow.collectAsStateWithLifecycle()
    val handleY by Prefs.handleY.flow.collectAsStateWithLifecycle()
    val handleSize by Prefs.handleSize.flow.collectAsStateWithLifecycle()
    val exclusion by Prefs.gestureExclusion.flow.collectAsStateWithLifecycle()
    val retention by Prefs.retentionDays.flow.collectAsStateWithLifecycle()

    fun installed(pkg: String) = if (context.isPackageInstalled(pkg)) "설치됨" else "설치 안 됨"

    val state = SettingsUi(
        sources = listOf(
            SourceSettingUi(Source.KAKAO, TargetApps.KAKAO, kakao, installed(TargetApps.KAKAO)),
            SourceSettingUi(Source.SMS, smsPackage, sms, smsPackage?.let { "기본 문자 앱 사용" } ?: "기본 문자 앱을 찾지 못했어요"),
            SourceSettingUi(Source.TEAMS, TargetApps.TEAMS, teams, installed(TargetApps.TEAMS)),
        ),
        handleEnabled = handleEnabled,
        handleOnLeft = handleOnLeft,
        handlePosition = handleY,
        handleSize = handleSize,
        gestureExclusion = exclusion,
        retentionDays = retention,
        permissions = permissions,
    )

    SettingsScreen(
        state = state,
        onBack = onBack,
        onSourceToggle = { source, on -> Prefs.sourcePref(source).value = on },
        onHandleEnabledChange = { Prefs.overlayEnabled.value = it },
        onHandleSideChange = { Prefs.handleOnLeft.value = it },
        onHandlePositionChange = { Prefs.handleY.value = it },
        onHandleSizeChange = { Prefs.handleSize.value = it },
        onGestureExclusionChange = { Prefs.gestureExclusion.value = it },
        onRetentionChange = { days ->
            Prefs.retentionDays.value = days
            // 바뀐 기간을 바로 적용한다 (0 = 계속 보관이면 아무것도 지우지 않음).
            scope.launch(Dispatchers.IO) { NotiApp.instance.chat.deleteOlderThan(days) }
        },
        onOpenPermissions = onOpenPermissions,
        onOpenDiagnostics = onOpenDiagnostics,
    )
}
