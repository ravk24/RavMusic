package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/** Search across playlists as a route above the Playlists tab (change `search`). */
@RunWith(AndroidJUnit4::class)
class PlaylistSearchNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playlists = FakePlaylistsHost()
    private val played = mutableListOf<Triple<List<Song>, Int, String>>()

    private fun setShell() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = FakeLibrary.loaded(),
                    onRefreshLibrary = {},
                    playerState = PlayerState(),
                    player = PlayerActions.none().copy(onPlaySongs = { songs, index, origin -> played += Triple(songs, index, origin) }),
                    playlists = playlists,
                )
            }
        }
    }

    @Test
    fun searchPlaysFromTheRightPlaylistAndKeepsTheQueryAcrossADetail() {
        val snapshot = FakeLibrary.snapshot()
        val alpha = snapshot.songs.first { it.id == 1L }
        val glass = snapshot.songs.first { it.id == 2L }
        val late = playlists.seed("Late night", listOf(alpha, glass))
        val focus = playlists.seed("Focus", listOf(glass))
        val focusGlass = playlists.tracks(focus).value.single()
        val lateGlass = playlists.tracks(late).value.first { it.title == "Glass Rain" }
        setShell()

        composeRule.onNodeWithTag("playlists_search").performClick()
        composeRule.onNodeWithTag("screen_playlist_search").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("search_field").performTextInput("glass")

        // The same song in two playlists is two hits; each plays its own playlist from that song.
        composeRule.onNodeWithTag("hit_row_${focusGlass.id}").performClick()
        assertEquals(listOf(2L), played[0].first.map { it.id })
        assertEquals(0, played[0].second)
        assertEquals("Focus", played[0].third)

        composeRule.onNodeWithTag("hit_row_${lateGlass.id}").performClick()
        assertEquals(listOf(1L, 2L), played[1].first.map { it.id })
        assertEquals(1, played[1].second)
        assertEquals("Late night", played[1].third)

        composeRule.onNodeWithTag("hit_open_${lateGlass.id}", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("screen_playlist_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_title").assertTextEquals("Late night")

        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlist_search").assertIsDisplayed()
        composeRule.onNodeWithTag("hit_row_${lateGlass.id}").assertIsDisplayed()

        // The field re-focuses on return and raises the keyboard, which takes the first back press.
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
    }
}
