package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.design.*

@Composable
fun LockScreen(onUnlock: () -> Unit, error: String?) {
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(NotiIcons.lock, null, Modifier.size(48.dp), tint = NotiColors.Accent)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.lock_screen_app_name), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lock_screen_locked), color = NotiColors.Secondary)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.lock_screen_unlock)) }
            if (error != null) Text(error, Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
