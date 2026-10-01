@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hamyareman.ir.ui.study

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.BookToc
import com.hamyareman.ir.platform.feature.study.BookToc.TocNode
import androidx.compose.material3.OutlinedButton
import com.hamyareman.ir.platform.feature.study.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import java.net.HttpURLConnection
import java.net.URL
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.awaitOnlineBlocking
import com.hamyareman.ir.ui.net.ResilientHttp

/**
 * «مدیریت دانلود کتاب‌ها» (v1.16) — با همان ساختار فهرست رسمی هر کتاب:
 *  - فصل/بخش = کارت آکاردئونی (هر لحظه فقط یکی باز؛ پیش‌فرض جمع؛ حافظه‌دار)؛
 *  - فقط ردیف‌های دارای تدریس (packId) در لیست‌اند — جلسه‌ها/ستایش/نیایش/… حذف؛
 *  - برای هر درس: وضعیت PDF و تک‌تک بخش‌های صوت؛
 *  - دو دکمه‌ی جدا در سطح کتاب: «دانلود همه‌ی PDFها» و «دانلود همه‌ی صوت‌ها»؛
 *  - حجم: «۱۸ مگابایت از ۱۲۰ مگابایت» (مجموع حجم واقعی فایل‌های سرور با HEAD)؛
 *  - ارقام با فونت جدولی (tnum) تا با تغییر عدد، متن نلرزد.
 */

private fun pdfCacheFile(ctx: android.content.Context, fileId: String): File =
    StudyPdfCache.file(ctx, fileId)

private fun pdfCached(ctx: android.content.Context, fileId: String): Boolean =
    StudyPdfCache.isValid(pdfCacheFile(ctx, fileId))

/** ارقام با عرض ثابت (۴ رقم، مکمل صفر) — با تغییر عدد، کل متن جابه‌جا نمی‌شود. */
private fun fixNum(n: Int): String = toPersianDigits(n.toString()).padStart(4, '۰')

/** درصد با عرض ثابت ۳ رقمی (بیشینه‌ی ۱۰۰). */
private fun fixPct(n: Int): String = toPersianDigits(n.toString()).padStart(3, '۰')

/** مثال: ۰۰۱۸ مگابایت از ۰۱۲۰ مگابایت */
private fun mbFixed(bytes: Long): String =
    fixNum((bytes / (1024.0 * 1024.0)).roundToInt()).let { "$it مگابایت" }

/** ارقام جدولی — عرض ثابت تا تغییر عدد، متن را نلرزاند. */
private val numStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")

/** حجم فایل سرور با درخواست HEAD — برای «X از Y مگابایت» (کش در حافظه‌ی پروسه). */
private val remoteSizeCache = mutableMapOf<String, Long>()

private fun headSizeBlocking(fileId: String): Long {
    remoteSizeCache[fileId]?.let { return it }
    val remoteId = StudyMedia.resolveFileId(fileId)
    return try {
        var found = -1L
        for (url in StudyMedia.candidateUrls(remoteId)) {
            val len = runCatching {
                val conn = ResilientHttp.open(url, connectMs = 10000, readMs = 10000, attempts = 3)
                try { if (conn.responseCode in 200..299) conn.contentLengthLong else -1L }
                finally { conn.disconnect() }
            }.getOrDefault(-1L)
            if (len > 0) { found = len; break }
        }
        // -2 یعنی «سرور ندارد/ناموفق» — همیشه کش می‌شود تا پروب بی‌نهایت نشود
        val cached = if (found > 0) found else -2L
        remoteSizeCache[fileId] = cached
        cached
    } catch (e: Exception) {
        remoteSizeCache[fileId] = -2L
        -2L
    }
}

/** دانلود PDF به کش مشترک همهٔ صفحه‌ها؛ resume/fallback/اعتبارسنجی متمرکز است. */
private fun downloadPdfBlocking(ctx: android.content.Context, fileId: String, onProgress: (Int) -> Unit) {
    val target = StudyPdfCache.obtain(ctx, fileId, onProgress)
    remoteSizeCache[fileId] = target.length()
}

