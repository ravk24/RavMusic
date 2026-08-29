package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.ui.folders.FolderDetailScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDetailSelectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val songs = FakeLibrary.snapshot().songsIn("music") + FakeLibrary.snapshot().songsIn("rock")
    private lateinit var host: FakePlaylistsHost
    private var played: Song? = null

    private fun set(seedPlaylist: Boolean = false) {
        host = FakePlaylistsHost()
        if (seedPlaylist) host.seed("Late night", songs.take(1))
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(
                    folderName = "Mixed",
                    songs = songs,
                    onBack = {},
                    onSongClick = { played = it },
                    playlists = host,
                )
            }
        }
    }

    private fun longPress(id: Long) {
        composeRule.onNodeWithTag("song_row_$id").performTouchInput { longClick() }
    }

    @Test
    fun longPressSelectsTapTogglesSelectAllAndClose() {
        set()
        longPress(1)
        composeRule.onNodeWithTag("selection_count").assertTextEquals("1 selected")
        composeRule.onNodeWithTag("song_check_1", useUnmergedTree = true).assertIsDisplayed()
        assertNull(played)

        composeRule.onNodeWithTag("song_row_2").performClick()
        composeRule.onNodeWithTag("selection_count").assertTextEquals("2 selected")
        composeRule.onNodeWithTag("song_row_2").performClick()
        composeRule.onNodeWithTag("selection_count").assertTextEquals("1 selected")
        assertNull(played)

        composeRule.onNodeWithTag("selection_all").performClick()
        composeRule.onNodeWithTag("selection_count").assertTextEquals("3 selected")
        composeRule.onNodeWithTag("add_to_playlist").assertIsDisplayed()

        composeRule.onNodeWithTag("selection_close").performClick()
        composeRule.onNodeWithTag("selection_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("folder_detail_title").assertIsDisplayed()

        composeRule.onNodeWithTag("song_row_2").performClick()
        assertEquals(2L, played?.id)
    }

    @Test
    fun systemBackLeavesSelectionFirst() {
        set()
        longPress(3)
        composeRule.onNodeWithTag("selection_bar").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithTag("selection_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("screen_folder_detail").assertIsDisplayed()
    }

    @Test
    fun addToNewPlaylistCreatesAndConfirms() {
        set()
        longPress(1)
        composeRule.onNodeWithTag("song_row_3").performClick()
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        composeRule.onNodeWithTag("sheet_new_playlist").performClick()
        composeRule.onNodeWithTag("playlist_name_field").performTextInput("Road trip")
        composeRule.onNodeWithTag("playlist_name_confirm").performClick()

        composeRule.waitUntil(5_000) { host.playlists.value.singleOrNull()?.songCount == 2 }
        composeRule.onNodeWithTag("selection_bar").assertDoesNotExist()
        composeRule.onNodeWithText("Added 2 to Road trip").assertIsDisplayed()
        assertEquals(listOf(1L, 3L), host.tracks(host.playlists.value.single().id).value.map { it.mediaStoreId })
    }

    @Test
    fun duplicatesPromptSkipAddsOnlyNew() {
        set(seedPlaylist = true)
        val id = host.playlists.value.single().id
        longPress(1)
        composeRule.onNodeWithTag("song_row_2").performClick()
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        composeRule.onNodeWithTag("sheet_playlist_$id").performClick()
        composeRule.onNodeWithTag("dup_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("1 already in this playlist").assertIsDisplayed()
        composeRule.onNodeWithTag("dup_skip").performClick()

        composeRule.waitUntil(5_000) { host.tracks(id).value.size == 2 }
        composeRule.onNodeWithText("Added 1 to Late night").assertIsDisplayed()
        assertEquals(listOf(1L, 2L), host.tracks(id).value.map { it.mediaStoreId })
    }

    @Test
    fun duplicatesPromptAddAnywayAddsAll() {
        set(seedPlaylist = true)
        val id = host.playlists.value.single().id
        longPress(1)
        composeRule.onNodeWithTag("add_to_playlist").performClick()
        composeRule.onNodeWithTag("sheet_playlist_$id").performClick()
        composeRule.onNodeWithTag("dup_add_anyway").performClick()
        composeRule.waitUntil(5_000) { host.tracks(id).value.size == 2 }
        assertEquals(listOf(1L, 1L), host.tracks(id).value.map { it.mediaStoreId })
    }
}
