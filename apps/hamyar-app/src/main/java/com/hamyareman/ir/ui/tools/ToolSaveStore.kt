package com.hamyareman.ir.ui.tools

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.study.StateSync
import org.json.JSONObject

/**
 * ذخیرهٔ ابزارها (تقویم، آزمایشگاه، ماشین‌حساب، مبدل) روی دستگاه و جدول `app_state`.
 * هر ابزار یک کلید داخل JSON واحد است تا جدول تازه‌ای ساخته نشود.
 */
object ToolSaveStore {

    const val KEY = "tool_saves"
    private const val PREF = "hamyar_tool_saves"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun get(ctx: Context, toolId: String): String =
        store(ctx).getString("tool_$toolId")

    fun put(ctx: Context, toolId: String, json: String) {
        store(ctx).putString("tool_$toolId", json)
        StateSync.markLocal(ctx, KEY)
    }

    fun exportAll(ctx: Context): String {
        val o = JSONObject()
        store(ctx).keysWithPrefix("tool_").forEach { k ->
            o.put(k.removePrefix("tool_"), store(ctx).getString(k))
        }
        return o.toString()
    }

    fun importAll(ctx: Context, payload: String) {
        val o = runCatching { JSONObject(payload) }.getOrNull() ?: return
        o.keys().forEach { id ->
            val v = o.optString(id)
            if (v.isNotBlank()) store(ctx).putString("tool_$id", v)
        }
    }

    suspend fun pull(ctx: Context, tables: TablesDbService, uid: String): Boolean {
        if (uid.isBlank()) return false
        val remote = StateSync.pull(ctx, tables, uid, KEY) ?: return false
        val local = exportAll(ctx)
        if (remote.second < StateSync.localAt(ctx, KEY) || remote.first == local) return false
        importAll(ctx, remote.first)
        StateSync.markSyncedAt(ctx, KEY, remote.second)
        return true
    }

    suspend fun push(ctx: Context, tables: TablesDbService, uid: String): Boolean {
        if (uid.isBlank()) return false
        return StateSync.push(ctx, tables, uid, KEY, exportAll(ctx))
    }
}
