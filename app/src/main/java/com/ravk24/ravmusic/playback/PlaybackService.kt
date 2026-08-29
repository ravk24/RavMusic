package com.ravk24.ravmusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ravk24.ravmusic.MainActivity

/**
 * The one place audio is played (spec F4). A Media3 `MediaSessionService` owning an ExoPlayer:
 * the session gives us the media notification, lock-screen and headset controls for free; the
 * player handles audio focus and pauses when headphones are unplugged.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

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
        player.addListener(MissingFileSkipper(player))

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
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
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /**
     * Spec F1 edge case: a song whose file is gone is skipped, not fatal. A source error leaves
     * the player idle on the bad item; moving on and re-preparing continues the queue. On the
     * last item the queue simply ends.
     */
    private class MissingFileSkipper(private val player: Player) : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Playback error on ${player.currentMediaItem?.mediaId}: ${error.errorCodeName}")
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
