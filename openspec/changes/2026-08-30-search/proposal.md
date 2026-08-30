## Why

There is no way to find a song by name. A long playlist or folder has to be scrolled, and a song that lives in several playlists can only be found by opening each one. The user asked for "a search option for searching a song in any playlist", and chose both readings: a filter inside a playlist, and one search across all of them.

## What Changes

- **Filter inside a playlist**: a search action on the playlist detail swaps the title bar for a search bar; rows narrow as the user types, matching the title or the artist. While a filter is active reordering is off (drag handles disappear — positions in a filtered list would lie), swipe-to-remove and tap-to-play keep working, and Play / Shuffle play still play the whole playlist.
- **The same filter on folder detail**, so browsing works the same everywhere. Long-press selection, drag-select and "Select all" operate over the rows shown; the query survives a selection.
- **Search across all playlists**: a search action on the Playlists home opens a full-screen search. Each hit shows the song with "Artist · Playlist"; tapping it plays that playlist from that song, a trailing action opens the playlist. A song in two playlists is two hits. The query survives rotation and opening a playlist from a result.
- One matching rule everywhere: case-insensitive "contains" on title or artist, whitespace trimmed, blank shows everything (or, for the global search, a hint).
- Version 1.1.0 (`versionCode` 4), shipped together with `open-with`.

Explicitly not in this change: searching the whole library (folders remain the browse unit), fuzzy matching, search history, searching the queue sheet.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `playlists`: "Playlists home grid" (a Search action), "Playlist detail" (the filter), "Reorder and remove" (reordering is off while filtering); a new requirement "Search across playlists".
- `folder-browser`: "Folder detail" (the filter).
- `multi-select`: "Entering and leaving selection" ("Select all" and drag-select over the filtered rows; the query outlives the selection).
- `app-shell`: "Detail screens hide the bottom navigation" (the search route is one of them).

## Impact

- Pure Kotlin: `data/model/SongQuery.kt` (matching rule), `data/model/PlaylistSearch.kt` (hits across playlists), `planPlaylistPlay` in `PlaylistModels.kt` (the detail's play logic, now shared with search).
- Data: `PlaylistDao.observeAllTracks()` (a query only — no schema change), `PlaylistStore.allTracks`, `PlaylistsHost.allTracks`.
- UI: `ui/components/SearchTopBar.kt` (+ `SearchEmpty`), `AppIcons.Search`, `SongRow` gains `subtitle` / `trailing`, `ReorderableList` gains `enabled`, `PlaylistDetailScreen`, `FolderDetailScreen`, `PlaylistsScreen`, new `PlaylistSearchScreen`, new `Search` route in `AppNavigation`.
- Tests: JVM tests for the matching rule, search hits and the play plan; Compose tests for every screen and a navigation test for the route; DAO test for the new query. Test fakes gain `allTracks`. No new dependencies or permissions.
