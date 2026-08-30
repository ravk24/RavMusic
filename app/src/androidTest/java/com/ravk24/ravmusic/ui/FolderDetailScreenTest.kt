package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.ui.folders.FolderDetailScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    @Test
    fun showsTitleCountAndRows() {
        val songs = FakeLibrary.snapshot().songsIn("music")
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Music", songs = songs, onBack = {})
            }
        }
        composeRule.onNodeWithTag("folder_detail_title").assertTextEquals("Music")
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("2 songs · long-press to select")
        composeRule.onNodeWithTag("song_row_1").assertIsDisplayed()
        composeRule.onNodeWithText("alpha song").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()
        composeRule.onNodeWithText("0:35").assertIsDisplayed()
        composeRule.onNodeWithText("Hyaline").assertIsDisplayed()
        composeRule.onNodeWithText("3:41").assertIsDisplayed()
    }

    @Test
    fun longDurationAndTaggedArtist() {
        val songs = FakeLibrary.snapshot().songsIn("download")
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Download", songs = songs, onBack = {})
            }
        }
        composeRule.onNodeWithText("Nocturne Ave").assertIsDisplayed()
        composeRule.onNodeWithText("1:02:05").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("1 song · long-press to select")
    }

    @Test
    fun songTapIsInertAndBackCallsBack() {
        var backs = 0
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(
                    folderName = "Rock",
                    songs = FakeLibrary.snapshot().songsIn("rock"),
                    onBack = { backs++ },
                )
            }
        }
        composeRule.onNodeWithTag("song_row_3").performClick()
        composeRule.onNodeWithTag("screen_folder_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_detail_back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun searchFiltersSongsAndTapPlaysTheMatch() {
        var played: Song? = null
        val songs = FakeLibrary.snapshot().songsIn("music")
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Music", songs = songs, onBack = {}, onSongClick = { played = it })
            }
        }
        composeRule.onNodeWithTag("folder_search").performClick()
        composeRule.onNodeWithTag("search_field").performTextInput("glass")
        composeRule.onNodeWithTag("song_row_1").assertDoesNotExist()
        composeRule.onNodeWithTag("song_row_2").assertIsDisplayed()
        composeRule.onNodeWithTag("song_row_2").performClick()
        assertEquals(2L, played?.id)

        composeRule.onNodeWithTag("search_field").performTextInput("zzz")
        composeRule.onNodeWithTag("search_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No songs match “glasszzz”").assertIsDisplayed()

        composeRule.onNodeWithTag("search_close").performClick()
        composeRule.onNodeWithTag("folder_detail_title").assertIsDisplayed()
        composeRule.onNodeWithTag("song_row_1").assertIsDisplayed()
        composeRule.onNodeWithTag("song_row_2").assertIsDisplayed()
    }

    @Test
    fun emptyFolder_showsEmptyStateWithBackAction() {
        var backs = 0
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Gone", songs = emptyList(), onBack = { backs++ })
            }
        }
        composeRule.onNodeWithTag("folder_detail_empty").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("0 songs")
        composeRule.onNodeWithTag("folder_detail_empty_action").performClick()
        assertEquals(1, backs)
    }
}
