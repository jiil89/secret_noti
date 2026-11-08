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
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.conversation_mute_body))
                Spacer(Modifier.height(12.dp))
                Column(Modifier.selectableGroup()) {
                    listOf(true, false).forEach { delete ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(deleteExisting == delete, role = Role.RadioButton,
                                onClick = { deleteExisting = delete }),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = deleteExisting == delete, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (delete) R.string.conversation_mute_delete_existing
                                else R.string.conversation_mute_keep_existing))
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.conversations_cancel)) } },
        confirmButton = { TextButton(onClick = { onConfirm(deleteExisting) }) {
            Text(stringResource(R.string.conversation_mute))
        } },
    )
}
