package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.MathAnswerScript
import com.hamyareman.ir.platform.feature.study.MathExamLedger
import com.hamyareman.ir.platform.feature.study.StudyPack
import kotlinx.coroutines.delay

internal data class MathTab(val key: String, val label: String)

/**
 * سربرگ درس ریاضی.
 * درس عادی: تدریس / تمرینات کتابی / خلاصه — فلش و آزمون فقط در جمع‌بندی فصل.
 * جمع‌بندی: تدریس / فلش‌کارت / خلاصه / آزمون — بدون تمرینات کتابی.
 */
@Composable
fun MathLessonScreen(
    packId: String,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    SecureWebEffect()
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val bookTitle = remember(packId) {
        BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == packId } }?.title.orEmpty()
    }
    if (pack == null) {
        AppTopBar(title = "درس ریاضی", onBack = onBack)
        Text("این درس پیدا نشد.", Modifier.padding(16.dp))
        return
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val html = remember(packId) { MathHtmlAssets.of(packId) }
    val isSum = html?.isSum == true || pack.lessonId.contains("SUM")
    val chapter = html?.chapter ?: Regex("""E(\d+)""").find(pack.packId)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    val hasPdf = pack.pdfFileName.isNotBlank()
    val tabs = remember(isSum, hasPdf) {
        val base = if (isSum) listOf(
            MathTab("teach", "تدریس"),
            MathTab("flash", "فلش‌کارت"),
            MathTab("summary", "خلاصه"),
            MathTab("exam", "آزمون"),
        ) else listOf(
            MathTab("teach", "تدریس"),
            MathTab("book", "تمرینات کتابی"),
            MathTab("summary", "خلاصه"),
        )
        if (hasPdf) base + MathTab("pdf", "کتاب درسی") else base
    }
    val startTab = if (pack.pdfOnly) {
        tabs.indexOfFirst { it.key == "pdf" }.takeIf { it >= 0 } ?: initialTab
    } else {
        initialTab
    }
    var tab by rememberSaveable(packId, isSum) {
        mutableIntStateOf(startTab.coerceIn(0, tabs.lastIndex))
    }
    if (tab > tabs.lastIndex) tab = 0
    val chromeStore = remember { com.hamyareman.ir.platform.core.common.LocalStore(ctx, "hamyar_math_ui") }
    var autoHide by rememberSaveable(packId) { mutableStateOf(chromeStore.getBool("autohide_$packId", true)) }
    var chromeHidden by remember { mutableStateOf(false) }
    var hideGen by remember { mutableIntStateOf(0) }

    LaunchedEffect(autoHide, chromeHidden, hideGen, tab) {
        if (!autoHide) {
            chromeHidden = false
            return@LaunchedEffect
        }
        if (chromeHidden) return@LaunchedEffect
        delay(5000)
        chromeHidden = true
    }

    // فقط «فهرست» کتاب، صفحه‌ی ساده‌ی PDF می‌ماند؛ همه‌ی درس‌ها و جمع‌بندیِ فصل‌ها
    // پلیرِ صوت + سربرگ‌ها را دارند (حتی اگر صوتشان هنوز روی سرور نباشد).
    if (pack.pdfOnly || pack.lessonId == "TOC" || pack.packId == "C905_TOC") {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = pack.title, onBack = onBack)
            TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        val tracksForBar = teachTracksOf(pack)
        if (!chromeHidden) {
            AppTopBar(title = pack.title, onBack = onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = autoHide,
                    onCheckedChange = {
                        autoHide = it
                        chromeStore.putBool("autohide_$packId", it)
                        if (!it) chromeHidden = false else hideGen++
                    },
                )
                Text("جمع شود", style = MaterialTheme.typography.labelMedium)
            }
        }
        // ۴) کارتِ پلیر «بالای سربرگ‌ها» و برای همه‌ی سربرگ‌ها.
        // نکته: کارت همیشه در ترکیب می‌ماند (فقط ارتفاعش صفر می‌شود) تا با جمع‌شدنِ
        // نوار، صوتِ در حالِ پخش قطع نشود.
        if (tracksForBar.isNotEmpty()) {
            Box(
                Modifier.then(
                    if (chromeHidden) Modifier.height(0.dp).clipToBounds() else Modifier,
                ),
            ) {
                TeachAudioBar(
                    packId = pack.packId,
                    screenTitle = pack.title,
                    bookTitle = bookTitle,
                    tracks = tracksForBar,
                )
            }
        }
        // ۷) سربرگ‌ها هرگز جمع نمی‌شوند — همیشه بالای محتوا و بالای فلش، قابلِ انتخاب.
        MathChromeTabRow(tabs, tab) { tab = it }
        if (chromeHidden) {
            // ۶) فلشِ بازکننده: وسطِ صفحه، بزرگ‌تر، با «نفس» آرام + سایه و حلقه.
            val breath = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(Unit) {
                while (true) {
                    breath.animateTo(
                        1.10f,
                        animationSpec = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    )
                    breath.animateTo(0.96f, animationSpec = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = {
                        chromeHidden = false
                        hideGen++
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { scaleX = breath.value; scaleY = breath.value },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ExpandMore, contentDescription = "باز کردن سربرگ‌ها", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        val currentKey = tabs.getOrNull(tab)?.key ?: "teach"
        val currentLabel = tabs.getOrNull(tab)?.label ?: "تدریس"
        DisposableEffect(pack.packId, currentKey) {
            val start = System.currentTimeMillis()
            StudyActivity.add(ctx, pack.packId, "tab", "باز کردن سربرگ $currentLabel")
            onDispose {
                val sec = ((System.currentTimeMillis() - start) / 1000L).toInt()
                if (sec >= 2) {
                    StudyActivity.add(ctx, pack.packId, "dwell", "سربرگ $currentLabel — ${sec} ثانیه")
                }
            }
        }
        // زوم مالِ WebView/PDF است. سوایپِ سربرگ روی همان لایه پینچ را می‌دزدید و
        // هنگ می‌ساخت — عوض‌کردن سربرگ فقط با خودِ سربرگ‌ها.
        Box(Modifier.weight(1f).fillMaxSize()) {
            when (currentKey) {
                "teach" -> MathTeachTab(pack, bookTitle, showPlayer = false)
                "book" -> MathBookHtmlTab(pack, html)
                "flash" -> MathFlashHtmlTab(pack, html)
                "summary" -> MathSummaryTab(pack, isSum = isSum, chapter = chapter)
                "exam" -> MathExamHtmlTab(pack, html)
                "pdf" -> TeachPdfPages(
                    modifier = Modifier.fillMaxSize(),
                    fileId = pack.pdfFileName,
                    pack = pack,
                )
                else -> MathTeachTab(pack, bookTitle, showPlayer = false)
            }
        }
    }
}

/**
 * افکتِ جهت‌دارِ سوایپِ سربرگ‌ها: نورِ لبه‌ی مقصد + فلش و برچسبِ سربرگِ مقصد که
 * با پیشرفتِ درگ روشن/بزرگ می‌شود. خواندنِ «progress» داخلِ لایه‌ی گرافیکی است تا
 * درگ باعثِ بازترکیب نشود.
 */
@Composable
private fun TabSwipeHint(destLeft: Boolean, label: String?, progress: () -> Float) {
    val tint = MaterialTheme.colorScheme.primary
    // «چپ/راستِ مطلق» با Start/End ساخته می‌شود: در RTL جایِ این دو عوض می‌شود.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val side = if (destLeft == rtl) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(side)
                .fillMaxHeight()
                .width(110.dp)
                .graphicsLayer { alpha = progress() * 0.85f }
                .background(
                    Brush.horizontalGradient(
                        if (destLeft) {
                            listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0f))
                        } else {
                            listOf(tint.copy(alpha = 0f), tint.copy(alpha = 0.55f))
                        },
                    ),
                ),
        )
        if (label != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(side)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer {
                        val p = progress()
                        alpha = p
                        translationX = (if (destLeft) -1f else 1f) * (1f - p) * 30.dp.toPx()
                        scaleX = 0.8f + 0.2f * p
                        scaleY = 0.8f + 0.2f * p
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        if (destLeft) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


/** سربرگ‌ها — در هر دو حالتِ جمع/باز یکی؛ هیچ‌وقت جمع نمی‌شوند. */
@Composable
private fun MathChromeTabRow(tabs: List<MathTab>, tab: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(selectedTabIndex = tab.coerceIn(0, tabs.lastIndex), edgePadding = 8.dp) {
        tabs.forEachIndexed { i, t ->
            Tab(
                selected = tab == i,
                onClick = { onSelect(i) },
                text = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

@Composable
private fun MathBookHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?, onZoomChanged: (Boolean) -> Unit = {}) {
    val asset = html?.bookAsset
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if (MathHtmlAssets.exists(ctx, asset)) {
        MathInteractiveHtml(
            packId = pack.packId,
            kind = "book",
            assetPath = asset.orEmpty(),
            modifier = Modifier.fillMaxSize(),
            onZoomChanged = onZoomChanged,
        )
    } else {
        MathStudyTab(pack)
    }
}

@Composable
private fun MathFlashHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?, onZoomChanged: (Boolean) -> Unit = {}) {
    val asset = html?.flashAsset
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if (MathHtmlAssets.exists(ctx, asset)) {
        MathInteractiveHtml(
            packId = pack.packId,
            kind = "flash",
            assetPath = asset.orEmpty(),
            modifier = Modifier.fillMaxSize(),
            onZoomChanged = onZoomChanged,
        )
    } else {
        MathFlashTab(pack)
    }
}

@Composable
private fun MathExamHtmlTab(pack: StudyPack, html: MathHtmlAssets.Spec?, onZoomChanged: (Boolean) -> Unit = {}) {
    val asset = html?.examAsset
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if (MathHtmlAssets.exists(ctx, asset)) {
        MathInteractiveHtml(
            packId = pack.packId,
            kind = "exam",
            assetPath = asset.orEmpty(),
            modifier = Modifier.fillMaxSize(),
            onZoomChanged = onZoomChanged,
        )
    } else {
        MathExamTab(pack)
    }
}

@Composable
private fun MathTeachTab(pack: StudyPack, bookTitle: String, showPlayer: Boolean = true, onZoomChanged: (Boolean) -> Unit = {}) {
    val tracks = teachTracksOf(pack)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val container = LocalAppContainer.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    ManagedWebMediaEffect { webRef[0] }
    var remoteHtml by remember(pack.packId) { mutableStateOf<String?>(null) }
    var remoteTried by remember(pack.packId) { mutableStateOf(false) }
    LaunchedEffect(pack.packId) {
        val fid = MathHtmlAssets.teachAsset(pack.packId)?.substringAfterLast('/')
        if (fid.isNullOrBlank()) {
            remoteTried = true
            return@LaunchedEffect
        }
        remoteHtml = withContext(Dispatchers.IO) {
            try { HtmlMediaKey.fetch(ctx, container.tables) } catch (_: Throwable) {}
            runCatching {
                if (!MediaVault.isVerified(ctx, fid)) {
                    MediaVault.downloadEncrypted(ctx, StudyMedia.candidateUrls(fid), fid) { _, _ -> }
                    if (MediaVault.isVerified(ctx, fid)) {
                        MediaFreshness.rememberDownload(ctx, "html:$fid", fid, fid, isPdf = false)
                    }
                }
                String(MediaVault.decryptToMemory(ctx, fid), Charsets.UTF_8)
            }.getOrNull()
        }
        remoteTried = true
    }
    // فقط سرور رمزشده — HTML داخل APK نیست.
    val teachHtml = ensureSeekShim(
        pack.teachHtml.ifBlank { remoteHtml.orEmpty() },
        TeachSeekMap.times(pack.packId),
    )
    val body = pack.teachText.ifBlank {
        pack.sections.filter { it.kind != "exam" }.joinToString("\n\n") { "«${it.title}»\n${it.body}" }
            .ifBlank { "متن تدریس این درس به‌زودی از پوشهٔ Books اضافه می‌شود." }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showPlayer && tracks.isNotEmpty()) {
            TeachAudioBar(packId = pack.packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)
        }
        Card(Modifier.fillMaxWidth().weight(1f)) {
            if (!remoteTried && teachHtml.isBlank()) {
                HtmlPercentLoader(35)
            } else if (teachHtml.isNotBlank()) {
                AndroidView(
                    factory = { c ->
                        WebView(c).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String) {
                                    view.bindManagedMediaLifecycle()
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = false
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            addJavascriptInterface(TeachHtmlBridge(), "HamyarPlayer")
                            installManagedMediaLifecycle()
                            webRef[0] = this
                            setBackgroundColor(android.graphics.Color.WHITE)
                            setOnTouchListener { v, e ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                        }
                    },
                    update = { wv ->
                        val tag = teachHtml.hashCode()
                        if (wv.tag != tag) {
                            wv.tag = tag
                            wv.loadDataWithBaseURL(
                                "https://local.hamyar/",
                                teachHtml,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    onRelease = {
                        it.stopManagedMedia()
                        if (webRef[0] === it) webRef[0] = null
                        it.destroy()
                    },
                )
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text("متن تدریس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MathStudyTab(pack: StudyPack) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
        MathExercisesPane(pack, Modifier.weight(1f))
    }
}

@Composable
private fun MathExercisesPane(pack: StudyPack, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val today = remember { JalaliDate.todayIso() }
    val ime = LocalSoftwareKeyboardController.current
    val bookQs = pack.questions.filter { it.topic == "book" && it.type == "mcq" }
    if (pack.exercises.isNotEmpty()) {
        /* جای‌خالی + کیبورد نماد — فقط وقتی تمرین تایپی در پک باشد */
    } else if (bookQs.isNotEmpty()) {
        MathBookMcqPane(pack, bookQs, modifier)
        return
    } else {
        Card(modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("تمرین‌های کتاب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ساختار تمرین آماده است؛ فایل HTML این درس هنوز در پوشهٔ Books نیست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    var idx by rememberSaveable(pack.packId) { mutableIntStateOf(0) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var showKeys by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastOk by remember { mutableStateOf<Boolean?>(null) }
    val ex = pack.exercises[idx.coerceIn(0, pack.exercises.lastIndex)]
    LaunchedEffect(ex.id) { field = TextFieldValue(""); feedback = null; lastOk = null; showKeys = false; ime?.hide() }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "تمرین کتاب ${toPersianDigits((idx + 1).toString())} از ${toPersianDigits(pack.exercises.size.toString())}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text(ex.prompt, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = field,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(ex.id) {
                            detectTapGestures {
                                ime?.hide()
                                showKeys = true
                            }
                        },
                    label = { Text("جواب — لمس برای کیبورد نماد") },
                    singleLine = true,
                )
                if (showKeys) {
                    Spacer(Modifier.height(4.dp))
                    MathSymbolKeyboard(value = field, onValue = { field = it; lastOk = null; feedback = null })
                    TextButton(onClick = { showKeys = false }) { Text("بستن کیبورد") }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val ok = MathAnswerScript.grade(ex, field.text)
                        container.studyProgress.recordExercise(pack.packId, ex.id, ok, today)
                        StudyActivity.add(ctx, pack.packId, "item", "تمرین ${ex.id} — ${if (ok) "درست" else "نادرست"}")
                        lastOk = ok
                        val stats = container.studyProgress.exerciseStats(pack.packId).optJSONObject(ex.id)
                        val bad = stats?.optInt("bad") ?: 0
                        val good = stats?.optInt("ok") ?: 0
                        val plan = MathAnswerScript.repeatPlan(ok, if (ok) 0 else bad, if (ok) good else 0)
                        feedback = if (ok) plan.message
                        else "${plan.message}\nنکته: ${ex.hint.ifBlank { "دوباره از روی کتاب نگاه کن." }}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("بررسی جواب") }
                if (feedback != null) {
                    Text(
                        feedback!!,
                        color = if (lastOk == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (lastOk == false) {
                        OutlinedButton(onClick = { field = TextFieldValue(""); lastOk = null; feedback = null; showKeys = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("حل دوباره")
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { idx = (idx - 1).coerceAtLeast(0) }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
            OutlinedButton(onClick = { idx = (idx + 1).coerceAtMost(pack.exercises.lastIndex) }, enabled = idx < pack.exercises.lastIndex, modifier = Modifier.weight(1f)) { Text("بعدی") }
        }
    }
}

@Composable
private fun MathBookMcqPane(pack: StudyPack, qs: List<StudyPack.Question>, modifier: Modifier) {
    val container = LocalAppContainer.current
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val today = remember { JalaliDate.todayIso() }
    var idx by rememberSaveable(pack.packId) { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastOk by remember { mutableStateOf<Boolean?>(null) }
    val q = qs[idx.coerceIn(0, qs.lastIndex)]
    LaunchedEffect(q.id) { pick = null; feedback = null; lastOk = null }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "تمرین کتاب ${toPersianDigits((idx + 1).toString())} از ${toPersianDigits(qs.size.toString())}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(q.text, style = MaterialTheme.typography.bodyMedium)
                q.options.forEach { opt ->
                    Row(
                        Modifier.fillMaxWidth().clickable { pick = opt; lastOk = null; feedback = null },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = pick == opt, onClick = { pick = opt; lastOk = null; feedback = null })
                        Text(opt, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = {
                        val ok = com.hamyareman.ir.platform.feature.study.QuizGrader.grade(q, pick.orEmpty()).second
                        container.studyProgress.recordExercise(pack.packId, q.id, ok, today)
                        StudyActivity.add(appCtx, pack.packId, "item", "تمرین ${q.id} — ${if (ok) "درست" else "نادرست"}")
                        lastOk = ok
                        feedback = if (ok) "درست بود ✓\n${q.explanation}"
                        else "نادرست. پاسخ درست: ${q.answer}\n${q.explanation}"
                    },
                    enabled = pick != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("بررسی جواب") }
                if (feedback != null) {
                    Text(
                        feedback!!,
                        color = if (lastOk == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { idx = (idx - 1).coerceAtLeast(0) }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
            OutlinedButton(onClick = { idx = (idx + 1).coerceAtMost(qs.lastIndex) }, enabled = idx < qs.lastIndex, modifier = Modifier.weight(1f)) { Text("بعدی") }
        }
    }
}

@Composable
private fun MathFlashTab(pack: StudyPack) {
    Text(
        "فلش‌کارت این فصل هنوز به‌صورت HTML در پوشهٔ Books نیست.",
        Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun MathSummaryTab(pack: StudyPack, isSum: Boolean, chapter: Int, onZoomChanged: (Boolean) -> Unit = {}) {
    val webRef = remember { arrayOfNulls<WebView>(1) }
    ManagedWebMediaEffect { webRef[0] }
    val summary = pack.summary.ifBlank {
        pack.sections.filter { it.kind == "exam" }.lastOrNull()?.body
            ?: "خلاصه‌ی چندسطری این درس به‌زودی از پوشهٔ Books نوشته می‌شود."
    }
    val tips = pack.examTips.ifBlank {
        pack.sections.filter { it.kind == "exam" }.joinToString("\n\n") { it.body }
    }
    val html = remember(pack.packId, pack.teachHtml, summary, tips, isSum, chapter) {
        if (pack.teachHtml.contains("<html", ignoreCase = true)) pack.teachHtml
        else htmlSummaryDocument(
            teachHtml = pack.teachHtml,
            isSum = isSum,
            fallback = mathSummaryHtml(pack.title, summary, tips, isSum, chapter),
        )
    }
    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.bindManagedMediaLifecycle()
                        }
                    }
                    settings.javaScriptEnabled = true
                    installManagedMediaLifecycle()
                    webRef[0] = this
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.defaultTextEncodingName = "utf-8"
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setOnTouchListener { v, _ ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                }
            },
            update = { wv ->
                val tag = html.hashCode()
                if (wv.tag != tag) {
                    wv.tag = tag
                    wv.loadDataWithBaseURL("https://local.hamyar/", html, "text/html", "utf-8", null)
                }
            },
            modifier = Modifier.weight(1f).padding(4.dp),
            onRelease = {
                it.stopManagedMedia()
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            },
        )
    }
}

/** جدول خلاصه + نکات + SVG/شکل‌های همان HTML تدریس، با استایل اصلی. */
internal fun htmlSummaryDocument(teachHtml: String, isSum: Boolean, fallback: String): String {
    if (teachHtml.isBlank() || !teachHtml.contains("<html", ignoreCase = true)) return fallback
    if (isSum) return teachHtml
    val styles = Regex("(?is)<style[^>]*>.*?</style>").findAll(teachHtml).joinToString("\n") { it.value }
    val markers = listOf("جدول خلاصه", "خلاصه‌ی فرمول", "summary-table", "نکات امتحانی مهم")
    val hit = markers.map { teachHtml.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return teachHtml
    val sec = teachHtml.lastIndexOf("<section", hit).takeIf { it >= 0 } ?: hit
    val end = listOf("</main>", "<footer", "</body>").map { teachHtml.indexOf(it, sec) }.filter { it > sec }.minOrNull()
        ?: teachHtml.length
    val fragment = teachHtml.substring(sec, end)
    if (fragment.length < 80) return teachHtml
    return """
<!DOCTYPE html><html dir="rtl" lang="fa"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
$styles
</head><body>
<div class="container">$fragment</div>
</body></html>
""".trimIndent()
}

private fun mathSummaryHtml(title: String, summary: String, tips: String, isSum: Boolean, chapter: Int): String {
    val ch = toPersianDigits(chapter.toString())
    val pointer = if (isSum) {
        "<p>فلش‌کارت و نمونه سوالات همین فصل در سربرگ‌های این جمع‌بندی است.</p>"
    } else {
        "<div class='note'>🎴 آزمون و فلش‌کارت این درس در <strong>انتهای فصل $ch</strong> — کارت «جمع‌بندی فصل $ch» — آمده است. خودِ درس فلش و آزمون جدا ندارد.</div>"
    }
    val tipsBlock = if (tips.isBlank()) "" else "<h2>نکات امتحانی</h2><p>${tips.replace("\n", "<br>")}</p>"
    return """
<!DOCTYPE html><html dir="rtl" lang="fa"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
@import url('https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;700;900&display=swap');
body{font-family:'Vazirmatn',Tahoma,sans-serif;background:linear-gradient(135deg,#eef2ff,#fdf2f8 50%,#ecfeff);color:#1e293b;margin:0;padding:12px;line-height:1.9}
.box{max-width:900px;margin:0 auto;background:#fff;border-radius:24px;box-shadow:0 20px 60px rgba(30,41,59,.15);overflow:hidden}
header{background:linear-gradient(135deg,#4f46e5,#7c3aed,#ec4899);color:#fff;padding:22px 20px;text-align:center}
h1{font-size:1.25rem;margin:0 0 6px;font-weight:900}
main{padding:18px 16px 28px}
h2{color:#4f46e5;font-size:1.05rem;border-bottom:2px dashed #c7d2fe;padding-bottom:6px}
.note{background:#e0f2fe;border-right:4px solid #0284c7;padding:12px 14px;border-radius:12px;margin:12px 0}
</style></head><body><div class="box">
<header><h1>خلاصه — $title</h1></header>
<main>
$pointer
<h2>خلاصه درس</h2>
<p>${summary.replace("\n", "<br>")}</p>
$tipsBlock
</main></div></body></html>
""".trimIndent()
}

@Composable
private fun MathExamTab(pack: StudyPack) {
    val container = LocalAppContainer.current
    val today = remember { JalaliDate.todayIso() }
    val mcq = remember(pack.packId) { MathExamLedger.mcqOf(pack) }
    var tick by remember { mutableIntStateOf(0) }
    val ledger = remember(pack.packId, tick) { container.studyProgress.examState(pack.packId) }
    var answers by remember(pack.packId) { mutableStateOf(mapOf<String, String>()) }
    var done by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("نمونه سوال چهارگزینه‌ای", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (mcq.isEmpty()) {
            Text(
                "نمونه سوالات این فصل در کارت جمع‌بندی همان فصل است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ledger.repeatCount > 0) ExamLedgerCard(ledger)
            return
        }
        if (done) {
            ExamLedgerCard(ledger)
            Button(onClick = { answers = emptyMap(); done = false }, modifier = Modifier.fillMaxWidth()) {
                Text("تجدید آزمون (نتیجهٔ نمودار جایگزین می‌شود)")
            }
            return
        }
        mcq.forEachIndexed { i, q ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${toPersianDigits((i + 1).toString())}. ${q.text}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    q.options.forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { answers = answers + (q.id to opt) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = answers[q.id] == opt, onClick = { answers = answers + (q.id to opt) })
                            Text(opt, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                container.studyProgress.recordExamSitting(pack, answers, today)
                tick++
                done = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = answers.size == mcq.size,
        ) { Text("تصحیح آزمون") }
    }
}

/**
 * زمانِ شروعِ هر سرفصل (میلی‌ثانیه) به ترتیبِ آیتم‌های فهرستِ HTML — پشتیبانِ
 * HTMLهایی که `data-seek-ms` ندارند (نسخهٔ ریموت/قدیمی). کلید = packId.
 * همه‌ی پنج صوتِ فصلِ ۱ (درس ۱ تا ۴ + جمع‌بندی) و درسِ ۱ تا ۳ فصلِ ۲ زمان‌هایشان
 * کامل است؛ بقیهٔ درس‌ها به‌محضِ گذاشتنِ `.txt` در Books با
 * `tools/seek-shim/sync_seek_html.py` همگام می‌شوند.
 */
internal object TeachSeekMap {
    // منبعِ حقیقت: فهرست‌های زمان‌بندیِ پوشهٔ صوتِ تدریس در Books
    // (ryazif01d01.txt … ryazif01review.txt). این جدول با همان فایل‌ها و با
    // «data-seek-ms»های داخلِ HTMLهای assets همگام است؛ آزمونِ
    // MediaVaultRangeTest هر سه را با هم مقایسه می‌کند تا از هم دور نیفتند.
    private val table: Map<String, List<Long>> = mapOf(
        "C905_E01-L01" to listOf(
            57_000, 221_000, 340_000, 443_000, 525_000,
            674_000, 749_000, 891_000, 1_047_000, 1_495_000,
        ),
        "C905_E01-L02" to listOf(
            60_000, 131_000, 286_000, 457_000, 624_000,
            786_000, 928_000, 1_164_000, 1_636_000,
        ),
        "C905_E01-L03" to listOf(
            70_000, 128_000, 233_000, 462_000, 595_000,
            779_000, 903_000, 972_000, 1_070_000, 1_325_000,
            1_796_000,
        ),
        "C905_E01-L04" to listOf(
            70_000, 137_000, 245_000, 341_000, 405_000,
            525_000, 664_000, 805_000, 885_000, 1_085_000,
            1_681_000,
        ),
        "C905_E01-SUM" to listOf(
            76_000, 172_000, 244_000, 366_000, 454_000,
            552_000, 648_000, 1_184_000, 1_283_000,
        ),
        // فصلِ ۲ — درسِ ۱ (عددهای گویا) و درسِ ۲ (عددهای حقیقی).
        // «بخش صفر: مقدمه»، «استراحت» و «جمع‌بندی»ِ فایلِ .txt در فهرستِ HTML جایی
        // ندارند، پس نگاشت با همترازیِ عنوان‌ها انجام شده (tools/seek-shim/sync_seek_html.py).
        "C905_E02-L01" to listOf(
            65_000, 149_000, 262_000, 365_000, 498_000,
            631_000, 897_000, 1_045_000, 1_234_000, 1_692_000,
        ),
        "C905_E02-L02" to listOf(
            67_000, 149_000, 308_000, 444_000, 573_000,
            774_000, 842_000, 912_000, 1_022_000, 1_433_000,
        ),
        // درسِ ۳ فصلِ ۲ (قدر مطلق) — همان قاعدهٔ همترازیِ عنوان‌ها.
        "C905_E02-L03" to listOf(
            81_000, 167_000, 230_000, 406_000, 568_000,
            878_000, 1_037_000, 1_162_000, 1_301_000, 1_777_000,
        ),
    )
    fun times(packId: String): List<Long> = table[packId].orEmpty()
}

/**
 * شیمِ سیکِ فهرست/سرفصل‌های HTML تدریس:
 *  ۱) زمان‌های [times] (به ترتیبِ فهرست) به لینک‌هایِ بدونِ `data-seek-ms` داده می‌شود؛
 *  ۲) نقشهٔ `#id → ms` از هر عنصرِ زمان‌دار ساخته می‌شود و **سرفصل‌های داخلِ متن**
 *     (`.section-title` / `.subsection-title` / هر `id="secN"`) هم همان زمان را
 *     می‌گیرند و کلیک‌پذیر می‌شوند — یعنی خودِ سرفصل هم پلیر را می‌برد؛
 *  ۳) کنارِ هر آیتمِ فهرست، برچسبِ زمان (`m:ss`) نمایش داده می‌شود (`.toc .t`)؛
 *  ۴) شنوندهٔ کلیک روی هر عنصرِ زمان‌دار (نه فقط `<a>`) `HamyarPlayer.seek(ms)`
 *     را صدا می‌زند و همان بند را اسکرول می‌کند؛ اگر زمانی نبود، از href/متن
 *     (`#t=5:32` یا `۵:۳۲`) استخراج می‌شود.
 */
internal fun ensureSeekShim(html: String, times: List<Long> = emptyList()): String {
    if (html.isBlank()) return html
    val arr = times.joinToString(",") { it.toString() }
    val shim = "<script>(function(){\"use strict\";" +
        "function digits(s){return String(s).replace(/[\u06F0-\u06F9]/g,function(d){return String(d.charCodeAt(0)-0x06F0);})" +
            ".replace(/[\u0660-\u0669]/g,function(d){return String(d.charCodeAt(0)-0x0660);});}" +
        "function parseMs(v){if(v==null)return null;var x=parseInt(digits(v).replace(/[^0-9]/g,''),10);" +
            "return (isNaN(x)||x<0)?null:x;}" +
        "function fa(s){return String(s).replace(/[0-9]/g,function(d){return String.fromCharCode(0x06F0+parseInt(d,10));});}" +
        "function label(ms){var m=Math.floor(ms/60000),s=Math.floor((ms%60000)/1000);return fa(m+':'+(s<10?'0':'')+s);}" +
        "function msOfText(el){" +
        "var h=(el.getAttribute&&el.getAttribute('href'))||'';" +
        "var m=digits(h).match(/(?:t=|#t)([0-9]{1,3}):([0-9]{1,2})(?::([0-9]{1,2}))?/);" +
        "if(m){var a=parseInt(m[1],10),b=parseInt(m[2],10);if(m[3]){a=a*60+b;b=parseInt(m[3],10);}return (a*60+b)*1000;}" +
        "var t=digits((el.textContent||'').slice(0,120)).match(/([0-9]{1,3}):([0-9]{1,2})(?::([0-9]{1,2}))?/);" +
        "if(t){var c=parseInt(t[1],10),d=parseInt(t[2],10);if(t[3]){c=c*60+d;d=parseInt(t[3],10);}return (c*60+d)*1000;}" +
        "return null;}" +
        "var TIMES=[$arr];" +
        "var links=document.querySelectorAll('.toc ol li a, .toc li a, .toc a');" +
        "for(var i=0;i<links.length;i++){var a0=links[i];" +
        "if(!a0.getAttribute('data-seek-ms')&&i<TIMES.length&&TIMES[i]>0){a0.setAttribute('data-seek-ms',String(TIMES[i]));}}" +
        "var map={};" +
        "document.querySelectorAll('[data-seek-ms]').forEach(function(el){" +
        "var ms=parseMs(el.getAttribute('data-seek-ms'));if(ms==null)return;" +
        "var h=el.getAttribute('href')||'';if(h.charAt(0)==='#'&&h.length>1){map[h.slice(1)]=ms;}" +
        "if(el.id){map[el.id]=ms;}});" +
        "document.querySelectorAll('.section-title,.subsection-title,[id^=\"sec\"]').forEach(function(el){" +
        "if(el.getAttribute('data-seek-ms'))return;var ms=map[el.id];if(ms==null)return;" +
        "el.setAttribute('data-seek-ms',String(ms));el.style.cursor='pointer';});" +
        "document.querySelectorAll('.toc a[data-seek-ms]').forEach(function(a){" +
        "if(a.querySelector('.t'))return;var ms=parseMs(a.getAttribute('data-seek-ms'));if(ms==null)return;" +
        "var sp=document.createElement('span');sp.className='t';sp.textContent=label(ms);" +
        "sp.style.marginLeft='6px';a.insertBefore(sp,a.firstChild);});" +
        "function go(el,ms,ev){if(ev)ev.preventDefault();" +
        "try{if(window.HamyarPlayer&&HamyarPlayer.seek){HamyarPlayer.seek(ms);}}catch(_){}" +
        "var id=(el.getAttribute&&el.getAttribute('href'))||'';" +
        "if(id.charAt(0)==='#'&&id.length>1){try{var t=document.querySelector(id);if(t)t.scrollIntoView({behavior:'smooth',block:'start'});}catch(_){}}" +
        "else if(el.id){try{el.scrollIntoView({behavior:'smooth',block:'start'});}catch(_){}}}" +
        "document.addEventListener('click',function(e){" +
        "var el=e.target,hops=0;" +
        "while(el&&hops<6){" +
        "var v=el.getAttribute?el.getAttribute('data-seek-ms'):null;" +
        "if(v!=null){var ms=parseMs(v);if(ms!=null){go(el,ms,e);return;}}" +
        "el=el.parentElement;hops++;}" +
        "var a=e.target;while(a&&a.tagName!=='A')a=a.parentElement;if(!a)return;" +
        "var ms2=msOfText(a);if(ms2!=null){go(a,ms2,e);}" +
        "},true);})();</script>"
    val i = html.lastIndexOf("</body>")
    return if (i >= 0) html.substring(0, i) + shim + html.substring(i) else html + shim
}

/** پل JS فهرست HTML → سیک پلیر تدریس. */
private class TeachHtmlBridge {
    @JavascriptInterface
    fun seek(ms: Int) {
        TeachSeekBus.seekMs(ms.toLong())
    }
}

@Composable
internal fun ExamLedgerCard(ledger: MathExamLedger.State) {
    val last = ledger.latest ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("نتیجهٔ نمودار (آخرین نشست)", fontWeight = FontWeight.Bold)
            Text("نمره: ${toPersianDigits(last.scorePct.toString())}٪ از ${toPersianDigits(last.total.toString())} سوال")
            Text("تعداد تکرار: ${toPersianDigits(ledger.repeatCount.toString())}")
            ledger.sittings.forEach { s ->
                val wrong = if (s.wrongNumbers.isEmpty()) "بدون غلط"
                else "غلط: ${s.wrongNumbers.joinToString("، ") { toPersianDigits(it.toString()) }}"
                Text(
                    "نشست ${toPersianDigits(s.n.toString())}: ${toPersianDigits(s.scorePct.toString())}٪ — $wrong",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
