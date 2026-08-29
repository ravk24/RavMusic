package com.ravk24.ravmusic.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Folder
import com.ravk24.ravmusic.data.model.LibrarySnapshot
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.ui.folders.FoldersScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoldersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private fun setScreen(
        state: LibraryState,
        onRefresh: () -> Unit = {},
        onOpenFolder: (Folder) -> Unit = {},
    ) {
        composeRule.setContent {
            RavMusicTheme {
                FoldersScreen(
                    state = state,
                    listState = rememberLazyListState(),
                    onRefresh = onRefresh,
                    onOpenFolder = onOpenFolder,
                )
            }
        }
    }

    @Test
    fun loaded_showsFoldersInOrderWithCountsTotalAndFooter() {
        setScreen(FakeLibrary.loaded())

        composeRule.onNodeWithTag("folders_total").assertTextContains("4 songs")
        composeRule.onNodeWithTag("folder_row_download").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_row_music").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_row_rock").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertIsDisplayed()
        composeRule.onNodeWithText("2 songs").assertIsDisplayed()
        composeRule.onNodeWithTag("folders_footer").assertIsDisplayed()

        // Order: Download, Music, Rock (alphabetical).
        val rows = composeRule.onAllNodes(hasTestTag("folder_row_download") or hasTestTag("folder_row_music") or hasTestTag("folder_row_rock"))
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }
        assertEquals(listOf("folder_row_download", "folder_row_music", "folder_row_rock"), rows)
    }

    @Test
    fun rowClick_passesTheFolder() {
        var opened: Folder? = null
        setScreen(FakeLibrary.loaded(), onOpenFolder = { opened = it })
        composeRule.onNodeWithTag("folder_row_rock").performClick()
        assertEquals("rock", opened?.id)
        assertEquals("Rock", opened?.name)
        assertEquals(1, opened?.songCount)
    }

    @Test
    fun emptyLibrary_showsEmptyStateAndRescanRefreshes() {
        var refreshes = 0
        setScreen(LibraryState.Loaded(LibrarySnapshot.EMPTY), onRefresh = { refreshes++ })
        composeRule.onNodeWithTag("library_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No music found").assertIsDisplayed()
        composeRule.onNodeWithTag("folders_list").assertDoesNotExist()
        composeRule.onNodeWithTag("rescan_button").performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun loading_showsIndicator() {
        setScreen(LibraryState.Loading)
        composeRule.onNodeWithTag("folders_loading").assertIsDisplayed()
        composeRule.onNodeWithText("Folders").assertIsDisplayed()
        composeRule.onNodeWithTag("folders_list").assertDoesNotExist()
    }

    @Test
    fun refreshing_keepsListVisible() {
        setScreen(FakeLibrary.loaded(refreshing = true))
        composeRule.onNodeWithTag("folders_list").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_row_music").assertIsDisplayed()
    }
}
