package com.hamyareman.ir.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.wellness.WellnessMove

/**
 * «تمرینات مخصوص این دوره» — صفحهٔ مخصوصِ روزهای چرخهٔ ماهانه.
 *
 * چهار سربرگ: یوگا · تمرینات تنفسی · کنترل درد · آرامش.
 * در هر سربرگ **فقط** حرکاتِ ملایم و تمرین‌های مختصِ همین دوره می‌آید؛
 * حرکاتِ سنگین/ورزشی این‌جا نیستند.
 */
@Composable
fun PeriodTrainingScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var tab by remember { mutableIntStateOf(0) }
    var moves by remember { mutableStateOf<List<WellnessMove>>(emptyList()) }
    var expanded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        moves = runCatching { container.wellnessMoves.list() }.getOrDefault(emptyList())
    }

    // ---- دادهٔ هر سربرگ: فقط ملایم‌ها ----
    val yoga = remember(moves) {
        val prefer = setOf("yoga-cat-cow", "yoga-balasana", "yoga-spinal-twist", "yoga-wide-child")
        moves.filter { it.slug in prefer }.ifEmpty {
            moves.filter { it.category == WellnessMove.Category.YOGA && it.level <= 4 }
        }
    }
    val breathing = remember(moves) {
        val prefer = setOf("breath-diaphragm", "breath-4-7-8")
        moves.filter { it.slug in prefer }.ifEmpty {
            moves.filter { it.category == WellnessMove.Category.BREATHING }
        }
    }


    Column(Modifier.fillMaxSize()) {
        AppTopBar("تمرینات مشترکِ چرخه‌ی پریود", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("یوگا", "تنفس کرامپ", "کشش", "تغذیه").forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i; expanded = null },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) })
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "این فهرست مخصوصِ همین روزهای چرخه است: فقط حرکاتِ ملایم و تمرین‌های سبک. " +
                        "هر جا درد بیشتر شد، همان‌جا بایست؛ اگر درد شدید یا همراه با خون‌ریزیِ زیاد بود، " +
                        "به مامان/بابا بگو و با پزشک مشورت کن.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when (tab) {
                0 -> {
                    MoveSection(
                        emptyText = "فهرست حرکاتِ یوگا الان در دسترس نیست؛ بعداً دوباره سر بزن.",
                        moves = yoga,
                        expanded = expanded,
                        onToggle = { expanded = if (expanded == it) null else it })
                    com.hamyareman.ir.ui.wellness.WellnessMenu.group("pd-yoga")?.items
                        ?.filter { it.wellnessSlug.isBlank() }
                        ?.forEach { item ->
                            InfoCard(
                                emoji = item.emoji,
                                title = item.title,
                                meta = item.minutes,
                                body = item.body.ifBlank { item.steps.joinToString(" ") })
                        }
                }

                1 -> MoveSection(
                    emptyText = "تمرین‌های تنفسی الان در دسترس نیست؛ بعداً دوباره سر بزن.",
                    moves = breathing,
                    expanded = expanded,
                    onToggle = { expanded = if (expanded == it) null else it })

                2 -> {
                    Text(
                        "کشش‌های هدفمند — کمر، لگن و گرما:",
                        style = MaterialTheme.typography.titleSmall)
                    MonthlyCycle.painExercises.forEach { ex ->
                        InfoCard(
                            emoji = ex.emoji,
                            title = ex.title,
                            meta = ex.duration,
                            body = ex.how)
                    }
                    com.hamyareman.ir.ui.wellness.WellnessMenu.group("pd-stretch")?.items?.forEach { item ->
                        InfoCard(
                            emoji = item.emoji,
                            title = item.title,
                            meta = item.minutes,
                            body = item.body.ifBlank { item.steps.joinToString(" ") })
                    }
                }

                else -> {
                    Text(
                        "راهنمای تغذیه‌ی هر فاز — آهن، منیزیم، آب بیشتر:",
                        style = MaterialTheme.typography.titleSmall)
                    com.hamyareman.ir.ui.wellness.WellnessMenu.group("pd-food")?.items?.forEach { item ->
                        InfoCard(
                            emoji = item.emoji,
                            title = item.title,
                            meta = item.minutes.ifBlank { "راهنما" },
                            body = item.body)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/** فهرستِ حرکاتِ سلامتی با متنِ راهنما (لمس = باز و بسته شدنِ توضیح). */
@Composable
private fun MoveSection(
    emptyText: String,
    moves: List<WellnessMove>,
    expanded: String?,
    onToggle: (String) -> Unit) {
    if (moves.isEmpty()) {
        if (emptyText.isNotBlank()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        moves.forEach { m ->
            val open = expanded == m.slug
            Card(
                Modifier.fillMaxWidth().clickable { onToggle(m.slug) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧘", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(m.titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            toPersianDigits(m.totalSeconds.toString()) + " ثانیه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (m.instructionsFa.isNotBlank() && open) {
                        Text(m.instructionsFa, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "برای دیدنِ راهنما بزن",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** کارتِ سادهٔ «ایموجی + عنوان + مدت + توضیح». */
@Composable
private fun InfoCard(emoji: String, title: String, meta: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


