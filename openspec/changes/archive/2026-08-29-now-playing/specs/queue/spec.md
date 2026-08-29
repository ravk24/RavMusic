## Purpose

The queue sheet: seeing what will play next, jumping ahead, and reordering what is coming.

## ADDED Requirements

### Requirement: Contents and order
The queue sheet SHALL list the current song first, highlighted, followed by every song that will play after it in the order it will play — the shuffled order when shuffle is on, the queue order otherwise. Its header SHALL read "Queue · N left" with N the number of songs after the current one. Each row SHALL show the title and the artist or "Unknown artist".

#### Scenario: Linear queue
- **WHEN** a five-song folder queue is on its second song with shuffle off
- **THEN** the sheet lists song 2 (highlighted) then songs 3, 4, 5, and the header reads "Queue · 3 left"

#### Scenario: Shuffled queue
- **WHEN** shuffle is on
- **THEN** the rows after the current song are in the shuffled play order, not the folder order

### Requirement: Jump
Tapping a row SHALL start that song immediately; the songs after it in the sheet remain next.

#### Scenario: Tap a later song
- **WHEN** the user taps the fourth row
- **THEN** that song starts playing, Now Playing shows it, and the sheet highlights it

### Requirement: Reorder
Dragging a row by its handle SHALL move it to the drop position among the upcoming songs, and the new order SHALL be the order that plays. Dragging a song to the position right after the current one SHALL make it play next. The current song SHALL keep playing without interruption when shuffle is off.

#### Scenario: Make a song play next
- **WHEN** the user drags the last row to directly below the current song and lets the current song end
- **THEN** the dragged song plays next

### Requirement: Stays in sync
The sheet SHALL reflect the session as it changes from any source: when a song ends, the next row becomes the highlighted current one and the count decreases; when the queue is replaced the sheet shows the new queue.

#### Scenario: Advances
- **WHEN** the current song ends while the sheet is open
- **THEN** the next row is highlighted and "N left" decreases by one
