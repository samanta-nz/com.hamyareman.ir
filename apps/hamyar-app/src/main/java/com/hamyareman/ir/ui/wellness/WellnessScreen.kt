package com.hamyareman.ir.ui.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch

private fun wellnessImageAspectRatio(move: WellnessMove): Float = when (move.category) {
    WellnessMove.Category.EXERCISE -> 1120f / 751f
    WellnessMove.Category.YOGA -> {
        val landscape751 = setOf("yga-01", "yga-02", "yga-03", "yga-05", "yga-06")
        val id = move.referenceImageUrl.substringAfterLast('/').substringBefore('.')
        if (id in landscape751) 1120f / 751f else 1120f / 625f
    }
    else -> 4f / 3f
}

/**
 * یک دسته در هر صفحه: یوگا / ورزش / تنفس / یادگیری — بدون سربرگ «همه».
 */
@Composable
fun WellnessScreen(
    onBack: () -> Unit,
    onSketchGallery: () -> Unit,
    initialCategory: String? = null) {
    val container = LocalAppContainer.current
    var moves by remember { mutableStateOf<List<WellnessMove>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val selectedCategory = remember(initialCategory) {
        WellnessMove.Category.fromWire(initialCategory ?: "yoga")
    }
    var activeMove by remember { mutableStateOf<WellnessMove?>(null) }

    LaunchedEffect(Unit) {
        moves = container.wellnessMoves.list()
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(
            when (selectedCategory) {
                WellnessMove.Category.YOGA -> "یوگا"
                WellnessMove.Category.EXERCISE -> "ورزش عمومی"
                WellnessMove.Category.BREATHING -> "تنفس آرام‌بخش"
                WellnessMove.Category.LEARNING -> "تمرینات تمرکز و یادگیری"
            },
            onBack)
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (activeMove != null) {
            ActiveMoveView(
                move = activeMove!!,
                onClose = { activeMove = null })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val filtered = moves.filter { it.category == selectedCategory }
                items(filtered, key = { it.slug }) { move ->
                    MoveCard(move = move, onStart = { activeMove = move })
                }
            }
        }
    }
}

@Composable
private fun MoveCard(move: WellnessMove, onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (move.referenceImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = move.referenceImageUrl,
                        contentDescription = move.titleFa,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(wellnessImageAspectRatio(move))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface))
                } else {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center) {
                        Text(move.category.displayFa.take(1), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(move.titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append("${move.durationSec} ثانیه")
                            if (move.reps > 0) append(" · ${move.reps} تکرار")
                            append(" · سطح ${move.level}")
                        },
                        style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(move.intensity.name.lowercase()) },
                            colors = AssistChipDefaults.assistChipColors())
                        move.tags.firstOrNull()?.let { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag) },
                                colors = AssistChipDefaults.assistChipColors())
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(move.instructionsFa, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "شروع جلسه", onClick = onStart, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ActiveMoveView(move: WellnessMove, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val timing = remember(move) { container.wellnessTiming.timingFor(move) }
    val timer = remember(move) {
        WellnessTimer(
            context = context,
            timingProvider = container.wellnessTiming,
            wellnessLogSink = object : WellnessTimer.WellnessLogSink {
                override suspend fun logSession(m: WellnessMove, secondsSpent: Int, completed: Boolean) {
                    val dayIso = java.time.LocalDate.now().toString()
                    container.wellnessLogs.log(
                        userId = container.auth.cachedUserId() ?: "",
                        move = m,
                        secondsSpent = secondsSpent,
                        completed = completed,
                        dayIso = dayIso)
                }
            })
    }
    DisposableEffect(timer) { onDispose { timer.release() } }
    LaunchedEffect(move) { timer.start(move) }

    val state by timer.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // تصویر مرجع
        if (move.referenceImageUrl.isNotBlank()) {
            AsyncImage(
                model = move.referenceImageUrl,
                contentDescription = move.titleFa,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(wellnessImageAspectRatio(move))
                    .clip(RoundedCornerShape(12.dp)))
        }

        // عنوان و سطح
        Text(move.titleFa, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(move.instructionsFa, style = MaterialTheme.typography.bodyMedium)

        // تایمر بصری
        val step = timing.steps.getOrNull(state.currentStepIndex)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${state.stepSecondsLeft} از ${state.stepSecondsTotal} ثانیه",
                    style = MaterialTheme.typography.titleMedium)
                Text("گام ${state.currentStepIndex + 1} از ${timing.steps.size}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (state.stepSecondsTotal - state.stepSecondsLeft).toFloat() / state.stepSecondsTotal.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            }
        }

        // نوار کلی پیشرفت
        Text(
            "پیشرفت کلی: ${state.totalSecondsElapsed} از ${state.totalSecondsPlanned} ثانیه",
            style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { state.totalSecondsElapsed.toFloat() / state.totalSecondsPlanned.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth())

        // دکمه‌ها
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.status == WellnessTimer.Status.RUNNING) {
                PrimaryButton("توقف", onClick = { timer.pause() }, modifier = Modifier.weight(1f))
            } else {
                PrimaryButton("ادامه", onClick = { timer.resume() }, modifier = Modifier.weight(1f))
            }
            PrimaryButton("خروج", onClick = { timer.stop(); onClose() }, modifier = Modifier.weight(1f))
        }
    }
}

/** نگه‌دارنده‌ی Context برای استفاده در جاهایی که @Composable نیست. */
private object LocalContextHolder

