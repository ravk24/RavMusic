# Decisions log

Settled decisions, newest last. Each records *what*, *why*, and what it rules out. If a later phase
needs to revisit one, add a new entry rather than editing history.

## 2026-08-29 — Explore session (spec vs. mockups reconciliation)

The spec (`music-player-spec.md`) and the design canvas (`Music Player Screens.html`, artboards 1a–1k)
disagreed or were silent in six places. Resolved as follows.

### D-01 Fixed brand palette, not Material You
- **Decision:** Blurple `#635BFF` single accent, navy `#0A2540` text/dark background, `#0C2E4E` dark
  surfaces, `#80E9FF` dark secondary accent. Dynamic (wallpaper) colour is never applied.
- **Why:** The mockups are designed around this palette (originally also for the gradient "art" tiles,
  since removed by D-56). Spec F8's "dynamic color" line is superseded.
- **Rules out:** `dynamicLightColorScheme` / `dynamicDarkColorScheme`.

### D-02 Gradient art now, embedded album art later — **superseded by D-56 (2026-08-29)**
- **Decision (historical):** Every playlist/song got a deterministic gradient slice as a stand-in for album
  art, with embedded-art extraction pencilled in for later. Album art is now permanently out of scope and the
  gradient placeholders were removed; see D-56.

### D-03 Settings entry via overflow menu
- **Decision:** `⋮` on the Playlists header → Settings. Bottom nav stays at exactly two tabs.
- **Why:** Neither spec nor mockups showed an entry point; overflow preserves the 2-tab purity.

### D-04 Playlists home is a grid
- **Decision:** Artboard 1a (grid); 1b (list) dropped.
- **Why:** A grid reads better for a handful of playlists; a personal library will not outgrow it.

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
- **Decision:** `ui/components/GradientArt.kt` mapped a seed (song or playlist id) to one of six gradient
  pairs — the D-02 placeholder in code; **removed by D-56**. `SongRow` gains `isCurrent` (title in the
  primary colour, `selected` semantics), which stays.

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

## 2026-08-29 — Sleep timer design (`openspec/changes/sleep-timer/design.md`)

### D-43 The sleep timer is a pure engine hosted by the service
- **Decision:** `SleepTimerEngine(actions, scope, clock)` over `SleepTimerActions { volume; pause() }` runs in
  `PlaybackService` on a `Dispatchers.Main.immediate` scope with `SystemClock.elapsedRealtime`. It fades the
  volume linearly over the last 10 s in 250 ms steps, pauses, then restores the volume. Unit-tested on a
  virtual clock.
- **Why:** Spec F6 requires survival without the activity; the playback wake lock keeps the CPU awake while
  music plays, so a coroutine delay is enough and no exact-alarm permission is needed.
- **Rules out:** `AlarmManager`, WorkManager, a UI-side countdown.

### D-44 Custom session commands in, session extras out
- **Decision:** `SleepTimerCommands` defines `…sleep.set` (duration or end-of-track), `…sleep.extend`,
  `…sleep.cancel`; the session callback advertises them in `onConnect` and dispatches in `onCustomCommand`;
  the engine's state is published with `setSessionExtras` and read by `PlayerConnection` on connect and in
  `MediaController.Listener.onExtrasChanged`.
- **Why:** One IPC path both ways, reconnect-safe, no extra binding.

### D-45 Remaining time is computed on the UI's own tick
- **Decision:** `SleepTimerState.Countdown` carries only the end time; the chip subtracts
  `SystemClock.elapsedRealtime()` on a 1 s tick while a countdown runs (independent of the position ticker,
  so it counts down even while paused).

### D-46 A manual pause does not cancel the timer
- **Decision:** If the user pauses before the countdown ends, the fade still runs and the final pause is a
  no-op; the volume is restored either way. Extend and cancel restore the volume immediately.

## 2026-08-29 — Polish design (`openspec/changes/polish/design.md`)

### D-47 Settings are two keys in Preferences DataStore
- **Decision:** `SettingsRepository(DataStore<Preferences>)` in `data/settings/` exposes `themeMode` and
  `minDurationMs` as flows with defaults (System, 30 s) and suspend setters; `AppContainer` owns the
  single `preferencesDataStore("settings")`. JVM tests run the real store on a temp file.
- **Rules out:** a Room table, `SharedPreferences`.

