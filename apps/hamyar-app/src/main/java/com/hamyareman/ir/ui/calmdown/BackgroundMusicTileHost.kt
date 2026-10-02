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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.ui.study.HmkWebViewClient
import com.hamyareman.ir.ui.study.HtmlMediaKey
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installHamyarAppearanceBridge
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.publishHamyarAppearance
import com.hamyareman.ir.ui.study.stopManagedMedia
import kotlinx.coroutines.delay

/** فایل tile کوچک و قابل‌استفادهٔ مجدد برای «بشنو و بخواب» و میزبان‌های آینده. */
private const val MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"

private class MusicTileBridge(
    private val onExpanded: (Boolean) -> Unit,
    private val onInteraction: () -> Unit,
) {
    @JavascriptInterface fun setExpanded(value: Boolean) = onExpanded(value)
    @JavascriptInterface fun touch() = onInteraction()
}

/**
 * tile در جریان طبیعی صفحه می‌ماند. برخلاف iframeهای درس، هیچ `fixed` یا overlay
 * سراسری ندارد؛ بازشدن فقط ارتفاع همین WebView را رو به پایین زیاد می‌کند.
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
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val webRef = remember { arrayOfNulls<WebView>(1) }
    var keyReady by remember { mutableStateOf<Boolean?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    fun setExpanded(value: Boolean) {
        expanded = value
        onExpandedChanged(value)
        if (value) interactionTick++
    }

    // شرط محصول: اگر دو ثانیه هیچ تعامل جدیدی در mixer نبود، tile جمع می‌شود.
    LaunchedEffect(expanded, interactionTick) {
        if (!expanded) return@LaunchedEffect
        delay(2_000)
        webRef[0]?.evaluateJavascript("window.BackgroundMusic&&window.BackgroundMusic.close&&window.BackgroundMusic.close();", null)
        setExpanded(false)
    }

    LaunchedEffect(Unit) {
        keyReady = runCatching { HtmlMediaKey.fetch(context, container.tables) }.getOrDefault(false)
    }

    val openHeight = ((screenHeight * 0.72f).toInt()).coerceIn(340, 620).dp
    val height = if (expanded) openHeight else 92.dp
    when (keyReady) {
        null -> Box(modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> Box(modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
            Text("دریافت پخش‌کننده ممکن نشد؛ اتصال اینترنت را بررسی کن.")
        }
        true -> Box(modifier.fillMaxWidth().height(height)) {
            AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    installHamyarAppearanceBridge(appearance)
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    addJavascriptInterface(
                        MusicTileBridge(
                            onExpanded = { value -> post { setExpanded(value) } },
                            onInteraction = { post { if (expanded) interactionTick++ } },
                        ),
                        "HamyarMusicTileHost",
                    )
                    webViewClient = object : HmkWebViewClient(
                        viewContext.applicationContext,
                        HmkWebViewClient.bucketHost(),
                    ) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.post { pageReady = true }
                            view.publishHamyarAppearance(
                                appearance.darkMode,
                                appearance.darkTheme,
                                cacheHit = mainDocumentWasLoadedFromCache(),
                            )
                            view.bindManagedMediaLifecycle()
                            // HTML tile ممکن است از postMessage مخصوص iframe استفاده کند؛
                            // در WebView مستقیم، تغییر class و همهٔ لمس‌ها را به native می‌رسانیم.
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarTileBound)return;
                                  window.__hamyarTileBound=true;
                                  const report=()=>HamyarMusicTileHost.setExpanded(document.body.classList.contains('is-open'));
                                  new MutationObserver(report).observe(document.body,{attributes:true,attributeFilter:['class']});
                                  ['pointerdown','touchstart','click','input','change','keydown'].forEach(name=>
                                    document.addEventListener(name,()=>HamyarMusicTileHost.touch(),{passive:true,capture:true})
                                  );
                                  report();
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }
                    installManagedMediaLifecycle()
                    webRef[0] = this
                    loadUrl(com.hamyareman.ir.ui.study.ServerResolver.internal(MUSIC_TILE_KEY))
                }
            },
            update = { it.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme) },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                it.stopManagedMedia()
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            },
            )
            if (!pageReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
