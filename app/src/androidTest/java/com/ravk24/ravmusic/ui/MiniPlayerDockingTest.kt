package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerDockingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private var player by mutableStateOf(PlayerState())
    private val playing = PlayerState(NowPlaying(3L, "Beta Song", null, "Rock"), isPlaying = true, durationMs = 41_000L)

    private var played: Triple<List<Song>, Int, String>? = null

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
                    onPlayPause = {},
                    onDismissPlayer = { player = PlayerState() },
                    onPlaySong = { songs, index, origin -> played = Triple(songs, index, origin) },
                )
            }
        }
    }

    @Test
    fun hiddenWhenIdle_shownAboveTabsWhenLoaded() {
        setShell()
        composeRule.onNodeWithTag("mini_player").assertDoesNotExist()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()

        player = playing
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
        val playerBottom = composeRule.onNodeWithTag("mini_player").fetchSemanticsNode().boundsInRoot.bottom
        val barTop = composeRule.onNodeWithTag("bottom_bar").fetchSemanticsNode().boundsInRoot.top
        assertTrue("mini player must sit above the bar", playerBottom <= barTop + 1f)
    }

    @Test
    fun shownAloneOnDetail_andDismissHides() {
        player = playing
        setShell()
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("folder_row_rock").performClick()
        composeRule.onNodeWithTag("screen_folder_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()

        player = PlayerState()
        composeRule.onNodeWithTag("mini_player").assertDoesNotExist()
    }

    @Test
    fun songTap_playsFolderFromIndex_andCurrentRowIsSelected() {
        setShell()
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("folder_row_music").performClick()
        composeRule.onNodeWithTag("song_row_2").performClick()

        val (songs, index, origin) = played!!
        assertEquals(listOf(1L, 2L), songs.map { it.id })
        assertEquals(1, index)
        assertEquals("Music", origin)

        player = playing.copy(nowPlaying = NowPlaying(2L, "Glass Rain", "Hyaline", "Music"))
        composeRule.onNodeWithTag("song_row_2").assertIsSelected()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
    }
}
