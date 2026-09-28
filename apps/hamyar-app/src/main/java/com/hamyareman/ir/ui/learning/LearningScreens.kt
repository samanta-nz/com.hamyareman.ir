package com.hamyareman.ir.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.LearningNode
import com.hamyareman.ir.ui.content.Lesson
import com.hamyareman.ir.ui.content.markLessonRead
import com.hamyareman.ir.ui.content.QuizQuestion
import com.hamyareman.ir.ui.navigation.Screen
import java.time.LocalDate

/** کلید «انجام شد» هر گره — ماژول هوش مصنوعی هم از همین برای پیشرفت استفاده می‌کند. */
internal const val NODE_DONE_PREFIX = "node_done_"
private const val PLACEMENT_LEVEL = "placement_level"
private const val PLACEMENT_SCORE = "placement_score"
private const val PLACEMENT_TOTAL = "placement_total"
private const val PLACEMENT_DATE = "placement_date"

@Composable
fun LearningHomeScreen(nav: NavController) {
    val container = LocalAppContainer.current
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }

    LaunchedEffect(Unit) { lessons = container.catalog.lessons() }

    // «جلسه‌ی امروز» بر اساس روز سال: در یک روز ثابت می‌ماند و هر روز عوض می‌شود.
    val todayLesson: Lesson? = remember(lessons) {
        if (lessons.isEmpty()) null else lessons[(LocalDate.now().dayOfYear - 1).coerceAtLeast(0) % lessons.size]
    }
    val level = container.store.getString(PLACEMENT_LEVEL)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("آموزش آزاد", style = MaterialTheme.typography.titleLarge)
        SectionCard(
            "آزمون تعیین سطح",
            if (level.isBlank()) "مبتدی / آشنا / نیمه‌حرفه‌ای" else "سطح فعلی تو: $level — می‌توانی دوباره بدهی",
        ) { nav.navigate(Screen.Placement.route) }
        SectionCard("نقشه راه", "گره‌ها با پیش‌نیاز؛ قفل تا وقتی قبلی انجام نشده") {
            nav.navigate(Screen.Roadmap.of())
        }
        SectionCard("آموزش هوش مصنوعی", "از پایه تا پروژه — با درس روزانه، آزمون و نمودار پیشرفت") {
            nav.navigate(Screen.AiLearning.route)
        }
        if (todayLesson != null) {
            SectionCard(
                "جلسه‌ی امروز: ${todayLesson.title}",
                "${todayLesson.subject} · توضیح → مثال → سؤال → تمرین",
            ) { nav.navigate(Screen.Lesson.of(todayLesson.id)) }
        }

        Text("همه‌ی درس‌ها", style = MaterialTheme.typography.titleMedium)
        lessons.groupBy { it.subject }.forEach { (subject, items) ->
            items.forEach { lesson ->
                SectionCard(lesson.title, subject + if (lesson.grade > 0) " · پایه ${lesson.grade}" else "") {
                    nav.navigate(Screen.Lesson.of(lesson.id))
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun LessonScreen(lessonId: String, onBack: () -> Unit, onQuiz: (String) -> Unit) {
    val container = LocalAppContainer.current
    var lesson by remember { mutableStateOf<Lesson?>(null) }

    LaunchedEffect(lessonId) {
        lesson = container.catalog.lesson(lessonId)
        // «خوانده‌شدن» درس؛ پایه‌ی نمودار پیشرفت و استریک مطالعه.
        if (lesson != null) markLessonRead(container.store, lessonId)
    }

    val current = lesson ?: return

    // پرامپت ۰۱: اگر درس رسانه (ویدیو/صوت) دارد → صفحه‌ی پلیر اختصاصی نشان داده شود.
    if (current.videoUrl.isNotBlank() || current.audioUrl.isNotBlank()) {
        LessonPlayerScreen(lesson = current, onBack = onBack)
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(current.title, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                current.subject + if (current.grade > 0) " · پایه ${current.grade}" else "",
                style = MaterialTheme.typography.bodySmall,
            )
            current.body.split("\n").filter { it.isNotBlank() }.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyLarge)
            }
            PrimaryButton("آزمون این درس") { onQuiz(current.id) }
            PrimaryButton("بازگشت", onBack)
        }
    }
}

/**
 * تعیین سطح: نمونه‌ای از سؤال‌های کاتالوگ (پخش‌شده بین درس‌ها) پرسیده می‌شود
 * و نتیجه روی دستگاه ذخیره می‌شود تا نقشه‌ی راه از همان‌جا شروع کند.
 */
