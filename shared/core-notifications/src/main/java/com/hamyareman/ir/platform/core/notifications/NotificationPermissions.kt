package com.hamyareman.ir.platform.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** بررسی مجوز اعلان (از اندروید ۱۳ لازم است). */
object NotificationPermissions {
    const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
