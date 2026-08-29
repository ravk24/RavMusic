# multi-select Specification

## Purpose
Selecting many songs quickly in a folder and adding them to a playlist — the "VLC fix" the app exists for (spec F2).

## Requirements

### Requirement: Entering and leaving selection
Long-pressing a song row in a folder detail SHALL enter selection mode with that song selected. In selection mode every row SHALL show a checkbox, tapping a row SHALL toggle it, and the top bar SHALL be replaced by a contextual bar showing "N selected", a "Select all N" action and a close action. Closing the bar or pressing system back SHALL leave selection mode with nothing selected. Selection SHALL survive scrolling within the screen and SHALL NOT survive leaving the screen.

#### Scenario: Long-press starts a selection
- **WHEN** the user long-presses "Beta Song"
- **THEN** selection mode is on, "Beta Song" is checked, and the bar reads "1 selected"

#### Scenario: Tap toggles
- **WHEN** in selection mode the user taps an unselected row and then taps it again
- **THEN** the count goes up by one and then back down

#### Scenario: Select all
- **WHEN** the user taps "Select all 88"
- **THEN** all 88 rows are checked and the bar reads "88 selected"

#### Scenario: Back leaves selection first
- **WHEN** the user presses system back while in selection mode
- **THEN** selection mode ends and the folder detail stays open

#### Scenario: Selection does not survive navigation
- **WHEN** the user selects songs, goes back to Folders and reopens the same folder
- **THEN** nothing is selected

### Requirement: Add to playlist
Selection mode SHALL show a primary action "Add N to playlist ›". Tapping it SHALL open a sheet listing the existing playlists (name and song count) and a "New playlist" entry. Choosing a playlist SHALL add the selected songs to the end of it in the folder's display order; choosing "New playlist" SHALL ask for a name, create it and add the songs. After adding, selection mode SHALL end and a confirmation "Added N to <playlist>" SHALL be shown.

#### Scenario: Add to an existing playlist
- **WHEN** three songs are selected and the user taps "Add 3 to playlist ›" then "Late night"
- **THEN** the three songs are appended to "Late night" in display order, selection ends, and "Added 3 to Late night" is shown

#### Scenario: Add to a new playlist
- **WHEN** the user taps "New playlist" in the sheet, enters "Road trip" and confirms
- **THEN** "Road trip" is created containing the selected songs and appears on the Playlists tab

### Requirement: Duplicates are warned, not silently added
If some selected songs are already in the chosen playlist, the app SHALL say how many ("3 already in this playlist") and offer "Add anyway" and "Skip duplicates". Skip duplicates SHALL add only the songs not yet present; Add anyway SHALL add all of them.

#### Scenario: Skip duplicates
- **WHEN** five songs are selected, three of which are already in the playlist, and the user chooses "Skip duplicates"
- **THEN** exactly two songs are added and the confirmation reads "Added 2 to <playlist>"

#### Scenario: Add anyway
- **WHEN** the same five songs are selected and the user chooses "Add anyway"
- **THEN** five tracks are appended, and the playlist now contains the three duplicates twice
