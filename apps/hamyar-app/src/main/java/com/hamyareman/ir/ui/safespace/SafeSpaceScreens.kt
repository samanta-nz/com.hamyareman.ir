package com.hamyareman.ir.ui.safespace

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.navigation.Screen
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

@Composable
fun SafeSpaceScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("فضای امن من") { nav.popBackStack() }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("خیلی از نوجوان‌ها وقتی زندگی خانوادگی‌شون تغییر می‌کنه، قاطی احساسات مختلف می‌شن. همه‌ی این‌ها طبیعیه.")
            SectionCard("نامه به خونه‌ی قبلی", "یه تمرین نوشتاری، کاملاً اختیاری.") { nav.navigate(Screen.Writing.route) }
            SectionCard("شماره‌های مشاوره", "همیشه در دسترس، بدون فشار.") { nav.navigate(Screen.Helplines.route) }
        }
    }
}

/**
 * نامه‌ی «خونه‌ی قبلی» — یک تمرین نوشتاری کاملاً اختیاری.
 *
 * متن با AES-GCM رمز می‌شود (کلید در Android Keystore) و فقط روی دستگاه می‌ماند؛
 * در فهرست `PrivacyPolicy.neverSyncTables` نیست چون اصلاً به سرور فرستاده نمی‌شود.
 */
internal data class Letter(val id: String, val createdAt: Long, val cipher: String)

private const val LETTERS_KEY = "safespace_letters"

private val WRITING_PROMPTS = listOf(
    "اگه بخوای به خونه‌ی قبلی یه نامه‌ی کوتاه بنویسی، چی می‌نویسی؟",
    "یک خاطره‌ی خوب از خونه‌ی قبلی را بنویس؛ فقط برای خودت.",
    "چه چیزی را دوست داری همان‌طور بماند؟",
    "به خودت در شش ماه بعد چه می‌گویی؟",
    "امروز چه چیزی سخت بود و چه چیزی آسان؟",
)

internal fun readLetters(store: LocalStore): List<Letter> = runCatching {
    val array = JSONArray(store.getString(LETTERS_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val cipher = o.optString("cipher")
            if (cipher.isBlank()) continue
            add(Letter(o.optString("id").ifBlank { "letter$i" }, o.optLong("createdAt"), cipher))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeLetters(store: LocalStore, letters: List<Letter>) {
    val array = JSONArray()
    letters.forEach { letter ->
        array.put(
            JSONObject()
                .put("id", letter.id).put("createdAt", letter.createdAt).put("cipher", letter.cipher),
        )
    }
    store.putString(LETTERS_KEY, array.toString())
}

@Composable
fun WritingPromptScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    var promptIndex by remember {
        mutableIntStateOf((LocalDate.now().dayOfYear - 1).coerceAtLeast(0) % WRITING_PROMPTS.size)
    }
    var text by remember { mutableStateOf("") }
    var letters by remember { mutableStateOf(readLetters(c.store)) }
    var opened by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نامه کوتاه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(WRITING_PROMPTS[promptIndex], style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { promptIndex = (promptIndex + 1) % WRITING_PROMPTS.size }) {
                Text("یه پیشنهاد دیگه")
            }
            Text(
                "این نامه با AES-GCM رمز می‌شود و کلیدش در Keystore خود گوشی است؛ " +
                    "هیچ‌وقت به سرور نمی‌رود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("هرچی دلت خواست بنویس") },
            )
            PrimaryButton("ذخیره پیش خودم") {
                if (text.isNotBlank()) {
                    val letter = Letter(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        cipher = c.encryptor.encrypt(text),
                    )
                    letters = listOf(letter) + letters
                    writeLetters(c.store, letters)
                    text = ""
                    notice = "ذخیره شد — فقط روی گوشی خودت."
                } else {
                    notice = "اول چیزی بنویس، حتی یک جمله."
                }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            if (letters.isEmpty()) {
                Text("هنوز نامه‌ای ننوشتی؛ اجباری هم نیست.", style = MaterialTheme.typography.bodySmall)
            }
            letters.forEach { letter ->
                val plain = remember(letter.cipher) { c.encryptor.decrypt(letter.cipher) }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            JalaliDate.stampFa(letter.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (opened == letter.id) {
                            Text(plain ?: "(رمزگشایی نشد)", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                plain?.lineSequence()?.firstOrNull()?.take(60) ?: "…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = { opened = if (opened == letter.id) null else letter.id }) {
                            Text(if (opened == letter.id) "بستن" else "خواندن")
                        }
                        TextButton(onClick = {
                            letters = letters.filterNot { it.id == letter.id }
                            writeLetters(c.store, letters)
                        }) { Text("حذف") }
                    }
                }
            }
        }
    }
}

@Composable
fun HelplinesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar("شماره‌های کمک", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بدون فشار. هر وقت خواستی.")
            Helplines.iran.forEach { h ->
                SectionCard("${h.name} — ${h.number}", h.hours) {
                    ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${h.number}")))
                }
            }
        }
    }
}
