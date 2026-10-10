package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color
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
// (برای HMK1 رمزگشایی و برای HTML عادی عبور مستقیم) عبور کنند.
private const val MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"
private val MUSIC_TILE_URL = HmkWebViewClient.bucketUrl(MUSIC_TILE_KEY)
private val TILE_HEIGHT = 92.dp

/**
 * سقف ارتفاعِ بخش بازشده را مستقیماً از اپ به صفحه می‌دهیم. CSS خودِ فایل از
 * `100dvh - 92px` استفاده می‌کند و وقتی WebView هنوز ۹۲dp است نتیجه صفر می‌شود؛
 * کاربر با لمس کادر یک صفحهٔ خالی می‌دید. این سبک روی بایت‌های فایل باکت دست نمی‌زند.
 */
private fun WebView.applyTileExpandedHeight(hostDp: Float) {
    val sheet = (hostDp - TILE_HEIGHT.value).toInt().coerceAtLeast(240)
    evaluateJavascript(
        """
        (function(){
          var s=document.getElementById('hamyar-tile-fix');
          if(!s){s=document.createElement('style');s.id='hamyar-tile-fix';(document.head||document.documentElement).appendChild(s);}
          s.textContent='body.tilemode .backdrop.open{max-height:${sheet}px!important;min-height:200px}'+
            'body.tilemode .sheet{max-height:${sheet - 8}px!important}'+
            'body.tilemode .catalog{overflow-y:auto;min-height:120px}';
        })();
        """.trimIndent(),
        null,
    )
}

/**
 * دستگیرهٔ کنترل پلیر موسیقی از بیرون tile (مثلاً دکمه‌های پایین «بشنو و بخواب»
 * و تایمر خواب). قبلاً آن دکمه‌ها فقط PlaybackController بومی را کنترل می‌کردند که
 * هیچ ترکی نداشت؛ پس روی صدای واقعی (Web Audio داخل tile) اثری نداشتند.
 */
class MusicTileHandle {
    internal var web: WebView? = null

    /** پخش/توقف؛ همان کاری که دکمهٔ play خود پلیر می‌کند. */
    fun toggle() {
        web?.evaluateJavascript(
            "try{var p=document.getElementById('miniPlay')||document.getElementById('play');if(p)p.click();}catch(e){}",
            null,
        )
    }

    fun pause() {
        web?.evaluateJavascript(
            "try{window.BackgroundMusic&&window.BackgroundMusic.pause();}catch(e){}",
            null,
        )
    }

    /** وضعیت واقعی پخش (AudioContext در حال اجرا). */
    fun queryPlaying(onResult: (Boolean) -> Unit) {
        val view = web ?: return onResult(false)
        view.evaluateJavascript(
            "(function(){try{return !!(window.BackgroundMusic&&window.BackgroundMusic.state.playing)}catch(e){return false}})()",
        ) { onResult(it == "true") }
    }
}

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
    handle: MusicTileHandle? = null,
    onExpandedChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val appearance = LocalUiPrefs.current
    val liveAppearance = rememberUpdatedState(appearance)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var contentGateReady by remember { mutableStateOf<Boolean?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val expandedHeight = (screenHeight * 0.78f).coerceIn(420.dp, 620.dp)
    val expandedHeightDp = rememberUpdatedState(expandedHeight.value)

    LaunchedEffect(Unit) {
        // Best effort only: a plain HTML player has no encryption-key dependency.
        runCatching { HtmlMediaKey.fetch(context, container.tables) }
        contentGateReady = true
    }
    LaunchedEffect(expanded) { onExpandedChanged(expanded) }

    when (contentGateReady) {
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
                            view.applyTileExpandedHeight(expandedHeightDp.value)
                            view.publishHamyarAppearance(
                                liveAppearance.value.darkMode,
                                liveAppearance.value.darkTheme,
                                cacheHit = mainDocumentWasLoadedFromCache(),
                            )
                            // تنها صفحات موسیقی/خواب نگهبان Web Audio دارند تا با قفل صفحه قطع نشوند.
                            view.bindManagedMediaLifecycle(watchWebAudio = true)
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
            handle?.web = web

            DisposableEffect(Unit) {
                onDispose {
                    if (handle?.web === web) handle.web = null
                    web.stopManagedMedia()
                    web.destroy()
                }
            }
            LaunchedEffect(appearance.darkMode, appearance.darkTheme) {
                web.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
            }
            LaunchedEffect(expanded, expandedHeightDp.value) {
                if (expanded) web.applyTileExpandedHeight(expandedHeightDp.value)
            }
            if (expanded) {
                BackHandler {
                    web.evaluateJavascript(
                        "window.BackgroundMusic&&window.BackgroundMusic.close&&window.BackgroundMusic.close();",
                        null,
                    )
                }
            }

            // فقط یک اندازهٔ نهایی؛ بدون انیمیشنِ چندچرخه‌ای روی Android WebView.
            val height = if (expanded) expandedHeight else TILE_HEIGHT
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
