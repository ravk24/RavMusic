package com.ravk24.ravmusic.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPathTest {

    @Test
    fun `nested path uses parent directory name and MediaStore-style hash`() {
        val ref = folderFromPath("/storage/emulated/0/Music/Rock/Beta Song.wav")
        assertEquals("Rock", ref.name)
        assertEquals("/storage/emulated/0/music/rock".hashCode().toString(), ref.id)
    }

    @Test
    fun `id is case-insensitive on the parent path`() {
        val a = folderFromPath("/storage/emulated/0/MUSIC/a.mp3")
        val b = folderFromPath("/storage/emulated/0/music/b.mp3")
        assertEquals(a.id, b.id)
    }

    @Test
    fun `root-level file uses the root directory name`() {
        val ref = folderFromPath("/storage/emulated/0/song.mp3")
        assertEquals("0", ref.name)
        assertEquals("/storage/emulated/0".hashCode().toString(), ref.id)
    }

    @Test
    fun `path without a directory is unknown`() {
        assertEquals(FolderRef("", UNKNOWN_FOLDER_NAME), folderFromPath("song.mp3"))
        assertEquals(FolderRef("", UNKNOWN_FOLDER_NAME), folderFromPath(""))
        assertEquals(FolderRef("", UNKNOWN_FOLDER_NAME), folderFromPath("/song.mp3"))
    }
}
