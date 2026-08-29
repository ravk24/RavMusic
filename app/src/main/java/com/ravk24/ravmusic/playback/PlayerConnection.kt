package com.ravk24.ravmusic.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-process client of [PlaybackService]. Owns one `MediaController`, mirrors the player into a
 * [PlayerState] value, and forwards commands. Connecting is asynchronous; a single command issued
 * before the controller is ready is kept and replayed once it connects.
 *
 * Must be used from the main thread (Media3 controllers are looper-bound).
 */
class PlayerConnection(private val context: Context) : PlayerBridge {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pending: ((MediaController) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    override fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                val c = try {
                    f.get()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaController connection failed", e)
                    future = null
                    return@addListener
                }
                controller = c
                c.addListener(listener)
                publish(c)
                pending?.invoke(c)
                pending = null
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play(plan: QueuePlan) = withController { c ->
        c.setMediaItems(plan.toMediaItems(), plan.startIndex, C.TIME_UNSET)
        c.prepare()
        c.play()
    }

    override fun togglePlayPause() = withController { c ->
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_ENDED -> {
                c.seekToDefaultPosition(0)
                c.play()
            }
            c.playbackState == Player.STATE_IDLE -> {
                c.prepare()
                c.play()
            }
            else -> c.play()
        }
    }

    override fun stopAndClear() = withController { c ->
        c.stop()
        c.clearMediaItems()
        publish(c)
    }

    override fun refreshPosition() {
        controller?.let { c ->
            val current = _state.value
            _state.value = current.copy(positionMs = c.currentPosition.coerceAtLeast(0L), durationMs = durationOf(c))
        }
    }

    override fun release() {
        controller?.removeListener(listener)
        controller = null
        future?.let { MediaController.releaseFuture(it) }
        future = null
        pending = null
        _state.value = PlayerState()
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending = block
            connect()
        }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerState(
            nowPlaying = item?.let(::nowPlayingFrom),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = durationOf(player),
        )
    }

    private fun durationOf(player: Player): Long =
        player.duration.let { if (it == C.TIME_UNSET || it < 0L) 0L else it }

    private companion object {
        const val TAG = "PlayerConnection"
    }
}
