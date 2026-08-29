package com.ravk24.ravmusic.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.player.MiniPlayer
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playing = PlayerState(
        nowPlaying = NowPlaying(4L, "Copper Sky", "Nocturne Ave", "Music"),
        isPlaying = true,
        positionMs = 50_000L,
        durationMs = 100_000L,
    )

    private fun set(state: PlayerState, onPlayPause: () -> Unit = {}, onExpand: () -> Unit = {}, onDismiss: () -> Unit = {}) {
        composeRule.setContent {
            RavMusicTheme {
                MiniPlayer(state = state, onPlayPause = onPlayPause, onExpand = onExpand, onDismiss = onDismiss)
            }
        }
    }

    @Test
    fun showsSongProgressAndPauseWhilePlaying() {
        set(playing)
        composeRule.onNodeWithText("Copper Sky").assertIsDisplayed()
        composeRule.onNodeWithText("Nocturne Ave").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player_progress").assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun showsPlayAndUnknownArtistWhenPaused() {
        set(playing.copy(nowPlaying = playing.nowPlaying!!.copy(artist = null), isPlaying = false))
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()
    }

    @Test
    fun toggleCallsBack() {
        var toggles = 0
        set(playing, onPlayPause = { toggles++ })
        composeRule.onNodeWithTag("mini_player_toggle").performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun swipeDismisses() {
        var dismissed = 0
        set(playing, onDismiss = { dismissed++ })
        composeRule.onNodeWithTag("mini_player").performTouchInput { swipeRight() }
        composeRule.waitUntil(5_000) { dismissed == 1 }
        assertEquals(1, dismissed)
    }

    @Test
    fun bodyTapIsInert() {
        var expanded = 0
        set(playing, onExpand = { expanded++ })
        composeRule.onNodeWithTag("mini_player_body").performClick()
        composeRule.onNodeWithText("Copper Sky").assertIsDisplayed()
        // onExpand is wired but the shell passes a no-op; the composable itself forwards the tap.
        assertEquals(1, expanded)
    }

    @Test
    fun hiddenWithoutQueue() {
        set(PlayerState())
        composeRule.onNodeWithTag("mini_player").assertDoesNotExist()
    }
}
