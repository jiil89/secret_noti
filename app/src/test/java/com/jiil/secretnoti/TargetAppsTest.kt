package com.jiil.secretnoti

import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.capture.TargetApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetAppsTest {
    @Test
    fun classifiesTargets() {
        assertEquals(Source.KAKAO, TargetApps.classify("com.kakao.talk", null))
        assertEquals(Source.TEAMS, TargetApps.classify("com.microsoft.teams", null))
        assertEquals(Source.SMS, TargetApps.classify("com.samsung.android.messaging", null))
        assertEquals(Source.SMS, TargetApps.classify("com.example.sms", "com.example.sms"))
        assertNull(TargetApps.classify("com.google.android.youtube", "com.samsung.android.messaging"))
    }
}
