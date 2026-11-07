package com.jiil.secretnoti.ui

import com.jiil.secretnoti.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.capture.AppLabels
import com.jiil.secretnoti.data.NotificationDump
import com.jiil.secretnoti.settings.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DumpScreen() {
    val context = LocalContext.current
    val dao = remember { NotiApp.instance.db.dumpDao() }
    val dumps by remember { dao.observeRecent(300) }.collectAsStateWithLifecycle(emptyList())
    val count by remember { dao.observeCount() }.collectAsStateWithLifecycle(0)
    val dumpAll by Prefs.dumpAllApps.flow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val n = runCatching { exportDumps(context, uri) }
            Toast.makeText(
                context,
                n.fold({ context.resources.getQuantityString(R.plurals.route_dump_saved, it, it) }, { context.getString(R.string.route_dump_failed, it.message ?: context.getString(R.string.route_unknown_error)) }),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.route_dump_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.route_dump_all), Modifier.weight(1f))
                    Switch(checked = dumpAll, onCheckedChange = { Prefs.dumpAllApps.value = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("noti-dumps-$stamp.json")
                    }) { Text(pluralStringResource(R.plurals.route_export_json, count, count)) }
                    OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { dao.clear() } }) { Text(stringResource(R.string.route_clear_all)) }
                }
            }
        }
        items(dumps, key = { it.id }) { dump ->
            DumpRow(dump, expanded == dump.id) { expanded = if (expanded == dump.id) null else dump.id }
        }
    }
}

private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)

@Composable
private fun DumpRow(dump: NotificationDump, expanded: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(timeFormat.format(Date(dump.receivedAt)), style = MaterialTheme.typography.labelSmall)
                Text(
                    AppLabels.label(LocalContext.current, dump.packageName),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (dump.event == "posted") stringResource(R.string.route_posted) else stringResource(R.string.route_removed),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dump.event == "posted") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val isSummary = remember(dump.id) { dump.json.contains("\"isGroupSummary\": true") }
            Text(
                when {
                    isSummary -> stringResource(R.string.route_group_summary)
                    else -> dump.title ?: stringResource(R.string.route_no_title)
                },
                fontWeight = FontWeight.Bold,
                color = if (isSummary) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(dump.text ?: "", maxLines = if (expanded) Int.MAX_VALUE else 2)
            if (expanded) {
                SelectionContainer {
                    Text(dump.json, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

private suspend fun exportDumps(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
    val all = NotiApp.instance.db.dumpDao().all()
    val array = JSONArray()
    all.forEach { array.put(JSONObject(it.json)) }
    context.contentResolver.openOutputStream(uri)!!.use { it.write(array.toString(2).toByteArray()) }
    all.size
}
