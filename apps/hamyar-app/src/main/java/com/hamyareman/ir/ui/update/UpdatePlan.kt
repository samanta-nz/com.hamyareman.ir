package com.hamyareman.ir.ui.update

/**
 * منطقِ خالصِ «کانالِ آپدیت» — آگاه از Android و `org.json` نیست تا در تست‌های
 * JVM (بدونِ Robolectric) اجرا شود.
 *
 * تنظیماتِ انتشار در **سرور** است: یک ردیفِ قطعی در جدولِ `app_state` با شناسهٔ
 * `app_release` (`userId = "global"`, `key = "app_release"`, ستونِ `payload` =
 * همان JSON پایین). فایلِ APK در **ریپوی عمومیِ انتشار** میزبانی می‌شود و نشانی‌اش
 * در همین ردیف می‌آید؛ پس برای اجباری‌کردنِ یک نسخه، تغییرِ متنِ پیام یا
 * رول‌آوتِ تدریجی هیچ APKی لازم نیست — فقط همان ردیف عوض می‌شود.
 *
 * نمونهٔ `payload`:
 * ```json
 * {"latest":67,"min":0,"url":"https://…/hamyar-1.66.apk","size":28000000,
 *  "sha256":"…","notes":["پلیرِ صوت","ورودِ آفلاین"],"chan":"stable","rollout":100}
 * ```
 */
data class UpdateInfo(
    /** `versionCode` نسخهٔ تازه؛ `0` یعنی «اطلاعاتِ معتبری نیست». */
    val latest: Int = 0,
    /** نامِ خوانای نسخه (مثل `1.66`) برای نمایش؛ خالی = همان `latest`. */
    val name: String = "",
    /** پایین‌تر از این `versionCode` آپدیت **اجباری** می‌شود؛ `0` = هیچ‌وقت. */
    val min: Int = 0,
    val url: String = "",
    /** حجمِ تقریبیِ فایل (بایت) برای نمایش؛ `0` = نامعلوم. */
    val size: Long = 0L,
    /** هشِ فایلِ APK؛ خالی = سرور هش نداده (بررسی به نصب‌کنندهٔ سیستم واگذار می‌شود). */
    val sha256: String = "",
    val notes: List<String> = emptyList(),
    val chan: String = "stable",
    /** درصدِ کاربرانی که این پیام را می‌بینند (۱..۱۰۰). */
    val rollout: Int = 100,
)

/** تصمیمِ نهاییِ اپ برای این نصب. */
sealed interface UpdateDecision {
    data object None : UpdateDecision
    data class Optional(val info: UpdateInfo) : UpdateDecision
    data class Forced(val info: UpdateInfo) : UpdateDecision
}

object UpdatePlan {

    /** شناسهٔ ردیفِ تنظیمات در جدولِ `app_state` (هم برای خواندن هم نوشتن). */
    const val ROW_ID = "app_release"

    /** کانالِ این بیلد. نسخه‌های `chan` دیگر (مثلاً `beta`) روی این بیلد کاری ندارند. */
    const val CHANNEL = "stable"

    /**
     * خواندنِ `payload`. هر خطای قالب → مقادیرِ امن (نتیجهٔ تصمیم = [UpdateDecision.None])
     * تا دادهٔ خرابِ سرور هرگز اپ را از کار نیندازد.
     */
    fun parse(json: String): UpdateInfo = UpdateInfo(
        latest = num(json, "latest").toInt(),
        name = str(json, "name"),
        min = num(json, "min").toInt(),
        url = str(json, "url"),
        size = num(json, "size"),
        sha256 = str(json, "sha256"),
        notes = arr(json, "notes"),
        chan = str(json, "chan").ifBlank { CHANNEL },
        rollout = num(json, "rollout").toInt().let { if (it <= 0) 100 else it.coerceAtMost(100) },
    )

