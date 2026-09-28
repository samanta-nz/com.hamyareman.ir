package com.hamyareman.ir.platform.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateTest {

    @Test
    fun `persian digits replace latin digits only`() {
        assertEquals("۱۴۰۵/۰۶/۱۵", toPersianDigits("1405-06-15".replace("-", "/")))
        assertEquals("آب ۲ لیتر", toPersianDigits("آب 2 لیتر"))
    }

    @Test
    fun `latin digits normalise persian and arabic input`() {
        assertEquals("123456", toLatinDigits("۱۲۳۴۵۶"))
        assertEquals("123456", toLatinDigits("١٢٣٤٥٦"))
        assertEquals("12a34", toLatinDigits("۱۲a۳۴"))
    }

    @Test
    fun `jalali formatting uses zero padding and persian digits`() {
        val jalali = JalaliDate.Jalali(1405, 6, 15)
        assertEquals("۱۴۰۵/۰۶/۱۵", jalali.fa)
        assertEquals("۱۵ شهریور ۱۴۰۵", jalali.faLong)
        assertEquals("1405-06-15", jalali.isoLike)
    }

    @Test
    fun `month names cover all twelve months`() {
        val expected = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
        (1..12).forEach { month -> assertEquals(expected[month - 1], JalaliDate.monthName(month)) }
        assertEquals("", JalaliDate.monthName(13))
    }

    @Test
    fun `known gregorian dates convert to jalali`() {
        assertEquals(JalaliDate.Jalali(1403, 1, 1), JalaliDate.toJalali("2024-03-20"))
        assertEquals(JalaliDate.Jalali(1404, 1, 1), JalaliDate.toJalali("2025-03-21"))
        assertEquals(JalaliDate.Jalali(1405, 1, 1), JalaliDate.toJalali("2026-03-21"))
        assertEquals(JalaliDate.Jalali(1405, 6, 24), JalaliDate.toJalali("2026-09-15"))
        assertEquals(JalaliDate.Jalali(1368, 10, 11), JalaliDate.toJalali("1990-01-01"))
    }

    @Test
    fun `jalali gregorian round trip for civil years`() {
        for (y in 1380..1410) {
            for (m in 1..12) {
                val last = JalaliDate.daysInMonth(y, m)
                for (d in listOf(1, 15, last)) {
                    val j = JalaliDate.Jalali(y, m, d)
                    val g = JalaliDate.toGregorianIso(j)
                    assertNotNull("$y-$m-$d", g)
                    assertEquals(j, JalaliDate.toJalali(g!!))
                }
            }
        }
    }

    @Test
    fun `leap esfand has 30 days only on leap years`() {
        assertTrue(JalaliDate.isLeapYear(1403))
        assertFalse(JalaliDate.isLeapYear(1404))
        assertEquals(30, JalaliDate.daysInMonth(1403, 12))
        assertEquals(29, JalaliDate.daysInMonth(1404, 12))
        assertTrue(JalaliDate.isValid(JalaliDate.Jalali(1403, 12, 30)))
        assertFalse(JalaliDate.isValid(JalaliDate.Jalali(1404, 12, 30)))
        assertNull(JalaliDate.toGregorianIso(JalaliDate.Jalali(1404, 12, 30)))
        assertEquals("2025-03-20", JalaliDate.toGregorianIso(JalaliDate.Jalali(1403, 12, 30)))
    }

    @Test
    fun `age is completed years from jalali birthday`() {
        val birth = JalaliDate.Jalali(1390, 6, 24)
        assertEquals(15, JalaliDate.ageYears(birth, JalaliDate.Jalali(1405, 6, 24)))
        assertEquals(14, JalaliDate.ageYears(birth, JalaliDate.Jalali(1405, 6, 23)))
        assertEquals(15, JalaliDate.ageYears(birth, JalaliDate.Jalali(1405, 7, 1)))
        assertNull(JalaliDate.ageYears(JalaliDate.Jalali(1406, 1, 1), JalaliDate.Jalali(1405, 1, 1)))
    }

    @Test
    fun `parse jalali accepts slash and persian digits`() {
        assertEquals(JalaliDate.Jalali(1405, 6, 24), JalaliDate.parseJalali("۱۴۰۵/۰۶/۲۴"))
        assertEquals(JalaliDate.Jalali(1405, 6, 24), JalaliDate.parseJalali("1405-06-24"))
        assertNull(JalaliDate.parseJalali("1404-12-30"))
    }

    @Test
    fun `iso instant uses tehran calendar day`() {
        // 2026-09-15T00:30Z = 04:00 تهران همان روز.
        val j = JalaliDate.toJalali("2026-09-15T00:30:00Z")
        assertEquals(JalaliDate.Jalali(1405, 6, 24), j)
    }
}
