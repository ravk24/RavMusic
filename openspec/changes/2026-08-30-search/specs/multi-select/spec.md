## MODIFIED Requirements

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