@Composable
fun PlacementTestScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var result by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val all = container.catalog.quizFor(null)
        questions = if (all.size <= 5) all else {
            val stride = all.size / 5.0
            (0 until 5).map { all[(it * stride).toInt().coerceIn(0, all.lastIndex)] }.distinctBy { it.id }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تعیین سطح", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shown = result
            if (shown != null) {
                Text(shown, style = MaterialTheme.typography.titleMedium)
                Text("نتیجه روی دستگاهت ذخیره شد؛ هر وقت خواستی دوباره امتحان بده.")
                PrimaryButton("بازگشت", onBack)
                return@Column
            }
            if (questions.isEmpty()) {
                Text("سؤال‌ها در حال آماده‌سازی‌اند…")
                return@Column
            }

            val q = questions[index.coerceAtMost(questions.lastIndex)]
            Text("سؤال ${index + 1} از ${questions.size}", style = MaterialTheme.typography.bodySmall)
            Text(q.question, style = MaterialTheme.typography.titleMedium)
            q.choices.forEachIndexed { i, choice ->
                val chosen = picked
                val label = when {
                    chosen == null -> choice
                    i == q.answerIndex -> "$choice ✅"
                    i == chosen -> "$choice ❌"
                    else -> choice
                }
                PrimaryButton(label) {
                    if (chosen == null) {
                        picked = i
                        if (i == q.answerIndex) score++
                    }
                }
            }
            if (picked != null) {
                PrimaryButton(if (index < questions.lastIndex) "سؤال بعدی" else "دیدن نتیجه") {
                    picked = null
                    if (index < questions.lastIndex) {
                        index++
                    } else {
                        val total = questions.size
                        val level = when {
                            score * 3 >= total * 2 -> "نیمه‌حرفه‌ای"
                            score * 3 >= total -> "آشنا"
                            else -> "مبتدی"
                        }
                        container.store.putString(PLACEMENT_LEVEL, level)
                        container.store.putInt(PLACEMENT_SCORE, score)
                        container.store.putInt(PLACEMENT_TOTAL, total)
                        container.store.putString(PLACEMENT_DATE, JalaliDate.todayIso())
                        result = "سطح تو: $level ($score از $total درست)"
                    }
                }
            }
        }
    }
}

/**
 * نقشه‌ی راه با گره‌های دارای پیش‌نیاز.
 *
 * [trackFilter] که داده شود، فقط همان مسیر نشان داده می‌شود (ماژول هوش مصنوعی
 * از همین راه فقط گره‌های خودش را نشان می‌دهد).
 */
@Composable
fun RoadmapScreen(onBack: () -> Unit, trackFilter: String = "") {
    val container = LocalAppContainer.current
    var nodes by remember { mutableStateOf<List<LearningNode>>(emptyList()) }
    var doneTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val all = container.catalog.learningNodes()
        nodes = if (trackFilter.isBlank()) all else all.filter { it.track == trackFilter }
    }

    val doneIds = remember(doneTick) {
        container.store.keysWithPrefix(NODE_DONE_PREFIX)
            .map { it.removePrefix(NODE_DONE_PREFIX) }.toSet()
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (trackFilter.isBlank()) "نقشه راه" else "نقشه راه: $trackFilter", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "${doneIds.size} از ${nodes.size} گره انجام شده — بدون فشار، هر گره یک قدم است.",
                style = MaterialTheme.typography.bodyMedium,
            )

            nodes.groupBy { it.track }.forEach { (track, items) ->
                Text(track, style = MaterialTheme.typography.titleMedium)
                items.sortedBy { it.orderIndex }.forEachIndexed { position, node ->
                    val isDone = node.id in doneIds
                    val unlocked = node.isUnlocked || (node.prerequisiteId in doneIds) ||
                        items.getOrNull(position - 1)?.id in doneIds
                    val state = when {
                        isDone -> "انجام شد ✅"
                        unlocked -> "باز — می‌توانی شروع کنی"
                        else -> "قفل تا پیش‌نیاز"
                    }
                    SectionCard("${node.orderIndex}. ${node.title}", state) {
                        if (unlocked && !isDone) {
                            container.store.putString(NODE_DONE_PREFIX + node.id, JalaliDate.todayIso())
                            doneTick++
                        }
                    }
                }
            }

            if (nodes.isEmpty()) Text("گره‌ها در حال آماده‌سازی‌اند…")
        }
    }
}
