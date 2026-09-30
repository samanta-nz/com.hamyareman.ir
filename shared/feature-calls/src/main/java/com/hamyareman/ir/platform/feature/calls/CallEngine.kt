package com.hamyareman.ir.platform.feature.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.hamyareman.ir.platform.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.Collections
import java.util.UUID

/** سرور ICE؛ برای عبور از NATهای سخت‌گیر به TURN نیاز است (مقادیر از سرور/BuildConfig می‌آید). */
data class IceServerConfig(
    val url: String,
    val username: String? = null,
    val credential: String? = null,
)

/**
 * موتور تماس صوتی WebRTC + سیگنالینگ روی Appwrite.
 *
 * جریان کار:
 *  1) مجوز میکروفون بررسی می‌شود.
 *  2) سطر `call_sessions` ساخته می‌شود تا طرف مقابل زنگ را ببیند.
 *  3) offer ساخته و در `call_signals` نوشته می‌شود.
 *  4) حلقه‌ی [pollSignals] پاسخ و کاندیداهای ICE طرف مقابل را می‌خواند.
 *  5) اتصال که برقرار شد فاز [CallPhase.InCall] می‌شود؛ در پایان سطر جلسه بسته می‌شود.
 *
 * تماس تصویری: دوربین جلو با `Camera2Enumerator` گرفته می‌شود، ترک ویدیو به
 * PeerConnection اضافه می‌شود و رندر سمت UI با `VideoSurface` انجام می‌شود.
 * اگر دوربین در دسترس نباشد یا مجوز نداشته باشیم، تماس **به صوتی ادامه می‌دهد** و
 * دلیلش صادقانه در [CallSession.notice] نوشته می‌شود (قطع کامل، تنبیه کاربر است).
 */
