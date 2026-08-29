package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueueEntry
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.navigation.NavTransitions
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The Now Playing route animates over the current screen (design D7 of `polish`): with the clock
 * paused, both screens exist mid-transition; once it settles only one does.
 */
@RunWith(AndroidJUnit4::class)
class NavTransitionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playing = PlayerState(
        nowPlaying = NowPlaying(3L, "Beta Song", null, "Rock"),
        isPlaying = true,
        durationMs = 41_000L,
        queue = listOf(QueueEntry(3L, "Beta Song", null, 0)),
        queueIndex = 0,
    )

    private fun setShell() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = FakeLibrary.loaded(),
                    onRefreshLibrary = {},
                    playerState = playing,
                    player = PlayerActions.none(),
                    playlists = FakePlaylistsHost(),
                )
            }
        }
    }

    @Test
    fun nowPlayingSlidesOverThenReplacesTheTab() {
        setShell()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag("mini_player_body").performClick()
        composeRule.mainClock.advanceTimeBy(NavTransitions.PLAYER_MS / 2L)
        composeRule.onNodeWithTag("screen_now_playing").assertExists()
        composeRule.onNodeWithTag("screen_playlists").assertExists()

        composeRule.mainClock.advanceTimeBy(NavTransitions.PLAYER_MS * 2L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
        composeRule.onNodeWithTag("screen_playlists").assertDoesNotExist()
    }

    @Test
    fun collapseSlidesTheTabBackIn() {
        setShell()
        composeRule.onNodeWithTag("mini_player_body").performClick()
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("np_collapse").performClick()
        composeRule.mainClock.advanceTimeBy(NavTransitions.PLAYER_MS / 2L)
        composeRule.onNodeWithTag("screen_now_playing").assertExists()
        composeRule.onNodeWithTag("screen_playlists").assertExists()

        composeRule.mainClock.advanceTimeBy(NavTransitions.PLAYER_MS * 2L)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("screen_now_playing").assertDoesNotExist()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
    }
}
