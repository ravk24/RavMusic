package com.ravk24.ravmusic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("2 songs")
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
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("1 song")
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
    fun emptyFolder_showsMessage() {
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Gone", songs = emptyList(), onBack = {})
            }
        }
        composeRule.onNodeWithTag("folder_detail_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_detail_subtitle").assertTextEquals("0 songs")
    }
}
