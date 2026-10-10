package com.hamyareman.ir.ui.study

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlCodecFormatTest {

    @Test
    fun acceptsOrdinaryUtf8Html() {
        val html = """
            <!doctype html>
            <html lang="fa"><head><meta charset="utf-8"></head>
            <body><main>سلام</main></body></html>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        assertTrue(HtmlCodec.isPlainHtml(html))
        assertFalse(HtmlCodec.isWrapped(html))
    }

    @Test
    fun acceptsUtf8BomBeforeDoctype() {
        val html = ("\uFEFF<!doctype html><html><body>content</body></html>")
            .toByteArray(Charsets.UTF_8)

        assertTrue(HtmlCodec.isPlainHtml(html))
    }

    @Test
    fun rejectsNonHtmlResponsesAndBinaryPayloads() {
        assertFalse(HtmlCodec.isPlainHtml("404 Not Found".toByteArray(Charsets.UTF_8)))
        assertFalse(HtmlCodec.isPlainHtml(byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x00,
        )))
    }

    @Test
    fun recognizesHmk1AsASeparateRepresentation() {
        val wrapped = ByteArray(HtmlCodec.MIN_WRAPPED_BYTES)
        "HMK1".toByteArray(Charsets.US_ASCII).copyInto(wrapped)

        assertTrue(HtmlCodec.isWrapped(wrapped))
        assertFalse(HtmlCodec.isPlainHtml(wrapped))
    }
}
