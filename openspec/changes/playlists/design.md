## Context

See proposal.md — Why. Current state after `playback-core`: the library is a live MediaStore snapshot (`LibrarySnapshot`, `Song(uri: String, …)`), playback is a Media3 session behind `PlayerViewModel.playSongs(songs, index, origin)`, the shell passes state as values from Activity-scoped ViewModels into `AppNavigation`, `FolderDetailScreen` renders `SongRow(isCurrent)`, and `artGradient(seed)` exists. Nothing is persisted; the Playlists tab is `PlaceholderList`. Requirements are in `specs/playlists/spec.md` and `specs/multi-select/spec.md`.

Constraints: AGP 9 built-in Kotlin (kapt incompatible → KSP), minSdk 26, manual DI, no `INTERNET`, D-05 (no library cache — but user-created playlists are exactly the data that must persist), mockups 1a / 1d (selection mode) / 1e.

Verified: Room 2.8.4 is current stable; KSP 2.3.11 is the latest and is Kotlin-version-independent (2.2.10-2.0.2 is the exact-Kotlin fallback).

## Goals / Non-Goals

**Goals:**
- Persisted playlists with a committed, versioned schema so later migrations are possible.
- Every non-trivial rule pure and JVM-tested: duplicate partition, reorder move, missing detection, totals formatting, queue start index.
- One interface between the shell and playlist behaviour (`PlaylistsHost`) so the acceptance test can run the real ViewModel over an in-memory database.
- Gesture code (drag reorder, swipe remove) confined to one composable.

**Non-Goals:**
- Drag-select ranges, a per-row overflow menu, importing/exporting playlists, syncing playlist metadata with MediaStore after tag edits.

## Decisions

### D1. Room 2.8.4 via KSP 2.3.11, schema exported to `app/schemas/`
`androidx.room` Gradle plugin with `schemaDirectory("$projectDir/schemas")`, `room-runtime` + `ksp(room-compiler)`, database version 1 with `exportSchema = true`. KSP is the processor path AGP's built-in Kotlin supports; 2.3.x no longer needs to match the Kotlin version. If 2.3.11 refuses to apply, pin `2.2.10-2.0.2` and note it in `Build-plan/decisions.md`.
*Alternatives:* `androidx.sqlite` with hand-written SQL — no processor, but loses compile-time query checks and Flow support for a few dozen lines saved; SQLDelight — another plugin and a new query language.

### D2. Two entities, tracks snapshot metadata, cascade delete
`PlaylistEntity(id, name, createdAt, sortOrder)` and `PlaylistTrackEntity(id, playlistId FK cascade, mediaStoreUri, title, artist, durationMs, position)` with an index on `(playlistId, position)`. Spec F3's model verbatim; `sortOrder` is stored now (creation order) so a manual playlist order can be added without a migration.

### D3. Missing = "not in the loaded library snapshot"
A track is missing when `LibraryState` is `Loaded` and its URI is absent from `snapshot.songs`. Pure, instant, and consistent with D-05 (the live query is the truth). Nothing is flagged while the library is `Idle`/`Loading`, so a fresh launch never shows false greys. Playback filters missing tracks out of the queue; the service's error-skipper (D-26) still catches races.
*Alternatives:* opening each file — slow on tap and racy; MediaStore `_ID IN (…)` — stale until rescan anyway.

### D4. `PlaylistsHost` interface implemented by an Activity-scoped `PlaylistsViewModel`
`playlists: StateFlow<List<Playlist>>`, `tracks(playlistId): StateFlow<List<PlaylistTrack>>` (per-id `stateIn` cache), `create`, `rename`, `delete`, `duplicateCount`, `addSongs(playlistId, songs, skipDuplicates)`, `removeTrack`, `move`, `cleanUp`. `AppNavigation` takes the host as one parameter instead of a dozen lambdas; tests pass a fake or the real ViewModel over `Room.inMemoryDatabaseBuilder`.

