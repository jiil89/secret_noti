package com.jiil.secretnoti.capture

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import java.util.concurrent.ConcurrentHashMap

/** 저장 방식이 달라지는 앱 종류. 카톡·문자·Teams 외의 앱은 OTHER(표준 메시지 알림으로 처리). */
enum class Source(val label: String) {
    KAKAO("카카오톡"),
    SMS("문자"),
    TEAMS("Teams"),
    OTHER("기타"),
}

object TargetApps {
    const val KAKAO = "com.kakao.talk"
    const val TEAMS = "com.microsoft.teams"

    /** 설정의 대상 앱 목록에서 "기본 문자 앱"을 가리키는 표시. 사용자가 문자 앱을 바꿔도 따라간다. */
    const val SMS_TOKEN = "sms"

    val KNOWN_SMS = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.mms",
    )

    /** 추천 목록에 먼저 보여줄 메신저 (설치돼 있을 때만). */
    val KNOWN_MESSENGERS = linkedMapOf(
        KAKAO to "카카오톡",
        TEAMS to "Teams",
        "jp.naver.line.android" to "LINE",
        "com.whatsapp" to "WhatsApp",
        "org.telegram.messenger" to "Telegram",
        "com.instagram.android" to "Instagram",
        "com.facebook.orca" to "Messenger",
        "com.discord" to "Discord",
        "com.Slack" to "Slack",
        "com.nhn.android.band" to "BAND",
        "org.thoughtcrime.securesms" to "Signal",
        "com.google.android.apps.dynamite" to "Google Chat",
        "com.viber.voip" to "Viber",
        "com.tencent.mm" to "WeChat",
        "com.snapchat.android" to "Snapchat",
        "com.towneers.www" to "당근",
    )

    val DEFAULT_ENABLED = setOf(KAKAO, TEAMS, SMS_TOKEN)

    fun defaultSmsPackage(context: Context): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    fun isSmsPackage(packageName: String, defaultSms: String?) =
        packageName == defaultSms || packageName in KNOWN_SMS

    fun classify(packageName: String, defaultSms: String?): Source = when {
        packageName == KAKAO -> Source.KAKAO
        packageName == TEAMS -> Source.TEAMS
        isSmsPackage(packageName, defaultSms) -> Source.SMS
        else -> Source.OTHER
    }

    fun sourceOf(context: Context, packageName: String): Source =
        classify(packageName, defaultSmsPackage(context))

    /** 설정에서 켠 앱인가. 기본 문자 앱은 [SMS_TOKEN]으로 켠다. */
    fun isEnabled(packageName: String, enabled: Set<String>, defaultSms: String?): Boolean =
        packageName in enabled || (SMS_TOKEN in enabled && isSmsPackage(packageName, defaultSms))
}

/** 설치된 앱 이름. PackageManager 조회 결과를 기억해 둔다. */
object AppLabels {
    private val cache = ConcurrentHashMap<String, String>()

    fun label(context: Context, packageName: String): String = cache.getOrPut(packageName) {
        runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        }.getOrNull() ?: TargetApps.KNOWN_MESSENGERS[packageName] ?: packageName.substringAfterLast('.')
    }

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
