package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * سینک دوسویه‌ی آمار بخش «مدرسه» (v1.14):
 *
 *  ارسال (همیشه فعال): هر نوشتنِ TeachStats / فلش‌کارت / آزمون همان لحظه در صف
 *  outbox ([SyncEngine]) می‌رود و خودکار به سرور upsert می‌شود.
 *
 *  بازیابی (جدید): با ورود به اپ، همه‌ی سطرهای «آمار تدریس» و «پیشرفت مطالعه»ی
 *  همین کاربر از سرور خوانده و با داده‌ی محلی **ادغام** می‌شود — پس با تعویض
 *  گوشی یا حذف/نصب دوباره‌ی اپ هیچ‌چیز از دست نمی‌رود.
 *
 *  ادغام برای آمارِ غیرقابل‌ویرایشِ تجمعی امن است:
 *   - شمارنده‌ها → بیشینه (نشست‌ها، ثانیه‌ی شنیدن، پرش‌ها، …)؛
 *   - رسانه‌های تمام‌شده (dt) و تاریخ نشست‌ها (sl) → اجتماع؛
 *   - پرچم اتمام دوره (d) → OR؛ تاریخ‌های شروع/اتمام → کهنه‌ترین/ نوترین؛
 *   - فلش‌کارت‌ها → وضعیت با مرورِ بیشتر؛ آزمون‌ها → اجتماع بدون تکرار.
 */
object SchoolSync {

    private const val TEACH_TABLE = TeachCloud.TABLE          // teach_stats
    private const val STUDY_TABLE = com.hamyareman.ir.platform.core.common.TableIds.STUDY_PROGRESS

    /** در هر اجرای پروسه فقط یک‌بار بازیابی کامل انجام شود. */
    @Volatile var restoredThisSession: Boolean = false

    suspend fun restoreAll(
        context: Context,
        tables: TablesDbService,
        sync: SyncEngine?,
        userId: String?,
        /** با true حتی اگر در این نشست قبلاً بازیابی شده باشد دوباره از سرور می‌خواند. */
        force: Boolean = false,
    ): Int =
        withContext(Dispatchers.IO) {
            if (restoredThisSession && !force) return@withContext 0
            if (!tables.isConfigured) return@withContext 0
            if (userId.isNullOrBlank()) return@withContext 0
            var merged = 0

            // ---------- آمار تدریس ----------
            when (val r = tables.list(TEACH_TABLE)) {
                is com.hamyareman.ir.platform.core.common.AppResult.Ok -> {
                    r.value.forEach { row ->
                        val packId = row.payload["packId"] as? String ?: return@forEach
                        val stats = row.payload["stats"] as? String ?: return@forEach
                        val remote = runCatching { JSONObject(stats) }.getOrNull() ?: return@forEach
                        if (mergeTeachIntoLocal(context, packId, remote)) merged++
                    }
                }
                else -> Unit
            }

            // ---------- پیشرفت مطالعه (فلش‌کارت + آزمون) ----------
            when (val r = tables.list(STUDY_TABLE)) {
                is com.hamyareman.ir.platform.core.common.AppResult.Ok -> {
                    r.value.forEach { row ->
                        val rowUserId = row.payload["userId"] as? String
                        if (!rowUserId.isNullOrBlank() && rowUserId != userId) return@forEach
                        val packId = row.payload["packId"] as? String ?: return@forEach
                        val srs = row.payload["srsState"] as? String ?: ""
                        val attempts = row.payload["attempts"] as? String ?: ""
                        val examLedger = row.payload["examLedger"] as? String ?: ""
                        val extras = row.payload["extras"] as? String ?: ""
                        if (mergeStudyIntoLocal(context, packId, srs, attempts, examLedger, extras)) merged++
                    }
                }
                else -> Unit
            }

            restoredThisSession = true
            // هر چیزی که محلیِ تازه‌تری بود، به‌سمت سرور هم برود.
            runCatching { sync?.pushAll() }
            TeachCloud.markSynced(context)
            merged
        }

