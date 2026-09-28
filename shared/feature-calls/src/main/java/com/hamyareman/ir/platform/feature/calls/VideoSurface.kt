package com.hamyareman.ir.platform.feature.calls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * رندر ویدیو داخل Compose.
 *
 * نکته‌ی مهم: `SurfaceViewRenderer` باید با **همان** کانتکست EGL که فکتوری WebRTC
 * ساخته شده init شود ([CallEngine.eglContext])، وگرنه تصویر سیاه می‌شود.
 *
 * [mirror] فقط برای تصویر محلی (دوربین جلو) درست است؛ تصویر طرف مقابل نباید آینه‌ای باشد.
 */
@Composable
fun VideoSurface(
    track: VideoTrack?,
    eglContext: EglBase.Context,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
) {
    val context = LocalContext.current
    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
            setMirror(mirror)
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(true)
        }
    }

    DisposableEffect(track) {
        track?.addSink(renderer)
        onDispose {
            runCatching { track?.removeSink(renderer) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { renderer.release() }
        }
    }

    AndroidView(factory = { renderer }, modifier = modifier)
}
