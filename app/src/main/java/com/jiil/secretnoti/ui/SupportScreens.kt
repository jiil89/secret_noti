package com.jiil.secretnoti.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.R
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.ContactKind
import kotlinx.coroutines.launch

/** 설정 → 도움말: 실제로 묻게 될 질문만, 누르면 펼쳐진다. */
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onReplayIntro: () -> Unit,
    onOpenPermissions: () -> Unit,
    onContact: (ContactKind) -> Unit,
) {
    BackScaffold(stringResource(R.string.help_title), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onReplayIntro, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(R.string.help_replay_intro), color = NotiColors.NoticeForeground)
            }
            HelpItem(R.string.help_q_not_saved, R.string.help_a_not_saved, initiallyOpen = true,
                action = R.string.help_check_permissions to onOpenPermissions)
            HelpItem(R.string.help_q_read, R.string.help_a_read)
            HelpItem(R.string.help_q_stuck, R.string.help_a_stuck)
            HelpItem(R.string.help_q_handle, R.string.help_a_handle)
            HelpItem(R.string.help_q_menu, R.string.help_a_menu)
            HelpItem(R.string.help_q_apps, R.string.help_a_apps,
                action = R.string.help_request_app to { onContact(ContactKind.NEW_APP) })
            HelpItem(R.string.help_q_storage, R.string.help_a_storage)
            HelpItem(R.string.help_q_deleted, R.string.help_a_deleted)
            OutlinedButton(onClick = { onContact(ContactKind.BUG) }, shape = NotiShapes.banner,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = NotiSpacing.button)) {
                Text(stringResource(R.string.help_contact))
            }
        }
    }
}

@Composable
private fun HelpItem(question: Int, answer: Int, initiallyOpen: Boolean = false, action: Pair<Int, () -> Unit>? = null) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    Surface(shape = NotiShapes.banner, color = NotiColors.Background, border = BorderStroke(1.dp, NotiColors.Divider)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().toggleable(open, role = Role.Button) { open = it }
                .heightIn(min = NotiSpacing.button).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(question), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    color = NotiColors.Text)
                Text(if (open) "–" else "+", color = NotiColors.Secondary, style = MaterialTheme.typography.titleMedium)
            }
            if (open) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(answer), style = MaterialTheme.typography.bodyMedium, color = NotiColors.Body)
                action?.let { (label, onClick) ->
                    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(label), color = NotiColors.NoticeForeground)
                    }
                }
            }
        }
    }
}

/** 설정 → 문의하기에서 올라오는 선택 창. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSheet(onPick: (ContactKind) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NotiColors.Background) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.contact_sheet_title), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp))
            ContactKind.entries.forEach { kind ->
                Row(Modifier.fillMaxWidth().clip(NotiShapes.banner)
                    .border(1.dp, NotiColors.Divider, NotiShapes.banner)
                    .clickable(role = Role.Button) { onPick(kind) }
                    .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(32.dp).background(NotiColors.DetailBackground, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(kind.icon(), null, Modifier.size(18.dp), tint = NotiColors.Body)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(kind.labelRes), style = MaterialTheme.typography.bodyLarge, color = NotiColors.Text)
                        kind.hintRes?.let {
                            Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = NotiColors.Secondary)
                        }
                    }
                }
            }
        }
    }
}

private fun ContactKind.icon(): ImageVector = when (this) {
    ContactKind.BUG -> NotiIcons.bug
    ContactKind.NEW_APP -> NotiIcons.plus
    ContactKind.OTHER -> NotiIcons.message
}

/** 문의 종류를 고른 뒤: 진단 기록 첨부 여부를 고르고 메일 앱을 연다. */
@Composable
fun ContactScreen(kind: ContactKind, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 새 앱 요청은 그 앱 알림 모양을 봐야 하므로 기본으로 첨부한다. 버그·기타는 사용자가 켤 때만.
    var attach by rememberSaveable { mutableStateOf(kind == ContactKind.NEW_APP) }
    val dumpAll by Prefs.dumpAllApps.flow.collectAsStateWithLifecycle()
    LaunchedEffect(kind) {
        // 새 앱 요청: 지금 쓰는 대상 앱이 아닌 알림도 24시간 기록해야 그 앱 알림을 볼 수 있다.
        if (kind == ContactKind.NEW_APP && !Prefs.dumpAllApps.value) {
            Prefs.dumpAllAppsSince.value = System.currentTimeMillis()
            Prefs.dumpAllApps.value = true
        }
    }
    BackScaffold(stringResource(kind.labelRes), onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (kind == ContactKind.NEW_APP) {
                Text(stringResource(R.string.contact_new_app_guide), style = MaterialTheme.typography.bodyMedium,
                    color = NotiColors.Body)
                SupportSwitch(stringResource(R.string.route_dump_all), null, dumpAll) {
                    if (it) Prefs.dumpAllAppsSince.value = System.currentTimeMillis()
                    Prefs.dumpAllApps.value = it
                }
            }
            SupportSwitch(stringResource(R.string.contact_attach), stringResource(R.string.contact_attach_hint), attach) { attach = it }
            if (attach) Surface(shape = NotiShapes.banner, color = NotiColors.NoticeBackground) {
                Text(stringResource(R.string.contact_attach_warning), Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall, color = NotiColors.NoticeForeground)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.contact_device_info), style = MaterialTheme.typography.bodyLarge, color = NotiColors.Text)
                Text(stringResource(R.string.contact_device_info_hint), style = MaterialTheme.typography.bodyMedium,
                    color = NotiColors.Secondary)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { scope.launch { SupportMail.compose(context, kind, attach) } }, shape = NotiShapes.banner,
                modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                Text(stringResource(R.string.contact_write), fontWeight = FontWeight.Bold)
            }
            Text(SupportMail.ADDRESS, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                color = NotiColors.Secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SupportSwitch(label: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
        .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = NotiColors.Text)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = NotiColors.Secondary)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
