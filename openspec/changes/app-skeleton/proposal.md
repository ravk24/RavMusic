## Why

The Gradle build is verified green with Compose wired in, but the app has no entry point, no theme, and no navigation — it cannot be launched. Phase 1 of the build order ("Skeleton") gives every later phase (library browser, playback, playlists, now-playing) an installable shell to plug into, and settles the three cross-cutting decisions — visual theme, navigation structure, and the audio-permission flow — once, so they are not re-litigated screen by screen.

## What Changes

- **BREAKING** (for any installed debug build only): rename the package, namespace, and `applicationId` from `com.example.ravmusic` to `com.ravk24.ravmusic`. The old debug app must be uninstalled once; there is no user data to migrate.
- Add `MainActivity` as the single activity, drawing edge-to-edge, plus an `Application` subclass as the home for app-wide wiring in later phases.
- Add a Material 3 theme using the fixed brand palette from the mockups (blurple `#635bff` accent, navy dark surfaces). Light/dark follows the system setting. Dynamic (wallpaper) color is deliberately **not** used.
- Add a two-tab bottom navigation shell — **Playlists** (default) and **Folders** — built on Navigation 3, with predictable back behaviour and per-tab state retention.
- Add placeholder content for both tabs. Real folder and playlist content arrives in the library and playlists phases.
- Add the runtime audio-permission flow: request `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 8.0–12), gate the tabs behind it, and show the "No music found / Allow access to audio" state (mockup 1h) when it is missing — including recovery when the permission was permanently denied or revoked.
- Add an overflow action on the Playlists screen that opens a stub **Settings** screen (title, back, and the "v1.0 · No INTERNET permission" footer). Theme override and library options land in a later change.
- Manifest gains the activity and the two storage permissions. It still declares **no** `INTERNET` permission — this remains the app's privacy guarantee.

Explicitly not in this change: MediaStore queries, Media3 playback, the mini player, Room, persisted theme override, embedded album art.

## Capabilities

### New Capabilities
- `app-shell`: single-activity shell — bottom navigation with two tabs, tab/back-stack behaviour, state retention across tab switches and rotation, and the Settings entry point.
- `theme`: fixed brand palette for light and dark, following the system dark-mode setting, with edge-to-edge system bars.
- `audio-permission`: the one runtime permission the app needs — which permission on which Android version, the gated empty state, and recovery from denial, permanent denial, and revocation.

### Modified Capabilities
<!-- none — no existing specs -->

## Impact

- `app/build.gradle.kts`, `gradle/libs.versions.toml`: new namespace/applicationId; Navigation 3 dependencies added.
- `app/src/main/AndroidManifest.xml`: `<application android:name>`, launcher `<activity>`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`).
- New Kotlin sources under `app/src/main/java/com/ravk24/ravmusic/` (`MainActivity`, `RavMusicApp`, `ui/theme/`, `ui/navigation/`, `ui/components/`, `ui/permission/`, placeholder `ui/playlists/`, `ui/folders/`, `ui/settings/`).
- Existing template test sources move to the new package.
- XML `Theme.RavMusic` stays only as the pre-Compose window theme; all visible theming moves to Compose.
- No external services, no new permissions beyond storage, no data model yet.
