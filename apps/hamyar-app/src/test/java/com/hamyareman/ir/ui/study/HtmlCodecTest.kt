package com.hamyareman.ir.ui.study

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class HtmlCodecTest {
    private val key = ByteArray(32) { it.toByte() }
    private fun wrap(plain: ByteArray): ByteArray {
        val iv = ByteArray(12) { (it + 30).toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return "HMK1".toByteArray() + iv + cipher.doFinal(plain)
    }
    @Test fun plainHtmlIsReturnedVerbatimWithoutUsingAKey() {
        val html = "\uFEFF  <!doctype html><html lang=fa>همیار</html>".toByteArray()
        assertSame(html, HtmlCodec.unwrapWithKey(html, ByteArray(0)))
    }
    @Test fun authenticatedEnvelopeDecodesExactOriginalHtml() {
        val html = "<!doctype html><html>متن اصلی</html>".toByteArray()
        assertArrayEquals(html, HtmlCodec.unwrapWithKey(wrap(html), key))
    }
    @Test fun mhk1CompatibilityEnvelopeUsesTheSameAuthenticatedCipher() {
        val html = "<html>journey</html>".toByteArray()
        val envelope = wrap(html)
        "MHK1".toByteArray().copyInto(envelope)
        assertTrue(HtmlCodec.hasMagic(envelope))
        assertArrayEquals(html, HtmlCodec.unwrapWithKey(envelope, key))
        envelope[envelope.lastIndex] = (envelope.last().toInt() xor 1).toByte()
        try { HtmlCodec.unwrapWithKey(envelope, key); fail("Tag must still be authenticated") }
        catch (_: javax.crypto.AEADBadTagException) { }
    }
    @Test fun modifiedAuthenticationTagIsRejected() {
        val data = wrap("<html>test</html>".toByteArray())
        data[data.lastIndex] = (data.last().toInt() xor 1).toByte()
        try { HtmlCodec.unwrapWithKey(data, key); fail("Tampered content must fail authentication") }
        catch (_: javax.crypto.AEADBadTagException) { }
    }
}
