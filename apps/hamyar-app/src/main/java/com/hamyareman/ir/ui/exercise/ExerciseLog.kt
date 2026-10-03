package com.hamyareman.ir.ui.exercise

import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import com.hamyareman.ir.ui.hub.DailyHealthRepository
import org.json.JSONArray
import org.json.JSONObject

/** یک جلسه‌ی تمرین تمام‌شده (محلی؛ فقط با opt-in به سرور می‌رود). */
internal data class ExerciseSession(
    val dayIso: String,
    val exerciseId: String,
    val title: String,
    val minutes: Int,
    val atMs: Long,
)

private const val KEY = "exercise_sessions"
private const val MAX_HISTORY = 200

internal fun readExerciseSessions(store: LocalStore): List<ExerciseSession> = runCatching {
    val array = JSONArray(store.getString(KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                ExerciseSession(
                    dayIso = o.optString("dayIso"),
                    exerciseId = o.optString("exerciseId"),
                    title = o.optString("title"),
                    minutes = o.optInt("minutes"),
                    atMs = o.optLong("atMs"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeExerciseSessions(store: LocalStore, sessions: List<ExerciseSession>) {
    val array = JSONArray()
    sessions.take(MAX_HISTORY).forEach { s ->
        array.put(
            JSONObject()
                .put("dayIso", s.dayIso).put("exerciseId", s.exerciseId)
                .put("title", s.title).put("minutes", s.minutes).put("atMs", s.atMs),
        )
    }
    store.putString(KEY, array.toString())
}

/**
 * ثبت جلسه‌ی تمام‌شده.
 *
 * مثل ماژول آب: **فقط** اگر زهرا «خلاصه‌ی هفتگی» را روشن کرده باشد (`weekly_optin`)
 * در صف Sync می‌رود؛ وگرنه فقط روی دستگاه می‌ماند و در نمودار پیشرفت خودش دیده می‌شود.
 */
internal fun recordExerciseSession(
    store: LocalStore,
    sync: SyncEngine?,
    exercise: Exercise,
    dailyHealth: DailyHealthRepository? = null,
) {
    val dayIso = JalaliDate.todayIso()
    val now = System.currentTimeMillis()
    val minutes = exercise.totalDurationMinutes
    val sessions = readExerciseSessions(store).toMutableList()
    sessions.add(0, ExerciseSession(dayIso, exercise.id, exercise.title, minutes, now))
    writeExerciseSessions(store, sessions)

    if (sync != null) {
        sync.enqueue(
            table = TableIds.EXERCISE_LOGS,
            rowId = "ex_${dayIso}_${exercise.id}_$now",
            payload = mapOf(
                "dayIso" to dayIso,
                "exerciseId" to exercise.id,
                "minutes" to minutes,
                "ownerId" to store.getString(AppwriteAuthService.KEY_USER_ID),
            ),
        )
    }

    dailyHealth?.recordExercise(
        exerciseId = exercise.id,
        title = exercise.title,
        minutes = minutes,
        dayIso = dayIso,
    )
}

internal fun exerciseSessionsOn(store: LocalStore, dayIso: String): Int =
    readExerciseSessions(store).count { it.dayIso == dayIso }

internal fun exerciseMinutesOn(store: LocalStore, dayIso: String): Int =
    readExerciseSessions(store).filter { it.dayIso == dayIso }.sumOf { it.minutes }