    // ------------------------------------------------------------ تدریس

    private fun teachStore(context: Context) = LocalStore(context, "hamyar_teach_stats")

    /** ادغام آمار از راه دور داخل محلی — true یعنی چیزی عوض شد. */
    private fun mergeTeachIntoLocal(context: Context, packId: String, remote: JSONObject): Boolean {
        val store = teachStore(context)
        val key = "ts_$packId"
        val local = runCatching { JSONObject(store.getString(key, "{}")) }.getOrDefault(JSONObject())
        if (local.length() == 0) {
            if (remote.length() == 0) return false
            store.putString(key, remote.toString())
            return true
        }
        val before = local.toString()
        // شمارنده‌ها → بیشینه
        listOf("s", "ls", "vs", "j", "ad", "vd", "exp", "sc").forEach { k ->
            val a = local.optInt(k)
            val b = remote.optInt(k)
            if (b > a) local.put(k, b)
        }
        // شروع = کهنه‌ترین؛ اتمام = نوترین؛ اتمام دوره = OR
        val stL = local.optLong("st"); val stR = remote.optLong("st")
        if (stR in 1 until (if (stL == 0L) Long.MAX_VALUE else stL)) local.put("st", stR)
        if (remote.optLong("ca") > local.optLong("ca")) local.put("ca", remote.optLong("ca"))
        if (remote.optBoolean("d") && !local.optBoolean("d")) local.put("d", true)
        // اجتماع رسانه‌های تمام‌شده و تاریخ نشست‌ها
        unionArrays(local, remote, "dt")
        unionArrays(local, remote, "sl", cap = 60)
        val after = local.toString()
        if (after != before) store.putString(key, after)
        return after != before
    }

    private fun unionArrays(target: JSONObject, source: JSONObject, field: String, cap: Int = Int.MAX_VALUE) {
        val a = target.optJSONArray(field) ?: JSONArray()
        val seen = buildSet { for (i in 0 until a.length()) add(a.opt(i)) }
        val b = source.optJSONArray(field) ?: return
        for (i in 0 until b.length()) {
            val v = b.opt(i)
            if (v !in seen) a.put(v)
        }
        while (a.length() > cap) a.remove(0)
        target.put(field, a)
    }

    // ------------------------------------------------ مطالعه (فلش‌کارت/آزمون)

