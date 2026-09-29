package com.hamyareman.ir.ui.study

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** محتوای مشترک HTML برای درس‌هایی که فایل اصلی آن‌ها هنوز تحویل نشده است. */
@Composable
fun AcademySoonScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadUrl("file:///android_asset/content/academy-coming-soon.html")
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
