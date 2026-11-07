package com.jiil.secretnoti.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 손잡이 제스처 시험 결과. 시스템 뒤로 가기 제스처가 터치를 가져가는지(CANCEL) 확인하는 용도. */
object GestureLog {
    data class Entry(val at: Long, val message: String)

    private val state = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> get() = state

    fun add(message: String) = state.update { (listOf(Entry(System.currentTimeMillis(), message)) + it).take(50) }

    fun clear() {
        state.value = emptyList()
    }
}
