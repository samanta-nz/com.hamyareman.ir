package com.hamyareman.ir.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hamyareman.ir.MainActivity
import com.hamyareman.ir.R

/** دریافت هشدار یادآور دارو و نمایش نوتیفیکیشن. */
class MedsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "دارو"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآور دارو و مراقبت", NotificationManager.IMPORTANCE_HIGH),
        )
        val pi = PendingIntent.getActivity(
            context, 101,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("یادآور مراقبت 💊")
            .setContentText("وقت $name است؛ مراقب خودت باش 🌿")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(name.hashCode(), notif)
    }

    companion object { private const val CHANNEL_ID = "meds_reminder" }
}
