# Decisions log

Settled decisions, newest last. Each records *what*, *why*, and what it rules out. If a later phase
needs to revisit one, add a new entry rather than editing history.

## 2026-08-29 — Explore session (spec vs. mockups reconciliation)

The spec (`music-player-spec.md`) and the design canvas (`Music Player Screens.html`, artboards 1a–1k)
disagreed or were silent in six places. Resolved as follows.

### D-01 Fixed brand palette, not Material You
- **Decision:** Blurple `#635BFF` single accent, navy `#0A2540` text/dark background, `#0C2E4E` dark
  surfaces, `#80E9FF` dark secondary accent. Dynamic (wallpaper) colour is never applied.
- **Why:** The mockups are designed around this palette and the "gradient as album art" idea only
  harmonises against a fixed palette. Spec F8's "dynamic color" line is superseded.
- **Rules out:** `dynamicLightColorScheme` / `dynamicDarkColorScheme`.

### D-02 Gradient art now, embedded album art later
- **Decision:** Every playlist/song gets a deterministic gradient slice; no `MediaMetadataRetriever` in v1.
- **Why:** Matches the mockups 1:1, zero decode/cache/memory work, keeps the app light. Embedded-art
  extraction is a candidate for a later phase.

### D-03 Settings entry via overflow menu
- **Decision:** `⋮` on the Playlists header → Settings. Bottom nav stays at exactly two tabs.
- **Why:** Neither spec nor mockups showed an entry point; overflow preserves the 2-tab purity.

### D-04 Playlists home is a grid
- **Decision:** Artboard 1a (grid); 1b (list) dropped.
- **Why:** Shows the gradient art better; a personal library will not outgrow a grid.

### D-05 No cached library — query MediaStore live
- **Decision:** No Room table mirroring MediaStore. "Rescan" is simply a re-query.
- **Why:** MediaStore queries are instant; a cache adds a sync problem for no user benefit. The
  "Last scan: …" line in mockup 1g is therefore just the last query time, not a crawl.

### D-06 minSdk 26, built-in Kotlin, Compose compiler plugin
- **Decision:** `minSdk = 26`; AGP 9's built-in Kotlin (no `org.jetbrains.kotlin.android` plugin);
  `org.jetbrains.kotlin.plugin.compose` at the exact bundled Kotlin version (2.2.10 for AGP 9.3.2).
- **Why:** Spec says 26; AGP 9 makes the legacy Kotlin plugin redundant; the Compose compiler must
  match the Kotlin compiler or the build fails.

## 2026-08-29 — Skeleton design (`openspec/changes/app-skeleton/design.md`)

### D-07 Package `com.ravk24.ravmusic`
- **Decision:** Namespace and `applicationId` renamed from the template's `com.example.ravmusic`.
- **Why:** Done before the first Kotlin file existed, so nothing had to move later.

### D-08 Navigation 3
- **Decision:** `androidx.navigation3` (1.1.7) with `@Serializable` `NavKey` routes, one back stack;
  tabs are `[Playlists]` / `[Playlists, Folders]`, Settings pushed on top.
- **Why:** Navigation 2.x is officially in maintenance mode. The stack model gives the spec'd back
  behaviour (Folders → Playlists → exit) for free.
- **Consequence:** kotlinx.serialization plugin (2.2.10) + runtime 1.9.0 (newest built for Kotlin 2.2).

### D-09 Per-tab scroll state hoisted in the shell
- **Decision:** `LazyListState` per tab lives in `AppNavigation` via `rememberSaveable`, not in the
  Nav3 entry.
- **Why:** Nav3's saveable-state decorator discards an entry's state when it is popped, and switching
  tabs pops. Hoisting keeps each tab exactly as it was left, across switches and rotation.

### D-10 Permission state is a value in `AppViewModel`
- **Decision:** `PermissionState { Unknown, Granted, Denied(canRequest) }` as a `StateFlow`; the
  Activity supplies only side-effects (dialog launcher, settings intent, resume re-check).
  "Permanently denied" is inferred only after at least one request has been made.
- **Why:** JVM-testable without a device; a fresh install also reports "no rationale", so the
  request-count guard prevents misclassifying it as permanent denial.

### D-11 Manual DI
- **Decision:** `RavMusicApp.container: AppContainer`, constructor injection, no Hilt/Koin.
- **Why:** < 10 injectables for a one-person app; Hilt would add KSP to every build.

