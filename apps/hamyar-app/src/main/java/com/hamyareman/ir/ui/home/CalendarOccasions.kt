package com.hamyareman.ir.ui.home

import android.content.Context
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** نوع مناسبت برای فیلتر نمایش زیر تاریخ داشبورد. */
enum class OccasionKind(val fa: String, val defaultOn: Boolean) {
    OFFICIAL("تعطیل رسمی", true),
    RELIGIOUS("مناسبت مذهبی", true),
    NATIONAL("مناسبت ملی معاصر", true),
    ANCIENT("مناسبت باستانی", true),
    WORLD("مناسبت جهانی", false),
}

data class Occasion(
    val title: String,
    val kind: OccasionKind,
    val holiday: Boolean = false,
)

data class CalEvent(
    val category: String,
    val title: String,
    val holiday: Boolean,
    val shamsiDay: Int = 0,
    val shamsiMonth: Int = 0,
    val hijriDay: Int = 0,
    val hijriMonth: Int = 0,
    val gregDay: Int = 0,
    val gregMonth: Int = 0,
) {
    val kind: OccasionKind
        get() = when (category) {
            "تعطیل رسمی", "تعطیل مدرسه" -> OccasionKind.OFFICIAL
            "جهانی" -> OccasionKind.WORLD
            "مذهبی قمری" -> OccasionKind.RELIGIOUS
            "ملی باستانی ایرانی" -> OccasionKind.ANCIENT
            else -> OccasionKind.NATIONAL
        }
}

object CalendarPrefs {
    private const val PREF = "hamyar_cal_show"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun show(ctx: Context, kind: OccasionKind): Boolean =
        store(ctx).getBool(kind.name, kind.defaultOn)

    fun setShow(ctx: Context, kind: OccasionKind, on: Boolean) {
        store(ctx).putBool(kind.name, on)
    }

    fun lunarOffset(ctx: Context): Int =
        LocalStore(ctx, "hamyar_class_plan").getInt("lunar_offset", 0).coerceIn(-2, 2)
}

/**
 * مناسبت‌های DentalPro (شمسی / قمری / میلادی) برای داشبورد، تقویم کلاسی و تعطیل مدرسه.
 */
object CalendarOccasions {

