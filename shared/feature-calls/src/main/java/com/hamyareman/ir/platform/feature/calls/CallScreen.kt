package com.hamyareman.ir.platform.feature.calls

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton

/**
 * صفحه‌ی تماس.
 *
 * [remoteUserId] شناسه‌ی Appwrite طرف مقابل است؛ اگر null باشد (یعنی پیوند فعال نیست
 * یا سرور پیکربندی نشده) فقط «زنگ با تلفن معمولی» کار می‌کند و دلیلش هم صادقانه
 * به کاربر گفته می‌شود.
 */
@Composable
fun CallScreen(
    engine: CallEngine,
    onClose: () -> Unit,
    fatherTel: String,
    remoteUserId: String? = null,
    remoteLabel: String = "بابا",
) {
    val context = LocalContext.current
    val session by engine.session.collectAsState()
    val localVideo by engine.localVideo.collectAsState()
    val remoteVideo by engine.remoteVideo.collectAsState()
    var micGranted by remember { mutableStateOf(engine.hasMicPermission()) }
    var cameraGranted by remember { mutableStateOf(engine.hasCameraPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] ?: micGranted
        cameraGranted = result[Manifest.permission.CAMERA] ?: cameraGranted
    }

    DisposableEffect(Unit) {
        onDispose { engine.reset() }
    }

    Scaffold(topBar = { AppTopBar("تماس", onClose) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (session.kind == CallKind.VIDEO && session.isActive) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val main = remoteVideo ?: localVideo
                    VideoSurface(
                        track = main,
                        eglContext = engine.eglContext,
                        modifier = Modifier.fillMaxSize(),
                        mirror = remoteVideo == null,
                    )
                    if (localVideo != null && remoteVideo != null) {
                        VideoSurface(
                            track = localVideo,
                            eglContext = engine.eglContext,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(width = 96.dp, height = 128.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            mirror = true,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("تماس با $remoteLabel", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                phaseFa(session.phase),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            if (session.phase == CallPhase.InCall && session.connectedAtMs != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "از ${JalaliDate.clockFa(session.connectedAtMs ?: 0L)} متصل هستیم",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            session.notice?.let {
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!micGranted || !cameraGranted) {
                Spacer(Modifier.height(12.dp))
                PrimaryButton("اجازه‌ی میکروفون و دوربین") {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "اگر قبلاً رد کرده‌ای، باید از تنظیمات اپ اجازه را روشن کنی.",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(4.dp))
                PrimaryButton("بازکردن تنظیمات اپ") {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (session.isActive) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InlineButton(
                        text = if (session.micMuted) "روشن‌کردن میکروفون" else "بی‌صدا",
                        onClick = { engine.toggleMic() },
                        modifier = Modifier.weight(1f),
                    )
                    InlineButton(
                        text = if (session.speakerOn) "گوشی" else "بلندگو",
                        onClick = { engine.toggleSpeaker() },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (session.kind == CallKind.VIDEO) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InlineButton(
                            text = if (session.videoMuted) "روشن‌کردن دوربین" else "خاموش‌کردن دوربین",
                            onClick = { engine.toggleVideo() },
                            modifier = Modifier.weight(1f),
                        )
                        InlineButton(
                            text = "چرخاندن دوربین",
                            onClick = { engine.switchCamera() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("قطع تماس") {
                    engine.hangup()
                    onClose()
                }
            } else {
                PrimaryButton("تماس صوتی آنلاین") {
                    engine.startCall(CallKind.AUDIO, remoteLabel, remoteUserId)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("تماس تصویری") {
                    engine.startCall(CallKind.VIDEO, remoteLabel, remoteUserId)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    if (fatherTel.isBlank()) "شماره‌ی بابا ثبت نشده" else "زنگ بزن با تلفن معمولی",
                ) { engine.dialTel(fatherTel) }
            }

            Spacer(Modifier.height(16.dp))
            if (session.phase == CallPhase.Ended && session.startedAtMs > 0) {
                Text(
                    toPersianDigits("آخرین تلاش: ${JalaliDate.stampFa(session.startedAtMs)}"),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton("بستن", onClose)
        }
    }
}

private fun phaseFa(phase: CallPhase) = when (phase) {
    CallPhase.Idle -> "آماده"
    CallPhase.Dialing -> "در حال شماره‌گیری…"
    CallPhase.Ringing -> "داره زنگ می‌خوره…"
    CallPhase.Connecting -> "در حال وصل شدن…"
    CallPhase.InCall -> "متصل"
    CallPhase.Ended -> "تمام شد"
    CallPhase.Failed -> "وصل نشد"
}
