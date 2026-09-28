package com.hamyareman.ir.ui.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.appwrite.FunctionsService
import com.hamyareman.ir.platform.core.common.FunctionIds
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.QuizGrader
import com.hamyareman.ir.platform.feature.study.StudyPack
import com.hamyareman.ir.platform.feature.study.StudyProgressRepository
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * پرامپت ۰۴ — صفحه‌ی «مطالعه‌ی عمیق» یک درس (پایلوت: ریاضی نهم E01-L01).
 *
 * چهار تب ساده و بدون گیج‌کنندگی:
 *  ۱) خلاصه‌ها — بخش‌های مهم/نکات/نکات امتحانی
 *  ۲) فلش‌کارت — تکرار فاصله‌دار (SM-2 سبک) تا اطمینان از یادگیری
 *  ۳) آزمون — تصحیح دورگه: آفلاین قطعی + AI اختیاری برای رفع اشکال مفهومی
 *  ۴) حل تشریحی — حل قدم‌به‌قدم سوال‌های نمونه
 *
 * همه‌چیز آفلاین کار می‌کند؛ AI فقط تقویت است و اگر در دسترس نباشد چیزی کم نمی‌شود.
 */
private val TABS = listOf("خلاصه و نکات" to "content", "فلش‌کارت" to "cards", "آزمون" to "quiz", "حل تشریحی" to "solutions")

