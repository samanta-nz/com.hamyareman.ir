package com.hamyareman.ir.platform.feature.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * پلیر خواب — پس‌زمینه مجاز. جدا از [PlaybackService] تدریس.
 * اعلان: پلی / استوپ / بستن. بدون سرعت و بدون نوار سیک.
 */
@UnstableApi
class SleepPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val player = object : androidx.media3.common.ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands =
                Player.Commands.Builder()
                    .addAll(super.getAvailableCommands())
                    .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .remove(Player.COMMAND_SEEK_TO_NEXT)
                    .remove(Player.COMMAND_SET_SPEED_AND_PITCH)
                    .build()
        }
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openSleepIntent())
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    val base = super.onConnect(session, controller)
                    if (!base.isAccepted) return base
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(
                            base.availableSessionCommands.buildUpon().add(STOP_COMMAND).build(),
                        )
                        .setAvailablePlayerCommands(base.availablePlayerCommands)
                        .setCustomLayout(listOf(stopButton()))
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle,
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == ACTION_STOP) {
                        runCatching { session.player.pause() }
                        runCatching { session.player.stop() }
                        runCatching { session.player.clearMediaItems() }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .build()
        mediaSession?.setCustomLayout(listOf(stopButton()))
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_audiobook)
            },
        )
    }

    private fun openSleepIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            77,
            Intent().apply {
                setClassName(packageName, "com.hamyareman.ir.MainActivity")
                action = OPEN_ACTION
                putExtra(OPEN_EXTRA, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let {
            it.player.release()
            it.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val OPEN_ACTION = "com.hamyareman.ir.OPEN_SLEEP"
        const val OPEN_EXTRA = "open_sleep"
        const val ACTION_STOP = "com.hamyareman.ir.STOP_SLEEP"
        val STOP_COMMAND = SessionCommand(ACTION_STOP, Bundle.EMPTY)

        fun stopButton(): CommandButton =
            CommandButton.Builder(CommandButton.ICON_STOP)
                .setDisplayName("بستن")
                .setSessionCommand(STOP_COMMAND)
                .setEnabled(true)
                .build()
    }
}
