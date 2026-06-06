# vp-cerca-intermedio-toggle Specification

## Purpose

Toggle between Cerca (near vision) and Intermedio (intermediate distance) modes in the VP card of the Refracción section, updating the card title, DIP label, and DIP bound value accordingly. Purely a UI state change — no new database fields.

## Requirements

### Requirement: Card Title Update

The VP card title SHALL display "VP Cerca/Intermedio" instead of "Adición (ADD)".

#### Scenario: Updated title on render

- GIVEN the evaluation screen is displayed
- WHEN the Refracción section renders the VP card
- THEN the card title reads "VP Cerca/Intermedio"
- AND "Adición (ADD)" no longer appears as the card title

### Requirement: Cerca/Intermedio Toggle

The system SHALL provide a Switch toggle for Cerca/Intermedio inside the VP card, defaulting to Cerca. The Adición section with its A/O toggle SHALL appear below this new toggle.

#### Scenario: Toggle defaults to Cerca

- GIVEN the evaluation screen loads
- WHEN the VP card renders
- THEN a Cerca/Intermedio Switch appears below the card title
- AND the Switch defaults to Cerca
- AND `isVpCerca` in `EvaluacionUiState` is `true`
- AND the Adición section (with A/O toggle) appears below the new Switch

#### Scenario: Toggle to Intermedio

- GIVEN the Switch is in Cerca position
- WHEN the optometric professional toggles to Intermedio
- THEN `isVpCerca` in `EvaluacionUiState` becomes `false`

### Requirement: DIP Label and Value Binding

The DIP section second field SHALL bind its label and value to `isVpCerca`.

#### Scenario: DIP in Cerca mode

- GIVEN `isVpCerca` is `true`
- WHEN the DIP section renders
- THEN the second field label reads "DIP Cerca"
- AND the bound value is `dipCerca`

#### Scenario: DIP in Intermedio mode

- GIVEN `isVpCerca` is `false`
- WHEN the DIP section renders
- THEN the second field label reads "DIP Intermedio"
- AND the bound value is `dipIntermedio`

#### Scenario: Toggle updates DIP in real time

- GIVEN the DIP section shows "DIP Cerca" with `dipCerca` value
- WHEN the optometric professional toggles to Intermedio
- THEN the label updates to "DIP Intermedio"
- AND the displayed value switches to `dipIntermedio`
- WHEN toggled back to Cerca
- THEN the label reverts to "DIP Cerca"
- AND the displayed value reverts to `dipCerca`

### Requirement: UI State Flag

`isVpCerca: Boolean = true` SHALL be added to `EvaluacionUiState`. This flag MUST NOT persist to the database.

#### Scenario: Default on evaluation load

- GIVEN any evaluation loads (new or existing)
- WHEN `EvaluacionUiState` is initialized
- THEN `isVpCerca` is `true`

#### Scenario: No database persistence

- GIVEN `isVpCerca` has been toggled to `false`
- WHEN the evaluation screen is closed or saved
- THEN `isVpCerca` is NOT written to the database
- AND the flag reverts to `true` on next evaluation load
