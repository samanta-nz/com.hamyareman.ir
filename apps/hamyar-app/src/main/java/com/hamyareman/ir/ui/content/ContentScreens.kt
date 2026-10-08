package com.hamyareman.ir.ui.content

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.ManagedWebMediaEffect
import com.hamyareman.ir.ui.study.MusicEmbedPalette
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.closeMusicFrameOverlay
import com.hamyareman.ir.ui.study.installHamyarAppearanceBridge
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installMusicOverlayHost
import com.hamyareman.ir.ui.study.publishHamyarAppearance
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
    val appearance = LocalUiPrefs.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    SecureWebEffect()
    ManagedWebMediaEffect { webRef[0] }
    // popup تمام‌صفحهٔ پلیر موسیقی داخل iframe (یوگا/ورزش) باز است؟ در این حالت Back
    // فقط همان popup را می‌بندد؛ قبلاً کل درس را می‌بست.
    var musicOverlayOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = musicOverlayOpen) { webRef[0]?.closeMusicFrameOverlay() }
    var activeId by remember(itemId) { mutableStateOf(itemId) }
    var error by remember(activeId) { mutableStateOf<String?>(null) }
    var progress by remember(activeId) { mutableStateOf(0) }
    var retry by remember(activeId) { mutableStateOf(0) }
    var refreshing by remember(activeId) { mutableStateOf(false) }
    var loadTitle by remember(activeId) { mutableStateOf("محتوا در حال دانلود") }
    var reloadToken by remember(activeId) { mutableStateOf(0) }
    val catalog = remember(ctx) { ContentCatalog.apply { load(ctx) } }
    val item = catalog.item(activeId)
    val initialUrl = remember(activeId, item?.key) {
        item?.key?.let { com.hamyareman.ir.ui.study.ServerResolver.internal(it) }
    }
    var pageUrl by remember(activeId, initialUrl) {
        mutableStateOf(
            initialUrl?.takeIf { com.hamyareman.ir.ui.study.LessonCache.isCached(ctx, it) },
        )
    }
    var pageWasCached by remember(activeId, initialUrl) {
        mutableStateOf(pageUrl != null)
    }
    // پالت دوم فقط برای iframeهای موسیقیِ فایل‌های «حرکات ورزشی» است.
    // یوگا و همهٔ دسته‌های دیگر همان سبز استاندارد پلیر را نگه می‌دارند.
    val musicPalette = if (item?.cat == "sport") {
        MusicEmbedPalette.SPORT
    } else {
        MusicEmbedPalette.DEFAULT
    }

    LaunchedEffect(activeId, retry) {
        val current = item
        error = null
        progress = 0
        refreshing = false
        if (current == null) {
            pageUrl = null
            error = "این فایل در کاتالوگ نیست."
            return@LaunchedEffect
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val url = com.hamyareman.ir.ui.study.ServerResolver.internal(current.key)
        val hasCache = com.hamyareman.ir.ui.study.LessonCache.isCached(ctx, url)
        if (hasCache && pageUrl == null) {
            pageUrl = url
            pageWasCached = true
        }

        withContext(Dispatchers.IO) {
            val keyReady = com.hamyareman.ir.ui.study.HtmlMediaKey.fetch(ctx, container.tables)
            if (!keyReady) {
                handler.post {
                    if (!hasCache) {
                        error = "کلید دسترسی در دسترس نیست. دوباره وارد حساب شو."
                    }
                }
                return@withContext
            }

            val prepared = com.hamyareman.ir.ui.study.LessonCache.prepare(
                ctx = ctx,
                url = url,
                onProgress = { done, total ->
                    if (total > 0) {
                        val p = ((done.toLong() * 100L) / total).toInt().coerceIn(0, 100)
                        handler.post { progress = p }
                    }
                },
                onStatus = { status ->
                    when (status) {
                        com.hamyareman.ir.ui.study.LessonCache.Freshness.CACHED -> {
                            handler.post {
                                refreshing = false
                                progress = 100
                                pageWasCached = true
                            }
                        }
                        com.hamyareman.ir.ui.study.LessonCache.Freshness.UPDATED -> {
                            handler.post {
                                refreshing = true
                                loadTitle = "محتوا در حال بروزرسانی"
                            }
                        }
                        com.hamyareman.ir.ui.study.LessonCache.Freshness.DOWNLOADED -> {
                            handler.post {
                                refreshing = true
                                loadTitle = "محتوا در حال دانلود"
                            }
                        }
                    }
                },
            )

            handler.post {
                when {
                    prepared == null && !hasCache -> {
                        pageUrl = null
                        error = "برای بار اول باز کردن این محتوا به اینترنت نیاز است."
                    }
                    prepared == null && hasCache -> {
                        refreshing = false
                    }
                    prepared?.freshness == com.hamyareman.ir.ui.study.LessonCache.Freshness.CACHED -> {
                        refreshing = false
                        pageWasCached = true
                        progress = 100
                        pageUrl = url
                    }
                    prepared?.freshness == com.hamyyareman.ir.ui.study.LessonCache.Freshness.UPDATED ||
                    prepared?.freshness == com.hamyareman.ir.ui.study.LessonCache.Freshness.DOWNLOADED -> {
                        pageWasCached = false
                        progress = 100
                        pageUrl = url
                        reloadToken++
                    }
                }
            }
        }
    }

    // میزبان Android/WebView در تمام جابه‌جایی‌های «قبلی/بعدی» یک نمونهٔ ثابت می‌ماند؛
    // فقط سند بعدی داخل همان میزبان بار می‌شود و state/native lifecycle از نو ساخته نمی‌شود.
    Box(Modifier.fillMaxSize()) {
        if (item != null) {
            AndroidView(
                factory = { c ->
                    com.hamyareman.ir.ui.study.ZoomResetWebView(c).apply {
                        installHamyarAppearanceBridge(appearance)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        // صفحه روی origin واقعی باکت باز می‌شود تا لینک نسبی «قبلی/بعدی»،
                        // iframe موسیقی و localStorage کار کنند. میانجی، بایت HMK1 را
                        // لحظهٔ تحویل رمزگشایی می‌کند و متن‌ساده روی دیسک نمی‌نشیند.
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : com.hamyareman.ir.ui.study.HmkWebViewClient(
                            c.applicationContext,
                            com.hamyareman.ir.ui.study.HmkWebViewClient.bucketHost(),
                        ) {
                            override fun onPageFinished(view: WebView, url: String) {
                                musicOverlayOpen = false
                                super.onPageFinished(view, url)
                                view.publishHamyarAppearance(
                                    appearance.darkMode,
                                    appearance.darkTheme,
                                    musicPalette = musicPalette,
                                    cacheHit = pageWasCached,
                                )
                                view.bindManagedMediaLifecycle()
                                // درس بعدی که کاربر با لینک نسبی به آن رفته را در وضعیت اپ
                                // ثبت می‌کنیم. tag هم همین‌جا به‌روز می‌شود تا update()
                                // صفحه‌ای را که همین الان بار شده دوباره لود نکند.
                                view.tag = url.substringBefore('#')
                                ContentCatalog.itemIdForRelativeFile(
                                    url.substringBefore('#').substringBefore('?').substringAfterLast('/'),
                                )?.let { if (it != activeId) activeId = it }
                            }
                        }
                        installManagedMediaLifecycle()
                        installMusicOverlayHost { musicOverlayOpen = it }
                        webRef[0] = this
                    }
                },
                update = { view ->
                    view.publishHamyarAppearance(
                        appearance.darkMode,
                        appearance.darkTheme,
                        musicPalette = musicPalette,
                        cacheHit = pageWasCached,
                    )
                    val target = pageUrl
                    if (target != null && view.tag != "$"+"target#$"+"reloadToken") {
                        view.tag = "$"+"target#$"+"reloadToken"
                        view.loadUrl(target)
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
        when {
            error != null -> Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                androidx.compose.material3.TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
            }
            pageUrl == null -> Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    com.hamyareman.ir.ui.study.HtmlPercentLoader(progress)
                    Text(loadTitle, style = MaterialTheme.typography.titleMedium)
                }
            }
            refreshing -> Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text(loadTitle, style = MaterialTheme.typography.titleMedium)
                    if (progress > 0) Text(faNum(progress) + "٪", style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> Unit
        }
    }
}
