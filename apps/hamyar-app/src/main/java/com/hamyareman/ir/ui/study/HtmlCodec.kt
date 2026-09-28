package com.hamyareman.ir.ui.study

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * لایهٔ رمز مشترک HTML روی باکت: جادوی `HMK1` + IV ۱۲ بایتی + AES-GCM.
 * فایل بدون این سرآیند همان متن خام قدیمی است (سازگار با HTMLهای رمزنشده).
 */
object HtmlCodec {

    private val MAGIC = byteArrayOf(0x48, 0x4D, 0x4B, 0x31) // HMK1

    fun isWrapped(data: ByteArray): Boolean {
        if (data.size < 4 + 12 + 16) return false
        for (i in MAGIC.indices) if (data[i] != MAGIC[i]) return false
        return true
    }

    fun unwrap(ctx: Context, data: ByteArray): ByteArray {
        if (!isWrapped(data)) return data
        val key = HtmlMediaKey.get(ctx)
            ?: error("کلید درس روی دستگاه نیست. دوباره وارد شو.")
        val iv = data.copyOfRange(4, 16)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return c.doFinal(data, 16, data.size - 16)
    }
}
