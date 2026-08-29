package com.ravk24.ravmusic.data.model

/** A storage directory that contains at least one song. */
data class Folder(
    val id: String,
    val name: String,
    val songCount: Int,
)
