## MODIFIED Requirements

### Requirement: Dark mode follows the system
The app SHALL follow the system dark-mode setting while the theme override in Settings is **System** (the default). While the override is **Light** or **Dark** the app SHALL render that palette regardless of the system setting. A change to the system setting or to the override while the app is open SHALL be reflected without restarting the app, and the system bar icons SHALL follow the palette actually shown, as specified by "Edge-to-edge content".

#### Scenario: System toggled while app is open
- **WHEN** the override is System and the user switches the system between light and dark mode while the app is in the foreground or recents
- **THEN** the app is shown in the matching palette the next time it is visible, without being relaunched

#### Scenario: Override wins over the system
- **WHEN** the override is Dark and the device is in light mode
- **THEN** the app renders the dark palette and the status-bar icons are light

#### Scenario: Override changed while open
- **WHEN** the user changes the override from Dark to Light in Settings
- **THEN** every screen, including Settings itself and the system bars, switches to the light palette immediately
