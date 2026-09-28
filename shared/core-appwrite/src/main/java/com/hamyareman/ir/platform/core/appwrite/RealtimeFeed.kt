package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Realtime
import com.hamyareman.ir.platform.core.common.TableIds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow

/** نوع رویداد — از رشته‌ی `events` در پیام Realtime استخراج می‌شود. */
enum class RowEventKind { CREATE, UPDATE, DELETE, UNKNOWN }

/**
 * یک رویداد Realtime روی یک سطر.
 *
 * [row] همان payload است (معمولاً کل سطر)، پس برای «چه چیزی عوض شد» نیازی به
 * خواندن دوباره از سرور نیست؛ ولی ما در اپ ترجیح می‌دهیم بعد از رویداد یک
 * `refresh()` بزنیم تا قوانین دسترسی/مرتب‌سازی سرور اعمال شود.
 */
data class RealtimeRowEvent(
    val events: List<String>,
    val channels: List<String>,
    val timestamp: String,
    val row: TableRow,
) {
    val kind: RowEventKind
        get() = when {
            events.any { it.endsWith(".create") } -> RowEventKind.CREATE
            events.any { it.endsWith(".update") || it.endsWith(".upsert") } -> RowEventKind.UPDATE
            events.any { it.endsWith(".delete") } -> RowEventKind.DELETE
            else -> RowEventKind.UNKNOWN
        }
}

/**
 * لایه‌ی Realtime (WebSocket) روی TablesDB.
 *
 * **قالب کانال** از خودِ SDK Appwrite گرفته شده (کلاس `Channel`):
 * `tablesdb.<databaseId>.tables.<tableId>.rows` و برای یک سطر خاص
 * `tablesdb.<db>.tables.<table>.rows.<rowId>`؛ پسوند عملیات هم اختیاری است
 * (`.create`/`.update`/`.delete`). قالب قدیمیِ `databases.<db>.collections.<col>.documents`
 * فقط برای Collections معتبر است و این پروژه از TablesDB استفاده می‌کند.
 *
 * طراحی «بدون شکست»: اگر بک‌اند تنظیم نشده باشد یا socket خطا بدهد، این Flow
 * هیچ چیز منتشر نمی‌کند و اپ روی همان مسیر قبلی (خواندن دوره‌ای/دستی) می‌ماند.
 * یعنی Realtime یک **بهینه‌سازی** است، نه یک وابستگی جدید.
 */
class AppwriteRealtimeFeed(private val provider: AppwriteClientProvider) {

    val isConfigured: Boolean get() = provider.isConfigured

    private val errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** پیام‌های خطای اتصال (برای نمایش صادقانه در UI، نه برای crash). */
    val errors: Flow<String> = errorMessages.asSharedFlow()

    /** یک نمونه‌ی Realtime برای کل اپ. خطای socket در SDK فعلی callback عمومی ندارد. */
    private val realtime: Realtime? by lazy {
        if (!provider.isConfigured) {
            null
        } else {
            runCatching {
                Realtime(provider.client)
            }.getOrNull()
        }
    }

    /** ساخت رشته‌ی کانال برای یک جدول. */
    fun channel(tableId: String, databaseId: String = provider.databaseId): String =
        "tablesdb.$databaseId.tables.$tableId.rows"

    /**
     * جریان رویدادهای یک جدول. تا وقتی collect می‌شود اتصال باز می‌ماند و با
     * پایان‌یافتن scope، `unsubscribe` صدا زده می‌شود (و اگر هیچ کانال دیگری
     * باز نمانده باشد، خود SDK سوکت را می‌بندد).
     */
    @Suppress("UNCHECKED_CAST")
    fun rows(tableId: String, databaseId: String = provider.databaseId): Flow<RealtimeRowEvent> =
        callbackFlow {
            val rt = realtime
            val channel = channel(tableId, databaseId)
            if (rt != null) {
                rt.subscribe(channel) { event ->
                    val payload = (event.payload as? Map<String, Any?>) ?: emptyMap()
                    val id = payload["\$id"] as? String ?: ""
                    trySend(
                        RealtimeRowEvent(
                            events = event.events.toList(),
                            channels = event.channels.toList(),
                            timestamp = event.timestamp,
                            row = TableRow(id, payload),
                        ),
                    )
                }
            }
            awaitClose { }
        }

    /** پیام‌های «حرف دل». */
    fun messages(databaseId: String = provider.databaseId): Flow<RealtimeRowEvent> =
        rows(TableIds.FATHER_MESSAGES, databaseId)

    /** سیگنال‌های تماس (SDP/ICE). */
    fun callSignals(databaseId: String = provider.databaseId): Flow<RealtimeRowEvent> =
        rows(TableIds.CALL_SIGNALS, databaseId)

    /** آلبوم خاطرات مشترک. */
    fun albumItems(databaseId: String = provider.databaseId): Flow<RealtimeRowEvent> =
        rows(TableIds.ALBUM_ITEMS, databaseId)

    /** جلسات تماس — برای «زنگ خوردن» بدون polling. */
    fun callSessions(databaseId: String = provider.databaseId): Flow<RealtimeRowEvent> =
        rows(TableIds.CALL_SESSIONS, databaseId)

    private fun faMessage(error: AppwriteException): String = when (error.code) {
        401, 403 -> "برای به‌روزرسانی زنده باید وارد حساب شده باشی؛ تا آن موقع اپ دستی به‌روز می‌شود."
        404 -> "کانال به‌روزرسانی زنده پیدا نشد (جدول ساخته نشده؟)."
        else -> "اتصال زنده برقرار نشد؛ اپ به‌روزآوری دوره‌ای را ادامه می‌دهد."
    }
}
