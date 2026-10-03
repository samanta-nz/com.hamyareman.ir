package com.hamyareman.ir.ui.ailearning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.Lesson
import com.hamyareman.ir.ui.content.LearningNode
import com.hamyareman.ir.ui.content.QuizQuestion
import com.hamyareman.ir.ui.content.lessonStreak
import com.hamyareman.ir.ui.content.readLessonIds
import com.hamyareman.ir.ui.learning.NODE_DONE_PREFIX
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.study.readQuizAttempts
import com.hamyareman.ir.platform.core.appwrite.DailyLesson
import com.hamyareman.ir.platform.core.common.AppResult

/**
 * ماژول مجزای «آموزش هوش مصنوعی» — از پایه تا پروژه.
 *
 * از همان زیرساخت کاتالوگ استفاده می‌کند (درس/آزمون/گره با پیش‌نیاز) ولی سه چیز
 * مخصوص خودش دارد:
 *  ۱) **مسیر پیش‌نیازدار** با یک گره‌ی ریاضی از مسیر دیگر (`node-math-1`)؛
 *  ۲) **تعیین سطح و نمودار پیشرفت** مخصوص همین ماژول (گره‌ها، درس‌های خوانده‌شده، دقت آزمون).
 *
 * همه‌ی داده‌های پیشرفت روی دستگاه می‌مانند و Sync نمی‌شوند.
 */
const val AI_TRACK = "هوش مصنوعی"
const val AI_LESSON_REMINDER_ID = "ai-lesson-daily"

private const val AI_LEVEL = "ai_level"
private const val AI_SCORE = "ai_score"
private const val AI_TOTAL = "ai_total"
private const val AI_DATE = "ai_date"

@Composable
fun AiLearningHomeScreen(nav: NavController) {
    val c = LocalAppContainer.current
    var nodes by remember { mutableStateOf<List<LearningNode>>(emptyList()) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var quizCount by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val allNodes = c.catalog.learningNodes()
        val aiLessons = c.catalog.lessons().filter { it.subject == AI_TRACK }
        nodes = allNodes.filter { it.track == AI_TRACK }.sortedBy { it.orderIndex }
        lessons = aiLessons
        val ids = aiLessons.map { it.id }.toSet()
        quizCount = c.catalog.quizFor(null).count { it.lessonId in ids }
    }

    val readIds = remember(tick) { readLessonIds(c.store) }
    val lessonsRead = lessons.count { it.id in readIds }
    val nodesDone = nodes.count { c.store.getString(NODE_DONE_PREFIX + it.id).isNotEmpty() }
    val aiAttempts = remember(tick) {
        val ids = lessons.map { it.id }.toSet()
        readQuizAttempts(c.store).filter { it.lessonId in ids }
    }
    val answered = aiAttempts.sumOf { it.total }
    val correct = aiAttempts.sumOf { it.score }
    val accuracy = if (answered == 0) 0 else (correct * 100) / answered
    val level = c.store.getString(AI_LEVEL)

    // اولین گره‌ی بازِ انجام‌نشده = «ادامه بده از اینجا»
    val nextNode = nodes.firstOrNull { node ->
        c.store.getString(NODE_DONE_PREFIX + node.id).isEmpty() &&
            (node.isUnlocked || c.store.getString(NODE_DONE_PREFIX + node.prerequisiteId).isNotEmpty())
    }
    // «درس امروز»: اولویت با انتخاب **سمت سرور** است (تابع lesson-of-the-day) چون
    // ترتیب و پیش‌نیازها از کاتالوگ سرور خوانده می‌شود، دو دستگاه یک درس را می‌بینند و
    // ساعت دستگاه (که قابل تغییر است) نقشی ندارد. اگر سرور در دسترس نبود، همان
    // انتخاب محلی قبلی بر اساس روز سال می‌ماند ⇒ صفحه هیچ‌وقت خالی نمی‌شود.
    var serverLesson by remember { mutableStateOf<DailyLesson?>(null) }
    LaunchedEffect(lessons) {
        if (lessons.isEmpty()) return@LaunchedEffect
        val done = readIds + nodes
            .filter { c.store.getString(NODE_DONE_PREFIX + it.id).isNotEmpty() }
            .map { it.id }
        serverLesson = (c.serverActions.lessonOfDay(AI_TRACK, done) as? AppResult.Ok<DailyLesson>)?.value
    }
    val localLesson = remember(lessons) {
        if (lessons.isEmpty()) null else lessons[(LocalDate.now().dayOfYear - 1).coerceAtLeast(0) % lessons.size]
    }
    val todayLessonId = serverLesson?.lessonId?.ifBlank { null } ?: localLesson?.id
    val todayLessonTitle = serverLesson?.title?.ifBlank { null } ?: localLesson?.title

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آموزش هوش مصنوعی") { nav.popBackStack() }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "از «هوش مصنوعی چیست» تا «ساختن دستیار کوچک» — با پیش‌نیاز، آزمون و پیشرفت واقعی.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionCard(
                title = if (level.isBlank()) "تعیین سطح نداده‌ای" else "سطح تو: $level",
                body = if (level.isBlank()) {
                    "شش سؤال کوتاه تا بدانیم از کجا شروع کنی."
                } else {
                    "${c.store.getInt(AI_SCORE)} از ${c.store.getInt(AI_TOTAL)} درست · " +
                        JalaliDate.formatFaLong(c.store.getString(AI_DATE))
                },
            ) { nav.navigate(Screen.AiAssessment.route) }

            if (todayLessonId != null && todayLessonTitle != null) {
                val server = serverLesson
                SectionCard(
                    title = "درس امروز: $todayLessonTitle",
                    body = when {
                        server != null -> buildString {
                            if (server.review) append("مرور: همه‌ی درس‌ها را خوانده‌ای. ")
                            if (server.total > 0) {
                                append("جای تو در این مسیر: ")
                                append(toPersianDigits(server.index.toString()))
                                append(" از ")
                                append(toPersianDigits(server.total.toString()))
                                append(" · ")
                            }
                            if (server.quizCount > 0) {
                                append(toPersianDigits(server.quizCount.toString()))
                                append(" پرسش دارد")
                                if (todayLessonId in readIds) append(" — خواندی ✅")
                                append(".")
                            } else if (todayLessonId in readIds) {
                                append("خواندی ✅ — دوباره مرور کن.")
                            } else {
                                append("توضیح + مثال + تمرین؛ حدود ۵ دقیقه.")
                            }
                        }
                        todayLessonId in readIds -> "خواندی ✅ — دوباره مرور کن یا برو آزمون بده."
                        else -> "توضیح + مثال + تمرین؛ حدود ۵ دقیقه."
                    },
                ) { nav.navigate(Screen.Lesson.of(todayLessonId)) }
            }
            if (nextNode != null) {
                SectionCard(
                    title = "ادامه از: ${nextNode.title}",
                    body = "نقشه‌ی راه با قفل پیش‌نیاز؛ هر گره که انجام شد، بعدی باز می‌شود.",
                ) { nav.navigate(Screen.Roadmap.of(AI_TRACK)) }
            }

            Spacer(Modifier.height(2.dp))
            Text("پیشرفت", style = MaterialTheme.typography.titleMedium)
            ProgressRow(
                label = "گره‌های مسیر",
                done = nodesDone,
                total = nodes.size,
                progress = { nodesDone.toFloat() / nodes.size.coerceAtLeast(1) },
            )
            ProgressRow(
                label = "درس‌های خوانده‌شده",
                done = lessonsRead,
                total = lessons.size,
                progress = { lessonsRead.toFloat() / lessons.size.coerceAtLeast(1) },
            )
            SectionCard(
                title = "آزمون‌ها",
                body = "$quizCount سؤال در این مسیر · پاسخ داده‌شده: $answered · دقت: " +
                    (if (answered == 0) "هنوز آماری نداری" else "${toPersianDigits(accuracy.toString())}٪"),
            ) { if (localLesson != null) nav.navigate(Screen.Quiz.of(localLesson.id)) }
            SectionCard(
                title = "استریک مطالعه: ${lessonStreak(c.store)} روز",
                body = "یک درس پنج‌دقیقه‌ای هم حساب است؛ روزِ از دست رفته استریک را می‌شکند، نه روحیه‌ات را.",
            ) { }

            Spacer(Modifier.height(2.dp))
            Text("همه‌ی درس‌های این مسیر", style = MaterialTheme.typography.titleMedium)
            if (lessons.isEmpty()) Text("درس‌ها در حال آماده‌سازی‌اند…", style = MaterialTheme.typography.bodySmall)
            lessons.forEach { lesson ->
                SectionCard(
                    title = lesson.title,
                    body = if (lesson.id in readIds) "خوانده‌شده ✅" else "هنوز نخواندی — ۵ دقیقه",
                ) { nav.navigate(Screen.Lesson.of(lesson.id)) }
            }

        }
    }
}

