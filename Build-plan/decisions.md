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
