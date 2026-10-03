package com.hamyareman.admin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * مخزن رمزهای عملیاتی ادمین.
 *
 * کلیدهای Appwrite، ParsPack و HTML هرگز در BuildConfig، git یا backup اندروید
 * ذخیره نمی‌شوند. متن رمز‌شده در SharedPreferences و کلید اصلی فقط در Android
 * Keystore همین دستگاه نگه‌داری می‌شود. برای دستگاه گم‌شده/مشکوک، از «پاک کردن
 * کلیدهای این دستگاه» استفاده کنید و کلید سمت سرویس را rotate کنید.
 */
class SecureAdminVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(name: String): String =
        prefs.getString(name, null)?.let(::decrypt).orEmpty()

    fun put(name: String, value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            prefs.edit().remove(name).apply()
        } else {
            prefs.edit().putString(name, encrypt(clean)).apply()
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun master(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, master())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(packed)
        encrypted.copyInto(packed, cipher.iv.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(ciphertext: String): String {
        return runCatching {
            val packed = Base64.decode(ciphertext, Base64.NO_WRAP)
            require(packed.size > IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                master(),
                GCMParameterSpec(128, packed.copyOfRange(0, IV_BYTES)),
            )
            cipher.doFinal(packed, IV_BYTES, packed.size - IV_BYTES).toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private companion object {
        const val PREFS = "hamyar_admin_secure_v2"
        const val KEY_ALIAS = "hamyar_admin_operator_v2"
        const val IV_BYTES = 12
    }
}
