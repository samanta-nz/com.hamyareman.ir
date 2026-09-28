package com.hamyareman.ir.ui.recipes

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.Recipe

private const val COOKED_KEY = "recipe_cooked_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(onBack: () -> Unit, onDetail: (String) -> Unit) {
    val container = LocalAppContainer.current
    var query by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf<String?>(null) }
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    LaunchedEffect(Unit) { recipes = container.catalog.recipes() }

    val results = remember(recipes, query, difficulty) {
        recipes.filter { r ->
            (difficulty == null || r.difficulty == difficulty) &&
                (query.isBlank() || r.title.contains(query.trim(), ignoreCase = true) ||
                    r.ingredients.any { it.contains(query.trim(), ignoreCase = true) })
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آشپزی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("چی بپزم؟ اسم غذا یا ماده‌ی اولیه") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("آسان", "متوسط", "کمی سخت").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { difficulty = if (difficulty == level) null else level },
                        label = { Text(level) },
                    )
                }
            }

            if (results.isEmpty()) {
                Text(
                    if (recipes.isEmpty()) "فهرست در حال آماده‌سازی است…" else "با این جست‌وجو چیزی پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            results.forEach { r ->
                val cooked = container.store.getString(COOKED_KEY + r.id).isNotEmpty()
                SectionCard(
                    title = r.title,
                    body = "${r.difficulty} · ${r.minutes} دقیقه · ${r.servings} نفره" +
                        if (cooked) " · پختیش ✅" else "",
                    onClick = { onDetail(r.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(recipeId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var recipe by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(recipeId) {
        recipe = container.catalog.recipes().firstOrNull { it.id == recipeId }
    }

    val current = recipe
    Column(Modifier.fillMaxSize()) {
        AppTopBar(current?.title ?: "دستور", onBack)
        if (current == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("این دستور پیدا نشد. اگر آنلاین نیستی، بعداً دوباره امتحان کن.")
                PrimaryButton("بازگشت", onBack)
            }
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("${current.minutes} دقیقه") })
                AssistChip(onClick = { }, label = { Text("${current.servings} نفره") })
                AssistChip(onClick = { }, label = { Text(current.difficulty) })
            }

            Text("مواد لازم", style = MaterialTheme.typography.titleMedium)
            current.ingredients.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(4.dp))
            Text("طرز تهیه", style = MaterialTheme.typography.titleMedium)
            current.steps.forEachIndexed { i, step ->
                Text("${i + 1}) $step", style = MaterialTheme.typography.bodyMedium)
            }

            if (current.tip.isNotBlank()) {
                SectionCard("نکته‌ی زهرا", current.tip) { }
            }

            val cookedLabel = container.store.getString(COOKED_KEY + current.id)
                .let { if (it.isEmpty()) null else JalaliDate.formatFaLong(it) }
            PrimaryButton(if (cookedLabel == null) "پختمش ✅" else "پختمش ✅ ($cookedLabel)") {
                container.store.putString(COOKED_KEY + current.id, JalaliDate.todayIso())
                onBack()
            }
        }
    }
}
