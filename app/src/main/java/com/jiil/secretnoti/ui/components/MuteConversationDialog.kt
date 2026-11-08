package com.jiil.secretnoti.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.R

@Composable
fun MuteConversationDialog(
    title: String,
    onConfirm: (deleteExisting: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteExisting by rememberSaveable(title) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conversation_mute_title, title)) },
        text = { MuteChoices(deleteExisting) { deleteExisting = it } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.conversations_cancel)) } },
        confirmButton = { TextButton(onClick = { onConfirm(deleteExisting) }) {
            Text(stringResource(R.string.conversation_mute))
        } },
    )
}

@Composable
private fun MuteChoices(deleteExisting: Boolean, onChange: (Boolean) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.conversation_mute_body))
        Spacer(Modifier.height(12.dp))
        Column(Modifier.selectableGroup()) {
            listOf(true, false).forEach { delete ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(deleteExisting == delete, role = Role.RadioButton,
                        onClick = { onChange(delete) }),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = deleteExisting == delete, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (delete) R.string.conversation_mute_delete_existing
                        else R.string.conversation_mute_keep_existing))
                }
            }
        }
    }
}

/**
 * 엣지패널(다른 앱 위 오버레이 창)용. 오버레이에는 Activity가 없어서 Dialog 창을 띄울 수 없으므로
 * 같은 내용을 패널 안 카드로 보여준다.
 */
@Composable
fun MuteConversationCard(
    title: String,
    onConfirm: (deleteExisting: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteExisting by rememberSaveable(title) { mutableStateOf(false) }
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.conversation_mute_title, title), style = MaterialTheme.typography.titleMedium)
            MuteChoices(deleteExisting) { deleteExisting = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.conversations_cancel)) }
                TextButton(onClick = { onConfirm(deleteExisting) }) { Text(stringResource(R.string.conversation_mute)) }
            }
        }
    }
}
