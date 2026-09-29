package com.hamyareman.ir.ui.content

import android.content.Context
import com.hamyareman.ir.ui.profile.AppEdition
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
    @Volatile private var mirrorKeys: Map<String, String> = emptyMap()
    @Volatile private var relativeFiles: Map<String, String> = emptyMap()
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
                val mirrors = mutableMapOf<String, String>()
                // catalog مرجع UI است؛ server-map همهٔ payloadهای مشترک از جمله
                // صوت/PDF/HTML تدریس را پوشش می‌دهد.
                map.values.forEach { mirrors[it.aw] = it.key }
                runCatching {
                    val mirrorText = ctx.applicationContext.assets.open("content/server-map.json")
                        .bufferedReader().use { it.readText() }
                    val mirrorArray = JSONObject(mirrorText).getJSONArray("entries")
                    for (i in 0 until mirrorArray.length()) {
                        val entry = mirrorArray.getJSONObject(i)
                        mirrors[entry.getString("id")] = entry.getString("key")
                    }
                }
                cats = cs.sortedBy { it.order }
                itemsById = map
                mirrorKeys = mirrors
                relativeFiles = map.values.associate { it.key.substringAfterLast('/') to it.id }
                loaded = true
            }
        }
    }

    private fun ensureLoaded(ctx: Context?) {
        if (!loaded && ctx != null) load(ctx)
    }

    fun categories(): List<ContentCat> = cats

    private fun belongsToEdition(item: ContentItem): Boolean {
        val lab = Regex("^lab-(\\d{2})-").find(item.id) ?: return true
        return lab.groupValues[1].toIntOrNull() == AppEdition.grade.num
    }

    /** آیتم‌های یک دسته با قفل هم‌زمان پایه و جنسیت همین APK. */
    fun itemsOf(cat: String, gender: String): List<ContentItem> =
        itemsById.values
            .filter {
                it.cat == cat && it.kind == "html" && belongsToEdition(it) &&
                    (it.gender == "all" || it.gender == gender)
            }
            .sortedBy { it.id }

    /** دستهٔ خالیِ پایهٔ دیگر اصلاً به‌صورت کاشیِ قابل کلیک نمایش داده نمی‌شود. */
    fun categoriesFor(gender: String): List<ContentCat> =
        cats.filter { itemsOf(it.id, gender).isNotEmpty() }

    fun item(id: String): ContentItem? = itemsById[id]?.takeIf(::belongsToEdition)

    /** نگاشت همهٔ payloadهای عمومی Appwrite ← کلید قطعی و اتمیک آروان. */
    fun keyFor(fileId: String): String? = mirrorKeys[fileId]

    /** مقصد لینک نسبی در HTMLهای آموزشی، مثل 02-handwriting.html. */
    fun itemIdForRelativeFile(fileName: String): String? =
        relativeFiles[fileName.substringBefore('?').substringBefore('#').substringAfterLast('/')]

    /** یک فایل واقعیِ موجود روی هر دو origin برای سنجش Range و سرعت. */
    fun sampleHtmlItem(): ContentItem? =
        itemsById.values.filter { it.kind == "html" }.minByOrNull { it.id }

    fun sampleHtmlKey(): String? = sampleHtmlItem()?.key

    /** جلد کاشی‌های آموزشگاه؛ شمارهٔ ۱ تا ۳۴ به جلد هم‌موضوع قبلی وصل می‌شود. */
    private val academyCovers = listOf(
        "sk-speed", "sk-hand", "sk-type", "sk-cornell", "sk-summary",
        "sk-debate", "sk-fallacy", "sk-present", "sk-voice", "sk-email", "sk-listen",
        "sk-math", "sk-palace", "sk-lateral", "sk-friend", "sk-feel", "sk-bias",
        "sk-resilience", "sk-plan", "sk-habit", "sk-desk", "sk-win", "sk-ai-what",
        "sk-search", "sk-privacy", "sk-fake", "sk-team", "sk-conflict", "sk-no",
        "sk-budget", "sk-need", "sk-storm", "sk-everyday", "mu-staff",
    )

    fun coverId(item: ContentItem): String {
        if (item.cat == "amozesh") {
            val number = item.id.removePrefix("amz-").toIntOrNull()
            if (number != null) return academyCovers.getOrElse(number - 1) { item.id }
        }
        return when (item.cat) {
            "yoga" -> "hl-yoga"
            "sport" -> "hl-exercise"
            "breath" -> "cl-breath"
            else -> item.id
        }
    }

    fun tileSubtitle(item: ContentItem): String = when (item.cat) {
        "amozesh" -> "آموزش تعاملی مهارت"
        "yoga" -> "حرکت تعاملی با راهنمای کامل"
        "sport" -> "تمرین تعاملی مرحله‌به‌مرحله"
        "breath" -> "تمرین تنفسی تعاملی"
        "lab" -> "آزمایشگاه تعاملی"
        else -> "محتوای تعاملی"
    }

    /** برای ابزارها: از روی شناسهٔ فایل. */
    fun keyForOrNull(fileId: String): String? = keyFor(fileId)
}