### D5. Shuffle is a flag on the existing play path
`PlayerBridge.play(plan, shuffle)` sets `shuffleModeEnabled` on the controller before `setMediaItems`; `PlayerViewModel.shufflePlay(songs, origin)` picks a random start index. `Play` and row taps pass `shuffle = false` so a previous Shuffle play does not leak into a folder queue. `PlaylistTrack.toSong()` (id parsed from the URI's last path segment, folder fields blank) reuses `planQueue` and `toMediaItems` unchanged, so `NowPlaying.songId` matches `PlaylistTrack` rows for highlighting.

### D6. Hand-rolled drag reorder, Material 3 swipe remove, both in `ReorderableList.kt`
`detectDragGesturesAfterLongPress` on the handle only (so row taps still play and swipes still remove), the dragged row translated by the accumulated offset, the target index computed from `LazyListState.layoutInfo` item bounds, `onMove(from, to)` on release. `SwipeToDismissBox` (end-to-start) removes. No third-party reorder library (spec: lightweight; phase page: keep gesture code in one composable).

### D7. Selection state lives in `FolderDetailScreen` with `rememberSaveable`
`selectedIds: Set<Long>` survives rotation and scrolling; leaving the screen pops the Nav3 entry, which drops it — exactly the spec's "survives scrolling, not navigation". `BackHandler(enabled = selecting)` clears it. The contextual bar is the same `TopAppBar` slot with navy container / white content (mockup 1d).

### D8. Add-to-playlist flow is a small state machine in the screen
`Idle → SheetOpen → NamingNew | DuplicatePrompt(playlist, dupes) → Done(snackbar)`. `duplicateCount` is asked before adding; the prompt is an `AlertDialog` with "Add anyway" / "Skip duplicates". Dialogs, the sheet and the name dialog live in `ui/playlists/PlaylistDialogs.kt` and are reused by the home FAB and the detail overflow (Rename/Delete).

### D9. Playlist detail is a pushed route; mini player stays docked
`PlaylistDetail(playlistId)` is a `@Serializable` `NavKey` above `[Playlists]`; the existing rule hides the bottom bar and keeps the mini player (D-27). Track rows come from `host.tracks(id)` collected in the entry; missing ids are computed there from `libraryState`.

### D10. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  PlaylistsViewModel.kt
  data/db/{PlaylistEntity,PlaylistTrackEntity,PlaylistDao,RavMusicDatabase}.kt
  data/model/PlaylistModels.kt              Playlist, PlaylistTrack, toSong, missing rule, partition, move (pure)
  data/repo/PlaylistRepository.kt
  playback/PlayerBridge.kt (+shuffle)       PlayerConnection, PlayerViewModel updated
  ui/playlists/{PlaylistsScreen,PlaylistCard,PlaylistDetailScreen,ReorderableList,PlaylistDialogs}.kt
  ui/folders/FolderDetailScreen.kt          selection mode
  ui/components/{AppIcons,Format}.kt        icons, formatTotalDuration
app/schemas/com.ravk24.ravmusic.data.db.RavMusicDatabase/1.json
```

## Risks / Trade-offs

- [KSP + built-in Kotlin combination is new] → verified by the first build; fallback pin documented (D1).
- [Drag reorder inside a `LazyColumn` fights item recycling] → the dragged item is keyed and kept in composition; indices are recomputed from `layoutInfo` on every drag delta; the acceptance for reorder is a persisted-order check, not pixel-perfect animation.
- [`SwipeToDismissBox` and long-press drag on the same row] → drag starts only on the handle; the row body owns tap/long-press; swipe is horizontal on the whole row and the handle consumes vertical drags only.
- [Room schema is the first persisted data] → version 1 exported and committed; any later change needs a migration and a `MigrationTestHelper` test.
- [Snackbars inside a pushed route] → the folder detail has its own `Scaffold`, so its `SnackbarHost` is local; the home and detail use the same pattern.

## Migration Plan

Fresh install creates the database. Updating from a Phase 3 build creates it on first use (no prior data). Rollback: `git revert`; the `ravmusic.db` file is ignored by an older build.

## Open Questions

- Whether "sortOrder" should become user-controlled (drag the home grid) — deferred; stored but not editable in this change.
