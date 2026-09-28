package com.hamyareman.ir.ui.content

import android.content.Context
import org.json.JSONObject

/** یک دستهٔ محتوا (آموزشگاه، یوگا، …). */
data class ContentCat(
    val id: String,
    val title: String,
    val emoji: String,
    val order: Int,
)

/** یک فایل محتوا با هر دو نشانی (Appwrite + کلید آروان). */
data class ContentItem(
    val id: String,
    val cat: String,
    val kind: String,   // html | jpg
    val title: String,
    val gender: String, // all | boy | girl
    val aw: String,     // شناسهٔ فایل روی Appwrite
    val key: String,    // کلید (مسیر) روی سرور ایرانی
)

/**
 * کاتالوگ بسته‌شده در assets/content/catalog.json —
 * ساخته‌شده از SandBoxFiles/content-map.json (۱۰۸ فایل).
 */
object ContentCatalog {

    @Volatile private var cats: List<ContentCat> = emptyList()
    @Volatile private var itemsById: Map<String, ContentItem> = emptyMap()
    @Volatile private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val text = ctx.applicationContext.assets.open("content/catalog.json")
                    .bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val cs = mutableListOf<ContentCat>()
                val arr = root.getJSONArray("categories")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    cs += ContentCat(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        emoji = o.optString("emoji", "📁"),
                        order = o.optInt("order", i),
                    )
                }
                val map = mutableMapOf<String, ContentItem>()
                val its = root.getJSONArray("items")
                for (i in 0 until its.length()) {
                    val o = its.getJSONObject(i)
                    val item = ContentItem(
                        id = o.getString("id"),
                        cat = o.getString("cat"),
                        kind = o.getString("kind"),
                        title = o.getString("title"),
                        gender = o.optString("gender", "all"),
                        aw = o.getString("aw"),
                        key = o.getString("key"),
                    )
                    map[item.id] = item
                }
                cats = cs.sortedBy { it.order }
                itemsById = map
                loaded = true
            }
        }
    }

    private fun ensureLoaded(ctx: Context?) {
        if (!loaded && ctx != null) load(ctx)
    }

    fun categories(): List<ContentCat> = cats

    /** آیتم‌های یک دسته (فقط HTML برای فهرست) با اعمال جنسیت. */
    fun itemsOf(cat: String, gender: String): List<ContentItem> =
        itemsById.values
            .filter { it.cat == cat && it.kind == "html" && (it.gender == "all" || it.gender == gender) }
            .sortedBy { it.id }

    fun item(id: String): ContentItem? = itemsById[id]

    /** نگاشت شناسهٔ Appwrite ← کلید آروان (برای ابزارها/آزمایشگاه‌ها هم). */
    fun keyFor(fileId: String): String? = itemsById[fileId]?.key

    /** یک کلید HTML کوچک‌تر برای کاوش — نخستین آیتم HTML. */
    fun sampleHtmlKey(): String? =
        itemsById.values.firstOrNull { it.kind == "html" }?.key

    /** برای ابزارها: از روی شناسهٔ Appwrite (tool-*.html). */
    fun keyForOrNull(fileId: String): String? = keyFor(fileId)
}
