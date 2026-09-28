package com.hamyareman.ir.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import kotlinx.coroutines.launch

/** درخواست دستی از کارت «آپدیت برنامه» — همان صفحهٔ دانلود را باز می‌کند. */
object UpdateUi {
    var session by mutableStateOf<Pair<UpdateInfo, Boolean>?>(null)
}

/**
 * کانالِ آپدیت:
 * اگر نسخه‌ی تازه‌ای باشد، فقط همان‌وقت اعلام می‌شود — اختیاری با «بعداً»، اجباری بدون آن.
 * اعلان جدا و overlay همیشگی نیست.
 */
@Composable
fun UpdateGateHost() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current

    LaunchedEffect(Unit) {
        val d = UpdateChecker.decide(ctx, container.tables, BuildConfig.VERSION_CODE, forceNetwork = true)
        val info: UpdateInfo = when (d) {
            is UpdateDecision.Forced -> d.info
            is UpdateDecision.Optional -> d.info
            else -> return@LaunchedEffect
        }
        UpdateNotes.refresh(ctx)
        if (UpdateUi.session == null) {
            UpdateUi.session = info to (d is UpdateDecision.Forced)
        }
    }

    UpdateUi.session?.let { (info, forced) ->
        UpdateDownloadScreen(
            info = info,
            forced = forced,
            onClose = { UpdateUi.session = null },
        )
    }
}

@Composable
private fun DownloadBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * صفحهٔ تمام‌صفحه‌ی دانلود و نصب آپدیت.
 * در حالت اجباری دکمهٔ بازگشت و بستن کار نمی‌کند.
 */
