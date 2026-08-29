## Purpose

Stopping playback by itself after a chosen time or at the end of the current song, gently, so the user can fall asleep to music and resume it later without ceremony.

## ADDED Requirements

### Requirement: Setting a timer
From Now Playing the user SHALL be able to start a sleep timer of 15, 30, 45 or 60 minutes, a custom whole number of minutes (1–600), or "End of current track". Setting a timer while one is active SHALL replace it. The timer SHALL only be offered while a queue is loaded.

#### Scenario: Preset
- **WHEN** the user opens the sleep-timer picker and taps "30 min"
- **THEN** a 30-minute countdown starts and the chip shows the remaining time

#### Scenario: Custom minutes
- **WHEN** the user enters "12" as custom minutes and confirms
- **THEN** a 12-minute countdown starts; entering 0 or a non-number leaves the confirm action unavailable

#### Scenario: Replace
- **WHEN** a 30-minute timer is running and the user picks "15 min"
- **THEN** the countdown restarts at 15 minutes

### Requirement: Fade then pause
During the last 10 seconds of a countdown the playback volume SHALL fade smoothly to zero. When the countdown reaches zero the session SHALL pause (not stop): the queue, the current song and its position SHALL be kept, and the volume SHALL be restored to its previous level so that resuming plays at normal volume. The timer SHALL then be off.

#### Scenario: One-minute timer
- **WHEN** a 1-minute timer is set while a song is playing
- **THEN** the volume starts dropping about 50 s in, playback pauses at about 60 s, and pressing play afterwards resumes the same song at the position it paused, at full volume

#### Scenario: Paused by the user first
- **WHEN** the user pauses manually before the countdown ends and the countdown then reaches zero
- **THEN** playback stays paused and the volume is at full when the user resumes

### Requirement: End of current track
"End of current track" SHALL pause the session exactly when the current song ends, before the next song starts playing, with the next song cued at its beginning. The timer SHALL then be off. If the queue ends first, nothing further happens.

#### Scenario: Pause at the transition
- **WHEN** "End of current track" is set with 20 s left in the current song
- **THEN** after about 20 s the session is paused, the next song is the current item at position 0, and nothing is heard from it

### Requirement: Extend and cancel
While a countdown is active the user SHALL be able to extend it by 15 minutes or cancel it. Cancelling SHALL leave playback untouched and restore the volume if a fade had begun. Extending during the fade SHALL restore the volume and continue playing.

#### Scenario: Extend
- **WHEN** a timer shows 02:10 remaining and the user taps "Extend 15 min"
- **THEN** the chip shows about 17:10 remaining

#### Scenario: Cancel during the fade
- **WHEN** the volume is fading and the user cancels the timer
- **THEN** the volume returns to full immediately and playback continues

### Requirement: Chip shows the state
The Now Playing chip SHALL read "Sleep timer" when no timer is set, "Sleep · mm:ss · tap to extend" with a live countdown while a countdown is active, and "Sleep · end of track" while end-of-track is set. Tapping the chip SHALL open the picker; while a timer is active the picker SHALL also show the remaining time and offer extend and cancel.

#### Scenario: Live countdown
- **WHEN** a 15-minute timer has been running for one minute
- **THEN** the chip reads about "Sleep · 14:00 · tap to extend" and keeps counting down while the screen is visible

#### Scenario: After the timer fires
- **WHEN** the countdown reaches zero and playback pauses
- **THEN** the chip reads "Sleep timer" again

### Requirement: Runs without the app
The timer SHALL keep running while the app is in the background or its activity is destroyed, and SHALL still fade and pause on time. Reopening the app SHALL show the timer's current state.

#### Scenario: Backgrounded for the whole duration
- **WHEN** a 1-minute timer is set and the user presses Home immediately
- **THEN** playback pauses about a minute later and reopening the app shows no timer set

#### Scenario: Reopen mid-countdown
- **WHEN** the activity is destroyed and recreated while a countdown runs
- **THEN** the chip shows the remaining time of the running countdown
