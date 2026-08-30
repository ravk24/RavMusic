package com.ravk24.ravmusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenedFileTest {

    private val view = "android.intent.action.VIEW"
    private val send = "android.intent.action.SEND"

    @Test
    fun `VIEW uses the intent data`() {
        assertEquals("content://a/1", openRequestUri(view, "content://a/1", "content://s/2", "content://c/3"))
    }

    @Test
    fun `VIEW without data falls back to the clip`() {
        assertEquals("content://c/3", openRequestUri(view, null, null, "content://c/3"))
        assertEquals("content://c/3", openRequestUri(view, "  ", null, "content://c/3"))
    }

    @Test
    fun `SEND uses the stream extra, then the clip`() {
        assertEquals("content://s/2", openRequestUri(send, "content://a/1", "content://s/2", "content://c/3"))
        assertEquals("content://c/3", openRequestUri(send, null, null, "content://c/3"))
    }

    @Test
    fun `other actions and blank URIs are not requests`() {
        assertNull(openRequestUri("android.intent.action.MAIN", "content://a/1", null, null))
        assertNull(openRequestUri(null, "content://a/1", null, null))
        assertNull(openRequestUri(view, "", null, null))
        assertNull(openRequestUri(send, null, " ", null))
    }

    @Test
    fun `title drops only the last extension when a stem remains`() {
        assertEquals("song", titleFromFileName("song.mp3"))
        assertEquals("a.b.c", titleFromFileName("a.b.c.flac"))
        assertEquals(".hidden", titleFromFileName(".hidden"))
        assertEquals("noext", titleFromFileName("noext"))
        assertEquals("song", titleFromFileName("  song.mp3  "))
    }

    @Test
    fun `blank or numeric names give the unknown title`() {
        assertEquals(UNKNOWN_TITLE, titleFromFileName(null))
        assertEquals(UNKNOWN_TITLE, titleFromFileName("   "))
        assertEquals(UNKNOWN_TITLE, titleFromFileName("42"))
    }

    @Test
    fun `synthetic ids are at most -2, stable and distinct`() {
        val a = syntheticSongId("content://x/a")
        val b = syntheticSongId("content://x/b")
        assertTrue(a <= -2L)
        assertTrue(b <= -2L)
        assertEquals(a, syntheticSongId("content://x/a"))
        assertNotEquals(a, b)
    }

    @Test
    fun `opened song normalises the artist and has no folder`() {
        val song = openedSong("file:///sdcard/Music/tone.wav", "tone", artist = "<unknown>")
        assertEquals(syntheticSongId("file:///sdcard/Music/tone.wav"), song.id)
        assertEquals("tone", song.title)
        assertNull(song.artist)
        assertEquals(0L, song.durationMs)
        assertEquals("", song.folderId)
        assertEquals("", song.folderName)
    }
}
