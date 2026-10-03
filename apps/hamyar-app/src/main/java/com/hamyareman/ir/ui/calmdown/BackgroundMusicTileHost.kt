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
// (و در نتیجه رمزگشاییِ HMK1) عبور کنند.
private const val MUSIC_TILE_KEY = "Bucket/Html-files/background-music-tile.html"
private val MUSIC_TILE_URL = HmkWebViewClient.bucketUrl(MUSIC_TILE_KEY)
private val TILE_HEIGHT = 92.dp

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
                            // تنها صفحات موسیقی/خواب نگهبان Web Audio دارند تا با قفل صفحه قطع نشوند.
                            view.bindManagedMediaLifecycle(watchWebAudio = true)
                            // background-music-tile.html خودش iframe داخلی را fixed می‌کند.
                            // در Android WebView آن لایهٔ fixed را به flow محلی تبدیل می‌کنیم؛
                            // خود فایل باکت، رمزنگاری و ۲۹ صدای embed شده دست‌نخورده می‌مانند.
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarSleepTileSafe)return;
                                  window.__hamyarSleepTileSafe=true;
                                  var s=document.createElement('style');
                                  s.setAttribute('data-hamyar-sleep-tile-safe','');
                                  s.textContent =
                                    'body.tilemode{position:relative!important;overflow:visible!important;touch-action:auto!important}' +
                                    'body.tilemode .backdrop.open{position:absolute!important;inset:0!important;max-height:none!important;overflow:visible!important}' +
                                    'body.is-open{position:relative!important;overflow:visible!important;touch-action:auto!important}' +
                                    'body.is-open #scrim{position:absolute!important;inset:0!important;z-index:2147483646!important}' +
                                    'body.is-open #backgroundMusicFrame,body.tilemode .backdrop.open iframe{position:absolute!important;top:0!important;right:0!important;bottom:0!important;left:0!important;width:100%!important;height:100%!important;max-width:none!important;max-height:none!important;z-index:2147483647!important}';
                                  (document.head||document.documentElement).appendChild(s);

                                  var apply=function(){
                                    try{
                                      var body=document.body;
                                      var backdrop=document.getElementById('backdrop');
                                      var open=!!((backdrop&&backdrop.classList.contains('open')) || body.classList.contains('is-open'));
                                      body.style.setProperty('overflow','visible','important');
                                      body.style.setProperty('position','relative','important');
                                      if(open){
                                        var frame=document.getElementById('backgroundMusicFrame');
                                        if(frame){
                                          frame.style.setProperty('position','absolute','important');
                                          frame.style.setProperty('top','0','important');
                                          frame.style.setProperty('right','0','important');
                                          frame.style.setProperty('bottom','0','important');
                                          frame.style.setProperty('left','0','important');
                                          frame.style.setProperty('width','100%','important');
                                          frame.style.setProperty('height','100%','important');
                                          frame.style.setProperty('z-index','2147483647','important');
                                        }
                                      }
                                    }catch(e){}
                                  };
                                  new MutationObserver(apply).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style']});
                                  window.addEventListener('resize',apply,{passive:true});
                                  apply();
                                })();
                                """.trimIndent(),
                                null,
                            )
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
            if (expanded) {
                BackHandler {
                    web.evaluateJavascript(
                        "window.BackgroundMusic&&window.BackgroundMusic.close&&window.BackgroundMusic.close();",
                        null,
                    )
                }
            }

            // فقط یک اندازهٔ نهایی؛ بدون انیمیشنِ چندچرخه‌ای روی Android WebView.
            val height = if (expanded) (screenHeight - 24.dp).coerceAtLeast(420.dp) else TILE_HEIGHT
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
