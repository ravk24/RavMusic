## Purpose

The app's visual identity: a fixed brand palette that looks the same on every device, a dark variant that follows the system setting, and content that draws edge-to-edge behind translucent system bars.

## ADDED Requirements

### Requirement: Fixed brand palette
The app SHALL use a fixed colour palette with blurple `#635BFF` as the single accent colour. The palette SHALL NOT change based on the device wallpaper or any dynamic-colour mechanism. In light mode surfaces SHALL be white/near-white with navy `#0A2540` as the primary text colour; in dark mode the background SHALL be navy `#0A2540`, elevated surfaces `#0C2E4E`, and the secondary accent `#80E9FF`.

#### Scenario: Palette is independent of wallpaper
- **WHEN** the device wallpaper is changed to a strongly coloured image and the app is reopened
- **THEN** the app's accent colour is still blurple `#635BFF` and its surfaces are unchanged

#### Scenario: Light palette
- **WHEN** the system is in light mode
- **THEN** the app renders white/near-white surfaces, navy primary text, and blurple accents

#### Scenario: Dark palette
- **WHEN** the system is in dark mode
- **THEN** the app renders a navy `#0A2540` background, `#0C2E4E` elevated surfaces, white primary text, and blurple/cyan accents

### Requirement: Dark mode follows the system
The app SHALL follow the system dark-mode setting. A change to the system setting while the app is open SHALL be reflected without restarting the app.

#### Scenario: System toggled while app is open
- **WHEN** the user switches the system between light and dark mode while the app is in the foreground or recents
- **THEN** the app is shown in the matching palette the next time it is visible, without being relaunched

### Requirement: Edge-to-edge content
App content SHALL extend behind the status bar and navigation bar. System bar icons SHALL remain legible in both palettes (dark icons over light surfaces, light icons over dark surfaces), and interactive content SHALL NOT be obscured by system bars.

#### Scenario: Status bar legible in light mode
- **WHEN** the app is shown in the light palette
- **THEN** the status bar icons are dark and readable against the app's light background

#### Scenario: Status bar legible in dark mode
- **WHEN** the app is shown in the dark palette
- **THEN** the status bar icons are light and readable against the app's navy background

#### Scenario: Bottom navigation clears the system navigation bar
- **WHEN** the device uses gesture or button navigation
- **THEN** the app's bottom navigation bar is fully tappable and not overlapped by the system navigation bar
