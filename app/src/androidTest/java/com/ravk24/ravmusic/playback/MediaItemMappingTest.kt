package com.ravk24.ravmusic.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaItemMappingTest {

    @Test
    fun songRoundTripsThroughMediaItem() {
        val song = Song(42L, "content://media/external/audio/media/42", "Glass Rain", "Hyaline", 245_000L, "m", "Music")
        val items = planQueue(listOf(song), 0, "Music")!!.toMediaItems()
        val item = items.single()

        assertEquals("42", item.mediaId)
        assertEquals(song.uri, item.localConfiguration?.uri.toString())
        val now = nowPlayingFrom(item)
        assertEquals(NowPlaying(42L, "Glass Rain", "Hyaline", "Music"), now)
    }

    @Test
    fun untaggedArtistStaysNull() {
        val song = Song(7L, "content://media/external/audio/media/7", "alpha song", null, 35_000L, "m", "Music")
        val now = nowPlayingFrom(planQueue(listOf(song), 0, "Music")!!.toMediaItems().single())
        assertNull(now.artist)
        assertEquals("alpha song", now.title)
    }
}
