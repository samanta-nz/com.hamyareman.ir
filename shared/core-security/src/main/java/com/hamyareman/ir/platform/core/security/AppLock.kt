package com.hamyareman.ir.platform.core.security

import com.hamyareman.ir.platform.core.common.LocalStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * قفل اپ با PIN.
 *
 * - هش با PBKDF2-HMAC-SHA256 و ۱۲۰٬۰۰۰ تکرار + نمک تصادفی ۱۶ بایتی.
 * - مقایسه‌ی زمان‌ثابت تا از timing attack جلوگیری شود.
 * - PIN هرگز ذخیره نمی‌شود؛ فقط هش و نمک.
 *
 * بیومتریک (اثر انگشت/چهره) لایه‌ی راحتی است، نه لایه‌ی امنیت: وقتی اضافه شد باید با
 * `androidx.biometric:biometric` و فقط به‌عنوان جایگزین «باز کردن قفل» استفاده شود و
 * کلید واقعی داده‌ها در Android Keystore بماند (نگاه کنید به [Encryptor]).
 */
class AppLock(private val store: LocalStore) {

    private var unlockedAtMs: Long = 0L

    fun hasPin(): Boolean = store.getString(KEY_HASH).isNotBlank()

    fun isEnabled(): Boolean = hasPin() && store.getBool(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        store.putBool(KEY_ENABLED, enabled && hasPin())
    }

    /** @return پیام خطای فارسی یا null یعنی معتبر است. */
    fun validatePin(pin: String): String? = when {
        pin.length !in MIN_PIN..MAX_PIN -> "PIN باید بین $MIN_PIN تا $MAX_PIN رقم باشد."
        pin.any { !it.isDigit() } -> "فقط رقم مجاز است."
        pin.toSet().size == 1 -> "یه PIN تکراری مثل این خیلی راحت حدس زده می‌شود."
        else -> null
    }

    fun setPin(pin: String): Boolean {
        if (validatePin(pin) != null) return false
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        store.putString(KEY_SALT, salt.toHex())
        store.putString(KEY_HASH, hash(pin, salt).toHex())
        store.putBool(KEY_ENABLED, true)
        unlockedAtMs = System.currentTimeMillis()
        return true
    }

    fun clearPin() {
        store.remove(KEY_SALT, KEY_HASH, KEY_ENABLED, KEY_TIMEOUT)
        unlockedAtMs = 0L
    }

    fun verify(pin: String): Boolean {
        val saltHex = store.getString(KEY_SALT)
        val expectedHex = store.getString(KEY_HASH)
        if (saltHex.isBlank() || expectedHex.isBlank()) return false
        val salt = saltHex.fromHex() ?: return false
        val actualHex = hash(pin, salt).toHex()
        val ok = constantTimeEquals(actualHex, expectedHex)
        if (ok) unlockedAtMs = System.currentTimeMillis()
        return ok
    }

    /** تایم‌اوت قفل خودکار (پیش‌فرض یک دقیقه). */
    fun autoLockTimeoutMs(): Long = store.getLong(KEY_TIMEOUT, DEFAULT_TIMEOUT_MS)

    fun setAutoLockTimeout(timeoutMs: Long) {
        store.putLong(KEY_TIMEOUT, timeoutMs.coerceAtLeast(0L))
    }

    /** آیا الان باید صفحه‌ی PIN نشان داده شود؟ */
    fun isLockedNow(timeoutMs: Long = autoLockTimeoutMs()): Boolean {
        if (!isEnabled()) return false
        if (unlockedAtMs == 0L) return true
        return System.currentTimeMillis() - unlockedAtMs > timeoutMs
    }

    fun markUnlocked() {
        unlockedAtMs = System.currentTimeMillis()
    }

    fun lock() {
        unlockedAtMs = 0L
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    companion object {
        private const val KEY_HASH = "app_lock_hash"
        private const val KEY_SALT = "app_lock_salt"
        private const val KEY_ENABLED = "app_lock_enabled"
        private const val KEY_TIMEOUT = "app_lock_timeout"
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        const val MIN_PIN = 4
        const val MAX_PIN = 8
        const val DEFAULT_TIMEOUT_MS = 60_000L
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun String.fromHex(): ByteArray? =
    if (length % 2 != 0) null
    else runCatching { chunked(2).map { it.toInt(16).toByte() }.toByteArray() }.getOrNull()
