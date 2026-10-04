package com.hamyareman.ir.ui.safespace

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.security.AppLock
import com.hamyareman.ir.platform.core.security.BiometricUnlock

/** سیاست پایان نشستِ مستقل فضای امن. */
enum class SafeExitPolicy(val wire: String, val titleFa: String, val timeoutMs: Long?) {
    IMMEDIATE("immediate", "فوری پس از خروج", 0L),
    SCREEN_LOCK("screen_lock", "با قفل‌شدن صفحه", null),
    ONE_MINUTE("1m", "۱ دقیقه پس از خروج", 60_000L),
    TWO_MINUTES("2m", "۲ دقیقه پس از خروج", 120_000L),
    THREE_MINUTES("3m", "۳ دقیقه پس از خروج", 180_000L),
    ;

    companion object {
        fun fromWire(value: String): SafeExitPolicy = entries.firstOrNull { it.wire == value } ?: IMMEDIATE
    }
}

/**
 * نشست فضای امن کاملاً جدا از قفل کل برنامه است.
 *
 * PIN/بیومتریک در SharedPreferences جدا ذخیره می‌شوند و وضعیت unlocked فقط در RAM است؛
 * بنابراین مرگ process همیشه نشست را می‌بندد. زمان‌های ۱/۲/۳ دقیقه فقط از لحظه‌ای
 * محاسبه می‌شوند که route امن واقعاً ترک شود، نه هنگام کار داخل آن.
 */
object SafeSpaceSession {
    private const val STORE = "hamyar_safe_space_lock"
    private const val KEY_POLICY = "safe_exit_policy"

    private var unlocked by mutableStateOf(false)
    @Volatile private var exitedAtMs = 0L
    @Volatile private var previousRoute: String? = null

    fun store(context: Context): LocalStore = LocalStore(context.applicationContext, STORE)
    fun lock(context: Context): AppLock = AppLock(store(context))
    fun biometric(context: Context): BiometricUnlock {
        val local = store(context)
        return BiometricUnlock(local, AppLock(local))
    }

    fun policy(context: Context): SafeExitPolicy =
        SafeExitPolicy.fromWire(store(context).getString(KEY_POLICY, SafeExitPolicy.IMMEDIATE.wire))

    fun setPolicy(context: Context, value: SafeExitPolicy) {
        store(context).putString(KEY_POLICY, value.wire)
        if (value == SafeExitPolicy.IMMEDIATE && exitedAtMs > 0L) forceLock()
    }

    fun markUnlocked() {
        unlocked = true
        exitedAtMs = 0L
    }

    fun isUnlocked(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!unlocked || !lock(context).hasPin()) return false
        val exited = exitedAtMs
        val timeout = policy(context).timeoutMs
        if (exited > 0L && timeout != null && nowMs - exited >= timeout) {
            forceLock()
            return false
        }
        return true
    }

    fun forceLock() {
        unlocked = false
        exitedAtMs = 0L
    }

    /** تغییر مقصد NavHost؛ خروج بین زیرصفحه‌های امن نشست را نمی‌بندد. */
    fun onRouteChanged(context: Context, route: String?) {
        val before = previousRoute
        val wasSafe = isSafeRoute(before)
        val isSafe = isSafeRoute(route)
        if (wasSafe && !isSafe && unlocked) {
            when (policy(context)) {
                SafeExitPolicy.IMMEDIATE -> forceLock()
                SafeExitPolicy.SCREEN_LOCK -> exitedAtMs = 0L
                else -> exitedAtMs = System.currentTimeMillis()
            }
        } else if (isSafe && unlocked) {
            // بازگشت در مهلت: شمارنده فقط هنگام خروج معنا دارد.
            if (isUnlocked(context)) exitedAtMs = 0L
        }
        previousRoute = route
    }

    /**
     * تعویض سادهٔ برنامه در سیاست «قفل صفحه» نشست را نمی‌بندد؛ فقط خاموش یا
     * واقعاً قفل‌شدن نمایشگر معتبر است. گیرندهٔ ACTION_SCREEN_OFF نیز همین رویداد
     * را به [onScreenLocked] می‌رساند تا ترتیب callbackها در گوشی‌ها مهم نباشد.
     */
    fun onAppBackgrounded(context: Context) {
        if (!unlocked || policy(context) != SafeExitPolicy.SCREEN_LOCK) return
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (power?.isInteractive == false || keyguard?.isKeyguardLocked == true) forceLock()
    }

    fun onScreenLocked(context: Context) {
        if (unlocked && policy(context) == SafeExitPolicy.SCREEN_LOCK) forceLock()
    }

    fun isSafeRoute(route: String?): Boolean {
        val value = route?.substringBefore('?') ?: return false
        return value in setOf(
            "safespace", "safe-free-writing", "secure-gallery", "journal", "writing", "diary", "notebooks", "poetry",
        )
    }
}
