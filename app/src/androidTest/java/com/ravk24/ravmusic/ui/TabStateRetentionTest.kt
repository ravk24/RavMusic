package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.permission.PermissionState
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
class TabStateRetentionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val folderCount = 30

    private val library = FakeLibrary.loaded(FakeLibrary.manyFolders(folderCount))

    /** Index of the last folder row (the footer follows it); scrolling here keeps that row visible. */
    private val lastIndex = folderCount - 1

    private val lastFolderName = "Folder %02d".format(folderCount)

    /** The shell is rendered in a short box so the placeholder list actually scrolls. */
    private fun setShortShell() {
        composeRule.setContent {
            RavMusicTheme {
                Box(modifier = Modifier.height(260.dp)) {
                    AppNavigation(
                        permissionState = PermissionState.Granted,
                        onRequestPermission = {},
                        onOpenAppSettings = {},
                        libraryState = library,
                        onRefreshLibrary = {},
                        playerState = PlayerState(),
                        onPlayPause = {},
                        onDismissPlayer = {},
                        onPlaySong = { _, _, _ -> },
                        onShufflePlay = { _, _ -> },
                        playlists = FakePlaylistsHost(),
                    )
                }
            }
        }
    }

    /** Current vertical scroll offset of the Folders placeholder list, from its semantics. */
    private fun foldersScrollOffset(): Float =
        composeRule.onNodeWithTag("folders_list")
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    @Test
    fun foldersScrollPosition_survivesSwitchingTabs() {
        setShortShell()
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        assertEquals(0f, foldersScrollOffset(), 0.01f)

        composeRule.onNodeWithTag("folders_list").performScrollToIndex(lastIndex)
        composeRule.waitForIdle()
        val scrolled = foldersScrollOffset()
        assertTrue("list should have scrolled, offset=$scrolled", scrolled > 0f)
        composeRule.onNodeWithText(lastFolderName).assertExists()

        composeRule.onNodeWithTag("tab_playlists").performClick()
        composeRule.onNodeWithTag("screen_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").performClick()

        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        assertEquals("scroll offset must survive the tab round-trip", scrolled, foldersScrollOffset(), 0.5f)
        composeRule.onNodeWithText(lastFolderName).assertExists()
    }

    @Test
    fun selectedTab_survivesRecreation() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = PermissionState.Granted,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = library,
                    onRefreshLibrary = {},
                    playerState = PlayerState(),
                    onPlayPause = {},
                    onDismissPlayer = {},
                    onPlaySong = { _, _, _ -> },
                    onShufflePlay = { _, _ -> },
                    playlists = FakePlaylistsHost(),
                )
            }
        }
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").assertIsSelected()
    }
}
