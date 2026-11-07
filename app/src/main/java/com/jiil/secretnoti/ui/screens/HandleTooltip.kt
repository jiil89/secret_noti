package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.design.*

@Composable
fun HandleTooltip() {
    Surface(color = NotiColors.Text, contentColor = Color.White,
        shape = RoundedCornerShape(10.dp), shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(NotiIcons.chevronLeft, null, Modifier.size(14.dp))
            Text("살짝 밀어서 새 메시지 보기", style = MaterialTheme.typography.labelMedium)
        }
    }
}
