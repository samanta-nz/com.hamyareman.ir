package com.hamyareman.ir.ui.study

import android.content.Context
import android.content.SharedPreferences

/** انتخاب و آخرین نتیجهٔ سنجش واقعی دو سرور محتوا. */
object ServerPrefs {

    enum class Mode { FASTEST, EXTERNAL, INTERNAL }
    enum class Origin { EXTERNAL, INTERNAL }

    private const val FILE = "hamyar_servers"
    private const val KEY_MODE = "server_mode"
    private const val KEY_FASTEST = "fastest_origin"
    private const val KEY_BENCH_AT = "benchmark_at"
    private const val KEY_EXT_OK = "external_ok"
    private const val KEY_EXT_LATENCY = "external_latency"
    private const val KEY_EXT_SPEED = "external_speed"
    private const val KEY_INT_OK = "internal_ok"
    private const val KEY_INT_LATENCY = "internal_latency"
    private const val KEY_INT_SPEED = "internal_speed"

    @Volatile private var prefs: SharedPreferences? = null

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
        set(value) { prefs?.edit()?.putString(KEY_MODE, value.name)?.apply() }

    var fastestOrigin: Origin?
        get() = prefs?.getString(KEY_FASTEST, null)?.let { runCatching { Origin.valueOf(it) }.getOrNull() }
        set(value) { prefs?.edit()?.putString(KEY_FASTEST, value?.name)?.apply() }

    data class StoredProbe(val ok: Boolean, val latencyMs: Long, val bytesPerSecond: Long)

    val lastBenchmarkAt: Long get() = prefs?.getLong(KEY_BENCH_AT, 0L) ?: 0L

    fun stored(origin: Origin): StoredProbe? {
        val p = prefs ?: return null
        val prefixExists = when (origin) {
            Origin.EXTERNAL -> p.contains(KEY_EXT_OK)
            Origin.INTERNAL -> p.contains(KEY_INT_OK)
        }
        if (!prefixExists) return null
        return when (origin) {
            Origin.EXTERNAL -> StoredProbe(
                p.getBoolean(KEY_EXT_OK, false),
                p.getLong(KEY_EXT_LATENCY, 0L),
                p.getLong(KEY_EXT_SPEED, 0L),
            )
            Origin.INTERNAL -> StoredProbe(
                p.getBoolean(KEY_INT_OK, false),
                p.getLong(KEY_INT_LATENCY, 0L),
                p.getLong(KEY_INT_SPEED, 0L),
            )
        }
    }

    fun saveBenchmark(external: StoredProbe, internal: StoredProbe, fastest: Origin?) {
        prefs?.edit()
            ?.putBoolean(KEY_EXT_OK, external.ok)
            ?.putLong(KEY_EXT_LATENCY, external.latencyMs)
            ?.putLong(KEY_EXT_SPEED, external.bytesPerSecond)
            ?.putBoolean(KEY_INT_OK, internal.ok)
            ?.putLong(KEY_INT_LATENCY, internal.latencyMs)
            ?.putLong(KEY_INT_SPEED, internal.bytesPerSecond)
            ?.putString(KEY_FASTEST, fastest?.name)
            ?.putLong(KEY_BENCH_AT, System.currentTimeMillis())
            ?.apply()
    }
}
