package com.hamyareman.ir.ui.study

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * منوی کتاب‌ها دقیقاً از روی `menu.json` هر کتاب روی باکت پارس ساخته شده و در
 * `assets/content/books-menu.json` جمع شده است.
 *
 * قرارداد محتوا (از ۳٫۰٫۴): هر گره‌ای که `key` دارد، آدرسش از پیش در اپ هست و
 * همیشه همان فایل باز می‌شود؛ پرچم `ready` در JSON دیگر تعیین‌کننده نیست.
 * یعنی برای نمایش یک محتوای تازه فقط باید فایل دقیقاً در همان مسیر باکت آپلود شود:
 *  - فایل هست  → لود می‌شود (و با هر تغییر در باکت، خودکار به‌روز می‌شود)
 *  - فایل هنوز نیست (۴۰۴) → صفحهٔ «در دست تولید» [SPACEHOLDER_KEY] نشان داده می‌شود
 *  - گره‌ای که اصلاً `key` ندارد → همیشه «به‌زودی»
 *
 * از ۳٫۰٫۷: صفحهٔ تدریس هر درس ریاضی (`exam/ryazif<فصل>d<درس>.html` یا `…review.html`)
 * هم از پیش در اپ تعریف می‌شود، حتی اگر هنگام ساخت JSON هنوز آپلود نشده بود؛ همان
 * شمارش‌گر فصل/درس `scripts/build_books_menu.py` اینجا تکرار شده است. صوت همان درس
 * (`…mp3` کنار html) در [BookNodeScreen] از روی همین کلید ساخته می‌شود.
 */
object BooksMenu {

    /** تک‌فایل «در دست تولید» — مقصد همهٔ گره‌های بی‌محتوا. */
    const val SPACEHOLDER_KEY = "Bucket/Html-files/spaceholder.html"

    private const val BOOKS_BASE = "Bucket/Pdf-files/G09"

    /** slug پوشهٔ exam هر کتاب؛ برای کتاب تازه فقط همین‌جا (و در اسکریپت ساخت منو) اضافه شود. */
    private val EXAM_SLUGS = mapOf("g9-math" to "ryazi")

    data class Tab(val title: String, val key: String?, val ready: Boolean?) {
        val hasContent: Boolean get() = ready == true && !key.isNullOrBlank()
    }

    data class Node(
        val kind: String,
        val title: String,
        val key: String?,
        val ready: Boolean?,
        /** صفحهٔ تدریس این درس در پوشهٔ exam؛ اگر هنوز آپلود نشده باشد اسپیس‌هولدر باز می‌شود. */
        val teachKey: String? = null,
        /** صوت تدریس همان درس در پوشهٔ exam (فقط وقتی هنگام ساخت منو وجود داشته). */
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
            books = parsed.map { withExamKeys(it) }
            loaded = true
        }
    }

    private fun nodes(arr: JSONArray?): List<Node> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val key = o.optStringOrNull("key")
            Node(
                kind = o.optString("kind", "plain"),
                title = o.optString("title"),
                key = key,
                // آدرس از پیش تعریف‌شده = آماده برای باز شدن؛ وجود فایل را باکت تعیین می‌کند.
                ready = if (key != null) true else null,
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
            val key = o.optStringOrNull("key")
            Tab(
                title = o.optString("title"),
                key = key,
                ready = if (key != null) true else null,
            )
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    /**
     * کلید صفحهٔ تدریس را برای هر درسِ کتابی که پوشهٔ exam دارد از پیش می‌سازد.
     * شمارش دقیقاً مثل `convert()` در `scripts/build_books_menu.py` است: هر گرهٔ
     * دارای فرزند یک فصل جدید است و شمارندهٔ درس برای هر فهرست از صفر شروع می‌شود.
     * مقدار موجود در JSON (در صورت وجود) دست‌نخورده می‌ماند.
     */
    private fun withExamKeys(book: Book): Book {
        val slug = EXAM_SLUGS[book.folder] ?: return book
        val chapter = intArrayOf(0)

        fun walk(list: List<Node>): List<Node> {
            var lessonNo = 0
            return list.map { n ->
                val container = n.children.isNotEmpty()
                if (container) chapter[0] += 1 else lessonNo += 1
                var teach = n.teachKey
                if (teach == null && !container && n.tabs.isNotEmpty() && chapter[0] > 0) {
                    val review = n.kind == "wrapup" || n.title.trim().startsWith("جمع")
                    val chapterPart = chapter[0].toString().padStart(2, '0')
                    val suffix = if (review) {
                        "f${chapterPart}review"
                    } else {
                        "f${chapterPart}d${lessonNo.toString().padStart(2, '0')}"
                    }
                    teach = "$BOOKS_BASE/${book.folder}/exam/$slug$suffix.html"
                }
                n.copy(teachKey = teach, children = walk(n.children))
            }
        }

        return book.copy(items = walk(book.items))
    }

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

    /** مقصد نهایی یک گره: فایل خودش اگر آدرس دارد، وگرنه «در دست تولید». */
    fun destination(key: String?, ready: Boolean?): String =
        if (ready == true && !key.isNullOrBlank()) key else SPACEHOLDER_KEY

    fun isPdf(key: String): Boolean = key.endsWith(".pdf", ignoreCase = true)
}
