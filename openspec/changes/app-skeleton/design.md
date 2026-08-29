## Context

See proposal.md — Why. Current state: AGP 9.3.2 with built-in Kotlin 2.2.10, Compose BOM 2026.08.00 / Material 3 1.4.0, activity-compose 1.13.0 and lifecycle 2.11.0 already in the version catalog, `assembleDebug` green, and **zero Kotlin sources**. The manifest has no activity. Structure comes from `music-player-spec.md` (F8, Permissions, Project Structure) and the design canvas artboards 1a (Playlists home), 1c (Folders), 1g (Settings), 1h (empty/permission state), 1i–1k (dark). The fixed palette, package rename, and Phase-1-only scope are settled user decisions.

Constraints that shape the approach: no `INTERNET`, no analytics, minimal dependencies (spec: "no bloat"), minSdk 26, a single activity, MVVM-ish with ViewModel + StateFlow.

## Goals / Non-Goals

**Goals:**
- A shell that later phases compose *into* without restructuring: tabs, back stack, theme, and permission gate are each a single, replaceable seam.
- Keep the permission gate testable without a device (state is a value, not an Activity callback).
- Leave a clear slot for the mini player and for a theme override without deciding their behaviour now.

**Non-Goals:**
- MediaStore queries, Media3, Room, the mini player, persisted settings, album art.
- A design-system component library — only what the skeleton screens need.
- Hilt or any DI framework.

## Decisions

### D1. Navigation 3 for the back stack
Use `androidx.navigation3:navigation3-runtime` + `navigation3-ui` (stable 1.1.7). Routes are `@Serializable` `NavKey` objects — `Playlists`, `Folders`, `Settings` — held in a `rememberNavBackStack(Playlists)`. The bottom bar is rendered by the shell `Scaffold` only when the top entry is a tab root; tab taps replace the stack with `[Playlists]` or `[Playlists, Folders]` so back from Folders lands on Playlists and back from Playlists exits (spec `app-shell`). `Settings` is pushed on top of whichever tab is showing.
*Alternatives:* Navigation Compose 2.10 — officially in maintenance mode, and its `NavHost`/route-string model is what Nav3 replaces. A hand-rolled `when (screen)` — no predictive-back or saved-state semantics; would be rewritten as soon as folder/playlist detail screens arrive.

### D2. Fixed colour schemes, `darkTheme` as a parameter
`ui/theme/Color.kt` holds the mockup hex values; `Theme.kt` builds one `lightColorScheme(...)` and one `darkColorScheme(...)`. `RavMusicTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` never calls `dynamicLightColorScheme`/`dynamicDarkColorScheme`. Making `darkTheme` a parameter is the seam for the later Settings override — a `ThemeMode` flow can be resolved at the call site in `MainActivity` without touching any screen.
*Alternatives:* Material You dynamic colour — rejected by the user; breaks the gradient-as-art idea. A `ThemeMode` enum now — would require persistence (DataStore) that is out of scope.

### D3. Edge-to-edge via `enableEdgeToEdge()`
Call `enableEdgeToEdge()` in `MainActivity.onCreate` (activity-compose 1.13 re-applies it on configuration change) and let `Scaffold`'s content padding + `NavigationBar`'s built-in insets handle the bars. System bar icon contrast follows the theme automatically because `enableEdgeToEdge` detects the background luminance.
*Alternatives:* `WindowCompat.setDecorFitsSystemWindows(false)` + manual `SystemBarStyle` — more code for the same result.

