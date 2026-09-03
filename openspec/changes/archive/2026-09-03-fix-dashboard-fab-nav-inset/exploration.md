# Exploration: fix-dashboard-fab-nav-inset

## Symptom

On CLK-LX3 (Android 14, OptoApp 1.16.11), Dashboard (`OperacionHoyScreen`) FAB `+` sits under the system navigation bar and is partially tappable / visually clipped.

## Device evidence (2026-09-03)

| Element | Bounds (px) |
|---------|-------------|
| Physical size | 1080 × 2412 |
| `android:id/navigationBarBackground` | `[0,2298][1080,2412]` |
| Main FAB / Acciones rápidas | `[864,2196][1032,2364]` |

Overlap: FAB bottom `2364` − nav top `2298` ≈ **66 px** into the system nav.

## Causal chain

1. App uses edge-to-edge / zeroed Scaffold insets pattern.
2. `OperacionHoyScreen` sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
3. Scroll content applies `.navigationBarsPadding()` — list clears the nav bar.
4. `floatingActionButton` speed-dial `Column` has **no** `navigationBarsPadding()`.
5. Material3 FAB slot therefore anchors to the physical bottom → overlaps gesture/nav bar.

## Related code

- Bug: [`OperacionHoyScreen.kt`](optoapp/src/main/java/com/example/optoapp/ui/screens/OperacionHoyScreen.kt) lines ~60–98
- Correct pattern:
  - [`PacientesListScreen.kt`](optoapp/src/main/java/com/example/optoapp/ui/screens/PacientesListScreen.kt) — FAB `Modifier.navigationBarsPadding()`
  - [`GastosScreen.kt`](optoapp/src/main/java/com/example/optoapp/ui/screens/GastosScreen.kt) — same
  - [`ServiciosExtraScreen.kt`](optoapp/src/main/java/com/example/optoapp/ui/screens/ServiciosExtraScreen.kt) — same

## Existing specs / changes

- Main spec [`openspec/specs/system-insets/spec.md`](openspec/specs/system-insets/spec.md): TopAppBar, Scaffold content, non-Scaffold — **no FAB requirement**.
- Incomplete active change `fix-window-insets-navigation-bar` scoped content/snackbars only; OperacionHoy content already padded; FAB gap **not** in that change. Do **not** reopen.

## Proposed direction

Approach B: add `Modifier.navigationBarsPadding()` to the FAB speed-dial `Column`. Do not remove global `contentWindowInsets = 0` in this change.

## Out of exploration scope

- Removing duplicate “Acciones rápidas” row vs FAB
- Adding Caja to speed dial
- Global Scaffold inset restore (Approach A)
