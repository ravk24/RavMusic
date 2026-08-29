## MODIFIED Requirements

### Requirement: Secondary chips
Below the controls the screen SHALL show a queue chip reading "Queue · N left" (N = songs after the current one in play order) that opens the queue sheet, and a sleep-timer chip whose label and behaviour are specified by the `sleep-timer` capability ("Sleep timer" when off, the live remaining time or "end of track" when active; tap opens the picker).

#### Scenario: Queue chip count
- **WHEN** a 5-song queue is on its second song
- **THEN** the chip reads "Queue · 3 left"

#### Scenario: Sleep chip is inert
- **WHEN** the user taps the sleep-timer chip
- **THEN** the chip is no longer inert: the sleep-timer picker opens
