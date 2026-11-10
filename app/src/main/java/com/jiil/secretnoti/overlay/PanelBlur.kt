package com.jiil.secretnoti.overlay

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager

/** 엣지 패널 뒤 화면 흐림. 안드로이드 12 이상의 창 뒤 흐림(blur behind)을 쓴다. */
object PanelBlur {
    const val RADIUS_DP = 40

    /** 갤럭시 전용 흐림(SemBlurInfo)이 있는가. 일반 흐림이 막혀 있어도 이것은 동작한다. */
    private val samsungAvailable: Boolean by lazy {
        runCatching {
            View::class.java.getMethod("semSetBlurInfo", Class.forName("android.view.SemBlurInfo"))
        }.isSuccess
    }

    /** 이 폰에 흐림 기능이 있는가 (안드로이드 12 이상 또는 갤럭시). */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || samsungAvailable

    /** 지금 흐림이 실제로 그려지는가. 일반 흐림은 절전 모드 등에서 꺼진다. */
    fun enabledNow(context: Context): Boolean = samsungAvailable ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled)

    /**
     * 갤럭시 전용 흐림(SemBlurInfo). 공개 API가 아니라서 리플렉션으로 시도하고, 없거나 막혀 있으면 조용히 넘어간다.
     * @return 적용에 성공했는가
     */
    fun trySamsungBlur(view: View, radiusPx: Int, cornerPx: Float): Boolean = runCatching {
        val builderClass = Class.forName("android.view.SemBlurInfo\$Builder")
        val builder = builderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(0) // BLUR_MODE_WINDOW
        builderClass.getMethod("setRadius", Int::class.javaPrimitiveType).invoke(builder, radiusPx)
        runCatching {
            builderClass.getMethod("setBackgroundCornerRadius", Float::class.javaPrimitiveType).invoke(builder, cornerPx)
        }
        val info = builderClass.getMethod("build").invoke(builder)
        View::class.java.getMethod("semSetBlurInfo", Class.forName("android.view.SemBlurInfo")).invoke(view, info)
        true
    }.getOrDefault(false)
}
