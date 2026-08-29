## MODIFIED Requirements

### Requirement: Placeholder tab content
Until the playlists capability exists, the Playlists tab SHALL show a clearly labelled placeholder (the destination title and a short "nothing here yet" message) rather than a blank screen. The Folders tab shows real content as specified by the `folder-browser` capability.

#### Scenario: Placeholder visible
- **WHEN** the user opens the Playlists tab with the audio permission granted
- **THEN** the tab's title and a placeholder message are visible and the screen is not blank

## ADDED Requirements

### Requirement: Detail screens hide the bottom navigation
A detail screen pushed above a tab (such as a folder's song list) SHALL NOT show the bottom navigation bar. Leaving it SHALL return to the tab it was opened from, with that tab selected and the bottom navigation bar visible.

#### Scenario: Detail hides the bar
- **WHEN** the user opens a folder from the Folders tab
- **THEN** the folder detail is shown without the bottom navigation bar

#### Scenario: Back restores the tab
- **WHEN** the user presses back on a folder detail
- **THEN** the Folders tab is shown, selected, with the bottom navigation bar visible
