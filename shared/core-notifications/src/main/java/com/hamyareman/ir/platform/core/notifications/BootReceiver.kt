package com.hamyareman.ir.platform.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** آلارم‌ها بعد از ریبوت پاک می‌شوند؛ اینجا همه را دوباره زمان‌بندی می‌کنیم. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runCatching { ReminderScheduler(context).rescheduleAll() }
    }
}
