package com.hamyareman.ir.ui.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ToolCard(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String)

internal fun WebSettings.enableStudyPinchZoom() {
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    loadWithOverviewMode = true
    useWideViewPort = true
}

/** آزمایشگاه تمام‌صفحه: pinch بدون overview تا صفحه به نوار باریک تبدیل نشود. */
internal fun WebSettings.enableLabLayout() {
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    loadWithOverviewMode = false
    useWideViewPort = false
}

@Composable
fun ToolHubScreen(
    title: String,
    subtitle: String,
    items: List<ToolCard>,
    onBack: () -> Unit,
    onOpen: (ToolCard) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.forEach { item ->
                Card(
                    onClick = { onOpen(item) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(item.emoji, fontSize = 28.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = AppTypography.cardTitle.family,
                                fontWeight = AppTypography.cardTitle.weight,
                                fontSize = AppTypography.cardTitle.size)
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = AppTypography.cardSub.family,
                                fontWeight = AppTypography.cardSub.weight,
                                fontSize = AppTypography.cardSub.size)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun GeneralToolkitScreen(onBack: () -> Unit, onOpen: (String) -> Unit) = ToolHubScreen(
    title = "جعبه‌ابزار عمومی",
    subtitle = "تقویم، ماشین‌حساب 120D و مبدل واحد — هرکدام کارت جدا.",
    items = listOf(
        ToolCard("calendar", "📅", "تقویم", "تقویم شمسی، تبدیل تاریخ و رویدادهای ذخیره‌شده"),
        ToolCard("dj120d", "🧮", "ماشین حساب 120D", "شبیه‌ساز DJ-120D Plus"),
        ToolCard("converter", "🔁", "مبدل", "مبدل همه‌کاره مهندسی")),
    onBack = onBack,
    onOpen = { onOpen(it.id) })

@Composable
fun MathToolkitScreen(onBack: () -> Unit, onOpen: (String) -> Unit) = ToolHubScreen(
    title = "جعبه‌ابزار ریاضی",
    subtitle = "ماشین‌حساب‌های مهندسی برای تمرین‌های ریاضی نهم.",
    items = listOf(
        ToolCard("ti_nspire", "📐", "TI-Nspire CX II-T CAS", "ماشین‌حساب نموداری تگزاس اینسترومنتس"),
        ToolCard("casio991", "🔢", "CASIO fx-991CW", "کاسیو ClassWiz نسل CW")),
    onBack = onBack,
    onOpen = { onOpen(it.id) })

@Composable
fun PhysicsLabScreen(onBack: () -> Unit) = ToolWebScreen("physics", "آزمایشگاه فیزیک", onBack)

@Composable
fun ChemistryLabScreen(onBack: () -> Unit) = ToolWebScreen("chemistry", "آزمایشگاه شیمی", onBack)

@Composable
fun BiologyLabScreen(onBack: () -> Unit) = ToolWebScreen("biology", "آزمایشگاه زیست‌شناسی", onBack)

internal fun toolTitle(id: String): String = when (id) {
    "calendar" -> "تقویم"
    "dj120d" -> "ماشین حساب 120D"
    "converter" -> "مبدل"
    "physics" -> "آزمایشگاه فیزیک"
    "chemistry" -> "آزمایشگاه شیمی"
    "biology" -> "آزمایشگاه زیست‌شناسی"
    "ti_nspire" -> "TI-Nspire CX II-T CAS"
    "casio991" -> "CASIO fx-991CW"
    else -> "ابزار"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ToolWebScreen(toolId: String, title: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val premium = StudentProfileState.isPaid()
    var pageUrl by remember(toolId) { mutableStateOf<String?>(null) }
    var loadErr by remember(toolId) { mutableStateOf<String?>(null) }
    LaunchedEffect(toolId) {
        loadErr = null
        val local = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ToolRemote.ensure(ctx, toolId) } }.getOrNull()
        pageUrl = when {
            local != null -> "file://$local"
            else -> {
                loadErr = "برای نمایش این صفحه به اینترنت نیاز است."
                null
            }
        }
    }
    val isLab = toolId == "chemistry" || toolId == "physics" || toolId == "biology"
    val isCalc = toolId == "ti_nspire" || toolId == "casio991" || toolId == "dj120d"
    val hideChrome = isLab || isCalc
    val activity = ctx as? Activity
    DisposableEffect(isLab) {
        if (!isLab) return@DisposableEffect onDispose { }
        val prev = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose { activity?.requestedOrientation = prev }
    }
    val bridge = remember(toolId) {
        HamyarToolBridge(ctx.applicationContext, toolId) { json ->
            ToolSaveStore.put(ctx, toolId, json)
            scope.launch {
                val uid = container.auth.cachedUserId()
                    ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                if (uid.isNotBlank()) ToolSaveStore.push(ctx, container.tables, uid)
            }
        }
    }
    fun applySaved(view: WebView?) {
        if (view == null) return
        val saved = ToolSaveStore.get(ctx, toolId)
        val quoted = if (saved.isBlank()) "null" else JSONObject.quote(saved)
        view.evaluateJavascript(
            "(function(){" +
                "function go(){try{if(window.HamyarToolApply&&$quoted)HamyarToolApply($quoted);}catch(e){}}" +
                "if(window.HamyarToolApply){go();return;}" +
                "var s=document.createElement('script');" +
                "s.src='file:///android_asset/tools/hamyar-tool-persist.js';" +
                "s.onload=go;" +
                "document.documentElement.appendChild(s);" +
                "})();",
            null)
    }
    LaunchedEffect(toolId) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) runCatching { ToolSaveStore.pull(ctx, container.tables, uid) }
        applySaved(webRef[0])
    }
    Column(Modifier.fillMaxSize()) {
        if (!hideChrome) AppTopBar(title, onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                pageUrl == null && loadErr != null -> Text(loadErr.orEmpty(), color = MaterialTheme.colorScheme.error)
                pageUrl == null -> CircularProgressIndicator()
                else -> {
                    val url = pageUrl.orEmpty()
                    AndroidView(
                    factory = { c ->
                        WebView(c).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            @Suppress("DEPRECATION")
                            run {
                                settings.allowFileAccessFromFileURLs = true
                                settings.allowUniversalAccessFromFileURLs = true
                            }
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            if (isLab) settings.enableLabLayout() else settings.enableStudyPinchZoom()
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                    Log.d("LabWebView", "${msg.message()} — ${msg.sourceId()}:${msg.lineNumber()}")
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                                override fun onPageFinished(view: WebView, url: String) {
                                    view.evaluateJavascript(toolPageJs(toolId, premium), null)
                                    view.post { applyLabViewport(view) }
                                    applySaved(view)
                                }
                            }
                            addJavascriptInterface(bridge, "HamyarTool")
                            setBackgroundColor(if (isLab) android.graphics.Color.parseColor("#050912") else android.graphics.Color.TRANSPARENT)
                            webRef[0] = this
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize().onSizeChanged {
                        webRef[0]?.let { applyLabViewport(it) }
                    },
                    onRelease = { webRef[0] = null; it.destroy() })
                }
            }
        }
    }
}

