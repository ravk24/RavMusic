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

    /** Re-reads the position; called by the UI-side ticker while playing. */
    fun refreshPosition()

    /** Releases the controller. [connect] may be called again afterwards. */
    fun release()
}