@Composable
fun UpdateDownloadScreen(info: UpdateInfo, forced: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<ApkProgress?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(ApkUpdate.isReady(ctx)) }
    var askedPermission by remember { mutableStateOf(false) }
    var repoNotes by remember {
        mutableStateOf(UpdateNotes.notes(ctx, UpdatePlan.versionLabel(info)))
    }

    LaunchedEffect(info.latest) {
        if (UpdateNotes.refresh(ctx)) {
            repoNotes = UpdateNotes.notes(ctx, UpdatePlan.versionLabel(info))
        }
    }
    val shownNotes = repoNotes.ifEmpty { info.notes }
    val versionFa = toPersianDigits(UpdatePlan.versionLabel(info))

    fun installNow() {
        note = null
        val identityError = if (ApkUpdate.isReady(ctx)) ApkUpdate.identityError(ctx, info) else null
        when {
            !ApkUpdate.isReady(ctx) -> note = "فایلِ نصبی آماده نیست؛ اول دانلودش کن."
            !ApkUpdate.verify(ctx, info.sha256) -> {
                ApkUpdate.clear(ctx)
                ready = false
                note = "فایلِ دانلودشده سالم نبود و پاک شد؛ یک‌بار دیگر دانلود کن."
            }
            identityError != null -> {
                ApkUpdate.clear(ctx)
                ready = false
                note = identityError + " فایل حذف شد."
            }
            !ApkUpdate.canInstall(ctx) -> {
                if (!askedPermission) {
                    askedPermission = true
                    ApkUpdate.openInstallPermission(ctx)
                    note = "اجازهٔ «نصب برنامه‌های ناشناس» را برای همیار من روشن کن؛ بعد خودش نصب می‌شود."
                } else {
                    ApkUpdate.openInstallPermission(ctx)
                    note = "هنوز مجوز نصب داده نشده. بعد از روشن‌کردنش به برنامه برگرد."
                }
            }
            !ApkUpdate.install(ctx, info) -> {
                ApkUpdate.explain(ctx)
                note = "نصب‌کننده باز نشد؛ یک‌بار دیگر بزن."
            }
        }
    }

    fun afterDownloadOk() {
        ready = true
        if (!ApkUpdate.canInstall(ctx) && !askedPermission) {
            askedPermission = true
            ApkUpdate.openInstallPermission(ctx)
            note = "دانلود کامل شد. اجازهٔ نصب از منبع ناشناس را یک‌بار تأیید کن."
        } else {
            installNow()
        }
    }

    fun startDownload() {
        note = null
        downloading = true
        progress = ApkProgress(0, info.size, 0)
        scope.launch {
            val f = ApkUpdate.download(ctx, info.url) { p -> progress = p }
            downloading = false
            when {
                f == null -> note = "دانلود کامل نشد؛ اینترنت را چک کن و دوباره بزن (از همان‌جا ادامه می‌دهد)."
                !ApkUpdate.verify(ctx, info.sha256) -> {
                    ApkUpdate.clear(ctx)
                    note = "فایلِ دانلودشده سالم نبود و پاک شد؛ یک‌بار دیگر بزن."
                }
                ApkUpdate.identityError(ctx, info) != null -> {
                    val why = ApkUpdate.identityError(ctx, info).orEmpty()
                    ApkUpdate.clear(ctx)
                    note = "$why فایل حذف شد."
                }
                else -> afterDownloadOk()
            }
        }
    }

    DisposableEffect(ready, askedPermission) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && ready && !downloading) {
                if (ApkUpdate.canInstall(ctx)) installNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun later() {
        if (forced) return
        UpdateChecker.skip(ctx, info.latest)
        onClose()
    }

    BackHandler(enabled = forced) { }

    Dialog(
        onDismissRequest = { if (!forced) later() },
        properties = DialogProperties(
            dismissOnBackPress = !forced,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text("آپدیت برنامه", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "نسخه‌ی تو: ${BuildConfig.VERSION_NAME}" +
                    "  •  نسخه‌ی تازه: $versionFa" +
                    if (info.size > 0) "  •  حجم: ${UpdatePlan.sizeLabel(info.size)}" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (forced) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "این به‌روزرسانی اجباری است. برای ادامه باید نسخه‌ی تازه نصب شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("تغییرات این نسخه", style = MaterialTheme.typography.titleMedium)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shownNotes.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }
                if (shownNotes.isEmpty()) {
                    Text("متن تغییرات هنوز نرسیده.", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (downloading) {
                val p = progress ?: ApkProgress(0, info.size, 0)
                DownloadBar(p.percent / 100f)
                Spacer(Modifier.height(6.dp))
                Text(
                    "نسخهٔ $versionFa — " + toPersianDigits("${p.percent}") + "٪",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    p.sizeText + "  •  " + p.speedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            } else if (ready) {
                Text(
                    "دانلود کامل شد ✅ — نصب از داخل برنامه انجام می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }

            when {
                downloading -> PrimaryButton("در حال دانلود…") { }
                ready -> PrimaryButton("نصب نسخه‌ی تازه") { installNow() }
                else -> PrimaryButton("دانلود و نصب") { startDownload() }
            }
            if (!forced) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (ready) {
                        TextButton(onClick = {
                            ApkUpdate.clear(ctx)
                            ready = false
                            startDownload()
                        }) { Text("دانلود مجدد") }
                    }
                    TextButton(onClick = { later() }) { Text("بعداً") }
                }
            }
        }
    }
}

/** کارتِ «آپدیت برنامه» برای صفحهٔ «بیشتر». */
@Composable
fun UpdateCheckCard() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔄 آپدیت برنامه", style = MaterialTheme.typography.titleMedium)
            Text(
                "نسخه‌ی نصب‌شده: ${BuildConfig.VERSION_NAME}" +
                    if (UpdateChecker.cached(ctx) != null) {
                        "  •  آخرین اعلام‌شده: " + UpdatePlan.versionLabel(UpdateChecker.cached(ctx)!!)
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    status = null
                    scope.launch {
                        val info = UpdateChecker.refresh(ctx, container.tables)
                        if (info == null || info.url.isBlank() || info.latest <= 0) {
                            status = "الان نتوانستم از سرور بپرسم؛ بعداً دوباره امتحان کن."
                        } else if (info.latest <= BuildConfig.VERSION_CODE) {
                            status = "همین نسخه را داری؛ «" + UpdatePlan.versionLabel(info) +
                                "» آخرین نسخه است ✅"
                        } else {
                            val d = UpdatePlan.decisionFor(BuildConfig.VERSION_CODE, info, bucket = 0)
                            UpdateNotes.refresh(ctx)
                            // بررسی دستی هم اگر سرور min گذاشته باشد اجباری است.
                            UpdateUi.session = info to (d is UpdateDecision.Forced)
                        }
                        busy = false
                    }
                },
            ) {
                Text(if (busy) "در حال بررسی…" else "بررسیِ نسخه‌ی تازه")
            }
        }
    }
}