private fun applyLabViewport(view: WebView) {
    val wPx = view.width
    val hPx = view.height
    if (wPx <= 0 || hPx <= 0) return
    val d = view.resources.displayMetrics.density.coerceAtLeast(0.5f)
    val w = (wPx / d).toInt().coerceAtLeast(1)
    val h = (hPx / d).toInt().coerceAtLeast(1)
    view.evaluateJavascript(
        "window.__labW=$w;window.__labH=$h;if(typeof window.__hamyarLabLayout==='function')window.__hamyarLabLayout();",
        null,
    )
}

private fun toolPageJs(toolId: String, premium: Boolean): String = buildString {
    append(webTitleFontJs())
    if (toolId == "chemistry" || toolId == "physics" || toolId == "biology") {
        append(labLayoutJs())
        append(labLockJs(premium))
    }
    if (toolId == "calendar") append(calendarLockJs(premium))
}

/** عنوان HTML ابزار = C.pageTitle (وزیر ضخیم ۱۹) — font-display:swap تا صفحه سفید نشود. */
private fun webTitleFontJs(): String = """
    (function(){
      if (window.__hamyarTitleFont) return;
      window.__hamyarTitleFont = true;
      var s = document.createElement('style');
      s.textContent =
        "@font-face{font-family:HamyarCTitle;src:url('file:///android_res/font/vazirmatn_bold.ttf');font-weight:700;font-display:swap;}" +
        "header .brand, header .brand span, header .brand-text, header .brand-text div," +
        ".brand-title, .header-section .brand-title, .brand-info .brand-title {" +
        "font-family:HamyarCTitle,Vazirmatn,Tahoma,sans-serif !important;" +
        "font-size:19px !important;font-weight:700 !important;}";
      document.head.appendChild(s);
    })();
""".trimIndent()

