package com.hamyareman.ir.ui.study

import android.content.Context
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Recognizes both supported HTML representations:
 * HMK1 + IV + AES-GCM ciphertext, and ordinary UTF-8 HTML.
 *
 * unwrap() remains strict and only decrypts HMK1. Call decodeHtml() only at a
 * call site that explicitly expects an HTML document and is allowed to accept
 * either representation.
 */
object HtmlCodec {

    private val MAGIC = byteArrayOf(0x48, 0x4D, 0x4B, 0x31) // HMK1
    private val HTML_TAG = Regex(
        """<!doctype\s+html\b|<[a-z][a-z0-9:-]*(?:\s|/|>)""",
        RegexOption.IGNORE_CASE,
    )

    /** Minimum envelope size: 4-byte magic + 12-byte IV + 16-byte GCM tag. */
    const val MIN_WRAPPED_BYTES: Int = 4 + 12 + 16

    /** Quick signature check for a byte prefix or full payload. */
    fun hasMagic(head: ByteArray): Boolean {
        if (head.size < MAGIC.size) return false
        for (i in MAGIC.indices) if (head[i] != MAGIC[i]) return false
        return true
    }

    fun isWrapped(data: ByteArray): Boolean =
        data.size >= MIN_WRAPPED_BYTES && hasMagic(data)

    /** Inspect only the cache file header when a caller needs to know if a key is required. */
    fun isWrappedFile(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_WRAPPED_BYTES) return false
        return runCatching {
            file.inputStream().use { input ->
                val head = ByteArray(MAGIC.size)
                var offset = 0
                while (offset < head.size) {
                    val n = input.read(head, offset, head.size - offset)
                    if (n <= 0) break
                    offset += n
                }
                offset == head.size && hasMagic(head)
            }
        }.getOrDefault(false)
    }

    /**
     * Validate an ordinary HTML document using only a bounded UTF-8 prefix.
     * This rejects binary files and common non-HTML error payloads before caching.
     */
    fun isPlainHtml(data: ByteArray): Boolean {
        if (data.size < 8 || hasMagic(data)) return false
        val prefix = String(data, 0, minOf(data.size, 16 * 1024), Charsets.UTF_8)
            .removePrefix("\uFEFF")
            .trimStart()
        if (prefix.isEmpty() || prefix.indexOf('\u0000') >= 0 || !prefix.contains('>')) return false
        return HTML_TAG.containsMatchIn(prefix)
    }

    /**
     * Decode only when the payload is wrapped; otherwise return validated plain HTML.
     * This does not weaken unwrap(), which stays fail-closed for encrypted-only callers.
     */
    fun decodeHtml(ctx: Context, data: ByteArray): ByteArray {
        if (!isWrapped(data)) {
            require(isPlainHtml(data)) { "محتوای دریافت‌شده HTML معتبر نیست." }
            return data
        }
        val plain = unwrap(ctx, data)
        require(isPlainHtml(plain)) { "محتوای رمزگشایی‌شده HTML معتبر نیست." }
        return plain
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
