package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.study.HmkWebViewClient
import com.hamyareman.ir.ui.study.HtmlAudioKeepAliveService
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.ServerResolver
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.stopManagedMedia
import kotlinx.coroutines.delay

/**
 * «نجواهای آرام‌بخش طبیعت» — تنها جایی که `background-music-full.html` باز می‌شود
 * و تنها جایی که تایمر خواب وجود دارد.
 *
 * چرا تایمر بومی است: فایل آپلودشده روی باکت هیچ `HamyarHost`، `keepAlive` یا
 * تایمری ندارد (بررسی شد) و قرار است HTMLها دست‌نخورده بمانند. پس شمارش معکوس،
 * اعلان و wake lock سمت اپ انجام می‌شود و در پایان، پخش صفحه متوقف می‌شود.
 */
private const val MUSIC_FULL_KEY = "Bucket/Html-files/background-music-full.html"

private val TIMER_CHOICES = listOf(15, 30, 45, 60)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CalmWhispersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    SecureWebEffect()

    var minutes by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(0) }

    // شمارش معکوس؛ در پایان صدا و سرویس با هم بسته می‌شوند.
    LaunchedEffect(minutes) {
        if (minutes <= 0) {
            secondsLeft = 0
            HtmlAudioKeepAliveService.stop(ctx)
            return@LaunchedEffect
        }
        HtmlAudioKeepAliveService.start(ctx)
        secondsLeft = minutes * 60
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        webRef[0]?.stopManagedMedia()
        HtmlAudioKeepAliveService.stop(ctx)
        minutes = 0
    }

    DisposableEffect(Unit) {
        onDispose {
            webRef[0]?.stopManagedMedia()
            HtmlAudioKeepAliveService.stop(ctx)
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نجواهای آرام‌بخش طبیعت", onBack)

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (secondsLeft > 0) "تایمر خواب: ${format(secondsLeft)} مانده" else "تایمر خواب",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TIMER_CHOICES.forEach { m ->
                    AssistChip(
                        onClick = { minutes = if (minutes == m) 0 else m },
                        label = { Text(if (minutes == m) "■ $m دقیقه" else "$m دقیقه") },
                    )
                }
                if (minutes > 0) TextButton(onClick = { minutes = 0 }) { Text("لغو") }
            }
            Text(
                if (secondsLeft > 0) {
                    "با خاموش شدن صفحه هم ادامه می‌دهد و در پایان خودش قطع می‌شود."
                } else {
                    "بدون تایمر، با خاموش شدن صفحه صدا قطع می‌شود."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        webViewClient = object : HmkWebViewClient(
                            c.applicationContext,
                            HmkWebViewClient.bucketHost(),
                        ) {
                            override fun onPageFinished(view: WebView, url: String) {
                                view.bindManagedMediaLifecycle()
                            }
                        }
                        installManagedMediaLifecycle()
                        webRef[0] = this
                        loadUrl(ServerResolver.internal(MUSIC_FULL_KEY))
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
    }
}

private fun format(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "$m:" + s.toString().padStart(2, '0')
}
