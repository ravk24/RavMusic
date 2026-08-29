package com.ravk24.ravmusic.playback

import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The session-extras side of [SleepTimerCommands] through a real (parcelled) [Bundle]. */
@RunWith(AndroidJUnit4::class)
class SleepTimerCommandsBundleTest {

    @Test
    fun allStatesRoundTripThroughAParcelledBundle() {
        val states = listOf(SleepTimerState.Off, SleepTimerState.Countdown(987_654_321L), SleepTimerState.EndOfTrack)
        for (state in states) {
            val extras = SleepTimerCommands.toExtras(state)
            assertEquals(state, SleepTimerCommands.fromExtras(reparcel(extras)))
        }
    }

    @Test
    fun missingExtrasMeanOff() {
        assertEquals(SleepTimerState.Off, SleepTimerCommands.fromExtras(null))
        assertEquals(SleepTimerState.Off, SleepTimerCommands.fromExtras(Bundle()))
    }

    private fun reparcel(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            bundle.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Bundle.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