### D4. Permission state as a value in `AppViewModel`
A sealed `PermissionState { Granted, Denied(canRequest: Boolean), Unknown }` is exposed as `StateFlow` from an `AppViewModel`. The composable `AudioPermissionGate(state, onRequest, onOpenSettings) { content }` shows `NoMusicFoundScreen` (artboard 1h) or `content`. The Activity supplies the two side-effects: `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` and an intent to `ACTION_APPLICATION_DETAILS_SETTINGS`. The permission name is a single function `audioPermissionFor(sdkInt: Int)` (`READ_MEDIA_AUDIO` when `sdkInt >= 33`, else `READ_EXTERNAL_STORAGE`) — pure, unit-testable. "Permanently denied" = not granted **and** `shouldShowRequestPermissionRationale == false` **and** at least one request has been made this install (tracked in the ViewModel's saved state), which avoids misclassifying a fresh install. The state is re-evaluated on every `ON_RESUME` via `LifecycleEventEffect` (spec: revocation on return).
*Alternatives:* Accompanist Permissions — archived. Handling everything in the Activity — untestable and leaks Activity into screens.

### D5. Manual DI with an `Application` subclass
`RavMusicApp : Application` exposes an `AppContainer` (empty now). ViewModels are created with a small `viewModelFactory` that pulls from the container. This is the spec's "manual is fine" path; it gives later phases (`LibraryRepository`, `PlaylistRepository`, the `MediaController` bridge) one obvious place to be constructed.
*Alternatives:* Hilt — adds KSP + annotation processing to every build for a one-person app with < 10 injectables.

### D6. Icons without `material-icons-extended`
Material 3 already brings `material-icons-core` (Add, MoreVert, ArrowBack, Settings, PlayArrow, Close, Check, Refresh). The two icons the shell needs that are not in core — Folder and QueueMusic — are built as `ImageVector`s in `ui/components/AppIcons.kt` from the exact 24dp path data used in the mockup SVGs.
*Alternatives:* `material-icons-extended` — no longer BOM-managed and ~10 MB of unshrunk classes; contradicts "lightweight".

### D7. Package rename to `com.ravk24.ravmusic`
Namespace, `applicationId`, main/test/androidTest source directories, and the template test classes all move. Done first so every new file is born in the right package.

### D8. Settings as a stub route
`SettingsScreen` renders a top bar with back, the "Appearance"/"Library" section headers are **not** rendered yet, and the footer shows `versionName` + "No INTERNET permission — this app cannot go online." Real rows arrive with the settings change; the route and back behaviour are what this change locks in.

### D9. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  RavMusicApp.kt            Application + AppContainer
  MainActivity.kt           enableEdgeToEdge, theme, permission side-effects, AppNavigation
  AppViewModel.kt           PermissionState StateFlow, request bookkeeping
  permission/AudioPermission.kt   audioPermissionFor(), PermissionState
  ui/theme/{Color,Theme,Type}.kt
  ui/navigation/{Routes,AppNavigation}.kt   NavKeys, NavDisplay, bottom bar, tab switching
  ui/components/AppIcons.kt
  ui/permission/{AudioPermissionGate,NoMusicFoundScreen}.kt
  ui/playlists/PlaylistsScreen.kt     placeholder + overflow → Settings
  ui/folders/FoldersScreen.kt         placeholder
  ui/settings/SettingsScreen.kt       stub
```
Matches the spec's planned structure; `data/` and `playback/` are created by later changes.

## Risks / Trade-offs

- [Navigation 3 API is young; entry decorators and saved-state helpers may shift between minors] → keep all Nav3 usage inside `ui/navigation/`; screens receive plain lambdas, never `NavBackStack`.
- [Package rename after a `com.example.ravmusic` debug install leaves two apps on the device] → note in tasks to uninstall the old one; no data exists to lose.
- [Permanent-denial heuristic can misfire on Android's "ask every time"/auto-reset behaviour] → treat as permanent only after a request has been made; the settings deep-link is a safe fallback either way (user can grant from there).
- [Edge-to-edge on API 26–29 uses different navigation-bar contrast rules] → verify on an API 26 emulator as part of the integration task.
- [Placeholder tabs look "done" but are empty] → placeholder text explicitly says content arrives in a later phase.

## Migration Plan

No user-facing migration: there is no persisted data. Developer step: `adb uninstall com.example.ravmusic` once before installing the renamed build. Rollback is `git revert`; nothing external is touched.

## Open Questions

- Where the mini player docks (inside the `Scaffold` `bottomBar` column above the `NavigationBar`, or as an overlay) is deferred to the playback change; the shell's `Scaffold` is the only thing that needs to know, so this does not affect the specs or tasks here.
