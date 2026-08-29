package com.ravk24.ravmusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.ui.folders.FolderDetailScreen
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/** Long-press then drag selects a range (spec F2 stretch, design D8 of `polish`). */
@RunWith(AndroidJUnit4::class)
class FolderDetailDragSelectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    private val songs = (1L..6L).map { i ->
        Song(i, "content://media/external/audio/media/$i", "Song $i", null, 200_000L, "f", "Folder")
    }

    private fun set() {
        composeRule.setContent {
            RavMusicTheme {
                FolderDetailScreen(folderName = "Folder", songs = songs, onBack = {})
            }
        }
    }

    /** Centre of a row in the list's own coordinates (what `performTouchInput` on the list uses). */
    private fun rowCentre(id: Long): Offset {
        val list = composeRule.onNodeWithTag("songs_list").fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag("song_row_$id").fetchSemanticsNode().boundsInRoot
        return Offset(row.center.x - list.left, row.center.y - list.top)
    }

    private fun count() = composeRule.onNodeWithTag("selection_count")

    @Test
    fun dragAfterLongPressSelectsARangeAndRetreatShrinksIt() {
        set()
        val r1 = rowCentre(1)
        val r2 = rowCentre(2)
        val r3 = rowCentre(3)
        val list = composeRule.onNodeWithTag("songs_list")

        list.performTouchInput {
            down(r1)
            advanceEventTime(LONG_PRESS_MS)
            moveTo(r1 + Offset(0f, 1f))
        }
        count().assertTextEquals("1 selected")

        list.performTouchInput { moveTo(r3) }
        count().assertTextEquals("3 selected")

        list.performTouchInput { moveTo(r2) }
        count().assertTextEquals("2 selected")

        list.performTouchInput { up() }
        count().assertTextEquals("2 selected")
    }

    @Test
    fun dragAddsToAnExistingSelection() {
        set()
        composeRule.onNodeWithTag("song_row_5").performTouchInput { longClick() }
        count().assertTextEquals("1 selected")

        val list = composeRule.onNodeWithTag("songs_list")
        list.performTouchInput {
            down(rowCentre(1))
            advanceEventTime(LONG_PRESS_MS)
            moveTo(rowCentre(2))
            up()
        }
        count().assertTextEquals("3 selected")
    }

    @Test
    fun plainLongPressStillSelectsOneRow() {
        set()
        composeRule.onNodeWithTag("song_row_2").performTouchInput { longClick() }
        count().assertTextEquals("1 selected")
    }

    private companion object {
        /** Comfortably past the platform long-press timeout (400 ms on the emulators). */
        const val LONG_PRESS_MS = 1_000L
    }
}