@Composable
private fun ProgressRow(
    label: String,
    done: Int,
    total: Int,
    progress: () -> Float,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                toPersianDigits("$done از $total"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * تعیین سطح هوش مصنوعی: شش سؤال پخش‌شده از بانک سؤال همین مسیر.
 * نتیجه فقط روی دستگاه می‌ماند و نقطه‌ی شروع نقشه‌ی راه را پیشنهاد می‌دهد.
 */
@Composable
fun AiAssessmentScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var result by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val lessonIds = c.catalog.lessons().filter { it.subject == AI_TRACK }.map { it.id }.toSet()
        val all = c.catalog.quizFor(null).filter { it.lessonId in lessonIds }
        questions = if (all.size <= 6) all.shuffled() else {
            val stride = all.size / 6.0
            (0 until 6).map { all[(it * stride).toInt().coerceIn(0, all.lastIndex)] }.distinctBy { it.id }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تعیین سطح AI", onBack)
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
                Text("نتیجه روی دستگاهت ذخیره شد؛ نقطه‌ی شروع نقشه‌ی راه هم بر همان اساس پیشنهاد می‌شود.")
                PrimaryButton("بازگشت", onBack)
                return@Column
            }
            if (questions.isEmpty()) {
                Text("سؤال‌ها در حال آماده‌سازی‌اند…", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val q = questions[index.coerceAtMost(questions.lastIndex)]
            LinearProgressIndicator(progress = { (index + 1f) / questions.size }, modifier = Modifier.fillMaxWidth())
            Text("سؤال ${toPersianDigits("${index + 1}")} از ${toPersianDigits("${questions.size}")}", style = MaterialTheme.typography.bodySmall)
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
                            score * 3 >= total * 2 -> "پیشرفته"
                            score * 3 >= total -> "در حال رشد"
                            else -> "مبتدی"
                        }
                        c.store.putString(AI_LEVEL, level)
                        c.store.putInt(AI_SCORE, score)
                        c.store.putInt(AI_TOTAL, total)
                        c.store.putString(AI_DATE, JalaliDate.todayIso())
                        result = "سطح تو: $level ($score از $total درست)"
                    }
                }
            }
        }
    }
}
