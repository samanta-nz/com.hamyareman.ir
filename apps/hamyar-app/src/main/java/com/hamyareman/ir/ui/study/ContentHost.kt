package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * میزبان یکپارچه‌ی بارگذاری برای هر محتوایی که از باکت می‌آید.
 * محتوای آماده به [content] داده می‌شود؛ بقیه‌ی وضعیت‌ها (بررسی، قطع اینترنت، دانلود با پیشرفت،
 * دیالوگ بروزرسانی، خطا) در همین‌جا و یک‌دست برای همه‌ی بخش‌ها نمایش داده می‌شود.
 */
@Composable
fun ContentHost(
    url: String,
    modifier: Modifier = Modifier,
    kind: ContentKind = ContentLoader.kindOf(url),
    content: @Composable (ContentReady) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember(url) { ContentLoader.session(ctx, url, kind) }
    val state by session.state.collectAsState()

    LaunchedEffect(session) { session.start(scope) }
    DisposableEffect(session) { onDispose { session.cancel() } }

    Box(modifier.fillMaxSize()) {
        when (val s = state) {
            is ContentState.Checking -> CenterColumn {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("در حال آماده‌سازی…", style = MaterialTheme.typography.bodyMedium)
            }

            is ContentState.NoInternet -> CenterColumn {
                Text(
                    "اتصال اینترنت برقرار نیست. اتصال خود را بررسی کن و دوباره تلاش کن.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.retry(scope) }) { Text("تلاش دوباره") }
            }

            is ContentState.Failed -> CenterColumn {
                Text(s.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.retry(scope) }) { Text("تلاش دوباره") }
            }

            is ContentState.Downloading -> CenterColumn { DownloadPanel(s, session) }

            is ContentState.Ready -> content(s.value)

            is ContentState.UpdateAvailable -> {
                // نمایش کش ادامه دارد؛ دیالوگ روی آن می‌آید.
                content(s.ready)
                AlertDialog(
                    onDismissRequest = { session.answerUpdate(false) },
                    title = { Text("محتوا بروزرسانی شده") },
                    text = { Text("نسخه‌ی تازه‌ای از این محتوا موجود است. برای نمایش آن لازم است محتوا دوباره بارگذاری شود.") },
                    confirmButton = { Button(onClick = { session.answerUpdate(true) }) { Text("بارگذاری مجدد") } },
                    dismissButton = { TextButton(onClick = { session.answerUpdate(false) }) { Text("بعداً") } },
                )
            }
        }
    }
}

@Composable
private fun CenterColumn(body: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { body() }
}

@Composable
private fun DownloadPanel(s: ContentState.Downloading, session: ContentSession) {
    val p = s.progress
    val frac = if (p.totalBytes > 0L) (p.doneBytes.toFloat() / p.totalBytes).coerceIn(0f, 1f) else null
    Text(
        if (s.isUpdate) "در حال دانلود نسخه‌ی تازه" else "در حال دانلود محتوا",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    // نوار پیشرفت همیشه چپ‌به‌راست پر می‌شود.
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (frac != null) {
            LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(8.dp))
    val sizeText = if (p.totalBytes > 0L) {
        "${fmtBytes(p.doneBytes)} از ${fmtBytes(p.totalBytes)}"
    } else {
        fmtBytes(p.doneBytes)
    }
    Text(sizeText, style = MaterialTheme.typography.bodyMedium)
    Text(
        if (p.paused) "متوقف‌شده (مکث)" else "سرعت: ${fmtBytes(p.speedBytesPerSec)}/ثانیه",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (p.paused) {
            Button(onClick = { session.resume() }) { Text("ادامه") }
        } else {
            OutlinedButton(onClick = { session.pause() }) { Text("مکث") }
        }
        OutlinedButton(onClick = { session.stop() }) { Text("توقف") }
    }
}

private fun fmtBytes(b: Long): String {
    if (b < 1024L) return "$b B"
    val kb = b / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}
