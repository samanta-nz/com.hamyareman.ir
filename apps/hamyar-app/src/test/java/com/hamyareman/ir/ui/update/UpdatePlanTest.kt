package com.hamyareman.ir.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های منطقِ خالصِ کانالِ آپدیت (JVM، بدونِ Android).
 *
 * آنچه این‌جا قفل می‌شود: «ردیفِ ناقص هیچ‌وقت پیام نمی‌سازد»، «min اجباری می‌کند»،
 * «rollout تدریجی است» و «نصبِ فعلی هرگز پیامِ نسخهٔ عقب‌تر نمی‌بیند».
 */
class UpdatePlanTest {

    private val full = """
        {"latest":67,"min":0,"url":"https://github.com/x/y/releases/download/v1.66/hamyar-1.66.apk",
         "size":28000000,"sha256":"abc","name":"1.66","notes":["پلیر صوت","ورود آفلاین"],
         "chan":"stable","rollout":100}
    """.trimIndent()

    @Test
    fun `reads every field of a complete payload`() {
        val info = UpdatePlan.parse(full)
        assertEquals(67, info.latest)
        assertEquals(0, info.min)
        assertTrue(info.url.endsWith("hamyar-1.66.apk"))
        assertEquals(28000000L, info.size)
        assertEquals("1.66", UpdatePlan.versionLabel(info))
        assertEquals("abc", info.sha256)
        assertEquals(listOf("پلیر صوت", "ورود آفلاین"), info.notes)
        assertEquals("stable", info.chan)
        assertEquals(100, info.rollout)
    }

    @Test
    fun `broken or empty payload is harmless`() {
        val info = UpdatePlan.parse("")
        assertEquals(0, info.latest)
        assertEquals("", info.url)
        assertEquals(100, info.rollout)
        assertTrue(info.notes.isEmpty())
        assertEquals("", UpdatePlan.versionLabel(info))
        assertEquals("", info.sha256)
    }

    @Test
    fun `missing url never triggers a prompt`() {
        val info = UpdatePlan.parse("""{"latest":99,"min":99}""")
        assertEquals(UpdateDecision.None, UpdatePlan.decisionFor(66, info))
    }

    @Test
    fun `older or equal app with optional min shows the dialog`() {
        val info = UpdatePlan.parse(full)
        assertTrue(UpdatePlan.decisionFor(66, info) is UpdateDecision.Optional)
        // نسخهٔ برابر یا تازه‌تر → هیچ پیامی.
        assertEquals(UpdateDecision.None, UpdatePlan.decisionFor(67, info))
        assertEquals(UpdateDecision.None, UpdatePlan.decisionFor(70, info))
    }

    @Test
    fun `min makes it forced for old installs only`() {
        val info = UpdatePlan.parse("""{"latest":70,"min":67,"url":"https://e/x.apk","rollout":100}""")
        assertTrue(UpdatePlan.decisionFor(66, info) is UpdateDecision.Forced)
        assertTrue(UpdatePlan.decisionFor(67, info) is UpdateDecision.Optional)
    }

    @Test
    fun `rollout keeps out-of-bucket installs silent`() {
        val info = UpdatePlan.parse("""{"latest":70,"min":0,"url":"https://e/x.apk","rollout":30}""")
        assertTrue(UpdatePlan.decisionFor(66, info, bucket = 10) is UpdateDecision.Optional)
        assertEquals(UpdateDecision.None, UpdatePlan.decisionFor(66, info, bucket = 55))
    }

    @Test
    fun `other channels do not reach stable builds`() {
        val info = UpdatePlan.parse("""{"latest":70,"url":"https://e/x.apk","chan":"beta"}""")
        assertEquals(UpdateDecision.None, UpdatePlan.decisionFor(66, info))
    }

    @Test
    fun `rollout bucket is stable per install`() {
        val a = UpdatePlan.rolloutBucket("device-alpha")
        assertEquals(a, UpdatePlan.rolloutBucket("device-alpha"))
        assertTrue(a in 0..99)
        // سطل‌های نصب‌های مختلف باید پخش شوند، نه همه یک عدد.
        val spread = (1..40).map { UpdatePlan.rolloutBucket("install-$it") }.toSet()
        assertTrue("سطل‌ها پخش نشده‌اند: $spread", spread.size > 5)
    }

    @Test
    fun `size label is human readable`() {
        assertEquals("", UpdatePlan.sizeLabel(0))
        assertEquals("512 کیلوبایت", UpdatePlan.sizeLabel(512L * 1024L))
        assertEquals("1 مگابایت", UpdatePlan.sizeLabel(1024L * 1024L))
        assertEquals("26 مگابایت", UpdatePlan.sizeLabel(28_000_000L))
    }

    @Test
    fun `update notes txt is grouped by version heading`() {
        val table = UpdateNotes.parseTxt(
            """
            # 1.75
            خط اول
            - خط دوم
            # 1.74
            قدیمی
            """.trimIndent(),
        )
        assertEquals(listOf("خط اول", "خط دوم"), table["1.75"])
        assertEquals(listOf("قدیمی"), table["1.74"])
    }
}
