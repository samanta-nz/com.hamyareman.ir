package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.StudyPack
import com.hamyareman.ir.LocalAppContainer

/**
 * «نمودار پیشرفت دروس» (v1.14) — تفکیک‌شده به‌ازای هر کتاب:
 * فقط آمار واقعیِ خودکارِ ثبت‌شده و سینک‌شونده، غیرقابل ویرایش:
 *  - تدریس: وضعیت اولین دوره‌ی هر درس — مشاهده‌شده از کل + باقیمانده، نشست‌ها،
 *    «اتمام دوره‌ی اول در چند نشست» و تاریخ/ساعت شمسیِ شروع/آخرین نشست/اتمام؛
 *  - فلش‌کارت‌ها: کل/یادگرفته/باقیمانده/مرورشده + تسلط؛
 *  - آزمون‌ها: تعداد، آخرین/بهترین/میانگین + موضوعات ضعیف، با تاریخ/ساعت شمسی.
 */
/**
 * v1.18 — نمودار پیشرفت «اختصاصی هر کتاب»: فقط گزارش‌های همان کتاب لیست می‌شود.
 * بدون bookCode: فهرست کتاب‌ها برای انتخاب (با شمار شروع‌شده/کامل‌شده‌ی هر کتاب).
 */
/** سقفِ کلِ یک همگام‌سازی — بعد از آن، پیام نمایش داده می‌شود و صفحه رها می‌گردد. */
private const val SYNC_TIMEOUT_MS = 25_000L

