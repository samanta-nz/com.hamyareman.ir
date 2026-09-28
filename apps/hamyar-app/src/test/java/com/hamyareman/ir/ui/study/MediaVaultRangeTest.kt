package com.hamyareman.ir.ui.study

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اندازه‌ی کلِ فایل‌های باکت از `Content-Range` خوانده می‌شود (پاسخ chunked است و
 * `Content-Length` ندارد) — این تجزیه باید دقیق باشد وگرنه درصدِ دانلود نشان
 * داده نمی‌شود.
 */
class MediaVaultRangeTest {

    @Test
    fun `total from a normal 206 content-range`() {
        assertEquals(23_947_850L, totalFromContentRange("bytes 0-1023/23947850"))
        assertEquals(27_903_834L, totalFromContentRange("bytes 0-1023/27903834"))
    }

    @Test
    fun `total from a 416 star-range response`() {
        assertEquals(12_345L, totalFromContentRange("bytes */12345"))
    }

    @Test
    fun `unknown total stays -1`() {
        assertEquals(-1L, totalFromContentRange(null))
        assertEquals(-1L, totalFromContentRange(""))
        assertEquals(-1L, totalFromContentRange("bytes 0-1023/*"))
        assertEquals(-1L, totalFromContentRange("bytes 0-1023/abc"))
    }

    /**
     * زمان‌بندیِ HTML درس‌ها باید با `TeachSeekMap` یکی باشد — وگرنه لمسِ فهرست،
     * پلیر را به دقیقه‌ثانیه‌ی غلط می‌برد. (اگر فایلِ asset در دسترس نبود، رد می‌شود.)
     */
    @Test
    fun `lesson-1 html seek times match the seek map`() {
        val asset = File("src/main/assets/math/c905/ryazif01d01.html")
        if (!asset.exists()) return
        val times = TeachSeekMap.times("C905_E01-L01")
        assertEquals(10, times.size)
        val inHtml = Regex("""data-seek-ms="(\d+)""")
            .findAll(asset.readText())
            .map { it.groupValues[1].toLong() }
            .toList()
        assertEquals("HTML times vs map", times, inHtml)
        assertEquals(57_000L, times.first())
        assertEquals(1_495_000L, times.last())
    }

    /**
     * نگهبانِ همگامیِ سه‌طرفه: `data-seek-ms`های HTML تدریس ↔ `TeachSeekMap` ↔
     * فهرست‌های زمان‌بندیِ پوشهٔ صوتِ تدریس در Books.
     *
     * چرا نه مقایسهٔ جایگاهی با .txt: تعدادِ آیتم‌های .txt با تعدادِ آیتم‌های فهرستِ
     * HTML یکی نیست («بخش صفر: مقدمه»، «استراحت» و گاهی «جمع‌بندی» در HTML جایی
     * ندارند). پس همترازیِ عنوان‌ها یک‌بار با
     * `tools/seek-shim/sync_seek_html.py` انجام و نتیجه در
     * `tools/seek-shim/expected-seek.json` ثبت می‌شود؛ این آزمون هم همان انتظار را
     * با HTML و با `TeachSeekMap` مقایسه می‌کند و هم بررسی می‌کند که انتظار،
     * **زیردنبالهٔ فزایندهٔ** خودِ .txt باشد (تا با به‌روزرسانیِ .txt، انتظارِ کهنه
     * سبز نماند).
     */
    @Test
    fun `html seek times match the Books timing lists`() {
        val dir = File("../../Books/Base-09/ریاضی/04- صوت تدریس")
        val expectedFile = File("../../tools/seek-shim/expected-seek.json")
        if (!dir.isDirectory || !expectedFile.exists()) return
        val expected = parseExpected(expectedFile.readText())
        var checked = 0
        for ((name, want) in expected) {
            if (want.isEmpty()) continue
            val html = File("src/main/assets/math/c905/$name.html")
            val txt = File(dir, "$name.txt")
            val pack = packOf(name)
            if (pack != null) {
                val mapped = TeachSeekMap.times(pack)
                if (mapped.isNotEmpty()) {
                    assertEquals("$name — TeachSeekMap با انتظار یکی نیست ($pack)", want, mapped)
                    checked++
                }
            }
            if (html.exists()) {
                val got = seekTimes(html.readText())
                assertEquals(
                    "$name — HTML با انتظارِ همگام‌شده یکی نیست؛ " +
                        "`python3 tools/seek-shim/sync_seek_html.py` را اجرا کن",
                    want,
                    got,
                )
            }
            if (txt.exists()) {
                assertTrue(
                    "$name — زمان‌های انتظار، زیردنبولهٔ فهرستِ Books نیستند (.txt عوض شده؟)",
                    isSubsequence(want, parseTimingList(txt.readText())),
                )
            }
        }
        assertTrue("هیچ زمان‌بندیِ همگام‌شده‌ای پیدا نشد", checked >= 5)
    }

    private fun seekTimes(html: String): List<Long> =
        Regex("data-seek-ms=\"(\\d+)\"")
            .findAll(html)
            .map { it.groupValues[1].toLong() }
            .toList()

    /**
     * تجزیهٔ سبکِ `expected-seek.json` بدونِ org.json (که در تستِ JVM ماک نیست):
     * هر درس با کلیدِ `ryazif…` و نخستین آرایهٔ عددیِ بعد از آن (`ms`، چون
     * `json.dumps(sort_keys=true)` آن را پیش از `pack` و `titles` می‌آورد).
     */
    private fun parseExpected(json: String): Map<String, List<Long>> {
        val out = LinkedHashMap<String, List<Long>>()
        val entry = Regex("(ryazif[a-z0-9]+)[^0-9\\[]*\\[([0-9,\\s]+)]")
        entry.findAll(json).forEach { m ->
            out[m.groupValues[1]] =
                m.groupValues[2].split(",").mapNotNull { it.trim().toLongOrNull() }
        }
        return out
    }

    private fun isSubsequence(sub: List<Long>, all: List<Long>): Boolean {
        var i = 0
        for (v in all) {
            if (i < sub.size && sub[i] == v) i++
        }
        return i == sub.size
    }

    private fun packOf(stem: String): String? {
        Regex("""ryazif(\d{2})d(\d{2})""").matchEntire(stem)?.let {
            return "C905_E${it.groupValues[1]}-L${it.groupValues[2]}"
        }
        Regex("""ryazif(\d{2})review""").matchEntire(stem)?.let {
            return "C905_E${it.groupValues[1]}-SUM"
        }
        return null
    }

    /**
     * «۱. ۰:۵۷ — عنوان» ⇒ 57000 میلی‌ثانیه. بردبار: ارقامِ فارسی/عربی، فاصلهٔ
     * اختیاری کنارِ دونقطه («۱۴ :۴۵») و ثانیهٔ تک‌رقمی («۲:۸» = ۲ دقیقه و ۸ ثانیه).
     */
    private fun parseTimingList(text: String): List<Long> = text.lineSequence().mapNotNull { line ->
        val s = line.map { c ->
            when (c) {
                in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                else -> c
            }
        }.joinToString("")
        Regex("""(\d{1,3})\s*:\s*(\d{1,2})(?!\d)""").find(s)?.let { m ->
            val sec = m.groupValues[2].toLong()
            if (sec < 60) m.groupValues[1].toLong() * 60_000 + sec * 1000 else null
        }
    }.toList()
}
