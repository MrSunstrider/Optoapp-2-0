# Design Tokens Specification

## Purpose

Define the visual foundation layer for OptoApp's design system: color palette, shape system, spacing scale, and typography tokens. All tokens integrate with Material 3 and serve as the single source of truth for the entire component library and screen migration.

## Requirements

### Requirement: Color Token Definitions

The system SHALL define a complete color palette as Kotlin `Color` constants in `OptoTokens.kt`, covering both light and dark themes. The palette SHALL replace all existing `Color.kt` definitions. Each token MUST map to exactly one M3 `ColorScheme` slot.

| Token | Light | Dark | M3 Slot |
|-------|-------|------|---------|
| primary | #6D4AFF | #9B8AFF | `primary` |
| onPrimary | #FFFFFF | #1A0F3D | `onPrimary` |
| primaryContainer | #EDE8FF | #2D1F6E | `primaryContainer` |
| secondary | #3DD9A5 | #6EE7B7 | `secondary` |
| background | #F5F7FA | #0B1220 | `background` |
| surface | #FFFFFF | #172033 | `surface` |
| surfaceVariant | #E8EAF0 | #1E293B | `surfaceVariant` |
| onSurfaceVariant | #475569 | #94A3B8 | `onSurfaceVariant` |
| outline | #CBD5E1 | #334155 | `outline` |
| outlineVariant | #E2E8F0 | #1E293B | `outlineVariant` |
| error | #DC2626 | #F87171 | `error` |

#### Scenario: All M3 color slots filled

- GIVEN the app starts in either light or dark theme
- WHEN `MaterialTheme.colorScheme` is accessed
- THEN every required M3 slot (`primary`, `onPrimary`, `primaryContainer`, `secondary`, `background`, `surface`, `surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `error`, `inverseSurface`, `scrim`) SHALL have a non-null value
- AND no slot SHALL use M3 default fallback values

#### Scenario: Zero visual regression after token swap

- GIVEN the app renders any screen in its current state
- WHEN `OptoTokens` colors replace `Color.kt` values in `Theme.kt`
- THEN every screen SHALL render with identical visual output (no color changes visible)

### Requirement: Shape Token Definitions

The system SHALL define shape tokens as `RoundedCornerShape` objects in `OptoTokens.kt` and register them in the M3 `Shapes` configuration.

| Token | Value | Usage |
|-------|-------|-------|
| small | 12.dp | Chips, small elements |
| medium | 16.dp | Cards, buttons, inputs |
| large | 24.dp | Dialogs, bottom sheets |

#### Scenario: Shapes registered in MaterialTheme

- GIVEN the app composes any screen
- WHEN `MaterialTheme.shapes` is accessed
- THEN `small`, `medium`, and `large` SHALL match the defined token values
- AND `MaterialTheme.shapes.medium` SHALL equal `RoundedCornerShape(16.dp)`

#### Scenario: Zero hardcoded RoundedCornerShape values after migration

- GIVEN the full codebase after Phase 4
- WHEN searching for `RoundedCornerShape(` in all `.kt` files under `ui/`
- THEN zero matches SHALL exist outside `OptoTokens.kt`

### Requirement: Spacing Token Definitions

The system SHALL define a spacing scale as `Dp` constants in `OptoTokens.kt`. The scale SHALL be: 4, 8, 12, 16, 24, 32 dp. These are used by components and screen layouts for consistent padding and gaps.

#### Scenario: Spacing constants accessible

- GIVEN a composable needs spacing
- WHEN referencing `OptoTokens.spacing.xs` (4.dp) through `OptoTokens.spacing.xxxl` (32.dp)
- THEN the value SHALL be the exact defined `Dp` amount

### Requirement: Theme Integration

`OptoAppTheme` in `Theme.kt` SHALL use the new `OptoTokens` color scheme and shapes. The function signature and behavior (dark mode toggle, status bar) SHALL remain unchanged.

#### Scenario: Theme accepts dark parameter

- GIVEN `OptoAppTheme(darkTheme = true)`
- WHEN content composes
- THEN `MaterialTheme.colorScheme.background` SHALL equal `#0B1220` (dark background token)

#### Scenario: Dynamic color remains disabled

- GIVEN any device configuration
- WHEN `OptoAppTheme` composes
- THEN `dynamicColor` SHALL NOT be used — the custom palette SHALL always apply
