package com.ravk24.ravmusic.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerEngineTest {

    private class FakeActions : SleepTimerActions {
        override var volume: Float = 1f
        val volumes = mutableListOf<Float>()
        var pauses = 0
        override fun pause() { pauses++ }
    }

    private fun kotlinx.coroutines.test.TestScope.engine(actions: FakeActions) =
        SleepTimerEngine(actions, backgroundScope, clock = { testScheduler.currentTime }, fadeMs = 10_000L, stepMs = 1_000L)

    @Test
    fun `countdown fades over the last ten seconds then pauses and restores volume`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.set(30_000L)
        assertEquals(SleepTimerState.Countdown(30_000L), engine.state.value)

        advanceTimeBy(19_999L); runCurrent()
        assertEquals(1f, actions.volume, 0.001f)
        assertEquals(0, actions.pauses)

        advanceTimeBy(5_001L); runCurrent()          // t = 25 s: halfway through the fade
        assertTrue("volume should be fading, was ${actions.volume}", actions.volume in 0.3f..0.7f)
        assertEquals(0, actions.pauses)

        advanceTimeBy(5_000L); runCurrent()          // t = 30 s
        assertEquals(1, actions.pauses)
        assertEquals(1f, actions.volume, 0.001f)
        assertEquals(SleepTimerState.Off, engine.state.value)
    }

    @Test
    fun `short timer starts fading immediately`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.set(4_000L)
        advanceTimeBy(2_001L); runCurrent()
        assertTrue(actions.volume < 0.9f)
        advanceTimeBy(2_000L); runCurrent()
        assertEquals(1, actions.pauses)
        assertEquals(1f, actions.volume, 0.001f)
    }

    @Test
    fun `extend moves the end and restores a mid-fade volume`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.set(15_000L)
        advanceTimeBy(10_001L); runCurrent()          // fading
        assertTrue(actions.volume < 1f)
        engine.extend(60_000L)
        assertEquals(1f, actions.volume, 0.001f)
        assertEquals(SleepTimerState.Countdown(75_000L), engine.state.value)
        advanceTimeBy(60_000L); runCurrent()          // t = 70 s: still 5 s before the end, fading
        assertEquals(0, actions.pauses)
        advanceTimeBy(5_000L); runCurrent()
        assertEquals(1, actions.pauses)
    }

    @Test
    fun `cancel restores the volume and stops the countdown`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.set(12_000L)
        advanceTimeBy(5_001L); runCurrent()
        assertTrue(actions.volume < 1f)
        engine.cancel()
        assertEquals(1f, actions.volume, 0.001f)
        assertEquals(SleepTimerState.Off, engine.state.value)
        advanceTimeBy(20_000L); runCurrent()
        assertEquals(0, actions.pauses)
    }

    @Test
    fun `replacing a timer cancels the previous one`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.set(5_000L)
        engine.set(60_000L)
        advanceTimeBy(10_000L); runCurrent()
        assertEquals(0, actions.pauses)
        assertEquals(SleepTimerState.Countdown(60_000L), engine.state.value)
    }

    @Test
    fun `end of track pauses when the host reports the transition`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.endOfTrack()
        assertEquals(SleepTimerState.EndOfTrack, engine.state.value)
        engine.onTrackEnded()
        assertEquals(1, actions.pauses)
        assertEquals(SleepTimerState.Off, engine.state.value)
        engine.onTrackEnded()
        assertEquals(1, actions.pauses)
    }

    @Test
    fun `extend without a countdown is ignored`() = runTest {
        val actions = FakeActions()
        val engine = engine(actions)
        engine.extend(60_000L)
        assertEquals(SleepTimerState.Off, engine.state.value)
        engine.endOfTrack()
        engine.extend(60_000L)
        assertEquals(SleepTimerState.EndOfTrack, engine.state.value)
    }
}