    /**
     * تصمیم بر پایهٔ `versionCode` فعلی و «سطلِ» رول‌آوتِ این نصب.
     *
     * ترتیبِ بررسی مهم است: اگر `url` یا `latest` نبود، اصلاً پیامی نشان داده
     * نمی‌شود (پس انتشارِ ردیفِ ناقص بی‌خطر است).
     */
    fun decisionFor(current: Int, info: UpdateInfo, bucket: Int = 0): UpdateDecision {
        if (info.latest <= 0 || info.url.isBlank()) return UpdateDecision.None
        if (info.chan != CHANNEL) return UpdateDecision.None
        if (current >= info.latest) return UpdateDecision.None
        if (info.rollout < 100 && bucket >= info.rollout) return UpdateDecision.None
        return if (info.min > 0 && current < info.min) {
            UpdateDecision.Forced(info)
        } else {
            UpdateDecision.Optional(info)
        }
    }

    /**
     * نگاشتِ قطعیِ یک نصب به ۰..۹۹ برای رول‌آوتِ تدریجی.
     *
     * پایه‌اش شناسهٔ تصادفیِ خودِ نصب است (نه شمارهٔ تلفن و نه شناسهٔ کاربر)، پس
     * یک نصب همیشه در یک سطل می‌ماند و رول‌آوت رفتارِ پلکانی دارد.
     */
    fun rolloutBucket(seed: String): Int {
        var h = -0x7ee3623b // FNV-1a 32-bit offset basis
        for (ch in seed) {
            h = h xor ch.code
            h *= 16777619
        }
        return (h and 0x7fffffff) % 100
    }

    /** نامِ نسخه برای نمایش: `name` سرور، وگرنه خودِ عددِ نسخه. */
    fun versionLabel(info: UpdateInfo): String =
        info.name.ifBlank { if (info.latest > 0) info.latest.toString() else "" }

    /** حجمِ خوانا برای نمایش (فقط نمایش؛ تصمیم‌گیری به آن وابسته نیست). */
    fun sizeLabel(bytes: Long): String = when {
        bytes <= 0L -> ""
        bytes < 1024L * 1024L -> "${(bytes / 1024).coerceAtLeast(1)} کیلوبایت"
        else -> "${bytes / (1024L * 1024L)} مگابایت"
    }

    // ---- خوانندهٔ کوچکِ JSON (فقط همان چند فیلدی که لازم داریم) ----

    private fun valueStart(s: String, key: String): Int {
        val i = s.indexOf("\"$key\"")
        if (i < 0) return -1
        var j = i + key.length + 2
        while (j < s.length && (s[j] == ' ' || s[j] == ':' || s[j] == '\t' || s[j] == '\n' || s[j] == '\r')) j++
        return if (j < s.length) j else -1
    }

    private fun str(s: String, key: String): String {
        val i = valueStart(s, key)
        if (i < 0 || s[i] != '"') return ""
        val out = StringBuilder()
        var j = i + 1
        while (j < s.length && s[j] != '"') {
            if (s[j] == '\\' && j + 1 < s.length) {
                out.append(s[j + 1])
                j += 2
                continue
            }
            out.append(s[j])
            j++
        }
        return out.toString()
    }

    private fun num(s: String, key: String): Long {
        val i = valueStart(s, key)
        if (i < 0) return 0L
        var j = i
        while (j < s.length && (s[j].isDigit() || s[j] == '-' || s[j] == '+' || s[j] == '.')) j++
        if (j == i) return 0L
        return s.substring(i, j).toDoubleOrNull()?.toLong() ?: 0L
    }

    private fun arr(s: String, key: String): List<String> {
        val i = valueStart(s, key)
        if (i < 0 || s[i] != '[') return emptyList()
        val end = s.indexOf(']', i)
        if (end < 0) return emptyList()
        val body = s.substring(i + 1, end)
        val out = mutableListOf<String>()
        var j = 0
        while (j < body.length) {
            if (body[j] == '"') {
                val sb = StringBuilder()
                j++
                while (j < body.length && body[j] != '"') {
                    if (body[j] == '\\' && j + 1 < body.length) {
                        sb.append(body[j + 1])
                        j += 2
                        continue
                    }
                    sb.append(body[j])
                    j++
                }
                out += sb.toString()
            }
            j++
        }
        return out
    }
}
