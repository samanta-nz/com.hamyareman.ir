package com.hamyareman.ir.ui.home

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WisdomLine(val text: String, val author: String) {
    fun oneLine(): String = if (author.isBlank()) text else "$text — $author"
}

object WisdomQuotes {
    private const val PREF = "hamyar_quotes"
    private const val KEY_IDX = "idx"
    private const val KEY_UNTIL = "until"
    private const val KEY_REMOTE = "remote_txt"

    /**
     * منبع زنده: فایلِ متنیِ ریپو (بدون ساختن APK تازه قابل ویرایش).
     * ریپوی سورس خصوصی است، پس آینه‌ی عمومیِ hamyar-releases هم خوانده می‌شود.
     */
    val REMOTE_URLS = listOf(
        "https://raw.githubusercontent.com/Aydinnza/com.hamyareman.ir/main/content/sokhanan.txt",
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/content/sokhanan.txt",
        "https://raw.githubusercontent.com/Aydinnza/hamyar-releases/main/sokhanan.txt",
    )

    fun parse(raw: String): List<WisdomLine> =
        raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { line ->
            val parts = line.split(" — ", limit = 2)
            if (parts.size == 2) WisdomLine(parts[0].trim(), parts[1].trim())
            else WisdomLine(line, "")
        }.toList()

    fun load(context: Context): List<WisdomLine> {
        val remote = LocalStore(context, PREF).getString(KEY_REMOTE, "")
        val raw = if (remote.length > 40) remote else runCatching {
            context.assets.open("quotes/sokhanan.txt").bufferedReader(Charsets.UTF_8).readText()
        }.getOrDefault("")
        val parsed = parse(raw)
        return parsed.ifEmpty { listOf(WisdomLine("همیار من کنارت است.", "")) }
    }

    /** خواندنِ تازه از ریپو؛ شکست شبکه بی‌صدا نادیده گرفته می‌شود. */
    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!NetState.isOnline(context)) return@withContext false
        for (url in REMOTE_URLS) {
            val ok = runCatching {
                val conn = ResilientHttp.open(url, attempts = 2)
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                runCatching { conn.disconnect() }
                if (body.startsWith("<") || parse(body).size < 3) return@runCatching false
                LocalStore(context, PREF).putString(KEY_REMOTE, body)
                true
            }.getOrDefault(false)
            if (ok) return@withContext true
        }
        false
    }

    fun current(context: Context, all: List<WisdomLine>): WisdomLine {
        if (all.isEmpty()) return WisdomLine("همیار من کنارت است.", "")
        val store = LocalStore(context, PREF)
        val now = System.currentTimeMillis()
        var idx = store.getInt(KEY_IDX, -1)
        var until = store.getLong(KEY_UNTIL, 0L)
        if (idx !in all.indices || now >= until) {
            idx = Random.nextInt(all.size)
            val hours = 3.0 + Random.nextDouble()
            until = now + (hours * 3_600_000L).toLong()
            store.putInt(KEY_IDX, idx)
            store.putLong(KEY_UNTIL, until)
        }
        return all[idx]
    }

    fun remainingMs(context: Context): Long {
        val until = LocalStore(context, PREF).getLong(KEY_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(1_000L)
    }

    /** لمس کارت داشبورد — سخن بعدی، جدا از تایمر ۳–۴ ساعته. */
    fun next(context: Context, all: List<WisdomLine>): WisdomLine {
        if (all.isEmpty()) return WisdomLine("همیار من کنارت است.", "")
        val store = LocalStore(context, PREF)
        val cur = store.getInt(KEY_IDX, 0)
        var idx = Random.nextInt(all.size)
        if (all.size > 1 && idx == cur) idx = (idx + 1 + Random.nextInt(all.size - 1)) % all.size
        val hours = 3.0 + Random.nextDouble()
        store.putInt(KEY_IDX, idx)
        store.putLong(KEY_UNTIL, System.currentTimeMillis() + (hours * 3_600_000L).toLong())
        return all[idx]
    }
}
