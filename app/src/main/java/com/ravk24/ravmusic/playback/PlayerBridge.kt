package com.ravk24.ravmusic.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * What the UI needs from the player, as an interface so ViewModels are testable with a fake.
 * Implemented by [PlayerConnection] over a Media3 `MediaController`.
 */
interface PlayerBridge {
    val state: StateFlow<PlayerState>

    /** Connects to the playback service if not already connected. Idempotent. */
    fun connect()

    /** Replaces the queue with [plan] and starts playing; [shuffle] sets the session's shuffle mode. */
    fun play(plan: QueuePlan, shuffle: Boolean = false)

    fun togglePlayPause()

    /** Stops playback and clears the queue; the mini player disappears. */
    fun stopAndClear()

    /** Re-reads the position; called by the UI-side tickers while playing. */
    fun refreshPosition()

    fun seekTo(positionMs: Long)

    fun next()

    /** Restart the current song, or go to the previous one when near its start (Media3 default). */
    fun previous()

    fun setShuffle(enabled: Boolean)

    fun setRepeat(mode: RepeatMode)

    /** Starts the song at [position] in the play-order queue. */
    fun jumpToQueuePosition(position: Int)

    /**
     * Moves the song at play-order [from] to [to]. With shuffle on, the shown order becomes the
     * fixed queue and shuffle turns off (design D4).
     */
    fun moveInQueue(from: Int, to: Int)

    /** Releases the controller. [connect] may be called again afterwards. */
    fun release()
}
