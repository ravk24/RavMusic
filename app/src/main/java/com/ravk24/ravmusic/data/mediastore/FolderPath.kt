package com.ravk24.ravmusic.data.mediastore

/** A folder's identity as derived from a file path. */
data class FolderRef(val id: String, val name: String)

const val UNKNOWN_FOLDER_NAME = "Unknown folder"

/**
 * Derives the folder of a file path the same way MediaStore computes `bucket_id` /
 * `bucket_display_name` on Android 10+: the id is the hash of the lower-cased parent path, the
 * name is the parent directory's own name. Used on API 26–28, where audio rows have no bucket
 * columns, so folder ids are identical across API levels.
 */
fun folderFromPath(path: String): FolderRef {
    val slash = path.lastIndexOf('/')
    if (slash <= 0) return FolderRef(id = "", name = UNKNOWN_FOLDER_NAME)
    val parent = path.substring(0, slash)
    val name = parent.substringAfterLast('/').ifEmpty { UNKNOWN_FOLDER_NAME }
    return FolderRef(id = parent.lowercase().hashCode().toString(), name = name)
}
