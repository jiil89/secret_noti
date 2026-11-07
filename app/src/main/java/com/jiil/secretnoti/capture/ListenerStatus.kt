package com.jiil.secretnoti.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object ListenerStatus {
    val connected = MutableStateFlow(false)

    private val refreshTick = MutableStateFlow(0)

    /** 앱으로 돌아올 때마다 올라간다. "다른 앱 위에 표시" 권한이 바뀌었는지 다시 확인하는 신호. */
    val refresh: StateFlow<Int> get() = refreshTick

    fun requestRefresh() = refreshTick.update { it + 1 }
}
