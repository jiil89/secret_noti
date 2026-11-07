package com.jiil.secretnoti.overlay

import com.jiil.secretnoti.NotiApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 손잡이 제스처 시험 결과. 시스템 뒤로 가기 제스처가 터치를 가져가는지(CANCEL) 확인하는 용도.
 * 개발 PC에서 `adb exec-out run-as com.jiil.secretnoti cat files/gesture.log`로 읽을 수 있게 파일에도 남긴다.
 */
object GestureLog {
    data class Entry(val at: Long, val message: String)

    private val state = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> get() = state

    private val file by lazy { File(NotiApp.instance.filesDir, "gesture.log") }
    private val stamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)

    fun add(message: String) {
        val now = System.currentTimeMillis()
        state.update { (listOf(Entry(now, message)) + it).take(50) }
        runCatching { file.appendText("${stamp.format(Date(now))}  $message\n") }
    }

    fun clear() {
        state.value = emptyList()
        runCatching { file.delete() }
    }
}
