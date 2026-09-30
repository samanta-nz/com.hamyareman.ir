package com.hamyareman.ir.ui.safespace

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.platform.core.security.BiometricStatus
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class SafeSection(val emoji: String, val title: String, val subtitle: String, val route: String)

/** فضای امن فقط پس از PIN یا بیومتریک باز می‌شود و هیچ محتوایی پیش از احراز نمایش نمی‌دهد. */
@Composable
fun SafeSpaceScreen(nav: NavController) {
    val context = LocalContext.current
    com.hamyareman.ir.ui.study.SecureWebEffect("Screenshots are disabled in the private workspace.")
    val activity = LocalActivity.current as? FragmentActivity
    val safeLock = remember { SafeSpaceSession.lock(context) }
    val safeBiometric = remember { SafeSpaceSession.biometric(context) }
    var unlocked by remember { mutableStateOf(SafeSpaceSession.isUnlocked(context)) }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val needsSetup = !safeLock.hasPin()

    if (!unlocked || !SafeSpaceSession.isUnlocked(context)) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("فضای امن", { nav.popBackStack() })
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (needsSetup) {
                        "برای فضای امن یک PIN مستقل بساز؛ این PIN با قفل خودِ برنامه فرق دارد."
                    } else {
                        "برای دیدن بخش‌های خصوصی، قفل مستقل فضای امن را باز کن."
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text(if (needsSetup) "PIN جدید فضای امن" else "PIN فضای امن") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (needsSetup) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it.filter(Char::isDigit).take(8); error = null },
                        label = { Text("تکرار PIN مستقل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PrimaryButton(if (needsSetup) "ساخت PIN و ورود" else "بازکردن فضای امن") {
                    if (needsSetup) {
                        error = when {
                            pin != confirmation -> "تکرار PIN یکسان نیست."
                            else -> safeLock.validatePin(pin)
                        }
                        if (error == null && safeLock.setPin(pin)) {
                            SafeSpaceSession.markUnlocked()
                            unlocked = true
                            if (activity != null && safeBiometric.status(context) == BiometricStatus.READY) {
                                BiometricPromptRunner.show(
                                    activity = activity,
                                    title = "فعال‌کردن بیومتریک فضای امن",
                                    subtitle = "برای ورود سریع‌تر به فضای امن یک بار هویتت را تأیید کن.",
                                    negativeText = "فعلاً نه",
                                    onSuccess = { safeBiometric.setEnabled(true, context) },
                                    onError = { error = it },
                                )
                            }
                        }
                    } else if (safeLock.verify(pin)) {
                        SafeSpaceSession.markUnlocked()
                        unlocked = true
                    } else {
                        error = "PIN فضای امن درست نیست."
                    }
                }
                if (!needsSetup && activity != null && safeBiometric.shouldOffer(activity)) {
                    OutlinedButton(
                        onClick = {
                            BiometricPromptRunner.show(
                                activity = activity,
                                title = "فضای امن",
                                subtitle = "ورود مستقل با اثر انگشت یا چهره",
                                onSuccess = {
                                    SafeSpaceSession.markUnlocked()
                                    unlocked = true
                                    error = null
                                },
                                onError = { error = it },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("بازکردن با اثر انگشت/چهره") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        return
    }

    // ترتیب ثابت: دل‌نوشت → دفتر خاطرات → نوشته‌های آزاد → آلبوم شخصی.
    val sections = listOf(
        SafeSection("💌", "دل‌نوشت", "یادداشت‌های خصوصی خط‌دار با عنوان و ویرایش", Screen.Journal.route),
        SafeSection("📕", "دفتر خاطرات", "جلد دلخواه، تاریخ شمسی و تورق راست‌به‌چپ", Screen.Diary.route),
        SafeSection("✍️", "نوشته‌های آزاد", "نوشته‌های خصوصی با عنوان و امکان ویرایش", Screen.SafeFreeWriting.route),
        SafeSection("🔐", "آلبوم شخصی", "عکس، ویدیو، صوت و دفتر خاطرات در نمای کتاب", Screen.SecureGallery.route),
    )
    var policy by remember { mutableStateOf(SafeSpaceSession.policy(context)) }
    var biometricEnabled by remember { mutableStateOf(safeBiometric.isEnabled()) }
    var showHelp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth()) {
            AppTopBar("فضای امن", { nav.popBackStack() })
            TextButton(
                onClick = { showHelp = true },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            ) { Text("?", style = MaterialTheme.typography.titleLarge) }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // تنظیمات امنیتی بلافاصله زیر عنوان می‌آید.
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنظیمات امنیتی مستقل", style = MaterialTheme.typography.titleMedium)
                    SafeExitPolicy.entries.forEach { option ->
                        FilterChip(
                            selected = policy == option,
                            onClick = {
                                policy = option
                                SafeSpaceSession.setPolicy(context, option)
                            },
                            label = { Text(option.titleFa) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("ورود با اثر انگشت/چهره")
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    safeBiometric.setEnabled(false, context)
                                    biometricEnabled = false
                                    error = null
                                } else if (activity == null) {
                                    error = "بیومتریک روی این صفحه در دسترس نیست."
                                } else {
                                    // فعال‌سازی فقط پس از تأیید واقعی سیستم‌عامل انجام می‌شود.
                                    BiometricPromptRunner.show(
                                        activity = activity,
                                        title = "فعال‌کردن ورود بیومتریک فضای امن",
                                        subtitle = "اثر انگشت یا چهره‌ات را برای تأیید ثبت کن.",
                                        negativeText = "بی‌خیال",
                                        onSuccess = {
                                            biometricEnabled = safeBiometric.setEnabled(true, context)
                                            error = if (biometricEnabled) null else "بیومتریک دستگاه آماده نیست."
                                        },
                                        onError = { error = it },
                                    )
                                }
                            },
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            SafeSpaceSession.forceLock()
                            unlocked = false
                            pin = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("خروج و قفل فوری فضای امن") }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            sections.forEach { section ->
                Card(Modifier.fillMaxWidth().clickable { nav.layerTo(section.route) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${section.emoji}  ${section.title}", style = MaterialTheme.typography.titleMedium)
                        Text(section.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            SafeSpaceBackupCard()
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("راهنمای فضای امن") },
            text = {
                Text(
                    "• PIN و بیومتریک این بخش از قفل اصلی برنامه مستقل‌اند.\n" +
                        "• گزینهٔ «با قفل صفحه» فقط با خاموش یا قفل‌شدن گوشی می‌بندد.\n" +
                        "• زمان‌های ۱، ۲ و ۳ دقیقه از لحظهٔ خروج از فضای امن محاسبه می‌شوند.\n" +
                        "• متن‌ها با کلید امن دستگاه و فایل‌های آلبوم در پوشهٔ خصوصی اپ نگهداری می‌شوند.\n" +
                        "• بکاپ رمزگذاری‌شده گزینهٔ پیشنهادی است؛ ZIP بدون رمز را فقط برای انتقال آگاهانه بساز."
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("فهمیدم") } },
        )
    }
}

private data class FreeWritingEntry(
    val id: String,
    val createdAt: Long,
    val title: String,
    val cipher: String,
)

private const val FREE_WRITING_ENTRIES = "safe_free_writing_entries"
private const val LEGACY_FREE_WRITING = "safe_free_writing"

private fun readFreeWriting(container: com.hamyareman.ir.di.AppContainer): List<FreeWritingEntry> {
    val parsed = runCatching {
        val a = JSONArray(container.store.getString(FREE_WRITING_ENTRIES, "[]"))
        buildList {
            for (i in 0 until a.length()) a.getJSONObject(i).let { o ->
                add(FreeWritingEntry(o.optString("id"), o.optLong("createdAt"), o.optString("title"), o.optString("cipher")))
            }
        }
    }.getOrDefault(emptyList())
    if (parsed.isNotEmpty()) return parsed.sortedByDescending { it.createdAt }
    val oldCipher = container.store.getString(LEGACY_FREE_WRITING, "")
    if (container.encryptor.decrypt(oldCipher).isNullOrBlank()) return emptyList()
    return listOf(FreeWritingEntry("legacy-free-writing", System.currentTimeMillis(), "نوشتهٔ آزاد", oldCipher))
}

private fun writeFreeWriting(container: com.hamyareman.ir.di.AppContainer, entries: List<FreeWritingEntry>) {
    val a = JSONArray()
    entries.forEach { e ->
        a.put(JSONObject().put("id", e.id).put("createdAt", e.createdAt).put("title", e.title).put("cipher", e.cipher))
    }
    container.store.putString(FREE_WRITING_ENTRIES, a.toString())
    container.store.remove(LEGACY_FREE_WRITING)
}

/** نوشته‌های آزاد؛ همان قالب خط‌دار، عنوان‌دار و قابل ویرایشِ دفترچه‌ها. */
@Composable
fun SafeFreeWritingScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf(readFreeWriting(container)) }
    var notice by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("نوشته‌های آزاد", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("هرچه در ذهنت هست بنویس؛ لمس هر نوشته آن را برای ویرایش به دفتر بالا می‌آورد.")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(100) },
                label = { Text("عنوان") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LinedNotebookInput(text, { text = it })
            PrimaryButton(if (editingId == null) "ذخیرهٔ امن" else "ذخیرهٔ ویرایش") {
                if (text.isBlank()) {
                    notice = "اول چیزی بنویس."
                } else {
                    val id = editingId ?: UUID.randomUUID().toString()
                    val created = entries.firstOrNull { it.id == id }?.createdAt ?: System.currentTimeMillis()
                    val changed = FreeWritingEntry(id, created, title.trim(), container.encryptor.encrypt(text.trim()))
                    entries = (listOf(changed) + entries.filterNot { it.id == id }).sortedByDescending { it.createdAt }
                    writeFreeWriting(container, entries)
                    title = ""; text = ""; editingId = null
                    notice = "با رمزگذاری دستگاه ذخیره شد."
                }
            }
            if (editingId != null) {
                TextButton(onClick = { editingId = null; title = ""; text = "" }) { Text("لغو ویرایش") }
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            entries.forEach { entry ->
                val plain = remember(entry.cipher) { container.encryptor.decrypt(entry.cipher).orEmpty() }
                Card(Modifier.fillMaxWidth().clickable {
                    editingId = entry.id
                    title = entry.title
                    text = plain
                    notice = "این نوشته برای ویرایش باز شد."
                }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleSmall)
                        Text(JalaliDate.stampFa(entry.createdAt), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        Text(plain, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            entries = entries.filterNot { it.id == entry.id }
                            writeFreeWriting(container, entries)
                            if (editingId == entry.id) { editingId = null; title = ""; text = "" }
                        }) { Text("حذف") }
                    }
                }
            }
        }
    }
}

/** مسیر قدیمی نوشتن را بدون بازگرداندن محتوای حذف‌شده به رونوشت امن می‌برد. */
@Composable
fun WritingPromptScreen(onBack: () -> Unit) = SafeFreeWritingScreen(onBack)

/** شماره‌های کمک مسیر مستقلی بیرون از محتوای چهاربخشی فضای امن دارند. */
@Composable
fun HelplinesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AppTopBar("شماره‌های کمک", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بدون فشار؛ هر وقت خواستی.")
            Helplines.iran.forEach { line ->
                SectionCard("${line.name} — ${line.number}", line.hours) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${line.number}")))
                }
            }
        }
    }
}