    val hijriMonthNames = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه",
    )

    @Volatile private var cache: List<CalEvent>? = null

    fun events(ctx: Context): List<CalEvent> {
        cache?.let { return it }
        val raw = runCatching {
            ctx.assets.open("calendar/occasions.json").bufferedReader(Charsets.UTF_8).readText()
        }.getOrDefault("[]")
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            CalEvent(
                category = o.optString("category"),
                title = o.optString("title"),
                holiday = o.optBoolean("is_holiday_in_iran", false),
                shamsiDay = o.optInt("shamsi_day", 0),
                shamsiMonth = o.optInt("shamsi_month", 0),
                hijriDay = o.optInt("qamari_day", 0),
                hijriMonth = o.optInt("qamari_month", 0),
                gregDay = o.optInt("miladi_day", 0),
                gregMonth = o.optInt("miladi_month", 0),
            )
        }
        cache = list
        return list
    }

    fun hijriOf(g: LocalDate): IntArray {
        val h = HijrahDate.from(g)
        return intArrayOf(
            h.get(ChronoField.YEAR),
            h.get(ChronoField.MONTH_OF_YEAR),
            h.get(ChronoField.DAY_OF_MONTH),
        )
    }

    fun hijriFa(g: LocalDate): String {
        val h = hijriOf(g)
        val month = hijriMonthNames.getOrElse(h[1] - 1) { "" }
        return "${h[2]} $month ${h[0]}"
    }

    /** تعطیلی مدارس در نوروز: از ۱ تا ۱۳ فروردین هر سال. */
    fun isSchoolNoruzHoliday(j: JalaliDate.Jalali): Boolean =
        j.month == 1 && j.day in 1..13

    fun matching(ctx: Context, date: LocalDate, lunarOffset: Int = CalendarPrefs.lunarOffset(ctx)): List<CalEvent> {
        val j = JalaliDate.toJalali(date.toString()) ?: return emptyList()
        val shifted = date.minusDays(lunarOffset.toLong())
        val hij = hijriOf(shifted)
        return events(ctx).filter { e ->
            when {
                e.shamsiDay > 0 && e.shamsiMonth > 0 ->
                    j.month == e.shamsiMonth && j.day == e.shamsiDay
                e.hijriDay > 0 && e.hijriMonth > 0 ->
                    hij[1] == e.hijriMonth && hij[2] == e.hijriDay
                e.gregDay > 0 && e.gregMonth > 0 ->
                    date.monthValue == e.gregMonth && date.dayOfMonth == e.gregDay
                else -> false
            }
        }
    }

    fun allOn(ctx: Context, j: JalaliDate.Jalali, lunarOffset: Int = CalendarPrefs.lunarOffset(ctx)): List<Occasion> {
        val iso = JalaliDate.toGregorianIso(j) ?: return emptyList()
        val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return emptyList()
        // پنجشنبه/جمعه فقط «وضعیت هفته» هستند، نه مناسبت تقویمی. متن رنگی
        // آن‌ها مستقیماً در داشبورد ساخته می‌شود و وارد فهرست مناسبت‌ها نمی‌شود.
        val list = matching(ctx, date, lunarOffset).flatMap { e ->
            val base = Occasion(e.title, e.kind, e.holiday)
            if (e.holiday) listOf(Occasion(e.title, OccasionKind.OFFICIAL, true), base) else listOf(base)
        }
        return if (isSchoolNoruzHoliday(j)) {
            list + Occasion("تعطیلات نوروزی مدرسه", OccasionKind.OFFICIAL, false)
        } else list
    }

    fun visibleOn(ctx: Context, j: JalaliDate.Jalali): List<Occasion> {
        val enabled = OccasionKind.entries.filter { CalendarPrefs.show(ctx, it) }.toSet()
        return allOn(ctx, j).filter { it.kind in enabled }
    }

    fun dashboardLine(ctx: Context, j: JalaliDate.Jalali): String? {
        val titles = visibleOn(ctx, j).map { it.title }.distinct()
        return titles.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    fun isOfficialHoliday(ctx: Context, date: LocalDate, lunarOffset: Int): Boolean =
        date.dayOfWeek == DayOfWeek.FRIDAY || matching(ctx, date, lunarOffset).any { it.holiday }

    /** رنگ خانهٔ تقویم: قرمز = تعطیل رسمی/جمعه/مناسبت، آبی = تعطیل مدرسه. */
    enum class DayTone { RED, BLUE, NONE }

    /**
     * در بازهٔ تعطیلات نوروزی مدرسه (۱ تا ۱۳ فروردین): جمعه‌ها و روزهای دارای مناسبت قرمز،
     * بقیهٔ روزهای بازه آبی. بیرون از این بازه: تعطیل رسمی قرمز و پنجشنبه آبی.
     */
    fun dayTone(ctx: Context, date: LocalDate, lunarOffset: Int = CalendarPrefs.lunarOffset(ctx)): DayTone {
        val j = JalaliDate.toJalali(date.toString())
        if (j != null && isSchoolNoruzHoliday(j)) {
            val hasOccasion = matching(ctx, date, lunarOffset).isNotEmpty() ||
                IranOfficialHolidays.occasion(j) != null
            return if (date.dayOfWeek == DayOfWeek.FRIDAY || hasOccasion) DayTone.RED else DayTone.BLUE
        }
        if (isOfficialHoliday(ctx, date, lunarOffset)) return DayTone.RED
        return if (isSchoolWeekend(date)) DayTone.BLUE else DayTone.NONE
    }

    /** پنجشنبه تعطیل مدرسه است، نه تعطیل رسمی. */
    fun isSchoolWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.THURSDAY

    fun monthOccasions(
        ctx: Context,
        year: Int,
        month: Int,
        dim: Int,
        lunarOffset: Int = CalendarPrefs.lunarOffset(ctx),
    ): List<Pair<Int, CalEvent>> {
        val out = mutableListOf<Pair<Int, CalEvent>>()
        val enabled = OccasionKind.entries.filter { CalendarPrefs.show(ctx, it) }.toSet()
        for (d in 1..dim) {
            val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, d)) ?: continue
            val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: continue
            val matched = matching(ctx, date, lunarOffset)
            matched.forEach { e ->
                val kinds = buildList {
                    add(e.kind)
                    if (e.holiday) add(OccasionKind.OFFICIAL)
                }
                if (kinds.any { it in enabled }) out += d to e
            }
            // در بازهٔ نوروز، جمعه‌های بدون مناسبت هم ثبت می‌شوند تا دلیل قرمز بودنشان معلوم باشد.
            if (month == 1 && d in 1..13 && matched.isEmpty() && date.dayOfWeek == DayOfWeek.FRIDAY) {
                out += d to CalEvent(category = "تعطیل رسمی", title = "جمعه", holiday = true, shamsiDay = d, shamsiMonth = 1)
            }
        }
        return out
    }
}