@Composable
fun LessonStudyScreen(packId: String, onBack: () -> Unit, onOpenPdf: (String) -> Unit = {}) {
    val container = LocalAppContainer.current
    val pack = remember(packId) { container.studyPacks.pack(packId) }
    if (pack != null && pack.bookCode == "C905") {
        MathLessonScreen(packId = packId, initialTab = 1, onBack = onBack)
        return
    }
    var refresh by remember { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableStateOf("content") }
    val today = remember { JalaliDate.todayIso() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = pack?.title ?: "مطالعه", onBack = onBack)

        if (pack == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("این بسته‌ی مطالعه هنوز آماده نیست.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "فعلاً دروس ۱ تا ۴ فصل ۱ ریاضی نهم آماده است؛ از هاب «مدرسه» انتخاب کن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        // نوار تسلط + شمار کارت‌های امروز — همیشه بالای صفحه دیده می‌شود.
        val mastery = remember(refresh, packId) { container.studyProgress.masteryPct(pack) }
        val dueCount = remember(refresh, packId) { container.studyProgress.dueCards(pack, today).size }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("تسلط", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { mastery / 100f },
                    modifier = Modifier.weight(1f).height(8.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("$mastery٪", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (dueCount > 0) "🎯 $dueCount کارت برای مرور امروز آماده است"
                else "✓ همه‌ی کارت‌های امروز مرور شد — آفرین!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // کارتِ پلیرِ صوتِ تدریس «بالای سربرگ‌ها» — در همه‌ی سربرگ‌ها یکی می‌ماند.
        val teachTracks = remember(pack.packId, refresh) { teachTracksOf(pack) }
        if (teachTracks.isNotEmpty()) {
            val teachBookTitle = remember(pack.packId) {
                BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == pack.packId } }?.title.orEmpty()
            }
            TeachAudioBar(
                packId = pack.packId,
                screenTitle = pack.title,
                bookTitle = teachBookTitle,
                tracks = teachTracks,
            )
        }

        val selected = TABS.indexOfFirst { it.second == tab }.coerceAtLeast(0)
        TabRow(selectedTabIndex = selected) {
            TABS.forEachIndexed { i, (label, key) ->
                Tab(
                    selected = selected == i,
                    onClick = { tab = key },
                    text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        when (tab) {
            "cards" -> FlashcardsTab(pack, container.studyProgress, today, refresh) { refresh++ }
            "quiz" -> QuizTab(
                pack = pack,
                progress = container.studyProgress,
                functions = container.functions,
                aiOn = container.functions.isConfigured,
                today = today,
            ) { refresh++ }
            "solutions" -> SolutionsTab(pack)
            else -> ContentTab(pack, onOpenPdf)
        }
    }
}

// ---------------------------------------------------------------- خلاصه‌ها

private fun kindLabel(kind: String): String = when (kind) {
    "concept" -> "مفهوم اصلی"
    "important" -> "مهم"
    "note" -> "نکته"
    "exam" -> "امتحانی"
    else -> kind
}

private fun kindColor(kind: String): Color = when (kind) {
    "concept" -> Color(0xFF3949AB)
    "important" -> Color(0xFF00695C)
    "note" -> Color(0xFF6D4C41)
    "exam" -> Color(0xFFB71C1C)
    else -> Color(0xFF546E7A)
}

@Composable
private fun ContentTab(pack: StudyPack, onOpenPdf: (String) -> Unit) {
    // طبق بازخورد مصوب: خلاصه‌ها = «همان متن کتاب» (PDF صفحه‌به‌صفحه) +
    // خلاصه/نکات امتحانی در یک جمع‌شونده. پلیرِ تدریس بالای سربرگ‌هاست (همه‌ی تب‌ها).
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        val examNotes = remember(pack.packId) { pack.sections.filter { it.kind == "exam" || it.kind == "important" } }
        if (examNotes.isNotEmpty()) {
            var showNotes by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { showNotes = !showNotes }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showNotes) "📄 بستن خلاصه و نکات امتحانی" else "📌 خلاصه و نکات امتحانی (${examNotes.size} مورد)")
            }
            if (showNotes) {
                Column(
                    Modifier.fillMaxWidth().weight(0.35f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    examNotes.forEach { s ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Surface(shape = RoundedCornerShape(6.dp), color = kindColor(s.kind).copy(alpha = 0.14f)) {
                                    Text(
                                        kindLabel(s.kind),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = kindColor(s.kind),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(s.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
    }
}


// ---------------------------------------------------------------- فلش‌کارت

/** چهار دکمه‌ی کیفیت — همان قرارداد SM-2 سبک: q=1 بلدنبودم، 3 سخت، 4 خوب، 5 عالی. */
private data class QualityBtn(val label: String, val q: Int)

private val QUALITIES = listOf(
    QualityBtn("بلد نبودم", 1),
    QualityBtn("سخت بود", 3),
    QualityBtn("خوب", 4),
    QualityBtn("عالی", 5),
)

@Composable
private fun FlashcardsTab(
    pack: StudyPack,
    progress: StudyProgressRepository,
    today: String,
    revision: Int,
    onChanged: () -> Unit,
) {
    // همیشه «اولین کارتِ سررسید» را نشان می‌دهیم؛ بعد از هر مرور، کارت به فردا
    // (یا دورتر) می‌رود و خودش از صف خارج می‌شود — بدون شمارنده‌ی شکننده.
    // [revision] هر مرور را از والد می‌آید تا صف دوباره خوانده شود.
    val queue = remember(revision, pack.packId, today) { progress.dueCards(pack, today) }
    var flipTick by remember { mutableIntStateOf(0) }
    val card = queue.firstOrNull()
    var flipped by remember { mutableStateOf(false) }
    LaunchedEffect(card?.id, flipTick) { flipped = false }

    if (card == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (pack.flashcards.isEmpty()) {
                Text("🃏 فلش‌کارت این درس به‌زودی اضافه می‌شود", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "محتوای تعاملی این درس از روی کتاب بازسازی می‌شود؛ تا آن موقع می‌توانی کتاب درس را در «خلاصه و نکات» بخوانی.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("✓ کارت سررسیدی امروز نمانده!", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "کارت‌های مرورشده طبق برنامه‌ی تکرار، روزهای بعد دوباره می‌آیند. آزمون هم اشتباه‌های قبلی را ۷ روز بعد دوباره می‌پرسد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val state = remember(card.id) { progress.stateOf(pack.packId, card.id) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "کارت‌های باقی‌مانده‌ی امروز: ${queue.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Card(
            Modifier.fillMaxWidth().clickable { flipped = !flipped }.height(240.dp),
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (card.topic.isNotBlank()) {
                    Text(card.topic, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    if (flipped) card.back else card.front,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (flipped) FontWeight.Normal else FontWeight.Bold,
                )
                if (flipped && card.hint.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "یادآوری: ${card.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!flipped) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "برای دیدن پاسخ لمس کن",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "دفعات مرور: ${state.reps} — مرور بعدی: ${if (state.dueKey.isBlank() || state.dueKey <= today) "همین امروز" else JalaliDate.formatFa(state.dueKey)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (!flipped) {
            PrimaryButton("نمایش پاسخ", Modifier.fillMaxWidth()) { flipped = true }
        } else {
            Text(
                "چقدر خوب یاد داشتی؟",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QUALITIES.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { b ->
                            OutlinedButton(
                                onClick = {
                                    progress.reviewCard(pack.packId, card.id, b.q, today)
                                    flipTick++
                                    onChanged()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(b.label) }
                        }
                    }
                }
            }
        }
    }
}


// ---------------------------------------------------------------- آزمون

private const val QUIZ_SIZE = 10

/** ساخت جلسه‌ی آزمون: اول سوال‌های اولویت‌دار (دوره‌ای/اشتباه‌ها)، بعد بقیه تصادفی. */
private fun buildSession(pack: StudyPack, priorityIds: List<String>, size: Int = QUIZ_SIZE): List<StudyPack.Question> {
    val byId = pack.questions.associateBy { it.id }
    val head = priorityIds.mapNotNull { byId[it] }.distinct().take(size)
    val rest = pack.questions.filter { q -> priorityIds.none { it == q.id } }.shuffled()
    return (head + rest).take(size.coerceAtMost(pack.questions.size))
}

@Composable
private fun QuizTab(
    pack: StudyPack,
    progress: StudyProgressRepository,
    functions: FunctionsService,
    aiOn: Boolean,
    today: String,
    onChanged: () -> Unit,
) {
    var phase by rememberSaveable { mutableStateOf("idle") } // idle | run | result
    var session by remember { mutableStateOf<List<StudyPack.Question>>(emptyList()) }
    var periodic by rememberSaveable { mutableStateOf(false) }
    var idx by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    var outcome by remember { mutableStateOf<QuizGrader.Outcome?>(null) }

    when (phase) {
        "run" -> QuizRun(
            pack = pack,
            session = session,
            idx = idx,
            answers = answers,
            periodic = periodic,
            onIdx = { idx = it },
            onAnswer = { qid, a -> answers[qid] = a },
            onSubmit = {
                val o = QuizGrader.gradeAll(pack, answers.toMap())
                progress.recordAttempt(
                    pack.packId,
                    StudyProgressRepository.Attempt(
                        dateKey = today,
                        scorePct = o.scorePct,
                        total = o.results.size,
                        wrongIds = o.wrongQuestionIds,
                        weakTopics = o.weakTopics,
                        atMs = System.currentTimeMillis(),
                    ),
                )
                outcome = o
                phase = "result"
                onChanged()
            },
            onCancel = { phase = "idle" },
        )
        "result" -> QuizResult(
            pack = pack,
            outcome = outcome,
            functions = functions,
            aiOn = aiOn,
            progress = progress,
            today = today,
            wasPeriodic = periodic,
            onRetryWrong = {
                val wrong = outcome?.wrongQuestionIds.orEmpty()
                if (wrong.isNotEmpty()) {
                    session = buildSession(pack, wrong)
                    answers.clear()
                    idx = 0
                    periodic = true
                    phase = "run"
                }
            },
            onNewQuiz = {
                session = buildSession(pack, emptyList())
                answers.clear()
                idx = 0
                periodic = false
                phase = "run"
            },
        )
        else -> {
            if (pack.questions.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("📝 سوال‌های این درس به‌زودی اضافه می‌شود", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "آزمون این درس از روی کتاب ساخته می‌شود؛ اول تدریس را کامل کن تا باز شود.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return
            }
            val due = progress.periodicQuizDue(pack.packId, today)
            val wrongIds = progress.periodicWrongIds(pack.packId).filter { id -> pack.questions.any { it.id == id } }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(
                    "آزمون از همه‌ی سوال‌های درس ساخته می‌شود. تصحیح همین‌جا و آفلاین انجام می‌شود؛ اگر بخواهی، هوش مصنوعی هم اشکال مفهومی‌ات را پیدا می‌کند.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (due && wrongIds.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("⏰ آزمون دوره‌ای سررسید!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "۷ روز گذشته؛ بهتر است ${wrongIds.size} سوالِ قبلاً اشتباه را دوباره ببینی تا مطمئن شویم یاد گرفته‌ای.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton("شروع آزمون دوره‌ای", Modifier.fillMaxWidth()) {
                                session = buildSession(pack, wrongIds)
                                answers.clear(); idx = 0; periodic = true; phase = "run"
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton("شروع آزمون جدید (${QUIZ_SIZE} سوال)", Modifier.fillMaxWidth()) {
                    session = buildSession(pack, emptyList())
                    answers.clear(); idx = 0; periodic = false; phase = "run"
                }
                Spacer(Modifier.height(10.dp))
                val attempts = progress.attempts(pack.packId)
                if (attempts.isNotEmpty()) {
                    Text("تاریخچه‌ی آزمون‌ها:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    attempts.takeLast(5).reversed().forEach { a ->
                        val fa = if (a.dateKey.isBlank()) "" else JalaliDate.formatFa(a.dateKey) + " — "
                        Text(
                            "• ${fa}نمره ${a.scorePct}٪ از ${a.total} سوال",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizRun(
    pack: StudyPack,
    session: List<StudyPack.Question>,
    idx: Int,
    answers: Map<String, String>,
    periodic: Boolean,
    onIdx: (Int) -> Unit,
    onAnswer: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    if (session.isEmpty()) {
        Text("سوالی برای آزمون پیدا نشد.", Modifier.padding(16.dp))
        return
    }
    val q = session[idx.coerceIn(0, session.size - 1)]
    val allAnswered = session.all { answers.containsKey(it.id) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سوال ${idx + 1} از ${session.size}" + if (periodic) " (دوره‌ای)" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("انصراف") }
        }
        LinearProgressIndicator(progress = { (idx + 1) / session.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp))
        Spacer(Modifier.height(14.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(q.text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                when (q.type) {
                    "mcq" -> {
                        q.options.forEach { opt ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onAnswer(q.id, opt) }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = answers[q.id] == opt, onClick = { onAnswer(q.id, opt) })
                                Text(opt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    "numeric" -> OutlinedTextField(
                        value = answers[q.id].orEmpty(),
                        onValueChange = { onAnswer(q.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("جواب عددی") },
                        singleLine = true,
                    )
                    else -> OutlinedTextField(
                        value = answers[q.id].orEmpty(),
                        onValueChange = { onAnswer(q.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("جواب کوتاه") },
                        minLines = 2,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onIdx(idx - 1) }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
            if (idx < session.size - 1) {
                Button(onClick = { onIdx(idx + 1) }, modifier = Modifier.weight(1f)) { Text("بعدی") }
            } else {
                Button(onClick = onSubmit, enabled = allAnswered, modifier = Modifier.weight(1f)) {
                    Text(if (allAnswered) "پایان و تصحیح" else "همه را جواب بده")
                }
            }
        }
        if (!allAnswered) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${session.count { !answers.containsKey(it.id) }} سوال بی‌جواب مانده.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuizResult(
    pack: StudyPack,
    outcome: QuizGrader.Outcome?,
    functions: FunctionsService,
    aiOn: Boolean,
    progress: StudyProgressRepository,
    today: String,
    wasPeriodic: Boolean,
    onRetryWrong: () -> Unit,
    onNewQuiz: () -> Unit,
) {
    val o = outcome
    if (o == null) {
        Text("نتیجه‌ای نیست.", Modifier.padding(16.dp))
        return
    }
    val scope = rememberCoroutineScope()
    val aiTexts = remember { mutableStateMapOf<String, String>() }
    val aiBusy = remember { mutableStateMapOf<String, Boolean>() }
    val byId = pack.questions.associateBy { it.id }
    val wrong = o.results.filter { !it.correct }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${o.scorePct}٪", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${o.results.count { it.correct }} درست از ${o.results.size} سوال",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (o.weakTopics.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "موضوع‌های نیازمند مرور: ${o.weakTopics.joinToString("، ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!wasPeriodic) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "اشتباه‌های این آزمون ۷ روز بعد در «آزمون دوره‌ای» دوباره پرسیده می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (wrong.isNotEmpty()) {
            item {
                Text("سوال‌های اشتباه — ببین کجا لغزیدی:", style = MaterialTheme.typography.titleSmall)
            }
            items(wrong, key = { it.questionId }) { r ->
                val q = byId[r.questionId]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(q?.text.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "جواب تو: ${if (r.given.isBlank()) "بی‌جواب" else r.given}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "جواب درست: ${q?.answer.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (!q?.explanation.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(q!!.explanation, style = MaterialTheme.typography.bodySmall)
                        }
                        if (aiOn) {
                            Spacer(Modifier.height(8.dp))
                            if (aiTexts[r.questionId] == null) {
                                TextButton(onClick = {
                                    if (q == null || aiBusy[r.questionId] == true) return@TextButton
                                    aiBusy[r.questionId] = true
                                    scope.launch {
                                        val body = JSONObject()
                                            .put("mode", "study-tutor")
                                            .put("lessonTitle", pack.title)
                                            .put("question", q.text)
                                            .put("correctAnswer", q.answer)
                                            .put("studentAnswer", r.given)
                                            .put("sectionTitle", pack.sections.firstOrNull { it.id == q.refSectionId }?.title.orEmpty())
                                            .toString()
                                        val res = runCatching { functions.callForBody(FunctionIds.STUDY_TUTOR, body) }.getOrNull()
                                        val parsed = res?.let { parseTutorReply(it) }
                                        if (parsed != null) aiTexts[r.questionId] = parsed
                                        else aiTexts[r.questionId] = "رفع اشکال با هوش مصنوعی الان در دسترس نیست — توضیح آفلاین بالا هم کمکت می‌کند."
                                        aiBusy[r.questionId] = false
                                    }
                                }) {
                                    Text(if (aiBusy[r.questionId] == true) "در حال رفع اشکال…" else "🤖 رفع اشکال با هوش مصنوعی")
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        aiTexts[r.questionId].orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onRetryWrong, modifier = Modifier.fillMaxWidth()) {
                    Text("همین ${wrong.size} اشتباه را همین حالا دوباره امتحان کن")
                }
            }
        }

        item {
            PrimaryButton("آزمون جدید", Modifier.fillMaxWidth()) { onNewQuiz() }
        }
    }
}

/** پاسخ تابع `study-tutor` را منعطف می‌خواند: reply | text | message | رشته‌ی خام. */
private fun parseTutorReply(raw: String): String? = runCatching {
    val o = JSONObject(raw)
    val direct = listOf("reply", "text", "message", "response").firstNotNullOfOrNull { key ->
        o.optString(key).takeIf { it.isNotBlank() }
    }
    direct ?: raw
}.getOrNull() ?: raw.ifBlank { null }

// ---------------------------------------------------------------- حل تشریحی

@Composable
private fun SolutionsTab(pack: StudyPack) {
    if (pack.solutions.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("✏️ حل تشریحی این درس به‌زودی اضافه می‌شود", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "حل تمرین‌های کتاب، قدم‌به‌قدم و تصویری، از روی خود کتاب بازسازی می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    if (pack.solutions.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "برای این درس هنوز حل تشریحی ثبت نشده.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(pack.solutions, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(s.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
