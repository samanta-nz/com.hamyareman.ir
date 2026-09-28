package com.hamyareman.ir.platform.core.appwrite

/**
 * ردیف ساده‌ی یک جدول TablesDB — مستقل از مدل‌های SDK.
 *
 * چرا کلاس خودمان:
 *  - مصرف‌کننده‌ها (ریپازیتوری‌ها) نباید به مدل‌های `io.appwrite` گره بخورند تا
 *    در تست‌های JVM بدون Robolectric قابل ساخت باشند.
 *  - Realtime در SDK مقدار payload را `Any?` می‌دهد؛ این‌جا یک‌بار به نقشه‌ی
 *    امن تبدیل و همیشه همین کلاس بین لایه‌ها جابه‌جا می‌شود.
 *
 * خواننده‌ها فقط این دو تایپ را برمی‌گردانند (همان چیزی که کاتالوگ/تماس/قرین‌سازی
 * لازم دارند): [string] و [long]. اگر خواندنی غایب یا از نوع دیگر باشد،
 * مقدار پیش‌فرض برمی‌گردد — نه exception (داده‌ی ناقص هرگز اپ را نباید بیندازد).
 */
data class TableRow(
    val id: String,
    val payload: Map<String, Any?> = emptyMap(),
) {
    /** نام مستعار هم‌راستا با مدل SDK — بعضی مصرف‌کننده‌ها `row.data[...]` می‌خوانند. */
    val data: Map<String, Any?> get() = payload

    /** مقدار رشته‌ای ستون؛ خالی/غایب → [default]. */
    fun string(key: String, default: String = ""): String =
        (payload[key] as? String)?.takeIf { it.isNotBlank() } ?: default

    /** مقدار بولی ستون؛ Boolean واقعی، «true/false» رشته‌ای، یا 0/1 عددی؛ غایب → [default]. */
    fun boolean(key: String, default: Boolean = false): Boolean = when (val v = payload[key]) {
        is Boolean -> v
        is String -> when (v.lowercase().trim()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> default
        }
        is Number -> v.toInt() != 0
        else -> default
    }

    /** مقدار عددی ستون؛ عدد یا رشته‌ی عددی؛ غایب/نامعتبر → [default]. */
    fun long(key: String, default: Long = 0L): Long = when (val v = payload[key]) {
        is Number -> v.toLong()
        is String -> v.toDoubleOrNull()?.toLong() ?: default
        else -> default
    }
}
