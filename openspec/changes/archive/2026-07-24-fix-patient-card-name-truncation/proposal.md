# Proposal: Fix Patient Card Name Truncation

## Intent

Patient names in `PacientesListScreen` are truncated with ellipsis ("Katherine Pa…", "Carmen Agui…") because three action icons (144dp) compete for horizontal space in the same `Row` as the name. Users cannot read full patient names at a glance, impairing patient selection during checkout and appointment flows.

## Scope

### In Scope
- Restructure `PacienteCard` layout: move action icons below the name/info column, aligned to end
- Allow name text up to 2 lines (currently `maxLines = 1`)
- Preserve all existing behavior (avatar, info rows, icon actions, click handlers)

### Out of Scope
- No visual redesign beyond the layout restructure
- No changes to other screens or card types
- No behavior changes to icon actions or their visibility logic

## Capabilities

### New Capabilities
None — pure UI layout restructure, no new features.

### Modified Capabilities
None — no spec-level behavior changes.

## Approach

Split `PacienteCard`'s single `Row` layout into a vertical `Column` containing two rows:

1. **Top row**: `Avatar (44dp) + Info Column (weight 1f)` — icons removed from this row, name gets full remaining width.
2. **Bottom row**: `Row(Arrangement.End)` with the 3 action icons (`Visibility`, `Inventory2`, `Call`), each as a 48dp `IconButton`.

Change `maxLines` from `1` to `2` on the name `Text` composable so longer names wrap onto a second line.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/…/ui/screens/PacientesListScreen.kt` (lines 284–322, `PacienteCard`) | Modified | Restructure from single Row to Column layout, adjust name maxLines |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|-------------|
| Icon touch targets shift position | Low | Icons remain same size (48dp), only vertical position changes |
| Card height increases with 2-line names | Low | Cards already scroll vertically; height change is negligible |

## Rollback Plan

Revert the layout in `PacienteCard` to the original single-`Row` structure (lines 284–322). The change is localized to one composable — revert is a single `git checkout` on the file.

## Dependencies

None.

## Success Criteria

- [ ] Patient names render without truncation in `PacientesListScreen` (verify with known long names: "Katherine Paz Tamara", "Maria More Arana")
- [ ] Action icons are visible below the patient info, aligned to end
- [ ] No visual regressions on avatar, info rows, or icon click handlers
