# playback Specification

## Purpose
Background playback of a queue of songs: the playback service, the system's media controls and notification, audio-focus behaviour, how a queue is built from a folder, and how the UI reconnects to a session that is already running.

## Requirements

### Requirement: Playback runs in a foreground media service
Audio SHALL be played by a media session service, not by the activity, so that playback continues when the app is backgrounded, the screen is locked, or the activity is destroyed. While something is playing the service SHALL be a foreground service of type media playback. When playback is paused and the app's task is removed, the service SHALL stop itself.

#### Scenario: Backgrounded while playing
- **WHEN** a song is playing and the user presses Home or locks the screen
- **THEN** the audio keeps playing without interruption

#### Scenario: Activity destroyed while playing
- **WHEN** a song is playing and the activity is destroyed (e.g. swiped from recents) and the app is opened again
- **THEN** the song is still playing and the app shows it as the current song with the correct playing state

#### Scenario: Task removed while paused
- **WHEN** playback is paused and the user swipes the app away from recents
- **THEN** the service stops and the media notification is removed

### Requirement: System media controls
While a queue is loaded the app SHALL show the system media notification with play/pause, next and previous, and the same controls SHALL work from the lock screen and from wired/Bluetooth headset buttons. Tapping the notification SHALL open the app.

#### Scenario: Notification controls
- **WHEN** a song is playing and the user taps pause in the notification
- **THEN** playback pauses and the notification and the app both show the paused state

#### Scenario: Headset button
- **WHEN** a song is playing and a headset play/pause button is pressed
- **THEN** playback toggles accordingly

#### Scenario: Notification tap
- **WHEN** the user taps the media notification
- **THEN** the app's activity is brought to the front

### Requirement: Audio focus and noisy audio
The app SHALL request audio focus for music playback: it SHALL pause for a phone call or another app taking exclusive focus, duck or pause for transient focus loss, and resume after a transient loss when appropriate. When headphones are unplugged (audio becoming noisy) playback SHALL pause, not stop, so that resume works normally.

#### Scenario: Headphones unplugged
- **WHEN** a song is playing and the headphones are disconnected
- **THEN** playback pauses, the queue and position are kept, and pressing play resumes from the same position

#### Scenario: Another app takes focus
- **WHEN** a song is playing and another app starts exclusive audio playback
- **THEN** this app pauses

### Requirement: Queue built from a folder
Tapping a song in a folder SHALL replace the queue with that folder's songs in their displayed order, starting at the tapped song, and start playing with shuffle off. Each queued item SHALL carry the song's title, artist and origin so that the current song and "Playing from: <folder name>" can be shown. Songs that follow SHALL play automatically in order; what happens after the last song is governed by the repeat mode (`shuffle-repeat`): with repeat off, playback stops at the end of the queue.

#### Scenario: Tap a song mid-folder
- **WHEN** the "Music" folder lists "alpha song", "Glass Rain", "Zebra" and the user taps "Glass Rain"
- **THEN** "Glass Rain" starts playing, followed automatically by "Zebra", and the origin is "Music"

#### Scenario: Tap replaces the queue
- **WHEN** a song from "Rock" is playing and the user taps a song in "Download"
- **THEN** the Download queue replaces the Rock queue and the tapped song plays

### Requirement: Missing files are skipped
If a queued song's file can no longer be opened when its turn comes, playback SHALL skip to the next song in the queue instead of stopping. If it was the last song in the queue, playback SHALL stop as at the end of the queue.

#### Scenario: Deleted file mid-queue
- **WHEN** the second of three queued songs has been deleted from storage and the first song ends
- **THEN** the third song starts playing

#### Scenario: Deleted last file
- **WHEN** the last queued song has been deleted and the previous song ends
- **THEN** playback stops without an error being shown

### Requirement: UI reflects the session
The app SHALL expose the current song (title, artist, origin), whether playback is playing, the current position and duration, whether a queue is loaded, the shuffle state, the repeat mode, the queue in play order and the current song's position in it, updated as they change from any source (app, notification, headset). Position SHALL be refreshed at least twice a second while playing and the app is visible.

#### Scenario: Paused from the notification
- **WHEN** the user pauses from the notification while the app is visible
- **THEN** the app shows the paused state without any user action in the app

#### Scenario: Progress advances
- **WHEN** a song is playing and the app is visible
- **THEN** the displayed progress advances continuously

#### Scenario: Shuffle and repeat are exposed
- **WHEN** shuffle is turned on and repeat set to All from the Now Playing screen
- **THEN** the exposed state reports shuffle on, repeat All, and a queue whose order matches what will play
