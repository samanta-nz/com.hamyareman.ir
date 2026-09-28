package com.hamyareman.ir.platform.feature.playback

import java.io.File
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** قراردادِ گاوصندوق: MAGIC(4)+IV(16)+AES/CTR ciphertext — همان قالبِ MediaVault. */
class CtrPlainReaderTest {

    private fun buildVaultBytes(plain: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        val rnd = SecureRandom()
        val key = ByteArray(32).also(rnd::nextBytes)
        val iv = ByteArray(16).also(rnd::nextBytes)
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return Triple(key, iv, c.doFinal(plain))
    }

    @Test
    fun `chunked irregular reads return the exact plaintext`() {
        val plain = ByteArray(4096 + 7).also { SecureRandom().nextBytes(it) }
        val (key, iv, enc) = buildVaultBytes(plain)
        val f = File.createTempFile("vault", ".enc").apply { deleteOnExit() }
        f.writeBytes("HMV1".toByteArray(Charsets.US_ASCII) + iv + enc)
        RandomAccessFile(f, "r").use { raf ->
            val r = CtrPlainReader(key, iv, dataOffset = 20L, size = plain.size.toLong(), raf = raf)
            assertEquals(plain.size.toLong(), r.size)
            val out = ByteArray(plain.size)
            var pos = 0L
            var chunk = 137
            var guard = 0
            while (pos < plain.size) {
                guard++
                assertTrue("no progress", guard < 100_000)
                val n = r.read(pos, out, pos.toInt(), minOf(chunk.toLong(), plain.size - pos).toInt())
                assertTrue("read must return >0, got $n", n > 0)
                pos += n
                chunk = (chunk * 3 % 911) + 17
            }
            assertArrayEquals(plain, out)
            // EOF and short tail
            assertEquals(-1, r.read(plain.size.toLong(), out, 0, 16))
            val tail = ByteArray(16)
            val n = r.read(plain.size - 5L, tail, 0, 16)
            assertEquals(5, n)
            assertArrayEquals(plain.copyOfRange(plain.size - 5, plain.size), tail.copyOfRange(0, 5))
        }
    }

    @Test
    fun `seek to arbitrary unaligned offsets matches full-decrypt baseline`() {
        val plain = ByteArray(100_000).also { SecureRandom().nextBytes(it) }
        val (key, iv, enc) = buildVaultBytes(plain)
        val f = File.createTempFile("vault2", ".enc").apply { deleteOnExit() }
        f.writeBytes("HMV1".toByteArray(Charsets.US_ASCII) + iv + enc)
        RandomAccessFile(f, "r").use { raf ->
            val r = CtrPlainReader(key, iv, 20L, plain.size.toLong(), raf)
            for (p in longArrayOf(0, 1, 15, 16, 17, 1023, 4096 + 1, 99_999 - 1)) {
                val dst = ByteArray(64)
                val n = r.read(p, dst, 0, 64)
                // read حداکثر «تعدادِ درخواستی» را برمی‌گرداند، نه همه‌ی باقی‌مانده.
                assertEquals(minOf(64L, (plain.size - p).coerceAtLeast(0L)).toInt(), n)
                assertArrayEquals(plain.copyOfRange(p.toInt(), p.toInt() + n), dst.copyOfRange(0, n))
            }
        }
    }

    @Test
    fun `shiftIv equals 128-bit big-endian addition`() {
        val iv = ByteArray(16) { (it + 1).toByte() }
        val blocks = (1L shl 62) + 12345L
        val got = CtrPlainReader.shiftIv(iv, blocks)
        val want = BigInteger(1, iv).add(BigInteger.valueOf(blocks)).toByteArray()
            .let { if (it.size > 16) it.takeLast(16).toByteArray() else it }
        assertArrayEquals(want, got)
        // سرریزِ شمارنده باید modulo 2^128 بچرخد (نه بزرگ‌تر شدنِ آرایه)
        val ff = ByteArray(16) { 0xFF.toByte() }
        assertArrayEquals(ByteArray(16), CtrPlainReader.shiftIv(ff, 1L))
    }
}