/**
 * تمام متن‌های این صفحه ۴ واحد کوچک‌تر و با «وزیرمتن لایت» نوشته می‌شوند —
 * فونت دست‌نویس سراسری اپ (badkhat) اینجا جای خود را به فونت خوانا می‌دهد.
 */
@Composable
private fun DownloadsTypography(content: @Composable () -> Unit) {
    val vazir = remember { FontFamily(Font(R.font.vazirmatn_light, FontWeight.Light)) }
    val base = MaterialTheme.typography
    fun TextStyle.tune() = copy(
        fontFamily = vazir,
        fontWeight = FontWeight.Light,
        // ۱٫۵ برابرِ قبلی بود؛ حالا همان منهای ۴ واحد.
        fontSize = (fontSize.value * 1.5f - 4f).coerceAtLeast(9f).sp,
        lineHeight = if (lineHeight.isSpecified) (lineHeight.value * 1.5f - 4f).coerceAtLeast(12f).sp else lineHeight,
    )
    MaterialTheme(
        typography = base.copy(
            displayLarge = base.displayLarge.tune(), displayMedium = base.displayMedium.tune(),
            displaySmall = base.displaySmall.tune(), headlineLarge = base.headlineLarge.tune(),
            headlineMedium = base.headlineMedium.tune(), headlineSmall = base.headlineSmall.tune(),
            titleLarge = base.titleLarge.tune(), titleMedium = base.titleMedium.tune(),
            titleSmall = base.titleSmall.tune(), bodyLarge = base.bodyLarge.tune(),
            bodyMedium = base.bodyMedium.tune(), bodySmall = base.bodySmall.tune(),
            labelLarge = base.labelLarge.tune(), labelMedium = base.labelMedium.tune(),
            labelSmall = base.labelSmall.tune(),
        ),
        content = content,
    )
}

@Composable
fun DownloadsScreen(onBack: () -> Unit) = DownloadsTypography {
    DownloadsScreenBody(onBack)
}

@Composable
private fun DownloadsScreenBody(onBack: () -> Unit) {
    // ترفند fontScale حذف شد؛ اندازه‌ها مستقیم در DownloadsTypography تعیین می‌شوند.
    DownloadsScreenInner(onBack)
}

