package com.hamyareman.ir.ui.study

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * منوی کتاب‌ها دقیقاً از روی `menu.json` هر کتاب روی باکت پارس ساخته شده و در
 * `assets/content/books-menu.json` جمع شده است.
 *
 * هر گره یک `key` دارد (مسیر فایل روی باکت) و یک پرچم `ready` که از v3.0.8 دیگر
 * تعیین‌کنندهٔ مقصد نیست: هر گره/سربرگ همیشه به یک آدرس مشخص روی باکت لینک می‌شود
 * (اگر `key` ندارد، آدرس قراردادی [syntheticKey] ساخته می‌شود) و تصمیم نهایی را
 * [ContentLoader] می‌گیرد:
 *  - فایل هست  → دانلود/کش/نمایش، و با هر تغییر در باکت (ETag) پیشنهاد بروزرسانی
 *  - فایل نیست (۴۰۴) → تک‌placeholder مشترک [SPACEHOLDER_KEY]
 *  - اینترنت مشکل دارد → پیام بررسی اتصال + تلاش دوباره
 */
object BooksMenu {

    /** تک‌فایل مشترک — مقصد همهٔ فایل‌هایی که روی باکت پیدا نشوند. */
    const val SPACEHOLDER_KEY = "Bucket/Html-files/spaceholder.html"

    data class Tab(val title: String, val key: String?, val ready: Boolean?) {
        val hasContent: Boolean get() = ready == true && !key.isNullOrBlank()
    }

    data class Node(
        val kind: String,
        val title: String,
        val key: String?,
        val ready: Boolean?,
        /** صفحهٔ تدریس آمادهٔ این درس در پوشهٔ exam (اگر آپلود شده باشد). */
        val teachKey: String? = null,
        /** صوت تدریس همان درس در پوشهٔ exam. */
        val audioKey: String? = null,
        val tabs: List<Tab>,
        val children: List<Node>,
    ) {
        /**
         * هر گره‌ای که سربرگ یا فرزند دارد باز می‌شود — چه `lesson` باشد چه
         * `container`. فصل‌های ریاضی `container` با صفر سربرگ و پنج فرزندند؛
         * شرط قبلی آن‌ها را برگ می‌دید و درس‌هایشان گم می‌شد.
         */
        val expandable: Boolean get() = tabs.isNotEmpty() || children.isNotEmpty()
        val hasContent: Boolean get() = ready == true && !key.isNullOrBlank()
    }

    data class Book(
        val folder: String,
        val code: String,
        val subject: String,
        val items: List<Node>,
    )

    @Volatile private var books: List<Book> = emptyList()
    @Volatile private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val parsed = runCatching {
                val text = ctx.applicationContext.assets
                    .open("content/books-menu.json").bufferedReader().use { it.readText() }
                val arr = JSONObject(text).getJSONArray("books")
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Book(
                        folder = o.optString("folder"),
                        code = o.optString("code"),
                        subject = o.optString("subject"),
                        items = nodes(o.optJSONArray("items")),
                    )
                }
            }.getOrDefault(emptyList())
            books = parsed
            loaded = true
        }
    }

    private fun nodes(arr: JSONArray?): List<Node> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Node(
                kind = o.optString("kind", "plain"),
                title = o.optString("title"),
                key = o.optStringOrNull("key"),
                ready = o.optBooleanOrNull("ready"),
                teachKey = o.optStringOrNull("teachKey"),
                audioKey = o.optStringOrNull("audioKey"),
                tabs = tabs(o.optJSONArray("tabs")),
                children = nodes(o.optJSONArray("children")),
            )
        }
    }

    private fun tabs(arr: JSONArray?): List<Tab> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Tab(
                title = o.optString("title"),
                key = o.optStringOrNull("key"),
                ready = o.optBooleanOrNull("ready"),
            )
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
        if (isNull(name) || !has(name)) null else optBoolean(name)

    fun all(ctx: Context): List<Book> {
        load(ctx)
        return books
    }

    /** کتاب‌ها با کد سه‌رقمی روی باکت‌اند؛ اپ کدها را به شکل `C905` نگه می‌دارد. */
    fun forBook(ctx: Context, bookCode: String): Book? {
        load(ctx)
        val digits = bookCode.filter { it.isDigit() }
        return books.firstOrNull { it.code == digits }
    }

    /**
     * مقصد نهایی یک گره: همیشه خودِ `key` (حتی اگر `ready` نباشد)؛ نبودن فایل را
     * [ContentLoader] با رفتن به [SPACEHOLDER_KEY] مدیریت می‌کند. فقط وقتی `key`
     * اصلاً وجود ندارد، همان placeholder برگردانده می‌شود.
     */
    @Suppress("UNUSED_PARAMETER")
    fun destination(key: String?, ready: Boolean?): String =
        if (!key.isNullOrBlank()) key else SPACEHOLDER_KEY

    /**
     * آدرس قراردادی (فرضی) روی باکت برای گره/سربرگی که `key` ندارد. بر اساس جایگاه در
     * منو ساخته می‌شود (نه عنوان) تا تغییر عنوان لینک را نشکند:
     * `Bucket/Html-files/app/books/<کد کتاب>/<مسیر گره>[-t<شمارهٔ سربرگ>].html`
     */
    fun syntheticKey(bookCode: String, nodePath: String, tabIndex: Int? = null): String {
        val node = nodePath.replace('.', '-')
        val tab = if (tabIndex != null) "-t${tabIndex + 1}" else ""
        return "Bucket/Html-files/app/books/$bookCode/$node$tab.html"
    }

    fun isPdf(key: String): Boolean = key.endsWith(".pdf", ignoreCase = true)
}
