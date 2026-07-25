# Tasks: Fix Patient Card Name Truncation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~80 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Layout Restructure

- [x] 1.1 Wrap the existing `Row(Avatar + InfoColumn + Icons)` in a `Column`, move the icons `Row` below the name/info `Row` in `PacienteCard` (line 284)
- [x] 1.2 Remove icons from the original horizontal `Row`, only keep `Avatar + InfoColumn(weight 1f)` in the top row
- [x] 1.3 Set bottom icon row to `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End)` to push icons right
- [x] 1.4 Change name `Text` composable `maxLines` from `1` to `2` so longer names wrap onto a second line

## Phase 2: Visual Verification

- [ ] 2.1 Launch debug build on emulator, navigate to `PacientesListScreen`, confirm "Katherine Paz Tamara" and "Maria More Arana" render without truncation
- [ ] 2.2 Tap each action icon (eye, inventory, call) — confirm click handlers still fire correctly despite new position
- [ ] 2.3 Take screenshot of card with long name + verify action icons appear below info and aligned to end
