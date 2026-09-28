package com.hamyareman.ir.ui.study

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * کلید مشترک رمز HTML — روی سرور (ردیف خصوصی بعد از ورود) و روی گوشی
 * فقط با Android Keystore پوشانده می‌شود. داخل APK نیست.
 */
object HtmlMediaKey {

    const val ROW_ID = "html_media_key"

    private const val PREFS = "hamyar_html_mk"
    private const val WRAPPED = "wrapped"
    private const val KS_ALIAS = "hamyar_html_mk_master"

    @Volatile private var cached: ByteArray? = null

    fun get(ctx: Context): ByteArray? {
        cached?.let { return it }
        val wrapped = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WRAPPED, null)
        val raw = runCatching { unwrap(wrapped) }.getOrNull() ?: return null
        if (raw.size != 32) return null
        cached = raw
        return raw
    }

    suspend fun fetch(ctx: Context, tables: TablesDbService): Boolean = withContext(Dispatchers.IO) {
        if (!tables.isConfigured) return@withContext get(ctx) != null
        when (val r = tables.get(TableIds.APP_STATE, ROW_ID)) {
            is AppResult.Ok -> {
                val payload = r.value?.string("payload").orEmpty()
                val b64 = runCatching { JSONObject(payload).optString("b") }.getOrDefault("")
                if (b64.isBlank()) return@withContext get(ctx) != null
                val raw = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                if (raw == null || raw.size != 32) return@withContext get(ctx) != null
                val w = runCatching { wrap(raw) }.getOrNull() ?: return@withContext false
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(WRAPPED, w).apply()
                cached = raw
                true
            }
            is AppResult.Err -> get(ctx) != null
        }
    }

    private fun master(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KS_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KS_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        return gen.generateKey()
    }

    private fun wrap(plain: ByteArray): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, master())
        val ct = c.doFinal(plain)
        val out = ByteArray(12 + ct.size)
        c.iv.copyInto(out)
        ct.copyInto(out, 12)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrap(wrapped: String?): ByteArray? {
        wrapped ?: return null
        val all = Base64.decode(wrapped, Base64.NO_WRAP)
        if (all.size <= 12) return null
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, master(), GCMParameterSpec(128, all.copyOfRange(0, 12)))
        return c.doFinal(all, 12, all.size - 12)
    }
}