/**
 * ارتفاع را از پیکسل واقعی WebView می‌گیرد (نه 100vh که در WebView اندروید صفر/غلط است).
 * media-query عرض<۱۱۰۰ را با CSS خنثی می‌کند تا workspace بریده نشود.
 */
private fun labLayoutJs(): String = """
    (function(){
      if (!document.getElementById('hamyar-lab-css')) {
        var s = document.createElement('style');
        s.id = 'hamyar-lab-css';
        s.textContent =
          'html,body{margin:0!important;overflow:hidden!important;display:flex!important;flex-direction:column!important;}' +
          '.workspace{display:flex!important;flex-wrap:wrap!important;overflow:auto!important;min-height:0!important;}' +
          '.sidebar-nav,.control-sidebar{width:100%!important;max-width:none!important;}' +
          '.stage-view{width:100%!important;min-height:240px!important;}';
        document.head.appendChild(s);
      }
      window.__hamyarLabLayout = function(){
        try {
          var vh = window.__labH || window.innerHeight || (document.documentElement && document.documentElement.clientHeight) || 0;
          if (vh < 80) return;
          var header = document.querySelector('header');
          var hh = header ? header.offsetHeight : 56;
          var rest = Math.max(160, vh - hh);
          var h = document.documentElement, b = document.body;
          if (h) { h.style.height = vh + 'px'; h.style.maxHeight = vh + 'px'; }
          if (b) { b.style.height = vh + 'px'; b.style.maxHeight = vh + 'px'; }
          var w = document.querySelector('.workspace');
          if (w) {
            w.style.height = rest + 'px';
            w.style.maxHeight = rest + 'px';
            w.style.minHeight = rest + 'px';
          }
        } catch (e) { console.log('labLayout', e); }
      };
      window.__hamyarLabLayout();
      window.addEventListener('resize', function(){ window.__hamyarLabLayout(); });
    })();
""".trimIndent()

private fun calcLockJs(): String = """
    (function(){
      if (window.__hamyarCalcLock) return;
      window.__hamyarCalcLock = true;
      var s = document.createElement('style');
      s.textContent =
        '.k-btn,.keypad,button.k-btn{pointer-events:none!important;opacity:.55!important;}' +
        'html,body{overflow:auto!important;pointer-events:auto!important;}';
      document.head.appendChild(s);
      document.addEventListener('click', function(e){
        var t = e.target;
        if (!t) return;
        if (t.closest && t.closest('.k-btn,.keypad')) { e.preventDefault(); e.stopPropagation(); }
      }, true);
      var el = document.getElementById('hamyar-sub-banner');
      if (!el) {
        el = document.createElement('div');
        el.id = 'hamyar-sub-banner';
        el.style.cssText = 'position:fixed;bottom:10px;left:10px;right:10px;z-index:2147483647;background:#7f1d1d;color:#fff;padding:10px 14px;border-radius:12px;font-family:Tahoma,sans-serif;text-align:center;font-size:13px;pointer-events:none';
        el.textContent = 'استفاده مخصوص اعضای مشترک 🔒';
        document.body.appendChild(el);
      }
    })();
""".trimIndent()

