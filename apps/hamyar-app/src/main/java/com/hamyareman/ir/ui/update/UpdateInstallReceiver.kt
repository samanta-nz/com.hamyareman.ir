package com.hamyareman.ir.ui.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/**
 * نتیجهٔ نصبِ «نشستِ PackageInstaller» را به کاربر نشان می‌دهد.
 *
 * نکتهٔ مهم: اگر سیستم بگوید `STATUS_PENDING_USER_ACTION`، باید همان اینتنتی که در
 * `Intent.EXTRA_INTENT` آمده **از همین گیرنده** باز شود؛ وگرنه پنجرهٔ تأییدِ نصب
 * هیچ‌وقت به کاربر نشان داده نمی‌شود و نصب بی‌صدا رد می‌شود.
 */
class UpdateInstallReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        // حتی اگر نصب از مسیر ACTION_VIEW انجام شده باشد و callback نشست نداشته
        // باشیم، سیستم پس از جایگزینی همین بسته این broadcast را می‌فرستد.
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ApkUpdate.cleanupAfterSuccessfulInstall(context)
            return
        }
        if (intent.action != ApkUpdate.ACTION_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirm) }
            }
            return
        }
        if (status == PackageInstaller.STATUS_SUCCESS) {
            // data directory در آپدیت حفظ می‌شود؛ APK و فایل part دیگر لازم نیستند.
            ApkUpdate.onInstallSucceeded(context)
        }
        val text = when (status) {
            PackageInstaller.STATUS_SUCCESS ->
                "نسخهٔ تازه نصب شد ✅ — فایل نصب پاک شد."
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                "نصب لغو شد."
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                "نصب اجازه نداشت؛ «نصب برنامه‌های ناشناس» را برای همیار من روشن کن."
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                "نسخهٔ نصب‌شده با این فایل نمی‌سازد (کلیدِ امضا یکی نیست)."
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                "این فایل با گوشیِ تو سازگار نیست."
            else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                ?: "نصب کامل نشد؛ یک‌بار دیگر تلاش کن."
        }
        runCatching { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    }
}
