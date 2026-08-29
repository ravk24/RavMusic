## Why

Falling asleep to music means the music plays all night unless something stops it. Spec F6 asks for a sleep timer that lives in the playback service (so it survives the app being backgrounded), fades the music out over the last ten seconds, and pauses rather than stops so resuming in the morning just works. The Now Playing screen has carried an inert "Sleep timer" chip since Phase 5 for exactly this.

## What Changes

- **Sleep timer in the playback service**: a countdown that runs inside `PlaybackService` and is driven through the media session with custom session commands (set, extend, cancel, end-of-track). It does not depend on the activity being alive.
- **Presets and custom**: 15 / 30 / 45 / 60 minutes, a custom number of minutes, and **End of current track**.
- **Fade then pause**: during the last 10 s the volume fades to zero; at zero playback pauses (not stops) and the volume is restored so pressing play resumes normally.
- **End of current track** pauses exactly when the current song ends, leaving the next song cued at its start.
- **Now Playing chip** becomes live: "Sleep timer" opens a picker; while active it reads "Sleep · 32:14 · tap to extend" (or "Sleep · end of track") and tapping it offers extend and cancel. The remaining time counts down on screen.
- The timer's state is exposed alongside the rest of the player state so any screen can show it.

Explicitly not in this change: scheduling for a clock time, system-alarm integration, showing the timer in the mini player or the notification.

## Capabilities

### New Capabilities
- `sleep-timer`: setting, extending and cancelling a timer; the fade-and-pause behaviour; end-of-track; what the Now Playing chip and picker show; survival while the app is backgrounded.

### Modified Capabilities
- `now-playing`: the "Secondary chips" requirement — the sleep-timer chip is no longer inert; it opens the picker and shows the active timer (scenario "Sleep chip is inert" kept and rewritten).

## Impact

- `playback/SleepTimerEngine` (pure countdown/fade logic over a small player-actions interface, JVM-tested) hosted by `PlaybackService`; a `MediaSession.Callback` that advertises and handles the custom commands and publishes timer state through session extras.
- `PlayerConnection` gains a `MediaController.Listener` for extras and four commands; `PlayerState` gains `sleepTimer`; `PlayerBridge`, `PlayerActions` and `PlayerViewModel` extended.
- `ui/nowplaying/SleepTimerSheet.kt` and the chip logic in `NowPlayingScreen`; `formatRemaining` in `ui/components/Format.kt`.
- Tests: JVM engine tests with a virtual clock; instrumented service tests (short timers with the 3 s tone: fade → pause → volume restored, end-of-track, extend, cancel, state after reconnect); Compose tests for the sheet and the chip states. No new dependencies or permissions.
