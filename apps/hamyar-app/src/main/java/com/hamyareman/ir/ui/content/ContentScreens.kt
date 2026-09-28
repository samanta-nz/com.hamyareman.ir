package com.hamyareman.ir.ui.content

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.key
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
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.study.SecureWebEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun faNum(n: Int): String =
    n.toString().map { '۰' + (it - '0') }.joinToString("")

/** منوی دسته‌ها — هر دسته یک کارت؛ از کاتالوگ assets. */
@Composable
fun ContentHubScreen(onBack: () -> Unit, onCategory: (String) -> Unit) {
    val ctx = LocalContext.current
    remember { ContentCatalog.load(ctx) }
    val cats = remember { ContentCatalog.categories() }
    val gender = StudentProfileState.gender
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
    remember { ContentCatalog.load(ctx) }
    val meta = remember(cat) { ContentCatalog.categories().firstOrNull { it.id == cat } }
    val gender = StudentProfileState.gender
    val items = remember(cat, gender) { ContentCatalog.itemsOf(cat, gender) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppTopBar(meta?.title ?: "محتوا", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.isEmpty()) {
                Text("هنوز محتوایی برای این دسته نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items.forEach { it ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpen(it.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("📄")
                        Text(it.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * نمایش یک صفحهٔ HTML از کاتالوگ: انتخاب سرور → دریافت → باز کردن HMK1 → WebView.
 * اگر سرورِ انتخاب‌شده جواب نداد، سرور دیگر را هم امتحان می‌کند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContentHtmlScreen(itemId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    SecureWebEffect()
    var activeId by remember(itemId) { mutableStateOf(itemId) }
    var html by remember(activeId) { mutableStateOf<String?>(null) }
    var error by remember(activeId) { mutableStateOf<String?>(null) }
    var progress by remember(activeId) { mutableStateOf(0) }
    var sourceLabel by remember(activeId) { mutableStateOf("") }
    var retry by remember(activeId) { mutableStateOf(0) }
    remember { ContentCatalog.load(ctx) }
    val item = ContentCatalog.item(activeId)

    LaunchedEffect(activeId, retry) {
        val current = item
        html = null
        error = null
        progress = 0
        sourceLabel = ""
        if (current == null) {
            error = "این فایل در کاتالوگ نیست."
            return@LaunchedEffect
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val loaded = withContext(Dispatchers.IO) {
            com.hamyareman.ir.ui.study.RemoteHtmlCache.load(ctx, current.aw, current.key) { done, total, source ->
                if (total > 0) {
                    val p = ((done * 100L) / total).toInt().coerceIn(0, 100)
                    handler.post {
                        progress = p
                        sourceLabel = if (source == com.hamyareman.ir.ui.study.ServerPrefs.Origin.INTERNAL) "آروان" else "Appwrite"
                    }
                }
            }
        }
        loaded.onSuccess {
            html = it.html
            progress = 100
            sourceLabel = if (it.source == com.hamyareman.ir.ui.study.ServerPrefs.Origin.INTERNAL) "آروان" else "Appwrite"
        }.onFailure {
            error = it.message?.take(220) ?: "دریافت فایل از سرور انتخاب‌شده ممکن نشد."
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(item?.title ?: "محتوا", onBack)
        if (sourceLabel.isNotBlank()) {
            Text(
                "منبع: $sourceLabel",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                androidx.compose.material3.TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
            }
            html == null -> com.hamyareman.ir.ui.study.HtmlPercentLoader(progress)
            else -> key(activeId, html) {
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
                            }
                            loadDataWithBaseURL(
                                "https://local.hamyar/", html!!, "text/html", "utf-8", null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
