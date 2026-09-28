package com.hamyareman.ir.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(onExerciseClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    // اول کاتالوگ داخلی نشان داده می‌شود (فوری)، بعد فهرست سرور/کش جایگزین می‌شود.
    var exercises by remember { mutableStateOf(ExerciseCatalog.exercises) }
    LaunchedEffect(Unit) {
        val loaded = container.catalog.exercises()
        if (loaded.isNotEmpty()) exercises = loaded
    }
    val filtered = exercises.filter { selectedCategory == null || it.category == selectedCategory }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("ورزش و کشش روزانه", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("اگر درد داشتی متوقف کن. هیچ ادعای درمانی نیست.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("همه") })
            ExerciseCategory.entries.forEach { category ->
                FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text("${category.emoji} ${category.displayName}") })
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.id }) { exercise ->
                Card(Modifier.fillMaxWidth().clickable { onExerciseClick(exercise.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exercise.category.emoji}  ${exercise.title}", style = MaterialTheme.typography.titleMedium)
                            Text("${exercise.totalDurationMinutes} دقیقه", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(exercise.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
