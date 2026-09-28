package com.hamyareman.ir.platform.feature.calls

enum class CallKind { AUDIO, VIDEO }

enum class CallPhase { Idle, Dialing, Ringing, Connecting, InCall, Ended, Failed }

/**
 * وضعیت یک تماس.
 *
 * [remoteUserId] شناسه‌ی کاربر Appwrite طرف مقابل است (نه نام نمایشی)؛ سیگنالینگ
 * روی همین شناسه سطر می‌سازد تا دو دستگاه همدیگر را پیدا کنند.
 */
data class CallSession(
    val id: String = "",
    val kind: CallKind = CallKind.AUDIO,
    val phase: CallPhase = CallPhase.Idle,
    val remoteLabel: String = "بابا",
    val remoteUserId: String? = null,
    val startedAtMs: Long = 0L,
    val connectedAtMs: Long? = null,
    val micMuted: Boolean = false,
    val videoMuted: Boolean = false,
    val speakerOn: Boolean = false,
    val notice: String? = null,
) {
    val isActive: Boolean get() =
        phase == CallPhase.Dialing || phase == CallPhase.Ringing ||
            phase == CallPhase.Connecting || phase == CallPhase.InCall
}

/** یک پیام سیگنالینگ (offer/answer/ice/end) که در جدول `call_signals` می‌نشیند. */
data class CallSignal(
    val id: String,
    val callId: String,
    val fromUserId: String,
    val toUserId: String,
    val type: SignalType,
    val payload: String,
    val createdAtMs: Long,
)

enum class SignalType { OFFER, ANSWER, ICE, END, RING, REJECT }
