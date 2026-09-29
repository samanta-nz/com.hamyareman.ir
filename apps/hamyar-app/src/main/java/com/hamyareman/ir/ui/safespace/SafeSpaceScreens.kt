package com.hamyareman.ir.ui.safespace

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen

private data class SafeSection(val emoji: String, val title: String, val subtitle: String, val route: String)

/** فضای امن فقط پس از PIN یا بیومتریک باز می‌شود و هیچ محتوایی پیش از احراز نمایش نمی‌دهد. */
@Composable
fun SafeSpaceScreen(nav: NavController) {
    val context = LocalContext.current
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

    val sections = listOf(
        SafeSection("📕", "دفتر خاطرات", "جلد دلخواه، تاریخ شمسی و ورق‌زدن راست‌به‌چپ", Screen.Diary.route),
        SafeSection("📓", "دفترچه‌های من", "دفتر خط‌دار با صفحه‌بندی خودکار", Screen.Notebooks.route),
        SafeSection("✍️", "رونوشت آزاد", "نوشتن آزاد و خصوصی", Screen.SafeFreeWriting.route),
        SafeSection("🔐", "آلبوم شخصی", "نمایش تمام‌صفحهٔ عکس، ویدیو و صوت", Screen.SecureGallery.route),
    )
    var policy by remember { mutableStateOf(SafeSpaceSession.policy(context)) }
    var biometricEnabled by remember { mutableStateOf(safeBiometric.isEnabled()) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("فضای امن", { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("قفل مستقل فضای امن", style = MaterialTheme.typography.titleMedium)
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
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("ورود با اثر انگشت/چهره")
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (safeBiometric.setEnabled(enabled, context)) {
                                    biometricEnabled = enabled
                                    error = null
                                } else {
                                    error = "بیومتریک دستگاه آماده نیست؛ PIN مستقل همچنان فعال است."
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
                    ) { Text("قفل فوری فضای امن") }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            sections.forEach { section ->
                Card(
                    Modifier.fillMaxWidth().clickable { nav.layerTo(section.route) },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${section.emoji}  ${section.title}", style = MaterialTheme.typography.titleMedium)
                        Text(section.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** رونوشت آزاد با رمزگذاری Keystore؛ متن خام هیچ‌وقت داخل SharedPreferences نمی‌رود. */
@Composable
fun SafeFreeWritingScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val key = "safe_free_writing"
    var text by remember {
        mutableStateOf(container.encryptor.decrypt(container.store.getString(key)).orEmpty())
    }
    var notice by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("رونوشت آزاد", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("هرچه در ذهنت هست، بدون ویرایش بنویس.", style = MaterialTheme.typography.bodyMedium)
            LinedNotebookInput(text, { text = it })
            PrimaryButton("ذخیرهٔ امن") {
                container.store.putString(key, container.encryptor.encrypt(text))
                notice = "با رمزگذاری دستگاه ذخیره شد."
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
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
