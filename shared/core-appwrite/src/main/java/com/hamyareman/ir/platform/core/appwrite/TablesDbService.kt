package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.models.Row
import io.appwrite.models.RowList
import io.appwrite.services.TablesDB
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult

/**
 * لایه‌ی دسترسی به جداول TablesDB — مستقل از SDK برای تست‌پذیری.
 *
 * چرا اینترفیس: ریپازیتوری‌ها فقط همین قرارداد را می‌شناسند تا در تست‌های JVM
 * بدون Robolectric با فیک قابل استفاده باشند.
 *
 * قرارداد خطا: هیچ متدی exception نمی‌اندازد؛ همه‌چیز [AppResult] است تا اپ با
 * قطعیِ سرور نلرزد (آفلاین‌محور بودن پروژه).
 *
 * توجه: `TableIds`/`BucketIds`/`FunctionIds`/`PrivacyPolicy` در ماژول
 * `core-common` زندگی می‌کنند (نسخه‌ی کپی‌شده‌ی قدیمی در این فایل حذف شد —
 * دو تعریف هم‌نام روی classpath خطرناک است).
 */
interface TablesDbService {

    /** آیا بک‌اند پیکربندی شده و آماده‌ی درخواست است؟ */
    val isConfigured: Boolean

    /** خواندن سطرهای یک جدول با کوئری‌های رشته‌ای Appwrite (مثل `equal("x",["1"])`). */
    suspend fun list(tableId: String, queries: List<String> = emptyList()): AppResult<List<TableRow>>

    /** خواندن یک سطر با شناسه‌ی قطعی؛ نبودِ سطر → Err (در فراخوان فولد می‌شود). */
    suspend fun get(tableId: String, rowId: String): AppResult<TableRow?>

    /** ساخت سطر با شناسه‌ی مشخص (مثلاً `ID.unique()` از سمت مصرف‌کننده). */
    suspend fun create(
        tableId: String,
        data: Map<String, Any?>,
        permissions: List<String>,
        rowId: String,
    ): AppResult<Unit>

    /** به‌روزرسانی سطر موجود. */
    suspend fun update(tableId: String, rowId: String, data: Map<String, Any?>): AppResult<Unit>

    /** ساخت یا بازنویسی سطر با rowId قطعی — ستون فقرات سینک outbox. */
    suspend fun upsert(
        tableId: String,
        rowId: String,
        data: Map<String, Any?>,
        permissions: List<String> = emptyList(),
    ): AppResult<Unit>

    /** حذف سطر. */
    suspend fun delete(tableId: String, rowId: String): AppResult<Unit>
}

/**
 * پیاده‌سازی واقعی روی Appwrite TablesDB (SDK اندروید).
 */
class AppwriteTablesDbService(
    private val provider: AppwriteClientProvider,
) : TablesDbService {

    private val db: TablesDB? by lazy {
        if (provider.isConfigured) {
            runCatching { TablesDB(provider.client) }.getOrNull()
        } else {
            null
        }
    }

    override val isConfigured: Boolean get() = db != null

    override suspend fun list(tableId: String, queries: List<String>): AppResult<List<TableRow>> = guarded {
        val res: RowList<Map<String, Any>> = db!!.listRows(
            databaseId = provider.databaseId,
            tableId = tableId,
            queries = queries,
        )
        res.rows.map { row -> row.toTableRow() }
    }

    override suspend fun get(tableId: String, rowId: String): AppResult<TableRow?> = guarded {
        db!!.getRow(
            databaseId = provider.databaseId,
            tableId = tableId,
            rowId = rowId,
        ).toTableRow()
    }

    override suspend fun create(
        tableId: String,
        data: Map<String, Any?>,
        permissions: List<String>,
        rowId: String,
    ): AppResult<Unit> = guarded {
        val row: Row<Map<String, Any>> = db!!.createRow(
            databaseId = provider.databaseId,
            tableId = tableId,
            rowId = rowId,
            data = data,
            permissions = permissions,
        )
        Unit
    }

    override suspend fun update(tableId: String, rowId: String, data: Map<String, Any?>): AppResult<Unit> = guarded {
        db!!.updateRow(
            databaseId = provider.databaseId,
            tableId = tableId,
            rowId = rowId,
            data = data,
        )
        Unit
    }

    override suspend fun upsert(
        tableId: String,
        rowId: String,
        data: Map<String, Any?>,
        permissions: List<String>,
    ): AppResult<Unit> = guarded {
        val row: Row<Map<String, Any>> = db!!.upsertRow(
            databaseId = provider.databaseId,
            tableId = tableId,
            rowId = rowId,
            data = data,
            permissions = permissions,
        )
        Unit
    }

    override suspend fun delete(tableId: String, rowId: String): AppResult<Unit> = guarded {
        db!!.deleteRow(
            databaseId = provider.databaseId,
            tableId = tableId,
            rowId = rowId,
        )
        Unit
    }

    private fun Row<Map<String, Any>>.toTableRow(): TableRow =
        TableRow(id = id, payload = data)

    /** هر خطای سرور/شبکه به Err با پیام آدمیزاد تبدیل می‌شود — اپ هرگز crash نمی‌کند. */
    private inline fun <T> guarded(block: () -> T): AppResult<T> {
        if (db == null) return AppResult.Err(AppError.Local("بک‌اند هنوز تنظیم نشده؛ داده‌ها محلی می‌مانند."))
        return try {
            AppResult.Ok(block())
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            AppResult.Err(AppError.Unknown("سرور درخواست را نپذیرفت (${e.code ?: 0}). بعداً دوباره امتحان می‌کنیم."))
        } catch (e: Exception) {
            AppResult.Err(AppError.Network())
        }
    }
}
