package com.hamyareman.ir.ui.content

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.calmdown.BackgroundMusicHost
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.ManagedWebMediaEffect
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.stopManagedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun faNum(n: Int): String =
    n.toString().map { '۰' + (it - '0') }.joinToString("")

/** منوی دسته‌ها — هر دسته یک کارت؛ از کاتالوگ assets. */
@Composable
fun ContentHubScreen(onBack: () -> Unit, onCategory: (String) -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember(ctx) { ContentCatalog.apply { load(ctx) } }
    val gender = StudentProfileState.gender
    val cats = remember(gender) { catalog.categoriesFor(gender) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppTopBar("محتوای همیار", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (cats.isEmpty()) {
                Text("کاتالوگ بارگذاری نشد.", color = MaterialTheme.colorScheme.error)
            }
            cats.forEach { c ->
                val n = ContentCatalog.itemsOf(c.id, gender).size
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onCategory(c.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(c.emoji, style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (n > 0) faNum(n) + " صفحه" else "به‌زودی",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

/** فهرست فایل‌های یک دسته (با فیلتر جنسیت ثبت‌نام). */
@Composable
fun ContentCategoryScreen(cat: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember(ctx) { ContentCatalog.apply { load(ctx) } }
    val meta = remember(cat) { catalog.categories().firstOrNull { it.id == cat } }
    val gender = StudentProfileState.gender
    val items = remember(cat, gender) { catalog.itemsOf(cat, gender) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppTopBar(meta?.title ?: "محتوا", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                Text("هنوز محتوایی برای این دسته نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HubCoverGrid(
                items.map { item ->
                    HubCoverTile(
                        id = ContentCatalog.coverId(item),
                        title = item.title,
                        subtitle = ContentCatalog.tileSubtitle(item),
                        onClick = { onOpen(item.id) },
                        imageAspectRatio = ContentCatalog.coverAspectRatio(item),
                    )
                },
                slotId = "hub.content.item",
            )
        }
    }
}

/**
 * نمایش HTML: انتخاب دقیق سرور → cache رمز → HMK1 → WebView.
 * فقط حالت «سریع‌ترین» هنگام خطای origin برنده، origin دوم را امتحان می‌کند؛
 * حالت‌های دستی هرگز بی‌صدا به سرور دیگر منتقل نمی‌شوند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContentHtmlScreen(itemId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = com.hamyareman.ir.LocalAppContainer.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    SecureWebEffect()
    ManagedWebMediaEffect { webRef[0] }
    var activeId by remember(itemId) { mutableStateOf(itemId) }
    var html by remember(activeId) { mutableStateOf<String?>(null) }
    var error by remember(activeId) { mutableStateOf<String?>(null) }
    var progress by remember(activeId) { mutableStateOf(0) }
    var retry by remember(activeId) { mutableStateOf(0) }
    val catalog = remember(ctx) { ContentCatalog.apply { load(ctx) } }
    val item = catalog.item(activeId)

    LaunchedEffect(activeId, retry) {
        val current = item
        html = null
        error = null
        progress = 0
        if (current == null) {
            error = "این فایل در کاتالوگ نیست."
            return@LaunchedEffect
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val loaded = withContext(Dispatchers.IO) {
            val keyReady = com.hamyareman.ir.ui.study.HtmlMediaKey.fetch(ctx, container.tables)
            if (!keyReady) {
                Result.failure(IllegalStateException("کد بازگشایی محتوا دریافت نشد؛ دوباره وارد حساب شو."))
            } else {
                com.hamyareman.ir.ui.study.RemoteHtmlCache.load(ctx, current.aw, current.key) { done, total, _ ->
                    if (total > 0) {
                        val p = ((done * 100L) / total).toInt().coerceIn(0, 100)
                        handler.post {
                            progress = p
                        }
                    }
                }
            }
        }
        loaded.onSuccess {
            html = it.html
            progress = 100
        }.onFailure {
            error = it.message?.take(220) ?: "دریافت فایل از سرور انتخاب‌شده ممکن نشد."
        }
    }

    // میزبان Android/WebView در تمام جابه‌جایی‌های «قبلی/بعدی» یک نمونهٔ ثابت می‌ماند؛
    // فقط سند بعدی داخل همان میزبان بار می‌شود و state/native lifecycle از نو ساخته نمی‌شود.
    Box(Modifier.fillMaxSize()) {
        if (item != null) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: android.webkit.WebResourceRequest,
                            ): Boolean {
                                val target = ContentCatalog.itemIdForRelativeFile(
                                    request.url.lastPathSegment.orEmpty(),
                                ) ?: return false
                                activeId = target
                                return true
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                view.bindManagedMediaLifecycle()
                                // در یوگا/ورزش، میزبان ثابت Android همان mini-player را نگه می‌دارد.
                                // فقط نمونهٔ تکراریِ DOM پنهان می‌شود؛ رشتهٔ HTML دریافتی تغییر نمی‌کند.
                                if (item?.cat == "yoga" || item?.cat == "sport") {
                                    view.evaluateJavascript(
                                        "document.querySelectorAll('iframe[src*=\\"background-music\\"],[data-background-music],#background-music,#backgroundMusic').forEach(function(e){e.style.display=\\"none\\"});",
                                        null,
                                    )
                                }
                            }
                        }
                        installManagedMediaLifecycle()
                        webRef[0] = this
                    }
                },
                update = { view ->
                    val payload = html
                    if (payload != null && view.tag != activeId) {
                        view.tag = activeId
                        view.loadDataWithBaseURL(
                            "https://local.hamyar/", payload, "text/html", "utf-8", null,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize().then(
                    if (item.cat == "yoga" || item.cat == "sport") Modifier.padding(top = 92.dp) else Modifier,
                ),
                onRelease = {
                    it.stopManagedMedia()
                    if (webRef[0] === it) webRef[0] = null
                    it.destroy()
                },
            )
            if (item.cat == "yoga" || item.cat == "sport") {
                BackgroundMusicHost(
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(4f),
                )
            }
        }
        when {
            error != null -> Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                androidx.compose.material3.TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
            }
            html == null -> Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            ) {
                com.hamyareman.ir.ui.study.HtmlPercentLoader(progress)
            }
            else -> Unit
        }
    }
}
