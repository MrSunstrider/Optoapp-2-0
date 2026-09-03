# System Insets

## Requirements

### Requirement: TopAppBar Inset Handling

The `OptoTopAppBar` component MUST use `TopAppBarDefaults.windowInsets` instead of zeroed `WindowInsets(0, 0, 0, 0)`, allowing Material 3's default inset behavior to apply automatically.

#### Scenario: Status bar does not overlap TopAppBar

- GIVEN a screen using `OptoTopAppBar` with edge-to-edge enabled
- WHEN the screen renders
- THEN the TopAppBar content (title, navigation icon) SHALL appear below the status bar
- AND no content SHALL be rendered behind the system status bar

#### Scenario: All screens using OptoTopAppBar inherit correct insets

- GIVEN ~25 screens that include `OptoTopAppBar`
- WHEN any of these screens is displayed
- THEN the TopAppBar SHALL respect the system status bar height
- AND no additional inset logic SHALL be required per-screen for the app bar area

### Requirement: Scaffold Content Window Insets

Scaffold composables MUST NOT override `contentWindowInsets` with `WindowInsets(0, 0, 0, 0)`. Screens SHALL omit this parameter or pass `ScaffoldDefaults.contentWindowInsets` to inherit Material 3 defaults.

#### Scenario: Scaffold respects default content insets

- GIVEN a screen with a `Scaffold` composable that previously set `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- WHEN the Scaffold renders its content area
- THEN the content SHALL be padded below the status bar and above the navigation bar
- AND scrollable content SHALL be fully visible without overlap

#### Scenario: Configuracion and Agenda screens fix content padding

- GIVEN `ConfiguracionScreen` or `AgendaScreen`
- WHEN either screen renders its Scaffold
- THEN `contentWindowInsets` SHALL NOT be zeroed
- AND content items SHALL appear with proper top padding from the status bar

### Requirement: Non-Scaffold Screen Inset Handling

Screens without a Scaffold composable MUST apply `Modifier.statusBarsPadding()` to their root layout to prevent content from rendering behind the status bar.

#### Scenario: Login screen content below status bar

- GIVEN `LoginScreen` with no Scaffold wrapper
- WHEN the login form renders
- THEN the root layout SHALL apply `statusBarsPadding()`
- AND all form elements SHALL appear below the status bar

#### Scenario: PIN screens respect status bar

- GIVEN `PinScreen` or `CreatePinScreen` with no Scaffold wrapper
- WHEN the PIN entry UI renders
- THEN the root layout SHALL apply `statusBarsPadding()`
- AND the PIN input and buttons SHALL be fully visible below the status bar

### Requirement: Scaffold FAB Navigation Bar Clearance

When a `Scaffold` zeros or omits system insets via `contentWindowInsets = WindowInsets(0, 0, 0, 0)` (or equivalent), any `floatingActionButton` content MUST apply `Modifier.navigationBarsPadding()` (or equivalent navigation-bar inset padding) so the FAB does not render under the system navigation bar.

#### Scenario: Dashboard speed dial clears navigation bar

- GIVEN `OperacionHoyScreen` with a Scaffold that sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
- AND a `floatingActionButton` speed dial (`Column` with main FAB and optional mini FABs)
- WHEN the Dashboard renders on a device with a visible system navigation bar
- THEN the FAB column SHALL apply `navigationBarsPadding()`
- AND the main FAB bottom edge SHALL sit at or above the system navigation bar top
- AND no part of the FAB SHALL be drawn under the navigation bar buttons/gesture area

#### Scenario: Other FAB screens already compliant remain valid

- GIVEN screens such as `PacientesListScreen`, `GastosScreen`, or `ServiciosExtraScreen` that already pad their FAB
- WHEN those screens render
- THEN they SHALL continue to satisfy this requirement without further change