@Composable
fun ProgressChartsScreen(bookCode: String?, onBack: () -> Unit, onPickBook: (String) -> Unit) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val books = remember { com.hamyareman.ir.ui.profile.GradeGate.filter(BookModuleRegistry.modules) { it.bookCode } }
    val module = remember(bookCode) { books.firstOrNull { it.bookCode == bookCode } }

    // ---------- سینکِ نمودار پیشرفت ----------
    // ورود به صفحه: اول هرچه محلی مانده می‌رود (push)، بعد از سرور گرفته و ادغام می‌شود (pull).
    // سقف زمانی دارد تا صفحه هرگز روی «در حال همگام‌سازی…» گیر نکند؛ آمارِ محلی
    // مستقل از سینک ثبت می‌شود و در سینکِ بعدی ارسال خواهد شد.
    var syncTick by remember { mutableIntStateOf(0) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    LaunchedEffect(bookCode, syncTick) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) {
            syncMsg = "برای سینکِ نمودارها وارد حساب شو."
            return@LaunchedEffect
        }
        syncing = true
        val merged = withContext(Dispatchers.IO) {
            try {
                withTimeout(SYNC_TIMEOUT_MS) {
                    val packIds = module?.packs?.map { it.packId }.orEmpty()
                    TeachCloud.enqueueAll(ctx, container.sync, uid, packIds)
                    packIds.forEach { runCatching { container.studyProgress.flush(it) } }
                    runCatching {
                        SchoolSync.restoreAll(ctx, container.tables, container.sync, uid, force = true)
                    }.getOrDefault(0)
                }
            } catch (e: TimeoutCancellationException) {
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                -1
            }
        }
        syncing = false
        syncMsg = when {
            merged == null -> "همگام‌سازی به درازا کشید؛ «به‌روزرسانی» را دوباره بزن."
            merged < 0 -> "همگام‌سازی ممکن نشد (شبکه)؛ آمارِ محلی سالم است."
            merged > 0 -> "از سرور به‌روز شد (${toPersianDigits(merged.toString())} مورد)."
            else -> "نمودارها همگام‌اند."
        }
    }

    if (module == null) {
        // ---------- انتخاب کتاب ----------
        Column(Modifier.fillMaxSize()) {
            AppTopBar("نمودار پیشرفت — انتخاب کتاب", onBack)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    syncMsg ?: "در حال همگام‌سازی…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { syncTick++ }, enabled = !syncing) { Text("به‌روزرسانی") }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // پیش از فهرستِ کتاب‌ها: دروسِ روزهای مرخصی/غیبت برای جبران
                LeaveCatchUpSection(onPickBook)
                books.forEach { m ->
                    val started = m.packs.count { TeachStats.raw(ctx, it.packId).length() > 0 }
                    val done = m.packs.count { TeachStats.isDone(ctx, it.packId) }
                    Card(
                        Modifier.fillMaxWidth().androidClickable { onPickBook(m.bookCode) },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            BookLeadIcon(m, size = 52.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${toPersianDigits(started.toString())} درس شروع شده · دوره‌ی اولِ ${toPersianDigits(done.toString())} درس کامل شده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نمودار پیشرفت — ${module.title}", onBack)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                syncMsg ?: "در حال همگام‌سازی…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { syncTick++ }, enabled = !syncing) { Text("به‌روزرسانی") }
        }
        if (module.bookCode == "C905") {
            MathLessonProgressPage(module, Modifier.weight(1f))
            return
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val books = listOf(module)

            // ---------- جمع‌بندی همین کتاب ----------
            val allPacks = module.packs
            val startedAll = allPacks.count { TeachStats.raw(ctx, it.packId).length() > 0 }
            val doneAll = allPacks.count { TeachStats.isDone(ctx, it.packId) }
            SectionCard(
                title = "جمع‌بندی این کتاب",
                body = "${toPersianDigits(startedAll.toString())} درس شروع شده · دوره‌ی اولِ ${toPersianDigits(doneAll.toString())} درس کامل شده · " +
                    "همه‌ی اعداد خودکار ثبت و سینک می‌شوند و قابل ویرایش نیستند (تاریخ‌ها شمسی).",
            ) { }

            // ---------- درس‌های همین کتاب ----------
            books.forEach { md ->
                val packs = md.packs
                val teachActive = packs.filter { TeachStats.raw(ctx, it.packId).length() > 0 }
                val doneHere = packs.count { TeachStats.isDone(ctx, it.packId) }
                val cardRows = packs.mapNotNull { p ->
                    val states = runCatching { container.studyProgress.cards(p.packId) }.getOrDefault(emptyMap())
                    if (p.flashcards.isEmpty() || states.isEmpty()) null else p to states
                }
                val quizRows = packs.mapNotNull { p ->
                    val at = runCatching { container.studyProgress.attempts(p.packId) }.getOrDefault(emptyList())
                    if (at.isEmpty()) null else p to at
                }
                if (teachActive.isEmpty() && cardRows.isEmpty() && quizRows.isEmpty()) {
                    Text(
                        "هنوز گزارشی برای این کتاب ثبت نشده — با تدریس/فلش‌کارت/آزمون، اینجا پر می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    return@forEach
                }

                Text("${bookGlyph(module.bookCode, module.title)} ${module.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (teachActive.isNotEmpty()) {
                    Text(
                        "دوره‌ی اول کامل شده: ${toPersianDigits(doneHere.toString())} از ${toPersianDigits(teachActive.size.toString())} درس شروع‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                teachActive.forEach { pack -> TeachRow(pack) }
                packs.forEach { pack ->
                    val exams = TeachStats.schoolExams(ctx, pack.packId)
                    if (exams.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("امتحان مدرسه — ${pack.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                exams.takeLast(8).forEach { (iso, t) ->
                                    Text("• $iso — $t", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                cardRows.forEach { (pack, states) ->
                    val total = pack.flashcards.size
                    val reviewed = states.values.sumOf { it.reps }
                    val learned = states.values.count { it.reps > 0 }
                    val remain = (total - learned).coerceAtLeast(0)
                    val mastery = runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                            val archived = runCatching { container.studyProgress.archivedCards(pack).size }.getOrDefault(0)
                            Text(
                                "یادگرفته ${toPersianDigits(learned.toString())} از ${toPersianDigits(total.toString())} · باقیمانده ${toPersianDigits(remain.toString())} · مرورشده ${toPersianDigits(reviewed.toString())} بار · آرشیو ${toPersianDigits(archived.toString())}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                                Spacer(Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = { mastery / 100f },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                )
                            }
                            Text(
                                "تسلط: ${toPersianDigits(mastery.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                quizRows.forEach { (pack, at) ->
                    val last = at.last()
                    val best = at.maxOf { it.scorePct }
                    val avg = at.sumOf { it.scorePct } / at.size.coerceAtLeast(1)
                    val weak = at.flatMap { it.weakTopics }.distinct().take(4)
                    val lastWhen = if (last.atMs > 0) JalaliDate.stampFa(last.atMs) else JalaliDate.formatFa(last.dateKey)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${toPersianDigits(at.size.toString())} آزمون · آخرین: ${toPersianDigits(last.scorePct.toString())}٪" +
                                    " · بهترین: ${toPersianDigits(best.toString())}٪ · میانگین: ${toPersianDigits(avg.toString())}٪",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "آخرین آزمون: $lastWhen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (weak.isNotEmpty()) {
                                Text(
                                    "موضوعات نیازمند مرور: ${weak.joinToString("، ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "این صفحه فقط از آمارِ خودکار پر می‌شود — چیزی اینجا قابل ویرایش نیست. با هر نوشتن، داده‌ها در حسابِ زهرا هم ذخیره می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * نمودار ریاضی: همان دکمهٔ قبلی، صفحهٔ سطری غیرقابل‌ویرایش،
 * هر درس یک کارت با ردیف‌های عنوان سربرگ.
 */
@Composable
private fun MathLessonProgressPage(
    module: com.hamyareman.ir.platform.feature.study.BookModule,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val store = remember { com.hamyareman.ir.platform.core.common.LocalStore(ctx, "hamyar_toc") }
    var openId by remember { mutableStateOf(store.getString("chart_acc_${module.bookCode}", "")) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "فقط آمار خودکار — قابل ویرایش نیست. هر درس را باز کن؛ همزمان فقط یکی باز است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        module.packs.filter { !it.pdfOnly }.forEach { pack ->
            val open = openId == pack.packId
            Card(Modifier.fillMaxWidth().androidClickable {
                openId = if (open) "" else pack.packId
                store.putString("chart_acc_${module.bookCode}", openId)
            }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (open) "▼" else "◀", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                        Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (open) {
                        val isSum = pack.lessonId.contains("SUM")
                        val labels = if (isSum) listOf("تدریس", "فلش‌کارت", "خلاصه", "آزمون")
                        else listOf("تدریس", "تمرینات کتابی", "خلاصه", "کتاب درسی")
                        labels.forEach { title ->
                            val line = mathTabProgressLine(ctx, container, pack, title)
                            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(line, style = MaterialTheme.typography.bodySmall)
                        }
                        val ex = runCatching { container.studyProgress.exerciseStats(pack.packId) }.getOrNull()
                        if (ex != null && ex.length() > 0) {
                            Text("جزئیات تمرین", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            ex.keys().forEach { k ->
                                val item = ex.optJSONObject(k) ?: return@forEach
                                val ok = item.optInt("ok")
                                val bad = item.optInt("bad")
                                val tries = item.optInt("tries")
                                Text(
                                    "• $k — تلاش ${toPersianDigits(tries.toString())} · درست ${toPersianDigits(ok.toString())} · نادرست ${toPersianDigits(bad.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        val schoolEx = TeachStats.schoolExams(ctx, pack.packId)
                        if (schoolEx.isNotEmpty()) {
                            Text("امتحان مدرسه", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            schoolEx.takeLast(8).forEach { (iso, t) ->
                                Text("• $iso — $t", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        val log = StudyActivity.rows(ctx, pack.packId).asReversed().take(80)
                        Text("فعالیت‌ها (سطر به سطر)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (log.isEmpty()) {
                            Text("هنوز ردیفی ثبت نشده.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            log.forEach { row ->
                                Text(
                                    "• ${row.whenFa} — ${row.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mathTabProgressLine(
    ctx: android.content.Context,
    container: com.hamyareman.ir.di.AppContainer,
    pack: StudyPack,
    tab: String,
): String {
    return when (tab) {
        "تدریس" -> {
            TeachStats.expectMedia(ctx, pack.packId, expectedTeachMedia(pack))
            val snap = TeachStats.snap(ctx, pack.packId)
            if (snap.sessions == 0 && !snap.done) "هنوز شروع نشده"
            else {
                val done = if (snap.done) "دورهٔ اول تمام" else "ناقص"
                "نشست ${toPersianDigits(snap.sessions.toString())} · شنیدن ${toPersianDigits(snap.listenSec.toString())}ث · پرش ${toPersianDigits(snap.jumps.toString())} · $done"
            }
        }
        "تمرینات کتابی" -> {
            val ex = runCatching { container.studyProgress.exerciseStats(pack.packId) }.getOrNull()
            val n = ex?.length() ?: 0
            if (n == 0) "تمرینی ثبت نشده"
            else {
                val obj = ex!!
                var ok = 0; var bad = 0; var tries = 0
                obj.keys().forEach { k ->
                    val item = obj.optJSONObject(k) ?: return@forEach
                    ok += item.optInt("ok")
                    bad += item.optInt("bad")
                    tries += item.optInt("tries")
                }
                "آیتم ${toPersianDigits(n.toString())} · تلاش ${toPersianDigits(tries.toString())} · درست ${toPersianDigits(ok.toString())} · نادرست ${toPersianDigits(bad.toString())}"
            }
        }
        "کتاب درسی" -> {
            val n = StudyActivity.rows(ctx, pack.packId).count { it.kind == "pdf" }
            if (n == 0) "صفحه‌ای دیده نشده"
            else "صفحهٔ دیده‌شده: ${toPersianDigits(n.toString())}"
        }
        "فلش‌کارت" -> {
            val total = pack.flashcards.size
            if (total == 0) "فلش‌کارت هنوز نیامده"
            else {
                val learned = runCatching { container.studyProgress.cards(pack.packId).values.count { it.reps > 0 } }.getOrDefault(0)
                val archived = runCatching { container.studyProgress.archivedCards(pack).size }.getOrDefault(0)
                "یادگرفته ${toPersianDigits(learned.toString())} از ${toPersianDigits(total.toString())} · آرشیو ${toPersianDigits(archived.toString())}"
            }
        }
        "خلاصه" -> {
            val all = StudyActivity.rows(ctx, pack.packId)
            val opens = all.count { it.kind == "tab" && it.label.contains("خلاصه") }
            val dwell = all.lastOrNull { it.kind == "dwell" && it.label.contains("خلاصه") }
            if (opens == 0) "هنوز باز نشده"
            else "باز شدن ${toPersianDigits(opens.toString())} بار" + (dwell?.let { " · ${it.label}" } ?: "")
        }
        "آزمون" -> {
            val st = runCatching { container.studyProgress.examState(pack.packId) }.getOrNull()
            val last = st?.latest
            if (last == null) "آزمونی ثبت نشده"
            else {
                val wrong = if (last.wrongNumbers.isEmpty()) "بدون غلط"
                else "غلط ${last.wrongNumbers.joinToString("، ") { toPersianDigits(it.toString()) }}"
                "تکرار ${toPersianDigits(st.repeatCount.toString())} · آخرین ${toPersianDigits(last.scorePct.toString())}٪ · $wrong"
            }
        }
        else -> "—"
    }
}

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

/** ردیف تدریس یک درس — وضعیت اولین دوره با محاسبه‌ی مشاهده/باقیمانده و نشست‌ها. */
@Composable
private fun TeachRow(pack: StudyPack) {
    val ctx = LocalContext.current
    val snap = TeachStats.snap(ctx, pack.packId)
    val totalSec = (snap.audioDurSec + snap.videoDurSec).coerceAtLeast(1)
    val watched = snap.watchedSec.coerceAtMost(totalSec)
    val remainSec = (totalSec - watched).coerceAtLeast(0)
    val pct = (watched * 100) / totalSec
    var open by remember(pack.packId) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                Text(if (snap.done) "✅" else "⏳", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Text(pack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${toPersianDigits(pct.toString())}٪", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(6.dp))
                Text(if (open) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(progress = { watched.toFloat() / totalSec }, modifier = Modifier.fillMaxWidth().height(8.dp))
            if (open) {
                Spacer(Modifier.height(6.dp))
                Text("مشاهده: ${teachMmss(watched * 1000L)} از ${teachMmss(totalSec * 1000L)} · باقیمانده: ${teachMmss(remainSec * 1000L)} · نشست‌ها: ${toPersianDigits(snap.sessions.toString())}" + if (snap.done) " · اتمام دوره‌ی اول: ${toPersianDigits(snap.sessionsToDone.toString())} نشست" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(buildString {
                    append("شروع: "); append(if (snap.startedAtMs > 0) JalaliDate.stampFa(snap.startedAtMs) else "—")
                    append(" · آخرین نشست: "); append(if (snap.lastSessionAtMs > 0) JalaliDate.stampFa(snap.lastSessionAtMs) else "—")
                    if (snap.done && snap.completedAtMs > 0) { append(" · اتمام: "); append(JalaliDate.stampFa(snap.completedAtMs)) }
                }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val log = StudyActivity.rows(ctx, pack.packId).asReversed().take(12)
                if (log.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp)); Text("فعالیت‌ها:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    log.forEach { row -> Text("• ${row.whenFa} — ${row.label}", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
/**
 * «پیگیری دروس و نواقص غیبت و مرخصی‌ها» — درس‌های روزهایی که مرخصی بوده،
 * پیش از فهرستِ کتاب‌ها می‌آید تا جبران‌شان راحت‌تر پیگیری شود.
 * لمسِ هر درس، نمودارِ همان کتاب را باز می‌کند.
 */
@Composable
private fun LeaveCatchUpSection(onPickBook: (String) -> Unit) {
    val ctx = LocalContext.current
    val snap = remember { ClassPlanStore.load(ctx) }
    val leaves = remember { ClassPlanStore.leaves(ctx) }
    var open by remember { mutableStateOf(false) }
    if (leaves.isEmpty()) return

    Column(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (open) "▾" else "◂")
            Text(
                "پیگیری دروس و نواقص غیبت و مرخصی‌ها",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (open) {
            leaves.forEach { rec ->
                val days = ClassPlanStore.daysBetween(rec.fromIso, rec.toIso)
                Text(
                    "${rec.reason} — ${ClassPlanStore.justificationLabel(rec.justification)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                days.forEach { iso ->
                    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return@forEach
                    val lessons = ClassPlanStore.lessonsFor(snap, date)
                        .filter { it.isNotBlank() && it != "—" }
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp)) {
                        Text(
                            "${JalaliDate.weekDayFa(iso)} ${JalaliDate.formatFaLong(iso)}: ",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (lessons.isEmpty()) {
                            Text(
                                "درسی در برنامه نبود",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(Modifier.weight(1f)) {
                                lessons.forEach { sub ->
                                    val code = com.hamyareman.ir.platform.feature.study.BookModuleRegistry.modules
                                        .firstOrNull { ClassPlanStore.shortBookName(it.title) == sub }?.bookCode
                                    Text(
                                        "• $sub",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (code == null) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable(enabled = code != null) {
                                            code?.let(onPickBook)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
