package com.hamyareman.ir.ui.hub

import com.hamyareman.ir.platform.core.appwrite.AppResult
import com.hamyareman.ir.platform.core.appwrite.BackendConfig
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class RoutineActivity(
    val id: String,
    val title: String,
    val category: String = "عمومی",
    val startMinute: Int = 8 * 60,
    val durationMinutes: Int = 20,
    val done: Boolean = false,
)

data class DailyHealthActivity(
    val id: String,
    val type: String,
    val title: String,
    val value: Int = 0,
    val atMs: Long = 0L,
    val meta: String = "",
)

data class DailyHealthSnapshot(
    val dayIso: String,
    val waterGoal: Int = 8,
    val waterConsumed: Int = 0,
    val lightDay: Boolean = false,
    val routine: List<RoutineActivity> = emptyList(),
    val activities: List<DailyHealthActivity> = emptyList(),
    val updatedAt: Long = 0L,
) {
    val routineDone: Int get() = routine.count { it.done }
    val routineTotal: Int get() = routine.size
    val sportsMinutes: Int
        get() = activities
            .filter { it.type == "exercise" || (it.type == "wellness" && it.meta in SPORT_CATEGORIES) }
            .sumOf { it.value }
    val yogaMinutes: Int
        get() = activities.filter { it.type == "wellness" && it.meta == "yoga" }.sumOf { it.value }
    val exerciseMinutes: Int
        get() = activities.filter { it.type == "exercise" || (it.type == "wellness" && it.meta == "exercise") }.sumOf { it.value }
    val wellnessMinutes: Int
        get() = activities.filter { it.type == "wellness" }.sumOf { it.value }

    companion object {
        private val SPORT_CATEGORIES = setOf("yoga", "exercise")
    }
}

class DailyHealthRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val provider: BackendConfig,
    private val sync: SyncEngine,
    private val userIdProvider: () -> String,
) {
    private fun dayKey(dayIso: String): String = "health_daily_" + dayIso
    private fun localKey(dayIso: String): String = "daily_health_snapshot_" + dayIso

    private fun rowId(dayIso: String): String {
        val seed = userIdProvider() + "|" + dayIso
        val hex = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "dh_" + hex.take(27)
    }

    fun snapshot(dayIso: String = JalaliDate.todayIso()): DailyHealthSnapshot =
        readLocal(dayIso) ?: DailyHealthSnapshot(dayIso = dayIso)

    suspend fun pull(dayIso: String = JalaliDate.todayIso()): DailyHealthSnapshot {
        val local = snapshot(dayIso)
        if (!provider.isConfigured || userIdProvider().isBlank()) return local

        return when (val result = tables.get(TableIds.APP_STATE, rowId(dayIso))) {
            is AppResult.Ok -> {
                val remote = result.value?.string("payload").orEmpty()
                if (remote.isBlank()) return local
                val decoded = fromJson(runCatching { JSONObject(remote) }.getOrNull(), dayIso) ?: return local
                val merged = merge(local, decoded)
                saveLocal(merged)
                merged
            }
            is AppResult.Err -> local
        }
    }

    suspend fun pullToday(): DailyHealthSnapshot = pull(JalaliDate.todayIso())

    suspend fun syncNow() = sync.pushAll()

    fun setLightDay(enabled: Boolean, dayIso: String = JalaliDate.todayIso()): DailyHealthSnapshot {
        val current = snapshot(dayIso)
        val next = current.copy(
            lightDay = enabled,
            updatedAt = System.currentTimeMillis(),
            activities = addActivity(
                current.activities,
                type = "routine_mode",
                title = if (enabled) "روز سبک فعال شد" else "روز معمولی فعال شد",
                meta = if (enabled) "light" else "normal",
            ),
        )
        persistAndQueue(next)
        return next
    }

    fun saveRoutine(
        items: List<RoutineActivity>,
        dayIso: String = JalaliDate.todayIso(),
        actionTitle: String = "روتین به‌روزرسانی شد",
    ): DailyHealthSnapshot {
        val current = snapshot(dayIso)
        val next = current.copy(
            routine = items.distinctBy { it.id }.take(40),
            updatedAt = System.currentTimeMillis(),
            activities = addActivity(
                current.activities,
                type = "routine",
                title = actionTitle,
                value = items.size,
            ),
        )
        persistAndQueue(next)
        mirrorRoutineRows(next)
        return next
    }

    fun recordWater(
        goal: Int,
        consumed: Int,
        delta: Int,
        dayIso: String = JalaliDate.todayIso(),
    ): DailyHealthSnapshot {
        val current = snapshot(dayIso)
        val next = current.copy(
            waterGoal = goal.coerceIn(1, 30),
            waterConsumed = consumed.coerceAtLeast(0),
            updatedAt = System.currentTimeMillis(),
            activities = addActivity(
                current.activities,
                type = "water",
                title = if (delta >= 0) "یک لیوان آب ثبت شد" else "یک لیوان آب کم شد",
                value = delta,
                meta = "consumed=" + consumed,
            ),
        )
        persistAndQueue(next)
        mirrorWaterRow(next)
        return next
    }

    fun recordWaterGoal(goal: Int, dayIso: String = JalaliDate.todayIso()): DailyHealthSnapshot {
        val current = snapshot(dayIso)
        val next = current.copy(
            waterGoal = goal.coerceIn(1, 30),
            updatedAt = System.currentTimeMillis(),
            activities = addActivity(current.activities, "water_goal", "هدف آب تغییر کرد", goal),
        )
        persistAndQueue(next)
        mirrorWaterRow(next)
        return next
    }

    fun recordExercise(
        exerciseId: String,
        title: String,
        minutes: Int,
        dayIso: String = JalaliDate.todayIso(),
    ): DailyHealthSnapshot =
        recordActivity("exercise", title, minutes.coerceAtLeast(0), exerciseId, dayIso)

    fun recordWellness(
        moveSlug: String,
        title: String,
        minutes: Int,
        category: String,
        dayIso: String = JalaliDate.todayIso(),
    ): DailyHealthSnapshot =
        recordActivity("wellness", title, minutes.coerceAtLeast(0), category.ifBlank { moveSlug }, dayIso)

    fun recordActivity(
        type: String,
        title: String,
        value: Int = 0,
        meta: String = "",
        dayIso: String = JalaliDate.todayIso(),
    ): DailyHealthSnapshot {
        val current = snapshot(dayIso)
        val next = current.copy(
            updatedAt = System.currentTimeMillis(),
            activities = addActivity(current.activities, type, title, value, meta),
        )
        persistAndQueue(next)
        return next
    }

    private fun persistAndQueue(snapshot: DailyHealthSnapshot) {
        saveLocal(snapshot)
        sync.enqueue(
            table = TableIds.APP_STATE,
            rowId = rowId(snapshot.dayIso),
            payload = mapOf(
                "userId" to userIdProvider(),
                "key" to dayKey(snapshot.dayIso),
                "payload" to toJson(snapshot).toString(),
                "updatedAt" to snapshot.updatedAt,
            ),
        )
    }

    private fun mirrorWaterRow(snapshot: DailyHealthSnapshot) {
        mirrorCompatibilityKeys(snapshot)
        val owner = userIdProvider()
        if (owner.isBlank()) return
        sync.enqueue(
            TableIds.WATER_LOGS,
            "water_" + snapshot.dayIso,
            mapOf("dayIso" to snapshot.dayIso, "glasses" to snapshot.waterConsumed, "ownerId" to owner),
        )
    }

    private fun mirrorRoutineRows(snapshot: DailyHealthSnapshot) {
        val owner = userIdProvider()
        if (owner.isBlank()) return
        snapshot.routine.forEach { item ->
            sync.enqueue(
                TableIds.ROUTINE_BLOCKS,
                "routine_" + snapshot.dayIso + "_" + item.id,
                mapOf(
                    "dayIso" to snapshot.dayIso,
                    "blockId" to item.id,
                    "title" to item.title,
                    "done" to item.done,
                    "ownerId" to owner,
                ),
            )
        }
    }

    private fun mirrorCompatibilityKeys(snapshot: DailyHealthSnapshot) {
        store.putInt("water_goal", snapshot.waterGoal)
        store.putInt("consumed_" + snapshot.dayIso, snapshot.waterConsumed)
        store.putBool("light_day", snapshot.lightDay)
        val arr = JSONArray()
        snapshot.routine.forEach { arr.put(routineToJson(it)) }
        store.putString("daily_routine_" + snapshot.dayIso, arr.toString())
    }

    private fun readLocal(dayIso: String): DailyHealthSnapshot? =
        runCatching {
            val raw = store.getString(localKey(dayIso), "")
            if (raw.isBlank()) null else fromJson(JSONObject(raw), dayIso)
        }.getOrNull()

    private fun saveLocal(snapshot: DailyHealthSnapshot) {
        store.putString(localKey(snapshot.dayIso), toJson(snapshot).toString())
        mirrorCompatibilityKeys(snapshot)
    }

    private fun addActivity(
        existing: List<DailyHealthActivity>,
        type: String,
        title: String,
        value: Int = 0,
        meta: String = "",
    ): List<DailyHealthActivity> {
        val now = System.currentTimeMillis()
        val item = DailyHealthActivity(
            id = type + "_" + now + "_" + existing.size,
            type = type,
            title = title,
            value = value,
            atMs = now,
            meta = meta,
        )
        return (listOf(item) + existing).take(100)
    }

    private fun merge(a: DailyHealthSnapshot, b: DailyHealthSnapshot): DailyHealthSnapshot {
        val activities = (a.activities + b.activities)
            .distinctBy { it.id }
            .sortedByDescending { it.atMs }
            .take(100)
        val routineById = (a.routine + b.routine).associateBy { it.id }
        val remoteNewer = b.updatedAt >= a.updatedAt
        return DailyHealthSnapshot(
            dayIso = a.dayIso,
            waterGoal = if (remoteNewer) b.waterGoal else a.waterGoal,
            waterConsumed = if (remoteNewer) b.waterConsumed else a.waterConsumed,
            lightDay = if (remoteNewer) b.lightDay else a.lightDay,
            routine = routineById.values.sortedBy { it.startMinute },
            activities = activities,
            updatedAt = maxOf(a.updatedAt, b.updatedAt),
        )
    }

    private fun toJson(snapshot: DailyHealthSnapshot): JSONObject = JSONObject().apply {
        put("dayIso", snapshot.dayIso)
        put("waterGoal", snapshot.waterGoal)
        put("waterConsumed", snapshot.waterConsumed)
        put("lightDay", snapshot.lightDay)
        put("updatedAt", snapshot.updatedAt)
        put("routine", JSONArray().apply { snapshot.routine.forEach { put(routineToJson(it)) } })
        put("activities", JSONArray().apply { snapshot.activities.forEach { put(activityToJson(it)) } })
    }

    private fun routineToJson(item: RoutineActivity): JSONObject = JSONObject()
        .put("id", item.id)
        .put("title", item.title)
        .put("category", item.category)
        .put("startMinute", item.startMinute)
        .put("durationMinutes", item.durationMinutes)
        .put("done", item.done)

    private fun activityToJson(item: DailyHealthActivity): JSONObject = JSONObject()
        .put("id", item.id)
        .put("type", item.type)
        .put("title", item.title)
        .put("value", item.value)
        .put("atMs", item.atMs)
        .put("meta", item.meta)

    private fun fromJson(o: JSONObject?, dayIso: String): DailyHealthSnapshot? {
        if (o == null) return null
        val routine = mutableListOf<RoutineActivity>()
        o.optJSONArray("routine")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                routine += RoutineActivity(
                    id = id,
                    title = item.optString("title"),
                    category = item.optString("category", "عمومی"),
                    startMinute = item.optInt("startMinute", 8 * 60),
                    durationMinutes = item.optInt("durationMinutes", 20).coerceIn(1, 240),
                    done = item.optBoolean("done", false),
                )
            }
        }
        val activities = mutableListOf<DailyHealthActivity>()
        o.optJSONArray("activities")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                activities += DailyHealthActivity(
                    id = id,
                    type = item.optString("type"),
                    title = item.optString("title"),
                    value = item.optInt("value", 0),
                    atMs = item.optLong("atMs", 0L),
                    meta = item.optString("meta"),
                )
            }
        }
        return DailyHealthSnapshot(
            dayIso = o.optString("dayIso", dayIso),
            waterGoal = o.optInt("waterGoal", 8).coerceIn(1, 30),
            waterConsumed = o.optInt("waterConsumed", 0).coerceAtLeast(0),
            lightDay = o.optBoolean("lightDay", false),
            routine = routine,
            activities = activities,
            updatedAt = o.optLong("updatedAt", 0L),
        )
    }
}
