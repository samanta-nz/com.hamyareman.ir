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
import com.hamyareman.ir.ui.hub.MedsStore

class MedsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id").orEmpty()
        val name = intent.getStringExtra("name").orEmpty()
            .ifBlank {
                MedsStore.load(com.hamyareman.ir.platform.core.common.LocalStore(context, "hamyar_health"))
                    .firstOrNull { it.id == id }?.name ?: "دارو"
            }

        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "یادآور دارو و مراقبت",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "هشدارهای زمان‌دار دارو و مراقبت"
                enableVibration(true)
            },
        )

        val openApp = PendingIntent.getActivity(
            context,
            8101,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("وقت دارو 💊")
            .setContentText("وقت «$name» است.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("وقت «$name» است."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notifications.notify(if (id.isBlank()) name.hashCode() else id.hashCode(), notification)
        if (id.isNotBlank()) MedsStore.scheduleNext(context, id)
    }
}
