## MODIFIED Requirements

### Requirement: Content
The screen SHALL show the song's title, its artist or "Unknown artist", and the label "Playing from" with the queue's origin (the folder or playlist name). It SHALL NOT show album art or any placeholder for it. The content SHALL follow the current song as the queue advances or the user skips.

#### Scenario: Song details
- **WHEN** "Copper Sky" by "Nocturne Ave" from the playlist "Late night" is playing
- **THEN** the screen shows "Copper Sky", "Nocturne Ave", "Playing from" and "Late night"

#### Scenario: Follows the queue
- **WHEN** the song ends and the next one starts while the screen is open
- **THEN** the title and artist change to the new song and the elapsed time restarts
