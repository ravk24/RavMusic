package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.OPENED_FILE_ORIGIN
import com.ravk24.ravmusic.data.model.OpenedFile
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.openedSong
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

/** The shell's side of `open-with`: play the file, then open Now Playing once the session has it. */
@RunWith(AndroidJUnit4::class)
class OpenWithNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val song = openedSong("content://com.example.files/doc/9", "voice note")
    private var permission by mutableStateOf<PermissionState>(PermissionState.Granted)
    private var player by mutableStateOf(PlayerState())
    private var opened by mutableStateOf<OpenedFile?>(null)
    private val played = mutableListOf<Triple<List<Song>, Int, String>>()
    private var handled = 0

    private fun setShell() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = permission,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = FakeLibrary.loaded(),
                    onRefreshLibrary = {},
                    playerState = player,
                    player = PlayerActions.none().copy(onPlaySongs = { songs, index, origin -> played += Triple(songs, index, origin) }),
                    playlists = FakePlaylistsHost(),
                    openedFile = opened,
                    onOpenedFileHandled = {
                        handled++
                        opened = null
                    },
                )
            }
        }
    }

    private fun playing(current: Song) = PlayerState(
        nowPlaying = NowPlaying(current.id, current.title, current.artist, OPENED_FILE_ORIGIN),
        isPlaying = true,
        queue = listOf(QueueEntry(current.id, current.title, current.artist, 0)),
        queueIndex = 0,
    )

    @Test
    fun openedFile_playsAloneThenShowsNowPlayingOnceTheSessionHasIt() {
        setShell()
        opened = OpenedFile(song, 1)
        composeRule.waitForIdle()

        assertEquals(1, played.size)
        assertEquals(listOf(song), played[0].first)
        assertEquals(0, played[0].second)
        assertEquals(OPENED_FILE_ORIGIN, played[0].third)
        assertEquals(1, handled)
        // Nothing is current yet: the shell must not push a screen that would close itself.
        composeRule.onNodeWithTag("screen_now_playing").assertDoesNotExist()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()

        player = playing(song)
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
        composeRule.onNodeWithTag("np_origin").assertTextContains(OPENED_FILE_ORIGIN, substring = true)

        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("mini_player").assertIsDisplayed()
    }

    @Test
    fun secondFileWhileNowPlayingIsOpen_doesNotStackAnotherPlayer() {
        player = playing(song)
        setShell()
        composeRule.onNodeWithTag("mini_player_body").performClick()
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()

        val other = openedSong("content://com.example.files/doc/10", "other note")
        opened = OpenedFile(other, 2)
        composeRule.waitForIdle()
        assertEquals(listOf(other), played.single().first)

        player = playing(other)
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
    }

    @Test
    fun withoutTheAudioPermission_stillPlaysAndBackLandsOnTheGate() {
        permission = PermissionState.Denied(canRequest = true)
        setShell()
        opened = OpenedFile(song, 1)
        composeRule.waitForIdle()
        assertEquals(1, played.size)

        player = playing(song)
        composeRule.onNodeWithTag("screen_now_playing").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithTag("no_music_found").assertIsDisplayed()
    }
}
