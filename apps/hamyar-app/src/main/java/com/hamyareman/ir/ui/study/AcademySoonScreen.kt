package com.hamyareman.ir.ui.study

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.ui.appearance.LocalUiPrefs

/** محتوای مشترک HTML برای درس‌هایی که فایل اصلی آن‌ها هنوز تحویل نشده است. */
@Composable
fun AcademySoonScreen() {
    val appearance = LocalUiPrefs.current
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                installHamyarAppearanceBridge(appearance)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
                    }
                }
                // JavaScript باید برای دریافت قرارداد ظاهر فعال باشد؛ فایل placeholder
                // به‌تنهایی اسکریپت اجرایی ندارد.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadUrl("file:///android_asset/content/academy-coming-soon.html")
            }
        },
        update = { it.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme) },
        modifier = Modifier.fillMaxSize(),
    )
}
