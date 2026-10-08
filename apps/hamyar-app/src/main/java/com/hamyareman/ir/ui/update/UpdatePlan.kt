package com.hamyareman.ir.ui.update

import com.hamyareman.ir.BuildConfig

/**
 * منطقِ خالصِ «کانالِ آپدیت» — آگاه از Android و `org.json` نیست تا در تست‌های
 * JVM (بدونِ Robolectric) اجرا شود.
 *
 * تنظیمات انتشار در **سرور** است: هر flavor یک ردیف قطعی و جدا در جدول
 * `app_state` دارد (`app_release_grade4` … `app_release_grade12`). payload علاوه
 * بر نسخه، `packageName`، `gradeId` و SHA-256 گواهی امضا را حمل می‌کند. فایل APK
 * در ریپوی عمومی انتشار میزبانی می‌شود و نشانی‌اش در همان ردیف پایه می‌آید؛ پس
 * برای اجباری‌کردن یک نسخه، تغییر متن پیام یا
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
    /** URL قدیمی/عمومی؛ برای payloadهای قبلی fallback منبع خارجی است. */
    val url: String = "",
    /** APK روی Appwrite برای حالت خارجی. */
    val externalUrl: String = "",
    /** همان APK روی ParsPack برای حالت داخلی ایران. */
    val internalUrl: String = "",
    /** حجمِ تقریبیِ فایل (بایت) برای نمایش؛ `0` = نامعلوم. */
    val size: Long = 0L,
    /** هشِ فایلِ APK؛ خالی = سرور هش نداده (بررسی به نصب‌کنندهٔ سیستم واگذار می‌شود). */
    val sha256: String = "",
    val notes: List<String> = emptyList(),
    val chan: String = "stable",
    /** applicationId مقصد؛ مانع پیشنهاد APK پایهٔ دیگر می‌شود. */
    val packageName: String = "",
    /** شناسهٔ پایهٔ مقصد، مثل grade9. */
    val gradeId: String = "",
    /** SHA-256 گواهی امضای APK (عمومی است، secret نیست). */
    val signingSha256: String = "",
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

    /** ردیف مستقل همان flavor؛ مثال: app_release_grade9. */
    val ROW_ID: String get() = BuildConfig.UPDATE_ROW_ID

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
        externalUrl = str(json, "externalUrl"),
        internalUrl = str(json, "internalUrl"),
        size = num(json, "size"),
        sha256 = str(json, "sha256"),
        notes = arr(json, "notes"),
        chan = str(json, "chan").ifBlank { CHANNEL },
        packageName = str(json, "packageName"),
        gradeId = str(json, "gradeId"),
        signingSha256 = str(json, "signingSha256"),
        rollout = num(json, "rollout").toInt().let { if (it <= 0) 100 else it.coerceAtMost(100) },
    )

    /**
     * payload قدیمی/اشتباه یا مربوط به پایهٔ دیگر هرگز به مرحلهٔ دانلود نمی‌رسد.
     * عمداً fail-closed است: نبود packageName/gradeId نیز ناسازگار محسوب می‌شود.
     */
    fun isCompatible(info: UpdateInfo, packageName: String, gradeId: String): Boolean =
        info.packageName == packageName && info.gradeId == gradeId

    /**
     * تصمیم بر پایهٔ `versionCode` فعلی و «سطلِ» رول‌آوتِ این نصب.
     *
     * ترتیبِ بررسی مهم است: اگر `url` یا `latest` نبود، اصلاً پیامی نشان داده
     * نمی‌شود (پس انتشارِ ردیفِ ناقص بی‌خطر است).
     */
    fun decisionFor(current: Int, info: UpdateInfo, bucket: Int = 0): UpdateDecision {
        if (info.latest <= 0 || (info.url.isBlank() && info.externalUrl.isBlank() && info.internalUrl.isBlank())) {
            return UpdateDecision.None
        }
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

    /**
     * نسخه با «هر سه شماره»ی کامل: `۲.۴.۲`.
     *
     * سرور گاهی نام را ناقص می‌فرستد (`2.4`) یا اصلاً نمی‌فرستد (آن‌وقت فقط
     * versionCode می‌ماند). کارتِ آپدیت نباید شمارهٔ بریده نشان بدهد، پس این‌جا
     * جای خالی با صفر پر می‌شود و اگر نامی نبود، سه شماره از خودِ versionCode
     * بیرون کشیده می‌شود (۲۴۲ ← ۲.۴.۲).
     */
    fun threePart(name: String, code: Int = 0): String {
        val parts = name.trim().removePrefix("v")
            .filter { it.isDigit() || it == '.' }
            .split('.')
            .filter { it.isNotEmpty() }
        val fromCode = if (code >= 100) "${code / 100}.${(code / 10) % 10}.${code % 10}" else ""
        return when {
            parts.size >= 3 -> parts.take(3).joinToString(".")
            parts.size == 2 -> parts.joinToString(".") + ".0"
            parts.size == 1 && parts[0] == code.toString() -> fromCode.ifBlank { parts[0] }
            parts.size == 1 -> parts[0] + ".0.0"
            else -> fromCode.ifBlank { if (code > 0) code.toString() else "" }
        }
    }

    /** همان `versionLabel` اما همیشه با هر سه شماره — ویژهٔ نمایش در کارتِ آپدیت. */
    fun fullVersionLabel(info: UpdateInfo): String = threePart(versionLabel(info), info.latest)

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
