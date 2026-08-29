package com.ravk24.ravmusic

import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerBridge
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueuePlan
import com.ravk24.ravmusic.playback.RepeatMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcher = MainDispatcherRule(dispatcher)

    private class FakeBridge : PlayerBridge {
        val flow = MutableStateFlow(PlayerState())
        override val state: StateFlow<PlayerState> = flow
        var connects = 0
        var refreshes = 0
        var toggles = 0
        var stops = 0
        var lastPlan: QueuePlan? = null
        var lastShuffle: Boolean? = null
        override fun connect() { connects++ }
        override fun play(plan: QueuePlan, shuffle: Boolean) { lastPlan = plan; lastShuffle = shuffle }
        override fun togglePlayPause() { toggles++ }
        override fun stopAndClear() { stops++ }
        override fun refreshPosition() { refreshes++ }
        var seeks = mutableListOf<Long>()
        var nexts = 0
        var previouses = 0
        var lastShuffleSet: Boolean? = null
        var lastRepeatSet: RepeatMode? = null
        var jumps = mutableListOf<Int>()
        var moves = mutableListOf<Pair<Int, Int>>()
        override fun seekTo(positionMs: Long) { seeks += positionMs }
        override fun next() { nexts++ }
        override fun previous() { previouses++ }
        override fun setShuffle(enabled: Boolean) { lastShuffleSet = enabled; flow.value = flow.value.copy(shuffleEnabled = enabled) }
        override fun setRepeat(mode: RepeatMode) { lastRepeatSet = mode; flow.value = flow.value.copy(repeatMode = mode) }
        override fun jumpToQueuePosition(position: Int) { jumps += position }
        override fun moveInQueue(from: Int, to: Int) { moves += from to to }
        override fun release() {}
    }

    private fun song(id: Long) = Song(id, "content://media/$id", "S$id", null, 100_000L, "f", "Folder")
    private val playing = PlayerState(NowPlaying(1L, "S1", null, "Folder"), isPlaying = true, durationMs = 100_000L)

    @Test
    fun `connects on creation`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        PlayerViewModel(bridge)
        assertEquals(1, bridge.connects)
    }

    @Test
    fun `ticker runs only while playing`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        PlayerViewModel(bridge, tickMs = 100L)
        advanceTimeBy(1_000L)
        assertEquals(0, bridge.refreshes)

        bridge.flow.value = playing
        advanceTimeBy(550L)
        assertEquals(5, bridge.refreshes)

        bridge.flow.value = playing.copy(isPlaying = false)
        advanceTimeBy(1_000L)
        assertEquals(5, bridge.refreshes)
    }

    @Test
    fun `playSongs forwards a plan and ignores empty folders`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        val vm = PlayerViewModel(bridge)
        vm.playSongs(listOf(song(1), song(2)), index = 1, origin = "Rock")
        assertEquals(listOf(1L, 2L), bridge.lastPlan!!.songs.map { it.id })
        assertEquals(1, bridge.lastPlan!!.startIndex)
        assertEquals("Rock", bridge.lastPlan!!.origin)
        assertEquals(false, bridge.lastShuffle)

        bridge.lastPlan = null
        vm.playSongs(emptyList(), 0, "Empty")
        assertNull(bridge.lastPlan)
    }

    @Test
    fun `shufflePlay enables shuffle and starts within range`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        val songs = listOf(song(1), song(2), song(3))
        val vm = PlayerViewModel(bridge, random = kotlin.random.Random(7))
        repeat(10) {
            vm.shufflePlay(songs, "Late night")
            assertEquals(true, bridge.lastShuffle)
            assertTrue(bridge.lastPlan!!.startIndex in 0..2)
            assertEquals("Late night", bridge.lastPlan!!.origin)
        }
        bridge.lastPlan = null
        vm.shufflePlay(emptyList(), "x")
        assertNull(bridge.lastPlan)
    }

    @Test
    fun `transport shuffle repeat and queue commands reach the bridge`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        val vm = PlayerViewModel(bridge)
        val a = vm.actions()
        a.onSeek(12_345L)
        a.onNext(); a.onPrevious()
        a.onToggleShuffle(); a.onToggleShuffle()
        a.onCycleRepeat(); a.onCycleRepeat(); a.onCycleRepeat()
        a.onJumpTo(3)
        a.onMoveInQueue(4, 1)
        a.onRefreshPosition()
        assertEquals(listOf(12_345L), bridge.seeks)
        assertEquals(1, bridge.nexts)
        assertEquals(1, bridge.previouses)
        assertEquals(false, bridge.lastShuffleSet)
        assertEquals(RepeatMode.OFF, bridge.lastRepeatSet)
        assertEquals(listOf(3), bridge.jumps)
        assertEquals(listOf(4 to 1), bridge.moves)
        assertEquals(1, bridge.refreshes)
    }

    @Test
    fun `toggle and stop are forwarded`() = runTest(dispatcher) {
        val bridge = FakeBridge()
        val vm = PlayerViewModel(bridge)
        vm.togglePlayPause()
        vm.stopAndClear()
        assertEquals(1, bridge.toggles)
        assertEquals(1, bridge.stops)
    }
}
