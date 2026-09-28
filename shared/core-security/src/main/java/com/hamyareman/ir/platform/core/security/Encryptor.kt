package com.hamyareman.ir.platform.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * رمزنگاری AES-256-GCM برای داده‌های خیلی خصوصی (دفترچه، نامه، چرخه).
 *
 * کلید در Android Keystore ساخته و نگه داشته می‌شود و هرگز از دستگاه بیرون نمی‌آید؛
 * بنابراین حتی با دسترسی به SharedPreferences هم چیزی خوانده نمی‌شود.
 *
 * قالب ذخیره: base64( IV(12) || ciphertext || tag(16) )
 */
class Encryptor(private val alias: String = DEFAULT_ALIAS) {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val key: SecretKey by lazy { getOrCreateKey() }

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val packed = iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String? = runCatching {
        val packed = Base64.decode(payload, Base64.NO_WRAP)
        if (packed.size <= IV_BYTES) return@runCatching null
        val iv = packed.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(packed, IV_BYTES, packed.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()

    /** برای «پاک‌کردن کامل داده‌ی من». */
    fun destroyKey() {
        runCatching { if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias) }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        if (alias == DEFAULT_ALIAS) {
            (keyStore.getKey(LEGACY_ALIAS, null) as? SecretKey)?.let { return it }
        }
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false) // IV را خودمان می‌سازیم و کنار متن نگه می‌داریم
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "hamyareman_private_v1"
        const val LEGACY_ALIAS = "roozhayeman_private_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
