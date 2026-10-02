package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.AdminApi
import com.hamyareman.admin.AdminUpdateChecker
import com.hamyareman.admin.AdminUpdateInfo
import com.hamyareman.admin.BuildConfig
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.ParsPackStorage
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.launch

/** تنظیم امن سرویس‌ها؛ رمزها صرفاً در Android Keystore دستگاه مدیر نگه‌داری می‌شوند. */
@Composable
fun AdminSettingsScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val container = LocalAdmin.current
    val prefs = container.prefs
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(prefs.name) }
    var endpoint by remember { mutableStateOf(prefs.endpoint) }
    var project by remember { mutableStateOf(prefs.projectId) }
    var database by remember { mutableStateOf(prefs.databaseId) }
    var bucket by remember { mutableStateOf(prefs.bucketId) }
    var supportTable by remember { mutableStateOf(prefs.supportTableId) }
    var apiKey by remember { mutableStateOf("") }

    var ppEndpoint by remember { mutableStateOf(prefs.parsPackEndpoint) }
    var ppBucket by remember { mutableStateOf(prefs.parsPackBucket) }
    var ppPublic by remember { mutableStateOf(prefs.parsPackPublicBase) }
    var ppRegion by remember { mutableStateOf(prefs.parsPackRegion) }
    var ppPrefix by remember { mutableStateOf(prefs.parsPackPrefix) }
    var ppAccess by remember { mutableStateOf("") }
    var ppSecret by remember { mutableStateOf("") }

    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AdminUpdateInfo?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun save() {
        // فیلد secret خالی یعنی «مقدار vault را دست نزن»؛ پاک‌سازی فقط با تأیید جداگانه است.
        prefs.saveAppwrite(
            name = name,
            endpoint = endpoint,
            projectId = project,
            databaseId = database,
            bucketId = bucket,
            supportTableId = supportTable,
            apiKey = apiKey.ifBlank { prefs.apiKey },
        )
        prefs.saveParsPack(
            endpoint = ppEndpoint,
            bucket = ppBucket,
            publicBase = ppPublic,
            region = ppRegion,
            prefix = ppPrefix,
            accessKey = ppAccess.ifBlank { prefs.parsPackAccessKey },
            secretKey = ppSecret.ifBlank { prefs.parsPackSecretKey },
        )
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنظیمات و دسترسی‌ها", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الگوی امن اتصال", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "شناسه‌ها عمومی‌اند. کلیدهای عملیاتی فقط با Android Keystore رمز می‌شوند، در backup منتقل نمی‌شوند و داخل APK یا git قرار ندارند. برای دسترسی API، کلیدی با کمترین scope لازم بسازید و در صورت گم‌شدن دستگاه آن را rotate کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsHeading("Appwrite · داده، کاربران و Storage")
            OutlinedTextField(name, { name = it }, label = { Text("نام محیط") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("API endpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(project, { project = it }, label = { Text("Project ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(database, { database = it }, label = { Text("Database ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(bucket, { bucket = it }, label = { Text("Content / media bucket ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(supportTable, { supportTable = it }, label = { Text("جدول پیشنهادها / تیکت‌ها") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            SecretField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = if (prefs.hasAppwriteCredential) "API key جدید (کلید فعلی محفوظ است)" else "Appwrite API key",
            )
            Text(
                "کلید رمز HTML به‌صورت خودکار از ردیف app_state/html_media_key در همین دیتابیس دریافت و فقط در vault رمز‌شدهٔ این دستگاه cache می‌شود؛ نیازی به وارد کردن دستی آن نیست.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsHeading("ParsPack · باکت داخلی")
            Text("این باکت با virtual-host و امضای SigV2 تنظیم شده است. کلیدهای S3 فقط در vault دستگاه ادمین باقی می‌مانند.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(ppEndpoint, { ppEndpoint = it }, label = { Text("S3 endpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ppBucket, { ppBucket = it }, label = { Text("Bucket") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ppPublic, { ppPublic = it }, label = { Text("Public base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ppRegion, { ppRegion = it }, label = { Text("Region") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(ppPrefix, { ppPrefix = it }, label = { Text("پیشوند اولیهٔ مرور فایل‌ها (اختیاری)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            SecretField(
                value = ppAccess,
                onValueChange = { ppAccess = it },
                label = if (prefs.parsPackAccessKey.isNotBlank()) "Access key جدید (فعلی محفوظ است)" else "ParsPack access key",
            )
            SecretField(
                value = ppSecret,
                onValueChange = { ppSecret = it },
                label = if (prefs.parsPackSecretKey.isNotBlank()) "Secret key جدید (فعلی محفوظ است)" else "ParsPack secret key",
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    save()
                    busy = true; error = null; info = null
                    scope.launch {
                        val appwrite = AdminApi(prefs.endpoint, prefs.projectId, prefs.apiKey, prefs.databaseId, prefs.bucketId)
                        val aw = adminIo { appwrite.ping() }
                        val ppMessage = if (prefs.hasParsPackCredential) {
                            try {
                                "ParsPack: ${ParsPackStorage(prefs).list(prefs.parsPackPrefix).size} فایل در این صفحه"
                            } catch (t: Throwable) {
                                "ParsPack: ${t.message ?: "آزمون اتصال ناموفق بود."}"
                            }
                        } else {
                            "کلید ParsPack هنوز وارد نشده است"
                        }
                        when (aw) {
                            is AppResult.Ok -> info = "Appwrite: ${aw.value} · $ppMessage"
                            is AppResult.Err -> error = aw.error.userMessage
                        }
                        busy = false
                    }
                },
                enabled = !busy && endpoint.startsWith("https://") && project.isNotBlank() && database.isNotBlank() && (apiKey.isNotBlank() || prefs.hasAppwriteCredential),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "در حال آزمون…" else "ذخیره و آزمون اتصال") }

            OutlinedButton(
                onClick = { save(); onSaved() },
                enabled = !busy && endpoint.startsWith("https://") && project.isNotBlank() && database.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیرهٔ تنظیمات") }

            OutlinedButton(
                onClick = {
                    busy = true; error = null; info = null
                    scope.launch {
                        runCatching { AdminUpdateChecker.check() }
                            .onSuccess { remote ->
                                if (remote == null || !AdminUpdateChecker.hasUpdate(remote)) {
                                    info = "نسخهٔ ${BuildConfig.VERSION_NAME} همین حالا جدیدترین نسخه است."
                                } else {
                                    updateInfo = remote
                                }
                            }
                            .onFailure { error = it.message ?: "بررسی به‌روزرسانی ناموفق بود." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) { Text("بررسی به‌روزرسانی ادمین") }

            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("پاک‌کردن کلیدهای این دستگاه")
            }
        }
    }

    updateInfo?.let { update ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("به‌روزرسانی ${update.versionName} آماده است") },
            text = {
                Text(
                    "نسخهٔ نصب‌شده: ${BuildConfig.VERSION_NAME}\n" +
                        "نسخهٔ جدید: ${update.versionName}\n" +
                        "دانلود از ParsPack با نصب‌کنندهٔ رسمی Android باز می‌شود. امضای نسخهٔ جدید باید همان امضای ادمین فعلی باشد.",
                )
            },
            confirmButton = { TextButton(onClick = { AdminUpdateChecker.openDownload(context, update); updateInfo = null }) { Text("دانلود و نصب") } },
            dismissButton = { TextButton(onClick = { updateInfo = null }) { Text("بعداً") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("کلیدها پاک شوند؟") },
            text = { Text("API key، کلیدهای ParsPack و کلید HTML فقط از vault این گوشی حذف می‌شوند. نشانی‌ها و شناسه‌ها باقی می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    prefs.clearSecrets()
                    confirmClear = false
                    onSaved()
                }) { Text("پاک کن") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
