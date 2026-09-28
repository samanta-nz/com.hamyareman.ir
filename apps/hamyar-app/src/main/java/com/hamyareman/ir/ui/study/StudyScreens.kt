package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.QuizQuestion
import com.hamyareman.ir.ui.navigation.Screen
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor


@Composable
fun StudyHomeScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("درس", style = MaterialTheme.typography.titleLarge)
        SectionCard("برنامه مدرسه و شیفت", "چرخه‌ی دوهفته‌ای صبح/عصر.") { nav.navigate(Screen.School.route) }
        SectionCard("آزمون‌ساز", "از روی درس، سؤال ۴گزینه‌ای.") { nav.navigate(Screen.Quiz.of()) }
        SectionCard("مرور غلط‌ها", "فاصله‌دار، بدون تنبیه.") { nav.navigate(Screen.QuizReview.route) }
        SectionCard("کتابخانه", "ژانر را خودت انتخاب می‌کنی.") { nav.navigate(Screen.Library.route) }
        SectionCard("کتاب صوتی", "پخش، بوکمارک، تایمر خواب.") { nav.navigate(Screen.Audiobook.route) }
        SectionCard("جزوه‌های شخصی و آزمونی", "دفتر نکات و گالری فایل روی گوشی.") { nav.navigate(Screen.Pdf.route) }
        SectionCard("نمودار پیشرفت", "فقط تشویق داده‌محور.") { nav.navigate(Screen.Charts.of(null)) }
        SectionCard("آموزش آزاد", "از مبتدی تا حرفه‌ای.") { nav.navigate(Screen.Learning.route) }
    }
}

@Composable
fun QuizScreen(lessonId: String, onBack: () -> Unit, onReview: () -> Unit) {
    val container = LocalAppContainer.current
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished) recordQuizAttempt(container.store, lessonId, score, questions.size)
    }

    LaunchedEffect(lessonId) {
        val all = container.catalog.quizFor(lessonId.ifBlank { null })
        // اگر برای این درس سؤال نبود، از کل بانک سؤال نمونه می‌گیریم.
        questions = (if (all.isNotEmpty()) all else container.catalog.quizFor(null)).shuffled().take(6)
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آزمون", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                questions.isEmpty() -> Text("سؤال‌ها در حال آماده‌سازی‌اند…")

                finished -> {
                    Text("تمام شد: $score از ${questions.size} درست", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "غلط‌ها رفتند توی «مرور فاصله‌دار» — بدون تنبیه، فقط تکرار هوشمند.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton("مرور غلط‌ها", onReview)
                    PrimaryButton("بازگشت", onBack)
                }

                else -> {
                    val q = questions[index.coerceAtMost(questions.lastIndex)]
                    LinearProgressIndicator(progress = { (index + 1f) / questions.size })
                    Spacer(Modifier.height(6.dp))
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
                                if (i == q.answerIndex) score++ else scheduleWrong(container.store, q)
                            }
                        }
                    }
                    if (picked != null) {
                        Text(
                            if (picked == q.answerIndex) "درست بود ✅" else "اشکالی ندارد — فردا دوباره مرور می‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        PrimaryButton(if (index < questions.lastIndex) "سؤال بعدی" else "پایان آزمون") {
                            picked = null
                            if (index < questions.lastIndex) index++ else finished = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizReviewScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var due by remember { mutableStateOf(dueItems(container.store)) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var reviewed by remember { mutableIntStateOf(0) }
    var grew by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("مرور فاصله‌دار", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "اگر درست جواب بدی فاصله ۲.۵ برابر می‌شود (سقف $MAX_INTERVAL_DAYS روز). اگر نه، فردا دوباره.",
                style = MaterialTheme.typography.bodyMedium,
            )

            val item = due.firstOrNull()
            if (item == null) {
                SectionCard(
                    title = if (reviewed == 0) "امروز چیزی برای مرور نیست" else "مرور امروز تمام شد ✅",
                    body = "هر سؤالی که در آزمون غلط بزنی، فردا اینجا می‌آید.",
                ) { }
                if (reviewed > 0) Text("$reviewed سؤال مرور شد؛ $grew مورد فاصله‌اش بلندتر شد.")
                PrimaryButton("بازگشت", onBack)
                return@Column
            }

            Text(
                "سررسید: ${JalaliDate.formatFaLong(item.dueIso)} — فاصله‌ی فعلی ${item.intervalDays} روز",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(item.question, style = MaterialTheme.typography.titleMedium)
            item.choices.forEachIndexed { i, choice ->
                val chosen = picked
                val label = when {
                    chosen == null -> choice
                    i == item.answerIndex -> "$choice ✅"
                    i == chosen -> "$choice ❌"
                    else -> choice
                }
                PrimaryButton(label) {
                    if (chosen == null) {
                        picked = i
                        if (i == item.answerIndex) {
                            growInterval(container.store, item)
                            grew++
                        } else {
                            resetInterval(container.store, item)
                        }
                    }
                }
            }
            if (picked != null) {
                Text(
                    if (picked == item.answerIndex) "آفرین — دفعه‌ی بعد دیرتر می‌پرسیم." else "فردا دوباره؛ بدون تنبیه.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PrimaryButton("مرور بعدی") {
                    picked = null
                    reviewed++
                    due = dueItems(container.store)
                }
            }
        }
    }
}
