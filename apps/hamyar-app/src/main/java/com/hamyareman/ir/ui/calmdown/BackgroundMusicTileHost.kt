package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.ui.study.HmkWebViewClient
import com.hamyareman.ir.ui.study.HtmlMediaKey
import com.hamyareman.ir.ui.study.ServerResolver
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installHamyarAppearanceBridge
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.publishHamyarAppearance
import com.hamyareman.ir.ui.study.stopManagedMedia

/** فایل tile کوچک و قابل‌استفادهٔ مجدد برای «بشنو و بخواب» و میزبان‌های آینده. */
private const val MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"
private val TILE_HEIGHT = 92.dp

private class MusicTileBridge(private val onExpanded: (Boolean) -> Unit) {
    @JavascriptInterface fun setExpanded(value: Boolean) = onExpanded(value)

    /**
     * تایمر بی‌لمسی تنها یک منبع دارد و آن خودِ tile است (همهٔ لمس‌های داخل
     * iframe را می‌بیند). این متد فقط برای سازگاری با HTML نگه داشته شده.
     */
    @JavascriptInterface fun touch() = Unit
}

/**
 * کادر ۹۲dp در جریان طبیعی صفحه می‌ماند. با لمس، همان WebView (بدون reload و
 * بدون قطع صدا) داخل یک Popup تمام‌صفحه می‌نشیند تا پاپ‌آپ HTML با اورلی روی کل
 * صفحهٔ میزبان دیده شود؛ با بسته‌شدن، دقیقاً به جای قبلی در جریان صفحه برمی‌گردد.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BackgroundMusicTileHost(
    modifier: Modifier = Modifier,
    onExpandedChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val appearance = LocalUiPrefs.current
    val liveAppearance = rememberUpdatedState(appearance)
    var keyReady by remember { mutableStateOf<Boolean?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        keyReady = runCatching { HtmlMediaKey.fetch(context, container.tables) }.getOrDefault(false)
    }

    when (keyReady) {
        null -> Box(modifier.fillMaxWidth().height(TILE_HEIGHT), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        false -> Box(modifier.fillMaxWidth().height(TILE_HEIGHT), contentAlignment = Alignment.Center) {
            Text("دریافت پخش‌کننده ممکن نشد؛ اتصال اینترنت را بررسی کن.")
        }
        true -> {
            // یک نمونهٔ واحد از WebView که بین کادر و پاپ‌آپ جابه‌جا می‌شود؛ جابه‌جایی
            // view در اندروید صفحه را دوباره لود نمی‌کند، بنابراین پخش قطع نمی‌شود.
            val web = remember {
                WebView(context).apply {
                    installHamyarAppearanceBridge(appearance)
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    addJavascriptInterface(
                        MusicTileBridge { value -> post { expanded = value } },
                        "HamyarMusicTileHost",
                    )
                    webViewClient = object : HmkWebViewClient(
                        context.applicationContext,
                        HmkWebViewClient.bucketHost(),
                    ) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.post { pageReady = true }
                            view.publishHamyarAppearance(
                                liveAppearance.value.darkMode,
                                liveAppearance.value.darkTheme,
                                cacheHit = mainDocumentWasLoadedFromCache(),
                            )
                            view.bindManagedMediaLifecycle()
                            // وضعیت باز/بسته فقط از همین کلاس HTML خوانده می‌شود؛
                            // منطق تایمر و پاپ‌آپ کامل سمت HTML است.
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarTileBound)return;
                                  window.__hamyarTileBound=true;
                                  const report=()=>HamyarMusicTileHost.setExpanded(document.body.classList.contains('is-open'));
                                  new MutationObserver(report).observe(document.body,{attributes:true,attributeFilter:['class']});
                                  report();
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }
                    installManagedMediaLifecycle()
                    loadUrl(ServerResolver.internal(MUSIC_TILE_KEY))
                }
            }

            LaunchedEffect(expanded) { onExpandedChanged(expanded) }
            DisposableEffect(Unit) {
                onDispose {
                    web.stopManagedMedia()
                    (web.parent as? ViewGroup)?.removeView(web)
                    web.destroy()
                }
            }
            LaunchedEffect(appearance.darkMode, appearance.darkTheme) {
                web.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
            }

            // جای ثابت کادر در جریان صفحه؛ حتی وقتی WebView موقتاً داخل پاپ‌آپ است
            // این فضا خالی نمی‌شود تا چیدمان صفحه نلرزد و کادر «سر جای قبلی» برگردد.
            Box(modifier.fillMaxWidth().height(TILE_HEIGHT)) {
                if (!expanded) {
                    AndroidView(
                        factory = { (web.parent as? ViewGroup)?.removeView(web); web },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (!pageReady) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
            }

            if (expanded) {
                BackHandler { web.closeMusicPopup() }
                Popup(
                    properties = PopupProperties(focusable = true, clippingEnabled = false),
                    onDismissRequest = { web.closeMusicPopup() },
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { (web.parent as? ViewGroup)?.removeView(web); web },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/** بستن پاپ‌آپ از سمت اپ؛ خود HTML تصمیم نهایی را می‌گیرد و وضعیت را گزارش می‌کند. */
private fun WebView.closeMusicPopup() {
    evaluateJavascript("window.BackgroundMusic&&window.BackgroundMusic.close&&window.BackgroundMusic.close();", null)
}
