package com.jiil.secretnoti.ui.model

import androidx.annotation.StringRes
import com.jiil.secretnoti.R

import com.jiil.secretnoti.capture.Source

/** 화면 계층이 받는 데이터. 데이터 계층(Room)과 디자인 계층 사이의 계약. */
data class ConversationUi(
    val id: Long,
    val source: Source,
    /** 알림을 보낸 앱의 패키지. 출처 앱 아이콘을 그릴 때 쓴다. */
    val packageName: String,
    /** 앱 이름 ("카카오톡", "LINE", "WhatsApp" …). 출처 표시와 "{앱}에서 열기"에 쓴다. */
    val appLabel: String,
    val title: String,
    val isGroup: Boolean,
    val lastSender: String?,
    val preview: String,
    val lastAt: Long,
    val unread: Int,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    /** 상대가 삭제한(원문을 보관 중인) 메시지 수. */
    val deletedCount: Int = 0,
    /** 설정한 키워드가 들어간 메시지가 있다. */
    val hasKeyword: Boolean = false,
    /** 앱 잠금 중이라 내용(미리보기)을 가려야 한다. 이름·시간은 보여도 된다. */
    val previewHidden: Boolean = false,
    /** 메시지가 "(광고)"로 시작하는 등 광고로 보이는 대화방. "저장 끄기"를 권한다. */
    val looksLikeAd: Boolean = false,
    /** 저장을 끈 대화방. */
    val isMuted: Boolean = false,
    /** 저장을 끌 때 기록을 남겼는지 (false면 지웠음). */
    val hasRecords: Boolean = true,
)

data class MessageUi(
    val id: Long,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val at: Long,
    val deletedBySender: Boolean,
    /** 앱 안에 저장해 둔 사진 미리보기 파일의 절대 경로. 없으면 null. */
    val imagePath: String? = null,
)

/** 필터 칩·설정에서 쓰는 앱 하나. */
data class AppRef(
    val packageName: String,
    val label: String,
)

data class SearchResultUi(
    val conversationId: Long,
    val messageId: Long,
    val roomTitle: String,
    val packageName: String,
    val appLabel: String,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val at: Long,
    val deletedBySender: Boolean,
)

/** 원래 앱(카톡 등)에서 먼저 읽은 대화방을 어떻게 할지. 안 읽은 수는 어느 쪽이든 0이 된다. */
enum class ReadElsewhereAction(@StringRes val labelRes: Int) {
    REMOVE_FROM_PANEL(R.string.settings_read_elsewhere_remove),
    KEEP(R.string.settings_read_elsewhere_keep),
}

enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
}
