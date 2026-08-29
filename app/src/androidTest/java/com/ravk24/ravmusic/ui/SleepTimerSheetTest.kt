package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.playback.SleepTimerState
import com.ravk24.ravmusic.ui.nowplaying.SleepTimerSheet
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepTimerSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private class Calls {
        var preset: Int? = null; var custom: Int? = null; var endOfTrack = 0; var extend = 0; var cancel = 0; var dismiss = 0
    }

    private fun set(state: SleepTimerState, remaining: Long? = null): Calls {
        val c = Calls()
        composeRule.setContent {
            RavMusicTheme {
                SleepTimerSheet(
                    state = state, remainingMs = remaining,
                    onPreset = { c.preset = it }, onCustom = { c.custom = it }, onEndOfTrack = { c.endOfTrack++ },
                    onExtend = { c.extend++ }, onCancel = { c.cancel++ }, onDismiss = { c.dismiss++ },
                )
            }
        }
        return c
    }

    @Test
    fun presetsCallBackAndDismiss() {
        val c = set(SleepTimerState.Off)
        composeRule.onNodeWithTag("sleep_preset_30").performClick()
        assertEquals(30, c.preset)
        assertEquals(1, c.dismiss)
    }

    @Test
    fun customMinutesValidation() {
        val c = set(SleepTimerState.Off)
        composeRule.onNodeWithTag("sleep_custom_set").assertIsNotEnabled()
        composeRule.onNodeWithTag("sleep_custom_field").performTextInput("0")
        composeRule.onNodeWithTag("sleep_custom_set").assertIsNotEnabled()
        composeRule.onNodeWithTag("sleep_custom_field").performTextReplacement("12")
        composeRule.onNodeWithTag("sleep_custom_set").assertIsEnabled()
        composeRule.onNodeWithTag("sleep_custom_set").performClick()
        assertEquals(12, c.custom)
        assertEquals(1, c.dismiss)
    }

    @Test
    fun endOfTrackCallsBack() {
        val c = set(SleepTimerState.Off)
        composeRule.onNodeWithTag("sleep_end_of_track").performClick()
        assertEquals(1, c.endOfTrack)
    }

    @Test
    fun activeCountdownShowsRemainingExtendAndCancel() {
        val c = set(SleepTimerState.Countdown(999L), remaining = 130_000L)
        composeRule.onNodeWithTag("sleep_remaining").assertTextEquals("Pausing in 02:10")
        composeRule.onNodeWithTag("sleep_preset_15").assertDoesNotExist()
        composeRule.onNodeWithTag("sleep_extend").performClick()
        assertEquals(1, c.extend)
        assertEquals(1, c.dismiss)
    }

    @Test
    fun endOfTrackModeOffersOnlyCancel() {
        val c = set(SleepTimerState.EndOfTrack)
        composeRule.onNodeWithTag("sleep_remaining").assertTextEquals("Pausing at the end of this track")
        composeRule.onNodeWithTag("sleep_extend").assertDoesNotExist()
        composeRule.onNodeWithTag("sleep_cancel").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_cancel").performClick()
        assertEquals(1, c.cancel)
    }
}
