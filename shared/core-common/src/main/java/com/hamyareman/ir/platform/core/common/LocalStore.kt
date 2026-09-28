package com.hamyareman.ir.platform.core.common

import android.content.Context
import android.content.SharedPreferences

/**
 * ذخیره‌ی محلی سبک بر پایه‌ی SharedPreferences.
 *
 * نقش آن در معماری: «کش/صف محلی» است، نه منبع حقیقت. منبع حقیقت داده‌های قابل‌اشتراک
 * TablesDB است؛ داده‌های هرگز-همگام‌نشده (چرخه، دفترچه، زمان صفحه) فقط همین‌جا می‌مانند.
 * برای داده‌ی حجیم/ساختاریافته در فاز بعد Room + SQLCipher جایگزین می‌شود.
 */
open class LocalStore(context: Context, name: String = "roozhayeman_local") {

    private val storeNameValue = name
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    val storeName: String get() = storeNameValue

    open fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    open fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }

    open fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)
    open fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }

    open fun getFloat(key: String, default: Float = 0f): Float = prefs.getFloat(key, default)
    open fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }

    open fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    open fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }

    open fun getBool(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    open fun putBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }

    open fun getStringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()) ?: emptySet()
    open fun putStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).apply()
    }

    open fun contains(key: String): Boolean = prefs.contains(key)

    /** «پاک‌کردن همه‌ی داده‌های محلی» در تنظیمات حریم خصوصی. */
    open fun clearAll() {
        prefs.edit().clear().apply()
    }

    open fun remove(vararg keys: String) {
        prefs.edit().apply { keys.forEach { remove(it) } }.apply()
    }

    /** همه‌ی کلیدهایی که با این پیشوند شروع می‌شوند (مثلاً «consumed_» برای آب روزانه). */
    open fun keysWithPrefix(prefix: String): Set<String> = prefs.all.keys.filter { it.startsWith(prefix) }.toSet()

    /** حذف دسته‌ای کلیدهای یک پیشوند — برای «پاک‌کردن داده‌ی من» در تنظیمات حریم خصوصی. */
    open fun clearPrefix(prefix: String) {
        prefs.edit().apply { keysWithPrefix(prefix).forEach { remove(it) } }.apply()
    }
}
