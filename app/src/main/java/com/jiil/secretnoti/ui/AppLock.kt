package com.jiil.secretnoti.ui

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.jiil.secretnoti.R
import com.jiil.secretnoti.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 잠금. 폰의 지문·얼굴·화면 잠금(PIN·패턴)으로 연다.
 * 앱을 처음 열 때와, 다른 앱으로 나갔다가 30초 넘게 지나서 돌아올 때 잠근다.
 */
object AppLock {
    private const val GRACE_MS = 30_000L

    private val state = MutableStateFlow(true)
    val locked: StateFlow<Boolean> get() = state

    private var leftAt = 0L

    fun isDeviceSecure(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /** 잠금을 써야 하는 상태인가 (설정을 켰고 폰에 화면 잠금이 있음). */
    fun isActive(context: Context) = Prefs.lockEnabled.value && isDeviceSecure(context)

    fun onAppStart(context: Context) {
        if (!isActive(context)) {
            state.value = false
        } else if (leftAt != 0L && System.currentTimeMillis() - leftAt > GRACE_MS) {
            state.value = true
        }
    }

    fun onAppStop() {
        leftAt = System.currentTimeMillis()
    }

    /** 설정에서 잠금을 켜거나 끌 때. 켜는 순간에는 이미 앱 안에 있으니 열린 상태로 둔다. */
    fun onSettingChanged(enabled: Boolean) {
        if (!enabled) state.value = false
    }

    fun authenticate(activity: FragmentActivity, onError: (String?) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    state.value = false
                    onError(null)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    onError(if (cancelled) null else errString.toString())
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.lock_prompt_title))
                .setSubtitle(activity.getString(R.string.lock_prompt_subtitle))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build(),
        )
    }
}
