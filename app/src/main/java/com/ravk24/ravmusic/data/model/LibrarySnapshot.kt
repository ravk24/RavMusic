package com.ravk24.ravmusic.data.model

/**
 * The result of one library query: all songs, the folders they group into, and when the query
 * ran. Immutable; a refresh produces a new snapshot. Never persisted (decision D-05).
 */
class LibrarySnapshot(
    val songs: List<Song>,
    val folders: List<Folder>,
    val scannedAt: Long,
) {
    private val songsByFolder: Map<String, List<Song>> = songs.groupBy { it.folderId }

    val totalSongs: Int get() = songs.size

    /** Songs in [folderId], in library order; empty when the folder is unknown. */
    fun songsIn(folderId: String): List<Song> = songsByFolder[folderId].orEmpty()

    companion object {
        val EMPTY = LibrarySnapshot(emptyList(), emptyList(), 0L)
    }
}

private val caseInsensitive = String.CASE_INSENSITIVE_ORDER

/**
 * Pure grouping/sorting step shared by every scanner: folders sorted by name (case-insensitive,
 * then id for stability), songs sorted by title (case-insensitive, then id).
 */
fun buildLibrarySnapshot(songs: List<Song>, scannedAt: Long): LibrarySnapshot {
    val sortedSongs = songs.sortedWith(compareBy<Song, String>(caseInsensitive) { it.title }.thenBy { it.id })
    val folders = sortedSongs
        .groupBy { it.folderId }
        .map { (id, group) -> Folder(id = id, name = group.first().folderName, songCount = group.size) }
        .sortedWith(compareBy<Folder, String>(caseInsensitive) { it.name }.thenBy { it.id })
    return LibrarySnapshot(songs = sortedSongs, folders = folders, scannedAt = scannedAt)
}