### D-12 No `material-icons-extended`
- **Decision:** Use `material-icons-core` (transitive via Material 3) plus hand-built `ImageVector`s
  from the mockup path data for Folder / QueueMusic / MusicNote.
- **Why:** icons-extended is no longer BOM-managed and adds ~10 MB unshrunk — contradicts "lightweight".

### D-13 Theme override is a parameter, not a feature (yet)
- **Decision:** `RavMusicTheme(darkTheme: Boolean = isSystemInDarkTheme())`. Skeleton follows the
  system only; the Settings phase will persist a `ThemeMode` and pass it in at the one call site.

### D-14 App icon: adaptive icon from `RavMusic-icon.png`
- **Decision:** The supplied artwork (dark rounded square, gold ring + waveform bars, cream note) is the
  launcher icon, delivered as an adaptive icon: solid `#202021` background layer, a transparent
  foreground holding only the artwork scaled into the 66 dp safe zone, and a white-silhouette
  monochrome layer for Android 13+ themed icons. Legacy 48 dp WebPs are regenerated from the full
  square for completeness (unused on minSdk 26). Generated by a Pillow script, not hand-edited.
- **Why:** Pasting the full image as the foreground would let launcher masks clip the gold ring; the
  layered form survives every mask shape and themed-icon tinting. Pulled forward from Phase 7 at the
  user's request (2026-08-29).

## 2026-08-29 — Library design (`openspec/changes/library-browser/design.md`)

### D-15 Library snapshot lives in the app-scoped repository; `LibraryViewModel` is Activity-scoped
- **Decision:** `LibraryRepository` (one instance in `AppContainer`) owns `StateFlow<LibraryState>`
  (`Idle` / `Loading` / `Loaded(snapshot, refreshing)`); `LibraryViewModel` is created in `AppRoot` and only
  forwards `ensureLoaded` / `refresh` / `clear`; screens receive `LibraryState` as a value.
- **Why:** The Folders Nav3 entry is popped on every tab switch, so an entry-scoped ViewModel would re-query
  each time. Same seam as `permissionState`.
- **Rules out:** per-screen ViewModels holding library data.

### D-16 Folder identity = MediaStore bucket on API 29+, the same formula on the path below
- **Decision:** On 29+ read `BUCKET_ID` / `BUCKET_DISPLAY_NAME`; on 26–28 read `DATA` and compute
  `id = parentPath.lowercase().hashCode()`, `name = parent directory name` (`folderFromPath`).
- **Why:** Audio rows have no bucket columns before 29 (verified in the SDK `api-versions.xml` and on the
  API 26 emulator). The formula reproduces MediaStore's real `bucket_id` values exactly (checked against
  `content query` on API 36: 82896267 for `/storage/emulated/0/Music`), so ids are deterministic and safe
  inside the saved `FolderDetail` navigation key.

### D-17 `Song.uri` is a `String`
- **Decision:** The content URI (`ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)`) is stored as text.
- **Why:** `android.net.Uri` is a stub on the JVM and would poison every pure test; Room stores the same
  string in the playlists phase; playback parses it at the player boundary.

### D-18 Short-audio filter lives in the SQL selection
- **Decision:** `IS_MUSIC != 0 AND DURATION >= ?` with `MIN_SONG_DURATION_MS = 30_000L` in
  `data/mediastore/MediaScanner.kt`; footer and empty-state copy derive from the constant.
- **Why:** Cheaper than filtering in memory and the query result equals what is shown. The Settings phase
  turns the constant into a scanner parameter.

### D-19 No error state in v1
- **Decision:** A null cursor or `SecurityException` is logged and yields an empty list (the empty-library
  state). `LibraryState` has no `Error`.
- **Why:** The only realistic failure is a missing permission, which the gate handles before a query runs.

### D-20 Pull-to-refresh via Material 3 `PullToRefreshBox`; Rescan button on the empty state
- **Decision:** `PullToRefreshBox` (stable in Material 3 1.4.0, no opt-in) wraps the folder list. The empty
  state cannot be pulled (nothing scrolls), so it has an explicit "Rescan" button. No `ContentObserver` and
  no re-query on resume.

### D-21 Shared `EmptyState` composable
- **Decision:** `ui/components/EmptyState.kt` holds the artboard-1h layout; `NoMusicFoundScreen` and the
  empty library both use it. `NoMusicFoundScreen` kept its public API, strings and test tags.

