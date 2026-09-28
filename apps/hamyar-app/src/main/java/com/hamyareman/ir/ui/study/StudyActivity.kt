package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * دفتر رویدادهای درسی — هر کنش یک سطر، غیرقابل ویرایش.
 * سقف ۳۰۰ سطر برای هر پک تا حافظه متورم نشود.
 */
object StudyActivity {

    data class Row(
        val atMs: Long,
        val kind: String,
        val label: String,
    ) {
        val whenFa: String get() = if (atMs > 0) JalaliDate.stampFa(atMs) else "—"
    }

    private const val MAX = 300

    private fun store(ctx: Context) = LocalStore(ctx.applicationContext, "hamyar_study_log")

    private fun key(packId: String) = "al_$packId"

    fun add(ctx: Context, packId: String, kind: String, label: String) {
        if (packId.isBlank() || label.isBlank()) return
        val s = store(ctx)
        val arr = runCatching { JSONArray(s.getString(key(packId), "[]")) }.getOrDefault(JSONArray())
        arr.put(
            JSONObject()
                .put("t", System.currentTimeMillis())
                .put("k", kind)
                .put("l", label),
        )
        while (arr.length() > MAX) arr.remove(0)
        s.putString(key(packId), arr.toString())
    }

    fun rows(ctx: Context, packId: String): List<Row> {
        val arr = runCatching { JSONArray(store(ctx).getString(key(packId), "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(Row(o.optLong("t"), o.optString("k"), o.optString("l")))
            }
        }
    }
}
