package com.hamyareman.ir.ui.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.hamyareman.ir.platform.core.common.LocalStore
import java.util.UUID

/**
 * شناسه‌ی پایدار همین نصب/دستگاه برای سقف دو دستگاه.
 *
 * اول ANDROID_ID (با نصب مجدد روی همان گوشی یکی می‌ماند)؛ اگر خالی/معیوب بود
 * یک UUID روی استور محلی ساخته می‌شود.
 */
object DeviceIdentity {
    private const val STORE = "hamyar_device"
    private const val KEY = "device_id"
    private const val BAD_ANDROID_ID = "9774d56d682e549c"

    fun id(ctx: Context): String {
        val store = LocalStore(ctx, STORE)
        val saved = store.getString(KEY)
        if (saved.isNotBlank()) return saved
        val androidId = runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
        val id = androidId.takeIf { it.isNotBlank() && !it.equals(BAD_ANDROID_ID, true) }
            ?: UUID.randomUUID().toString()
        store.putString(KEY, id)
        return id
    }

    fun label(): String {
        val raw = listOf(Build.MANUFACTURER, Build.MODEL)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        return raw.ifBlank { "دستگاه ناشناس" }
    }
}
