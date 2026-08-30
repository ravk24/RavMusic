## MODIFIED Requirements

### Requirement: Detail screens hide the bottom navigation
A detail screen pushed above a tab (such as a folder's song list, or the search across playlists) SHALL NOT show the bottom navigation bar. Leaving it SHALL return to the tab it was opened from, with that tab selected and the bottom navigation bar visible.

#### Scenario: Detail hides the bar
- **WHEN** the user opens a folder from the Folders tab
- **THEN** the folder detail is shown without the bottom navigation bar

#### Scenario: Back restores the tab
- **WHEN** the user presses back on a folder detail
- **THEN** the Folders tab is shown, selected, with the bottom navigation bar visible

#### Scenario: Search hides the bar
- **WHEN** the user opens search from the Playlists tab
- **THEN** the search screen is shown without the bottom navigation bar, and back returns to the Playlists tab with the bar visible
