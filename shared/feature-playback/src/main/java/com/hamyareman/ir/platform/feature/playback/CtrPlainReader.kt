package com.hamyareman.ir.platform.feature.playback

import java.io.RandomAccessFile
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * خوانشِ تصادفیِ Plain روی بدنه‌ی AES/CTR/NoPadding (همان قراردادِ گاوصندوق:
 * `MAGIC(4) + IV(16) + ciphertext`). چونِ CTR حالتِ جریانی است، plaintext و
 * ciphertext هم‌طول‌اند و برای موقعیتِ p کافی است بلاکِ ۱۶بایتیِ هم‌ترازِ زیرِ p
 * پیدا شود، IV به تعدادِ بلاک جابه‌جا (add) شود، و همان‌جا بخوانیم — بدونِ
 * رمزگشاییِ کل فایل. این همان چیزی است که پخشِ «محلی» را از بلوکه‌شدنِ چند
 * ثانیه‌ایِ لودر نجات می‌دهد.
 */
class CtrPlainReader(
    private val key: ByteArray,
    private val iv: ByteArray,
    /** جایِ شروعِ بدنه‌ی رمزنگاری‌شده در فایل (MAGIC+IV). */
    private val dataOffset: Long,
    override val size: Long,
    private val raf: RandomAccessFile,
) : VaultStream {

    override fun read(pos: Long, dst: ByteArray, off: Int, len: Int): Int {
        if (len <= 0 || pos >= size) return -1
        val aligned = (pos / BLOCK) * BLOCK
        val pre = (pos - aligned).toInt()
        val encLen = minOf(size - aligned, (len + pre).toLong()).toInt()
        if (encLen <= 0) return -1
        val tmp = ByteArray(encLen)
        synchronized(raf) {
            raf.seek(dataOffset + aligned)
            raf.readFully(tmp)
        }
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            IvParameterSpec(shiftIv(iv, aligned / BLOCK)),
        )
        val dec = c.doFinal(tmp)
        val n = minOf(len.toLong(), (dec.size - pre).coerceAtLeast(0).toLong()).toInt()
        if (n <= 0) return -1
        System.arraycopy(dec, pre, dst, off, n)
        return n
    }

    override fun close() {
        runCatching { raf.close() }
    }

    companion object {
        private const val BLOCK = 16L

        /** `iv + blocks` به‌صورتِ عددِ ۱۲۸بیتیِ بزرگ‌پایان (شمارنده‌ی CTR). */
        fun shiftIv(iv: ByteArray, blocks: Long): ByteArray {
            val out = iv.copyOf()
            var x = blocks
            var i = out.size - 1
            while (i >= 0 && (x != 0L)) {
                val sum = (out[i].toInt() and 0xFF) + (x and 0xFF)
                out[i] = (sum and 0xFF).toByte()
                x = (x shr 8) + (sum shr 8)
                i--
            }
            return out
        }
    }
}
