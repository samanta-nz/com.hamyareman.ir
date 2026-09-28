package com.hamyareman.ir.platform.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.hamyareman.ir.platform.core.common.LocalStore

/**
 * بازکردن قفل با بیومتریک (اثر انگشت / چهره).
 *
 * مدل امنیتی — صریح و ساده:
 * - **PIN منبع اعتبار است** ([AppLock] با PBKDF2)؛ بیومتریک فقط «راه راحت‌ترِ بازکردن همان قفل» است.
 * - بیومتریک بدون PIN فعال نمی‌شود، چون اگر PIN نباشد قفل‌کردن معنی ندارد.
 * - داده‌های بیومتریک هرگز از دستگاه بیرون نمی‌روند و در اپ هم ذخیره نمی‌شوند؛ ما فقط
 *   نتیجه‌ی `SUCCESS` را می‌گیریم و `AppLock.markUnlocked()` را صدا می‌زنیم.
 * - دکمه‌ی منفی پرامپت «واردکردن PIN» است تا اگر حسگر کار نکرد، کاربر پشت در نماند.
 *
 * نکته: `BIOMETRIC_WEAK` هم مجاز است تا دستگاه‌هایی که فقط تشخیص چهره‌ی نرم‌افزاری دارند
 * (مثل خیلی از گوشی‌های میان‌رده) از قابلیت محروم نشوند؛ اگر سیاست سخت‌گیرانه خواستی،
 * [AUTHENTICATORS] را فقط به `BIOMETRIC_STRONG` تغییر بده.
 */
class BiometricUnlock(
    private val store: LocalStore,
    private val lock: AppLock,
) {

    /** وضعیت سخت‌افزار/ثبت روی دستگاه — برای پیام فارسی در UI. */
    fun status(context: Context): BiometricStatus =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NO_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.NEEDS_SECURITY_UPDATE
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.UNSUPPORTED
            else -> BiometricStatus.UNAVAILABLE
        }

    fun isEnabled(): Boolean = store.getBool(KEY_ENABLED, false)

    /** آیا دکمه‌ی بیومتریک باید روی صفحه‌ی قفل دیده شود؟ */
    fun shouldOffer(context: Context): Boolean =
        isEnabled() && lock.hasPin() && status(context) == BiometricStatus.READY

    /**
     * @return true یعنی تغییر اعمال شد؛ false یعنی شرط‌ها برقرار نبود (PIN نیست یا سخت‌افزار آماده نیست).
     */
    fun setEnabled(enabled: Boolean, context: Context): Boolean {
        if (!enabled) {
            store.putBool(KEY_ENABLED, false)
            return true
        }
        if (!lock.hasPin() || status(context) != BiometricStatus.READY) return false
        store.putBool(KEY_ENABLED, true)
        return true
    }

    /** وقتی PIN پاک می‌شود، بیومتریک هم باید خاموش شود تا تنظیمات متناقض نماند. */
    fun clear() {
        store.remove(KEY_ENABLED)
    }

    companion object {
        private const val KEY_ENABLED = "biometric_unlock_enabled"
        val AUTHENTICATORS: Int =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}

enum class BiometricStatus(val fa: String) {
    READY("این دستگاه حسگر دارد و اثر انگشت/چهره ثبت شده است."),
    NO_ENROLLED("اول در تنظیمات خودِ گوشی، اثر انگشت یا چهره‌ات را ثبت کن؛ بعد این کلید روشن می‌شود."),
    NO_HARDWARE("این دستگاه حسگر اثر انگشت/چهره‌ی پشتیبانی‌شده ندارد."),
    HW_UNAVAILABLE("حسگر الان در دسترس نیست (مثلاً دستگاه تازه روشن شده). بعداً دوباره تلاش کن."),
    NEEDS_SECURITY_UPDATE("گوشی یک به‌روزرسانی امنیتی لازم دارد تا بیومتریک کار کند."),
    UNSUPPORTED("این نسخه‌ی اندروید از بیومتریک با این تنظیمات پشتیبانی نمی‌کند."),
    UNAVAILABLE("بیومتریک الان در دسترس نیست؛ می‌توانی با PIN باز کنی."),
}

/**
 * اجرای پرامپت بیومتریک. جدا از [BiometricUnlock] است تا منطق ذخیره‌سازی قابل تست بماند
 * و این آبجکت فقط به اندروید/APIها وابسته باشد.
 *
 * حتماً از روی نخ اصلی صدا زده شود (BiometricPrompt همین را می‌خواهد).
 */
object BiometricPromptRunner {

    fun show(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeText: String = "واردکردن PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    // یعنی «شناسایی نشد» — پرامپت باز می‌ماند و خودِ سیستم پیام می‌دهد.
                    onError("شناسایی نشد. دوباره انگشت/چهره‌ات را درست بگیر.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (isCancel(errorCode)) {
                        onCancelled()
                    } else {
                        onError(faMessage(errorCode))
                    }
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricUnlock.AUTHENTICATORS)
            .setNegativeButtonText(negativeText)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }

    private fun isCancel(errorCode: Int): Boolean = when (errorCode) {
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_CANCELED,
        -> true
        else -> false
    }

    private fun faMessage(errorCode: Int): String = when (errorCode) {
        BiometricPrompt.ERROR_HW_UNAVAILABLE ->
            "حسگر الان در دسترس نیست. کمی صبر کن یا با PIN باز کن."
        BiometricPrompt.ERROR_UNABLE_TO_PROCESS ->
            "پردازش بیومتریک ناموفق بود. دوباره تلاش کن."
        BiometricPrompt.ERROR_TIMEOUT -> "وقت تمام شد. دوباره تلاش کن."
        BiometricPrompt.ERROR_LOCKOUT ->
            "چند بار اشتباه شد؛ حسگر موقتاً قفل است. با PIN باز کن."
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
            "بیومتریک تا بازکردن قفل خودِ گوشی (PIN/الگو/پترن) کار نمی‌کند. اول گوشی را باز کن."
        BiometricPrompt.ERROR_VENDOR -> "خطای حسگر از سمت سازنده‌ی دستگاه."
        BiometricPrompt.ERROR_NO_BIOMETRICS ->
            "اثر انگشت یا چهره‌ای روی این گوشی ثبت نشده است."
        BiometricPrompt.ERROR_HW_NOT_PRESENT -> "این دستگاه حسگر بیومتریک ندارد."
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED ->
            "برای بیومتریک باید به‌روزرسانی امنیتی گوشی نصب شود."
        else -> "بیومتریک ناموفق بود. می‌توانی با PIN باز کنی."
    }
}
