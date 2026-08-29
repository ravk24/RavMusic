package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.PlaylistsViewModel
import com.ravk24.ravmusic.data.db.RavMusicDatabase
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.repo.PlaylistRepository
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The phase's acceptance test: the real ViewModel over an in-memory Room database, driven through
 * the shell — select three songs, add them to a new playlist, see them in order, then get the
 * duplicate prompt on a second add.
 */
@RunWith(AndroidJUnit4::class)
class AddToPlaylistFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(120)

    private lateinit var db: RavMusicDatabase
    private lateinit var viewModel: PlaylistsViewModel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var shuffled: Pair<List<Song>, String>? = null

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RavMusicDatabase::class.java).build()
        viewModel = PlaylistsViewModel(PlaylistRepository(db.playlistDao()), scope = scope)
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = FakeLibrary.loaded(FakeLibrary.manyFolders(1).let { FakeLibrary.snapshot() }),
                    onRefreshLibrary = {},
                    playerState = PlayerState(),
                    onPlayPause = {},
                    onDismissPlayer = {},
                    onPlaySong = { _, _, _ -> },
                    onShufflePlay = { songs, origin -> shuffled = songs to origin },
                    playlists = viewModel,
                )
            }
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun selectThreeAddToNewPlaylistThenSkipDuplicates() {
        // Music folder has songs 1 ("alpha song") and 2 ("Glass Rain"); Rock has 3; Download has 4.
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("folder_row_music").performClick()
        composeRule.onNodeWithTag("song_row_1").performTouchInput { longClick() }
        composeRule.onNodeWithTag("song_row_2").performClick()
        composeRule.onNodeWithTag("selection_count").assertTextEquals("2 selected")
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        composeRule.onNodeWithTag("sheet_new_playlist").performClick()
        composeRule.onNodeWithTag("playlist_name_field").performTextInput("Late night")
        composeRule.onNodeWithTag("playlist_name_confirm").performClick()
        composeRule.waitUntil(10_000) { viewModel.playlists.value.singleOrNull()?.songCount == 2 }
        composeRule.onNodeWithText("Added 2 to Late night").assertIsDisplayed()

        // Add one more from Rock, this time to the existing playlist.
        composeRule.onNodeWithTag("folder_detail_back").performClick()
        waitForTag("folder_row_rock")
        composeRule.onNodeWithTag("folder_row_rock").performClick()
        waitForTag("song_row_3")
        composeRule.onNodeWithTag("song_row_3").performTouchInput { longClick() }
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        val id = viewModel.playlists.value.single().id
        composeRule.onNodeWithTag("sheet_playlist_$id").performClick()
        composeRule.waitUntil(10_000) { viewModel.playlists.value.single().songCount == 3 }

        // Home shows the card; the detail lists the three in the order they were added.
        composeRule.onNodeWithTag("folder_detail_back").performClick()
        waitForTag("tab_playlists")
        composeRule.onNodeWithTag("tab_playlists").performClick()
        waitForTag("playlist_card_$id")
        composeRule.onNodeWithTag("playlist_card_$id").assertIsDisplayed()
        composeRule.onNodeWithText("Late night").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_card_$id").performClick()
        waitForTag("playlist_subtitle")
        composeRule.onNodeWithTag("playlist_subtitle").assertTextEquals("3 songs · 4m")
        composeRule.waitUntil(10_000) { viewModel.tracks(id).value.size == 3 }
        assertEquals(listOf(1L, 2L, 3L), viewModel.tracks(id).value.map { it.mediaStoreId })
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()

        // Shuffle play hands the playlist's songs to the player with the playlist as origin.
        composeRule.onNodeWithTag("playlist_shuffle").performClick()
        assertEquals("Late night", shuffled?.second)
        assertEquals(listOf(1L, 2L, 3L), shuffled?.first?.map { it.id })

        // Adding the same songs again prompts; skipping keeps three.
        composeRule.onNodeWithTag("playlist_back").performClick()
        waitForTag("tab_folders")
        composeRule.onNodeWithTag("tab_folders").performClick()
        waitForTag("folder_row_music")
        composeRule.onNodeWithTag("folder_row_music").performClick()
        waitForTag("song_row_1")
        composeRule.onNodeWithTag("song_row_1").performTouchInput { longClick() }
        composeRule.onNodeWithTag("selection_all").performClick()
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        composeRule.onNodeWithTag("sheet_playlist_$id").performClick()
        composeRule.onNodeWithText("2 already in this playlist").assertIsDisplayed()
        composeRule.onNodeWithTag("dup_skip").performClick()
        composeRule.onNodeWithText("Added 0 to Late night").assertIsDisplayed()
        assertEquals(3, viewModel.playlists.value.single().songCount)
    }
}
