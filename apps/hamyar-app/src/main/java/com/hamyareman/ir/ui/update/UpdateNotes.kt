package com.hamyareman.ir.ui.update

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * متنِ تغییراتِ نسخه از فایلِ متنیِ ریپو خوانده می‌شود تا بدون ساختن APK تازه
 * قابل ویرایش باشد.
 *
 * فایل: `content/update-notes.txt` در `Aydinnza/com.hamyareman.ir`
 * (آینهٔ عمومی در `Aydinnza/hamyar-releases`). شکل:
 *
 * ```
 * # 1.75
 * خط اول
 * خط دوم
 * ```
 *
 * اگر txt در دسترس نبود، `update-notes.json` قدیمی به‌عنوان پشتیبان خوانده می‌شود.
 */
object UpdateNotes {

    val TXT_URLS = listOf(
        "https://raw.githubusercontent.com/Aydinnza/com.hamyareman.ir/main/content/update-notes.txt",
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/content/update-notes.txt",
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/update-notes.txt",
    )

    const val JSON_URL =
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/update-notes.json"

    private const val PREF = "hamyar_update_notes"
    private const val KEY_TXT = "txt"
    private const val KEY_JSON = "json"
    private const val KEY_AT = "fetched_at"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun parseTxt(body: String): Map<String, List<String>> {
        val map = linkedMapOf<String, MutableList<String>>()
        var cur = "latest"
        body.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#")) {
                cur = line.trimStart('#').trim().removePrefix("v")
                map.getOrPut(cur) { mutableListOf() }
            } else {
                val clean = line.removePrefix("- ").removePrefix("• ").trim()
                if (clean.isNotEmpty()) map.getOrPut(cur) { mutableListOf() }.add(clean)
            }
        }
        return map
    }

    fun table(ctx: Context): Map<String, List<String>> {
        val txt = store(ctx).getString(KEY_TXT, "")
        if (txt.length > 8) {
            val parsed = runCatching { parseTxt(txt) }.getOrDefault(emptyMap())
            if (parsed.isNotEmpty()) return parsed
        }
        return runCatching {
            val root = JSONObject(store(ctx).getString(KEY_JSON, "{}"))
            buildMap {
                root.keys().forEach { version ->
                    val arr = root.optJSONArray(version) ?: return@forEach
                    put(
                        version,
                        buildList {
                            for (i in 0 until arr.length()) {
                                val line = arr.optString(i).trim()
                                if (line.isNotEmpty()) add(line)
                            }
                        },
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun notes(ctx: Context, version: String): List<String> {
        val table = table(ctx)
        val keys = listOf(version.trim(), version.trim().removePrefix("v"), "latest", "all")
        keys.forEach { k -> table[k]?.takeIf { it.isNotEmpty() }?.let { return it } }
        return emptyList()
    }

    fun fetchedAt(ctx: Context): Long = store(ctx).getLong(KEY_AT, 0L)

    suspend fun refresh(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (!NetState.isOnline(ctx)) return@withContext false
        for (url in TXT_URLS) {
            val ok = runCatching {
                val conn = ResilientHttp.open(url, attempts = 2)
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                runCatching { conn.disconnect() }
                if (body.startsWith("<") || parseTxt(body).isEmpty()) return@runCatching false
                store(ctx).putString(KEY_TXT, body)
                store(ctx).putLong(KEY_AT, System.currentTimeMillis())
                true
            }.getOrDefault(false)
            if (ok) return@withContext true
        }
        runCatching {
            val conn = ResilientHttp.open(JSON_URL, attempts = 2)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            JSONObject(body)
            store(ctx).putString(KEY_JSON, body)
            store(ctx).putLong(KEY_AT, System.currentTimeMillis())
            true
        }.getOrDefault(false)
    }
}