### D-22 Detail routes are gated and hide the bottom bar
- **Decision:** `FolderDetail(folderId, name)` is a `@Serializable` `NavKey` pushed onto `[Playlists, Folders]`,
  not in `TabRoutes` (so the bar hides, like `Settings`), and wrapped in `AudioPermissionGate` so a revoke
  detected on resume replaces the song list. Songs are looked up from the snapshot by id.

### D-23 `kotlinx-coroutines-test` pinned to 1.9.0
- **Decision:** Matches the coroutines version Compose 1.12 / lifecycle 2.11 resolve (checked with
  `:app:dependencies`). Test-only dependency.

## 2026-08-29 — Playback design (`openspec/changes/playback-core/design.md`)

### D-24 Media3 1.11.0 `MediaSessionService` + ExoPlayer own playback
- **Decision:** `playback/PlaybackService` creates one ExoPlayer (music audio attributes, audio focus,
  handle-becoming-noisy, local wake mode) inside a `MediaSession` whose session activity opens
  `MainActivity`. The default notification provider supplies notification, lock-screen and headset handling.
- **Why:** Spec F4 asks for exactly this; the legacy `MediaSessionCompat` stack is what Media3 replaces.
- **Rules out:** `MediaPlayer` in the Activity, hand-rolled notifications.

### D-25 `PlayerConnection` bridge behind an Activity-scoped `PlayerViewModel`; `PlayerState` is a value
- **Decision:** One `MediaController` per process (`AppContainer.playerConnection`), mirrored into
  `StateFlow<PlayerState>` (`NowPlaying?`, `isPlaying`, position, duration); `PlayerViewModel` connects in
  `init`, releases in `onCleared`, and runs a 500 ms position ticker only while playing. Title, artist and
  origin travel in each `MediaItem`'s metadata so the UI can rebuild `NowPlaying` after reconnecting.
- **Why:** Same seam as permission and library state; survives tab switches and rotation; `nowPlaying == null`
  is the single rule for hiding the mini player.

### D-26 Missing files are skipped at playback time, not pre-filtered
- **Decision:** `onPlayerError` on the service side seeks to the next item (or stops and clears at the end of
  the queue). No file-descriptor check when the queue is built.
- **Why:** Opening every file of a large folder on tap is slow and still races with deletion; ExoPlayer already
  reports the failure. Spec F1's "skip on playback" is exactly this. The playlists change adds the greyed rows.

### D-27 Mini player docks in the Scaffold `bottomBar` as a column above the navigation bar
- **Decision:** `AppNavigation`'s `bottomBar` is `Column { MiniPlayer?; NavigationBar? }`; on routes without
  the bar the column takes the navigation-bar inset itself. Present on tabs, `FolderDetail` and `Settings`.
- **Why:** Content padding grows automatically, no per-screen bottom padding, and Now Playing can later expand
  from the same slot.
- **Rules out:** an overlay `Box` above the content.

### D-28 Gradient art helper and current-row highlight
- **Decision:** `ui/components/GradientArt.kt` maps a seed (song or playlist id) to one of the six mockup
  gradient pairs (`artGradientIndex`, pure) — the D-02 "gradient as album art" in code. `SongRow` gains
  `isCurrent` (title in the primary colour, `selected` semantics).

### D-29 Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`; no `POST_NOTIFICATIONS`
- **Decision:** All three are install-time permissions; the service declares
  `foregroundServiceType="mediaPlayback"`. Media-session notifications are exempt from the notification
  runtime permission, so `audio-permission`'s "exactly one runtime permission" still holds.

## 2026-08-29 — Playlists design (`openspec/changes/playlists/design.md`)

### D-30 Room 2.8.4 via KSP 2.3.11; schema exported and committed
- **Decision:** `androidx.room` Gradle plugin with `schemaDirectory("$projectDir/schemas")`, `room-runtime` +
  `ksp(room-compiler)`, database version 1 with `exportSchema = true` (`app/schemas/…/1.json` is in git).
- **Why:** kapt is incompatible with AGP's built-in Kotlin; KSP ≥ 2.3.0 is independent of the Kotlin version
  and was verified to build with Kotlin 2.2.10. Any later schema change needs a migration plus a
  `MigrationTestHelper` test against the committed JSON.
- **Rules out:** hand-written SQLite, SQLDelight.

