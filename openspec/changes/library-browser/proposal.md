## Why

The Folders tab is still a placeholder: the app cannot show a single real song. Phase 2 of the build order ("Library") replaces that placeholder with the device's actual audio grouped by the folders it lives in (spec F1) — the browsing surface every later phase (tap-to-play, multi-select, playlists) builds on.

## What Changes

- Add a live MediaStore query for music on the device (`IS_MUSIC`, grouped by storage folder), with **no persistent cache** (decision D-05): every refresh is a fresh query, and only the result of the last query is held in memory.
- Replace the Folders placeholder with the real folder list (mockup 1c): folder name + song count, alphabetical, total song count in the header, pull-to-refresh to re-query.
- Add a folder detail screen (mockup 1d, browse mode): song title, artist (or "Unknown artist"), duration; pushed above the tabs, so the bottom bar is hidden; back returns to the folder list. Tapping a song does nothing yet — playback arrives in the playback-core change.
- Hide very short audio (under 30 s — notification sounds, voice notes) via a constant in code; a Settings control comes later.
- Show an empty-library state (same visual as the permission state, different copy, with a "Rescan" action) when the query returns nothing.
- The library follows the audio permission: it is queried once the permission is granted and cleared when it is revoked; the "No music found" permission state is also shown on the folder detail screen when the permission is missing.
- On Android 8.0–9 (API 26–28) MediaStore has no folder ("bucket") columns for audio, so the folder is derived from the file path using the same rule MediaStore applies on Android 10+.

Explicitly not in this change: playback, the mini player, multi-select, playlists, album art, Settings rows, automatic re-query when files change (ContentObserver).

## Capabilities

### New Capabilities
- `folder-browser`: the device music library grouped by folder — live query, short-audio filter, folder list, folder detail, pull-to-refresh, empty-library state, and how the library follows the audio permission.

### Modified Capabilities
- `app-shell`: the "Placeholder tab content" requirement narrows to the Playlists tab only (Folders now has real content); detail screens pushed above the tabs hide the bottom navigation bar and return to their tab on back.

## Impact

- New `data/` package (`model`, `mediastore`, `repo`) with the scanner, in-memory snapshot, and repository; `AppContainer` gains its first dependency.
- `MainActivity`/`AppRoot` hosts an Activity-scoped library ViewModel next to the permission ViewModel; `AppNavigation` gains a `FolderDetail` route and receives the library state as a value.
- `ui/folders/` rewritten (FoldersScreen, new FolderDetailScreen); shared `EmptyState`, `SongRow`, duration/count formatting and a chevron icon added to `ui/components/`; `NoMusicFoundScreen` re-implemented on `EmptyState` with an unchanged public API and test tags.
- Version catalog: `kotlinx-coroutines-test` (test-only). No new runtime dependencies, no new permissions, still no `INTERNET`.
- Existing Compose tests that construct `AppNavigation` gain two parameters; the tab-retention test scrolls a fake folder list instead of the placeholder.
