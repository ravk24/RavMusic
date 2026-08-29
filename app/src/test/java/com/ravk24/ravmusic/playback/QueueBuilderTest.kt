package com.ravk24.ravmusic.playback

import com.ravk24.ravmusic.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueBuilderTest {

    private fun song(id: Long) = Song(id, "content://media/$id", "Song $id", null, 100_000L, "f", "Folder")
    private val songs = listOf(song(1), song(2), song(3))

    @Test
    fun `keeps folder order and start index`() {
        val plan = planQueue(songs, startIndex = 1, origin = "Rock")!!
        assertEquals(listOf(1L, 2L, 3L), plan.songs.map { it.id })
        assertEquals(1, plan.startIndex)
        assertEquals("Rock", plan.origin)
    }

    @Test
    fun `clamps an out-of-range start index`() {
        assertEquals(2, planQueue(songs, 99, "x")!!.startIndex)
        assertEquals(0, planQueue(songs, -4, "x")!!.startIndex)
    }

    @Test
    fun `empty folder gives no plan`() {
        assertNull(planQueue(emptyList(), 0, "x"))
    }

    @Test
    fun `plan is a copy of the input`() {
        val mutable = songs.toMutableList()
        val plan = planQueue(mutable, 0, "x")!!
        mutable.clear()
        assertEquals(3, plan.songs.size)
    }
}
