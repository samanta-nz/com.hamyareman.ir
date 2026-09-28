package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * v1.25 — «شرط پخش تدریس: صفحه‌ی تدریس باز باشد» به معنای واقعی‌اش:
 * با خروج از صفحه به هر روشی — دکمه‌ی هوم، دکمه‌ی پنجره‌ها، سوییچ اپ،
 * خاموش/قفل‌شدن صفحه — پخش مکث می‌شود (دکمه‌ی بک از قبل با onDispose همین کار را می‌کند).
 * برگشت به صفحه پخش را از سر نمی‌گیرد؛ کاربر خودش پلی می‌زند (موقعیت حافظه‌دار است).
 */
@Composable
fun PauseOnStopEffect(pause: () -> Unit, stop: (() -> Unit)? = null) {
    val owner = LocalLifecycleOwner.current
    val halt = stop ?: pause
    DisposableEffect(owner) {
        com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
                    pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
                    halt()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
            halt()
        }
    }
}
