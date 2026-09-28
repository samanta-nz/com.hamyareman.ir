package com.hamyareman.ir.ui.wellness

/**
 * پرامپت ۰۲ — سازنده‌ی پیش‌فرض [WellnessTiming] برای هر حرکت.
 *
 * چرا اینجا به‌صورت الگوریتمی است:
 *  - هر حرکت یک الگوی زمانی ساده دارد که در کد تعریف می‌شود.
 *  - هر اعلان صوتی در ثانیه‌ی مشخصی از مرحله پخش می‌شود.
 *  - این الگوریتم مستقل از داده‌های سرور است؛ پس حتی اگر Appwrite قطع باشد
 *    تایمر کار می‌کند.
 */
class WellnessTimingProvider : WellnessTimer.TimingProvider {

    override fun timingFor(move: WellnessMove): WellnessTiming {
        return when (move.category) {
            WellnessMove.Category.YOGA -> yogaTiming(move)
            WellnessMove.Category.EXERCISE -> exerciseTiming(move)
            WellnessMove.Category.BREATHING -> breathingTiming(move)
            WellnessMove.Category.LEARNING -> learningTiming(move)
        }
    }

    // ---------------- یوگا ----------------

    private fun yogaTiming(move: WellnessMove): WellnessTiming {
        val total = move.durationSec.coerceAtLeast(20)
        val cues = mutableListOf<AudioCue>()
        // معرفی اول (فایل شروع پخش می‌شود)
        cues += AudioCue(0, "${move.titleFa} — ${move.instructionsFa.take(80)}…", AudioCue.Kind.INTRO)
        // ۱۰ ثانیه آخر: آرام‌سازی (فایل میانه پخش می‌شود)
        if (total >= 30) cues += AudioCue(total - 10, "به‌آرامی نفس بکش و رها کن.", AudioCue.Kind.GUIDE)
        // پایان جلسه (فایل پایان پخش می‌شود)
        cues += AudioCue(total, "آفرین. تمرین تمام شد.", AudioCue.Kind.FINISH)
        return WellnessTiming(
            move = move,
            steps = listOf(WellnessStep("نگه‌دار", total)),
            cues = cues,
        )
    }

    // ---------------- ورزش (تکراری) ----------------

    private fun exerciseTiming(move: WellnessMove): WellnessTiming {
        val reps = move.reps.takeIf { it > 0 } ?: 1
        val perRep = move.durationSec.coerceAtLeast(15)
        val steps = (0 until reps).map { i -> WellnessStep("تکرار ${i + 1} از $reps", perRep) }
        val cues = mutableListOf<AudioCue>()
        cues += AudioCue(0, "${move.titleFa} — آماده شو، ${reps} بار تکرار.", AudioCue.Kind.INTRO)
        // هر ۵ تکرار، تشویق کوتاه
        steps.forEachIndexed { i, step ->
            if ((i + 1) % 5 == 0 && i < steps.size - 1) {
                cues += AudioCue(step.seconds - 2, "خوبه، ادامه بده.", AudioCue.Kind.GUIDE)
            }
        }
        return WellnessTiming(move, steps, cues)
    }

    // ---------------- تنفس ----------------

    private fun breathingTiming(move: WellnessMove): WellnessTiming {
        val (inH, holdH, outH, holdOutH) = breathRatios(move)
        val cycle = inH + holdH + outH + holdOutH
        val cycles = (move.durationSec / cycle).coerceAtLeast(1)
        val steps = (0 until cycles).map { i -> WellnessStep("دور ${i + 1} از $cycles", cycle) }
        val cues = mutableListOf<AudioCue>()
        cues += AudioCue(0, "${move.titleFa} — ${cycles} دور، چشم‌ها را ببند.", AudioCue.Kind.INTRO)
        // در هر سیکل، اعلان‌های دم/نگه/بازدم/سکوت
        steps.forEachIndexed { i, step ->
            var t = 0
            if (inH > 0) {
                cues += AudioCue(t, "دم…", AudioCue.Kind.BREATH_IN)
                t += inH
            }
            if (holdH > 0) {
                cues += AudioCue(t, "نگه‌دار…", AudioCue.Kind.BREATH_HOLD)
                t += holdH
            }
            if (outH > 0) {
                cues += AudioCue(t, "بازدم…", AudioCue.Kind.BREATH_OUT)
                t += outH
            }
            if (holdOutH > 0) {
                cues += AudioCue(t, "سکوت…", AudioCue.Kind.BREATH_HOLD)
            }
            // پیشنهاد شمارش در تنفس ۴-۷-۸ و جعبه‌ای
            if (move.slug in setOf("breath-4-7-8", "breath-box")) {
                cues += AudioCue(step.seconds - 3, "سه…", AudioCue.Kind.COUNT)
            }
        }
        return WellnessTiming(move, steps, cues)
    }

    private data class BreathRatios(val inH: Int, val holdH: Int, val outH: Int, val holdOut: Int)

    private fun breathRatios(move: WellnessMove): BreathRatios = when (move.slug) {
        "breath-diaphragm" -> BreathRatios(4, 0, 6, 0)
        "breath-4-7-8" -> BreathRatios(4, 7, 8, 0)
        "breath-box" -> BreathRatios(4, 4, 4, 4)
        "breath-nadi" -> BreathRatios(4, 2, 4, 2)
        "breath-sigh" -> BreathRatios(3, 0, 6, 1)
        "breath-lion" -> BreathRatios(4, 0, 3, 0)
        "breath-counting" -> BreathRatios(4, 4, 4, 0)
        "breath-bedtime" -> BreathRatios(4, 0, 6, 0)
        else -> BreathRatios(4, 0, 4, 0)
    }

    // ---------------- یادگیری ----------------

    private fun learningTiming(move: WellnessMove): WellnessTiming {
        val total = move.durationSec.coerceAtLeast(300)
        val cues = mutableListOf<AudioCue>()
        cues += AudioCue(0, "${move.titleFa} — شروع می‌کنیم. ${move.instructionsFa.take(100)}", AudioCue.Kind.INTRO)
        // یادآوری در نیمه‌ی راه
        if (total >= 600) cues += AudioCue(total / 2, "نصف راه رد شد. ادامه بده.", AudioCue.Kind.GUIDE)
        // ۲ دقیقه آخر
        if (total >= 300) cues += AudioCue(total - 120, "دو دقیقه‌ی آخر. جمع‌بندی کن.", AudioCue.Kind.GUIDE)
        // پایان
        cues += AudioCue(total, "تمام شد. آفرین.", AudioCue.Kind.FINISH)
        return WellnessTiming(
            move = move,
            steps = listOf(WellnessStep("اجرا", total)),
            cues = cues,
        )
    }
}
