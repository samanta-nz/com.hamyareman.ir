package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.stopManagedMedia

/**
 * میزبان ثابت فایل اصلی background-music.html.
 * فایل و همهٔ audio/image data-URIهای داخلش byte-for-byte نگه داشته شده‌اند؛ این صفحه
 * هیچ sanitize، encode یا upload روی محتوا انجام نمی‌دهد.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BackgroundMusicScreen(onBack: () -> Unit) {
    val webRef = remember { arrayOfNulls<WebView>(1) }
    SecureWebEffect()
    DisposableEffect(Unit) {
        onDispose { webRef[0]?.stopManagedMedia() }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("فضای آرام من", onBack)
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.bindManagedMediaLifecycle()
                        }
                    }
                    installManagedMediaLifecycle()
                    webRef[0] = this
                    loadUrl("file:///android_asset/content/background-music.html")
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                it.stopManagedMedia()
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            },
        )
    }
}
