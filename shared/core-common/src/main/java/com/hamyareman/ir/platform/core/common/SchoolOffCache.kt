package com.hamyareman.ir.platform.core.common

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * کش روزهای بدون‌مدرسه (تعطیل رسمی، پنجشنبه/جمعه، مرخصی)
 * تا [ReminderReceiver] بدون وابستگی به ماژول اپ بتواند آلارم بیداری/سرویس/حضور را رد کند.
 */
object SchoolOffCache {
    const val PREF = "hamyar_class_plan"
    const val KEY = "off_cache"

    fun isWeekend(date: LocalDate): Boolean {
        val d = date.dayOfWeek
        return d == DayOfWeek.THURSDAY || d == DayOfWeek.FRIDAY
    }

    fun isOff(ctx: Context, date: LocalDate = LocalDate.now(JalaliDate.TEHRAN)): Boolean {
        if (isWeekend(date)) return true
        val iso = date.toString()
        val cache = LocalStore(ctx, PREF).getString(KEY)
        if (cache.isBlank()) return false
        return cache.split(',').any { it == iso }
    }

    fun write(ctx: Context, isos: Collection<String>) {
        LocalStore(ctx, PREF).putString(KEY, isos.joinToString(","))
    }
}
