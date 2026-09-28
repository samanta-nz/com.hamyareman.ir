package com.hamyareman.ir.platform.feature.hearttoheart

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** ضبط ویس با MediaRecorder (AAC در ظرف MPEG-4). */
class VoiceRecorder(private val context: Context) {

    data class Recording(val path: String, val durationMs: Long, val fileName: String)

    private var recorder: MediaRecorder? = null
    private var outputPath: String? = null
    private var startedAtMs = 0L

    val isRecording: Boolean get() = recorder != null

    /** @return مسیر فایل در حال ضبط، یا پیام خطا. */
    fun start(): Result<String> = runCatching {
        if (recorder != null) error("already-recording")
        val dir = File(context.cacheDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        instance.setAudioSource(MediaRecorder.AudioSource.MIC)
        instance.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        instance.setOutputFile(file.absolutePath)
        instance.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        instance.setAudioEncodingBitRate(64_000)
        instance.setAudioSamplingRate(44_100)
        instance.prepare()
        instance.start()
        recorder = instance
        outputPath = file.absolutePath
        startedAtMs = System.currentTimeMillis()
        file.absolutePath
    }

    fun stop(): Recording? {
        val instance = recorder ?: return null
        val path = outputPath ?: return null
        val duration = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        runCatching { instance.stop() }
        runCatching { instance.release() }
        recorder = null
        outputPath = null
        startedAtMs = 0L
        return Recording(path, duration, File(path).name)
    }

    /** سطح صدا برای نمایش یک نوار ساده (۰ تا ~۳۲۰۰۰). */
    fun amplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun cancel() {
        val instance = recorder ?: return
        val path = outputPath
        runCatching { instance.stop() }
        runCatching { instance.release() }
        recorder = null
        outputPath = null
        path?.let { runCatching { File(it).delete() } }
    }
}
