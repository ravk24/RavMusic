## 1. Dependencies and manifest

- [ ] 1.1 Add `media3 = "1.11.0"` with `androidx-media3-exoplayer` and `androidx-media3-session` to `gradle/libs.versions.toml` and as `implementation` in `app/build.gradle.kts`; verify `.\gradlew.bat assembleDebug` resolves both with no "Could not find"
- [ ] 1.2 Declare `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `WAKE_LOCK` and the `.playback.PlaybackService` `<service>` (`exported="true"`, `foregroundServiceType="mediaPlayback"`, `androidx.media3.session.MediaSessionService` intent filter) in `AndroidManifest.xml`; verify the merged manifest at `app/build/intermediates/merged_manifest/debug/AndroidManifest.xml` contains the service and the three permissions and still has 0 hits for `INTERNET`

## 2. Pure playback model

- [ ] 2.1 Create `playback/PlayerState.kt` (`NowPlaying`, `PlayerState`) and `playback/QueueBuilder.kt` with pure `plan(songs, startIndex, origin)` (items in folder order, start index preserved, origin attached, empty/out-of-range inputs handled); verify `QueueBuilderTest` and a `PlayerStateTest` for the "no queue ⇔ nowPlaying == null" rule
- [ ] 2.2 Add `toMediaItems()` (Android wrapper: `mediaId = song.id`, uri, title, artist, `extras["origin"]`) and `NowPlaying.from(MediaItem)`; verify an instrumented `MediaItemMappingTest` round-trips a `Song` through `MediaItem` and back

## 3. Service

- [ ] 3.1 Create `playback/PlaybackService.kt`: `MediaSessionService` with an `ExoPlayer` (music audio attributes, audio focus, handle-becoming-noisy, local wake mode), a `MediaSession` with a session activity to `MainActivity`, `onGetSession`, `onTaskRemoved` stopping when not playing, release in `onDestroy`; verify the app installs and `adb shell dumpsys activity services com.ravk24.ravmusic` lists the service after playback starts
- [ ] 3.2 Add the error-skip listener (`onPlayerError` → next item + prepare + play, or stop and clear at the end of the queue); verify the instrumented `PlaybackServiceTest` queues `[good, missing, good]` and observes the third item playing, and `[good, missing]` ends stopped with no queue

## 4. Controller bridge and ViewModel

- [ ] 4.1 Create `playback/PlayerConnection.kt` (async `MediaController`, `Player.Listener` → `StateFlow<PlayerState>`, `playFolder`, `togglePlayPause`, `stopAndClear`, one pending command while connecting, `release()`); verify `PlaybackServiceTest` drives the service through `PlayerConnection` with a bundled 3 s WAV: `isPlaying` becomes true, `nowPlaying` carries title/artist/origin, position advances, toggle pauses, `stopAndClear` returns `nowPlaying == null`
- [ ] 4.2 Create `PlayerViewModel.kt` (connect in `init`, release in `onCleared`, 500 ms position ticker only while playing) and register `PlayerConnection` in `AppContainer`; create it in `AppRoot` with `viewModelFactory` and pass `playerState` + `onPlayPause`/`onDismissPlayer`/`onPlaySong` into `AppNavigation`; verify a `PlayerViewModelTest` with a fake connection shows the ticker starts and stops with `isPlaying`

## 5. UI

- [ ] 5.1 Add `AppIcons.PlayArrow` / `AppIcons.Pause` and `ui/components/GradientArt.kt` (`artGradient(seed)` cycling the six mockup gradient pairs); verify a `GradientArtTest` that equal seeds give equal brushes and the six pairs are all reachable, and previews render
- [ ] 5.2 Create `ui/player/MiniPlayer.kt` (progress line, gradient tile, title, artist or "Unknown artist", play/pause button, `SwipeToDismissBox` → `onDismiss`, inert body tap); verify `MiniPlayerTest`: play/pause callback, texts, progress fraction, swipe calls `onDismiss`, tap does nothing
- [ ] 5.3 Dock the mini player in `AppNavigation` (`bottomBar` column above the `NavigationBar`, present on `FolderDetail` and `Settings` too, absent when `nowPlaying == null`); verify `MiniPlayerDockingTest`: hidden with empty state, shown above `bottom_bar` on tabs, shown without `bottom_bar` on a detail, hidden after dismiss
- [ ] 5.4 Wire tap-to-play: `FolderDetail` entry passes `onSongClick = { onPlaySong(songs, index, folderName) }` and `nowPlayingId`; `SongRow` highlights the current song; verify `LibraryNavigationTest` gains a case that tapping `song_row_<id>` invokes the play lambda with the folder's songs, the tapped index and the folder name, and that the row is highlighted when `nowPlayingId` matches

## 6. Integration and docs

- [ ] 6.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify `BUILD SUCCESSFUL` and every test passes on both
- [ ] 6.2 Manual walkthrough on both emulators with the Phase 2 WAVs: tap a song → audio plays and the mini player appears; Home + lock screen → keeps playing, notification shows controls; pause from the notification → app shows paused; `adb shell am broadcast -a android.media.AUDIO_BECOMING_NOISY` (or `cmd media_session dispatch pause` fallback) → paused, play resumes at the same position; kill the activity (`am force-stop` is too strong — use "swipe from recents") and reopen → same song shown; delete the next queued file and let the current one end → skipped; swipe the mini player away → stops, notification gone; verify each `playback` / `mini-player` scenario and note deviations here
- [ ] 6.3 Update `Build-plan/README.md` (row 3 ✅), `Build-plan/phases/03-playback-core.md` (status, result, task progress) and `Build-plan/decisions.md` (D-24… for the docking, error-skip, controller-bridge and manifest decisions); commit on `main` and push; verify `git status` is clean and `git log -1` shows the commit
