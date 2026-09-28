package com.hamyareman.ir.ui.wellness

import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.appwrite.BackendConfig
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * پرامپت ۰۲ — ریپازیتوری خواندن/نوشتن حرکات سلامتی.
 *
 * سه لایه:
 *  1) **کش محلی** (LocalStore با کلید `wellness_moves_v1`).
 *  2) **سرور Appwrite** (table `wellness_moves`).
 *  3) **کاتالوگ داخلی** ([WellnessCatalog]) — fallback آفلاین.
 */
class WellnessMoveRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val provider: BackendConfig,
) {

    suspend fun list(): List<WellnessMove> {
        if (provider.isConfigured) {
            when (val res = tables.list(TableIds.WELLNESS_MOVES, listOf("limit(200)"))) {
                is AppResult.Ok -> {
                    val parsed = res.value.mapNotNull { it.toWellnessMove() }
                    if (parsed.isNotEmpty()) {
                        writeCache(parsed)
                        return parsed
                    }
                }
                is AppResult.Err -> { /* ignore */ }
            }
        }
        return readCache().ifEmpty { WellnessCatalog.all }
    }

    suspend fun bySlug(slug: String): WellnessMove? = list().firstOrNull { it.slug == slug }

    suspend fun byCategory(category: WellnessMove.Category): List<WellnessMove> =
        list().filter { it.category == category }

    private fun cacheKey(): String = "wellness_moves_v1"

    private fun readCache(): List<WellnessMove> = runCatching {
        val array = JSONArray(store.getString(cacheKey(), "[]"))
        buildList {
            for (i in 0 until array.length()) fromJson(array.getJSONObject(i))?.let { add(it) }
        }
    }.getOrDefault(emptyList())

    private fun writeCache(items: List<WellnessMove>) {
        val arr = JSONArray()
        items.forEach { arr.put(toJson(it)) }
        store.putString(cacheKey(), arr.toString())
    }

    private fun toJson(m: WellnessMove): JSONObject = JSONObject()
        .put("slug", m.slug)
        .put("category", m.category.wire)
        .put("titleFa", m.titleFa)
        .put("level", m.level)
        .put("durationSec", m.durationSec)
        .put("reps", m.reps)
        .put("instructionsFa", m.instructionsFa)
        .put("audioCueId", m.audioCueId)
        .put("referenceImageUrl", m.referenceImageUrl)
        .put("referenceImagePromptTemplate", m.referenceImagePromptTemplate)
        .put("orderIndex", m.orderIndex)
        .put("tags", JSONArray(m.tags))

    private fun fromJson(o: JSONObject): WellnessMove? = runCatching {
        val tagsArr = o.optJSONArray("tags")
        val tags = if (tagsArr != null) buildList { for (i in 0 until tagsArr.length()) add(tagsArr.optString(i)) } else emptyList()
        WellnessMove(
            slug = o.getString("slug"),
            category = WellnessMove.Category.fromWire(o.optString("category")),
            titleFa = o.getString("titleFa"),
            level = o.optInt("level", 1),
            durationSec = o.optInt("durationSec", 60),
            reps = o.optInt("reps", 0),
            instructionsFa = o.optString("instructionsFa"),
            audioCueId = o.optString("audioCueId"),
            referenceImageUrl = o.optString("referenceImageUrl"),
            referenceImagePromptTemplate = o.optString("referenceImagePromptTemplate"),
            orderIndex = o.optInt("orderIndex", 0),
            tags = tags,
        )
    }.getOrNull()

    private fun com.hamyareman.ir.platform.core.appwrite.TableRow.toWellnessMove(): WellnessMove? {
        val slug = string("slug")
        if (slug.isBlank()) return null
        return WellnessMove(
            slug = slug,
            category = WellnessMove.Category.fromWire(string("category")),
            titleFa = string("titleFa"),
            level = long("level").toInt(),
            durationSec = long("durationSec").toInt(),
            reps = long("reps").toInt(),
            instructionsFa = string("instructionsFa"),
            audioCueId = string("audioCueId"),
            referenceImageUrl = string("referenceImageUrl"),
            referenceImagePromptTemplate = string("referenceImagePromptTemplate"),
            orderIndex = long("orderIndex").toInt(),
            tags = string("tags").split(',').map { it.trim() }.filter { it.isNotBlank() },
        )
    }
}

class WellnessLogRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val provider: BackendConfig,
    private val sync: SyncEngine,
) {
    suspend fun log(userId: String, move: WellnessMove, secondsSpent: Int, completed: Boolean, dayIso: String) {
        val rowId = "${userId.ifBlank { "anon" }}__${move.slug}__${dayIso}__${System.currentTimeMillis()}"
        val payload = mapOf(
            "userId" to userId.ifBlank { "anon" },
            "moveSlug" to move.slug,
            "category" to move.category.wire,
            "dayIso" to dayIso,
            "secondsSpent" to secondsSpent,
            "completed" to completed,
        )
        sync.enqueue(TableIds.WELLNESS_LOGS, rowId, payload)
    }

    suspend fun listForDay(userId: String, dayIso: String): List<WellnessSession> {
        if (!provider.isConfigured) return readLocalCache(dayIso)
        when (val res = tables.list(TableIds.WELLNESS_LOGS, listOf(
            "equal(\"userId\",[\"$userId\"])",
            "equal(\"dayIso\",[\"$dayIso\"])",
            "limit(50)",
        ))) {
            is AppResult.Ok -> return res.value.map { it.toWellnessSession() }
            is AppResult.Err -> return readLocalCache(dayIso)
        }
    }

    private fun readLocalCache(dayIso: String): List<WellnessSession> {
        val pending = sync.pending()
        return pending
            .filter { it.table == TableIds.WELLNESS_LOGS && it.data.opt("dayIso") == dayIso }
            .map {
                WellnessSession(
                    moveSlug = it.data.optString("moveSlug"),
                    category = it.data.optString("category"),
                    secondsSpent = (it.data.opt("secondsSpent") as? Number)?.toInt() ?: 0,
                    completed = it.data.optBoolean("completed", false),
                )
            }
    }

    private fun com.hamyareman.ir.platform.core.appwrite.TableRow.toWellnessSession(): WellnessSession = WellnessSession(
        moveSlug = string("moveSlug"),
        category = string("category"),
        secondsSpent = long("secondsSpent").toInt(),
        completed = boolean("completed"),
    )
}

data class WellnessSession(
    val moveSlug: String,
    val category: String,
    val secondsSpent: Int,
    val completed: Boolean,
)
