package com.hamyareman.ir.ui.art

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.ArtPrompt
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** یک اثر کشیده‌شده در گالری (فقط روی دستگاه زهرا می‌ماند). */
internal data class GalleryEntry(
    val promptId: String,
    val title: String,
    val moodTag: String,
    val dateIso: String,
)

private const val GALLERY_KEY = "art_gallery"

internal fun readGallery(store: LocalStore): List<GalleryEntry> = runCatching {
    val array = JSONArray(store.getString(GALLERY_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                GalleryEntry(
                    promptId = o.optString("promptId"),
                    title = o.optString("title"),
                    moodTag = o.optString("moodTag"),
                    dateIso = o.optString("dateIso"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeGallery(store: LocalStore, entries: List<GalleryEntry>) {
    val array = JSONArray()
    entries.forEach {
        array.put(
            JSONObject()
                .put("promptId", it.promptId).put("title", it.title)
                .put("moodTag", it.moodTag).put("dateIso", it.dateIso),
        )
    }
    store.putString(GALLERY_KEY, array.toString())
}

/**
 * استریک نقاشی: چند روز پشت‌سرهم (تا امروز یا دیروز) کار کشیده شده.
 * «امروز نه» استریک را نمی‌شکند؛ فقط روزِ بی‌کار پشت‌سرهم حساب نمی‌شود.
 */
internal fun artStreak(entries: List<GalleryEntry>, today: LocalDate = LocalDate.now()): Int {
    val days = entries.mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
        .filter { !it.isAfter(today) } // تاریخ آینده (خطای ساعت دستگاه) استریک را باد نمی‌کند.
        .distinct().sortedDescending()
    if (days.isEmpty()) return 0
    // اگر آخرین کار بیش از یک روز پیش است، استریک صفر است.
    if (ChronoUnit.DAYS.between(days.first(), today) > 1) return 0
    var streak = 1
    for (i in 1 until days.size) {
        if (ChronoUnit.DAYS.between(days[i], days[i - 1]) == 1L) streak++ else break
    }
    return streak
}

@Composable
fun DailyArtPromptScreen(onBack: () -> Unit, onGallery: () -> Unit) {
    val container = LocalAppContainer.current
    var prompts by remember { mutableStateOf<List<ArtPrompt>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var drawnToday by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val all = container.catalog.artPrompts()
        prompts = all
        index = all.indexOf(container.catalog.artPromptOfDay(all)).coerceAtLeast(0)
        drawnToday = readGallery(container.store).any { it.dateIso == LocalDate.now().toString() }
    }

    val current: ArtPrompt? = prompts.getOrNull(index)

    Column(Modifier.fillMaxSize()) {
        AppTopBar("ایده‌ی سیاه‌قلم", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val streak = artStreak(readGallery(container.store))
            SectionCard(
                title = "استریک تو: $streak روز",
                body = if (drawnToday) "امروز کشیدی ✅ — آفرین." else "یک اسکیس کوچک هم حساب است؛ ۱۰ دقیقه کافی‌ست.",
            ) { }

            if (current == null) {
                Text("ایده‌ها در حال آماده‌سازی‌اند…")
            } else {
                Text("موضوع: ${current.title}", style = MaterialTheme.typography.titleMedium)
                Text(current.prompt, style = MaterialTheme.typography.bodyLarge)
                if (current.moodTag.isNotBlank()) {
                    Text("حس‌وهوا: ${current.moodTag}", style = MaterialTheme.typography.bodySmall)
                }
                PrimaryButton(if (drawnToday) "امروز کشیدم ✅" else "کشیدمش ✅") {
                    val entries = readGallery(container.store).toMutableList()
                    val todayIso = LocalDate.now().toString()
                    if (entries.none { it.dateIso == todayIso && it.promptId == current.id }) {
                        entries.add(0, GalleryEntry(current.id, current.title, current.moodTag, todayIso))
                        writeGallery(container.store, entries)
                    }
                    drawnToday = true
                    onGallery()
                }
                PrimaryButton("ایده‌ی دیگه") {
                    if (prompts.isNotEmpty()) index = (index + 1) % prompts.size
                }
            }

            PrimaryButton("امروز نه — استریک جداست", onBack)
            PrimaryButton("گالری پیشرفت", onGallery)
        }
    }
}

@Composable
fun ArtGalleryScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var entries by remember { mutableStateOf<List<GalleryEntry>>(emptyList()) }
    LaunchedEffect(Unit) { entries = readGallery(container.store) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("گالری", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = "${entries.size} کار کشیده‌شده",
                body = "استریک: ${artStreak(entries)} روز — عکس اثرها بعداً اضافه می‌شود؛ الان موضوع و تاریخ ثبت می‌شود.",
            ) { }

            if (entries.isEmpty()) {
                Text(
                    "هنوز چیزی اینجا نیست. از «ایده‌ی سیاه‌قلم» شروع کن؛ حتی یک اسکیس ۵ دقیقه‌ای.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            entries.forEach { e ->
                SectionCard(
                    title = e.title,
                    body = JalaliDate.formatFaLong(e.dateIso) +
                        (if (e.moodTag.isBlank()) "" else " · ${e.moodTag}"),
                ) { }
            }
        }
    }
}
