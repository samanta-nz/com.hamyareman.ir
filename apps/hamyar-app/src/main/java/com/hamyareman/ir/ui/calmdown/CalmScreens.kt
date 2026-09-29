package com.hamyareman.ir.ui.calmdown

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.components.LinedNotebookInput
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Composable
fun CalmMenuScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("الان حالم زیاد خوب نیست") { nav.popBackStack() }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("هر کدوم را که دوست داری انتخاب کن. هیچ‌کدوم اجباری نیست.")
            SectionCard("تنفس ۲ دقیقه‌ای", "یه الگوی نرم.") { nav.navigate(Screen.Breath.route) }
            SectionCard("دفترچه‌ی خصوصی", "هرچی دلت خواست بنویس؛ رمز می‌شود.") { nav.navigate(Screen.Journal.route) }
            SectionCard("پیاده‌روی کوتاه", "فقط چند قدم دور خونه.") { }
            SectionCard("شماره‌های کمک", "همیشه در دسترس، بدون فشار.") { nav.navigate(Screen.Helplines.route) }
        }
    }
}

/** یک یادداشت دفترچه؛ متن فقط به‌شکل رمزشده روی دستگاه می‌ماند. */
private data class JournalEntry(val id: String, val createdAt: Long, val cipher: String)

private const val JOURNAL_KEY = "journal_entries"

private fun readJournal(store: LocalStore): List<JournalEntry> = runCatching {
    val array = JSONArray(store.getString(JOURNAL_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(JournalEntry(o.optString("id"), o.optLong("createdAt"), o.optString("cipher")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeJournal(store: LocalStore, entries: List<JournalEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject()
                .put("id", entry.id)
                .put("createdAt", entry.createdAt)
                .put("cipher", entry.cipher),
        )
    }
    store.putString(JOURNAL_KEY, array.toString())
}

@Composable
fun JournalScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    var text by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(readJournal(c.store)) }
    var opened by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("دفترچه خصوصی", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "این دفترچه با AES-GCM رمز می‌شود و کلیدش در Android Keystore است؛ " +
                    "هرگز به سرور نمی‌رود و در فهرست «هرگز sync نمی‌شود» هم هست.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("هرچی دلت خواست") },
            )
            PrimaryButton("ذخیرهٔ رمزشده") {
                if (text.isNotBlank()) {
                    val entry = JournalEntry(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        cipher = c.encryptor.encrypt(text),
                    )
                    writeJournal(c.store, (readJournal(c.store) + entry))
                    entries = readJournal(c.store)
                    text = ""
                }
            }

            if (entries.isEmpty()) {
                Text("هنوز یادداشتی ننوشتی.", style = MaterialTheme.typography.bodySmall)
            }
            entries.forEach { entry ->
                val plain = remember(entry.cipher) { c.encryptor.decrypt(entry.cipher) }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            JalaliDate.stampFa(entry.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (opened == entry.id) {
                            Text(plain ?: "(رمزگشایی نشد)", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                plain?.lineSequence()?.firstOrNull()?.take(60) ?: "…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { opened = if (opened == entry.id) null else entry.id }) {
                                Text(if (opened == entry.id) "بستن" else "خواندن")
                            }
                            TextButton(onClick = {
                                writeJournal(c.store, readJournal(c.store).filterNot { it.id == entry.id })
                                entries = readJournal(c.store)
                            }) { Text("پاک‌کردن") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                toPersianDigits("${entries.size} یادداشت روی این دستگاه"),
                style = MaterialTheme.typography.labelSmall,
            )
            InlineButton("بازگشت") { onBack() }
        }
    }
}

private const val GRATITUDE_KEY = "gratitude_journal_entries"

@Composable
fun GratitudeJournalScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var text by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(readEncryptedEntries(container.store, GRATITUDE_KEY)) }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("دفترچه شکرگزاری", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("امروز بابت چه چیزی—even کوچک—قدردانی می‌کنی؟", style = MaterialTheme.typography.titleMedium)
            LinedNotebookInput(text, { text = it })
            PrimaryButton("ذخیره در دفترچه") {
                if (text.isBlank()) {
                    notice = "اول یک جمله بنویس."
                } else {
                    val next = listOf(
                        JournalEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), container.encryptor.encrypt(text.trim())),
                    ) + entries
                    writeEncryptedEntries(container.store, GRATITUDE_KEY, next)
                    entries = next
                    text = ""
                    notice = "در دفترچه شکرگزاری ذخیره شد."
                }
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            entries.forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(JalaliDate.stampFa(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(container.encryptor.decrypt(entry.cipher).orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun readEncryptedEntries(store: LocalStore, key: String): List<JournalEntry> = runCatching {
    val array = JSONArray(store.getString(key, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            add(JournalEntry(row.optString("id"), row.optLong("createdAt"), row.optString("cipher")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeEncryptedEntries(store: LocalStore, key: String, entries: List<JournalEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(JSONObject().put("id", entry.id).put("createdAt", entry.createdAt).put("cipher", entry.cipher))
    }
    store.putString(key, array.toString())
}

@Composable
fun BreathingScreen(onBack: () -> Unit) {
    var n by remember { mutableIntStateOf(4) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنفس", onBack)
        Column(Modifier.padding(16.dp)) {
            Text("دم… نگه… بازدم. $n شماره.", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("یک دور دیگر") { n = if (n == 4) 7 else 4 }
        }
    }
}
