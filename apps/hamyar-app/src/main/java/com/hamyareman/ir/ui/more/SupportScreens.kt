package com.hamyareman.ir.ui.more

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.appwrite.RowPermissions
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/** معرفی محصول؛ عمداً هیچ نام شخصی یا نام سازنده‌ای در این صفحه نمی‌آید. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("درباره ما", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("همیار من", style = MaterialTheme.typography.headlineSmall)
            Text(
                "همیار من یک همراه آموزشی و شخصی برای برنامه‌ریزی درس، تمرین مهارت‌ها، " +
                    "سلامتی و نگهداری امن یادداشت‌هاست. هدف برنامه این است که ابزارهای روزمره " +
                    "در یک فضای ساده، فارسی و قابل‌اعتماد در دسترس باشند.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionCard(
                "حریم خصوصی",
                "بخش‌های حساس با قفل مستقل و رمزگذاری دستگاه محافظت می‌شوند. " +
                    "هرجا داده‌ای برای همگام‌سازی یا پشتیبانی به سرور فرستاده شود، همان‌جا صریح اعلام می‌شود.",
            ) { }
            SectionCard(
                "نسخه برنامه",
                BuildConfig.VERSION_NAME,
            ) { }
        }
    }
}

/** فرم واقعی پشتیبانی؛ درخواست مستقیماً به جدول سروری موجود app_state فرستاده می‌شود. */
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تماس با ما", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "پیامت مستقیماً برای پشتیبانی فرستاده می‌شود. لطفاً رمز، PIN یا اطلاعات بانکی را ننویس.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it.take(120); notice = null },
                label = { Text("موضوع") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(4000); notice = null },
                label = { Text("متن پیام") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(if (sending) "در حال ارسال…" else "ارسال برای پشتیبانی") {
                when {
                    sending -> Unit
                    subject.trim().length < 3 -> notice = "موضوع را کمی کامل‌تر بنویس."
                    message.trim().length < 10 -> notice = "متن پیام باید دست‌کم ۱۰ نویسه باشد."
                    else -> {
                        sending = true
                        notice = null
                        scope.launch {
                            val uid = container.auth.cachedUserId()
                                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                            if (uid.isBlank()) {
                                sending = false
                                success = false
                                notice = "برای ارسال درخواست باید وارد حساب باشی."
                                return@launch
                            }
                            val now = System.currentTimeMillis()
                            val payload = JSONObject()
                                .put("subject", subject.trim())
                                .put("message", message.trim())
                                .put("status", "new")
                                .put("appVersion", BuildConfig.VERSION_NAME)
                                .put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(120))
                                .put("createdAtMs", now)
                                .toString()
                            val result = container.tables.create(
                                tableId = TableIds.APP_STATE,
                                rowId = "sp_${UUID.randomUUID().toString().replace("-", "")}",
                                permissions = RowPermissions.forUser(uid),
                                data = mapOf(
                                    "userId" to uid,
                                    "key" to "support_ticket",
                                    "payload" to payload,
                                    "updatedAt" to now,
                                ),
                            )
                            sending = false
                            when (result) {
                                is AppResult.Ok -> {
                                    success = true
                                    notice = "درخواستت ثبت شد. پشتیبانی آن را از پنل سرور می‌بیند."
                                    subject = ""
                                    message = ""
                                }
                                is AppResult.Err -> {
                                    success = false
                                    notice = result.error.userMessage
                                }
                            }
                        }
                    }
                }
            }
            notice?.let {
                Text(
                    it,
                    color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
