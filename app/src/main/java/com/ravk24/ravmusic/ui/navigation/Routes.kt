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

/** Keys whose screens show the bottom navigation bar. */
val TabRoutes: Set<NavKey> = setOf(Playlists, Folders)