private fun calendarLockJs(premium: Boolean): String {
    val flag = if (premium) "true" else "false"
    return """
    (function(){
      if (window.__hamyarCalLock) return;
      window.__hamyarCalLock = true;
      var premium = $flag;
      function banner(on){
        var el = document.getElementById('hamyar-cal-banner');
        if (!on) { if (el) el.style.display = 'none'; return; }
        if (!el) {
          el = document.createElement('div');
          el.id = 'hamyar-cal-banner';
          el.style.cssText = 'position:fixed;bottom:10px;left:10px;right:10px;z-index:2147483647;background:#7f1d1d;color:#fff;padding:10px 14px;border-radius:12px;font-family:Tahoma,sans-serif;text-align:center;font-size:13px;pointer-events:none';
          el.textContent = 'فقط بخش «تقویم و تبدیل» رایگان است. بقیه با اشتراک فعال باز می‌شود.';
          document.body.appendChild(el);
        }
        el.style.display = 'block';
        setTimeout(function(){ el.style.display = 'none'; }, 2800);
      }
      var orig = window.switchTab;
      if (typeof orig === 'function' && !orig.__hy) {
        var wrapped = function(name, btn){
          if (!premium && name !== 'convert') { banner(true); return; }
          return orig.apply(this, arguments);
        };
        wrapped.__hy = true;
        window.switchTab = wrapped;
      }
      if (!premium) {
        document.querySelectorAll('.tab-btn').forEach(function(b){
          var oc = b.getAttribute('onclick') || '';
          if (oc.indexOf('convert') >= 0) return;
          if (b.dataset.hyLock) return;
          b.dataset.hyLock = '1';
          b.style.opacity = '0.7';
          b.appendChild(document.createTextNode(' 🔒'));
        });
      }
    })();
    """.trimIndent()
}

private fun labLockJs(premium: Boolean): String {
    val flag = if (premium) "true" else "false"
    return """
    (function(){
      if (window.__hamyarLabLock) return;
      window.__hamyarLabLock = true;
      window.HamyarPremium = $flag;
      window.__labLinear = 0;
      function items(){ return Array.prototype.slice.call(document.querySelectorAll('.exp-item')); }
      function linear(){
        var a = document.querySelector('.exp-item.active');
        var i = items().indexOf(a);
        return i < 0 ? (window.__labLinear||0) : i;
      }
      function free(){ return !!window.HamyarPremium || linear() <= 1; }
      function banner(on){
        var el = document.getElementById('hamyar-sub-banner');
        if (!on) { if (el) el.style.display = 'none'; return; }
        if (!el) {
          el = document.createElement('div');
          el.id = 'hamyar-sub-banner';
          el.style.cssText = 'position:fixed;bottom:10px;left:10px;right:10px;z-index:2147483647;background:#7f1d1d;color:#fff;padding:10px 14px;border-radius:12px;font-family:Tahoma,sans-serif;text-align:center;font-size:13px;pointer-events:none';
          el.textContent = 'اجرای این آزمایش با اشتراک فعال ممکن هست 🔒';
          document.body.appendChild(el);
        }
        el.style.display = 'block';
      }
      function decorate(){
        if (window.HamyarPremium) return;
        items().forEach(function(el, i){
          if (i <= 1) return;
          if (el.dataset.hyLock) return;
          el.dataset.hyLock = '1';
          el.style.opacity = '0.72';
          el.appendChild(document.createTextNode(' 🔒'));
        });
      }
      function wrap(name){
        var fn = window[name];
        if (typeof fn !== 'function' || fn.__hy) return;
        var wrapped = function(){
          if (name.indexOf('navigate') === 0) {
            var el = arguments[2];
            if (el) window.__labLinear = items().indexOf(el);
            var r = fn.apply(this, arguments);
            decorate();
            banner(!free());
            return r;
          }
          if (name === 'buildNavTree') {
            var r2 = fn.apply(this, arguments);
            decorate();
            banner(!free());
            return r2;
          }
          if (!free()) { banner(true); return; }
          return fn.apply(this, arguments);
        };
        wrapped.__hy = true;
        window[name] = wrapped;
      }
      ['buildNavTree','navigateChemLab','navigatePhysLab','navigateBioLab','toggleChemSim','togglePhysSim','toggleBioSim','startSim'].forEach(wrap);
      document.addEventListener('click', function(e){
        var t = e.target;
        if (!t || free()) return;
        var run = (t.id === 'btnRun') || (t.closest && t.closest('#btnRun,.btn-run'));
        if (run) { e.preventDefault(); e.stopPropagation(); banner(true); }
      }, true);
      decorate();
      banner(!free());
    })();
    """.trimIndent()
}

internal class HamyarToolBridge(
    private val appCtx: android.content.Context,
    private val toolId: String,
    private val onChanged: (String) -> Unit) {
    @JavascriptInterface
    fun onSave(json: String) {
        if (json.isBlank()) return
        ToolSaveStore.put(appCtx, toolId, json)
        onChanged(json)
    }

    @JavascriptInterface
    fun load(): String = ToolSaveStore.get(appCtx, toolId)
}
