package com.hamyareman.ir.ui.content

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.hamyareman.ir.ui.study.HtmlCodec
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.ServerResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
    var html by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    remember { ContentCatalog.load(ctx) }
    val item = remember(itemId) { ContentCatalog.item(itemId) }

    LaunchedEffect(itemId) {
        val it = item
        if (it == null) {
            err = "این فایل در کاتالوگ نیست."
            return@LaunchedEffect
        }
        html = withContext(Dispatchers.IO) {
            val candidates = listOf(
                ServerResolver.pick(it.aw, it.key),
                ServerResolver.external(it.aw),
                ServerResolver.internal(it.key),
            ).distinct()
            var result: String? = null
            for (u in candidates) {
                val r = runCatching {
                    val conn = (URL(u).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 90_000
                        instanceFollowRedirects = true
                    }
                    conn.connect()
                    if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                    val bytes = conn.inputStream.use { s -> s.readBytes() }
                    conn.disconnect()
                    String(HtmlCodec.unwrap(ctx, bytes), Charsets.UTF_8)
                }
                if (r.isSuccess) {
                    result = r.getOrNull()
                    break
                }
            }
            result
        }
        if (html == null) err = "دریافت فایل ممکن نشد — اتصال اینترنت را بررسی کن."
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(item?.title ?: "محتوا", onBack)
        when {
            err != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(err.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            html == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> key(html) {
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
