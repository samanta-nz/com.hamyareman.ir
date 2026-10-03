package com.hamyareman.ir.platform.core.sync

import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.PrivacyPolicy
import org.json.JSONArray
import org.json.JSONObject

/** یک آیتم در صف ارسال. */
data class OutboxItem(
    val id: String,
    val table: String,
    val rowId: String,
    val data: JSONObject,
    val attempts: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("table", table)
        .put("rowId", rowId)
        .put("data", data)
        .put("attempts", attempts)
        .put("createdAt", createdAt)

    fun dataMap(): Map<String, Any?> = buildMap {
        data.keys().forEach { key -> put(key, data.opt(key)) }
    }

    companion object {
        fun fromJson(o: JSONObject): OutboxItem = OutboxItem(
            id = o.optString("id"),
            table = o.optString("table"),
            rowId = o.optString("rowId"),
            data = o.optJSONObject("data") ?: JSONObject(),
            attempts = o.optInt("attempts", 0),
            createdAt = o.optLong("createdAt", 0L),
        )
    }
}

data class SyncReport(
    val pushed: Int = 0,
    val failed: Int = 0,
    val skippedPrivate: Int = 0,
    val remaining: Int = 0,
) {
    val isClean: Boolean get() = remaining == 0
}

/**
 * صف ارسال (Outbox) با ذخیره‌ی محلی.
 *
 * قانون سخت پروژه: جدول‌های [PrivacyPolicy.neverSyncTables] (چرخه، حال، دفترچه،
 * زمان صفحه، تاریخچه‌ی چت) هرگز وارد صف نمی‌شوند — حتی اگر کسی از UI صدایشان بزند.
 * این همان چیزی است که در EthicalNotice اپ پدر به کاربر وعده داده شده.
 */
class SyncEngine(
    private val store: LocalStore,
    private val tables: TablesDbService,
) {

    fun neverSyncTables(): Set<String> = PrivacyPolicy.neverSyncTables

    fun pendingCount(): Int = items().size

    fun pending(): List<OutboxItem> = items()

    fun lastSyncAt(): Long = store.getLong(KEY_LAST_SYNC, 0L)

    /**
     * افزودن به صف.
     * @return true یعنی در صف رفت، false یعنی به دلایل حریم خصوصی رد شد.
     */
    fun enqueue(table: String, rowId: String, payload: Map<String, Any?>): Boolean {
        if (PrivacyPolicy.isNeverSynced(table)) return false
        if (rowId.isBlank()) return false

        val data = JSONObject()
        payload.forEach { (key, value) -> if (value != null) data.put(key, value) }

        val existing = items().toMutableList()
        // هم‌جدول و هم‌rowId یعنی به‌روزرسانی همان سطر → فقط آخرین نسخه می‌ماند.
        existing.removeAll { it.table == table && it.rowId == rowId }
        existing += OutboxItem(
            id = "${table}_${rowId}",
            table = table,
            rowId = rowId,
            data = data,
        )
        persist(existing)
        return true
    }

    fun clear() {
        persist(emptyList())
    }

    /**
     * تلاش برای ارسال کل صف. هر سطر با upsert می‌رود تا rowId دستگاه حفظ شود و
     * ارسال تکراری سطر نسازد.
     */
    suspend fun pushAll(batchSize: Int = MAX_BATCH): SyncReport {
        val queue = items()
        if (queue.isEmpty()) return SyncReport(remaining = 0)
        if (!tables.isConfigured) {
            return SyncReport(remaining = queue.size)
        }

        var pushed = 0
        var failed = 0
        var skipped = 0
        val survivors = mutableListOf<OutboxItem>()

        queue.take(batchSize).forEach { item ->
            if (PrivacyPolicy.isNeverSynced(item.table)) {
                skipped++
                return@forEach // از صف هم حذف می‌شود؛ هرگز نباید بیرون برود
            }
            val uid = store.getString(AppwriteAuthService.KEY_USER_ID).orEmpty()
            val permissions = if (uid.isBlank()) emptyList() else AppwriteClientProvider.ownerOnly(uid)
            when (val result = tables.upsert(item.table, item.rowId, item.dataMap(), permissions)) {
                is AppResult.Ok -> pushed++
                is AppResult.Err -> {
                    failed++
                    // خطا باعث حذف داده نمی‌شود؛ آخرین snapshot تا اتصال بعدی در صف
                    // می‌ماند و retry توسط WorkManager/شبکه انجام می‌شود.
                    survivors += item.copy(attempts = item.attempts + 1)
                }
            }
        }

        survivors.addAll(queue.drop(batchSize.coerceAtMost(queue.size)))
        persist(survivors)
        store.putLong(KEY_LAST_SYNC, System.currentTimeMillis())
        return SyncReport(pushed, failed, skipped, survivors.size)
    }

    private fun items(): List<OutboxItem> = runCatching {
        val array = JSONArray(store.getString(KEY_OUTBOX, "[]"))
        buildList {
            for (i in 0 until array.length()) add(OutboxItem.fromJson(array.getJSONObject(i)))
        }
    }.getOrDefault(emptyList())

    private fun persist(list: List<OutboxItem>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        store.putString(KEY_OUTBOX, array.toString())
    }

    companion object {
        private const val KEY_OUTBOX = "sync_outbox"
        private const val KEY_LAST_SYNC = "sync_last_at"
        private const val MAX_BATCH = 25
    }
}
