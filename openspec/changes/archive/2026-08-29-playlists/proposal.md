## Why

Folders are where songs live; playlists are what you play — and nothing in the app persists yet. Phase 4 delivers the product's reason to exist (spec F2, F3): pick many songs fast with long-press multi-select, save them as playlists that survive reboots, and play a playlist shuffled with one tap. The Playlists tab has been a placeholder since Phase 1.

## What Changes

- Add local persistence with Room: `Playlist` and `PlaylistTrack` tables. Tracks snapshot title, artist and duration so playlists render instantly without touching MediaStore (decision D-05 stays: the *library* is never cached, only what the user saved).
- Add **multi-select** to the folder detail (mockup 1d): long-press a song to enter selection mode, tap to toggle, checkboxes on every row, a contextual bar with the count, "Select all N" and close; the primary action **Add N to playlist ›** opens a sheet listing playlists plus "New playlist". Duplicates are warned about ("3 already in this playlist — Add anyway / Skip duplicates").
- Replace the Playlists placeholder with the real home (mockup 1a): a two-column grid of gradient-art cards with name and "N songs · Xh Ym", a "+" FAB that creates a playlist by name, and an empty state.
- Add the playlist detail (mockup 1e): header with art, name and totals; **Shuffle play** (enables shuffle, starts at a random track) and **Play**; rows with drag handles for reordering, swipe-to-remove, the current song highlighted; rename and delete (with confirmation) from an overflow menu.
- Missing files: a playlist track whose file is no longer in the library is shown greyed, skipped when the playlist is played, and removed by a "Clean up" action.
- `app-shell`'s "Placeholder tab content" requirement is retired; `folder-browser`'s "Folder detail" gains long-press selection. The `playback` capability is reused unchanged apart from a shuffle flag when starting a queue.

Explicitly not in this change: drag-select ranges (Phase 7 stretch goal), a per-row overflow menu (long-pressing a single song covers "add one song"), Now Playing, Settings.

## Capabilities

### New Capabilities
- `playlists`: persisted playlists — data that survives process death and reboot, create/rename/delete, the home grid, the detail screen, play and shuffle play, reorder, remove, and missing-file handling.
- `multi-select`: selecting many songs in a folder and adding them to a playlist — entering/leaving selection, toggling, select all, the add-to-playlist sheet, duplicate handling, confirmation.

### Modified Capabilities
- `app-shell`: the "Placeholder tab content" requirement is removed — the Playlists tab now shows real content.
- `folder-browser`: the "Folder detail" requirement additionally states that long-pressing a song enters selection mode (as specified by `multi-select`); every existing scenario is kept.

## Impact

- New dependencies: Room 2.8.4 (`room-runtime`, `room-compiler` via KSP, `room-testing` for tests) and the KSP Gradle plugin (KSP is the annotation-processing path AGP's built-in Kotlin supports; kapt is incompatible). Exported schema JSON is committed under `app/schemas/`.
- New `data/db/` (entities, DAO, database) and `data/repo/PlaylistRepository`; `AppContainer` builds the database.
- `PlayerBridge`/`PlayerConnection`/`PlayerViewModel` gain a shuffle flag and a `shufflePlay` entry point; missing tracks are filtered out of a playlist queue.
- New Activity-scoped `PlaylistsViewModel` behind a `PlaylistsHost` interface; `AppNavigation` gains a `PlaylistDetail` route and the host; `AppRoot` wires it.
- `ui/playlists/` (home grid, detail, reorderable list, dialogs and the add-to-playlist sheet), `ui/folders/FolderDetailScreen` selection mode, six new icons, `formatTotalDuration`. `PlaceholderList` and `UpcomingPhases` are deleted.
- No new permissions; still no `INTERNET`.