### D-31 Playlists snapshot title/artist/duration; the library is still never cached
- **Decision:** `PlaylistTrackEntity` stores `mediaStoreUri` + title, artist, duration and a position
  (spec F3's model). Playlists render without MediaStore; D-05 is untouched because only user data persists.

### D-32 Missing = "not in the loaded library snapshot"
- **Decision:** `missingTrackIds(tracks, libraryState)` flags a track when the library is `Loaded` and its
  URI is absent; nothing is flagged while `Idle`/`Loading`. Missing tracks are greyed, filtered out of the
  queue, and removed by "Clean up".
- **Why:** Instant and consistent with the live query; the service's error-skipper (D-26) covers races.

### D-33 `PlaylistsHost` interface, Activity-scoped `PlaylistsViewModel`
- **Decision:** `AppNavigation` takes one `playlists: PlaylistsHost` instead of a dozen lambdas; the
  ViewModel implements it over `PlaylistStore` (Room) and tests use an in-memory fake or the real ViewModel
  on `Room.inMemoryDatabaseBuilder`. Track flows are `stateIn`-cached per playlist id.

### D-34 Shuffle is a flag on the existing play path
- **Decision:** `PlayerBridge.play(plan, shuffle)` sets `shuffleModeEnabled` before `setMediaItems`;
  `PlayerViewModel.shufflePlay` picks a random start index. Folder taps and playlist Play pass `false`, so a
  previous Shuffle play never leaks into the next queue. `PlaylistTrack.toSong()` reuses `planQueue`.

### D-35 Hand-rolled drag reorder + Material 3 swipe remove, both in `ReorderableList.kt`
- **Decision:** `detectDragGesturesAfterLongPress` on the handle only, target index from
  `LazyListState.layoutInfo`, `onMove(from, to)` on release; `SwipeToDismissBox` end-to-start removes.
- **Why:** No third-party reorder library (spec: lightweight); the phase page asked for gesture code in one place.

### D-36 Selection state is `rememberSaveable` in the folder detail; no per-row overflow menu
- **Decision:** `selectedIds` survives scrolling and rotation and dies with the Nav3 entry (spec F2: "survives
  scrolling but not navigation away"). Long-pressing one song is the "add a single song" path, so the spec's
  optional per-row overflow menu is not built.

## 2026-08-29 — Now Playing design (`openspec/changes/now-playing/design.md`)

### D-37 Now Playing is a Nav3 route
- **Decision:** `NowPlaying` is a `NavKey` pushed above whatever is showing; the shell renders neither the
  navigation bar nor the mini player while it is on top, and pops it itself when the queue disappears.
- **Why:** Back-stack semantics for free; Phase 7 animates the same route.
- **Rules out:** a modal sheet for the player.

### D-38 Play order comes from a pure timeline walk on `PlayerState`
- **Decision:** `playOrder(count, currentIndex, first, next)` walks Media3's `getFirstWindowIndex` /
  `getNextWindowIndex(…, REPEAT_MODE_OFF, shuffle)`; `PlayerState.queue` is that order as `QueueEntry`s with
  `queueIndex`, `remaining`, `hasNext`, `hasPrevious`. `RepeatMode` is an app enum mapped at the controller.

### D-39 Reordering the queue under shuffle freezes the shown order and turns shuffle off
- **Decision:** Shuffle off → `moveMediaItem`; shuffle on → `setMediaItems(shownOrder, current, position)`,
  shuffle off, then the move. The `MediaController` has no API to set a custom shuffle order, so this is the
  only way the drag can mean what the user sees. The brief re-prepare of the current song is accepted and
  written into the `shuffle-repeat` spec.

### D-40 Position ticker: 500 ms app-wide, 250 ms only while Now Playing is visible
- **Decision:** The ViewModel's ticker serves the mini player; `NowPlayingScreen` runs its own
  `LaunchedEffect` loop while playing and visible, calling `refreshPosition()`.

### D-41 `PlayerActions` bundle and a generic `ReorderableList`
- **Decision:** All player commands travel as one `PlayerActions` value built by `PlayerViewModel.actions()`
  (tests use `PlayerActions.none().copy(…)`); the playlist drag reorder became `ReorderableList<T>` so the
  queue sheet reuses the same gesture code.

### D-42 Commands issued before the controller connects are queued, not last-one-wins
- **Decision:** `PlayerConnection` keeps an ordered queue of pending commands and replays all of them on
  connect (refines D-25, which kept only one).
- **Why:** `play` immediately followed by `setRepeat` (a real sequence from Now Playing after a cold start)
  dropped the `play`; the instrumented `PlaybackServiceTest` caught it.
