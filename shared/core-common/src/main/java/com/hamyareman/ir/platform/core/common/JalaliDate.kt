package com.hamyareman.ir.platform.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * تاریخ جلالی (شمسی).
 *
 * تبدیل با الگوریتم Roozh (چرخه‌ی ۳۳ساله) انجام می‌شود — مستقل از ICU، چون
 * Android ICU کلاس `PersianCalendar` ندارد. نسخه‌ی قبلی آفست ۱۶۰۰/۶۲۱ را
 * اشتباه می‌بست و حدود یک سال جلو می‌زد (مثلاً ۲۰۲۶-۰۹-۱۵ → ۱۴۰۶ به‌جای ۱۴۰۵).
 *
 * قرارداد پروژه:
 *  - «امروز» و مهر زمانی با منطقه‌ی [TEHRAN] است، نه [ZoneId.systemDefault]؛
 *  - کلیدهای ذخیره‌سازی و فاصله‌ی روزها ISO میلادی می‌مانند ([todayIso])؛
 *  - نمایش برای کاربر جلالی با رقم فارسی است ([formatFa]).
 */
object JalaliDate {

    val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    data class Jalali(val year: Int, val month: Int, val day: Int) {
        /** «۱۴۰۵/۰۶/۱۵» با رقم فارسی. */
        val fa: String get() = toPersianDigits("%04d/%02d/%02d".format(year, month, day))

        /** «۱۵ شهریور ۱۴۰۵» */
        val faLong: String get() = toPersianDigits("$day ${monthName(month)} $year")

        /** ذخیره‌سازی: `1405-06-15` با رقم لاتین. */
        val isoLike: String get() = "%04d-%02d-%02d".format(year, month, day)
    }

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val weekDayNames = listOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه",
    )

    /** سال کبیسه‌ی جلالی در چرخه‌ی ۳۳ساله (۱،۵،۹،۱۳،۱۷،۲۲،۲۶،۳۰). */
    fun isLeapYear(jy: Int): Boolean {
        val r = ((jy % 33) + 33) % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    fun daysInMonth(jy: Int, jm: Int): Int = when (jm) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeapYear(jy)) 30 else 29
        else -> 0
    }

    fun isValid(j: Jalali): Boolean =
        j.year in 1200..1600 && j.month in 1..12 && j.day in 1..daysInMonth(j.year, j.month)

    fun monthName(month: Int): String = monthNames.getOrElse(month - 1) { "" }

    /** نام روز هفته‌ی یک تاریخ ISO میلادی (تقویم مدنی، مستقل از منطقه). */
    fun weekDayFa(isoDate: String): String =
        runCatching { weekDayNames[LocalDate.parse(isoDate.take(10)).dayOfWeek.value % 7] }.getOrDefault("")

    fun todayIso(): String = LocalDate.now(TEHRAN).format(iso)

    fun todayJalali(): Jalali = toJalali(System.currentTimeMillis())

    fun fromGregorian(gy: Int, gm: Int, gd: Int): Jalali {
        val c = gregorianToJalali(gy, gm, gd)
        return Jalali(c[0], c[1], c[2])
    }

    /**
     * ISO تاریخ (`yyyy-MM-dd`) یا instant (`…T…Z`) → جلالی.
     * تاریخ تقویمی بدون شیفت منطقه؛ instant با تهران.
     */
    fun toJalali(isoDate: String): Jalali? = runCatching {
        val raw = isoDate.trim()
        if (raw.contains('T')) {
            toJalali(Instant.parse(raw).toEpochMilli())
        } else {
            val date = LocalDate.parse(raw.take(10))
            fromGregorian(date.year, date.monthValue, date.dayOfMonth)
        }
    }.getOrNull()

    fun toJalali(epochMillis: Long): Jalali {
        // LocalDate.ofInstant از جاوا ۹ / API ۳۳ است؛ روی Android 10 → NoSuchMethodError بعد از لاگین.
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TEHRAN).toLocalDate()
        return fromGregorian(date.year, date.monthValue, date.dayOfMonth)
    }

    fun parseJalali(text: String): Jalali? {
        val t = toLatinDigits(text.trim()).replace('/', '-')
        val p = t.split('-')
        if (p.size != 3) return null
        val y = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val d = p[2].toIntOrNull() ?: return null
        val j = Jalali(y, m, d)
        return j.takeIf { isValid(it) }
    }

    /**
     * جلالی → میلادی ISO. تاریخ نامعتبر → null (دیگر به «امروز» برنمی‌گردد؛
     * آن رفتار سن را خراب می‌کرد).
     */
    fun toGregorianIso(jalali: Jalali): String? {
        if (!isValid(jalali)) return null
        return runCatching {
            val c = jalaliToGregorian(jalali.year, jalali.month, jalali.day)
            LocalDate.of(c[0], c[1], c[2]).format(iso)
        }.getOrNull()
    }

    /**
     * سن کامل به سال، نسبت به [on] (پیش‌فرض امروز تهران).
     * قبل از تولدِ امسال هنوز سال تمام نشده.
     */
    fun ageYears(birth: Jalali, on: Jalali = todayJalali()): Int? {
        if (!isValid(birth) || !isValid(on)) return null
        if (birth.year > on.year ||
            (birth.year == on.year && birth.month > on.month) ||
            (birth.year == on.year && birth.month == on.month && birth.day > on.day)
        ) return null
        var age = on.year - birth.year
        if (on.month < birth.month || (on.month == birth.month && on.day < birth.day)) age--
        return age
    }

    /**
     * الگوریتم Roozh: میلادی → جلالی.
     * آفست ۳۵۵۶۶۶ و مبدأ −۱۵۹۵؛ بدون انشعاب غلطِ سال ۱۶۰۰.
     */
    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 +
            (gy2 + 399) / 400 + gd + monthDays[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }

    /** الگوریتم Roozh: جلالی → میلادی. */
    private fun jalaliToGregorian(jyInput: Int, jm: Int, jd: Int): IntArray {
        val jy = jyInput + 1595
        var days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4 + jd +
            if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days -= 1
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days += 1
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val monthLengths = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gd > monthLengths[gm + 1]) {
            gd -= monthLengths[gm + 1]
            gm++
        }
        return intArrayOf(gy, gm + 1, gd)
    }

    /** نمایش فارسی یک تاریخ ISO میلادی یا instant → «۱۴۰۵/۰۶/۱۵». */
    fun formatFa(isoDate: String): String = toJalali(isoDate)?.fa ?: isoDate.replace("-", "/")

    /** «۱۴۰۵/۰۶/۱۵» از روی زمان میلی‌ثانیه‌ای (تهران). */
    fun formatFa(epochMillis: Long): String = toJalali(epochMillis).fa

    /** «۱۵ شهریور ۱۴۰۵» از روی زمان میلی‌ثانیه‌ای. */
    fun formatFaLong(epochMillis: Long): String = toJalali(epochMillis).faLong

    /** «۱۵ شهریور ۱۴۰۵» از روی ISO یا instant. */
    fun formatFaLong(isoDate: String): String = toJalali(isoDate)?.faLong ?: isoDate

    /** ساعت با رقم فارسی در تهران: «۱۴:۳۵». */
    fun clockFa(epochMillis: Long): String {
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TEHRAN)
        return toPersianDigits("%02d:%02d".format(time.hour, time.minute))
    }

    /** «۱۵ شهریور ۱۴۰۵، ۱۴:۳۵» — مهر کامل برای پیام‌ها و گزارش‌ها. */
    fun stampFa(epochMillis: Long): String = "${formatFaLong(epochMillis)}، ${clockFa(epochMillis)}"

    fun daysBetween(fromIso: String, toIso: String): Long = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(fromIso.take(10)), LocalDate.parse(toIso.take(10)))
    }.getOrDefault(0)

    /** ۰۱۲۳۴۵۶۷۸۹ → ۰۱۲۳۴۵۶۷۸۹ (رقم فارسی). */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            append(if (ch in '0'..'9') ('۰' + (ch - '0')) else ch)
        }
    }
}
