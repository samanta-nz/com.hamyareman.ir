package com.hamyareman.ir.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.PrivacyPolicy
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import java.util.UUID
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.platform.core.notifications.QuietHoursAutomation
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.platform.core.security.BiometricStatus
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.navigation.Screen
import kotlinx.coroutines.launch
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.hamyareman.ir.ui.sync.SyncCenter
import java.util.Locale

@Composable
fun SettingsScreen(nav: NavController) {
    val container = LocalAppContainer.current
    // فقط فهرست تنظیمات دو واحد جمع‌تر است؛ خانوادهٔ انتخابی کاربر حفظ می‌شود.
    val compactTypography = settingsTypography(MaterialTheme.typography, sizeDelta = -2f)
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = compactTypography,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            AppTopBar("تنظیمات") { nav.popBackStack() }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionCard(
                    "تم برنامه",
                    "رنگ، حالت روشن یا تاریک و اندازهٔ نوشته.",
                ) { nav.navigate(Screen.Appearance.route) }
                SectionCard(
                    "قفل برنامه",
                    if (container.lock.isEnabled()) "فعال — ورود با PIN یا بیومتریک." else "غیرفعال — برای محافظت فعالش کن.",
                ) { nav.navigate(Screen.Lock.route) }
                SectionCard(
                    "یادآورها و ساعات سکوت",
                    "مدیریت یادآورها و یک بازهٔ سکوت روزانه.",
                ) { nav.navigate(Screen.Reminders.route) }
                SectionCard(
                    "تنظیمات سرور",
                    "منبع محتوا، همگام‌سازی و کش دستگاه.",
                ) { nav.navigate(Screen.Sync.route) }
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val store = container.store
    var notice by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("حریم خصوصی", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "چرخه، ژورنال و چت خام هرگز به سرور نمی‌روند. بقیه‌ی پیشرفت درسی با حساب خودت همگام می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("هرگز به سرور نمی‌رود", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        PrivacyPolicy.neverSyncTables.joinToString("، "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "این جدول‌ها فقط روی همین دستگاه می‌مانند؛ SyncEngine آن‌ها را حتی وارد صف هم نمی‌کند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("پاک‌کردن داده", style = MaterialTheme.typography.titleMedium)
            PrimaryButton("پاک‌کردن کش گفت‌وگو از این دستگاه") {
                container.heart.clearLocal()
                notice = "کش گفت‌وگو از این دستگاه پاک شد."
            }
            PrimaryButton("پاک‌کردن همه‌ی داده‌های محلی (شامل PIN)") {
                container.heart.clearLocal()
                container.sync.clear()
                container.lock.clearPin()
                store.clearAll()
                notice = "همه‌ی داده‌های محلی پاک شد. اپ را دوباره باز کن."
            }
            notice?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AppLockScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val lock = container.lock
    val bio = container.biometric
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val bioStatus = remember { bio.status(context) }
    var bioState by remember { mutableStateOf(bio.isEnabled()) }
    var currentPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var timeoutMs by remember { mutableStateOf(lock.autoLockTimeoutMs()) }
    var hasPin by remember { mutableStateOf(lock.hasPin()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("قفل برنامه", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "PIN با PBKDF2 و ۱۲۰٬۰۰۰ تکرار هش می‌شود و هرگز به‌شکل خام ذخیره نمی‌شود. " +
                    "بیومتریک (اثر انگشت/چهره) فقط راه جایگزینِ بازکردنِ همین قفل است و " +
                    "جای PIN را نمی‌گیرد.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (hasPin) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it.filter(Char::isDigit).take(8) },
                    label = { Text("PIN فعلی") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text(if (hasPin) "PIN جدید" else "PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.filter(Char::isDigit).take(8) },
                label = { Text("تکرار PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(if (hasPin) "تغییر PIN" else "فعال‌کردن قفل") {
                val invalid = lock.validatePin(pin)
                when {
                    hasPin && !lock.verify(currentPin) -> message = "PIN فعلی درست نیست."
                    invalid != null -> message = invalid
                    pin != confirm -> message = "تکرار PIN با PIN جدید یکسان نیست."
                    else -> {
                        val changingExistingPin = hasPin
                        lock.setPin(pin)
                        hasPin = true
                        currentPin = ""
                        pin = ""
                        confirm = ""
                        message = if (changingExistingPin) "PIN با موفقیت تغییر کرد." else "قفل برنامه فعال شد."
                        if (!changingExistingPin && bioStatus == BiometricStatus.READY && activity != null) {
                            // همان لحظهٔ ساخت PIN، یک بار اجازهٔ سیستم را می‌گیریم؛
                            // فعال‌سازی خاموش/روشنِ بی‌تأیید مجاز نیست.
                            BiometricPromptRunner.show(
                                activity = activity,
                                title = "فعال‌کردن ورود بیومتریک",
                                subtitle = "برای ورود سریع‌تر به همیار من هویتت را تأیید کن.",
                                negativeText = "فعلاً نه",
                                onSuccess = {
                                    bioState = bio.setEnabled(true, context)
                                    message = if (bioState) "قفل و ورود بیومتریک فعال شدند." else "قفل فعال شد؛ بیومتریک آماده نبود."
                                },
                                onError = { message = "قفل فعال شد؛ $it" },
                                onCancelled = { message = "قفل فعال شد؛ بیومتریک را هر وقت خواستی از همین‌جا روشن کن." },
                            )
                        }
                    }
                }
            }
            if (hasPin) {
                PrimaryButton("غیرفعال‌کردن قفل") {
                    if (!lock.verify(currentPin)) {
                        message = "برای غیرفعال‌کردن، PIN فعلی را وارد کن."
                    } else {
                        lock.clearPin()
                        bio.clear()
                        hasPin = false
                        currentPin = ""
                        bioState = false
                        message = "قفل برنامه غیرفعال شد."
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("قفل خودکار بعد از", style = MaterialTheme.typography.titleSmall)
            Text(
                "زمان فعلی: " + when (timeoutMs) {
                    30_000L -> "۳۰ ثانیه"
                    60_000L -> "۱ دقیقه"
                    300_000L -> "۵ دقیقه"
                    else -> toPersianDigits((timeoutMs / 1000).toString()) + " ثانیه"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InlineButton("۳۰ ثانیه", Modifier.weight(1f)) { timeoutMs = 30_000; lock.setAutoLockTimeout(timeoutMs) }
                InlineButton("۱ دقیقه", Modifier.weight(1f)) { timeoutMs = 60_000; lock.setAutoLockTimeout(timeoutMs) }
                InlineButton("۵ دقیقه", Modifier.weight(1f)) { timeoutMs = 300_000; lock.setAutoLockTimeout(timeoutMs) }
            }

            Spacer(Modifier.height(8.dp))
            Text("ورود با اثر انگشت / چهره", style = MaterialTheme.typography.titleSmall)
            Text(bioStatus.fa, style = MaterialTheme.typography.bodySmall)
            when {
                !hasPin -> Text(
                    "اول قفل را با PIN فعال کن؛ بیومتریک فقط همان قفل را سریع‌تر باز می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                )
                bioStatus == BiometricStatus.READY && activity != null -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Switch(
                            checked = bioState,
                            onCheckedChange = { want ->
                                if (!want) {
                                    bio.setEnabled(false, context)
                                    bioState = false
                                    message = "بیومتریک خاموش شد؛ فقط PIN کار می‌کند."
                                } else {
                                    // روشن‌کردن بدون تأیید هویت یعنی هرکس گوشی را برداشت
                                    // می‌تواند قفل را دور بزند؛ پس یک بار پرامپت می‌گیریم.
                                    BiometricPromptRunner.show(
                                        activity = activity,
                                        title = "فعال‌کردن ورود بیومتریک",
                                        subtitle = "برای روشن‌کردن این گزینه یک بار هویتت را تأیید کن.",
                                        negativeText = "بی‌خیال",
                                        onSuccess = {
                                            bioState = bio.setEnabled(true, context)
                                            message = if (bioState) {
                                                "روشن شد. از این بعد روی صفحه‌ی قفل دکمه‌ی بیومتریک داری."
                                            } else {
                                                "روشن نشد؛ اول PIN لازم است."
                                            }
                                        },
                                        onError = { text -> message = text },
                                    )
                                }
                            },
                        )
                        Text(
                            if (bioState) "روشن است" else "خاموش است",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        "داده‌های بیومتریک در اپ ذخیره نمی‌شوند و به سرور نمی‌روند؛ فقط نتیجه‌ی «موفق/ناموفق» " +
                            "از سیستم‌عامل گرفته می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> Text(
                    "روی این دستگاه نمی‌توان بیومتریک را روشن کرد؛ همان PIN کار می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scheduler = container.reminders
    val quiet = container.quiet
    fun visibleReminders() = scheduler.all().filterNot {
        it.id == com.hamyareman.ir.ui.ailearning.AI_LESSON_REMINDER_ID ||
            it.id == com.hamyareman.ir.ui.study.SchoolAlarmStore.SCHOOL_M ||
            it.id == com.hamyareman.ir.ui.study.SchoolAlarmStore.SCHOOL_N
    }
    var reminders by remember { mutableStateOf(visibleReminders()) }
    var quietState by remember { mutableStateOf(quiet.state()) }
    var policyGranted by remember { mutableStateOf(QuietHoursAutomation.hasPolicyAccess(context)) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("18") }
    var minute by remember { mutableStateOf("0") }
    var message by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        policyGranted = QuietHoursAutomation.hasPolicyAccess(context)
        QuietHoursAutomation.syncNow(context)
        onPauseOrDispose { }
    }

    fun pickQuietTime(start: Boolean) {
        val h = if (start) quietState.startHour else quietState.endHour
        val m = if (start) quietState.startMinute else quietState.endMinute
        android.app.TimePickerDialog(context, { _, pickedHour, pickedMinute ->
            if (start) {
                quiet.update(startHour = pickedHour, startMinute = pickedMinute)
            } else {
                quiet.update(endHour = pickedHour, endMinute = pickedMinute)
            }
            quietState = quiet.state()
            QuietHoursAutomation.schedule(context)
            scheduler.rescheduleAll()
        }, h, m, true).show()
    }

    fun timeLabel(hourValue: Int, minuteValue: Int): String =
        toPersianDigits("%02d:%02d".format(hourValue, minuteValue))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppTopBar("یادآورها", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "در بازهٔ سکوت، یادآورها متوقف می‌شوند و گوشی با اجازهٔ سیستم روی سکوت کامل می‌رود.",
                style = MaterialTheme.typography.bodySmall,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("ساعات سکوت", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = quietState.enabled,
                            onCheckedChange = { enabled ->
                                quiet.update(enabled = enabled)
                                quietState = quiet.state()
                                QuietHoursAutomation.schedule(context)
                                scheduler.rescheduleAll()
                                message = if (enabled && !policyGranted) {
                                    "برای سایلنت‌شدن گوشی، دسترسی «مزاحم نشو» را فعال کن."
                                } else null
                            },
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InlineButton(
                            "از  ${timeLabel(quietState.startHour, quietState.startMinute)}",
                            Modifier.weight(1f),
                        ) { pickQuietTime(start = true) }
                        InlineButton(
                            "تا  ${timeLabel(quietState.endHour, quietState.endMinute)}",
                            Modifier.weight(1f),
                        ) { pickQuietTime(start = false) }
                    }
                    Text(
                        if (policyGranted) {
                            "دسترسی سکوت گوشی فعال است. در شروع بازه، اعلان وضعیت با گزینهٔ «غیرفعال شود» نمایش داده می‌شود."
                        } else {
                            "برای خاموش‌شدن صدای گوشی و آلارم‌ها، دسترسی ویژهٔ «مزاحم نشو» لازم است."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (policyGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    if (!policyGranted) {
                        TextButton(onClick = {
                            runCatching { context.startActivity(QuietHoursAutomation.policySettingsIntent()) }
                        }) { Text("دادن دسترسی سکوت گوشی") }
                    }
                }
            }

            Text("یادآورهای فعال", style = MaterialTheme.typography.titleMedium)
            if (reminders.isEmpty()) {
                Text("هنوز یادآوری نداری.", style = MaterialTheme.typography.bodySmall)
            }
            reminders.forEach { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reminder.title, style = MaterialTheme.typography.titleSmall)
                            Text(reminder.body, style = MaterialTheme.typography.bodySmall)
                            Text(
                                toPersianDigits("%02d:%02d".format(reminder.hour, reminder.minute)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Switch(
                            checked = reminder.enabled,
                            onCheckedChange = { enabled ->
                                scheduler.setEnabled(reminder.id, enabled)
                                reminders = visibleReminders()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("یادآور تازه", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("متن") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                    label = { Text("ساعت") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                    label = { Text("دقیقه") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryButton("افزودن یادآور") {
                if (title.isBlank()) {
                    message = "عنوان خالی است."
                } else {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 18
                    val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    scheduler.upsert(
                        Reminder(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            body = body.trim().ifBlank { title.trim() },
                            hour = h,
                            minute = m,
                        ),
                    )
                    reminders = visibleReminders()
                    title = ""
                    body = ""
                    message = "یادآور برای ${toPersianDigits("%02d:%02d".format(h, m))} تنظیم شد."
                }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SyncScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    var status by remember { mutableStateOf(SyncCenter.status(ctx, container)) }
    var limit by remember { mutableStateOf(SyncCenter.cacheLimitMb(ctx)) }
    var notice by remember { mutableStateOf<String?>(null) }
    var askClear by remember { mutableStateOf(false) }

    fun mb(bytes: Long): String =
        toPersianDigits(String.format(Locale.US, "%.1f", bytes / 1048576.0))

    fun refresh() {
        status = SyncCenter.status(ctx, container)
    }

    // صفحهٔ سرور عمداً جمع‌تر و بدون فونت دست‌نویس است تا متن‌های فنی یکنواخت خوانده شوند.
    val serverTypography = settingsTypography(
        base = MaterialTheme.typography,
        sizeDelta = -4f,
        forceVazirmatnLight = true,
    )
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = serverTypography,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AppTopBar("تنظیمات سرور", onBack)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                ServerOptionsSection()

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("همگام‌سازی خودکار", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "داده‌های قابل همگام‌سازی ابتدا روی دستگاه ذخیره و هنگام اتصال ارسال می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("وضعیت اتصال", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "اتصال به سرور: " + when {
                                !container.isBackendConfigured -> "پیکربندی نشده (حالت محلی)"
                                status.online -> "آنلاین و آماده"
                                else -> "آفلاین — داده‌ها در صف می‌مانند"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "در صف ارسال: " + toPersianDigits(status.outbox.toString()) +
                                "  •  پیام‌های در انتظار: " + toPersianDigits(status.heartPending.toString()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            if (status.lastSyncAt > 0) {
                                "آخرین همگام‌سازی: " + JalaliDate.stampFa(status.lastSyncAt)
                            } else {
                                "هنوز همگام‌سازی انجام نشده."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        status.lastError?.let {
                            Text(
                                "آخرین خطای سینک: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("کش دستگاه", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "حجمِ کش: " + mb(status.cacheBytes) + " مگابایت از " +
                                toPersianDigits(status.limitMb.toString()) + " مگابایت",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "موارد کش‌شده: " + toPersianDigits(status.cacheItems.toString()) +
                                "  •  رسانه‌ها: " + toPersianDigits(status.mediaFiles.toString()) +
                                " فایل، " + mb(status.mediaBytes) + " مگابایت",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "داده‌های محلی همگام‌سازی‌شده: " +
                                toPersianDigits(status.stateRows.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "سقفِ کش: " + toPersianDigits(limit.toString()) + " مگابایت",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = limit.toFloat(),
                            onValueChange = { limit = it.toInt() },
                            valueRange = SyncCenter.MIN_LIMIT_MB.toFloat()..SyncCenter.MAX_LIMIT_MB.toFloat(),
                        )
                        Text(
                            "با عبور از سقف، قدیمی‌ترین رسانه‌ها پاک و در صورت نیاز دوباره دریافت می‌شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("اعمالِ سقف") {
                                SyncCenter.setCacheLimitMb(ctx, limit)
                                refresh()
                                notice = "سقفِ کش روی " + toPersianDigits(limit.toString()) + " مگابایت تنظیم شد."
                            }
                            TextButton(onClick = { askClear = true }) { Text("پاک‌کردنِ کشِ رسانه") }
                        }
                        notice?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text(
                    "چیزی که هرگز همگام نمی‌شود: ${PrivacyPolicy.neverSyncTables.joinToString("، ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!container.isBackendConfigured) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "همگام‌سازی ابری در این نسخه پیکربندی نشده است.",
                            Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (askClear) {
            AlertDialog(
                onDismissRequest = { askClear = false },
                title = { Text("پاک‌کردنِ کشِ رسانه؟") },
                text = {
                    Text(
                        "فایل‌های صوتی/ویدیویی که دانلود کرده‌ای پاک می‌شوند (برنامه، تیک‌ها و " +
                            "نکته‌ها دست‌نخورده می‌مانند). هر فایلی لازم شود، دوباره از سرور می‌آید.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        SyncCenter.clearMediaCache(ctx)
                        askClear = false
                        refresh()
                        notice = "کشِ رسانه پاک شد."
                    }) { Text("پاک کن") }
                },
                dismissButton = { TextButton(onClick = { askClear = false }) { Text("بی‌خیال") } },
            )
        }
    }
}
