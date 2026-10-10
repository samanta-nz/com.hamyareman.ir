package com.hamyareman.ir.ui.study

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * لایهٔ رمز مشترک HTML روی باکت: جادوی `HMK1` + IV ۱۲ بایتی + AES-GCM.
 * HTML روی باکت یا HMK1 است یا HTML ساده (با `<` شروع می‌شود)؛ [unwrap] هر دو را می‌پذیرد و
 * هر بدنهٔ دیگری fail-closed رد می‌شود.
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

    /**
     * HTML سادهٔ بدون HMK1: بعد از BOM و فاصله‌های ابتدایی با `<` شروع می‌شود.
     * فقط [length] بایت اول بررسی می‌شود.
     */
    fun looksLikeHtml(head: ByteArray, length: Int = head.size): Boolean {
        val n = minOf(length, head.size)
        var i = 0
        if (n >= 3 && head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()) i = 3
        while (i < n) {
            val b = head[i]
            if (b == 0x20.toByte() || b == 0x0A.toByte() || b == 0x0D.toByte() || b == 0x09.toByte()) i++ else break
        }
        return i < n && head[i] == 0x3C.toByte()
    }

    /** HMK1 را رمزگشایی می‌کند؛ HTML ساده (بدون magic) همان‌طور برمی‌گرد؛ بدنهٔ دیگر رد می‌شود. */
    fun unwrap(ctx: Context, data: ByteArray): ByteArray {
        if (!isWrapped(data) && looksLikeHtml(data)) return data
        require(isWrapped(data)) { "فایل نه HMK1 معتبر است و نه HTML ساده." }
        val key = HtmlMediaKey.get(ctx)
            ?: error("کلید درس روی دستگاه نیست. دوباره وارد شو.")
        val iv = data.copyOfRange(4, 16)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return c.doFinal(data, 16, data.size - 16)
    }
}
