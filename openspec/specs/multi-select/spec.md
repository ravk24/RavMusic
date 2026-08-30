# multi-select Specification

## Purpose
Selecting many songs quickly in a folder and adding them to a playlist — the "VLC fix" the app exists for (spec F2).

## Requirements

### Requirement: Entering and leaving selection
Long-pressing a song row in a folder detail SHALL enter selection mode with that song selected. Keeping the finger down after the long-press and dragging up or down the list SHALL select the contiguous range of rows between the long-pressed row and the row under the finger; dragging back towards the anchor SHALL shrink the range again, and rows selected before the drag SHALL stay selected. In selection mode every row SHALL show a checkbox, tapping a row SHALL toggle it, and the top bar SHALL be replaced by a contextual bar showing "N selected", a "Select all N" action and a close action; the swap between the bars SHALL be animated. While a search filter is active, the range drag and "Select all N" SHALL apply to the rows shown, N SHALL be the number of rows shown, and the selection bar SHALL take precedence over the search bar; leaving selection SHALL return to the search with its text intact. Closing the bar or pressing system back SHALL leave selection mode with nothing selected. Selection SHALL survive scrolling within the screen and SHALL NOT survive leaving the screen.

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

#### Scenario: Drag selects a range
- **WHEN** the user long-presses the first row and, without lifting, drags down to the third row
- **THEN** rows one to three are checked and the bar reads "3 selected"

#### Scenario: Dragging back shrinks the range
- **WHEN** during that drag the finger moves back up to the second row
- **THEN** only rows one and two are checked and the bar reads "2 selected"

#### Scenario: Drag adds to an existing selection
- **WHEN** row five is already selected and the user long-presses row one and drags to row two
- **THEN** rows one, two and five are checked and the bar reads "3 selected"

#### Scenario: Select all while filtering
- **WHEN** a search filter shows 2 of a folder's 3 songs and the user long-presses one and taps "Select all 2"
- **THEN** the bar reads "2 selected"; pressing back ends selection and shows the search bar with its text still typed, and pressing back again closes the search

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
