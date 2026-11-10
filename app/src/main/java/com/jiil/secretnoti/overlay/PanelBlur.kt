package com.jiil.secretnoti.overlay

import android.content.Context
import android.os.Build
import android.view.WindowManager

/** 엣지 패널 뒤 화면 흐림. 안드로이드 12 이상의 창 뒤 흐림(blur behind)을 쓴다. */
object PanelBlur {
    const val RADIUS_DP = 24

    /** 이 안드로이드 버전에 흐림 기능이 있는가. */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** 지금 흐림이 실제로 그려지는가. 절전 모드이거나 기기가 지원하지 않으면 false. */
    fun enabledNow(context: Context): Boolean =
        supported && context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled
}
