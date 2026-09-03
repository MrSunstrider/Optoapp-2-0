# Delta for System Insets — FAB navigation padding

## ADDED Requirements

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
