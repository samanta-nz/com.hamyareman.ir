package com.hamyareman.ir.ui.water

import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine

data class WaterUiState(val goal: Int = 8, val consumed: Int = 0) {
    val remaining: Int get() = (goal - consumed).coerceAtLeast(0)
    val isGoalReached: Boolean get() = consumed >= goal
}

/**
 * مصرف آب — محلی‌اول.
 *
 * مصرف آب حساب کاربر در جدول `water_logs` ذخیره می‌شود تا دادهٔ شخصیِ خودش
 * بین دستگاه‌ها دریافت/ارسال شود. `weekly_optin` فقط کنترل اشتراک هفتگی است.
 */
class WaterRepository(
    private val store: LocalStore,
    private val sync: SyncEngine? = null,
) {
    private fun key(): String = "consumed_${JalaliDate.todayIso()}"

    fun state(): WaterUiState = WaterUiState(
        store.getInt("water_goal", DEFAULT_GOAL).coerceIn(1, 30),
        store.getInt(key(), 0),
    )

    fun addGlass() {
        val current = state()
        store.putInt(key(), (current.consumed + 1).coerceAtMost(current.goal + 10))
enqueueIfConfigured()
    }

    fun undoGlass() {
        val current = state()
        store.putInt(key(), (current.consumed - 1).coerceAtLeast(0))
        enqueueIfOptedIn()
    }

    fun setGoal(goal: Int) {
        store.putInt("water_goal", goal.coerceIn(1, 30))
        enqueueIfConfigured()
    }

    private fun enqueueIfConfigured() {
        val engine = sync ?: return
        val dayIso = JalaliDate.todayIso()
        engine.enqueue(
            table = TableIds.WATER_LOGS,
            rowId = "water_$dayIso",
            payload = mapOf(
                "dayIso" to dayIso,
                "glasses" to state().consumed,
                "ownerId" to store.getString(AppwriteAuthService.KEY_USER_ID),
            ),
        )
    }

    companion object {
        const val DEFAULT_GOAL = 8
    }
}
