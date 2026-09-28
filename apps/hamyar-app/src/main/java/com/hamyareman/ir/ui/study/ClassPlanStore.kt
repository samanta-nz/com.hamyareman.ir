package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.SchoolOffCache
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.platform.core.notifications.ReminderScheduler
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.home.CalendarOccasions
import com.hamyareman.ir.ui.home.IranOfficialHolidays
import com.hamyareman.ir.ui.profile.GradeGate
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * برنامهٔ کلاسی مدرسه + آماده‌سازی فردا + شیفت چرخشی.
 * خصوصی روی دستگاه؛ Sync نمی‌شود.
 */
object ClassPlanStore {

    private const val PREF = "hamyar_class_plan"
    val WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه") // ۱..۵
    const val SPORT = "ورزش"

    /** درسِ پایهٔ نهم که کتابش در رجیستریِ ماژول‌ها نیست ولی در مدرسه زنگ دارد. */
    const val DEFENSE = "آمادگی دفاعی"

    /** گزینهٔ انتهای فهرست: زنگ را خالی می‌کند (مقدارِ ذخیره‌شده = رشتهٔ خالی). */
    const val EMPTY_SUBJECT = "— خالی —"

    /**
     * نامِ ثبت‌شدهٔ درس در برنامهٔ هفتگی — قانونِ مدرسه: چند کتاب، یک زنگ هستند.
     *  - «تعلیمات اسلامی» (همان پیام‌های آسمان) ⇒ **معارف**
     *  - «نگارش» و «فارسی» ⇒ **فارسی**
     *  - «زبان انگلیسی» و «کتاب کار زبان انگلیسی» ⇒ **زبان انگلیسی**
     * بقیهٔ درس‌ها با نامِ کوتاهِ خودِ کتاب ثبت می‌شوند. تابع idempotent است، پس
     * دادهٔ ذخیره‌شدهٔ نسخه‌های قدیمی هم موقعِ خواندن به نامِ درست می‌نشیند.
     */
    fun canonicalSubject(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty() || t == EMPTY_SUBJECT) return ""
        if (t.contains("تعلیمات اسلامی") || t.contains("پیام")) return "معارف"
        if (t.contains("نگارش") || t.contains("فارسی")) return "فارسی"
        if (t.contains("زبان انگلیسی") || t.contains("کتاب کار")) return "زبان انگلیسی"
        return t
    }

    data class SlotDay(val subjects: List<String>)
    data class Snapshot(
        val locked: Boolean,
        val days: Map<Int, List<String>>, // dayIndex 1..5
        val cycleWeeks: Int,
        val anchorIso: String,
        val fixedEvening: Boolean,
        val lunarOffset: Int,
        val morningHour: Int,
        val morningMinute: Int,
        val wakeLeadMin: Int,
        val noonHour: Int,
        val noonMinute: Int,
        val sleepMorning: String,
        val sleepEvening: String,
        val weekPattern: List<String>,
        val virtualMorningHour: Int,
        val virtualMorningMinute: Int,
        val virtualNoonHour: Int,
        val virtualNoonMinute: Int,
        /** ساعت خروج از مدرسه — شیفت صبح (HH:MM). */
        val exitMorning: String = "13:30",
        /** ساعت خروج از مدرسه — شیفت ظهر (HH:MM). */
        val exitNoon: String = "17:30",
        /** بخش اولِ تنظیمات شیفت: شیفتِ هفته‌ی جاری (لنگر). */
        val thisWeekShift: Shift = Shift.MORNING,
        /** هفته‌ی چندمِ چرخه (۱..cycleWeeks) با آن هفته‌ی لنگر هم‌خوانی دارد. */
        val cycleWeekOffset: Int = 1,
        /** درسِ دومِ هر خانه — هم‌اندازهٔ `days`؛ رشتهٔ خالی یعنی خانه تک‌درسی است. */
        val second: Map<Int, List<String>> = emptyMap(),
        /**
         * ساعتِ هر زنگ به صورت «HH:MM-HH:MM» (خانهٔ i = «تایم زنگ i+1»).
         * یک فهرست برای همهٔ روزها؛ از صفحهٔ برنامهٔ مدرسه تنظیم و سینک می‌شود.
         */
        val bells: List<String> = emptyList())

    /** «HH:MM-HH:MM» زنگِ شمارهٔ [slot] (صفر‌پایه) — اگر تنظیم نشده باشد رشتهٔ خالی. */
    fun bellOf(snap: Snapshot, slot: Int): String = snap.bells.getOrNull(slot).orEmpty()

    /** نمایشِ فارسیِ بازهٔ زنگ: «۰۷:۳۰ تا ۰۸:۱۵»؛ خالی اگر تنظیم نشده باشد. */
    fun bellLabel(snap: Snapshot, slot: Int): String {
        val raw = bellOf(snap, slot)
        val parts = raw.split("-")
        val a = parts.getOrNull(0)?.trim().orEmpty()
        val b = parts.getOrNull(1)?.trim().orEmpty()
        if (a.isBlank() || b.isBlank()) return ""
        return "${toPersianDigits(a)} تا ${toPersianDigits(b)}"
    }

    /** ساعت خروجِ شیفتِ داده‌شده به صورت «HH:MM». */
    fun exitOf(snap: Snapshot, shift: Shift): String =
        if (shift == Shift.MORNING) snap.exitMorning.ifBlank { "13:30" } else snap.exitNoon.ifBlank { "17:30" }

    /** دقیقه‌های گذشته از نیمه‌شب برای یک زمانِ «HH:MM». */
    fun minutesOf(hhmm: String): Int {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * آیا «رفرشِ بعد از مدرسه» برای امروز انجام شده؟ (تیک‌های فردا آزاد می‌شوند)
     */
    fun refreshedToday(ctx: Context, today: LocalDate = LocalDate.now(JalaliDate.TEHRAN)): Boolean =
        store(ctx).getBool("refreshed_$today", false)

    /**
     * رفرشِ اطلاع‌رسانی‌های فردا در ساعتِ خروج (یا پایانِ کلاس مجازی):
     * تیک‌های قفل‌شده آزاد می‌شوند تا برای روز بعد آماده شوند.
     */
    fun maybeRefreshAtExit(
        ctx: Context,
        now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN),
        reminders: ReminderScheduler? = null) {
        val today = now.toLocalDate()
        val s = store(ctx)
        if (s.getBool("refreshed_$today", false)) return
        val snap = load(ctx)
        val shift = shiftOf(snap, today)
        val exitMin = minutesOf(exitOf(snap, shift))
        // روزهای مجازی: پایانِ کلاس مجازی جای ساعت خروج را می‌گیرد.
        val endMin = virtualSessions(ctx).firstOrNull { it.dayIndex == SchoolShift.dayIndex(today) }
            ?.endMinFor(shift)
        val gate = endMin ?: exitMin
        if (now.hour * 60 + now.minute < gate) return
        s.keysWithPrefix("lock_").forEach { s.remove(it) }
        s.putBool("refreshed_$today", true)
        // همهٔ اطلاع‌رسانی‌های مربوط به فردا دوباره زمان‌بندی می‌شوند.
        if (reminders != null) {
            val next = firstSchoolDay(snap, today.plusDays(1), ctx)
            syncAlarms(ctx, reminders, snap, next)
        }
    }

    fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun shortBookName(title: String): String =
        title.replace(" پایه نهم", "").substringBefore(" — ").trim()

    /**
     * فهرستِ درس‌های قابلِ انتخاب در زنگ‌ها: کتاب‌های همان پایه (با نامِ
     * [canonicalSubject] و بدونِ تکرار) + «آمادگی دفاعی» و «ورزش» + در انتها
     * گزینهٔ [EMPTY_SUBJECT] برای خالی‌کردنِ خانه.
     */
    fun subjectOptions(ctx: Context): List<String> {
        val books = GradeGate.filter(BookModuleRegistry.modules) { it.bookCode }
            .map { canonicalSubject(shortBookName(it.title)) }
            .filter { it.isNotBlank() }
        val extras = mutableListOf<String>()
        if (com.hamyareman.ir.ui.profile.StudentProfileState.grade ==
            com.hamyareman.ir.ui.profile.GradeLevel.G9
        ) extras += DEFENSE
        extras += SPORT
        return (books + extras).distinct() + EMPTY_SUBJECT
    }

    /** شیفتِ «هفته‌ی لنگر» با منطقِ قدیمی — فقط برای داده‌های نسخه‌های قبل. */
    private fun legacyAnchorShift(cycleWeeks: Int, anchorIso: String, fixedEvening: Boolean, weekPattern: List<String>, date: LocalDate): Shift {
        if (weekPattern.size == cycleWeeks && weekPattern.isNotEmpty()) {
            val pos = Math.floorMod(SchoolShift.weekIndex(anchorIso, date), cycleWeeks.toLong()).toInt()
            return if (weekPattern.getOrNull(pos) == "evening") Shift.EVENING else Shift.MORNING
        }
        if (cycleWeeks == 1) return if (fixedEvening) Shift.EVENING else Shift.MORNING
        return SchoolShift.shiftOn(anchorIso, date, cycleWeeks)
    }

    fun load(ctx: Context): Snapshot {
        val s = store(ctx)
        val today = LocalDate.now(JalaliDate.TEHRAN)
        val days = (1..5).associateWith { d ->
            runCatching {
                val arr = JSONArray(s.getString("day_$d", "[]"))
                (0 until arr.length()).map { canonicalSubject(arr.optString(it)) }
                    .filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }
        val second = (1..5).associateWith { d ->
            runCatching {
                val arr = JSONArray(s.getString("dayb_$d", "[]"))
                (0 until arr.length()).map { canonicalSubject(arr.optString(it)) }
            }.getOrDefault(emptyList())
        }
        val bells0 = runCatching {
            val arr = JSONArray(s.getString("bell_times", "[]"))
            (0 until arr.length()).map { arr.optString(it) }
        }.getOrDefault(emptyList())
        val cycleWeeks0 = s.getInt("cycle_weeks", 2).let { if (it in listOf(1, 2, 4)) it else 2 }
        val anchorIso0 = s.getString("anchor").ifBlank { SchoolShift.startOfPersianWeek(today).toString() }
        val weekPattern0 = s.getString("week_pattern").split(",").map { it.trim() }.filter { it == "morning" || it == "evening" }
        val thisWeekShift0 = when (s.getString("this_week_shift")) {
            "evening" -> Shift.EVENING
            "morning" -> Shift.MORNING
            else -> legacyAnchorShift(cycleWeeks0, anchorIso0, s.getBool("fixed_evening", false), weekPattern0, today)
        }
        val offset0 = s.getInt("cycle_week_offset", 0).let {
            if (it in 1..cycleWeeks0) it
            else if (cycleWeeks0 == 1) 1
            else Math.floorMod(SchoolShift.weekIndex(anchorIso0, today), cycleWeeks0.toLong()).toInt() + 1
        }
        return Snapshot(
            locked = s.getBool("locked", false),
            days = days,
            cycleWeeks = cycleWeeks0,
            anchorIso = anchorIso0,
            fixedEvening = s.getBool("fixed_evening", false),
            lunarOffset = s.getInt("lunar_offset", 0).coerceIn(-2, 2),
            morningHour = s.getInt("m_hour", 7),
            morningMinute = s.getInt("m_min", 30),
            wakeLeadMin = s.getInt("wake_lead", 60),
            noonHour = s.getInt("n_hour", 12),
            noonMinute = s.getInt("n_min", 0),
            sleepMorning = s.getString("sleep_am", "21:30").ifBlank { "21:30" },
            sleepEvening = s.getString("sleep_pm", "23:00").ifBlank { "23:00" },
            weekPattern = weekPattern0,
            virtualMorningHour = s.getInt("virt_m_h", 8),
            virtualMorningMinute = s.getInt("virt_m_m", 0),
            virtualNoonHour = s.getInt("virt_n_h", 14),
            virtualNoonMinute = s.getInt("virt_n_m", 0),
            exitMorning = s.getString("exit_am").ifBlank { "13:30" },
            exitNoon = s.getString("exit_pm").ifBlank { "17:30" },
            thisWeekShift = thisWeekShift0,
            cycleWeekOffset = offset0,
            second = second,
            bells = bells0)
    }

    /** حذفِ یک خانه (درس) از یک روزِ برنامهٔ هفتگی. */
    fun removeSlot(ctx: Context, dayIndex: Int, slotIndex: Int) {
        val s = store(ctx)
        val arr = runCatching { JSONArray(s.getString("day_$dayIndex", "[]")) }.getOrDefault(JSONArray())
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            if (i == slotIndex) continue
            out.put(arr.optString(i))
        }
        s.putString("day_$dayIndex", out.toString())
        val arrB = runCatching { JSONArray(s.getString("dayb_$dayIndex", "[]")) }.getOrDefault(JSONArray())
        val outB = JSONArray()
        for (i in 0 until arrB.length()) {
            if (i == slotIndex) continue
            outB.put(arrB.optString(i))
        }
        s.putString("dayb_$dayIndex", outB.toString())
        StateSync.markLocal(ctx, StateSync.KEY_WEEK)
    }

    /**
     * ذخیرهٔ خانه‌های برنامهٔ هفتگی. [seconds] درسِ دومِ هر خانه است (اختیاری) و
     * [bells] ساعتِ زنگ‌ها برای همهٔ روزها (اختیاری). `null` یعنی «دست نزن».
     */
    fun saveDays(
        ctx: Context,
        days: Map<Int, List<String>>,
        locked: Boolean,
        seconds: Map<Int, List<String>>? = null,
        bells: List<String>? = null) {
        val s = store(ctx)
        days.forEach { (d, list) ->
            val arr = JSONArray(); list.forEach { arr.put(it) }
            s.putString("day_$d", arr.toString())
        }
        seconds?.forEach { (d, list) ->
            val arr = JSONArray(); list.forEach { arr.put(it) }
            s.putString("dayb_$d", arr.toString())
        }
        bells?.let { saveBellTimes(ctx, it, markChanged = false) }
        s.putBool("locked", locked)
        StateSync.markLocal(ctx, StateSync.KEY_WEEK)
    }

    /**
     * «تایم زنگ ۱..n» — یک فهرستِ مشترک برای همهٔ روزها به صورت «HH:MM-HH:MM».
     */
    fun saveBellTimes(ctx: Context, bells: List<String>, markChanged: Boolean = true) {
        val arr = JSONArray()
        bells.forEach { arr.put(it) }
        store(ctx).putString("bell_times", arr.toString())
        if (markChanged) StateSync.markLocal(ctx, StateSync.KEY_WEEK)
    }

    fun saveShift(
        ctx: Context,
        cycleWeeks: Int,
        anchorIso: String,
        fixedEvening: Boolean,
        pattern: List<String> = emptyList(),
        thisWeekShift: Shift? = null,
        cycleWeekOffset: Int? = null) {
        val s = store(ctx)
        s.putInt("cycle_weeks", cycleWeeks)
        s.putString("anchor", anchorIso)
        s.putBool("fixed_evening", fixedEvening)
        // الگوی هفتگیِ دستیِ قدیمی را با ذخیره‌ی تازه پاک می‌کنیم — شیفتِ هفته‌ها
        // از این‌جا به‌بعد «مشتق» از شیفتِ هفته‌ی جاری است.
        s.putString("week_pattern", if (pattern.isNotEmpty()) pattern.joinToString(",") else "")
        thisWeekShift?.let { s.putString("this_week_shift", if (it == Shift.EVENING) "evening" else "morning") }
        cycleWeekOffset?.let { s.putInt("cycle_week_offset", it.coerceIn(1, cycleWeeks)) }
        StateSync.markLocal(ctx, StateSync.KEY_SHIFT)
    }

    /** بخش اول تنظیمات: شیفتِ هفته‌ی جاری. لنگر = شنبه‌ی همین هفته می‌شود. */
    fun setCurrentWeekShift(ctx: Context, want: Shift) {
        val snap = load(ctx)
        val weekStart = SchoolShift.startOfPersianWeek(LocalDate.now(JalaliDate.TEHRAN)).toString()
        saveShift(ctx, snap.cycleWeeks, weekStart, want == Shift.EVENING, thisWeekShift = want)
    }

    /** بخش دوم: چرخه‌ی شیفت (ثابت/دوهفته‌ای/چهارهفته‌ای). */
    fun setCycleWeeks(ctx: Context, weeks: Int) {
        val snap = load(ctx)
        if (weeks !in listOf(1, 2, 4)) return
        saveShift(ctx, weeks, snap.anchorIso, snap.thisWeekShift == Shift.EVENING, thisWeekShift = snap.thisWeekShift, cycleWeekOffset = 1)
    }

    /** هفته‌ی چندمِ چرخه، هفته‌ی جاری است؟ (فقط برای چرخه‌های ۲/۴ هفته‌ای). */
    fun setCycleWeekOffset(ctx: Context, weekPos: Int) {
        val snap = load(ctx)
        if (snap.cycleWeeks <= 1) return
        // لنگر = شنبهٔ همین هفته تا «هفته‌ی k = همین هفته» دقیق دربیاید.
        val weekStart = SchoolShift.startOfPersianWeek(LocalDate.now(JalaliDate.TEHRAN)).toString()
        saveShift(ctx, snap.cycleWeeks, weekStart, snap.fixedEvening, thisWeekShift = snap.thisWeekShift, cycleWeekOffset = weekPos)
    }

    fun saveVirtualHours(ctx: Context, mh: Int, mm: Int, nh: Int, nm: Int) {
        val s = store(ctx)
        s.putInt("virt_m_h", mh); s.putInt("virt_m_m", mm)
        s.putInt("virt_n_h", nh); s.putInt("virt_n_m", nm)
        StateSync.markLocal(ctx, StateSync.KEY_SHIFT)
    }

    fun saveTimes(
        ctx: Context,
        morningHour: Int, morningMinute: Int, wakeLeadMin: Int,
        noonHour: Int, noonMinute: Int,
        sleepMorning: String, sleepEvening: String) {
        val s = store(ctx)
        s.putInt("m_hour", morningHour); s.putInt("m_min", morningMinute)
        s.putInt("wake_lead", wakeLeadMin)
        s.putInt("n_hour", noonHour); s.putInt("n_min", noonMinute)
        s.putString("sleep_am", sleepMorning); s.putString("sleep_pm", sleepEvening)
        StateSync.markLocal(ctx, StateSync.KEY_SHIFT)
    }

    fun saveExitTimes(ctx: Context, morning: String, noon: String) {
        val s = store(ctx)
        s.putString("exit_am", morning)
        s.putString("exit_pm", noon)
        StateSync.markLocal(ctx, StateSync.KEY_SHIFT)
    }

    fun saveLunarOffset(ctx: Context, offset: Int) {
        store(ctx).putInt("lunar_offset", offset.coerceIn(-2, 2))
        StateSync.markLocal(ctx, StateSync.KEY_SHIFT)
    }

    /**
     * آلفا = شیفتِ بلوکِ لنگر ([thisWeekShift]).
     * بتا = هفته‌ی چندمِ همان بلوک ([cycleWeekOffset]، ۱..N).
     * هر بلوک N هفته همان شیفت می‌ماند، بعد به شیفت مخالف برمی‌گردد.
     */
    fun shiftOf(snap: Snapshot, date: LocalDate): Shift {
        if (snap.weekPattern.size == snap.cycleWeeks && snap.weekPattern.isNotEmpty()) {
            val pos = Math.floorMod(SchoolShift.weekIndex(snap.anchorIso, date), snap.cycleWeeks.toLong()).toInt()
            return if (snap.weekPattern.getOrNull(pos) == "evening") Shift.EVENING else Shift.MORNING
        }
        if (snap.cycleWeeks == 1) return snap.thisWeekShift
        val k = SchoolShift.weekIndex(snap.anchorIso, date)
        val totalPos = snap.cycleWeekOffset - 1L + k
        val block = Math.floorDiv(totalPos, snap.cycleWeeks.toLong())
        return if (Math.floorMod(block, 2L) == 0L) snap.thisWeekShift else snap.thisWeekShift.opposite()
    }

    /** هفته‌ی چندمِ بلوکِ جاری (۱..cycleWeeks) — متغیر بتا برای [date]. */
    fun cycleWeekPos(snap: Snapshot, date: LocalDate): Int {
        if (snap.cycleWeeks <= 1) return 1
        return Math.floorMod(snap.cycleWeekOffset - 1L + SchoolShift.weekIndex(snap.anchorIso, date), snap.cycleWeeks.toLong()).toInt() + 1
    }

    fun weekOrdinal(n: Int): String = when (n) {
        1 -> "اولین"
        2 -> "دومین"
        3 -> "سومین"
        4 -> "چهارمین"
        else -> toPersianDigits(n.toString()) + "مین"
    }

    fun captionOf(snap: Snapshot, date: LocalDate): String {
        val shift = shiftOf(snap, date)
        if (snap.cycleWeeks == 1) return "همیشه ${shift.label}"
        return "این هفته ${weekOrdinal(cycleWeekPos(snap, date))} هفته از ${shift.label} است"
    }

    fun lessonsFor(snap: Snapshot, date: LocalDate): List<String> {
        val idx = SchoolShift.dayIndex(date)
        if (idx !in 1..5) return emptyList()
        return snap.days[idx].orEmpty()
    }

    fun isSchoolHoliday(snap: Snapshot, date: LocalDate, ctx: Context? = null): Boolean {
        val idx = SchoolShift.dayIndex(date)
        if (idx >= 6) return true // پنجشنبه و جمعه
        if (ctx != null && CalendarOccasions.isOfficialHoliday(ctx, date, snap.lunarOffset)) return true
        val j = JalaliDate.toJalali(date.toString()) ?: return false
        if (IranOfficialHolidays.occasion(j) != null) return true
        return lunarOccasion(j, snap.lunarOffset) != null
    }

    /** تعطیل رسمی / آخر هفته / مرخصی ثبت‌شده. */
    fun isSchoolOff(ctx: Context, snap: Snapshot, date: LocalDate): Boolean =
        isSchoolHoliday(snap, date, ctx) || isOnLeave(ctx, date.toString())

    /** کش روزهای تعطیل برای گیرندهٔ آلارم. */
    fun refreshOffCache(ctx: Context) {
        val snap = load(ctx)
        val today = LocalDate.now(JalaliDate.TEHRAN)
        val isos = (0..40).map { today.plusDays(it.toLong()) }
            .filter { isSchoolOff(ctx, snap, it) }
            .map { it.toString() }
        SchoolOffCache.write(ctx, isos)
    }

    fun occasionLabel(snap: Snapshot, date: LocalDate): String? {
        val j = JalaliDate.toJalali(date.toString()) ?: return null
        IranOfficialHolidays.occasion(j)?.let { return it }
        lunarOccasion(j, snap.lunarOffset)?.let { return it }
        val idx = SchoolShift.dayIndex(date)
        return when (idx) {
            6 -> "پنجشنبه"
            7 -> "جمعه"
            else -> null
        }
    }

    fun holidayRoutine(date: LocalDate): String {
        return when (SchoolShift.dayIndex(date)) {
            6 -> "امروز پنجشنبه است؛ مرور سبک درس‌ها و کمی استراحت."
            7 -> "امروز جمعه است؛ خانواده، بازی و خواب کافی."
            else -> "امروز تعطیل رسمی است؛ روتین آرام: کتاب آزاد و پیاده‌روی کوتاه."
        }
    }

    private fun lunarOccasion(j: JalaliDate.Jalali, offset: Int): String? {
        if (offset == 0) {
            return IranOfficialHolidays.lunarOn(j)
        }
        val iso = JalaliDate.toGregorianIso(j) ?: return null
        val shifted = LocalDate.parse(iso).minusDays(offset.toLong())
        val sj = JalaliDate.toJalali(shifted.toString()) ?: return null
        return IranOfficialHolidays.lunarOn(sj)
    }

    fun prepBag(ctx: Context, iso: String) = store(ctx).getBool("bag_$iso", false)
    fun prepHw(ctx: Context, iso: String) = store(ctx).getBool("hw_$iso", false)

    /**
     * تیکِ «کیف/تکالیف» بعد از یک‌بار زدن **قفل** می‌شود تا ساعتِ خروج؛
     * آن‌وقت [maybeRefreshAtExit] قفل‌ها را برای روز بعد باز می‌کند.
     */
    fun bagLocked(ctx: Context, iso: String) = store(ctx).getBool("lock_bag_$iso", false)
    fun hwLocked(ctx: Context, iso: String) = store(ctx).getBool("lock_hw_$iso", false)

    fun setPrepBag(ctx: Context, iso: String, v: Boolean) {
        store(ctx).putBool("bag_$iso", v)
        if (v) store(ctx).putBool("lock_bag_$iso", true) else store(ctx).remove("lock_bag_$iso")
        StateSync.markLocal(ctx, StateSync.KEY_CHECKS)
    }

    fun setPrepHw(ctx: Context, iso: String, v: Boolean) {
        store(ctx).putBool("hw_$iso", v)
        if (v) store(ctx).putBool("lock_hw_$iso", true) else store(ctx).remove("lock_hw_$iso")
        StateSync.markLocal(ctx, StateSync.KEY_CHECKS)
    }

    // --- آمادگیِ امتحان (فقط وقتی امتحان وجود دارد) ---
    private val EXAM_PREP = listOf("مرور خلاصهٔ درس", "حل تمرین‌های کلیدی", "فلش‌کارت‌ها", "یک نمونه‌سؤال")

    fun examPrepOptions(): List<String> = EXAM_PREP

    fun examPrepDone(ctx: Context, iso: String, option: String): Boolean =
        store(ctx).getBool("examprep_${iso}_$option", false)

    fun setExamPrepDone(ctx: Context, iso: String, option: String, v: Boolean) {
        store(ctx).putBool("examprep_${iso}_$option", v)
        StateSync.markLocal(ctx, StateSync.KEY_CHECKS)
    }
    fun examOf(ctx: Context, iso: String) = store(ctx).getString("exam_$iso")
    fun setExam(ctx: Context, iso: String, subject: String) {
        store(ctx).putString("exam_$iso", subject)
        StateSync.markLocal(ctx, StateSync.KEY_CHECKS)
    }
    fun reportOf(ctx: Context, iso: String) = store(ctx).getString("rep_$iso")
    fun setReport(ctx: Context, iso: String, text: String) {
        store(ctx).putString("rep_$iso", text)
        StateSync.markLocal(ctx, StateSync.KEY_CHECKS)
    }

    fun virtualDays(ctx: Context): Set<String> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_days", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }

    fun isVirtual(ctx: Context, iso: String): Boolean = iso in virtualDays(ctx)

    fun setVirtual(ctx: Context, iso: String, on: Boolean) {
        val set = virtualDays(ctx).toMutableSet()
        if (on) set += iso else set -= iso
        val arr = JSONArray(); set.sorted().forEach { arr.put(it) }
        store(ctx).putString("virtual_days", arr.toString())
        StateSync.markLocal(ctx, StateSync.KEY_VIRTUAL)
    }

    fun setVirtualRange(ctx: Context, fromIso: String, toIso: String, on: Boolean) {
        val from = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return
        val to = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return
        var d = from
        while (!d.isAfter(to)) {
            setVirtual(ctx, d.toString(), on)
            d = d.plusDays(1)
        }
    }

    fun packIdForSubject(subject: String): String? {
        if (subject.isBlank() || subject == SPORT) return null
        return BookModuleRegistry.modules.firstOrNull { shortBookName(it.title) == subject }
            ?.packs?.firstOrNull()?.packId
    }

    fun saveExamReport(ctx: Context, iso: String, subject: String, text: String) {
        setReport(ctx, iso, text)
        val packId = packIdForSubject(subject) ?: return
        if (text.isBlank()) return
        TeachStats.noteSchoolExam(ctx, packId, iso, text)
        StudyActivity.add(ctx, packId, "school_exam", "گزارش امتحان مدرسه ($iso): $text")
    }

    fun wakeHourMinute(snap: Snapshot, shift: Shift): Pair<Int, Int> {
        return if (shift == Shift.MORNING) {
            var m = snap.morningHour * 60 + snap.morningMinute - snap.wakeLeadMin
            if (m < 0) m = 0
            (m / 60) to (m % 60)
        } else {
            snap.noonHour to snap.noonMinute
        }
    }

    fun sleepText(snap: Snapshot, shift: Shift): String =
        if (shift == Shift.MORNING) snap.sleepMorning else snap.sleepEvening

    const val ALARM_MORNING = "school_wake_morning"
    const val ALARM_NOON = "school_wake_noon"

    fun syncAlarms(ctx: Context, reminders: ReminderScheduler, snap: Snapshot, date: LocalDate) {
        SchoolAlarmStore.sync(ctx, reminders, snap, date)
    }

    fun alarmIsSet(reminders: ReminderScheduler, snap: Snapshot, date: LocalDate, ctx: Context? = null): Boolean {
        if (ctx != null && isSchoolOff(ctx, snap, date)) return false
        val shift = shiftOf(snap, date)
        val id = if (shift == Shift.MORNING) ALARM_MORNING else ALARM_NOON
        val r = reminders.find(id) ?: return false
        val (h, m) = wakeHourMinute(snap, shift)
        return r.enabled && r.hour == h && r.minute == m
    }

    // ------------------------------------------------------------ کلاس مجازی

    /** یک جلسه‌ی کلاس مجازی در یک روز هفته (شنبه=۱ … پنجشنبه=۵). */
    data class VirtualSession(
        val dayIndex: Int,
        val startH: Int,
        val startM: Int,
        val endH: Int,
        val endM: Int,
        val subject: String = "",
        val eveStartH: Int = 14,
        val eveStartM: Int = 0,
        val eveEndH: Int = 15,
        val eveEndM: Int = 0) {
        val timeFa: String
            get() = timeFaFor(Shift.MORNING)
        fun timeFaFor(shift: Shift): String {
            val (a, b, c, d) = if (shift == Shift.MORNING) listOf(startH, startM, endH, endM)
            else listOf(eveStartH, eveStartM, eveEndH, eveEndM)
            return toPersianDigits("%d:%02d".format(a, b)) +
                " تا " + toPersianDigits("%d:%02d".format(c, d))
        }
        fun endMinFor(shift: Shift): Int =
            if (shift == Shift.MORNING) endH * 60 + endM else eveEndH * 60 + eveEndM
    }

    fun virtualSessions(ctx: Context): List<VirtualSession> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_sessions", "[]")) }
            .getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            VirtualSession(
                dayIndex = o.optInt("dayIndex", 1).coerceIn(1, 5),
                startH = o.optInt("startH", 8).coerceIn(0, 23),
                startM = o.optInt("startM", 0).coerceIn(0, 59),
                endH = o.optInt("endH", 9).coerceIn(0, 23),
                endM = o.optInt("endM", 0).coerceIn(0, 59),
                subject = o.optString("subject"),
                eveStartH = o.optInt("eveStartH", 14).coerceIn(0, 23),
                eveStartM = o.optInt("eveStartM", 0).coerceIn(0, 59),
                eveEndH = o.optInt("eveEndH", 15).coerceIn(0, 23),
                eveEndM = o.optInt("eveEndM", 0).coerceIn(0, 59))
        }.sortedBy { it.dayIndex }
    }

    fun saveVirtualSessions(ctx: Context, list: List<VirtualSession>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("dayIndex", s.dayIndex)
                    .put("startH", s.startH).put("startM", s.startM)
                    .put("endH", s.endH).put("endM", s.endM)
                    .put("subject", s.subject))
        }
        store(ctx).putString("virtual_sessions", arr.toString())
        StateSync.markLocal(ctx, StateSync.KEY_VIRTUAL)
    }

    /** یک بازه‌ی تاریخ مجازی (برای نمایش در آکاردیون و حذف تکی). */
    data class VirtualRange(val id: String, val fromIso: String, val toIso: String)

    fun virtualRanges(ctx: Context): List<VirtualRange> {
        val arr = runCatching { JSONArray(store(ctx).getString("virtual_ranges", "[]")) }
            .getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            VirtualRange(
                id = o.optString("id"),
                fromIso = o.optString("from"),
                toIso = o.optString("to"))
        }.sortedBy { it.fromIso }
    }

    /** ثبت بازه + اعمال روی روزها؛ شناسه برمی‌گردد تا در آکاردیون لیست شود. */
    fun addVirtualRange(ctx: Context, fromIso: String, toIso: String): VirtualRange? {
        val from = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return null
        val to = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return null
        val (a, b) = if (from.isAfter(to)) to to from else from to to
        val range = VirtualRange(id = "vr_${System.currentTimeMillis()}", a.toString(), b.toString())
        val list = virtualRanges(ctx).toMutableList()
        list += range
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
        }
        store(ctx).putString("virtual_ranges", arr.toString())
        setVirtualRange(ctx, a.toString(), b.toString(), true)
        return range
    }

    fun removeVirtualRange(ctx: Context, id: String) {
        val list = virtualRanges(ctx)
        val gone = list.firstOrNull { it.id == id } ?: return
        val arr = JSONArray()
        list.filterNot { it.id == id }.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
        }
        store(ctx).putString("virtual_ranges", arr.toString())
        setVirtualRange(ctx, gone.fromIso, gone.toIso, false)
    }

    // ------------------------------------------------- واژه‌ی «امروز/فردا»

    /**
     * واژه‌ی روز مقصد: اگر همان روزِ جاری باشد «امروز»، اگر فردا باشد «فردا»،
     * وگرنه نام روز هفته.
     */
    fun dayWordFor(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "امروز"
        today.plusDays(1) -> "فردا"
        else -> JalaliDate.weekDayFa(date.toString())
    }

    /**
     * روزِ مقصدِ آماده‌سازی با لحاظ‌کردن عبور از نیمه‌شب:
     * بعد از ساعت ۱۲ شب، «فردا»ی دیشب همان «امروز» است.
     * (تا ساعت ۴ صبح هنوز همان روزِ پیش‌رو مدنظر است.)
     */
    fun prepTargetDate(now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN)): LocalDate {
        val today = now.toLocalDate()
        return if (now.hour < 4) today else today.plusDays(1)
    }

    /**
     * روزی که کارت داشبورد باید نشان بدهد:
     *  - تا وقتی ساعت خروج (یا پایان کلاس مجازی) نرسیده، **امروز**؛
     *  - بعد از آن، نخستین روز مدرسه‌ی بعد.
     * باگ قبلی: بعد از ساعت ۴ صبح همیشه «فردا» بود و پنجشنبه/جمعه به شنبه می‌پرید
     * در حالی که تاریخِ روی کارت هنوز امروز بود.
     */
    fun dashboardShowDate(
        snap: Snapshot,
        now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN),
        virtualEndMin: Int? = null,
        ctx: Context? = null): LocalDate {
        val today = now.toLocalDate()
        if (ctx != null && isSchoolOff(ctx, snap, today)) {
            return firstSchoolDay(snap, today.plusDays(1), ctx)
        }
        if (isSchoolHoliday(snap, today, ctx)) return firstSchoolDay(snap, today.plusDays(1), ctx)
        val shift = shiftOf(snap, today)
        val gate = virtualEndMin ?: minutesOf(exitOf(snap, shift))
        val nowMin = now.hour * 60 + now.minute
        return if (nowMin < gate) today else firstSchoolDay(snap, today.plusDays(1), ctx)
    }

    /** برچسب کاملِ روز مقصد همراه با شیفت: «امروز صبح» / «فردا ظهر». */
    fun dayLabelFor(date: LocalDate, today: LocalDate, shift: Shift): String =
        "${dayWordFor(date, today)} ${if (shift == Shift.MORNING) "صبح" else "ظهر"}"

    /** نخستین روزِ غیرتعطیل از [from] به بعد (برای آماده‌سازیِ روز بعد). */
    fun firstSchoolDay(snap: Snapshot, from: LocalDate, ctx: Context? = null): LocalDate =
        generateSequence(from) { it.plusDays(1) }
            .take(21)
            .firstOrNull { d ->
                !isSchoolHoliday(snap, d, ctx) && (ctx == null || !isOnLeave(ctx, d.toString()))
            }
            ?: from

    /** آیا کلاس‌های این هفته تمام شده (بعد از خروج چهارشنبه یا پنجشنبه/جمعه)؟ */
    fun weekClassesDone(
        snap: Snapshot,
        now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN),
        ctx: Context? = null): Boolean {
        val today = now.toLocalDate()
        val idx = SchoolShift.dayIndex(today)
        if (idx >= 6) return true
        if (idx <= 4) return false
        if (ctx != null && isSchoolOff(ctx, snap, today)) return true
        val shift = shiftOf(snap, today)
        val virt = virtualSessions(ctx ?: return now.hour * 60 + now.minute >= minutesOf(exitOf(snap, shift)))
            .firstOrNull { it.dayIndex == 5 }
        val gate = virt?.endMinFor(shift) ?: minutesOf(exitOf(snap, shift))
        return now.hour * 60 + now.minute >= gate
    }

    /**
     * متن شیفت پس از اتمام هفته:
     * «کلاس‌های این هفته تمام شده و از روز شنبه دومین هفته از ۲هفته شیفتت شروع میشود»
     */
    fun nextCycleStartCaption(
        snap: Snapshot,
        now: LocalDateTime = LocalDateTime.now(JalaliDate.TEHRAN),
        ctx: Context? = null): String? {
        if (snap.cycleWeeks <= 1) return null
        if (!weekClassesDone(snap, now, ctx)) return null
        val next = firstSchoolDay(snap, now.toLocalDate().plusDays(1), ctx)
        val day = JalaliDate.weekDayFa(next.toString())
        val nShift = shiftOf(snap, next)
        val nPos = cycleWeekPos(snap, next)
        return "کلاس‌های این هفته تمام شده و از روز $day ${weekOrdinal(nPos)} هفته از ${nShift.label} شروع میشود"
    }


    // --------------------------------------------- صورتِ تیک‌ها (برای گزارش ماهانه)

    data class CheckEntry(
        val iso: String,
        /** `روزمره` یا `امتحان`. */
        val group: String,
        val title: String,
        val detail: String)

    /**
     * همهٔ تیک‌های ثبت‌شده (روزمره و مربوط به امتحان) برای دسته‌بندیِ ماهانه
     * در صفحهٔ آماده‌سازی فردا.
     */
    fun checkEntries(ctx: Context): List<CheckEntry> {
        val s = store(ctx)
        val out = mutableListOf<CheckEntry>()
        s.keysWithPrefix("bag_").forEach { k ->
            val iso = k.removePrefix("bag_")
            if (iso.length == 10 && s.getBool(k)) out += CheckEntry(iso, "daily", "کیف مدرسه آماده است", "")
        }
        s.keysWithPrefix("hw_").forEach { k ->
            val iso = k.removePrefix("hw_")
            if (iso.length == 10 && s.getBool(k)) out += CheckEntry(iso, "daily", "تکالیف انجام شده", "")
        }
        s.keysWithPrefix("examprep_").forEach { k ->
            val rest = k.removePrefix("examprep_")
            val iso = rest.take(10)
            val option = rest.drop(11)
            if (iso.length == 10 && s.getBool(k)) out += CheckEntry(iso, "exam", option, "آمادگی امتحان")
        }
        s.keysWithPrefix("exam_").forEach { k ->
            val iso = k.removePrefix("exam_")
            if (iso.length == 10) {
                val sub = s.getString(k)
                if (sub.isNotBlank()) out += CheckEntry(iso, "exam", "امتحان $sub", "")
            }
        }
        s.keysWithPrefix("rep_").forEach { k ->
            val iso = k.removePrefix("rep_")
            if (iso.length == 10) {
                val text = s.getString(k)
                if (text.isNotBlank()) out += CheckEntry(iso, "report", "گزارش روز", text)
            }
        }
        return out.sortedWith(compareByDescending<CheckEntry> { it.iso }.thenBy { it.group })
    }

    // --------------------------------- گزارشِ ماهانهٔ آمادگی حضور در مدرسه

    /** یک موردِ تیک‌نخورده در یک روز. */
    data class MissingItem(val iso: String, val title: String)

    /**
     * گزارشِ «آمادگی حضور در مدرسه»: برای هر روزِ مدرسه فقط مواردی که تیک
     * نخورده‌اند برگردانده می‌شود؛ روزی که تیک‌هایش کامل است در گزارش نمی‌آید.
     *  - روزهای تعطیل/پنجشنبه/جمعه و روزهای مرخصی حساب نمی‌شوند؛
     *  - «کیف مدرسه» در روزهای مجازی ناقص شمرده نمی‌شود.
     * خروجی: فهرستِ (نامِ ماهِ شمسی، مواردِ جامانده) — تازه‌ترین ماه اول.
     */
    fun readinessReport(
        ctx: Context,
        today: LocalDate = LocalDate.now(JalaliDate.TEHRAN)): List<Pair<String, List<MissingItem>>> {
        val snap = load(ctx)
        val s = store(ctx)
        val leaveList = leaves(ctx)
        val virtualSet = virtualDays(ctx)
        // از اولین روزی که برایش چیزی ثبت شده (یا از اولِ ماهِ شمسیِ جاری) تا امروز.
        val recorded = listOf("bag_", "hw_", "exam_", "rep_").flatMap { p ->
            s.keysWithPrefix(p).map { it.removePrefix(p).take(10) }
        }.filter { it.length == 10 }.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        val j0 = JalaliDate.toJalali(today.toString())
        val monthStart = j0?.let { JalaliDate.toGregorianIso(JalaliDate.Jalali(it.year, it.month, 1)) }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: today.withDayOfMonth(1)
        val floor = today.minusDays(366)
        val from = (listOfNotNull(recorded.minOrNull(), monthStart).minOrNull() ?: today).let {
            if (it.isBefore(floor)) floor else it
        }
        val out = mutableListOf<MissingItem>()
        var d = from
        while (!d.isAfter(today)) {
            val iso = d.toString()
            val onLeave = leaveList.any { iso >= it.fromIso && iso <= it.toIso }
            if (!isSchoolHoliday(snap, d, ctx) && !onLeave) {
                val virtual = iso in virtualSet
                if (!virtual && !prepBag(ctx, iso)) out += MissingItem(iso, "کیف مدرسه آماده است")
                if (!prepHw(ctx, iso)) out += MissingItem(iso, "تکالیف انجام شده")
            }
            d = d.plusDays(1)
        }
        return out.groupBy { item ->
            val j = JalaliDate.toJalali(item.iso)
            if (j == null) "نامشخص" else "${JalaliDate.monthName(j.month)} ${toPersianDigits(j.year.toString())}"
        }.entries
            .sortedByDescending { e -> e.value.maxOf { it.iso } }
            .map { e -> e.key to e.value.sortedByDescending { it.iso } }
    }

    // ------------------------------------------------------------------ مرخصی

    /** وضعیتِ توجیهِ مرخصی برای مدرسه. */
    const val JUST_FATHER = "father"    // توسط پدر موجه شده
    const val JUST_MOTHER = "mother"    // توسط مادر موجه شده
    const val JUST_NONE = "none"        // موجه نشده
    const val JUST_MEDICAL = "medical"  // با گواهی پزشکی موجه شده (فقط مریضی)

    /** علت‌های پیش‌فرضِ مرخصی؛ کاربر می‌تواند علتِ دلخواه هم اضافه کند. */
    private val DEFAULT_LEAVE_REASONS = listOf("مریضی", "کار شخصی", "خواب موندم", "حوصله نداشتم")
    const val SICK = "مریضی"
    const val ADD_CUSTOM = "＋ اضافه کردن علت خاص"

    data class LeaveRecord(
        val id: String,
        val fromIso: String,
        val toIso: String,
        val reason: String,
        val medicalCert: Boolean = false,
        /** یکی از JUST_*؛ خالی یعنی هنوز انتخاب نشده. */
        val justification: String = "")

    fun leaveReasons(ctx: Context): List<String> {
        val arr = runCatching { JSONArray(store(ctx).getString("leave_reasons", "[]")) }.getOrDefault(JSONArray())
        val custom = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        return (DEFAULT_LEAVE_REASONS + custom).distinct()
    }

    fun addLeaveReason(ctx: Context, title: String) {
        val t = title.trim()
        if (t.isBlank()) return
        val next = (leaveReasons(ctx) + t).distinct()
        store(ctx).putString("leave_reasons", JSONArray().apply { next.forEach { put(it) } }.toString())
        StateSync.markLocal(ctx, StateSync.KEY_LEAVES)
    }

    fun leaves(ctx: Context): List<LeaveRecord> {
        val arr = runCatching { JSONArray(store(ctx).getString("leave_records", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            LeaveRecord(
                id = o.optString("id"),
                fromIso = o.optString("from"),
                toIso = o.optString("to"),
                reason = o.optString("reason"),
                medicalCert = o.optBoolean("med", false),
                justification = o.optString("just", ""))
        }.sortedBy { it.fromIso }
    }

    private fun saveLeaves(ctx: Context, list: List<LeaveRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("from", r.fromIso)
                    .put("to", r.toIso)
                    .put("reason", r.reason)
                    .put("med", r.medicalCert)
                    .put("just", r.justification))
        }
        store(ctx).putString("leave_records", arr.toString())
        StateSync.markLocal(ctx, StateSync.KEY_LEAVES)
        refreshOffCache(ctx)
    }

    fun addLeave(ctx: Context, fromIso: String, toIso: String, reason: String, medicalCert: Boolean, justification: String) {
        val rec = LeaveRecord(
            id = "lv_${System.currentTimeMillis()}",
            fromIso = fromIso,
            toIso = toIso,
            reason = reason,
            medicalCert = medicalCert && reason == SICK,
            justification = justification)
        saveLeaves(ctx, leaves(ctx) + rec)
    }

    fun updateLeaveJustification(ctx: Context, id: String, justification: String) {
        saveLeaves(ctx, leaves(ctx).map { if (it.id == id) it.copy(justification = justification) else it })
    }

    fun removeLeave(ctx: Context, id: String) {
        saveLeaves(ctx, leaves(ctx).filterNot { it.id == id })
    }

    /** آیا این روز در بازه‌ی یکی از مرخصی‌هاست؟ */
    fun leaveOn(ctx: Context, iso: String): LeaveRecord? =
        leaves(ctx).firstOrNull { iso >= it.fromIso && iso <= it.toIso }

    fun isOnLeave(ctx: Context, iso: String): Boolean = leaveOn(ctx, iso) != null

    /** همه‌ی روزهای یک بازه (از تا) به صورت ISO. */
    fun daysBetween(fromIso: String, toIso: String): List<String> {
        val from = runCatching { LocalDate.parse(fromIso) }.getOrNull() ?: return listOf(fromIso)
        val to = runCatching { LocalDate.parse(toIso) }.getOrNull() ?: return listOf(fromIso)
        if (to.isBefore(from)) return listOf(fromIso)
        val out = mutableListOf<String>()
        var d = from
        while (!d.isAfter(to)) { out += d.toString(); d = d.plusDays(1) }
        return out
    }

    fun justificationLabel(code: String): String = when (code) {
        JUST_FATHER -> "توسط پدر موجه شده"
        JUST_MOTHER -> "توسط مادر موجه شده"
        JUST_MEDICAL -> "با گواهی پزشکی موجه شده"
        JUST_NONE -> "موجه نشده"
        else -> "تعیین نشده"
    }

    // ------------------------------------------------------- سینک با سرور

    /** وضعیتِ یک کلید را به صورت JSON بیرون می‌دهد (برای `app_state`). */
    fun exportState(ctx: Context, key: String): String {
        val snap = load(ctx)
        return when (key) {
            StateSync.KEY_WEEK -> JSONObject().apply {
                put("locked", snap.locked)
                val days = JSONObject()
                snap.days.forEach { (d, list) -> days.put(d.toString(), JSONArray().apply { list.forEach { put(it) } }) }
                put("days", days)
                // درسِ دومِ هر خانه + «تایم زنگ ۱..n» (برای همهٔ روزها یکی است)
                val sec = JSONObject()
                snap.second.forEach { (d, list) -> sec.put(d.toString(), JSONArray().apply { list.forEach { put(it) } }) }
                put("seconds", sec)
                put("bells", JSONArray().apply { snap.bells.forEach { put(it) } })
            }.toString()

            StateSync.KEY_SHIFT -> JSONObject().apply {
                put("cycleWeeks", snap.cycleWeeks)
                put("anchorIso", snap.anchorIso)
                put("fixedEvening", snap.fixedEvening)
                put("thisWeekShift", if (snap.thisWeekShift == Shift.EVENING) "evening" else "morning")
                put("cycleWeekOffset", snap.cycleWeekOffset)
                put("weekPattern", JSONArray().apply { snap.weekPattern.forEach { put(it) } }.toString())
                put("morningHour", snap.morningHour)
                put("morningMinute", snap.morningMinute)
                put("noonHour", snap.noonHour)
                put("noonMinute", snap.noonMinute)
                put("wakeLeadMin", snap.wakeLeadMin)
                put("sleepMorning", snap.sleepMorning)
                put("sleepEvening", snap.sleepEvening)
                put("exitMorning", snap.exitMorning)
                put("exitNoon", snap.exitNoon)
                put("lunarOffset", snap.lunarOffset)
                put("virtualMorningHour", snap.virtualMorningHour)
                put("virtualMorningMinute", snap.virtualMorningMinute)
                put("virtualNoonHour", snap.virtualNoonHour)
                put("virtualNoonMinute", snap.virtualNoonMinute)
            }.toString()

            StateSync.KEY_VIRTUAL -> JSONObject().apply {
                put("sessions", JSONArray().apply {
                    virtualSessions(ctx).forEach { s ->
                        put(
                            JSONObject()
                                .put("dayIndex", s.dayIndex)
                                .put("startH", s.startH).put("startM", s.startM)
                                .put("endH", s.endH).put("endM", s.endM)
                                .put("subject", s.subject)
                                .put("eveStartH", s.eveStartH).put("eveStartM", s.eveStartM)
                                .put("eveEndH", s.eveEndH).put("eveEndM", s.eveEndM))
                    }
                })
                put("ranges", JSONArray().apply {
                    virtualRanges(ctx).forEach { r ->
                        put(JSONObject().put("id", r.id).put("from", r.fromIso).put("to", r.toIso))
                    }
                })
                put("days", JSONArray().apply { virtualDays(ctx).sorted().forEach { put(it) } })
            }.toString()

            StateSync.KEY_LEAVES -> JSONObject().apply {
                val arr = JSONArray()
                leaves(ctx).forEach { r ->
                    arr.put(
                        JSONObject()
                            .put("id", r.id)
                            .put("from", r.fromIso)
                            .put("to", r.toIso)
                            .put("reason", r.reason)
                            .put("med", r.medicalCert)
                            .put("just", r.justification))
                }
                put("items", arr)
                put("reasons", JSONArray().apply { leaveReasons(ctx).forEach { put(it) } })
            }.toString()

            StateSync.KEY_CHECKS -> JSONObject().apply {
                val s = store(ctx)
                val bag = JSONObject(); val hw = JSONObject(); val exam = JSONObject()
                val rep = JSONObject(); val prep = JSONObject()
                s.keysWithPrefix("bag_").forEach { k -> bag.put(k.removePrefix("bag_"), s.getBool(k)) }
                s.keysWithPrefix("hw_").forEach { k -> hw.put(k.removePrefix("hw_"), s.getBool(k)) }
                s.keysWithPrefix("exam_").forEach { k -> exam.put(k.removePrefix("exam_"), s.getString(k)) }
                s.keysWithPrefix("rep_").forEach { k -> rep.put(k.removePrefix("rep_"), s.getString(k)) }
                // تیک‌های آمادگیِ امتحان: کلیدِ محلی examprep_<تاریخ>_<گزینه>
                s.keysWithPrefix("examprep_").forEach { k -> prep.put(k.removePrefix("examprep_"), s.getBool(k)) }
                put("bag", bag); put("hw", hw); put("exam", exam)
                put("report", rep); put("examprep", prep)
            }.toString()

            else -> "{}"
        }
    }

    /** اِعمالِ وضعیتِ رسیده از سرور روی دستگاه (آخرین نوشته برنده است). */
    fun importState(ctx: Context, key: String, payload: String) {
        val o = runCatching { JSONObject(payload) }.getOrNull() ?: return
        when (key) {
            StateSync.KEY_WEEK -> {
                val daysObj = o.optJSONObject("days") ?: return
                fun dayList(parent: JSONObject?, d: Int, keepBlank: Boolean): List<String> = runCatching {
                    val arr = parent?.optJSONArray(d.toString()) ?: JSONArray()
                    (0 until arr.length()).map { arr.optString(it) }
                        .let { if (keepBlank) it else it.filter(String::isNotBlank) }
                }.getOrDefault(emptyList())
                val days = (1..5).associateWith { d -> dayList(daysObj, d, keepBlank = false) }
                val seconds = (1..5).associateWith { d -> dayList(o.optJSONObject("seconds"), d, keepBlank = true) }
                val bells = o.optJSONArray("bells")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }
                }
                if (days.values.any { it.isNotEmpty() } || !bells.isNullOrEmpty()) {
                    saveDays(ctx, days, o.optBoolean("locked", false), seconds = seconds, bells = bells)
                }
            }

            StateSync.KEY_SHIFT -> {
                val cycle = o.optInt("cycleWeeks", 2).let { if (it in listOf(1, 2, 4)) it else 2 }
                val anchor = o.optString("anchorIso")
                val pattern = runCatching {
                    val arr = JSONArray(o.optString("weekPattern", "[]"))
                    (0 until arr.length()).map { arr.optString(it) }.filter { it == "morning" || it == "evening" }
                }.getOrDefault(emptyList())
                if (o.has("lunarOffset")) saveLunarOffset(ctx, o.optInt("lunarOffset", 0))
                if (o.has("virtualMorningHour")) {
                    saveVirtualHours(
                        ctx,
                        o.optInt("virtualMorningHour", 8), o.optInt("virtualMorningMinute", 0),
                        o.optInt("virtualNoonHour", 14), o.optInt("virtualNoonMinute", 0))
                }
                if (anchor.isNotBlank()) {
                    val tws = if (o.optString("thisWeekShift") == "evening") Shift.EVENING
                        else if (o.optString("thisWeekShift") == "morning") Shift.MORNING else null
                    saveShift(
                        ctx, cycle, anchor, o.optBoolean("fixedEvening", false), pattern,
                        thisWeekShift = tws,
                        cycleWeekOffset = o.optInt("cycleWeekOffset", 0).takeIf { it in 1..cycle })
                }
                saveTimes(
                    ctx,
                    o.optInt("morningHour", 7), o.optInt("morningMinute", 30), o.optInt("wakeLeadMin", 60),
                    o.optInt("noonHour", 12), o.optInt("noonMinute", 0),
                    o.optString("sleepMorning").ifBlank { "21:30" },
                    o.optString("sleepEvening").ifBlank { "23:00" })
                saveExitTimes(
                    ctx,
                    o.optString("exitMorning").ifBlank { "13:30" },
                    o.optString("exitNoon").ifBlank { "17:30" })
            }

            StateSync.KEY_VIRTUAL -> {
                runCatching {
                    val arr = o.optJSONArray("sessions") ?: JSONArray()
                    val sessions = (0 until arr.length()).mapNotNull { i ->
                        val so = arr.optJSONObject(i) ?: return@mapNotNull null
                        VirtualSession(
                            dayIndex = so.optInt("dayIndex", 1),
                            startH = so.optInt("startH", 8), startM = so.optInt("startM", 0),
                            endH = so.optInt("endH", 9), endM = so.optInt("endM", 0),
                            subject = so.optString("subject"),
                            eveStartH = so.optInt("eveStartH", 14), eveStartM = so.optInt("eveStartM", 0),
                            eveEndH = so.optInt("eveEndH", 15), eveEndM = so.optInt("eveEndM", 0))
                    }
                    if (sessions.isNotEmpty()) saveVirtualSessions(ctx, sessions)
                }
                runCatching {
                    val days = o.optJSONArray("days") ?: JSONArray()
                    val set = (0 until days.length()).map { days.optString(it) }.filter { it.isNotBlank() }.toSet()
                    set.forEach { setVirtual(ctx, it, true) }
                }
            }

            StateSync.KEY_LEAVES -> {
                val arr = o.optJSONArray("items") ?: return
                val remote = (0 until arr.length()).mapNotNull { i ->
                    val it = arr.optJSONObject(i) ?: return@mapNotNull null
                    LeaveRecord(
                        id = it.optString("id"),
                        fromIso = it.optString("from"),
                        toIso = it.optString("to"),
                        reason = it.optString("reason"),
                        medicalCert = it.optBoolean("med", false),
                        justification = it.optString("just", ""))
                }.filter { it.fromIso.isNotBlank() }
                val reasons = o.optJSONArray("reasons")
                reasons?.let { rr ->
                    (0 until rr.length()).map { rr.optString(it) }.filter { it.isNotBlank() }.forEach { addLeaveReason(ctx, it) }
                }
                // ادغام بر اساسِ id (آخرین نوشته برنده است)
                val local = leaves(ctx).associateBy { it.id }.toMutableMap()
                remote.forEach { local[it.id] = it }
                if (local.isNotEmpty()) saveLeaves(ctx, local.values.sortedBy { it.fromIso })
            }

            StateSync.KEY_CHECKS -> {
                val s = store(ctx)
                fun obj(name: String) = o.optJSONObject(name)
                obj("bag")?.keys()?.forEach { k -> s.putBool("bag_$k", obj("bag")!!.optBoolean(k)) }
                obj("hw")?.keys()?.forEach { k -> s.putBool("hw_$k", obj("hw")!!.optBoolean(k)) }
                obj("exam")?.keys()?.forEach { k -> s.putString("exam_$k", obj("exam")!!.optString(k)) }
                obj("report")?.keys()?.forEach { k -> s.putString("rep_$k", obj("report")!!.optString(k)) }
                obj("examprep")?.keys()?.forEach { k -> s.putBool("examprep_$k", obj("examprep")!!.optBoolean(k)) }
            }
        }
    }
}
