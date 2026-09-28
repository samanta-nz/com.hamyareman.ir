package com.hamyareman.ir.ui.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hamyareman.ir.MainActivity
import com.hamyareman.ir.platform.core.notifications.NotificationChannels

/** اعلانِ «نسخهٔ تازه آماده است» — هر بار که اپ کامل اجرا شود و نسخهٔ تازه‌ای باشد. */ 
object UpdateNotifier {

    private const val NOTIF_ID = 7601

    fun notify(ctx: Context, info: UpdateInfo) {
        runCatching {
            val launch = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_UPDATE, true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getActivity(ctx, NOTIF_ID, launch, flags)
            val label = UpdatePlan.versionLabel(info).ifBlank { info.latest.toString() }
            val n = NotificationCompat.Builder(ctx, NotificationChannels.UPDATES)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("آپدیت برنامه")
                .setContentText("نسخهٔ $label آماده است — برای دانلود و نصب بزن.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "نسخهٔ $label آماده است. این به‌روزرسانی لازم است؛ از داخل برنامه دانلود و نصب می‌شود.",
                    ),
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, n)
        }
    }

    const val EXTRA_OPEN_UPDATE = "open_update"
}
