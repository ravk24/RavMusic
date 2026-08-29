# Phase 6 — Sleep timer

**OpenSpec change:** `sleep-timer` · **Status:** ✅ Done (2026-08-29)

**Result:** Shipped as designed. `assembleDebug` + 76/76 JVM tests green; 92/92 connected tests on API 36 and
the full suite on API 26 (plus a `SleepTimerCommandsBundleTest` parcel round-trip); merged manifest has 0
`INTERNET` hits. Manual walkthrough on both emulators (API 36 `emulator-5556`, API 26 `emulator-5554`):
custom 1-minute timer → chip counts down from `00:58` → `PLAYING` → `PAUSED` at 60 s, chip back to
"Sleep timer", play resumes normally; "End of current track" on a 50 s / 65 s song → paused exactly at the
transition with the next song current at ~0 ms; 15-min preset → `14:58`, Extend → `29:4x`, Cancel → off;
Home + `dumpsys deviceidle force-idle` for a whole 1-minute timer → still paused at 59–60 s, reopening the
app shows the idle chip. Deviations: the timer keeps running (and fires) if the queue ends or the user
stops playback before the countdown does (accepted, D-46); the volume fade cannot be heard on a
`-no-audio` emulator and is verified by `PlaybackServiceTest.sleepTimerFadesPausesAndRestoresVolume`
instead; `screencap` returns a black frame on the API 26 AVD, so screenshots were taken on API 36 only.

## Goal
Fall asleep to music without it playing all night (spec **F6**).

## In scope
- Moon chip in Now Playing → picker: 15 / 30 / 45 / 60 min, custom minutes, **End of current track**
- Timer runs as a coroutine *inside* `PlaybackService` (survives the app being backgrounded), driven by
  a custom `SessionCommand` (`SET_SLEEP_TIMER`, `CANCEL_SLEEP_TIMER`) and reported back via session extras
- Last 10 s: fade volume to 0, then **pause** (not stop) and restore volume so resume works normally
- "End of current track" hooks `onMediaItemTransition`
- Active timer shown in Now Playing ("Sleep · 32:14 · tap to extend"); tap → cancel or extend

## Out of scope
Scheduling for a future time; system alarm integration.

## Capabilities
`sleep-timer` (new)

## Key risks
- Doze / app-standby could delay a long timer → foreground service keeps the process alive; verify
  with `adb shell dumpsys deviceidle force-idle`

## Verification
- Set 1 min (custom): fade starts at 50 s, pause at 60 s, volume restored on resume
- "End of current track" with 20 s left: pauses exactly at the transition
- Background the app for the whole duration: still fires

## Task progress
See `openspec/changes/sleep-timer/tasks.md` (8 tasks, five groups) — moves to
`openspec/changes/archive/` when the change is archived.
