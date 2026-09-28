package com.hamyareman.ir.ui.chatbot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.Helplines
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * «همراه زهرا» — گفت‌وگوی امن با سه لایه:
 * ایمنی محلی (بحران) ← لایه‌ی AI سمت سرور ← قواعد محلی.
 *
 * هر پاسخ برچسب منبع دارد تا کاربر بداند **کی** جواب داده: مدل، قواعد محلی،
 * یا پاسخ ایمنی. تاریخچه فقط روی دستگاه می‌ماند.
 */
@Composable
fun ChatScreen(onSettings: () -> Unit, onHelplines: () -> Unit) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var lines by remember { mutableStateOf(c.ai.lines()) }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    // اولین اجرا: یک سلام کوتاه، بدون پرکردن حافظه‌ی کاربر با متن ساختگی.
    LaunchedEffect(Unit) {
        if (lines.isEmpty()) {
            c.ai.append(
                ChatLine(
                    id = "welcome",
                    fromBot = true,
                    text = AiCompanion.WELCOME,
                    createdAt = System.currentTimeMillis(),
                    source = ReplySource.RULES_LOCAL,
                ),
            )
            lines = c.ai.lines()
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    if (c.ai.chatDisabled) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTopBar("همراه زهرا")
            SectionCard(
                title = "همراه خاموش است",
                body = "خودت از تنظیمات خاموشش کردی. هر وقت خواستی دوباره روشنش کن؛ حافظه‌ی گفت‌وگو پاک نشده.",
            ) { }
            PrimaryButton("تنظیمات همراه", onSettings)
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("همراه زهرا", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    !c.ai.aiEnabled -> "قواعد محلی"
                    !c.ai.isConfigured -> "بدون سرور"
                    else -> "لایه‌ی AI · لحن ${c.ai.tone.label}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lines, key = { it.id }) { line ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (line.fromBot) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(line.text, style = MaterialTheme.typography.bodyLarge)
                        if (line.fromBot) {
                            Text(
                                "${line.source.label} · ${JalaliDate.clockFa(line.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (line.crisis) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("شماره‌های کمک (رایگان)", style = MaterialTheme.typography.titleSmall)
                            Helplines.iran.take(4).forEach { h ->
                                Text("${h.name}: ${h.number} (${h.hours})", style = MaterialTheme.typography.bodyMedium)
                            }
                            PrimaryButton("دیدن همه‌ی شماره‌ها", onHelplines)
                        }
                    }
                }
            }
            if (thinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.height(18.dp))
                        Text("در حال فکر کردن…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("پیام") },
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (thinking) "در حال ارسال…" else "بفرست") {
            val text = input.trim()
            if (text.isBlank() || thinking) return@PrimaryButton
            c.ai.userLine(text)
            lines = c.ai.lines()
            input = ""
            thinking = true
            notice = null
            scope.launch {
                val reply = c.ai.replyTo(text)
                c.ai.botLine(reply)
                lines = c.ai.lines()
                thinking = false
                notice = when (reply.source) {
                    ReplySource.MODEL -> null
                    ReplySource.SAFETY -> "این پاسخ ایمنی محلی است؛ لطفاً با یک آدم واقعی حرف بزن."
                    ReplySource.SERVER_FALLBACK -> "به مدل نرسیدم؛ با قواعد محلی جواب دادم."
                    ReplySource.RULES_LOCAL -> if (c.ai.isConfigured && !c.ai.aiEnabled) {
                        "لایه‌ی AI خاموش است؛ از تنظیمات روشنش کن."
                    } else {
                        "بک‌اند تنظیم نشده؛ پاسخ از قواعد محلی است."
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("تنظیمات چت", onSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    val c = LocalAppContainer.current
    var aiEnabled by remember { mutableStateOf(c.ai.aiEnabled) }
    var tone by remember { mutableStateOf(c.ai.tone) }
    var disabled by remember { mutableStateOf(c.ai.chatDisabled) }
    var stored by remember { mutableStateOf(c.ai.lines().size) }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنظیمات همراه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = "حریم خصوصی این گفت‌وگو",
                body = "تاریخچه‌ی چت فقط روی گوشی تو می‌ماند و در فهرست «هرگز sync نمی‌شود» است. " +
                    "وقتی لایه‌ی AI روشن باشد، متن پیام برای ساختن پاسخ به تابع سرور خودمان می‌رود؛ " +
                    "کلید مدل در اپ نیست و متن پیام در سرور ذخیره یا لاگ نمی‌شود.",
            ) { }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = aiEnabled,
                    onCheckedChange = {
                        aiEnabled = it
                        c.ai.aiEnabled = it
                    },
                )
                Column {
                    Text("پاسخ با لایه‌ی AI (سمت سرور)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (c.ai.isConfigured) "بک‌اند وصل است." else "بک‌اند پیکربندی نشده؛ فعلاً قواعد محلی جواب می‌دهد.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Text("لحن پاسخ", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatTone.entries.forEach { item ->
                    FilterChip(
                        selected = tone == item,
                        onClick = {
                            tone = item
                            c.ai.tone = item
                        },
                        label = { Text(item.label) },
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = disabled,
                    onCheckedChange = {
                        disabled = it
                        c.ai.chatDisabled = it
                    },
                )
                Text("خاموش‌کردن کامل همراه", style = MaterialTheme.typography.bodyLarge)
            }

            SectionCard(
                title = "حافظه‌ی گفت‌وگو",
                body = "$stored خط روی این دستگاه ذخیره شده. پاک‌کردن فقط همین دستگاه را خالی می‌کند.",
            ) { }
            PrimaryButton("پاک‌کردن حافظهٔ گفت‌وگو") {
                c.ai.clearHistory()
                stored = 0
                notice = "حافظه‌ی گفت‌وگو پاک شد."
            }

            SectionCard(
                title = "ایمنی",
                body = "پیام‌هایی که نشانه‌ی آسیب به خود دارند هرگز به مدل فرستاده نمی‌شوند؛ " +
                    "پاسخ ایمنی و شماره‌های کمک بلافاصله و آفلاین نشان داده می‌شوند.",
            ) { }

            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            PrimaryButton("بازگشت", onBack)
        }
    }
}
