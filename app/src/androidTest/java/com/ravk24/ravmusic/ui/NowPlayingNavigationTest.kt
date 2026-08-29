package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueueEntry
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playing = PlayerState(
        nowPlaying = NowPlaying(3L, "Beta Song", null, "Rock"),
        isPlaying = true,
        durationMs = 41_000L,
        queue = listOf(QueueEntry(3L, "Beta Song", null, 0), QueueEntry(4L, "gamma", null, 1)),
        queueIndex = 0,
    )
    private var player by mutableStateOf(playing)
    private var jumped: Int? = null

    private fun setShell() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = FakeLibrary.loaded(),
                    onRefreshLibrary = {},
                    playerState = player,
                    player = PlayerActions.none().copy(onJumpTo = { jumped = it }),
                    playlists = FakePlaylistsHost(),
                )
            }
        }
    }

    private fun open() {
        composeRule.onNodeWithTag("mini_player_body").performClick()
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
    }

    @Test
    fun miniPlayerTapOpensFullScreenWithoutBarOrMiniPlayer() {
        setShell()
        open()
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("mini_player").assertDoesNotExist()
    }

    @Test
    fun collapseAndBackReturnWithMiniPlayer() {
        setShell()
        open()
        composeRule.onNodeWithTag("np_collapse").performClick()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()

        open()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
    }

    @Test
    fun queueClearedClosesTheScreen() {
        setShell()
        open()
        player = PlayerState()
        composeRule.onNodeWithTag("screen_now_playing").assertDoesNotExist()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertDoesNotExist()
    }

    @Test
    fun queueSheetJumpGoesThroughActions() {
        setShell()
        open()
        composeRule.onNodeWithTag("np_queue_chip").performClick()
        composeRule.onNodeWithTag("queue_row_1").performClick()
        assertEquals(1, jumped)
    }
}
