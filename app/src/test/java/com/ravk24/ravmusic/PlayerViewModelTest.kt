package com.ravk24.ravmusic

import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerBridge
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueuePlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        override fun connect() { connects++ }
        override fun play(plan: QueuePlan) { lastPlan = plan }
        override fun togglePlayPause() { toggles++ }
        override fun stopAndClear() { stops++ }
        override fun refreshPosition() { refreshes++ }
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

        bridge.lastPlan = null
        vm.playSongs(emptyList(), 0, "Empty")
        assertNull(bridge.lastPlan)
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
