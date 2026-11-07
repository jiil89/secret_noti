package com.jiil.secretnoti.ui.model

import com.jiil.secretnoti.capture.Source

/** 화면 계층이 받는 데이터. 데이터 계층(Room)과 디자인 계층 사이의 계약. */
data class ConversationUi(
    val id: Long,
    val source: Source,
    val title: String,
    val isGroup: Boolean,
    val lastSender: String?,
    val preview: String,
    val lastAt: Long,
    val unread: Int,
)

data class MessageUi(
    val id: Long,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val at: Long,
    val deletedBySender: Boolean,
)

enum class SourceFilter { ALL, KAKAO, SMS, TEAMS }
