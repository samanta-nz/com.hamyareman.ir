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
        refresh()
    }

    fun undoGlass() {
        repo.undoGlass()
        refresh()
    }

    fun setGoal(goal: Int) {
        repo.setGoal(goal)
        refresh()
    }

    /** تلاش بی‌صدا برای فرستادن صف؛ اگر آفلاین باشیم چیزی خراب نمی‌شود. */
    fun syncNow() {
        if (!container.isBackendConfigured) return
        viewModelScope.launch {
            val report = container.sync.pushAll()
            _pending.value = report.remaining
            _note.value = when {
                report.pushed > 0 -> "به‌روزرسانی‌ها به سرور رسید."
                report.remaining > 0 -> "چند قلم هنوز در صف است (آفلاین)."
                else -> null
            }
        }
    }

    private fun refresh() {
        _ui.value = repo.state()
        _pending.value = container.sync.pendingCount()
    }
}
