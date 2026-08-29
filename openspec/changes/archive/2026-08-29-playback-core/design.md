## Context

See proposal.md — Why. Current state after `library-browser`: `LibrarySnapshot` holds `Song(id, uri: String, title, artist?, durationMs, folderId, folderName)`; `FolderDetailScreen` already exposes `onSongClick: (Song) -> Unit` (a no-op); `AppNavigation` receives state as values from Activity-scoped ViewModels created in `AppRoot`; `AppContainer` is the manual-DI home. No `playback/` package, no service, no Media3 dependency.

Constraints: no `INTERNET`, exactly one runtime permission (media notifications are exempt from `POST_NOTIFICATIONS`), lightweight dependencies, minSdk 26 / targetSdk 37 (foreground-service type is mandatory from API 34), fixed palette with gradient art (D-01/D-02), Navigation 3 shell with the bottom-bar seam left open by the skeleton design ("where the mini player docks is deferred to the playback change").

Verified before design: Media3 1.11.0 is the current stable release on Google's Maven; `media3-session` ships `DefaultMediaNotificationProvider` (notification, lock screen, headset handled by the session); Guava is a transitive dependency of `media3-common`, so `ListenableFuture` callbacks need no extra artifact.

## Goals / Non-Goals

**Goals:**
- One service that owns the player; the UI is a thin `MediaController` client that can reconnect to a running session.
- Player state reaches Compose as a plain value (`PlayerState`) through the same seam as permission and library state.
- The queue-building rule (folder → items from the tapped index, origin label) is pure and unit-tested.
- A docking decision for the mini player that later phases (Now Playing expand, playlists) do not need to revisit.

**Non-Goals:**
- Now Playing UI, seek, shuffle/repeat, queue editing, sleep timer (later changes; the session already supports them).
- Persisting the queue across process death of the service.
- Album-art extraction; custom notification layouts.

## Decisions

### D1. Media3 `MediaSessionService` + ExoPlayer in `playback/PlaybackService`
`PlaybackService : MediaSessionService` creates one `ExoPlayer` with `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MUSIC)` and `handleAudioFocus = true`, `setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_LOCAL)`, wraps it in a `MediaSession` whose session activity is a `PendingIntent` to `MainActivity`, and returns it from `onGetSession`. `onTaskRemoved` stops the service when the player is not playing; `onDestroy` releases session and player. The default notification provider supplies notification, lock-screen and headset handling.
*Alternatives:* `MediaBrowserServiceCompat` + `MediaSessionCompat` — the legacy stack Media3 replaces; a plain `Service` with `MediaPlayer` — no session integration, would have to hand-roll notification and focus.

### D2. `PlayerConnection` bridge in the UI process, exposed by an Activity-scoped `PlayerViewModel`
`playback/PlayerConnection` builds a `MediaController` via `MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()` and completes it with `addListener(…, MoreExecutors.directExecutor())`. It registers a `Player.Listener` and publishes `StateFlow<PlayerState>`; commands are `playFolder(songs, startIndex, origin)`, `togglePlayPause()`, `stopAndClear()`. `PlayerViewModel` (created in `AppRoot` like `LibraryViewModel`) connects in `init`, releases the controller in `onCleared`, and runs a position ticker (500 ms) only while `isPlaying`. `AppNavigation` receives `playerState` + three lambdas.
*Alternatives:* entry-scoped ViewModel — dies on tab switch; connecting the controller inside composables — leaks bindings on recomposition.

### D3. `PlayerState` is a plain value; the mapping is pure
```
data class NowPlaying(val songId: Long, val title: String, val artist: String?, val origin: String)
data class PlayerState(val nowPlaying: NowPlaying? = null, val isPlaying: Boolean = false,
                       val positionMs: Long = 0, val durationMs: Long = 0)
```
`nowPlaying == null` ⇔ no queue loaded ⇔ mini player hidden. Title/artist/origin travel inside each `MediaItem.mediaMetadata` (title, artist, `extras["origin"]`) and `mediaId = song.id`, so the UI can rebuild `NowPlaying` from the controller alone after a reconnect. `MediaItem` is an Android class, so the pure, tested part is `QueueBuilder.plan(songs, startIndex)` → ordered list of `(song, origin)` plus the start index; the `MediaItem` construction is a thin Android wrapper.