    /** ادغام وضعیت SRS و آزمون‌ها — true یعنی چیزی عوض شد. */
    private fun mergeStudyIntoLocal(
        context: Context,
        packId: String,
        srsRemote: String,
        attemptsRemote: String,
        examRemote: String = "",
        extrasRemote: String = "",
    ): Boolean {
        val store = LocalStore(context) // همان store پیش‌فرضِ StudyProgressRepository؛ کلیدها با پیشوند study:
        var changed = false

        if (srsRemote.isNotBlank()) {
            val remoteCards = runCatching { JSONObject(srsRemote) }.getOrNull()
            if (remoteCards != null && remoteCards.length() > 0) {
                val key = "study:$packId:cards"
                val local = runCatching { JSONObject(store.getString(key, "{}")) }.getOrDefault(JSONObject())
                val merged = JSONObject()
                val ids = mutableSetOf<String>()
                local.keys().forEach { ids.add(it) }
                remoteCards.keys().forEach { ids.add(it) }
                ids.forEach { id ->
                    val l = local.optJSONObject(id)
                    val r = remoteCards.optJSONObject(id)
                    val winner = when {
                        l == null -> r
                        r == null -> l
                        else -> {
                            val lr = l.optInt("reps"); val rr = r.optInt("reps")
                            if (rr > lr) r
                            else if (rr == lr && r.optInt("intervalDays") > l.optInt("intervalDays")) r
                            else l
                        }
                    }
                    merged.put(id, winner)
                }
                if (merged.toString() != local.toString()) {
                    store.putString(key, merged.toString())
                    changed = true
                }
            }
        }

        if (attemptsRemote.isNotBlank()) {
            val remoteArr = runCatching { JSONArray(attemptsRemote) }.getOrNull()
            if (remoteArr != null && remoteArr.length() > 0) {
                val key = "study:$packId:attempts"
                val localArr = runCatching { JSONArray(store.getString(key, "[]")) }.getOrDefault(JSONArray())
                val seen = mutableSetOf<String>()
                val all = JSONArray()
                fun add(o: JSONObject) {
                    val sig = "${o.optString("dateKey")}|${o.optInt("scorePct")}|${o.optInt("total")}|${o.optLong("atMs")}"
                    if (sig in seen) return
                    seen.add(sig)
                    all.put(o)
                }
                for (i in 0 until localArr.length()) add(localArr.optJSONObject(i) ?: continue)
                for (i in 0 until remoteArr.length()) add(remoteArr.optJSONObject(i) ?: continue)
                // تازه‌ها آخر
                val sorted = JSONArray()
                all.let { arr ->
                    val list = (0 until arr.length()).map { arr.optJSONObject(it)!! }
                        .sortedWith(compareBy({ it.optString("dateKey") }, { it.optLong("atMs") }))
                    list.forEach { sorted.put(it) }
                }
                if (sorted.length() != localArr.length()) {
                    val trimmed = JSONArray()
                    for (i in (sorted.length() - 50).coerceAtLeast(0) until sorted.length()) trimmed.put(sorted.optJSONObject(i))
                    store.putString(key, trimmed.toString())
                    changed = true
                }
            }
        }

        if (examRemote.isNotBlank()) {
            val remote = com.hamyareman.ir.platform.feature.study.MathExamLedger.State.fromJson(examRemote)
            val local = com.hamyareman.ir.platform.feature.study.MathExamLedger.State.fromJson(store.getString("study:$packId:exam"))
            val mergedExam = com.hamyareman.ir.platform.feature.study.MathExamLedger.merge(local, remote)
            if (mergedExam.toJson() != local.toJson()) {
                store.putString("study:$packId:exam", mergedExam.toJson())
                changed = true
            }
        }
        if (extrasRemote.isNotBlank()) {
            val o = runCatching { JSONObject(extrasRemote) }.getOrNull()
            if (o != null) {
                listOf("archive" to "study:$packId:archive", "sameday" to "study:$packId:sameday", "ex" to "study:$packId:ex").forEach { (field, key) ->
                    val v = o.optString(field)
                    if (v.isNotBlank() && store.getString(key).isBlank()) {
                        store.putString(key, v)
                        changed = true
                    }
                }
            }
        }
        return changed
    }

    suspend fun watchLive(
        context: Context,
        realtime: com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed,
        userId: String?,
    ) {
        if (!realtime.isConfigured || userId.isNullOrBlank()) return
        kotlinx.coroutines.coroutineScope {
            launch {
                realtime.rows(TEACH_TABLE).collect { ev ->
                    val packId = ev.row.payload["packId"] as? String ?: return@collect
                    val stats = ev.row.payload["stats"] as? String ?: return@collect
                    val remote = runCatching { JSONObject(stats) }.getOrNull() ?: return@collect
                    mergeTeachIntoLocal(context, packId, remote)
                }
            }
            launch {
                realtime.rows(STUDY_TABLE).collect { ev ->
                    val rowUserId = ev.row.payload["userId"] as? String
                    if (!rowUserId.isNullOrBlank() && rowUserId != userId) return@collect
                    val packId = ev.row.payload["packId"] as? String ?: return@collect
                    mergeStudyIntoLocal(
                        context,
                        packId,
                        ev.row.payload["srsState"] as? String ?: "",
                        ev.row.payload["attempts"] as? String ?: "",
                        ev.row.payload["examLedger"] as? String ?: "",
                        ev.row.payload["extras"] as? String ?: "",
                    )
                }
            }
        }
    }
}
