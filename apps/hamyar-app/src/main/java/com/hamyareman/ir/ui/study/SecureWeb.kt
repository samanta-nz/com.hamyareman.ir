package com.hamyareman.ir.ui.study

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat

/**
 * اسکرین‌شات و ضبط صفحه را مسدود می‌کند. روی Android 14+ تلاش ثبت‌شده با پیام
 * انگلیسی کوتاه اعلام می‌شود؛ نسخه‌های قدیمی API تشخیص تلاش را ندارند اما
 * FLAG_SECURE همچنان خود تصویر را مسدود می‌کند.
 */
@Composable
fun SecureWebEffect(message: String = "Screenshots are disabled on this page.") {
    val activity = LocalActivity.current
    DisposableEffect(activity, message) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        var callback: Activity.ScreenCaptureCallback? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity != null) {
            val screenCallback = Activity.ScreenCaptureCallback {
                val toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT)
                toast.show()
                Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 900L)
            }
            callback = screenCallback
            activity.registerScreenCaptureCallback(ContextCompat.getMainExecutor(activity), screenCallback)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity != null) {
                callback?.let { activity.unregisterScreenCaptureCallback(it) }
            }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
