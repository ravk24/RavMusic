package com.ravk24.ravmusic.data.repo

import com.ravk24.ravmusic.data.model.LibrarySnapshot

/** What the UI knows about the library. There is deliberately no error state (design D5). */
sealed interface LibraryState {
    /** Nothing queried yet, or the permission was lost. */
    data object Idle : LibraryState

    /** First query in flight; nothing to show yet. */
    data object Loading : LibraryState

    /** Last query result; [refreshing] while a re-query is in flight behind it. */
    data class Loaded(val snapshot: LibrarySnapshot, val refreshing: Boolean = false) : LibraryState
}
