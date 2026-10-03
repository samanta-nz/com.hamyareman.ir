package com.hamyareman.ir.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.hub.DailyHealthSnapshot
import com.hamyareman.ir.ui.hub.RoutineActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

private val defaultRoutine = listOf(
    RoutineActivity("wake", "بیدارشدن و آماده‌شدن", "شخصی", 7 * 60, 30),
    RoutineActivity("school", "مدرسه", "تحصیل", 8 * 60, 300),
    RoutineActivity("study", "درس و مرور", "تحصیل", 15 * 60, 60),
    RoutineActivity("sport", "ورزش / کشش", "سلامت", 17 * 60, 30),
    RoutineActivity("free", "وقت آزاد", "تفریح", 18 * 60, 60),
    RoutineActivity("sleep", "آرام‌سازی و خواب", "خواب", 22 * 60, 30),
)

@Composable
fun RoutineScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.dailyHealth
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(repo.snapshot()) }
    var filter by remember { mutableStateOf("همه") }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineActivity?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        busy = true
        snapshot = repo.pullToday()
        if (snapshot.routine.isEmpty()) {
            snapshot = repo.saveRoutine(defaultRoutine, actionTitle = "روتین پیش‌فرض امروز ساخته شد")
        }
        repo.syncNow()
        snapshot = repo.snapshot()
        busy = false
    }

    fun saveRoutine(items: List<RoutineActivity>, action: String) {
        snapshot = repo.saveRoutine(items, actionTitle = action)
        scope.launch(Dispatchers.IO) { runCatching { repo.syncNow() } }
    }

    LaunchedEffect(Unit) { refresh() }

    val categories = listOf("همه") + snapshot.routine.map { it.category }.distinct()
    val visible = if (filter == "همه") snapshot.routine else snapshot.routine.filter { it.category == filter }
    val progress = if (snapshot.routineTotal == 0) 0f else snapshot.routineDone.toFloat() / snapshot.routineTotal

    Scaffold(
        topBar = { AppTopBar("روتین امروز", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("برنامهٔ قابل ویرایش امروز", style = MaterialTheme.typography.titleLarge)
                            Text(
                                snapshot.routineDone.toString() + " از " + snapshot.routineTotal + " فعالیت انجام شده",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = snapshot.lightDay,
                            onCheckedChange = {
                                snapshot = repo.setLightDay(it)
                                scope.launch(Dispatchers.IO) { runCatching { repo.syncNow() } }
                            },
                        )
                    }
                    Text(if (snapshot.lightDay) "روز سبک فعال است" else "روز معمولی فعال است", style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editing = null; editorOpen = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("افزودن فعالیت")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { refresh() } },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (busy) "در حال سینک…" else "دریافت و ارسال")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(categories, key = { it }) { category ->
                    FilterChip(
                        selected = filter == category,
                        onClick = { filter = category },
                        label = { Text(category) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id }) { activity ->
                    RoutineItemCard(
                        item = activity,
                        onToggle = {
                            val updated = snapshot.routine.map {
                                if (it.id == activity.id) it.copy(done = !it.done) else it
                            }
                            saveRoutine(updated, if (activity.done) "فعالیت بازگردانده شد" else "فعالیت انجام شد")
                        },
                        onEdit = { editing = activity; editorOpen = true },
                        onDelete = {
                            saveRoutine(snapshot.routine.filterNot { it.id == activity.id }, "فعالیت حذف شد")
                        },
                        onDuplicate = {
                            val copy = activity.copy(
                                id = "routine_" + System.currentTimeMillis(),
                                title = activity.title + " (کپی)",
                                done = false,
                            )
                            saveRoutine(snapshot.routine + copy, "فعالیت کپی شد")
                        },
                        onMoveUp = {
                            val list = snapshot.routine.toMutableList()
                            val i = list.indexOfFirst { it.id == activity.id }
                            if (i > 0) {
                                val a = list.removeAt(i)
                                list.add(i - 1, a)
                                saveRoutine(list, "ترتیب روتین تغییر کرد")
                            }
                        },
                        onMoveDown = {
                            val list = snapshot.routine.toMutableList()
                            val i = list.indexOfFirst { it.id == activity.id }
                            if (i in 0 until list.lastIndex) {
                                val a = list.removeAt(i)
                                list.add(i + 1, a)
                                saveRoutine(list, "ترتیب روتین تغییر کرد")
                            }
                        },
                    )
                }
            }
        }
    }

    if (editorOpen) {
        RoutineEditorDialog(
            existing = editing,
            onDismiss = { editorOpen = false },
            onSave = { item ->
                val next = if (editing == null) {
                    snapshot.routine + item.copy(id = "routine_" + System.currentTimeMillis())
                } else {
                    snapshot.routine.map { if (it.id == editing!!.id) item.copy(id = editing!!.id) else it }
                }
                saveRoutine(next, if (editing == null) "فعالیت تازه اضافه شد" else "فعالیت ویرایش شد")
                editorOpen = false
                editing = null
            },
        )
    }
}

@Composable
private fun RoutineItemCard(
    item: RoutineActivity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = item.done, onCheckedChange = { onToggle() })
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatMinute(item.startMinute) + " · " + item.durationMinutes + " دقیقه · " + item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "ویرایش") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "بالا") }
                IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "پایین") }
                IconButton(onClick = onDuplicate) { Icon(Icons.Default.ContentCopy, contentDescription = "کپی") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
            }
        }
    }
}

@Composable
private fun RoutineEditorDialog(
    existing: RoutineActivity?,
    onDismiss: () -> Unit,
    onSave: (RoutineActivity) -> Unit,
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "شخصی") }
    var time by remember(existing) { mutableStateOf(existing?.let(::formatMinute) ?: "18:00") }
    var duration by remember(existing) { mutableStateOf(existing?.durationMinutes?.toString() ?: "20") }
    val categoryOptions = listOf("شخصی", "تحصیل", "سلامت", "تفریح", "خواب")
    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن فعالیت" else "ویرایش فعالیت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان فعالیت") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ساعت شروع (مثلاً 18:00)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مدت (دقیقه)") },
                    singleLine = true,
                )
                Text("دسته", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryOptions.forEach {
                        FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) })
                    }
                }
                Text("مدت‌های آماده", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationOptions.take(4).forEach {
                        FilterChip(
                            selected = duration.toIntOrNull() == it,
                            onClick = { duration = it.toString() },
                            label = { Text(it.toString()) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    durationOptions.drop(4).forEach {
                        FilterChip(
                            selected = duration.toIntOrNull() == it,
                            onClick = { duration = it.toString() },
                            label = { Text(it.toString()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        RoutineActivity(
                            id = existing?.id ?: "",
                            title = title.trim(),
                            category = category,
                            startMinute = parseMinute(time),
                            durationMinutes = (duration.toIntOrNull() ?: 20).coerceIn(1, 240),
                            done = existing?.done ?: false,
                        ),
                    )
                },
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

private fun parseMinute(value: String): Int {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour * 60 + minute
}

private fun formatMinute(total: Int): String =
    String.format(Locale.US, "%02d:%02d", (total / 60).coerceIn(0, 23), (total % 60).coerceIn(0, 59))
