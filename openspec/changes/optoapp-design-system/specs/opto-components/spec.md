# Opto Components Specification

## Purpose

Define the reusable component library for OptoApp: core UI primitives (Phase 2), domain-specific input components (Phase 3), and migration requirements for all 25 screens (Phase 4). Each component MUST use `OptoTokens` for colors, shapes, and spacing.

## Requirements

### Requirement: Core Component Library

The system SHALL provide five core components. Each MUST use `OptoTokens` defaults and support M3 theming.

| Component | Key Params | Defaults |
|-----------|-----------|----------|
| `OptoCard` | `modifier, elevation, shape, colors, onClick?, content` | shape=`shapes.medium`, elevation=`2.dp`, onClick=null |
| `OptoButton` | `text, onClick, modifier, variant: OptoButtonVariant, enabled, icon?` | variant=`Filled`, enabled=`true` |
| `OptoTopAppBar` | `title, modifier, navigationIcon?, onNavigationClick?, actions` | navIcon=null, actions=`{}` |
| `OptoDialog` | `onDismissRequest, title, confirmText, onConfirm, dismissText?, content` | confirm=`"OK"`, dismiss=`"Cancel"` |
| `OptoFilterChip` | `selected, onClick, label, modifier, leadingIcon?` | leadingIcon=null |

`OptoButtonVariant` enum: `Filled`, `Outlined`, `Text`.

#### Scenario: Card uses token shape

- GIVEN `OptoCard` with defaults
- WHEN it renders
- THEN shape SHALL be `MaterialTheme.shapes.medium` (16dp), elevation `2.dp`

#### Scenario: TopAppBar consistent colors

- GIVEN `OptoTopAppBar(title = "Patients")`
- WHEN it renders
- THEN background SHALL be `colorScheme.surface`, title SHALL be `colorScheme.onSurface`

#### Scenario: Button variant colors

- GIVEN `OptoButton(text = "Save", onClick = {}, variant = Filled)`
- WHEN it renders
- THEN background SHALL be `colorScheme.primary`, text SHALL be `colorScheme.onPrimary`

### Requirement: Input Component Library

The system SHALL provide four input components for clinical data entry.

| Component | Key Params | Purpose |
|-----------|-----------|---------|
| `OptoSegmentedSelector` | `options, selectedIndex, onSelect, modifier` | Replace inline segmented controls |
| `OptoQuickAddChip` | `value, isSelected, onClick, modifier` | Quick-add lens powers (+1.00, +2.00, +3.00) |
| `OptoVisionInput` | `value, onValueChange, label, modifier, isError` | Vision acuity entry (20/20 format) |
| `OptoTextField` (upgrade) | Add: `leadingIcon?, maxLength?, showCharCount?` | Leading icon, char count, error states |

#### Scenario: SegmentedSelector selected state

- GIVEN `OptoSegmentedSelector(options = ["Near", "Intermediate"], selectedIndex = 0, onSelect = {})`
- WHEN it renders
- THEN selected option SHALL use `surfaceVariant` background; unselected transparent

#### Scenario: QuickAddChip selection

- GIVEN `OptoQuickAddChip(value = "+2.00", isSelected = true, onClick = {})`
- WHEN it renders
- THEN background SHALL be `primary.copy(alpha = 0.2f)`, text SHALL be `primary`

#### Scenario: Upgraded OptoTextField char count

- GIVEN `OptoTextField(maxLength = 100, showCharCount = true)` with value "hello"
- WHEN it renders
- THEN supporting text SHALL show "5/100"

### Requirement: Screen Migration

All 25 screens SHALL migrate to design system components. Order: TopAppBar → Cards → colors → shapes.

#### Scenario: Zero hardcoded colors

- GIVEN all screens after Phase 4
- WHEN grepping for direct color imports in `ui/screens/` and `ui/components/`
- THEN zero matches SHALL exist outside `OptoTokens.kt` and `Theme.kt`

#### Scenario: Zero hardcoded shapes

- GIVEN all screens after Phase 4
- WHEN grepping for `RoundedCornerShape(` in `ui/screens/` and `ui/components/`
- THEN zero matches SHALL exist outside `OptoTokens.kt`

#### Scenario: All TopBars replaced

- GIVEN all screens after Phase 4
- WHEN grepping for `TopAppBar(` in `ui/screens/`
- THEN zero matches SHALL exist — all SHALL use `OptoTopAppBar`

#### Scenario: Build and tests pass

- GIVEN migration complete
- WHEN `./gradlew :optoapp:testDebugUnitTest --stacktrace` runs
- THEN all existing tests SHALL pass
- AND `./gradlew :optoapp:assembleDebug` SHALL succeed