### D-48 The short-audio threshold is read at scan time
- **Decision:** `MediaScanner.scan(minDurationMs)`; `LibraryRepository` takes a `suspend () -> Long`
  provider and calls it inside every `refresh()`; `LibrarySnapshot.minDurationMs` records what was applied
  so the Folders footer and hint describe the real query. `SettingsViewModel.setMinDuration` persists,
  then refreshes once.

### D-49 The theme override is resolved above the navigation graph
- **Decision:** `MainActivity` collects `themeMode`, resolves it against `isSystemInDarkTheme()`, passes it
  to `RavMusicTheme(darkTheme)`, and re-invokes `enableEdgeToEdge` with matching `SystemBarStyle`s on
  every change so bar icons follow the palette. One frame in the system palette on a cold start is
  accepted rather than blocking `onCreate` on a disk read.

### D-50 Deleted files: re-query on resume, and a broadcast skip notice
- **Decision:** `AppRoot`'s `ON_RESUME` effect calls `LibraryViewModel.refreshIfLoaded()` (never the first
  query); files can only vanish while the app is away, so no `ContentObserver`. `MissingFileSkipper`
  broadcasts `PlaybackEvents.SKIPPED_MISSING` with the title before skipping; `PlayerConnection` maps it
  to `PlayerState.skipped: SkipNotice(title, seq)` and the shell shows one snackbar per `seq`.
- **Why:** the controller's `onPlayerError` can be coalesced away by the session; a custom command is an
  explicit event on the same channel family as D-44.

### D-51 Motion is declared at the `NavDisplay`
- **Decision:** `NavTransitions` supplies `transitionSpec` / `popTransitionSpec` /
  `predictivePopTransitionSpec` (260 ms fade + 1/8 horizontal slide; mirrored on pop). The Now Playing
  entry carries marker metadata and slides up over the current screen (`targetContentZIndex = 1`) and
  down off it (320 ms). `enableOnBackInvokedCallback` turns on predictive back. The selection bar is an
  `AnimatedContent` over `selecting`.
- **Rules out:** shared-element transitions, custom `SceneStrategy`s.

