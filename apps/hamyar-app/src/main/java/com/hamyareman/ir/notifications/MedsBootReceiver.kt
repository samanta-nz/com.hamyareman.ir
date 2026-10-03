package com.hamyareman.ir.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hamyareman.ir.ui.hub.MedsStore

class MedsBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> MedsStore.scheduleAll(context)
        }
    }
}
