## MODIFIED Requirements

### Requirement: Queue built from a folder
Tapping a song in a folder SHALL replace the queue with that folder's songs in their displayed order, starting at the tapped song, and start playing with shuffle off. Each queued item SHALL carry the song's title, artist and origin so that the current song and "Playing from: <folder name>" can be shown. Songs that follow SHALL play automatically in order; what happens after the last song is governed by the repeat mode (`shuffle-repeat`): with repeat off, playback stops at the end of the queue.

#### Scenario: Tap a song mid-folder
- **WHEN** the "Music" folder lists "alpha song", "Glass Rain", "Zebra" and the user taps "Glass Rain"
- **THEN** "Glass Rain" starts playing, followed automatically by "Zebra", and the origin is "Music"

#### Scenario: Tap replaces the queue
- **WHEN** a song from "Rock" is playing and the user taps a song in "Download"
- **THEN** the Download queue replaces the Rock queue and the tapped song plays

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
