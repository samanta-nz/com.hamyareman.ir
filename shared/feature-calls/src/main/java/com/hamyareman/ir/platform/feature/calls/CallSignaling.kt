package com.hamyareman.ir.platform.feature.calls

import io.appwrite.ID
import io.appwrite.Query
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * سیگنالینگ تماس روی TablesDB.
 *
 * سیگنال‌ها از دو مسیر می‌رسند:
 *  1) **Realtime** روی کانال `tablesdb.main_db.tables.call_signals.rows` (قالب کانال از
 *     خودِ SDK Appwrite گرفته شده) — تقریباً بی‌درنگ؛
 *  2) **polling** به‌عنوان تور ایمنی، چون WebSocket ممکن است در شبکه‌ی موبایل قطع شود
 *     و تماس نباید به‌خاطر آن ساکت بماند.
 *
 * وقتی Realtime در دسترس نباشد (بک‌اند تنظیم نشده)، [liveSignals] جریان خالی می‌دهد و
 * فقط همان polling کار می‌کند؛ یعنی هیچ رفتاری نسبت به قبل بدتر نمی‌شود.
 */
class CallSignaling(
    private val tables: TablesDbService,
    private val provider: AppwriteClientProvider? = null,
    private val realtime: AppwriteRealtimeFeed? = null,
) {
    val isConfigured: Boolean get() = tables.isConfigured

    /** آیا مسیر زنده فعال است؟ موتور تماس با این، فاصله‌ی polling را کمتر می‌کند. */
    val hasRealtime: Boolean get() = realtime?.isConfigured == true

    /**
     * سیگنال‌های زنده‌ی یک تماس. فیلترها سمت کلاینت اعمال می‌شوند چون کانال
     * در سطح «همه‌ی ردیف‌های جدول» است (Realtime در Appwrite کوئری روی کانال
     * TablesDB را در این نسخه پشتیبانی نمی‌کند).
     */
    fun liveSignals(callId: String, afterCreatedAtMs: Long = 0L): Flow<CallSignal> =
        (realtime?.callSignals() ?: emptyFlow())
            .map { event -> event.row.toSignal() }
            .filter { it.callId == callId && it.createdAtMs > afterCreatedAtMs }

    /** ساخت سطر جلسه‌ی تماس در `call_sessions` (برای تاریخچه‌ی اپ پدر). */
    suspend fun openSession(
        callId: String,
        kind: CallKind,
        fromUserId: String,
        toUserId: String,
        fromLabel: String,
    ): AppResult<Unit> {
        if (!isConfigured) return AppResult.Err(AppError.Local("بدون Appwrite تماس برقرار نمی‌شود."))
        val data = mapOf(
            "callId" to callId,
            "kind" to kind.name,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "fromLabel" to fromLabel,
            "status" to "ringing",
            "startedAt" to System.currentTimeMillis(),
        )
        val permissions = provider?.let {
            AppwriteClientProvider.sharedWith(fromUserId, toUserId)
        } ?: emptyList()
        return when (val result = tables.upsert(TableIds.CALL_SESSIONS, callId, data, permissions)) {
            is AppResult.Ok -> AppResult.Ok(Unit)
            is AppResult.Err -> result
        }
    }

    suspend fun send(signal: CallSignal): AppResult<Unit> {
        if (!isConfigured) return AppResult.Err(AppError.Local("بدون Appwrite تماس برقرار نمی‌شود."))
        val data = mapOf(
            "callId" to signal.callId,
            "fromUserId" to signal.fromUserId,
            "toUserId" to signal.toUserId,
            "type" to signal.type.name,
            "payload" to signal.payload,
            "createdAtMs" to signal.createdAtMs,
        )
        val permissions = provider?.let {
            AppwriteClientProvider.sharedWith(signal.fromUserId, signal.toUserId)
        } ?: emptyList()
        return when (val result = tables.create(TableIds.CALL_SIGNALS, data, permissions, ID.unique())) {
            is AppResult.Ok -> AppResult.Ok(Unit)
            is AppResult.Err -> result
        }
    }

    /**
     * خواندن سیگنال‌های جدیدِ مقصدِ من برای این تماس.
     * @param afterCreatedAtMs فقط ردیف‌های جدیدتر از این زمان برگردانده می‌شوند.
     */
    suspend fun poll(callId: String, afterCreatedAtMs: Long): AppResult<List<CallSignal>> {
        if (!isConfigured) return AppResult.Ok(emptyList())
        val queries = listOf(
            Query.equal("callId", callId),
            Query.greaterThan("createdAtMs", afterCreatedAtMs),
            Query.orderAsc("createdAtMs"),
            Query.limit(50),
        )
        return when (val result = tables.list(TableIds.CALL_SIGNALS, queries)) {
            is AppResult.Ok -> AppResult.Ok(result.value.map { it.toSignal() })
            is AppResult.Err -> result
        }
    }

    /** تماس‌های ورودیِ زنگ‌خورده (برای اعلان/صفحه‌ی پدر). */
    suspend fun incomingRinging(toUserId: String): AppResult<List<TableRow>> {
        if (!isConfigured) return AppResult.Ok(emptyList())
        val queries = listOf(
            Query.equal("toUserId", toUserId),
            Query.equal("status", "ringing"),
            Query.orderDesc("\$createdAt"),
            Query.limit(5),
        )
        return tables.list(TableIds.CALL_SESSIONS, queries)
    }

    suspend fun closeSession(callId: String, status: String) {
        if (!isConfigured) return
        runCatching {
            tables.update(
                TableIds.CALL_SESSIONS,
                callId,
                mapOf("status" to status, "endedAt" to System.currentTimeMillis()),
            )
        }
    }

    private fun TableRow.toSignal(): CallSignal = CallSignal(
        id = id,
        callId = string("callId"),
        fromUserId = string("fromUserId"),
        toUserId = string("toUserId"),
        type = runCatching { SignalType.valueOf(string("type")) }.getOrDefault(SignalType.ICE),
        payload = string("payload"),
        createdAtMs = long("createdAtMs"),
    )
}
