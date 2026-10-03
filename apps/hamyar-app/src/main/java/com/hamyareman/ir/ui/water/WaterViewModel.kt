package com.hamyareman.ir.ui.water

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hamyareman.ir.HamyarApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WaterViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HamyarApplication).container
    private val repo = WaterRepository(container.store, container.sync)

    private val _ui = MutableStateFlow(repo.state())
    val uiState: StateFlow<WaterUiState> = _ui

    private val _pending = MutableStateFlow(container.sync.pendingCount())
    val pendingSync: StateFlow<Int> = _pending

    private val _note = MutableStateFlow<String?>(null)
    val note: StateFlow<String?> = _note

    fun addGlass() {
        repo.addGlass()
        val state = repo.state()
        container.dailyHealth.recordWater(state.goal, state.consumed, +1)
        syncImmediately()
    }

    fun undoGlass() {
        val before = repo.state().consumed
        repo.undoGlass()
        val state = repo.state()
        if (state.consumed != before) container.dailyHealth.recordWater(state.goal, state.consumed, -1)
        syncImmediately()
    }

    fun setGoal(goal: Int) {
        repo.setGoal(goal)
        val state = repo.state()
        container.dailyHealth.recordWaterGoal(state.goal)
        syncImmediately()
    }

    /** تلاش بی‌صدا برای فرستادن صف؛ اگر آفلاین باشیم چیزی خراب نمی‌شود. */
    fun syncNow() {
        viewModelScope.launch {
            if (container.isBackendConfigured) {
                val remote = runCatching { container.dailyHealth.pullToday() }.getOrNull()
                remote?.let {
                    repo.applyRemoteState(it.waterGoal, it.waterConsumed)
                }
            }
            val report = container.dailyHealth.syncNow()
            _ui.value = repo.state()
            _pending.value = report.remaining
            _note.value = when {
                report.pushed > 0 -> "دریافت و ارسال با دیتابیس انجام شد."
                report.remaining > 0 -> "چند قلم هنوز در صف است."
                else -> null
            }
        }
    }

    private fun syncImmediately() {
        refresh()
        viewModelScope.launch {
            if (container.isBackendConfigured) runCatching { container.dailyHealth.syncNow() }
            _pending.value = container.sync.pendingCount()
        }
    }

    fun 
        _ui.value = repo.state()
        _pending.value = container.sync.pendingCount()
    }
}
