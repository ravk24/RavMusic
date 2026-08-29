package com.ravk24.ravmusic.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Top-level tab: playlists home. Always the root of the back stack. */
@Serializable
data object Playlists : NavKey

/** Top-level tab: folder browser. */
@Serializable
data object Folders : NavKey

/** Pushed above the tabs; hides the bottom navigation bar. */
@Serializable
data object Settings : NavKey

/**
 * A folder's song list, pushed above the Folders tab; hides the bottom navigation bar.
 * Carries the id and display name so the screen can render its header before (or without)
 * the library snapshot; songs are looked up from the snapshot by [folderId].
 */
@Serializable
data class FolderDetail(val folderId: String, val name: String) : NavKey

/** A playlist's tracks, pushed above the Playlists tab; hides the bottom navigation bar. */
@Serializable
data class PlaylistDetail(val playlistId: Long) : NavKey

/** The full-screen player, pushed above any screen; hides the bottom bar and the mini player. */
@Serializable
data object NowPlaying : NavKey

/** Keys whose screens show the bottom navigation bar. */
val TabRoutes: Set<NavKey> = setOf(Playlists, Folders)
