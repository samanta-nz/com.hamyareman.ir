package com.hamyareman.ir.platform.core.appwrite

import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.FunctionIds
import org.json.JSONObject

/**
 * کلاینتِ تایپ‌شده‌ی **توابع سرور**.
 *
 * چرا این منطق‌ها سمت سرور هستند و نه کلاینت؟
 *  - `notify-guardian`: متن هشدار و مقصدش را سرور می‌نویسد؛ اپ دستکاری‌شده نمی‌تواند
 *    هشدار را بی‌مقصد بفرستد. محدودیت نرخ هم سمت سرور است.
 *  - `album-consent`: «مالک آلبوم کیست» سمت سرور بررسی می‌شود، نه با اعتماد به کش کلاینت.
 *  - `lesson-of-the-day`: ترتیب درس‌ها و پیش‌نیازها از کاتالوگ سرور می‌آید و ساعت دستگاه
 *    (که قابل تغییر است) در آن نقشی ندارد.
 *  - `catalog-digest`: با یک اثر انگشت کوچک، دانلود چندصدسطری کاتالوگ وقتی محتوا
 *    عوض نشده **اصلاً انجام نمی‌شود**.
 *  - `daily-checkin`: تجمیع داده‌ی قابل اشتراک سمت سرور است؛ اپ پدر نیازی به خواندن
 *    تک‌تک جدول‌های زهرا ندارد (و دسترسی‌اش هم نیست).
 *
 * همه‌ی متدها `AppResult` برمی‌گردانند و هیچ‌کدام استثنای نگرفته بیرون نمی‌دهند،
 * پس اگر تابعی deploy نشده باشد اپ روی مسیر محلی/قبلی خودش می‌ماند.
 */
class ServerActions(private val functions: FunctionsService) {

    val isConfigured: Boolean get() = functions.isConfigured

    // --- notify-guardian ----------------------------------------------------

    /**
     * خبردادن به پدر.
     *
     * @param kind یکی از `crisis` / `help` / `checkin` (سرور متن را خودش می‌سازد).
     * @param note یادداشت اختیاری کاربر؛ سرور به ۲۰۰ حرف کوتاه می‌کند.
     */
    suspend fun notifyGuardian(kind: String, note: String = ""): AppResult<GuardianAlert> =
        call(FunctionIds.NOTIFY_GUARDIAN, JSONObject().put("kind", kind).put("note", note)) { o ->
            GuardianAlert(
                sent = o.optBoolean("ok"),
                deduped = o.optBoolean("deduped"),
                alertId = o.optString("alertId"),
                reason = o.optString("error").ifBlank { null },
                helplines = o.optJSONArray("helplines")?.let { array ->
                    (0 until array.length()).mapNotNull { i ->
                        val h = array.optJSONObject(i) ?: return@mapNotNull null
                        ServerHelpline(h.optString("name"), h.optString("number"))
                    }
                } ?: emptyList(),
            )
        }

    // --- lesson-of-the-day --------------------------------------------------

    /**
     * درس امروز از سرور.
     *
     * @param doneIds شناسه‌ی درس‌ها/گره‌هایی که کاربر تمام کرده (فقط شناسه؛ هیچ داده‌ی
     *   خصوصی دیگری فرستاده نمی‌شود).
     */
    suspend fun lessonOfDay(
        track: String = "",
        doneIds: Collection<String> = emptyList(),
    ): AppResult<DailyLesson> =
        call(FunctionIds.LESSON_OF_THE_DAY, JSONObject().apply {
            put("track", track)
            val array = org.json.JSONArray()
            doneIds.forEach { array.put(it) }
            put("done", array)
        }) { o ->
            val lesson = o.optJSONObject("lesson") ?: JSONObject()
            val node = o.optJSONObject("node")
            val position = o.optJSONObject("position") ?: JSONObject()
            DailyLesson(
                dayIso = o.optString("dayIso"),
                track = o.optString("track"),
                lessonId = lesson.optString("id"),
                title = lesson.optString("title"),
                subject = lesson.optString("subject"),
                body = lesson.optString("body"),
                grade = lesson.optInt("grade"),
                quizCount = o.optInt("quizCount"),
                review = o.optString("mode") == "review",
                index = position.optInt("index"),
                total = position.optInt("total"),
                nodeId = node?.optString("id")?.ifBlank { null },
                nodeTitle = node?.optString("title")?.ifBlank { null },
            )
        }

    // --- catalog-digest -----------------------------------------------------

