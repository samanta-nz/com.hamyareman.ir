package com.hamyareman.ir.platform.feature.hearttoheart

import android.media.AudioAttributes
import android.media.MediaPlayer

/** پخش ویس — هم از فایل محلی و هم از لینک Storage. */
class VoicePlayer {

    private var player: MediaPlayer? = null
    private var currentKey: String? = null

    val playingKey: String? get() = currentKey

    /** @return true یعنی الان دارد پخش می‌کند، false یعنی متوقف شد. */
    fun play(key: String, source: String, onFinished: () -> Unit = {}): Boolean = runCatching {
        if (currentKey == key && player?.isPlaying == true) {
            stop()
            return@runCatching false
        }
        stop()
        val instance = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setDataSource(source)
            setOnCompletionListener {
                stop()
                onFinished()
            }
            prepare()
            start()
        }
        player = instance
        currentKey = key
        true
    }.getOrDefault(false)

    fun stop() {
        runCatching { player?.takeIf { it.isPlaying }?.stop() }
        runCatching { player?.release() }
        player = null
        currentKey = null
    }
}
