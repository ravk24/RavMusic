## Why

The app can browse every song on the device but cannot play one. Phase 3 of the build order ("Playback core") is the first "it's a real app" moment: tap a song in a folder and it plays — in the background, on the lock screen, through headset buttons — with a mini player docked above the navigation bar. Everything after this (playlists, Now Playing, sleep timer) is built on this service and this bridge.

## What Changes

- Add a foreground playback service built on Media3 (`MediaSessionService` + ExoPlayer): background playback, the system media notification with play/pause/next/previous, lock-screen and Bluetooth/headset controls, audio focus (pause on call, duck/pause on other audio), and pause when headphones are unplugged.
- Add a UI-side player bridge (`MediaController` behind a `PlayerViewModel`) that exposes what is playing, whether it is playing, position and duration, and the queue's origin ("Playing from: Rock") as plain state for Compose.
- **Tap-to-play from a folder**: tapping a song in a folder detail starts an ad-hoc queue of that folder's songs from the tapped one (spec F1/F4). The `folder-browser` "song tap is inert" scenario is replaced.
- Add the **mini player** (mockup 1c bottom bar): docked above the bottom navigation on every screen while something is loaded — gradient art tile (decision D-02), title, artist, play/pause, a thin progress line; tap is reserved for Now Playing (no effect in this change); swipe away stops playback and clears the queue.
- Missing-file handling at playback time: a song whose file no longer opens is skipped to the next one instead of stopping playback (spec F1 edge case).
- Manifest: the `<service>`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (declared, not user-facing) and `WAKE_LOCK` so playback continues with the screen off. Still **no** `INTERNET`, still exactly one runtime permission.

Explicitly not in this change: the full Now Playing screen, seek bar, shuffle/repeat UI, queue sheet, sleep timer, playlists, embedded album art, crossfade.

## Capabilities

### New Capabilities
- `playback`: background playback of a queue of songs — service lifetime, system controls and notification, audio focus and noisy-audio handling, queue building from a folder, queue origin, missing-file skipping, reconnecting the UI to a running session.
- `mini-player`: the persistent docked mini player — visibility, content, play/pause, progress, swipe-to-dismiss, and its place in the shell relative to the bottom navigation.

### Modified Capabilities
- `folder-browser`: the "Folder detail" requirement — tapping a song now starts playback of that folder from the tapped song (replaces "Tapping a song SHALL have no effect in this change").

## Impact

- New dependencies: `androidx.media3:media3-exoplayer` and `media3-session` 1.11.0 (Guava arrives transitively with them; no separate coroutines-guava artifact).
- `AndroidManifest.xml`: `PlaybackService` with `foregroundServiceType="mediaPlayback"` and the `MediaSessionService` intent filter; `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` (all normal permissions).
- New `playback/` package (service, controller bridge, queue building); `PlayerViewModel` next to the other Activity-scoped ViewModels; `AppContainer` gains the bridge.
- `AppNavigation`: the Scaffold's bottom slot becomes a column of mini player + navigation bar; `FolderDetail` entry wires `onSongClick`. New `ui/player/MiniPlayer.kt`, `ui/components/GradientArt.kt`, Play/Pause icons.
- Tests: JVM tests for the pure queue/state helpers; Compose tests for the mini player and the shell docking; an instrumented service test that plays a bundled test WAV through a real `MediaController`.