@Composable
private fun DownloadsScreenInner(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx, "hamyar_downloads") }
    // v1.19: آکاردئون کتاب‌ها — فقط یک کتاب باز؛ حافظه‌دار
    var openBook by remember { mutableStateOf(store.getString("dl_openbook", "")) }
    fun toggleBook(code: String) {
        openBook = if (openBook == code) "" else code
        store.putString("dl_openbook", openBook)
    }
    val books = remember { com.hamyareman.ir.ui.profile.GradeGate.filter(BookModuleRegistry.modules) { it.bookCode } }
    val scope = rememberCoroutineScope()
    val busy = remember { mutableStateMapOf<String, Int>() }   // key → درصد
    val netErr = remember { mutableStateMapOf<String, Boolean>() } // key → خطای شبکه
    var tick by remember { mutableIntStateOf(0) }

    suspend fun dl(fileId: String, cacheKey: String, key: String, asPdf: Boolean) {
        if (busy.containsKey(key)) return
        netErr.remove(key)
        busy[key] = 0
        try {
            withContext(Dispatchers.IO) {
                if (asPdf) downloadPdfBlocking(ctx, fileId) { busy[key] = it }
                else {
                    val remoteId = StudyMedia.resolveFileId(fileId)
                    MediaVault.downloadEncrypted(ctx, StudyMedia.candidateUrls(remoteId), cacheKey) { p, t -> busy[key] = if (t > 0) ((p * 100) / t).toInt() else 0 }
                }
            }
            tick++
            // محتوای دانلودشده «امضا» می‌شود تا اگر بعداً روی سرور عوض شد،
            // همین صفحه بفهمد و فقط همان فایل را دوباره بگیرد (کانالِ محتواییِ
            // آپدیت، بدونِ APK).
            runCatching {
                MediaFreshness.rememberDownload(ctx, key, fileId, cacheKey, asPdf)
            }
        } catch (e: Exception) {
            netErr[key] = true
        } finally {
            busy.remove(key)
        }
    }

    // --- به‌روزرسانیِ محتوا: فقط فایل‌هایی که روی سرور عوض شده‌اند ---
    var mediaCheck by remember { mutableStateOf<MediaFreshness.Check?>(null) }
    var mediaBusy by remember { mutableStateOf(false) }

    fun runMediaCheck() {
        if (mediaBusy) return
        mediaBusy = true
        scope.launch {
            mediaCheck = MediaFreshness.findStale(ctx)
            mediaBusy = false
        }
    }

    /** پاک‌کردنِ نسخهٔ کهنه و گرفتنِ نسخهٔ تازه — فقط برای فایل‌های تغییریافته. */
    fun refreshStale(items: List<MediaFreshness.Item>) {
        scope.launch {
            for (item in items) {
                if (!isActive) break
                withContext(Dispatchers.IO) {
                    if (item.isPdf) pdfCacheFile(ctx, item.fileId).delete()
                    else MediaVault.delete(ctx, item.cacheKey)
                }
                dl(item.fileId, item.cacheKey, item.key, item.isPdf)
            }
            tick++
            mediaCheck = MediaFreshness.findStale(ctx)
        }
    }

    // بررسیِ خودکار در پس‌زمینه، اگر نتیجهٔ قبلی کهنه است (شش ساعت).
    LaunchedEffect(Unit) {
        if (!MediaFreshness.isFresh(ctx)) runMediaCheck()
    }

    fun download(moduleFiles: List<Quadruple>) {
        scope.launch {
            for (q in moduleFiles) {
                if (!isActive) break
                dl(q.fileId, q.cacheKey, q.statusKey, q.isPdf)
            }
        }
    }

    AppTopBar("مدیریت دانلود کتاب‌ها", onBack)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item {
            MediaUpdateCard(
                check = mediaCheck,
                busy = mediaBusy,
                lastCheckAt = MediaFreshness.lastCheckAt(ctx),
                onCheck = { runMediaCheck() },
                onUpdate = { items -> refreshStale(items) },
            )
        }
        items(books.size) { i ->
            BookDlCard(
                module = books[i],
                store = store,
                busy = busy,
                netErr = netErr,
                tick = tick,
                expanded = openBook == books[i].bookCode,
                onToggleBook = { toggleBook(books[i].bookCode) },
                onDownload = { files -> download(files) },
                // پروب حجم فایل‌های گم‌شده (یک‌بار، در پس‌زمینه)
                onProbe = { ids ->
                    scope.launch {
                        withContext(Dispatchers.IO) { ids.forEach { headSizeBlocking(it) } }
                        tick++
                    }
                },
                onRefresh = { tick++ },
            )
        }
    }
}

private data class Quadruple(val statusKey: String, val fileId: String, val cacheKey: String, val isPdf: Boolean)

