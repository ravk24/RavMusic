package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.playback.PlayerBridge
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.planQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Activity-scoped bridge between the UI and the playback session. Connects on creation, releases
 * the controller when the Activity finishes, and keeps the position fresh while playing (the
 * ticker only runs while `isPlaying`, so it costs nothing when paused).
 */
class PlayerViewModel(
    private val bridge: PlayerBridge,
    private val tickMs: Long = 500L,
) : ViewModel() {

    val state: StateFlow<PlayerState> = bridge.state

    init {
        bridge.connect()
        viewModelScope.launch {
            state.map { it.isPlaying }.distinctUntilChanged().collectLatest { playing ->
                while (playing) {
                    delay(tickMs)
                    bridge.refreshPosition()
                }
            }
        }
    }

    /** Tap-to-play from a folder: the folder becomes the queue, starting at [index]. */
    fun playSongs(songs: List<Song>, index: Int, origin: String) {
        planQueue(songs, index, origin)?.let(bridge::play)
    }

    fun togglePlayPause() = bridge.togglePlayPause()

    /** Mini player swiped away. */
    fun stopAndClear() = bridge.stopAndClear()

    override fun onCleared() {
        bridge.release()
    }
}
