package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.LocalAppContainer
import java.util.LinkedHashMap

/**
 * نمایشگر PDF کتاب درس (پرامپت ۰۵ + تصمیم مصوب): فایل از باکت Appwrite
 * دانلود و روی گوشی کش می‌شود (filesDir — بدون کش سرور)، با PdfRenderer
 * بومی اندروید صفحه‌به‌صفحه رندر می‌شود (lazy + LRU).
 */

private sealed class PdfState {
    data object Idle : PdfState()
    data class Downloading(val progressPct: Int) : PdfState()
    data class Ready(val pageCount: Int) : PdfState()
    data class Error(val message: String) : PdfState()
}

/** کش LRU صفحه‌ها تا حافظه کنترل شود (همزمان حداکثر ~۸ صفحه). */
private class PageCache(private val maxPages: Int = 8) {
    private val map = LinkedHashMap<Int, Bitmap>(16, 0.75f, true)

    operator fun get(index: Int): Bitmap? = synchronized(this) { map[index] }

    operator fun set(index: Int, value: Bitmap) {
        synchronized(this) {
            map[index] = value
            while (map.size > maxPages) map.remove(map.keys.first())
        }
    }
}

@Composable
fun LessonPdfScreen(packId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val pack = remember(packId) { container.studyPacks.pack(packId) }
    val fileId = pack?.pdfFileName.orEmpty()

    var state by remember(fileId) { mutableStateOf<PdfState>(PdfState.Idle) }
    val pageCache = remember(fileId) { PageCache() }

    LaunchedEffect(fileId) {
        if (fileId.isBlank()) {
            state = PdfState.Error("این پک فایل PDF ندارد.")
            return@LaunchedEffect
        }
        try {
            state = PdfState.Downloading(0)
            val target = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                StudyPdfCache.obtain(ctx, fileId) { pct ->
                    if ((state as? PdfState.Downloading)?.progressPct != pct) {
                        state = PdfState.Downloading(pct)
                    }
                }
            }
            state = PdfState.Downloading(100)
            val pageCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
                val probe = PdfRenderer(fd)
                try { probe.pageCount } finally { probe.close() }
            }
            state = PdfState.Ready(pageCount)
        } catch (e: Exception) {
            state = PdfState.Error(e.message ?: "خطای ناشناخته در باز کردن PDF")
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "📕 " + (pack?.title ?: "کتاب درس"), onBack = onBack)
        // v1.9: پلیر صوت در همه‌ی صفحات جزوه‌ها هم هست (صوت همان درس، همان‌جا پخش می‌شود).
        val tracks = remember(packId) { pack?.let { teachTracksOf(it) } ?: emptyList() }
        val bookTitle = remember(packId) {
            com.hamyareman.ir.platform.feature.study.BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == packId } }?.title.orEmpty()
        }
        if (tracks.isNotEmpty()) {
            TeachAudioBar(packId = packId, screenTitle = pack?.title ?: "", bookTitle = bookTitle, tracks = tracks)
        }
        when (val st = state) {
            is PdfState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(st.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            is PdfState.Downloading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("در حال آماده‌سازی کتاب… (${st.progressPct}٪)", style = MaterialTheme.typography.bodySmall)
                }
            }
            is PdfState.Ready -> {
                val p = pack
                if (p != null) {
                    TeachPdfPages(modifier = Modifier.weight(1f).fillMaxSize(), fileId = fileId, pack = p)
                } else {
                    Text("کتاب پیدا نشد.", modifier = Modifier.padding(16.dp))
                }
            }
            PdfState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
