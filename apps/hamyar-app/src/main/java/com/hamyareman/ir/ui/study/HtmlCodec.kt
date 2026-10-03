package com.hamyareman.ir.ui.study

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * لایهٔ رمز مشترک HTML روی باکت: جادوی `HMK1` + IV ۱۲ بایتی + AES-GCM.
 * محتوای remote باید همیشه HMK1 باشد؛ نبود magic به‌صورت fail-closed رد می‌شود.
 */
object HtmlCodec {

    private val MAGIC = byteArrayOf(0x48, 0x4D, 0x4B, 0x31) // HMK1

    /** کمینهٔ اندازهٔ یک فایل معتبر: magic ۴ + IV ۱۲ + تگ GCM ۱۶. */
    const val MIN_WRAPPED_BYTES: Int = 4 + 12 + 16

    /** فقط چهار بایت اول را می‌سنجد — برای وارسی سریع فایل روی دیسک یا سرِ پاسخ. */
    fun hasMagic(head: ByteArray): Boolean {
        if (head.size < MAGIC.size) return false
        for (i in MAGIC.indices) if (head[i] != MAGIC[i]) return false
        return true
    }

    fun isWrapped(data: ByteArray): Boolean {
        if (data.size < MIN_WRAPPED_BYTES) return false
        return hasMagic(data)
    }

    fun unwrap(ctx: Context, data: ByteArray): ByteArray {
        require(isWrapped(data)) { "فایل HTML رمز معتبر HMK1 ندارد." }
        val key = HtmlMediaKey.get(ctx)
            ?: error("کلید درس روی دستگاه نیست. دوباره وارد شو.")
        val iv = data.copyOfRange(4, 16)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return c.doFinal(data, 16, data.size - 16)
    }
}
