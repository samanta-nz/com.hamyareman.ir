package com.hamyareman.ir.ui.study

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * دانلودِ صوت «چندرشته‌ای» است: هر بازهٔ Range با شمارندهٔ CTRِ خودش رمز می‌شود.
 * این آزمون ثابت می‌کند نتیجهٔ بازه‌بازه دقیقاً با رمزنگاریِ تک‌رشته یکی است —
 * وگرنه فایلِ دانلودشده جز چند کیلوبایتِ اول، نویز بود.
 */
class MediaVaultParallelCryptoTest {

    private val key = SecretKeySpec(ByteArray(32) { (it * 3).toByte() }, "AES")

    private fun encryptAll(plain: ByteArray, iv: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return c.doFinal(plain)
    }

    @Test
    fun `counter iv adds the block index as a big-endian 128-bit counter`() {
        val zero = ByteArray(16)
        assertEquals(0, MediaVault.counterIv(zero, 0)[15].toInt())
        assertEquals(1, MediaVault.counterIv(zero, 1)[15].toInt())
        assertEquals(1, MediaVault.counterIv(zero, 256L + 9)[14].toInt())
        assertEquals(9, MediaVault.counterIv(zero, 256L + 9)[15].toInt())

        // سرریز از بایتِ آخر به بایتِ قبل
        val ff = ByteArray(16).also { it[15] = 0xFF.toByte() }
        val next = MediaVault.counterIv(ff, 1)
        assertEquals(1, next[14].toInt())
        assertEquals(0, next[15].toInt())
    }

    @Test
    fun `chunked encryption equals single-stream encryption`() {
        val iv = ByteArray(16) { (it * 7 + 1).toByte() }
        // اندازهٔ عمداً غیرمضربِ ۱۶ تا «بازهٔ آخرِ ناتمام» هم سنجیده شود
        val plain = ByteArray(100_003) { ((it * 31) % 251).toByte() }
        val want = encryptAll(plain, iv)

        val out = ByteArray(plain.size)
        var off = 0
        val chunk = 4096 // مضربِ ۱۶، مثلِ بازه‌های واقعیِ دانلود
        while (off < plain.size) {
            val len = minOf(chunk, plain.size - off)
            val c = Cipher.getInstance("AES/CTR/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(MediaVault.counterIv(iv, off / 16L)))
            c.doFinal(plain, off, len).copyInto(out, off)
            off += len
        }
        assertArrayEquals("رمزِ بازه‌بازه با تک‌رشته یکی نیست", want, out)
    }

    @Test
    fun `a middle chunk decrypts with its own counter`() {
        val iv = ByteArray(16) { (it * 5).toByte() }
        val plain = ByteArray(50_000) { ((it * 17) % 256).toByte() }
        val enc = encryptAll(plain, iv)

        // همان کاری که «تأییدِ انتهای فایل» می‌کند: رمزگشاییِ یک بازهٔ وسط/آخر
        // با شمارندهٔ همان بازه، بدونِ رمزگشاییِ کلِ فایل.
        val start = 8_192L
        val len = 64
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(MediaVault.counterIv(iv, start / 16)))
        val back = c.doFinal(enc, start.toInt(), len)
        assertArrayEquals(plain.copyOfRange(start.toInt(), start.toInt() + len), back)
    }

    @Test
    fun `unaligned tail decrypts correctly`() {
        val iv = ByteArray(16) { (it + 3).toByte() }
        val total = 12_345 // نه مضربِ ۱۶
        val plain = ByteArray(total) { ((it * 13) % 256).toByte() }
        val enc = encryptAll(plain, iv)

        val alignStart = ((total - 64) / 16) * 16
        val len = total - alignStart
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(MediaVault.counterIv(iv, alignStart / 16L)))
        val back = c.doFinal(enc, alignStart, len)
        assertArrayEquals(plain.copyOfRange(alignStart, total), back)
    }
}
