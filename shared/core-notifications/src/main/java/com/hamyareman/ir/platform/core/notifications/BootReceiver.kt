package com.hamyareman.ir.platform.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** آلارم‌ها بعد از ریبوت پاک می‌شوند؛ اینجا همه را دوباره زمان‌بندی می‌کنیم. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        ) return
        runCatching { ReminderScheduler(context).rescheduleAll() }
        runCatching { QuietHoursAutomation.schedule(context) }
    }
}
