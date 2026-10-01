package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.stopManagedMedia

private class MusicSheetBridge(private val onExpanded: (Boolean) -> Unit) {
    @JavascriptInterface
    fun setExpanded(value: Boolean) = onExpanded(value)
}

/**
 * همان کادر mini داخل background-music.html، بدون دست‌کاری حتی یک بایت از HTML یا
 * data-URIها. WebView تا پایان عمر صفحه ثابت می‌ماند؛ بازشدن sheet فقط ارتفاع میزبان
 * Android را تغییر می‌دهد، بنابراین تعویض درس صدا را reload نمی‌کند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BackgroundMusicHost(
    modifier: Modifier = Modifier,
    startExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(startExpanded) }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    DisposableEffect(Unit) {
        onDispose { webRef[0]?.stopManagedMedia() }
    }
    val hostModifier = modifier.then(
        when {
            expanded && startExpanded -> Modifier.fillMaxSize()
            expanded -> Modifier.fillMaxWidth().height(620.dp)
            else -> Modifier.fillMaxWidth().height(92.dp)
        },
    )
    Box(hostModifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    addJavascriptInterface(
                        MusicSheetBridge { value -> post { expanded = value } },
                        "HamyarMusicHost",
                    )
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.bindManagedMediaLifecycle()
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarHostBound)return;
                                  window.__hamyarHostBound=true;
                                  const send=()=>HamyarMusicHost.setExpanded(document.body.classList.contains('is-open'));
                                  new MutationObserver(send).observe(document.body,{attributes:true,attributeFilter:['class']});
                                  send();
                                  ${if (startExpanded) "window.BackgroundMusic&&window.BackgroundMusic.open();" else ""}
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }
                    installManagedMediaLifecycle()
                    webRef[0] = this
                    loadUrl("file:///android_asset/content/background-music.html#embedded")
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

/**
 * صفحهٔ «فضای آرام من» حالا همان نجواهای آرام‌بخش است: فایل کامل از باکت و
 * تایمر خواب بومی. کادر جمع‌شوندهٔ قبلی از اینجا و از یوگا/حرکات ورزشی برداشته شد.
 */
@Composable
fun BackgroundMusicScreen(onBack: () -> Unit) = CalmWhispersScreen(onBack)