@Composable
private fun BookDlCard(
    module: com.hamyareman.ir.platform.feature.study.BookModule,
    store: LocalStore,
    busy: Map<String, Int>,
    netErr: Map<String, Boolean>,
    tick: Int,
    expanded: Boolean,
    onToggleBook: () -> Unit,
    onDownload: (List<Quadruple>) -> Unit,
    onProbe: (List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val ctx = LocalContext.current
    val cover = remember(module.bookCode) {
        runCatching { BitmapFactory.decodeStream(ctx.assets.open("book-covers/${module.bookCode}.jpg")) }.getOrNull()
    }
    val toc = remember(module.bookCode) { BookToc.forBook(module.bookCode) }

    data class KindStat(val total: Int, val done: Int, val missing: List<Quadruple>)

    fun statOf(kind: String?): KindStat {
        val all = mutableListOf<Quadruple>()
        val doneFlags = mutableListOf<Boolean>()
        fun walk(n: TocNode) {
            n.packId?.let { pid ->
                BookModuleRegistry.pack(pid)?.let { pack ->
                    if (kind != "audio" && pack.pdfFileName.isNotBlank()) {
                        all.add(Quadruple("$pid:PDF", pack.pdfFileName, "", true))
                        doneFlags.add(pdfCached(ctx, pack.pdfFileName))
                    }
                    if (kind != "pdf") teachTracksOf(pack).forEach { t ->
                        all.add(Quadruple("$pid:${t.cacheKey}", t.fileId, t.cacheKey, false))
                        doneFlags.add(MediaVault.isCached(ctx, t.cacheKey))
                    }
                }
            }
            n.children.forEach(::walk)
        }
        toc.forEach(::walk)
        val done = doneFlags.count { it }
        return KindStat(all.size, done, all.filterIndexed { i, _ -> !doneFlags[i] })
    }

    // v1.16: آمار تفکیک PDF/صوت — هر نوع با نوار و دکمه‌ی خودش
    val pdfStat = remember(module.bookCode, tick) { statOf("pdf") }
    val audioStat = remember(module.bookCode, tick) { statOf("audio") }
    val allStat = remember(module.bookCode, tick) { statOf(null) }

    // حجم محلی دانلودشده + حجم کل سرور (فایل‌های گم‌شده با HEAD)
    val downloadedBytes = remember(module.bookCode, tick) {
        var sum = 0L
        fun walk(n: TocNode) {
            n.packId?.let { pid ->
                BookModuleRegistry.pack(pid)?.let { pack ->
                    if (pack.pdfFileName.isNotBlank()) {
                        pdfCacheFile(ctx, pack.pdfFileName).takeIf { it.exists() }?.let { sum += it.length() }
                    }
                    teachTracksOf(pack).forEach { t ->
                        if (MediaVault.isCached(ctx, t.cacheKey)) sum += MediaVault.vaultFile(ctx, t.cacheKey).length()
                    }
                }
            }
            n.children.forEach(::walk)
        }
        toc.forEach(::walk)
        sum
    }
    val totalBytes = remember(module.bookCode, tick) {
        var sum = downloadedBytes
        val probe = mutableListOf<String>()
        fun walk(n: TocNode) {
            n.packId?.let { pid ->
                BookModuleRegistry.pack(pid)?.let { pack ->
                    if (pack.pdfFileName.isNotBlank() && !pdfCached(ctx, pack.pdfFileName)) {
                        when (val s = remoteSizeCache[pack.pdfFileName]) {
                            null -> probe.add(pack.pdfFileName)
                            else -> if (s > 0) sum += s
                        }
                    }
                    teachTracksOf(pack).forEach { t ->
                        if (!MediaVault.isCached(ctx, t.cacheKey)) {
                            when (val s = remoteSizeCache[t.fileId]) {
                                null -> probe.add(t.fileId)
                                else -> if (s > 0) sum += s
                            }
                        }
                    }
                }
            }
            n.children.forEach(::walk)
        }
        toc.forEach(::walk)
        if (probe.isNotEmpty()) onProbe(probe)
        sum
    }

    val anyBusy = busy.keys.any { k ->
        val pid = k.substringBefore(":")
        module.packs.any { it.packId == pid }
    }
    // (۱۳) حذف فایل فقط با دیالوگ تایید — هر کتاب دیالوگ خودش را دارد
    var pendingDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    // آکاردئون فصل‌ها — هر لحظه فقط یک فصل باز؛ حافظه‌دار؛ پیش‌فرض همه جمع.
    var openSection by remember(module.bookCode) {
        mutableStateOf(store.getString("dl_acc_${module.bookCode}", ""))
    }
    fun toggle(id: String) {
        openSection = if (openSection == id) "" else id
        store.putString("dl_acc_${module.bookCode}", openSection)
    }

    // v1.19: کارت کتاب = آکاردئون — سربرگ همیشه دیده می‌شود؛ محتوا فقط کتابِ باز
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().androidClickable(onClick = onToggleBook),
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.width(52.dp).height(70.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(module.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${mbFixed(downloadedBytes)} از ${mbFixed(totalBytes)}",
                        style = numStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (!expanded) return@Column
            Spacer(Modifier.height(8.dp))
            // نوار وضعیت PDF — تفکیک از صوت
            KindBar(
                label = "PDF",
                done = pdfStat.done,
                total = pdfStat.total,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            // نوار وضعیت صوت — تفکیک از PDF
            KindBar(
                label = "صوت",
                done = audioStat.done,
                total = audioStat.total,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onDownload(pdfStat.missing) },
                    enabled = !anyBusy && pdfStat.missing.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDFها (${fixNum(pdfStat.missing.size)})", style = numStyle, maxLines = 1)
                }
                Button(
                    onClick = { onDownload(audioStat.missing) },
                    enabled = !anyBusy && audioStat.missing.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("صوت‌ها (${fixNum(audioStat.missing.size)})", style = numStyle, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            val hasSections = toc.any { it.packId == null && it.children.isNotEmpty() }
            if (hasSections) {
                // فصل‌ها: آکاردئون «فقط یکی باز»
                toc.forEach { node ->
                    DlNode(node, 0, openId = openSection, onToggle = ::toggle, store = store, busy = busy, netErr = netErr, onDownload = onDownload, tick = tick, onDelete = { label, onYes ->
                        pendingDelete = label to onYes
                    })
                }
            } else {
                // v1.18: کتاب بدون فصل (قرآن/عربی/…) — فهرست درس‌ها داخل یک گروه جمع‌شونده
                val listOpen = openSection == "lessons"
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { toggle("lessons") }.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (listOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("فهرست درس‌ها (${fixNum(toc.count { it.packId != null })})", style = numStyle, fontWeight = FontWeight.Bold)
                    }
                }
                if (listOpen) {
                    toc.forEach { node ->
                        DlNode(node, 0, openId = "", onToggle = {}, store = store, busy = busy, netErr = netErr, onDownload = onDownload, tick = tick, onDelete = { label, onYes ->
                            pendingDelete = label to onYes
                        })
                    }
                }
            }
        }
    }
    // v1.19: به‌جای پاپ‌آپ — منوی پایین صفحه
    pendingDelete?.let { (label, onYes) ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { pendingDelete = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("حذف فایل دانلودشده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("«$label» از حافظه‌ی دستگاه حذف شود؟", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onYes(); onRefresh(); pendingDelete = null }) { Text("حذف") }
                    OutlinedButton(onClick = { pendingDelete = null }) { Text("انصراف") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** پیمایش فهرست رسمی — فصل‌ها آکاردئونی؛ فقط ردیف‌های دارای تدریس. */
private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

/** یک نوار وضعیت برای یک نوع فایل (PDF یا صوت): «PDF · ۰۰۰۳ از ۰۰۱۲» + نوار. */
@Composable
private fun KindBar(label: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$label · ${fixNum(done)} از ${fixNum(total)}",
                style = numStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 1f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun DlNode(
    node: TocNode,
    depth: Int,
    openId: String,
    onToggle: (String) -> Unit,
    store: LocalStore,
    busy: Map<String, Int>,
    netErr: Map<String, Boolean>,
    onDownload: (List<Quadruple>) -> Unit,
    tick: Int,
    onDelete: (String, () -> Unit) -> Unit,
) {
    when {
        node.packId == null && node.children.isNotEmpty() -> {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 8).dp, top = 3.dp, bottom = 3.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(node.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (openId == node.id) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        node.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        else -> {
            // smart-cast کراس‌ماژول ممکن نیست — با val محلی
            val pid = node.packId
            val pack = pid?.let { remember(it) { BookModuleRegistry.pack(it) } }
            if (pack != null) DlLessonRow(pack, depth + 1, store, busy, netErr, onDownload, tick, onDelete)
        }
    }
    if (node.children.isNotEmpty() && (node.packId != null || openId == node.id)) {
        node.children.forEach { child ->
            DlNode(child, depth + 1, openId = openId, onToggle = onToggle, store = store, busy = busy, netErr = netErr, onDownload = onDownload, tick = tick, onDelete = onDelete)
        }
    }
}

@Composable
private fun DlLessonRow(
    pack: StudyPack,
    depth: Int,
    store: LocalStore,
    busy: Map<String, Int>,
    netErr: Map<String, Boolean>,
    onDownload: (List<Quadruple>) -> Unit,
    tick: Int,
    onDelete: (String, () -> Unit) -> Unit,
) {
    val ctx = LocalContext.current
    val tracks = remember(pack.packId) { teachTracksOf(pack) }

    data class Chip(val label: String, val done: Boolean, val is404: Boolean, val isNetErr: Boolean, val isBusy: Boolean, val pct: Int, val fileId: String, val cacheKey: String, val isPdf: Boolean)

    @Composable
    fun chipOf(isPdf: Boolean, track: TeachTrack?): Chip {
        val fileId = if (isPdf) pack.pdfFileName else track!!.fileId
        val key = if (isPdf) "${pack.packId}:PDF" else "${pack.packId}:${track!!.cacheKey}"
        val done = if (isPdf) pdfCached(ctx, fileId) else MediaVault.isCached(ctx, track!!.cacheKey)
        val is404 = store.getString("dl404_$fileId", "0") == "1"
        val pct = busy[key]
        return Chip(
            label = if (isPdf) "PDF" else track!!.label,
            done = done,
            is404 = is404,
            isNetErr = netErr[key] == true,
            isBusy = pct != null,
            pct = pct ?: 0,
            fileId = fileId,
            cacheKey = if (isPdf) "" else track!!.cacheKey,
            isPdf = isPdf,
        )
    }

    @Composable
    fun ChipView(c: Chip, onClick: (Chip) -> Unit) {
        val (icon, tint) = when {
            c.isBusy -> Icons.Outlined.Schedule to MaterialTheme.colorScheme.primary
            c.done -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.tertiary
            c.is404 -> Icons.Outlined.CloudOff to MaterialTheme.colorScheme.outline
            c.isNetErr -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
            else -> Icons.Outlined.Download to MaterialTheme.colorScheme.onSurfaceVariant
        }
        val label = when {
            c.isBusy -> "در حال دانلود ${fixPct(c.pct)}٪"
            c.done -> "دانلود شده"
            c.is404 -> "فایل روی سرور نیست"
            c.isNetErr -> "اینترنت را بررسی کن"
            else -> "دانلود نشده"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = !c.isBusy) { onClick(c) }
                .padding(2.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                if (c.isBusy) label else "${c.label} · $label",
                style = numStyle,
                color = tint,
                maxLines = 1,
            )
        }
    }

    fun clickChip(c: Chip) {
        if (c.done) {
            onDelete(c.label) {
                if (c.isPdf) {
                    pdfCacheFile(ctx, c.fileId).delete()
                    store.putString("dl404_${c.fileId}", "0")
                } else {
                    MediaVault.delete(ctx, c.cacheKey)
                }
            }
        } else {
            onDownload(listOf(Quadruple(if (c.isPdf) "${pack.packId}:PDF" else "${pack.packId}:${c.cacheKey}", c.fileId, c.cacheKey, c.isPdf)))
        }
    }

    val anyBusy = busy.keys.any { it.startsWith("${pack.packId}:") }
    // v1.16: سطر اول = عنوان درس (کلیک = دانلود همه‌ی ناقص‌ها)؛
    // سطر دوم = نمایشگر وضعیت PDF و تک‌تک صوت‌ها.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 8).dp, top = 4.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !anyBusy) {
                    val files = mutableListOf<Quadruple>()
                    if (pack.pdfFileName.isNotBlank() && !pdfCached(ctx, pack.pdfFileName)) {
                        files.add(Quadruple("${pack.packId}:PDF", pack.pdfFileName, "", true))
                    }
                    tracks.forEach { t ->
                        if (!MediaVault.isCached(ctx, t.cacheKey)) files.add(Quadruple("${pack.packId}:${t.cacheKey}", t.fileId, t.cacheKey, false))
                    }
                    if (files.isNotEmpty()) onDownload(files)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(pack.title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pack.pdfFileName.isNotBlank()) {
                ChipView(chipOf(true, null)) { clickChip(it) }
            }
            tracks.forEach { t ->
                Spacer(Modifier.width(10.dp))
                ChipView(chipOf(false, t)) { clickChip(it) }
            }
        }
    }
}

/**
 * کارتِ «به‌روزرسانی محتوا» — کانالِ محتواییِ آپدیت:
 *
 * فایل‌های صوتی/PDFِ تدریس روی سرور می‌توانند عوض شوند، در حالی که نسخه‌ی
 * دانلودشده روی گوشی همان قدیمی می‌ماند. این کارت اثرِ انگشتِ محتوای سرور را با
 * اثرِ انگشتی که هنگامِ دانلود ثبت شده مقایسه می‌کند و **فقط فایل‌های
 * تغییریافته** را با دکمه‌ی «به‌روزرسانی» دوباره می‌گیرد.
 */
@Composable
private fun MediaUpdateCard(
    check: MediaFreshness.Check?,
    busy: Boolean,
    lastCheckAt: Long,
    onCheck: () -> Unit,
    onUpdate: (List<MediaFreshness.Item>) -> Unit,
) {
    val stale = check?.stale.orEmpty()
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🔄 به‌روزرسانی محتوا", style = MaterialTheme.typography.titleMedium)

            val body = when {
                busy -> "در حال بررسی…"
                check == null && lastCheckAt <= 0L ->
                    "ببین کدام فایلِ دانلودشده روی سرور تازه‌تر شده؛ فقط همان‌ها دوباره گرفته می‌شوند."
                check == null ->
                    "آخرین بررسی: ${agoLabel(lastCheckAt)}"
                stale.isEmpty() && check.failed == 0 ->
                    "همه‌چیز به‌روز است ✅  (${toPersianDigits(check.checked.toString())} فایل بررسی شد)"
                else -> buildString {
                    if (stale.isNotEmpty()) {
                        append(toPersianDigits(stale.size.toString()))
                        append(" فایل روی سرور تازه‌تر شده: ")
                        append(stale.take(4).joinToString("، ") { it.fileId })
                        if (stale.size > 4) append(" و …")
                    }
                    if (check.failed > 0) {
                        if (isNotEmpty()) append("\n")
                        append(toPersianDigits(check.failed.toString()))
                        append(" فایل بررسی نشد (اینترنت را چک کن).")
                    }
                    if (check.checked == 0) append("هنوز چیزی دانلود نکرده‌ای.")
                }
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy, onClick = onCheck) {
                    Text(if (busy) "در حال بررسی…" else "بررسی")
                }
                if (stale.isNotEmpty()) {
                    Button(enabled = !busy, onClick = { onUpdate(stale) }) {
                        Text("به‌روزرسانی ${toPersianDigits(stale.size.toString())} فایل")
                    }
                }
            }
        }
    }
}

/** «۲ دقیقه پیش» / «۳ ساعت پیش» / «۵ روز پیش» — برای خطِ آخرین بررسی. */
private fun agoLabel(at: Long): String {
    if (at <= 0L) return "—"
    val d = System.currentTimeMillis() - at
    val min = d / 60_000
    return when {
        min < 1 -> "همین حالا"
        min < 60 -> toPersianDigits(min.toString()) + " دقیقه پیش"
        min < 24 * 60 -> toPersianDigits((min / 60).toString()) + " ساعت پیش"
        else -> toPersianDigits((min / (24 * 60)).toString()) + " روز پیش"
    }
}
