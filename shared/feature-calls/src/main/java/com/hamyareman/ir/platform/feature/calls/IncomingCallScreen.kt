package com.hamyareman.ir.platform.feature.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * میزبانِ تماس ورودی: دیدبانی را شروع می‌کند، زنگ را روی هر صفحه‌ای نشان می‌دهد و
 * بعد از «پاسخ»، همان [CallScreen] را بالا می‌آورد.
 *
 * چرا در سطح Activity و نه داخل NavHost؟ چون زنگ باید روی **هر** صفحه‌ای دیده شود
 * (خانه، آلبوم، تنظیمات…) و گم‌شدن در پشته‌ی مسیرها یعنی تماسِ از دست رفته.
 *
 * نکته‌ی حریم خصوصی: این میزبان فقط در شاخه‌ی «قفل بازشده» نصب می‌شود؛ تا اپ قفل
 * است حتی نام تماس‌گیرنده هم روی صفحه نمی‌آید.
 */
@Composable
fun IncomingCallsHost(
    watcher: IncomingCallWatcher,
    engine: CallEngine,
    phoneFallback: String,
    remoteLabel: String,
    remoteUserId: String? = null,
) {
    val scope = rememberCoroutineScope()
    val call by watcher.incoming.collectAsState()
    val session by engine.session.collectAsState()
    var accepted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { watcher.start() }
    DisposableEffect(Unit) { onDispose { watcher.stop() } }

    // وقتی تماس تمام شد، صفحه‌ی پاسخ خودش بسته می‌شود.
    LaunchedEffect(session.phase) {
        if (session.phase == CallPhase.Idle || session.phase == CallPhase.Ended) accepted = false
    }

    IncomingCallOverlay(
        call = call,
        onAccept = { incoming ->
            accepted = true
            watcher.clear()
            engine.acceptIncoming(incoming.callId, incoming.fromLabel, incoming.fromUserId)
        },
        onDecline = { incoming -> scope.launch { watcher.decline(incoming) } },
    )

    if (accepted) {
        CallScreen(
            engine = engine,
            onClose = { accepted = false },
            fatherTel = phoneFallback,
            remoteUserId = remoteUserId,
            remoteLabel = remoteLabel,
        )
    }
}

/** زنگِ تمام‌صفحه: نام تماس‌گیرنده + «پاسخ» و «رد کردن». */
@Composable
fun IncomingCallOverlay(
    call: IncomingCall?,
    onAccept: (IncomingCall) -> Unit,
    onDecline: (IncomingCall) -> Unit,
) {
    if (call == null) return
    Dialog(
        // عقب‌رفتن با دکمه‌ی Back = ردکردن تماس (مثل گوشی).
        onDismissRequest = { onDecline(call) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("تماس ورودی", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            Text(call.fromLabel, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                if (call.kind == CallKind.VIDEO) "تماس تصویری" else "تماس صوتی",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onDecline(call) }) { Text("رد کردن") }
                Spacer(Modifier.width(4.dp))
                Button(onClick = { onAccept(call) }) { Text("پاسخ") }
            }
        }
    }
}
