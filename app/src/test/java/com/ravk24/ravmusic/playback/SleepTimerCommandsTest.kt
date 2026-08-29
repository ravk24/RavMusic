package com.ravk24.ravmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerCommandsTest {

    @Test
    fun `all states round-trip through the encoding`() {
        for (state in listOf(SleepTimerState.Off, SleepTimerState.Countdown(123_456L), SleepTimerState.EndOfTrack)) {
            assertEquals(state, SleepTimerCommands.decode(SleepTimerCommands.encode(state)))
        }
    }

    @Test
    fun `end of track wins over a stale end time and zero end time is off`() {
        assertEquals(SleepTimerState.EndOfTrack, SleepTimerCommands.decode(SleepTimerCommands.Encoded(99L, true)))
        assertEquals(SleepTimerState.Off, SleepTimerCommands.decode(SleepTimerCommands.Encoded(0L, false)))
        assertEquals(SleepTimerState.Off, SleepTimerCommands.decode(SleepTimerCommands.Encoded(null, false)))
    }
}
