# Proposal — fix-dashboard-fab-nav-inset

## Intent

Keep the Dashboard speed-dial FAB fully above the system navigation bar on gesture/3-button devices.

## IN

- `OperacionHoyScreen` FAB column MUST apply `navigationBarsPadding()`.
- Delta requirement on capability `system-insets` for Scaffold FABs when insets are zeroed.
- Strict TDD: characterization test that source of `OperacionHoyScreen` requires `navigationBarsPadding` inside `floatingActionButton`.
- Verify on device (ADB bounds) and `assembleDebug` / focused unit tests.

## OUT

- Removing `contentWindowInsets = WindowInsets(0,0,0,0)` globally (Approach A).
- Changing Acciones rápidas row / adding Caja to dial.
- Supabase schema, RLS, web companion.
- Completing/archiving `fix-window-insets-navigation-bar`.

## Capabilities

- **Modified**: `system-insets` (FAB nav padding requirement).
- **New**: none.

## Rollback

Revert the modifier + test + delta. No data migration.

## Success criteria

- [ ] FAB bottom edge ≤ navigation bar top on CLK-LX3 (or equivalent).
- [ ] Unit characterization test GREEN.
- [ ] `./gradlew :optoapp:assembleDebug` succeeds.