    /** اثر انگشت کاتالوگ؛ برای مقایسه با مقدار کش‌شده قبل از دانلود کامل. */
    suspend fun catalogDigest(): AppResult<CatalogDigest> =
        call(FunctionIds.CATALOG_DIGEST, JSONObject()) { o ->
            val counts = LinkedHashMap<String, Int>()
            o.optJSONObject("tables")?.let { tables ->
                tables.keys().forEach { key ->
                    counts[key] = tables.optJSONObject(key)?.optInt("count", -1) ?: -1
                }
            }
            CatalogDigest(digest = o.optString("digest"), counts = counts)
        }

    // --- daily-checkin ------------------------------------------------------

    /** ساخت/به‌روزرسانی خلاصه‌ی امروز برای پدر (فقط با opt-in و پیوند فعال). */
    suspend fun dailyCheckin(): AppResult<DailyCheckin> =
        call(FunctionIds.DAILY_CHECKIN, JSONObject()) { o ->
            val summary = o.optJSONObject("summary") ?: JSONObject()
            DailyCheckin(
                ok = o.optBoolean("ok"),
                dayIso = o.optString("dayIso"),
                note = summary.optString("note"),
                reason = o.optString("error").ifBlank { null },
            )
        }

    // --- album-consent ------------------------------------------------------

    /** تأیید/رد خاطره‌ی پدر در آلبوم، با بررسی مالکیت سمت سرور. */
    suspend fun albumConsent(itemId: String, approve: Boolean): AppResult<Boolean> =
        call(FunctionIds.ALBUM_CONSENT, JSONObject().apply {
            put("action", if (approve) "approve" else "reject")
            put("itemId", itemId)
        }) { o -> o.optBoolean("ok") && o.optBoolean("approved") == approve }

    // --- زیرساخت ------------------------------------------------------------

    private suspend fun <T> call(
        functionId: String,
        body: JSONObject,
        parse: (JSONObject) -> T,
    ): AppResult<T> {
        if (!functions.isConfigured) {
            return AppResult.Err(AppError.Local("بک‌اند تنظیم نشده؛ این کار سمت سرور انجام نمی‌شود."))
        }
        return when (val result = functions.call(functionId, body.toString())) {
            is AppResult.Err -> result
            is AppResult.Ok -> runCatching {
                val json = JSONObject(result.value.body)
                if (!json.optBoolean("ok", true) && result.value.statusCode !in 200..299) {
                    error("server said ${json.optString("error")}")
                }
                AppResult.Ok(parse(json))
            }.getOrElse {
                AppResult.Err(AppError.Local("پاسخ تابع «$functionId» خوانده نشد."))
            }
        }
    }
}

/** شماره‌ی کمکی که سرور برمی‌گرداند — حتی وقتی پیوندی با پدر وجود ندارد. */
data class ServerHelpline(val name: String, val number: String)

/** نتیجه‌ی «به بابا خبر بده». */
data class GuardianAlert(
    val sent: Boolean,
    val deduped: Boolean,
    val alertId: String,
    val reason: String?,
    val helplines: List<ServerHelpline>,
) {
    /** توضیح فارسیِ «چرا نرفت» — صادقانه، چون کاربر در لحظه‌ی سختی است. */
    val reasonFa: String
        get() = when (reason) {
            "no_link" -> "پیوند فعال با بابا نداری؛ اول از «پیوند با پدر» کد بده."
            "no_user" -> "اول وارد شو تا بتوانم خبر بدهم."
            "server_error" -> "سرور نرسید پیام را بفرستد."
            null -> "نرسیدم خبر بدهم."
            else -> "نرسیدم خبر بدهم ($reason)."
        }
}

/** درس انتخاب‌شده‌ی امروز. */
data class DailyLesson(
    val dayIso: String,
    val track: String,
    val lessonId: String,
    val title: String,
    val subject: String,
    val body: String,
    val grade: Int,
    val quizCount: Int,
    val review: Boolean,
    val index: Int,
    val total: Int,
    val nodeId: String?,
    val nodeTitle: String?,
)

/** اثر انگشت کاتالوگ محتوا. */
data class CatalogDigest(val digest: String, val counts: Map<String, Int>) {
    val isValid: Boolean get() = digest.isNotBlank()
}

/** نتیجه‌ی خلاصه‌ی روزانه. */
data class DailyCheckin(val ok: Boolean, val dayIso: String, val note: String, val reason: String?)
