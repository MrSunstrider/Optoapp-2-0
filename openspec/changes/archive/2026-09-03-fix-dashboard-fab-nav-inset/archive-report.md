# Archive Report: fix-dashboard-fab-nav-inset

**Archived:** 2026-09-03  
**Destination:** `openspec/changes/archive/2026-09-03-fix-dashboard-fab-nav-inset/`

## Summary

Dashboard FAB speed dial on `OperacionHoyScreen` now applies `navigationBarsPadding()` so it clears the system navigation bar when Scaffold content insets are zeroed. Main spec `system-insets` gained the FAB clearance requirement.

## Artifacts

- exploration.md, proposal.md, design.md, tasks.md, verify-report.md
- specs/system-insets/spec.md (delta merged into main)

## Code

- `OperacionHoyScreen.kt` — FAB Column `Modifier.navigationBarsPadding()`
- `OperacionHoyScreenTest.kt` — characterization test for FAB padding
