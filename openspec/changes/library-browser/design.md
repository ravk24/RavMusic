## Context

See proposal.md — Why. Current state: the skeleton shell (`AppNavigation`, `AudioPermissionGate`, `AppViewModel` for permission state, empty `AppContainer`) is in place and archived as `app-shell` / `theme` / `audio-permission`. The Folders tab is `PlaceholderList`. No `data/` package exists. Requirements are in `specs/folder-browser/spec.md` and the `app-shell` delta.

Constraints that shape the approach: decision D-05 (no cached library — live MediaStore queries), D-11 (manual DI through `AppContainer`), D-12 (no icon artifacts), minSdk 26 while MediaStore's audio bucket columns only exist from API 29, and Navigation 3's entry lifecycle (a tab's entry is popped on every tab switch).

Verified before design: `BUCKET_ID`/`BUCKET_DISPLAY_NAME` are `since=29` in the SDK `api-versions.xml` and `content query` on an API 26 emulator fails with "no such column"; Material 3 1.4.0's `PullToRefreshBox` is stable (no opt-in); the coroutines version resolved by Compose 1.12 / lifecycle 2.11 is 1.9.0.

## Goals / Non-Goals

**Goals:**
- A pure-Kotlin library model (`Song`, `Folder`, `LibrarySnapshot`) and repository that are JVM-testable without Android.
- One place that owns "the last query result", surviving tab switches and rotation, cleared when the permission goes away.
- Screens that receive state as values and emit lambdas — the same seam the shell already uses for permission state — so the playback change can add `onSongClick` without restructuring.
- Identical folder identity on API 26–28 and 29+.

**Non-Goals:**
- Persisting the library (Room) or observing MediaStore changes automatically.
- Paging: a `LazyColumn` over an in-memory list is enough for a personal library (thousands of rows are fine).
- Error UI for query failures; embedded album art; playback of any kind.

## Decisions

### D1. Library snapshot lives in an app-scoped repository; the ViewModel is an Activity-scoped bridge
`LibraryRepository` (constructed once in `AppContainer`) holds `StateFlow<LibraryState>` with `Idle`, `Loading` (first load) and `Loaded(snapshot, refreshing)`. `LibraryViewModel` is created in `AppRoot` with `viewModelFactory` and only forwards `ensureLoaded` / `refresh` / `clear`; `AppNavigation` receives `LibraryState` as a value plus an `onRefreshLibrary` lambda.
*Why:* the Folders Nav3 entry is popped whenever the user switches tabs, so an entry-scoped ViewModel would re-query on every tab switch. Holding the snapshot in the repository also keeps "Rescan is just a re-query" (D-05) honest — there is nothing else to invalidate.
*Alternatives:* entry-scoped ViewModel via `rememberViewModelStoreNavEntryDecorator` — dies on tab switch; a Room cache — rejected by D-05.

### D2. Folder identity: MediaStore bucket on API 29+, the same formula applied to the path below
On 29+ the scanner reads `BUCKET_ID` / `BUCKET_DISPLAY_NAME`. On 26–28 it reads the deprecated `DATA` column and `folderFromPath` computes `id = parentPath.lowercase().hashCode().toString()`, `name = parent directory name` — the rule MediaStore itself uses to compute `bucket_id`. Folder ids are therefore deterministic across scans and safe to embed in the saved `FolderDetail` navigation key.
*Alternatives:* always reading `DATA` — deprecated and unreliable on scoped storage; using the full parent path as the id on both levels — would need `DATA` on 29+.

### D3. `Song.uri` is a `String`
The content URI (`ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)`) is stored as a string. `android.net.Uri` is a stub on the JVM, which would poison every pure test that constructs a `Song`; the playlists phase stores the same string in Room, and the playback phase parses it at the player boundary.

### D4. Short-audio filter in the SQL selection
`DURATION >= ?` with `MIN_SONG_DURATION_MS = 30_000L` (a constant in `data/mediastore/`) is part of the query alongside `IS_MUSIC != 0`; the footer and empty-state copy derive from the constant. The Settings change turns the constant into a scanner parameter.
*Alternatives:* filtering in memory — wastes cursor rows and makes the query result disagree with what is shown.

### D5. No error state in v1
A null cursor or `SecurityException` is logged by the scanner and yields an empty list, i.e. the empty-library state. The only realistic failure is a missing permission, which the gate already covers before a query can run.

### D6. Pull-to-refresh via Material 3 `PullToRefreshBox`; Rescan button on the empty state
`PullToRefreshBox(isRefreshing = state.refreshing, onRefresh)` wraps the folder `LazyColumn`. The empty state is not scrollable, so it cannot trigger a pull; it offers an explicit "Rescan" button instead.

### D7. Shared `EmptyState` composable
The visual of `NoMusicFoundScreen` (icon badge, title, body, button, hint) is extracted into `ui/components/EmptyState.kt`. `NoMusicFoundScreen` becomes two `EmptyState` calls with its public signature, strings and test tags unchanged, so the archived `audio-permission` tests and scenarios are untouched.

### D8. Folder detail is a pushed route, gated like the tabs
`FolderDetail(folderId, name)` is a `@Serializable` `NavKey` pushed onto `[Playlists, Folders]`; it is not a tab route, so the shell hides the bottom bar exactly as it does for `Settings`. Its entry is wrapped in `AudioPermissionGate` so revocation detected on resume replaces the song list. Songs are looked up from the current snapshot by folder id (a folder that disappears after a rescan shows an empty list rather than crashing).

### D9. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  LibraryViewModel.kt
  data/model/{Song,Folder,LibrarySnapshot}.kt      pure
  data/mediastore/{MediaScanner,FolderPath}.kt      pure (interface + constant, path rule)
  data/mediastore/MediaStoreScanner.kt              Android (ContentResolver)
  data/repo/{LibraryState,LibraryRepository}.kt     pure Kotlin + coroutines
  ui/components/{EmptyState,SongRow,Format}.kt
  ui/folders/{FoldersScreen,FolderRow,FolderDetailScreen}.kt
```

## Risks / Trade-offs

- [OEM media scanners flag some music as `IS_MUSIC = 0` (e.g. files in odd directories)] → accepted for v1; the manual walkthrough checks `/Music`, `/Download` and a nested folder. The Settings change can add an "include all audio" toggle if it bites.
- [`DURATION` is `MediaColumns.DURATION` since 29 in the SDK stubs although the column has existed since API 1] → lint `InlinedApi` warning; suppressed with a comment at the projection.
- [Rows with a NULL duration are excluded by `DURATION >= ?`] → acceptable; such rows are unplayable metadata stubs.
- [Large libraries] → query on `Dispatchers.IO`, grouping done once per scan, `LazyColumn` with stable keys.
- [Compose pull-to-refresh gesture in instrumented tests can be flaky] → the deterministic test uses the Rescan button; the swipe test asserts `onRefresh >= 1` and is kept small.

## Migration Plan

No persisted data, no manifest changes. Rollback is `git revert`. Developer step: none beyond installing the new build.
