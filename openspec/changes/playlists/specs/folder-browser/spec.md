## MODIFIED Requirements

### Requirement: Folder detail
Tapping a folder SHALL open a detail screen pushed above the tabs, titled with the folder name and showing its song count, listing that folder's songs sorted by title (case-insensitive). Each row SHALL show the title, the artist or "Unknown artist" when the file has no artist tag, and the duration formatted as minutes:seconds (hours:minutes:seconds when an hour or longer). The screen SHALL provide a back affordance; back (affordance or system back) SHALL return to the Folders tab with the tab selected. Tapping a song SHALL start playback of the folder from that song, as specified by the `playback` capability, and the row of the song currently playing SHALL be visually highlighted. Long-pressing a song SHALL enter selection mode as specified by the `multi-select` capability; while selecting, tapping a row toggles its selection instead of playing.

#### Scenario: Open a folder
- **WHEN** the user taps the "Rock" row containing "Beta Song" (41 s, no artist tag)
- **THEN** a screen titled "Rock" with "1 song" is shown, listing "Beta Song", "Unknown artist", "0:41"

#### Scenario: Back returns to Folders
- **WHEN** the user presses the back affordance or system back on a folder detail screen
- **THEN** the Folders tab is shown with the Folders tab selected

#### Scenario: Tagged artist and long duration
- **WHEN** a song has artist "Nocturne Ave" and duration 1 h 2 min 5 s
- **THEN** its row shows "Nocturne Ave" and "1:02:05"

#### Scenario: Song tap is inert
- **WHEN** the user taps a song row
- **THEN** the tap is no longer inert: playback starts as described in "Song tap plays the folder"

#### Scenario: Song tap plays the folder
- **WHEN** the user taps "Glass Rain" in the "Music" folder
- **THEN** "Glass Rain" starts playing with the rest of the folder queued after it, the mini player appears, and the "Glass Rain" row is highlighted as the current song

#### Scenario: Long-press enters selection
- **WHEN** the user long-presses "Glass Rain"
- **THEN** selection mode starts with "Glass Rain" selected and nothing starts playing
