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
