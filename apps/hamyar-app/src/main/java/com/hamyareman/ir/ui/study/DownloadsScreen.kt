@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hamyareman.ir.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * مدیریت دانلود کتاب‌ها از همان books-menu.json ساخته‌شده از منوی زندهٔ ParsPack.
 *
 * این صفحه عمداً دیگر BookModuleRegistry/BookToc یا نام فایل‌های قدیمی را نمی‌خواند:
 * مسیر URL، کلید cache و وضعیت هر PDF/صوت دقیقاً همان است که BookNodeScreen استفاده
 * می‌کند. بنابراین «دانلود شده» در اینجا معادل فایل قابل‌استفاده در صفحهٔ کتاب است.
 */
private enum class DownloadKind { PDF, AUDIO }

private data class BookDownload(
    val statusKey: String,
    val label: String,
    val bucketKey: String,
    val kind: DownloadKind,
    val cacheKey: String,
) {
    val urls: List<String> get() = StudyMedia.candidateUrls(bucketKey)
}

private fun pdfFile(context: android.content.Context, item: BookDownload): File =
    StudyPdfCache.file(context, item.cacheKey)

private fun isCached(context: android.content.Context, item: BookDownload): Boolean = when (item.kind) {
    DownloadKind.PDF -> StudyPdfCache.isValid(pdfFile(context, item))
    DownloadKind.AUDIO -> MediaVault.isVerified(context, item.cacheKey)
}

private fun cachedBytes(context: android.content.Context, item: BookDownload): Long = when (item.kind) {
    DownloadKind.PDF -> pdfFile(context, item).takeIf { StudyPdfCache.isValid(it) }?.length() ?: 0L
    DownloadKind.AUDIO -> MediaVault.vaultFile(context, item.cacheKey).takeIf { MediaVault.isVerified(context, item.cacheKey) }?.length() ?: 0L
}

/** تمام PDFها و صوت‌های حاضر در درخت یک کتاب، با حذف تکرارهای tab/گره. */
private fun collectDownloads(book: BooksMenu.Book): List<BookDownload> {
    val out = linkedMapOf<String, BookDownload>()

    fun addPdf(key: String?, ready: Boolean?, label: String) {
        if (ready != true || key.isNullOrBlank() || !BooksMenu.isPdf(key)) return
        out.putIfAbsent(
            "pdf:$key",
            BookDownload(
                statusKey = "pdf:$key",
                label = label,
                bucketKey = key,
                kind = DownloadKind.PDF,
                cacheKey = StudyMedia.bookCacheKey("book-pdf", key),
            ),
        )
    }

    fun addAudio(key: String?, label: String) {
        if (key.isNullOrBlank()) return
        out.putIfAbsent(
            "audio:$key",
            BookDownload(
                statusKey = "audio:$key",
                label = "$label · صوت تدریس",
                bucketKey = key,
                kind = DownloadKind.AUDIO,
                cacheKey = StudyMedia.bookCacheKey("book-audio", key),
            ),
        )
    }

    fun walk(nodes: List<BooksMenu.Node>) {
        nodes.forEach { node ->
            addPdf(node.key, node.ready, node.title)
            // audioKey فقط در منوی تولیدشده و فقط وقتی فایل واقعی وجود داشته باشد
            // نوشته می‌شود؛ پس دکمهٔ دانلود هرگز برای صوت placeholder ساخته نمی‌شود.
            addAudio(node.audioKey, node.title)
            node.tabs.forEach { tab ->
                addPdf(tab.key, tab.ready, "${node.title} · ${tab.title}")
            }
            walk(node.children)
        }
    }
    walk(book.items)
    return out.values.toList()
}

private fun fixedNumber(value: Int): String = toPersianDigits(value.toString()).padStart(3, '۰')
private fun megabytes(bytes: Long): String =
    toPersianDigits((bytes / (1024.0 * 1024.0)).roundToInt().toString()) + " مگابایت"

/** صفحهٔ دانلود از تایپوگرافی استاندارد سراسری استفاده می‌کند. */
@Composable
private fun DownloadsTypography(content: @Composable () -> Unit) {
    content()
}

