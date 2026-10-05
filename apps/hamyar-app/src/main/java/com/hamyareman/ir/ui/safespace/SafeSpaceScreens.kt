package com.hamyareman.ir.ui.safespace

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.LockBackdropLayer
import com.hamyareman.ir.platform.core.designsystem.LockVariant
import com.hamyareman.ir.platform.core.designsystem.PatternLockGrid
import com.hamyareman.ir.platform.core.designsystem.PinLockGate
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.core.security.AppLock
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.platform.core.security.BiometricStatus
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class SafeSection(val icon: ImageVector, val title: String, val subtitle: String, val route: String)

/** فضای امن فقط پس از PIN، الگو یا بیومتریکِ مستقل باز می‌شود و هیچ محتوایی پیش از احراز نمایش نمی‌دهد. */
@Composable
fun SafeSpaceScreen(nav: NavController) {
    val context = LocalContext.current
    com.hamyareman.ir.ui.study.SecureWebEffect("Screenshots are disabled in the private workspace.")
    val activity = LocalActivity.current as? FragmentActivity
    val safeLock = remember { SafeSpaceSession.lock(context) }
    val safeBiometric = remember { SafeSpaceSession.biometric(context) }
    var unlocked by remember { mutableStateOf(SafeSpaceSession.isUnlocked(context)) }
    var pattern by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val needsSetup = !safeLock.hasPin()

    if (!unlocked || !SafeSpaceSession.isUnlocked(context)) {
        if (needsSetup) {
            SafeSpacePinSetup(
                onBack = { nav.popBackStack() },
                validate = { safeLock.validatePin(it) },
                create = { safeLock.setPin(it) },
                onDone = {
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
                },
            )
        } else {
            val offerBiometric = activity != null && safeBiometric.shouldOffer(activity)
            Box(Modifier.fillMaxSize()) {
                // قفل فضای امن: صحنهٔ شب جنگلی، الگوی واقعی نقطه + خط و تأیید خودکار با رها کردن انگشت.
                PinLockGate(
                    title = "قفل فضای امن",
                    subtitle = "برای دیدن بخش‌های خصوصی، PIN مستقل فضای امن را وارد کن.",
                    minLength = AppLock.MIN_PIN,
                    maxLength = AppLock.MAX_PIN,
                    patternEnabled = safeLock.hasPattern(),
                    onPatternVerify = { value -> safeLock.verifyPattern(value) },
                    variant = LockVariant.SafeSpace,
                    biometricLabel = if (offerBiometric) "بازکردن با اثر انگشت/چهره" else null,
                    externalNotice = error,
                    onBiometricRequest = if (offerBiometric && activity != null) {
                        {
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
                        }
                    } else {
                        null
                    },
                    onVerify = { pin -> safeLock.verify(pin) },
                    onUnlocked = {
                        SafeSpaceSession.markUnlocked()
                        unlocked = true
                        error = null
                    },
                )
                LockBackButton(onClick = { nav.popBackStack() }, modifier = Modifier.align(Alignment.TopStart))
            }
        }
        return
    }

    // ترتیب فضای امن (طبق طرح): بکاپ و بازیابی ← قالب بکاپ و هشدارها ← سایر بخش‌ها ← امنیت مستقل (آخر، همیشه بسته).
    val sections = listOf(
        SafeSection(Icons.Outlined.Email, "دل‌نوشت", "یادداشت‌های خصوصی خط‌دار با عنوان و ویرایش", Screen.Journal.route),
        SafeSection(Icons.Outlined.MenuBook, "دفتر خاطرات", "جلد دلخواه، تاریخ شمسی و تورق راست‌به‌چپ", Screen.Diary.route),
        SafeSection(Icons.Outlined.EditNote, "نوشته‌های آزاد", "نوشته‌های خصوصی با عنوان و امکان ویرایش", Screen.SafeFreeWriting.route),
        SafeSection(Icons.Outlined.HistoryEdu, "دفتر شعر", "غزل، قصیده، دوبیتی و شعر آزاد با تورق راست‌به‌چپ", Screen.Poetry.route),
        SafeSection(Icons.Outlined.PhotoLibrary, "آلبوم شخصی", "عکس، ویدیو، صوت و دفتر خاطرات در نمای کتاب", Screen.SecureGallery.route),
    )
    var policy by remember { mutableStateOf(SafeSpaceSession.policy(context)) }
    var biometricEnabled by remember { mutableStateOf(safeBiometric.isEnabled()) }
    var showHelp by remember { mutableStateOf(false) }
    var securityExpanded by remember { mutableStateOf(false) } // همیشه در حالت اولیه بسته
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SafeSpaceBackupCard()
            SafePanel {
                Text(
                    "بخش‌های فضای امن",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                )
                sections.forEachIndexed { index, section ->
                    if (index > 0) SafeDivider()
                    SafeRow(section.icon, section.title, section.subtitle) { nav.layerTo(section.route) }
                }
            }
            SafePanel {
                Row(
                    Modifier.fillMaxWidth().clickable { securityExpanded = !securityExpanded }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SafeIconBadge(Icons.Outlined.Lock)
                    Column(Modifier.weight(1f)) {
                        Text("تنظیمات امنیتی", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "قفل جداگانه · الگوی نقطه‌وخط · ورود بیومتریک",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (securityExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (securityExpanded) "بستن تنظیمات امنیتی" else "باز کردن تنظیمات امنیتی",
                    )
                }
                if (securityExpanded) {
                    Column(
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("الگوی خط‌ونقطهٔ مستقل", style = MaterialTheme.typography.labelLarge)
                        PatternLockGrid(pattern = pattern, onPatternChange = { pattern = it }, enabled = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { pattern = "" }, modifier = Modifier.weight(1f)) { Text("پاک‌کردن") }
                            PrimaryButton("ذخیرهٔ الگو", Modifier.weight(2f)) {
                                if (safeLock.validatePattern(pattern)) {
                                    safeLock.setPattern(pattern)
                                    pattern = ""
                                    error = null
                                } else {
                                    error = "حداقل ۴ نقطهٔ متفاوت رسم کن."
                                }
                            }
                        }
                        SafeExitPolicy.entries.forEach { option ->
                            FilterChip(
                                selected = policy == option,
                                onClick = { policy = option; SafeSpaceSession.setPolicy(context, option) },
                                label = { Text(option.titleFa) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ورود با اثر انگشت/چهره")
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) { safeBiometric.setEnabled(false, context); biometricEnabled = false }
                                    else if (activity != null) BiometricPromptRunner.show(
                                        activity = activity,
                                        title = "فعال‌کردن ورود بیومتریک فضای امن",
                                        subtitle = "اثر انگشت یا چهره‌ات را برای تأیید ثبت کن.",
                                        negativeText = "بی‌خیال",
                                        onSuccess = { biometricEnabled = safeBiometric.setEnabled(true, context) },
                                        onError = { error = it },
                                    )
                                },
                            )
                        }
                        OutlinedButton(
                            onClick = { SafeSpaceSession.forceLock(); unlocked = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("خروج و قفل فوری فضای امن") }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Text(
                "امنیت فضای امن مستقل از قفل برنامه است و می‌توانی رمز یا الگوی جداگانه‌ای برای آن انتخاب کنی.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("راهنمای فضای امن") },
            text = {
                Text(
                    "• PIN، الگو و بیومتریک این بخش از قفل اصلی برنامه مستقل‌اند.\n" +
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

/** دکمهٔ بازگشت شیشه‌ایِ روی پس‌زمینهٔ تیرهٔ قفل. */
@Composable
private fun LockBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .statusBarsPadding()
            .padding(12.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
    }
}

/** ساخت PIN مستقل فضای امن (بار اول)، روی همان پس‌زمینهٔ شب جنگلی. */
@Composable
private fun SafeSpacePinSetup(
    onBack: () -> Unit,
    validate: (String) -> String?,
    create: (String) -> Boolean,
    onDone: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF3D8BFF),
        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
    )
    Box(Modifier.fillMaxSize()) {
        LockBackdropLayer(LockVariant.SafeSpace)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            Text("قفل فضای امن", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(
                "برای فضای امن یک PIN مستقل بساز؛ این PIN با قفل خودِ برنامه فرق دارد.",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(AppLock.MAX_PIN); error = null },
                label = { Text("PIN جدید فضای امن") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it.filter(Char::isDigit).take(AppLock.MAX_PIN); error = null },
                label = { Text("تکرار PIN مستقل") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            )
            error?.let { Text(it, color = Color(0xFFFF9A9A), textAlign = TextAlign.Center) }
            Button(
                onClick = {
                    error = when {
                        pin != confirmation -> "تکرار PIN یکسان نیست."
                        else -> validate(pin)
                    }
                    if (error == null) {
                        if (create(pin)) onDone() else error = "ذخیرهٔ PIN انجام نشد."
                    }
                },
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            ) { Text("ساخت PIN و ورود") }
        }
        LockBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** کارت کاغذیِ گوشه‌گرد؛ پایهٔ همهٔ بخش‌های صفحهٔ فضای امن. */
@Composable
internal fun SafePanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) { Column(Modifier.fillMaxWidth(), content = content) }
}

@Composable
internal fun SafeIconBadge(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun SafeDivider() {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
internal fun SafeRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SafeIconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
