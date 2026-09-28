package com.hamyareman.admin

import android.app.Application

class AdminApplication : Application() {
    lateinit var container: AdminContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                getSharedPreferences(CRASH_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(
                        CRASH_KEY,
                        (error.javaClass.simpleName + ": " + (error.message ?: "") + "\n" +
                            error.stackTraceToString()).take(2500),
                    )
                    .commit()
            }
            val name = thread.name.orEmpty()
            // کرش SDK روی نخ OkHttp نباید کل فرایند را بکشد.
            if (name.contains("OkHttp", ignoreCase = true) || name.contains("DefaultDispatcher")) {
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, error)
        }
        container = AdminContainer(this)
    }

    fun rebuildContainer() {
        container = AdminContainer(this)
    }

    fun consumeLastCrash(): String? {
        val prefs = getSharedPreferences(CRASH_PREFS, MODE_PRIVATE)
        val text = prefs.getString(CRASH_KEY, null)?.trim().orEmpty()
        if (text.isBlank()) return null
        prefs.edit().remove(CRASH_KEY).apply()
        return text
    }

    companion object {
        private const val CRASH_PREFS = "admin_crash"
        private const val CRASH_KEY = "last"
    }
}
