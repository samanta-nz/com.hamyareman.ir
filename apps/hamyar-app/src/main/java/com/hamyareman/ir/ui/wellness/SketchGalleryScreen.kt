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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * پرامپت ۰۲ — صفحه‌ی «مرجع نقاشی سیاه‌قلم».
 *
 * کاربر یک موضوع + سطح (۴-۱۰) انتخاب می‌کند. Appwrite Function
 * `generate-sketch-reference` یک تصویر سیاه‌وسفید می‌سازد، در Storage می‌گذارد
 * و در گالری نمایش می‌دهد.
 */
@Composable
fun SketchGalleryScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("face") }
    var level by remember { mutableStateOf(6) }
    var generating by remember { mutableStateOf(false) }
    val gallery = remember { mutableStateListOf<SketchItem>() }

    // لود گالری از سرور/کش محلی
    LaunchedEffect(Unit) {
        // ابتدا: کش محلی
        val cached = container.store.getString(KEY_GALLERY)
        if (cached.isNotBlank()) {
            runCatching {
                val arr = org.json.JSONArray(cached)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    gallery += SketchItem(
                        titleFa = o.optString("titleFa"),
                        imageUrl = o.optString("imageUrl"),
                        subject = o.optString("subject"),
                        level = o.optInt("level"),
                    )
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar("مرجع نقاشی", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // انتخاب موضوع
            Text("موضوع", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("face" to "چهره", "nature" to "طبیعت", "animal" to "حیوان", "object" to "اشیاء").forEach { (s, label) ->
                    FilterChip(
                        selected = subject == s,
                        onClick = { subject = s },
                        label = { Text(label) },
                    )
                }
            }

            // انتخاب سطح
            Text("سطح مهارت: $level از ۱۰", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (4..10).forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { level = lvl },
                        label = { Text(lvl.toString()) },
                    )
                }
            }

            // دکمه‌ی تولید
            Button(
                onClick = {
                    if (generating) return@Button
                    generating = true
                    scope.launch {
                        try {
                            val resBody = container.functions.callForBody(
                                functionId = com.hamyareman.ir.platform.core.common.FunctionIds.GENERATE_SKETCH_REFERENCE,
                                body = JSONObject()
                                    .put("subject", subject)
                                    .put("level", level)
                                    .put("userId", container.auth.cachedUserId() ?: "")
                                    .toString(),
                            )
                            if (resBody != null) {
                                val obj = JSONObject(resBody)
                                if (obj.optBoolean("ok")) {
                                    val item = SketchItem(
                                        titleFa = "${subjectLabelFa(subject)} - سطح $level",
                                        imageUrl = obj.optString("imageUrl"),
                                        subject = subject,
                                        level = level,
                                    )
                                    gallery.add(0, item)
                                    saveGalleryToCache(container, gallery.toList())
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        } finally {
                            generating = false
                        }
                    }
                },
                enabled = !generating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (generating) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("در حال ساخت…")
                } else {
                    Text("تولید مرجع تازه")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("گالری مرجع‌های من (${gallery.size})", style = MaterialTheme.typography.titleSmall)

            if (gallery.isEmpty()) {
                Text(
                    "هنوز مرجعی نساختی. یک موضوع و سطح انتخاب کن و «تولید مرجع تازه» بزن.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(gallery, key = { it.imageUrl }) { item ->
                        SketchCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SketchCard(item: SketchItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.titleFa,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            )
            Text(
                item.titleFa,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

private data class SketchItem(
    val titleFa: String,
    val imageUrl: String,
    val subject: String,
    val level: Int,
)

private fun subjectLabelFa(s: String): String = when (s) {
    "face" -> "چهره"
    "nature" -> "طبیعت"
    "animal" -> "حیوان"
    "object" -> "اشیاء"
    else -> s
}

private fun saveGalleryToCache(container: com.hamyareman.ir.di.AppContainer, items: List<SketchItem>) {
    val arr = org.json.JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject()
                .put("titleFa", item.titleFa)
                .put("imageUrl", item.imageUrl)
                .put("subject", item.subject)
                .put("level", item.level)
        )
    }
    container.store.putString(KEY_GALLERY, arr.toString())
}

private const val KEY_GALLERY = "sketch_gallery_v1"
