package com.ravk24.ravmusic.ui

import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.model.LibrarySnapshot
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.buildLibrarySnapshot
import com.ravk24.ravmusic.data.repo.LibraryState

/** Fixture builders for Compose tests that need a library without touching MediaStore. */
object FakeLibrary {

    fun song(
        id: Long,
        title: String,
        folderId: String,
        folderName: String,
        artist: String? = null,
        durationMs: Long = 221_000L,
    ) = Song(
        id = id,
        uri = "content://media/external/audio/media/$id",
        title = title,
        artist = artist,
        durationMs = durationMs,
        folderId = folderId,
        folderName = folderName,
    )

    /** Download (1), Music (2), Rock (1) — the spec's "Folder rows" scenario. */
    fun snapshot(minDurationMs: Long = MIN_SONG_DURATION_MS): LibrarySnapshot = buildLibrarySnapshot(
        listOf(
            song(1, "alpha song", "music", "Music", durationMs = 35_000L),
            song(2, "Glass Rain", "music", "Music", artist = "Hyaline"),
            song(3, "Beta Song", "rock", "Rock", durationMs = 41_000L),
            song(4, "gamma", "download", "Download", artist = "Nocturne Ave", durationMs = 3_725_000L),
        ),
        scannedAt = 1L,
        minDurationMs = minDurationMs,
    )

    /** Many folders, one song each, so a list actually scrolls. */
    fun manyFolders(count: Int): LibrarySnapshot = buildLibrarySnapshot(
        (1..count).map { i -> song(i.toLong(), "Song $i", "f$i", "Folder %02d".format(i)) },
        scannedAt = 1L,
    )

    fun loaded(snapshot: LibrarySnapshot = snapshot(), refreshing: Boolean = false): LibraryState =
        LibraryState.Loaded(snapshot, refreshing)
}
