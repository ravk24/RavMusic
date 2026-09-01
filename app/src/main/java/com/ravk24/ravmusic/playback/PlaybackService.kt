package com.ravk24.ravmusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ravk24.ravmusic.MainActivity
import com.ravk24.ravmusic.RavMusicApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The one place audio is played (spec F4). A Media3 `MediaSessionService` owning an ExoPlayer:
 * the session gives us the media notification, lock-screen and headset controls for free; the
 * player handles audio focus and pauses when headphones are unplugged. Also hosts the sleep
 * timer (spec F6), driven by custom session commands and reported through session extras.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimer: SleepTimerEngine? = null
    private var effects: AudioEffects? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(MissingFileSkipper(player) { session })

        // The equalizer (design D1/D2 of `add-equalizer`): a session id of our own lets the
        // effects attach before the first song, and DataStore drives them from then on. If the
        // player ever renews the id the effects silently detach, so recreate and re-apply.
        val equalizerSettings = (application as RavMusicApp).container.equalizerSettingsRepository
        player.audioSessionId = (getSystemService(AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        effects = AudioEffects.create(player.audioSessionId)
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                effects?.release()
                effects = AudioEffects.create(audioSessionId)
                scope.launch { effects?.apply(equalizerSettings.settings.first()) }
            }
        })
        scope.launch {
            equalizerSettings.settings.collect { snapshot -> effects?.apply(snapshot) }
        }

        val engine = SleepTimerEngine(
            actions = object : SleepTimerActions {
                override var volume: Float
                    get() = player.volume
                    set(value) { player.volume = value }
                override fun pause() = player.pause()
            },
            scope = scope,
            clock = { SystemClock.elapsedRealtime() },
        )
        sleepTimer = engine
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) engine.onTrackEnded()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // The queue ended on its own: an end-of-track timer has nothing left to do.
                if (playbackState == Player.STATE_ENDED && engine.state.value is SleepTimerState.EndOfTrack) engine.cancel()
            }
        })

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val built = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback(engine) { effects?.capabilities ?: EqCapabilities() })
            .build()
        session = built

        scope.launch {
            engine.state.collect { state -> built.sessionExtras = SleepTimerCommands.toExtras(state) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiped from recents: keep going if playing, otherwise there is nothing to keep alive. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepTimer?.cancel()
        scope.cancel()
        effects?.release()
        effects = null
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /** Advertises the custom commands (sleep timer, equalizer capabilities) and routes them. */
    private class SessionCallback(
        private val engine: SleepTimerEngine,
        private val capabilities: () -> EqCapabilities,
    ) : MediaSession.Callback {

        private val commands = listOf(
            SleepTimerCommands.SET,
            SleepTimerCommands.EXTEND,
            SleepTimerCommands.CANCEL,
            EqualizerCommands.GET_CAPABILITIES,
        ).map { SessionCommand(it, Bundle.EMPTY) }

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): ConnectionResult {
            val available = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .apply { commands.forEach { add(it) } }
                .build()
            return ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(available)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                SleepTimerCommands.SET -> {
                    if (args.getBoolean(SleepTimerCommands.ARG_END_OF_TRACK, false)) {
                        engine.endOfTrack()
                    } else {
                        engine.set(args.getLong(SleepTimerCommands.ARG_DURATION_MS, 0L))
                    }
                }
                SleepTimerCommands.EXTEND -> engine.extend(args.getLong(SleepTimerCommands.ARG_EXTRA_MS, SLEEP_EXTEND_MS))
                SleepTimerCommands.CANCEL -> engine.cancel()
                EqualizerCommands.GET_CAPABILITIES -> return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS, EqualizerCommands.toBundle(capabilities())),
                )
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /**
     * Spec F1 edge case: a song whose file is gone is skipped, not fatal. A source error leaves
     * the player idle on the bad item; moving on and re-preparing continues the queue. On the
     * last item the queue simply ends.
     */
    private class MissingFileSkipper(
        private val player: Player,
        private val session: () -> MediaSession?,
    ) : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val item = player.currentMediaItem
            Log.w(TAG, "Playback error on ${item?.mediaId}: ${error.errorCodeName}")
            // Tell whoever is listening before moving on (design D5 of `polish`).
            val title = item?.mediaMetadata?.title?.toString().orEmpty().ifBlank { "this song" }
            session()?.broadcastCustomCommand(
                SessionCommand(PlaybackEvents.SKIPPED_MISSING, Bundle.EMPTY),
                Bundle().apply { putString(PlaybackEvents.ARG_TITLE, title) },
            )
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            } else {
                player.stop()
                player.clearMediaItems()
            }
        }
    }

    private companion object {
        const val TAG = "PlaybackService"
    }
}