### D-52 Drag-select is a pure range plus one list-level gesture in the Initial pass
- **Decision:** `rangeSelection(base, ids, anchor, current)` and `rowIndexAt(y, rows)` are pure and
  JVM-tested. `Modifier.dragSelect` on the `LazyColumn` waits out the long-press timeout itself, reads
  moves in `PointerEventPass.Initial` (so the rows' `combinedClickable` keeps working) and consumes them
  so the list does not scroll. No auto-scroll at the edges in this change.

## 2026-08-29 — Release design (`openspec/changes/release/design.md`)

### D-53 Signing secrets live in a git-ignored `keystore.properties`, never in the build script
- **Decision:** `app/build.gradle.kts` reads `RAVMUSIC_STORE_FILE / _STORE_PASSWORD / _KEY_ALIAS / _KEY_PASSWORD`
  from `keystore.properties` at the repo root (falling back to environment variables) and creates the
  `release` signing config only when the store file is configured; otherwise `assembleRelease` yields an
  unsigned APK. The keystore itself is `%USERPROFILE%\.android\ravmusic-release.jks`, alias `ravmusic`.
- **Why:** `local.properties` is owned (and may be rewritten) by Android Studio; the wizard is not
  repeatable from the command line.

### D-54 R8 and resource shrinking are on for release; keep rules only for verified breakage
- **Decision:** `optimization { enable = true }`; `app/src/main/keepRules/rules.keep` stays empty unless a
  smoke test on the release build fails. Compose, Media3, Room, kotlinx.serialization, DataStore and
  Navigation 3 all ship consumer rules.

### D-55 `ACCESS_NETWORK_STATE` is removed from the merged manifest
- **Decision:** Media3 declares it for its bandwidth meter; the app plays local files only and has no
  `INTERNET`, so the permission is stripped with `tools:node="remove"`. The release APK lists exactly
  the storage and foreground-service permissions.

## 2026-08-29 — No album art (`openspec/changes/no-album-art/`)

### D-56 No album art, ever
- **Decision:** Album art is permanently out of scope: no embedded-art extraction, no `MediaMetadataRetriever`,
  and no placeholder tiles. `ui/components/GradientArt.kt` and every gradient box (Now Playing, mini
  player, playlist cards, playlist header, add-to-playlist sheet) were removed; layouts close up around the
  text. Supersedes D-02 and the helper half of D-28.
- **Why:** The user decided it; a placeholder that promises a feature that is not coming is worse than none.

### D-57 Settings footer is a maker credit in the secondary-text colour
- **Decision:** The footer reads the version and "Built by Ravi Kant" in `onSurfaceVariant` (Slate on white,
  SlateDark on navy) instead of the privacy line in `outlineVariant`, which was a border tone and barely
  legible in dark mode. The no-`INTERNET` guarantee is unchanged and lives in the manifest and README.

## 2026-08-30 — Open with and search (`openspec/changes/2026-08-30-open-with/`, `2026-08-30-search/`)

### D-58 "Open with" plays just that file, and the shell decides when Now Playing opens
- **Decision:** `MainActivity` registers `VIEW` (content/file, audio MIME types plus the `application/*`
  aliases file managers use) and `SEND` (`audio/*`) filters; no web schemes, no multi-file share. The
  intent is reduced to a pure `OpenRequest` held in `AppViewModel` (memory only: a recreation never
  replays the file; `onNewIntent` handles a second file). `UriSongResolver` reuses a library song by
  URI, else the MediaStore row (canonical `audio/media/<id>` URI so playlist rows still highlight),
  else the provider's display name, else the file name; anything without a MediaStore row gets a
  synthetic id `<= -2` so it can never match a library or playlist song. The queue is that one song,
  origin "Opened file"; `AppNavigation` pushes `NowPlaying` only once the session reports the song
  current (the screen pops itself while no queue is loaded). Not gated by the audio permission: the
  system's per-file grant is enough. No `takePersistableUriPermission`, no `MediaMetadataRetriever`.
- **Why:** The user asked for the app to appear in "Open with" and chose "play just that file"; the
  rest is the smallest path that keeps `Song` pure, avoids a Now Playing race, and never lets a
  fabricated id light up someone else's row.

### D-59 One search rule and bar everywhere; filtering disables reordering; global search is a route
- **Decision:** `matchesQuery` (trimmed, case-insensitive contains on title or artist, blank = all) is
  the only matching rule; `SearchTopBar` swaps in for a screen's title bar with the same tags on every
  screen. In a filtered playlist the drag handles disappear (positions in a filtered list would lie),
  swipe-to-remove and tap-to-play keep working with indices mapped back to the full list, and Play /
  Shuffle play still play the whole playlist. Folder detail has a three-way bar (title / search /
  selection, selection wins) and selection works over the rows shown. Search across playlists is a
  Nav3 route above `Playlists` (bottom bar hidden, query kept while a detail sits above it), fed by one
  `observeAllTracks()` flow joined in memory with the playlist names; the detail's play logic became
  `planPlaylistPlay`, shared with search hits.
- **Why:** The user wanted both a per-playlist filter and a cross-playlist search; one rule and one bar
  keep the three screens identical to learn and to test, and a route avoids special-casing the FAB,
  grid scroll and bottom bar on the home screen.

### D-60 Equalizer: audiofx on our own session id, DataStore drives the service
- **Decision:** `android.media.audiofx` `Equalizer`/`BassBoost`/`Virtualizer` created eagerly (disabled)
  in `PlaybackService` on a self-generated audio session id set on the player, recreated and re-applied
  on `onAudioSessionIdChanged`; every HAL call sits behind try/catch so a flaky device degrades to a
  disabled control instead of a crash. All state (`eq_enabled`, preset index with −1 = Custom,
  comma-joined millibel band levels, 0–1000 strengths) lives in the existing Preferences DataStore
  behind `EqualizerSettingsRepository`: the UI writes snapshots (conflated ~100 ms during drags), the
  service collects the flow and applies — no session commands for state, unlike the sleep timer, since
  nothing here is transient. Device capabilities (band count, centre freqs, level range, preset names
  and per-preset shapes) travel over one `GET_CAPABILITIES` custom session command rather than
  `sessionExtras`, which the sleep-timer collector already overwrites wholesale. Stored values are
  fitted to whatever device applies them: out-of-range presets fall back to Custom, band-count
  mismatches reset to flat, levels clamp to the device range. UI is `EqualizerSheet` (master switch,
  preset chips, rotated vertical band sliders, strength sliders) off a Now Playing chip; the chip row
  became a `FlowRow` so three chips wrap on narrow screens.
- **Why:** DataStore-as-source-of-truth gives "applies on relaunch without opening the UI" for free and
  keeps the service the only owner of effect objects; effects default off so a fresh install sounds
  identical. Verified live on the API 26 AVD — both emulators ship NXP software effects, and
  `dumpsys media.audio_flinger` showed the three-effect chain on our session — leaving only the by-ear
  check on the real phone (tasks 4.2/4.3 annotated in the archived change).