@Composable
fun DownloadsScreen(onBack: () -> Unit) = DownloadsTypography {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { LocalStore(context, "hamyar_downloads") }
    val books = remember(context) { BooksMenu.all(context) }
    var openBook by remember { mutableStateOf(store.getString("dl_openbook_current", "")) }
    val scope = rememberCoroutineScope()
    val progress = remember { mutableStateMapOf<String, Int>() }
    val failed = remember { mutableStateMapOf<String, Boolean>() }
    var revision by remember { mutableIntStateOf(0) }

    fun startDownload(items: List<BookDownload>) {
        scope.launch {
            for (item in items.distinctBy { it.statusKey }) {
                if (!isActive || isCached(context, item) || progress.containsKey(item.statusKey)) continue
                failed.remove(item.statusKey)
                progress[item.statusKey] = 0
                try {
                    withContext(Dispatchers.IO) {
                        when (item.kind) {
                            DownloadKind.PDF -> StudyPdfCache.obtain(
                                context,
                                item.cacheKey,
                                item.urls,
                            ) { percent -> progress[item.statusKey] = percent }

                            DownloadKind.AUDIO -> MediaVault.downloadEncrypted(
                                context,
                                item.urls,
                                item.cacheKey,
                            ) { done, total ->
                                progress[item.statusKey] = if (total > 0) {
                                    ((done * 100L) / total).toInt().coerceIn(0, 100)
                                } else 0
                            }
                        }
                    }
                } catch (_: Exception) {
                    failed[item.statusKey] = true
                } finally {
                    progress.remove(item.statusKey)
                    revision++
                }
            }
        }
    }

    AppTopBar("مدیریت دانلود کتاب‌ها", onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("دانلودهای کتاب", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فهرست و مسیرها از منوی زندهٔ کتاب‌های پارس‌پک می‌آید. وضعیت اینجا با cache همان صفحهٔ کتاب یکی است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(books, key = { it.folder }) { book ->
            val files = remember(book.folder) { collectDownloads(book) }
            CurrentBookDownloadCard(
                book = book,
                files = files,
                expanded = openBook == book.folder,
                revision = revision,
                progress = progress,
                failed = failed,
                onToggle = {
                    openBook = if (openBook == book.folder) "" else book.folder
                    store.putString("dl_openbook_current", openBook)
                },
                onDownload = ::startDownload,
            )
        }
    }
}

@Composable
private fun CurrentBookDownloadCard(
    book: BooksMenu.Book,
    files: List<BookDownload>,
    expanded: Boolean,
    revision: Int,
    progress: Map<String, Int>,
    failed: Map<String, Boolean>,
    onToggle: () -> Unit,
    onDownload: (List<BookDownload>) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pdfs = remember(files) { files.filter { it.kind == DownloadKind.PDF } }
    val audios = remember(files) { files.filter { it.kind == DownloadKind.AUDIO } }
    val cachedPdf = remember(revision, files) { pdfs.count { isCached(context, it) } }
    val cachedAudio = remember(revision, files) { audios.count { isCached(context, it) } }
    val downloaded = remember(revision, files) { files.sumOf { cachedBytes(context, it) } }
    val active = progress.keys.any { it in files.map(BookDownload::statusKey).toSet() }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(book.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${megabytes(downloaded)} · ${fixedNumber(files.size)} فایل آماده برای دانلود",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "باز کردن",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (!expanded) return@Column

            Spacer(Modifier.height(10.dp))
            DownloadProgress("PDF", cachedPdf, pdfs.size)
            Spacer(Modifier.height(6.dp))
            DownloadProgress("صوت تدریس", cachedAudio, audios.size)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !active && pdfs.any { !isCached(context, it) },
                    onClick = { onDownload(pdfs.filterNot { isCached(context, it) }) },
                ) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("همهٔ PDFها")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !active && audios.any { !isCached(context, it) },
                    onClick = { onDownload(audios.filterNot { isCached(context, it) }) },
                ) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("همهٔ صوت‌ها")
                }
            }
            Spacer(Modifier.height(8.dp))
            files.forEach { item ->
                CurrentDownloadRow(
                    item = item,
                    cached = isCached(context, item),
                    percent = progress[item.statusKey],
                    hasError = failed[item.statusKey] == true,
                    enabled = !active,
                    onDownload = { onDownload(listOf(item)) },
                )
            }
        }
    }
}

@Composable
private fun DownloadProgress(label: String, done: Int, total: Int) {
    Column {
        Text(
            "$label · ${fixedNumber(done)} از ${fixedNumber(total)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(5.dp),
        )
    }
}

@Composable
private fun CurrentDownloadRow(
    item: BookDownload,
    cached: Boolean,
    percent: Int?,
    hasError: Boolean,
    enabled: Boolean,
    onDownload: () -> Unit,
) {
    val (icon, color, state) = when {
        percent != null -> Triple(Icons.Outlined.Schedule, MaterialTheme.colorScheme.primary, "${fixedNumber(percent)}٪")
        cached -> Triple(Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.tertiary, "دانلود شده")
        hasError -> Triple(Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error, "تلاش دوباره")
        item.urls.isEmpty() -> Triple(Icons.Outlined.CloudOff, MaterialTheme.colorScheme.outline, "نشانی ندارد")
        else -> Triple(Icons.Outlined.Download, MaterialTheme.colorScheme.onSurfaceVariant, "دانلود")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !cached && percent == null && item.urls.isNotEmpty(), onClick = onDownload)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(
                if (item.kind == DownloadKind.PDF) "PDF کتاب" else "صوت تدریس",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            enabled = enabled && !cached && percent == null && item.urls.isNotEmpty(),
            onClick = onDownload,
        ) { Text(state, color = color, style = MaterialTheme.typography.labelSmall) }
    }
}