class CallEngine(
    private val context: Context,
    private val signaling: CallSignaling? = null,
    private val selfUserId: suspend () -> String? = { null },
    private val iceServers: List<IceServerConfig> = DEFAULT_ICE_SERVERS,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val egl: EglBase by lazy { EglBase.create() }
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var localAudioTrack: org.webrtc.AudioTrack? = null
    private var pollJob: Job? = null

    // --- ویدیو ---
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    private val _localVideo = MutableStateFlow<VideoTrack?>(null)
    private val _remoteVideo = MutableStateFlow<VideoTrack?>(null)

    private val _session = MutableStateFlow(CallSession())
    val session: StateFlow<CallSession> = _session

    /** ترک ویدیوی محلی برای رندر در `VideoSurface`. */
    val localVideo: StateFlow<VideoTrack?> = _localVideo

    /** ترک ویدیوی طرف مقابل (وقتی تماس تصویری وصل شود). */
    val remoteVideo: StateFlow<VideoTrack?> = _remoteVideo

    /**
     * کانتکست EGL همان است که فکتوری WebRTC استفاده می‌کند؛ رندرها باید با همین
     * مقدار init شوند تا سطح ویدیو سیاه نشود.
     */
    val eglContext: EglBase.Context get() = egl.eglBaseContext

    private val audioManager: AudioManager?
        get() = context.getSystemService(AudioManager::class.java)

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    /** شروع تماس. اگر [remoteUserId] نباشد یا سیگنالینگ پیکربندی نشده باشد، صادقانه شکست می‌خورد. */
    fun startCall(kind: CallKind, remoteLabel: String, remoteUserId: String? = null) {
        val active = _session.value
        if (active.isActive) return

        val effectiveKind = kind
        val notice: String? = null

        if (effectiveKind == CallKind.VIDEO && !hasCameraPermission()) {
            _session.value = CallSession(
                kind = CallKind.VIDEO,
                phase = CallPhase.Failed,
                remoteLabel = remoteLabel,
                notice = "برای تماس تصویری باید اجازه‌ی دوربین هم بدهی.",
            )
            return
        }

        if (!hasMicPermission()) {
            _session.value = CallSession(
                kind = effectiveKind,
                phase = CallPhase.Failed,
                remoteLabel = remoteLabel,
                notice = "برای تماس باید اجازه‌ی میکروفون بدهی.",
            )
            return
        }
        val engine = signaling
        if (engine == null || !engine.isConfigured || remoteUserId.isNullOrBlank()) {
            _session.value = CallSession(
                kind = effectiveKind,
                phase = CallPhase.Failed,
                remoteLabel = remoteLabel,
                notice = "تماس آنلاین فقط با سرور خارجی و پیوند فعال کار می‌کند. از «زنگ با تلفن معمولی» استفاده کن.",
            )
            return
        }

        val callId = UUID.randomUUID().toString()
        _session.value = CallSession(
            id = callId,
            kind = effectiveKind,
            phase = CallPhase.Dialing,
            remoteLabel = remoteLabel,
            remoteUserId = remoteUserId,
            startedAtMs = System.currentTimeMillis(),
            notice = notice,
        )
        // از همان لحظه‌ی شماره‌گیری: اگر صفحه خاموش شود، میکروفون نباید بمیرد.
        CallForegroundService.start(context, remoteLabel)

        scope.launch {
            val me = selfUserId().orEmpty()
            engine.openSession(callId, effectiveKind, me, remoteUserId, remoteLabel)
            engine.send(
                CallSignal(
                    id = UUID.randomUUID().toString(),
                    callId = callId,
                    fromUserId = me,
                    toUserId = remoteUserId,
                    type = SignalType.RING,
                    payload = "{}",
                    createdAtMs = System.currentTimeMillis(),
                ),
            )
            createPeerConnection(me, remoteUserId, isCaller = true)
        }
    }

    /** پاسخ به تماس ورودی (وقتی پدر/زهرا زنگ را دید). */
    fun acceptIncoming(callId: String, remoteLabel: String, remoteUserId: String) {
        if (!hasMicPermission() || signaling == null || !signaling.isConfigured) return
        _session.value = CallSession(
            id = callId,
            kind = CallKind.AUDIO,
            phase = CallPhase.Connecting,
            remoteLabel = remoteLabel,
            remoteUserId = remoteUserId,
            startedAtMs = System.currentTimeMillis(),
        )
        scope.launch {
            val me = selfUserId().orEmpty()
            createPeerConnection(me, remoteUserId, isCaller = false)
        }
    }

    private suspend fun createPeerConnection(me: String, remoteUserId: String, isCaller: Boolean) {
        val engine = signaling ?: return
        val session = _session.value
        ensureFactory()
        val factoryRef = factory ?: return

        val rtcConfig = PeerConnection.RTCConfiguration(
            iceServers.map { config ->
                PeerConnection.IceServer.builder(config.url)
                    .setUsername(config.username.orEmpty())
                    .setPassword(config.credential.orEmpty())
                    .createIceServer()
            },
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val connection = factoryRef.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED,
                    -> {
                        _session.value = _session.value.copy(
                            phase = CallPhase.InCall,
                            connectedAtMs = System.currentTimeMillis(),
                        )
                        applyAudioRouting()
                        CallForegroundService.start(context, _session.value.remoteLabel)
                    }

                    PeerConnection.IceConnectionState.DISCONNECTED ->
                        _session.value = _session.value.copy(notice = "اتصال قطع و وصل می‌شود…")

                    PeerConnection.IceConnectionState.FAILED -> {
                        _session.value = _session.value.copy(
                            phase = CallPhase.Failed,
                            notice = "اتصال برقرار نشد. احتمالاً به سرور TURN نیاز است.",
                        )
                        hangup()
                    }

                    PeerConnection.IceConnectionState.CLOSED ->
                        _session.value = _session.value.copy(phase = CallPhase.Ended)

                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit

            override fun onIceCandidate(candidate: IceCandidate?) {
                val c = candidate ?: return
                scope.launch {
                    engine.send(
                        CallSignal(
                            id = UUID.randomUUID().toString(),
                            callId = session.id,
                            fromUserId = me,
                            toUserId = remoteUserId,
                            type = SignalType.ICE,
                            payload = """{"sdpMid":"${c.sdpMid}","sdpMLineIndex":${c.sdpMLineIndex},"sdp":${quote(c.sdp)}}""",
                            createdAtMs = System.currentTimeMillis(),
                        ),
                    )
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {
                val remote = (receiver?.track() as? VideoTrack)
                    ?: streams?.firstOrNull()?.videoTracks?.firstOrNull()
                if (remote != null) _remoteVideo.value = remote
            }
        }) ?: return

        peer = connection

        // ترک صوتی محلی — بدون این، طرف مقابل هیچ صدایی نمی‌شنود.
        val audioSource = factoryRef.createAudioSource(MediaConstraints())
        localAudioTrack = factoryRef.createAudioTrack("audio-local", audioSource).also { track ->
            track.setEnabled(true)
            connection.addTrack(track, listOf("stream-local"))
        }

        val wantsVideo = session.kind == CallKind.VIDEO
        if (wantsVideo) setupVideo(connection, factoryRef)

        val offerConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", wantsVideo.toString()))
        }

        if (isCaller) {
            connection.createOffer(object : SdpAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    val description = sdp ?: return
                    connection.setLocalDescription(SdpAdapter(), description)
                    scope.launch {
                        engine.send(
                            CallSignal(
                                id = UUID.randomUUID().toString(),
                                callId = session.id,
                                fromUserId = me,
                                toUserId = remoteUserId,
                                type = SignalType.OFFER,
                                payload = """{"type":"${description.type.canonicalForm()}","sdp":${quote(description.description)}}""",
                                createdAtMs = System.currentTimeMillis(),
                            ),
                        )
                        _session.value = _session.value.copy(phase = CallPhase.Ringing)
                    }
                }

                override fun onCreateFailure(error: String?) {
                    _session.value = _session.value.copy(
                        phase = CallPhase.Failed,
                        notice = "ساخت پیشنهاد تماس ناموفق بود: ${error ?: "خطای ناشناخته"}",
                    )
                }
            }, offerConstraints)
        } else {
            _session.value = _session.value.copy(phase = CallPhase.Connecting)
        }

        startPolling(me, remoteUserId)
    }

    /** حلقه‌ی خواندن سیگنال‌ها تا پایان تماس. */
    private fun startPolling(me: String, remoteUserId: String) {
        val engine = signaling ?: return
        pollJob?.cancel()
        pollJob = scope.launch {
            var cursor = _session.value.startedAtMs - 1_000
            // یک سیگنال ممکن است هم از Realtime و هم از polling برسد؛ با شناسه‌ی سطر
            // تکراری‌ها را دور می‌ریزیم تا SDP دو بار setRemoteDescription نشود.
            val handled = Collections.synchronizedSet(mutableSetOf<String>())

            suspend fun deliver(signal: CallSignal) {
                if (!handled.add(signal.id)) return
                cursor = maxOf(cursor, signal.createdAtMs)
                handleSignal(signal, me, remoteUserId, engine)
            }

            // لایه‌ی ۱: Realtime (اگر بک‌اند تنظیم شده باشد) — بدون تأخیر.
            launch {
                engine.liveSignals(_session.value.id, cursor).collect { signal -> deliver(signal) }
            }

            // لایه‌ی ۲: polling به‌عنوان تور ایمنی. با Realtime فعال، فاصله کمتر می‌شود
            // تا خواندن بی‌مصرف سرور زیاد نشود؛ بدون Realtime همان ۱٫۵ ثانیه‌ی قبل است.
            val interval = if (engine.hasRealtime) REALTIME_BACKUP_POLL_MS else POLL_INTERVAL_MS
            while (_session.value.isActive) {
                val result = engine.poll(_session.value.id, cursor)
                if (result is AppResult.Ok) {
                    result.value.forEach { signal -> deliver(signal) }
                }
                delay(interval)
            }
        }
    }

    private suspend fun handleSignal(
        signal: CallSignal,
        me: String,
        remoteUserId: String,
        engine: CallSignaling,
    ) {
        val connection = peer ?: return
        when (signal.type) {
            SignalType.OFFER -> {
                val sdp = readSdp(signal.payload) ?: return
                connection.setRemoteDescription(object : SdpAdapter() {
                    override fun onSetSuccess() {
                        val constraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                            mandatory.add(
                                MediaConstraints.KeyValuePair(
                                    "OfferToReceiveVideo",
                                    (_session.value.kind == CallKind.VIDEO).toString(),
                                ),
                            )
                        }
                        connection.createAnswer(object : SdpAdapter() {
                            override fun onCreateSuccess(answer: SessionDescription?) {
                                val description = answer ?: return
                                connection.setLocalDescription(SdpAdapter(), description)
                                scope.launch {
                                    engine.send(
                                        signal.copy(
                                            id = UUID.randomUUID().toString(),
                                            fromUserId = me,
                                            toUserId = remoteUserId,
                                            type = SignalType.ANSWER,
                                            payload = """{"type":"${description.type.canonicalForm()}","sdp":${quote(description.description)}}""",
                                            createdAtMs = System.currentTimeMillis(),
                                        ),
                                    )
                                }
                            }
                        }, constraints)
                    }
                }, SessionDescription(SessionDescription.Type.OFFER, sdp))
                _session.value = _session.value.copy(phase = CallPhase.Connecting)
            }

            SignalType.ANSWER -> {
                val sdp = readSdp(signal.payload) ?: return
                connection.setRemoteDescription(SdpAdapter(), SessionDescription(SessionDescription.Type.ANSWER, sdp))
                _session.value = _session.value.copy(phase = CallPhase.Connecting)
            }

            SignalType.ICE -> {
                val candidate = parseIce(signal.payload) ?: return
                connection.addIceCandidate(candidate)
            }

            SignalType.END, SignalType.REJECT -> {
                _session.value = _session.value.copy(
                    phase = CallPhase.Ended,
                    notice = if (signal.type == SignalType.REJECT) "طرف مقابل تماس را رد کرد." else "تماس تمام شد.",
                )
                hangup()
            }

            SignalType.RING -> {
                _session.value = _session.value.copy(phase = CallPhase.Ringing)
            }
        }
    }

    /**
     * راه‌اندازی دوربین جلو، ترک ویدیوی محلی و افزودنش به PeerConnection.
     * اگر دوربینی در دسترس نبود، تماس بدون ویدیو ادامه پیدا می‌کند (صدا قطع نمی‌شود).
     */
    private fun setupVideo(connection: PeerConnection, factoryRef: PeerConnectionFactory) {
        runCatching {
            val enumerator = Camera2Enumerator(context)
            val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
                ?: return
            val capturer = enumerator.createCapturer(deviceName, null) ?: return
            val helper = SurfaceTextureHelper.create("CaptureThread", egl.eglBaseContext) ?: return
            val source = factoryRef.createVideoSource(capturer.isScreencast)
            capturer.initialize(helper, context.applicationContext, source.capturerObserver)
            capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)

            val track = factoryRef.createVideoTrack("video-local", source)
            track.setEnabled(true)
            connection.addTrack(track, listOf("stream-local"))

            videoCapturer = capturer
            surfaceTextureHelper = helper
            videoSource = source
            localVideoTrack = track
            _localVideo.value = track
        }.onFailure {
            _session.value = _session.value.copy(notice = "دوربین در دسترس نبود؛ تماس صوتی ادامه دارد.")
        }
    }

    /** تعویض دوربین جلو/عقب در میانه‌ی تماس. */
    fun switchCamera() {
        runCatching { videoCapturer?.switchCamera(null) }
    }

    /** @return true یعنی ویدیو خاموش شد. */
    fun toggleVideo(): Boolean {
        val track = localVideoTrack ?: return false
        val next = track.enabled()
        track.setEnabled(!next)
        _session.value = _session.value.copy(videoMuted = next)
        return next
    }

    fun toggleMic(): Boolean {
        val track = localAudioTrack ?: return _session.value.micMuted
        val next = !_session.value.micMuted
        track.setEnabled(!next)
        _session.value = _session.value.copy(micMuted = next)
        return next
    }

    fun toggleSpeaker(): Boolean {
        val next = !_session.value.speakerOn
        _session.value = _session.value.copy(speakerOn = next)
        applyAudioRouting()
        return next
    }

    private fun applyAudioRouting() {
        val manager = audioManager ?: return
        runCatching {
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            val wantSpeaker = _session.value.speakerOn
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wanted = if (wantSpeaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                val device = manager.availableCommunicationDevices.firstOrNull { it.type == wanted }
                if (device != null) manager.setCommunicationDevice(device)
            } else {
                @Suppress("DEPRECATION")
                manager.isSpeakerphoneOn = wantSpeaker
            }
        }
    }

    fun hangup() {
        pollJob?.cancel()
        pollJob = null
        val session = _session.value
        val engine = signaling
        if (engine != null && engine.isConfigured && session.id.isNotBlank()) {
            scope.launch {
                val me = selfUserId().orEmpty()
                engine.send(
                    CallSignal(
                        id = UUID.randomUUID().toString(),
                        callId = session.id,
                        fromUserId = me,
                        toUserId = session.remoteUserId.orEmpty(),
                        type = SignalType.END,
                        payload = "{}",
                        createdAtMs = System.currentTimeMillis(),
                    ),
                )
                engine.closeSession(session.id, if (session.phase == CallPhase.InCall) "completed" else "missed")
            }
        }
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        runCatching { surfaceTextureHelper?.dispose() }
        runCatching { localVideoTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        videoCapturer = null
        surfaceTextureHelper = null
        localVideoTrack = null
        videoSource = null
        _localVideo.value = null
        _remoteVideo.value = null
        runCatching { localAudioTrack?.dispose() }
        localAudioTrack = null
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        runCatching { audioManager?.mode = AudioManager.MODE_NORMAL }
        CallForegroundService.stop(context)
        _session.value = session.copy(phase = CallPhase.Ended)
    }

    fun reset() {
        _session.value = CallSession()
    }

    /** زنگ با شماره‌ی واقعی — همیشه در دسترس است، حتی بدون اینترنت. */
    fun dialTel(number: String) {
        if (number.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** آزادسازی منابع وقتی صفحه‌ی تماس بسته می‌شود. */
    fun release() {
        hangup()
        runCatching { factory?.dispose() }
        factory = null
        runCatching { egl.release() }
    }

    private fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setAudioDeviceModule(
                JavaAudioDeviceModule.builder(context.applicationContext).createAudioDeviceModule(),
            )
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun quote(value: String?): String =
        "\"" + (value.orEmpty().replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r")) + "\""

    private fun readSdp(payload: String): String? = runCatching {
        org.json.JSONObject(payload).optString("sdp").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun parseIce(payload: String): IceCandidate? = runCatching {
        val json = org.json.JSONObject(payload)
        IceCandidate(
            json.optString("sdpMid"),
            json.optInt("sdpMLineIndex"),
            json.optString("sdp"),
        )
    }.getOrNull()

    companion object {
        private const val POLL_INTERVAL_MS = 1_500L

        /** وقتی Realtime فعال است، polling فقط تور ایمنی است؛ پس کندتر می‌شود. */
        private const val REALTIME_BACKUP_POLL_MS = 5_000L
        private const val VIDEO_WIDTH = 640
        private const val VIDEO_HEIGHT = 480
        private const val VIDEO_FPS = 24

        val DEFAULT_ICE_SERVERS = listOf(
            IceServerConfig("stun:stun.cloudflare.com:3478"),
            IceServerConfig("stun:stun.l.google.com:19302"),
        )
    }
}

/** پیاده‌سازی خالی [org.webrtc.SdpObserver] تا فقط بخش‌های لازم override شوند. */
open class SdpAdapter : org.webrtc.SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String?) = Unit
    override fun onSetFailure(error: String?) = Unit
}
