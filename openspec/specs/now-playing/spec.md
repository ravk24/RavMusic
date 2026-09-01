# now-playing Specification

## Purpose
The full-screen player: what the user sees and can do with the song that is playing — title, artist, where it came from, seeking, skipping, play/pause — and how the screen opens from and returns to the rest of the app.

## Requirements

### Requirement: Opens from the mini player and closes back to where the user was
Tapping the mini player SHALL open the Now Playing screen as a full-screen destination above the current screen, sliding up over it. While it is open, neither the bottom navigation bar nor the mini player SHALL be shown. A collapse affordance and the system back action SHALL both return to exactly the screen the user came from, sliding the player down, with the mini player visible again; on devices with predictive back the player SHALL follow the back gesture. If the queue is cleared or ends while the screen is open, the screen SHALL close by itself.

#### Scenario: Open from a tab
- **WHEN** a song is playing and the user taps the mini player on the Playlists tab
- **THEN** the Now Playing screen slides up and is shown with no bottom navigation bar and no mini player

#### Scenario: Collapse
- **WHEN** the user taps the collapse affordance or presses system back on Now Playing
- **THEN** the player slides down and the previous screen is shown with the mini player docked and the bottom bar back where it was

#### Scenario: Queue ends while open
- **WHEN** the last song ends (repeat off) while Now Playing is open
- **THEN** the screen closes and the previous screen is shown without a mini player

#### Scenario: Mid-transition both screens exist
- **WHEN** the open animation is in progress
- **THEN** the previous screen is still composed underneath the incoming player until the animation finishes

### Requirement: Content
The screen SHALL show the song's title, its artist or "Unknown artist", and the label "Playing from" with the queue's origin (the folder or playlist name). It SHALL NOT show album art or any placeholder for it. The content SHALL follow the current song as the queue advances or the user skips.

#### Scenario: Song details
- **WHEN** "Copper Sky" by "Nocturne Ave" from the playlist "Late night" is playing
- **THEN** the screen shows "Copper Sky", "Nocturne Ave", "Playing from" and "Late night"

#### Scenario: Follows the queue
- **WHEN** the song ends and the next one starts while the screen is open
- **THEN** the title and artist change to the new song and the elapsed time restarts

### Requirement: Seek bar
The screen SHALL show a seek bar with the elapsed time on the left and the total duration on the right, both as minutes:seconds (hours:minutes:seconds from an hour). While the user drags the bar the elapsed label SHALL show the scrub position; releasing SHALL seek the session to that position. While the screen is visible and a song is playing, the bar and elapsed label SHALL refresh at least four times a second.

#### Scenario: Drag to seek
- **WHEN** the user drags the seek bar to roughly the middle of a 3:41 song and releases
- **THEN** playback continues from about 1:50 and the elapsed label shows about 1:50

#### Scenario: Elapsed advances
- **WHEN** a song is playing and the screen is visible for two seconds
- **THEN** the elapsed label has advanced by about two seconds

### Requirement: Transport controls
The screen SHALL provide previous, play/pause and next. Play/pause SHALL reflect the session's playing state from any source (notification, headset, mini player) and toggle it. Next SHALL start the next song in play order; previous SHALL restart the current song or go to the previous one according to the session's standard behaviour (previous song when near the start of the current one).

#### Scenario: Skip
- **WHEN** the user taps next
- **THEN** the next song in the queue starts and the screen shows it

#### Scenario: Pause reflected
- **WHEN** the user pauses from the notification while Now Playing is open
- **THEN** the play/pause control shows "play"

### Requirement: Secondary chips
Below the controls the screen SHALL show a queue chip reading "Queue · N left" (N = songs after the current one in play order) that opens the queue sheet, a sleep-timer chip whose label and behaviour are specified by the `sleep-timer` capability ("Sleep timer" when off, the live remaining time or "end of track" when active; tap opens the picker), and an equalizer chip reading "Equalizer" that opens the equalizer surface specified by the `equalizer` capability.

#### Scenario: Queue chip count
- **WHEN** a 5-song queue is on its second song
- **THEN** the chip reads "Queue · 3 left"

#### Scenario: Sleep chip is inert
- **WHEN** the user taps the sleep-timer chip
- **THEN** the chip is no longer inert: the sleep-timer picker opens

#### Scenario: Equalizer chip opens the surface
- **WHEN** the user taps the equalizer chip on Now Playing
- **THEN** the equalizer surface opens over the player
