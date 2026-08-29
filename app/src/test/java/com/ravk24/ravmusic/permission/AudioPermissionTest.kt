package com.ravk24.ravmusic.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionTest {

    @Test
    fun `api 26 uses legacy storage permission`() {
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", audioPermissionFor(26))
    }

    @Test
    fun `api 32 still uses legacy storage permission`() {
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", audioPermissionFor(32))
    }

    @Test
    fun `api 33 uses READ_MEDIA_AUDIO`() {
        assertEquals("android.permission.READ_MEDIA_AUDIO", audioPermissionFor(33))
    }

    @Test
    fun `api 37 uses READ_MEDIA_AUDIO`() {
        assertEquals("android.permission.READ_MEDIA_AUDIO", audioPermissionFor(37))
    }
}
