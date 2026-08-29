package com.ravk24.ravmusic.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-process client of [PlaybackService]. Owns one `MediaController`, mirrors the player into a
 * [PlayerState] value, and forwards commands. Connecting is asynchronous; commands issued before
 * the controller is ready are queued and replayed in order once it connects.
 *
 * Must be used from the main thread (Media3 controllers are looper-bound).
 */
class PlayerConnection(private val context: Context) : PlayerBridge {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Commands issued before the controller connected, replayed in order once it does. */
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    /** Sleep-timer state as last published by the service through session extras. */
    private var timerState: SleepTimerState = SleepTimerState.Off

    /** The last skip notice broadcast by the service; `seq` makes repeats distinguishable. */
    private var skipped: SkipNotice? = null

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            timerState = SleepTimerCommands.fromExtras(extras)
            publish(controller)
        }

        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (command.customAction == PlaybackEvents.SKIPPED_MISSING) {
                val title = args.getString(PlaybackEvents.ARG_TITLE).orEmpty().ifBlank { "this song" }
                skipped = SkipNotice(title, (skipped?.seq ?: 0) + 1)
                publish(controller)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).setListener(controllerListener).buildAsync()
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
                timerState = SleepTimerCommands.fromExtras(c.sessionExtras)
                publish(c)
                while (pending.isNotEmpty()) pending.removeFirst().invoke(c)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play(plan: QueuePlan, shuffle: Boolean) = withController { c ->
        c.shuffleModeEnabled = shuffle
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

    /** Test hook: the controller's shuffle mode, or null while disconnected. */
    fun shuffleModeEnabledForTest(): Boolean? = controller?.shuffleModeEnabled

    /** Test hook: the session's playback volume, or null while disconnected. */
    fun volumeForTest(): Float? = controller?.volume

    override fun refreshPosition() {
        controller?.let { c ->
            val current = _state.value
            _state.value = current.copy(positionMs = c.currentPosition.coerceAtLeast(0L), durationMs = durationOf(c))
        }
    }

    override fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs.coerceAtLeast(0L))
        publish(c)
    }

    override fun next() = withController { c -> c.seekToNext() }

    override fun previous() = withController { c -> c.seekToPrevious() }

    override fun setShuffle(enabled: Boolean) = withController { c -> c.shuffleModeEnabled = enabled }

    override fun setRepeat(mode: RepeatMode) = withController { c ->
        c.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    override fun jumpToQueuePosition(position: Int) = withController { c ->
        val entry = _state.value.queue.getOrNull(position) ?: return@withController
        c.seekToDefaultPosition(entry.mediaIndex)
        c.play()
    }

    override fun moveInQueue(from: Int, to: Int) = withController { c ->
        val queue = _state.value.queue
        if (from !in queue.indices || to !in queue.indices || from == to) return@withController
        if (!c.shuffleModeEnabled) {
            // Play order == item order: queue positions are media indices.
            c.moveMediaItem(queue[from].mediaIndex, queue[to].mediaIndex)
        } else {
            // The controller cannot set a custom shuffle order: freeze the shown order as the
            // queue, turn shuffle off, then apply the move (design D4).
            val items = queue.map { c.getMediaItemAt(it.mediaIndex) }
            val currentPos = _state.value.queueIndex.coerceAtLeast(0)
            val positionMs = c.currentPosition
            val wasPlaying = c.playWhenReady
            c.shuffleModeEnabled = false
            c.setMediaItems(items, currentPos, positionMs)
            c.prepare()
            c.moveMediaItem(from, to)
            c.playWhenReady = wasPlaying
        }
        publish(c)
    }

    override fun setSleepTimer(durationMs: Long) = withController { c ->
        c.sendCustomCommand(
            SessionCommand(SleepTimerCommands.SET, Bundle.EMPTY),
            Bundle().apply { putLong(SleepTimerCommands.ARG_DURATION_MS, durationMs) },
        )
    }

    override fun setSleepTimerEndOfTrack() = withController { c ->
        c.sendCustomCommand(
            SessionCommand(SleepTimerCommands.SET, Bundle.EMPTY),
            Bundle().apply { putBoolean(SleepTimerCommands.ARG_END_OF_TRACK, true) },
        )
    }

    override fun extendSleepTimer(extraMs: Long) = withController { c ->
        c.sendCustomCommand(
            SessionCommand(SleepTimerCommands.EXTEND, Bundle.EMPTY),
            Bundle().apply { putLong(SleepTimerCommands.ARG_EXTRA_MS, extraMs) },
        )
    }

    override fun cancelSleepTimer() = withController { c ->
        c.sendCustomCommand(SessionCommand(SleepTimerCommands.CANCEL, Bundle.EMPTY), Bundle.EMPTY)
    }

    override fun release() {
        controller?.removeListener(listener)
        controller = null
        future?.let { MediaController.releaseFuture(it) }
        future = null
        pending.clear()
        timerState = SleepTimerState.Off
        skipped = null
        _state.value = PlayerState()
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending.addLast(block)
            connect()
        }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        val timeline = player.currentTimeline
        val shuffle = player.shuffleModeEnabled
        val order = playOrder(
            count = timeline.windowCount,
            currentIndex = player.currentMediaItemIndex,
            first = timeline.getFirstWindowIndex(shuffle).takeIf { it != C.INDEX_UNSET },
        ) { i -> timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle).takeIf { it != C.INDEX_UNSET } }
        val queue = order.mediaIndices.map { index ->
            val entry = nowPlayingFrom(player.getMediaItemAt(index))
            QueueEntry(songId = entry.songId, title = entry.title, artist = entry.artist, mediaIndex = index)
        }
        _state.value = PlayerState(
            nowPlaying = item?.let(::nowPlayingFrom),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = durationOf(player),
            shuffleEnabled = shuffle,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            queue = queue,
            queueIndex = order.currentPosition,
            sleepTimer = timerState,
            skipped = skipped,
        )
    }

    private fun durationOf(player: Player): Long =
        player.duration.let { if (it == C.TIME_UNSET || it < 0L) 0L else it }

    private companion object {
        const val TAG = "PlayerConnection"
    }
}
