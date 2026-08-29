# RavMusic

A personal, offline, ad-free music player for Android. It plays the audio files already on the phone,
groups them by the folders they live in, and lets you build playlists fast with long-press multi-select —
no accounts, no telemetry, no subscriptions, and **no `INTERNET` permission**, so it physically cannot go online.

Built for one user and sideloaded; never intended for the Play Store.

## What it does

- **Folders** — browse the device's music grouped by storage folder (live MediaStore query, nothing cached),
  pull to refresh, clips under 30 s hidden.
- **Playback** — background playback with a Media3 foreground service: notification and lock-screen controls,
  headset buttons, audio focus, pause on unplug; missing files are skipped.
- **Mini player** — docked above the navigation bar on every screen; tap to expand, swipe to stop.
- **Playlists** — long-press songs in a folder to select, add them to a playlist (duplicates are flagged),
  reorder by drag, swipe to remove, Shuffle play / Play, stored locally in Room.
- **Now Playing** — full-screen player with seek, shuffle, repeat (off / all / one), and a queue sheet you
  can jump into and reorder.

Coming next: sleep timer, settings (theme override, short-audio threshold), polish, signed release APK.

## Screens

Fixed navy / blurple palette, light and dark; no album art by design.
The design canvas the screens follow lives outside the repo; the build order and every settled decision are
in [`Build-plan/`](Build-plan/README.md).

## Status

| Phase | Change | Status |
|---|---|---|
| 1 Skeleton — theme, Navigation 3 shell, permission flow, launcher icon | `app-skeleton` | ✅ |
| 2 Library — MediaStore scanner, folder browser | `library-browser` | ✅ |
| 3 Playback core — Media3 service, tap-to-play, mini player | `playback-core` | ✅ |
| 4 Playlists — Room, multi-select, playlist home and detail | `playlists` | ✅ |
| 5 Now Playing — full screen, seek, shuffle/repeat, queue | `now-playing` | ✅ |
| 6 Sleep timer | `sleep-timer` | ⏳ |
| 7 Polish — settings, empty states, missing-file handling, motion | `polish` | ⏳ |
| 8 Ship — release build, local signing, sideload | `release` | ⏳ |

Every phase ends installable and is verified with JVM tests, Compose/instrumented tests on API 36 and API 26
emulators, and an adb walkthrough. Current totals: 66 JVM tests, 81 instrumented tests, all green on both APIs.

## Tech stack

| Layer | Choice |
|---|---|
| Language / UI | Kotlin (AGP 9 built-in Kotlin), Jetpack Compose + Material 3, Navigation 3 |
| Playback | Media3 (ExoPlayer + `MediaSessionService`) |
| Library | MediaStore queries — never cached |
| Persistence | Room (playlists only), schema exported to `app/schemas/` |
| Architecture | Single activity, Activity-scoped ViewModels + `StateFlow`, manual DI (`AppContainer`) |
| Process | [OpenSpec](https://github.com/Fission-AI/OpenSpec) — each phase is a change with proposal, specs, design and tasks |

Min SDK 26 · target SDK 37 · one runtime permission (`READ_MEDIA_AUDIO`, or `READ_EXTERNAL_STORAGE` below Android 13).

## Building

Requires Android Studio's bundled JDK (or any JDK 17+) and the Android SDK.

```powershell
# Windows PowerShell — JAVA_HOME is not set globally on the dev machine
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat testDebugUnitTest             # JVM tests
.\gradlew.bat connectedDebugAndroidTest     # Compose + instrumented tests (needs a device or emulator)
```

Install the debug APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, then copy some music into
`/Music` (or any folder) and let the media scanner index it. Emulator setup, test-audio generation and the adb
commands used for verification are documented in [`Build-plan/README.md`](Build-plan/README.md).

## Repository layout

```
app/src/main/java/com/ravk24/ravmusic/
  data/        MediaStore scanner, library snapshot, Room entities/DAO, repositories
  playback/    PlaybackService (Media3), PlayerConnection (MediaController bridge), PlayerState/Actions
  ui/          theme, navigation shell, folders, playlists, nowplaying, player (mini player), components
  *ViewModel   AppViewModel (permission), LibraryViewModel, PlayerViewModel, PlaylistsViewModel
app/schemas/   exported Room schemas (versioned)
Build-plan/    product spec, phase pages, decision log
openspec/      specs (current behaviour) and changes (per-phase proposal/design/tasks, archived when shipped)
```

## Hard constraints

- No ads, accounts, telemetry, subscriptions, API keys or backend.
- The manifest never declares `INTERNET`.
- No analytics SDKs, crash reporters or bloat dependencies (no icon packs, no DI framework).
