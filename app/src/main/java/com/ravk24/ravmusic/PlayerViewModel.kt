package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerBridge
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.planQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Activity-scoped bridge between the UI and the playback session. Connects on creation, releases
 * the controller when the Activity finishes, and keeps the position fresh while playing (the
 * ticker only runs while `isPlaying`, so it costs nothing when paused). Now Playing runs its own
 * faster ticker through [refreshPosition] while it is visible.
 */
class PlayerViewModel(
    private val bridge: PlayerBridge,
    private val tickMs: Long = 500L,
    private val random: Random = Random.Default,
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

    /** Tap-to-play: [songs] become the queue, starting at [index]; shuffle is off unless asked for. */
    fun playSongs(songs: List<Song>, index: Int, origin: String, shuffle: Boolean = false) {
        planQueue(songs, index, origin)?.let { bridge.play(it, shuffle) }
    }

    /** Spec F5: Shuffle Play enables shuffle and starts from a random track. */
    fun shufflePlay(songs: List<Song>, origin: String) {
        if (songs.isEmpty()) return
        playSongs(songs, random.nextInt(songs.size), origin, shuffle = true)
    }

    fun togglePlayPause() = bridge.togglePlayPause()

    /** Mini player swiped away. */
    fun stopAndClear() = bridge.stopAndClear()

    fun refreshPosition() = bridge.refreshPosition()

    fun seekTo(positionMs: Long) = bridge.seekTo(positionMs)

    fun next() = bridge.next()

    fun previous() = bridge.previous()

    fun toggleShuffle() = bridge.setShuffle(!state.value.shuffleEnabled)

    fun cycleRepeat() = bridge.setRepeat(state.value.repeatMode.next())

    fun jumpTo(queuePosition: Int) = bridge.jumpToQueuePosition(queuePosition)

    fun moveInQueue(from: Int, to: Int) = bridge.moveInQueue(from, to)

    /** The shell's view of this ViewModel. */
    fun actions(): PlayerActions = PlayerActions(
        onPlayPause = ::togglePlayPause,
        onDismiss = ::stopAndClear,
        onPlaySongs = { songs, index, origin -> playSongs(songs, index, origin) },
        onShufflePlay = ::shufflePlay,
        onSeek = ::seekTo,
        onNext = ::next,
        onPrevious = ::previous,
        onToggleShuffle = ::toggleShuffle,
        onCycleRepeat = ::cycleRepeat,
        onJumpTo = ::jumpTo,
        onMoveInQueue = ::moveInQueue,
        onRefreshPosition = ::refreshPosition,
    )

    override fun onCleared() {
        bridge.release()
    }
}
