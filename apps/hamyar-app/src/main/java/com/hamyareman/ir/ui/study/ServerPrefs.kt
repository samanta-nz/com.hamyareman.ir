package com.hamyareman.ir.ui.study

import android.content.Context
import android.content.SharedPreferences

/** منبع فایل‌های محتوایی اپ، فقط پارس‌پک است. */
object ServerPrefs {

    enum class Mode { INTERNAL }
    enum class Origin { INTERNAL }

    private const val FILE = "hamyar_servers"
    private const val KEY_MODE = "server_mode"
    private const val KEY_BENCH_AT = "benchmark_at"
    private const val KEY_INT_OK = "internal_ok"
    private const val KEY_INT_LATENCY = "internal_latency"
    private const val KEY_INT_SPEED = "internal_speed"

    @Volatile private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs == null) synchronized(this) {
            if (prefs == null) {
                prefs = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                // حالت‌های قدیمی FASTEST/EXTERNAL به‌طور قطعی به پارس‌پک مهاجرت می‌کنند.
                prefs?.edit()
                    ?.putString(KEY_MODE, Mode.INTERNAL.name)
                    ?.remove("fastest_origin")
                    ?.remove("external_ok")
                    ?.remove("external_latency")
                    ?.remove("external_speed")
                    ?.apply()
            }
        }
    }

    /** برای سازگاری تنظیمات قدیمی؛ منبع فعال همیشه پارس‌پک است. */
    var mode: Mode
        get() = Mode.INTERNAL
        set(@Suppress("UNUSED_PARAMETER") value) {
            prefs?.edit()?.putString(KEY_MODE, Mode.INTERNAL.name)?.apply()
        }

    data class StoredProbe(val ok: Boolean, val latencyMs: Long, val bytesPerSecond: Long)

    val lastBenchmarkAt: Long get() = prefs?.getLong(KEY_BENCH_AT, 0L) ?: 0L

    fun stored(): StoredProbe? {
        val p = prefs ?: return null
        if (!p.contains(KEY_INT_OK)) return null
        return StoredProbe(
            p.getBoolean(KEY_INT_OK, false),
            p.getLong(KEY_INT_LATENCY, 0L),
            p.getLong(KEY_INT_SPEED, 0L),
        )
    }

    fun saveBenchmark(internal: StoredProbe) {
        prefs?.edit()
            ?.putString(KEY_MODE, Mode.INTERNAL.name)
            ?.putBoolean(KEY_INT_OK, internal.ok)
            ?.putLong(KEY_INT_LATENCY, internal.latencyMs)
            ?.putLong(KEY_INT_SPEED, internal.bytesPerSecond)
            ?.putLong(KEY_BENCH_AT, System.currentTimeMillis())
            ?.apply()
    }
}
