package com.ravk24.ravmusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.permission.PermissionState
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private var permission by mutableStateOf<PermissionState>(PermissionState.Granted)
    private var library by mutableStateOf<LibraryState>(FakeLibrary.loaded())

    private fun setShell() {
        composeRule.setContent {
            RavMusicTheme {
                AppNavigation(
                    permissionState = permission,
                    onRequestPermission = {},
                    onOpenAppSettings = {},
                    libraryState = library,
                    onRefreshLibrary = {},
                    playerState = PlayerState(),
                    onPlayPause = {},
                    onDismissPlayer = {},
                    onPlaySong = { _, _, _ -> },
                )
            }
        }
    }

    private fun openRock() {
        composeRule.onNodeWithTag("tab_folders").performClick()
        composeRule.onNodeWithTag("folder_row_rock").performClick()
        composeRule.onNodeWithTag("screen_folder_detail").assertIsDisplayed()
    }

    @Test
    fun folderRow_opensDetailWithoutBottomBar() {
        setShell()
        openRock()
        composeRule.onNodeWithTag("folder_detail_title").assertTextEquals("Rock")
        composeRule.onNodeWithTag("song_row_3").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertDoesNotExist()
    }

    @Test
    fun systemBack_returnsToFoldersWithTabSelected() {
        setShell()
        openRock()
        Espresso.pressBack()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").assertIsSelected()
    }

    @Test
    fun backAffordance_returnsToFolders() {
        setShell()
        openRock()
        composeRule.onNodeWithTag("folder_detail_back").performClick()
        composeRule.onNodeWithTag("screen_folders").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_folders").assertIsSelected()
    }

    @Test
    fun permissionDeniedOnDetail_showsGate() {
        setShell()
        openRock()
        permission = PermissionState.Denied(canRequest = true)
        composeRule.onNodeWithTag("no_music_found").assertIsDisplayed()
        composeRule.onNodeWithTag("songs_list").assertDoesNotExist()
    }

    @Test
    fun libraryClearedOnDetail_showsEmptyFolder() {
        setShell()
        openRock()
        library = LibraryState.Idle
        composeRule.onNodeWithTag("folder_detail_empty").assertIsDisplayed()
    }
}
