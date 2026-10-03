package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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

/** فایل tile کاملِ باکت (خودبسنده، همهٔ صداها داخل خودش). */
// نشانی مستقیم و ثابتِ باکت: مسیر موسیقی عمداً از ServerResolver رد نمی‌شود تا
// صفحه و هر درخواستِ داخلِ آن روی یک origin بمانند و حتماً از HmkWebViewClient
// (و در نتیجه رمزگشاییِ HMK1) عبور کنند.
private const val MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"
private val MUSIC_TILE_URL = HmkWebViewClient.bucketUrl(MUSIC_TILE_KEY)
private val TILE_HEIGHT = 92.dp

/**
 * کادر ۹۲dp در جریان صفحه می‌ماند و با لمس، **رو به پایین** باز می‌شود؛ همان
 * آکاردئونِ `body.tilemode` در خود HTML. تایمر دو ثانیه‌ایِ بی‌لمسی هم تنها در
 * HTML است (چون همهٔ لمس‌ها را می‌بیند) و اپ فقط ارتفاع میزبان را دنبال می‌کند
 * تا سقف `calc(100dvh - 92px)` واقعاً جا داشته باشد.
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
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var keyReady by remember { mutableStateOf<Boolean?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        keyReady = runCatching { HtmlMediaKey.fetch(context, container.tables) }.getOrDefault(false)
    }
    LaunchedEffect(expanded) { onExpandedChanged(expanded) }

    when (keyReady) {
        null -> Box(modifier.fillMaxWidth().height(TILE_HEIGHT), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        false -> Box(modifier.fillMaxWidth().height(TILE_HEIGHT), contentAlignment = Alignment.Center) {
            Text("دریافت پخش‌کننده ممکن نشد؛ اتصال اینترنت را بررسی کن.")
        }
        true -> {
            val web = remember {
                WebView(context).apply {
                    // هم پل ظاهر همیار و هم HamyarHost (قرارداد خودِ فایل‌های موسیقی)
                    installHamyarAppearanceBridge(
                        prefs = appearance,
                        onMusicTile = { value -> post { expanded = value } },
                    )
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
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
                            // پشتیبان: اگر رابط HamyarHost به هر دلیل صدا نخورد،
                            // تغییر کلاس body همان وضعیت را گزارش می‌کند.
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarTileBound)return;
                                  window.__hamyarTileBound=true;
                                  var report=function(){
                                    var open=document.getElementById('backdrop');
                                    open=open?open.classList.contains('open'):document.body.classList.contains('is-open');
                                    HamyarHost.onMusicTile(open);
                                  };
                                  new MutationObserver(report).observe(document.body,{attributes:true,attributeFilter:['class'],subtree:true});
                                  report();
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }
                    installManagedMediaLifecycle()
                    loadUrl(MUSIC_TILE_URL)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    web.stopManagedMedia()
                    web.destroy()
                }
            }
            LaunchedEffect(appearance.darkMode, appearance.darkTheme) {
                web.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
            }
            if (expanded) {
                BackHandler {
                    web.evaluateJavascript(
                        "window.BackgroundMusic&&window.BackgroundMusic.close&&window.BackgroundMusic.close();",
                        null,
                    )
                }
            }

            // باز که می‌شود، میزبان تا ته صفحه بالا می‌آید تا آکاردئون بریده نشود.
            val target = if (expanded) (screenHeight - 24.dp).coerceAtLeast(420.dp) else TILE_HEIGHT
            val height by animateDpAsState(target, tween(340), label = "music tile height")
            Box(modifier.fillMaxWidth().height(height)) {
                AndroidView(
                    factory = { web },
                    modifier = Modifier.fillMaxSize(),
                )
                if (!pageReady) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}
