package com.jiil.secretnoti

import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.capture.TargetApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAppsTest {
    @Test
    fun classifiesTargets() {
        assertEquals(Source.KAKAO, TargetApps.classify("com.kakao.talk", null))
        assertEquals(Source.TEAMS, TargetApps.classify("com.microsoft.teams", null))
        assertEquals(Source.SMS, TargetApps.classify("com.samsung.android.messaging", null))
        assertEquals(Source.SMS, TargetApps.classify("com.example.sms", "com.example.sms"))
        assertEquals(Source.OTHER, TargetApps.classify("jp.naver.line.android", "com.samsung.android.messaging"))
    }

    @Test
    fun enabledSetUsesSmsTokenForDefaultSmsApp() {
        val enabled = setOf(TargetApps.KAKAO, TargetApps.SMS_TOKEN)
        assertTrue(TargetApps.isEnabled("com.kakao.talk", enabled, "com.samsung.android.messaging"))
        assertTrue(TargetApps.isEnabled("com.samsung.android.messaging", enabled, "com.samsung.android.messaging"))
        assertFalse(TargetApps.isEnabled("com.microsoft.teams", enabled, null))
    }
}
