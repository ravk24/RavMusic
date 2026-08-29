## 1. Engine and state

- [x] 1.1 Create `playback/SleepTimerEngine.kt` (`SleepTimerState`, `SleepTimerActions`, engine with `set`, `endOfTrack`, `extend`, `cancel`, `onTrackEnded`, fade-then-pause, volume restore) and `formatRemaining()` in `ui/components/Format.kt`; verify `SleepTimerEngineTest` on a virtual clock: fade steps start at `end - 10 s`, pause at `end` with volume restored, extend moves the end and restores mid-fade volume, cancel restores, end-of-track pauses on `onTrackEnded`, replacing a timer cancels the old one; `FormatTest` covers "32:14" and "1:02:14"
- [x] 1.2 Add `sleepTimer: SleepTimerState` to `PlayerState` and `playback/SleepTimerCommands.kt` (command names, arg keys, `toBundle`/`fromBundle`); verify a JVM `SleepTimerCommandsTest` round-trips all three states through a Bundle-free map representation and an instrumented round trip through a real `Bundle`

## 2. Service

- [x] 2.1 Host the engine in `PlaybackService`: `MediaSession.Callback` advertising and handling the custom commands, engine actions over the ExoPlayer, `onMediaItemTransition(AUTO)` → `onTrackEnded`, and state published via `setSessionExtras`; verify `PlaybackServiceTest` additions with the 3 s tone: a 4 s timer reports `Countdown` then pauses at about 4 s with volume 1.0 afterwards; end-of-track pauses at the transition with the next item current at position 0; extend pushes the end; cancel leaves playback running; the state survives releasing and reconnecting a controller

## 3. Bridge and ViewModel

- [x] 3.1 Add `setSleepTimer(durationMs)`, `setSleepTimerEndOfTrack()`, `extendSleepTimer(ms)`, `cancelSleepTimer()` to `PlayerBridge`/`PlayerConnection` (custom commands) and a `MediaController.Listener` that maps extras into `PlayerState.sleepTimer` (also read on connect); add the four actions to `PlayerActions`/`PlayerViewModel`; verify `PlayerViewModelTest` additions forward each action

## 4. UI

- [x] 4.1 Create `ui/nowplaying/SleepTimerSheet.kt` (presets, custom minutes 1–600 with Set, End of current track; active mode with remaining time, Extend 15 min, Cancel timer) and make the Now Playing chip live (labels per state, accent styling when active, disabled without a queue); verify `SleepTimerSheetTest` (preset/custom/end-of-track callbacks, invalid custom disables Set, active mode buttons) and `NowPlayingScreenTest` additions (chip labels for Off / Countdown / EndOfTrack, countdown ticks down, chip opens the sheet)

## 5. Integration and docs

- [x] 5.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify `BUILD SUCCESSFUL`, every test passes, merged manifest still 0 hits for `INTERNET`
- [x] 5.2 Manual walkthrough on both emulators: set a custom 1-minute timer → chip counts down → volume audibly fades (`dumpsys media_session` volume / `dumpsys audio`) from ~50 s → paused at ~60 s → play resumes at full volume; "End of current track" on a 35 s song → paused at the transition; extend and cancel; Home for the whole duration → still fires; `adb shell dumpsys deviceidle force-idle` during a 1-minute timer → still fires; screenshots of the chip states and the sheet; note deviations here
- [x] 5.3 Update `Build-plan/README.md` (row 6 ✅), `Build-plan/phases/06-sleep-timer.md` (status, result, task progress) and `Build-plan/decisions.md` (D-43…); commit on `main` and push; verify `git status` is clean and `git log -1` shows the commit
