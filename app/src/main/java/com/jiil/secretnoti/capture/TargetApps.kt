package com.jiil.secretnoti.capture

import android.content.Context
import android.provider.Telephony

enum class Source(val label: String) {
    KAKAO("카카오톡"),
    SMS("문자"),
    TEAMS("Teams"),
}

object TargetApps {
    const val KAKAO = "com.kakao.talk"
    const val TEAMS = "com.microsoft.teams"
    val KNOWN_SMS = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.mms",
    )

    fun defaultSmsPackage(context: Context): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    fun classify(packageName: String, defaultSms: String?): Source? = when {
        packageName == KAKAO -> Source.KAKAO
        packageName == TEAMS -> Source.TEAMS
        packageName == defaultSms || packageName in KNOWN_SMS -> Source.SMS
        else -> null
    }

    fun sourceOf(context: Context, packageName: String): Source? =
        classify(packageName, defaultSmsPackage(context))
}