### D4. Missing files are skipped at playback time, not pre-filtered
The phase page suggested pre-filtering unresolvable URIs when the queue is built; opening every file descriptor of a large folder on tap is slow and still races with deletion. Instead, `Player.Listener.onPlayerError` on the service side (a source/IO error) calls `seekToNextMediaItem()` + `prepare()` + `play()` when there is a next item, otherwise `stop()` and clear. This is what spec F1's "skip on playback" asks for and costs nothing on the happy path. The playlists change adds the greyed-row UI on top.

### D5. Mini player docks inside the Scaffold's `bottomBar` as a column
`AppNavigation`'s `bottomBar = { Column { if (playerState.nowPlaying != null) MiniPlayer(...); if (showBottomBar) NavigationBar(...) } }`. Content padding therefore grows automatically, the mini player is present on every route (tabs, `FolderDetail`, `Settings`), and the Now Playing change can later animate the same slot into a full screen.
*Alternatives:* an overlay `Box` above content — content would scroll under it and each screen would need its own bottom padding.

### D6. Mini player composable and gradient art
`ui/player/MiniPlayer.kt`: `MiniPlayer(state: PlayerState, onPlayPause, onExpand, onDismiss)` — 2 dp progress line (`positionMs / durationMs`), 40 dp tile painted with `artGradient(songId)` from `ui/components/GradientArt.kt` (the six mockup gradient pairs, chosen by `id mod 6`, per D-02), title (SemiBold, ellipsized), artist or "Unknown artist", and a 34 dp circular play/pause button (`AppIcons.PlayArrow` / `AppIcons.Pause`, navy on light, cyan-tinted on dark). Dismissal uses Material 3 `SwipeToDismissBox` (both directions) calling `onDismiss`; `onExpand` is wired to nothing in this change.

### D7. Current-song highlight in `FolderDetailScreen`
`FolderDetailScreen` gains `nowPlayingId: Long?`; `SongRow` gains `isCurrent: Boolean` and tints the title with the primary colour when true. Cheap, and the same row is reused by playlist detail later.

### D8. Manifest and permissions
`<service android:name=".playback.PlaybackService" android:exported="true" android:foregroundServiceType="mediaPlayback">` with the `androidx.media3.session.MediaSessionService` intent filter; `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` — all normal (install-time) permissions, so the `audio-permission` spec's "exactly one runtime permission" holds. No `POST_NOTIFICATIONS`: media-session notifications are exempt.

### D9. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  PlayerViewModel.kt
  playback/PlaybackService.kt        MediaSessionService + ExoPlayer, error-skip
  playback/PlayerConnection.kt       MediaController client, StateFlow<PlayerState>, commands
  playback/PlayerState.kt            NowPlaying, PlayerState (pure)
  playback/QueueBuilder.kt           plan(songs, startIndex, origin) (pure) + toMediaItems (Android)
  ui/player/MiniPlayer.kt
  ui/components/GradientArt.kt       artGradient(seed)
  ui/components/AppIcons.kt          + PlayArrow, Pause
```

## Risks / Trade-offs

- [Foreground-service start restrictions on API 34+] → playback only ever starts from a user tap in the foreground activity; the service is started by the `MediaController` connection, which is exempt while the app is visible.
- [`MediaController` connection is asynchronous; a tap before it completes] → `PlayerConnection` queues the first command until connected (a single pending command), and the mini player only appears once the controller reports an item.
- [Position ticker drains battery if left running] → it runs only while `isPlaying` and the ViewModel is alive; Now Playing will reuse it.
- [Sending `AUDIO_BECOMING_NOISY` from `adb shell am broadcast` may be rejected as a protected broadcast on some images] → verification falls back to `adb shell cmd media_session dispatch pause` for the pause path and documents the noisy-audio scenario as verified via ExoPlayer's `setHandleAudioBecomingNoisy` unit-level guarantee if the broadcast is refused.
- [Guava on the classpath via Media3] → transitive and R8-shrunk in release; no new direct dependency.
- [Instrumented service test needs a playable file] → a 3 s WAV bundled in `androidTest/assets`, copied to the app cache and played by file URI; MediaStore is not involved in the test.

## Migration Plan

No persisted data. New install-time permissions are granted automatically on update. Rollback is `git revert`; the service is only started on user action, so a reverted build leaves nothing running.

## Open Questions

- Whether the mini player should also be shown while the permission gate is displayed (permission revoked mid-playback). Deferred: the service keeps playing either way; the polish change decides the UI.
