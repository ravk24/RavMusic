package com.ravk24.ravmusic.ui

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.ui.playlists.PlaylistsScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val playlists = listOf(
        Playlist(1, "Late night", 42, (2 * 60 + 58) * 60_000L, 0),
        Playlist(2, "Focus", 1, 4 * 60_000L, 0),
    )

    private fun set(list: List<Playlist>, onOpen: (Playlist) -> Unit = {}, onCreate: (String) -> Unit = {}) {
        composeRule.setContent {
            RavMusicTheme {
                PlaylistsScreen(
                    playlists = list,
                    gridState = rememberLazyGridState(),
                    onOpenSettings = {},
                    onOpenPlaylist = onOpen,
                    onCreate = onCreate,
                )
            }
        }
    }

    @Test
    fun gridShowsCardsWithMetaAndTotal() {
        var opened: Playlist? = null
        set(playlists, onOpen = { opened = it })
        composeRule.onNodeWithText("Late night").assertIsDisplayed()
        composeRule.onNodeWithText("42 songs · 2h 58m").assertIsDisplayed()
        composeRule.onNodeWithText("1 song · 4m").assertIsDisplayed()
        composeRule.onNodeWithText("2 playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("playlist_card_2").performClick()
        assertEquals(2L, opened?.id)
    }

    @Test
    fun emptyStateOffersCreate() {
        set(emptyList())
        composeRule.onNodeWithTag("playlists_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No playlists yet").assertIsDisplayed()
        composeRule.onNodeWithTag("playlists_list").assertDoesNotExist()
        composeRule.onNodeWithTag("empty_new_playlist").performClick()
        composeRule.onNodeWithTag("playlist_name_dialog").assertIsDisplayed()
    }

    @Test
    fun fabCreatesAfterValidName() {
        var created: String? = null
        set(playlists, onCreate = { created = it })
        composeRule.onNodeWithTag("new_playlist_fab").performClick()
        composeRule.onNodeWithTag("playlist_name_confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("playlist_name_field").performTextInput("   ")
        composeRule.onNodeWithTag("playlist_name_confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("playlist_name_field").performTextInput("Road trip ")
        composeRule.onNodeWithTag("playlist_name_confirm").assertIsEnabled()
        composeRule.onNodeWithTag("playlist_name_confirm").performClick()
        assertEquals("Road trip", created)
        composeRule.onNodeWithTag("playlist_name_dialog").assertDoesNotExist()
    }
}
