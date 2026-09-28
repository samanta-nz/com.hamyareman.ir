package com.hamyareman.ir.platform.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** دکمه‌ی «توقف» روی اعلانِ آلارم — صدای زنگ را قطع می‌کند. */
class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmRinger.stop()
    }
}
