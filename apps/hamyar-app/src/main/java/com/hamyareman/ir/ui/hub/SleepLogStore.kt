package com.hamyareman.ir.ui.hub

import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * هر شب یک رکورد یکتا دارد. زمان‌ها epoch هستند تا از تغییر ساعت/تقویم دستگاه
 * در امان بمانند؛ تاریخ کلیدِ تهران است تا کارت خوابِ شب با منطقهٔ کاربر یکی بماند.
 */
internal data class SleepLogEntry(
    val id: String,
    val dayIso: String,
    val bedtimeAt: Long? = null,
    val wokeAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** ذخیرهٔ local-first خواب و قراردادی کوچک برای ارسال امن در SyncEngine. */
internal object SleepLogStore {
    private const val KEY = "sleep_log_entries_v2"
    private const val MAX_ENTRIES = 180

    fun entries(store: LocalStore): List<SleepLogEntry> = runCatching {
        val array = JSONArray(store.getString(KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                val bed = json.optLong("bedtimeAt", 0L).takeIf { it > 0L }
                val wake = json.optLong("wokeAt", 0L).takeIf { it > 0L }
                val id = json.optString("id")
                val day = json.optString("dayIso")
                if (id.isNotBlank() && day.isNotBlank()) {
                    add(
                        SleepLogEntry(
                            id = id,
                            dayIso = day,
                            bedtimeAt = bed,
                            wokeAt = wake,
                            updatedAt = json.optLong("updatedAt", 0L),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }

    fun latestOpen(store: LocalStore): SleepLogEntry? =
        entries(store).firstOrNull { it.bedtimeAt != null && it.wokeAt == null }

    /** حذف قطعیِ ثبت انتخاب‌شده از دفتر محلی؛ سایر شب‌ها دست‌نخورده می‌مانند. */
    fun delete(store: LocalStore, entryId: String): Boolean {
        val before = entries(store)
        val after = before.filterNot { it.id == entryId }
        if (after.size == before.size) return false
        persist(store, after)
        return true
    }

    fun recordBedtime(store: LocalStore, at: Long = System.currentTimeMillis()): SleepLogEntry {
        val entry = SleepLogEntry(
            id = "sleep_${JalaliDate.todayIso()}_$at",
            dayIso = JalaliDate.todayIso(),
            bedtimeAt = at,
            updatedAt = at,
        )
        persist(store, listOf(entry) + entries(store))
        return entry
    }

    fun recordWakeTime(store: LocalStore, at: Long = System.currentTimeMillis()): SleepLogEntry {
        val open = latestOpen(store)
        val revised = if (open != null) {
            open.copy(wokeAt = at, updatedAt = at)
        } else {
            // دکمهٔ بیداری حتی اگر کاربر شب قبل فرصت ثبت خواب نداشت، timestamp
            // واقعی بیدارشدن را از دست نمی‌دهد.
            SleepLogEntry(
                id = "sleep_${JalaliDate.todayIso()}_$at",
                dayIso = JalaliDate.todayIso(),
                wokeAt = at,
                updatedAt = at,
            )
        }
        persist(store, listOf(revised) + entries(store).filterNot { it.id == revised.id })
        return revised
    }

    fun streak(store: LocalStore, todayIso: String = JalaliDate.todayIso()): Int {
        val recordedDays = entries(store)
            .filter { it.bedtimeAt != null || it.wokeAt != null }
            .mapTo(mutableSetOf()) { it.dayIso }
        var day = runCatching { LocalDate.parse(todayIso) }.getOrElse { return 0 }
        // امروز اگر هنوز چیزی ندارد، رشته را از دیشب می‌سنجیم.
        if (day.toString() !in recordedDays) day = day.minusDays(1)
        var total = 0
        while (day.toString() in recordedDays && total < MAX_ENTRIES) {
            total++
            day = day.minusDays(1)
        }
        return total
    }

    /**
     * سرورِ فعلی جدول عمومی wellness_logs دارد. event خواب با همان قرارداد پایدار
     * (moveSlug/category/dayIso/secondsSpent/completed) همگام می‌شود و rowId
     * شامل timestamp بستر خواب است؛ بنابراین upsertِ ثبت بیداری، همان شب را به‌روز
     * می‌کند و سطر تکراری نمی‌سازد.
     */
    fun enqueueSync(sync: SyncEngine, identityStore: LocalStore, entry: SleepLogEntry) {
        val userId = identityStore.getString(AppwriteAuthService.KEY_USER_ID).ifBlank { "anon" }
        val sleepSeconds = if (entry.bedtimeAt != null && entry.wokeAt != null) {
            ((entry.wokeAt - entry.bedtimeAt) / 1_000L).coerceIn(0L, 86_400L).toInt()
        } else {
            0
        }
        sync.enqueue(
            table = TableIds.WELLNESS_LOGS,
            rowId = "sl_${userId}_${entry.id}",
            payload = mapOf(
                "userId" to userId,
                "moveSlug" to entry.id,
                "category" to "sleep",
                "dayIso" to entry.dayIso,
                "secondsSpent" to sleepSeconds,
                "completed" to (entry.wokeAt != null),
            ),
        )
    }

    private fun persist(store: LocalStore, source: List<SleepLogEntry>) {
        val array = JSONArray()
        source.sortedByDescending { it.updatedAt }.take(MAX_ENTRIES).forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("dayIso", entry.dayIso)
                    .put("bedtimeAt", entry.bedtimeAt ?: 0L)
                    .put("wokeAt", entry.wokeAt ?: 0L)
                    .put("updatedAt", entry.updatedAt),
            )
        }
        store.putString(KEY, array.toString())
    }
}
