package com.hamyareman.ir.ui.study

import android.content.Context
import android.content.SharedPreferences

/**
 * انتخاب سرور محتوا در «تنظیمات سرور».
 * FASTEST = پیش‌فرض: سریع‌ترین سرور (کاوش به سرور داخلی، در صورت خطا خارجی)
 * EXTERNAL = سرور خارجی (Appwrite)
 * INTERNAL = سرور ایرانی (آروان)
 */
object ServerPrefs {

    enum class Mode { FASTEST, EXTERNAL, INTERNAL }

    private const val FILE = "hamyar_servers"
    private const val KEY_MODE = "server_mode"
    private const val KEY_INTERNAL_OK = "internal_probe_ok"
    private const val KEY_INTERNAL_AT = "internal_probe_at"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs == null) synchronized(this) {
            if (prefs == null) {
                prefs = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            }
        }
    }

    var mode: Mode
        get() = runCatching {
            Mode.valueOf(prefs?.getString(KEY_MODE, Mode.FASTEST.name) ?: Mode.FASTEST.name)
        }.getOrDefault(Mode.FASTEST)
        set(value) {
            prefs?.edit()?.putString(KEY_MODE, value.name)?.apply()
        }

    /** نتیجهٔ آخرین کاوش داخلی (null = هنوز کاوشی نشده). */
    var lastProbeOk: Boolean?
        get() = prefs?.let { if (it.contains(KEY_INTERNAL_OK)) it.getBoolean(KEY_INTERNAL_OK, false) else null }
        set(value) {
            prefs?.edit()
                ?.putBoolean(KEY_INTERNAL_OK, value ?: false)
                ?.putLong(KEY_INTERNAL_AT, System.currentTimeMillis())
                ?.apply()
        }

    val lastProbeAt: Long get() = prefs?.getLong(KEY_INTERNAL_AT, 0L) ?: 0L
}
