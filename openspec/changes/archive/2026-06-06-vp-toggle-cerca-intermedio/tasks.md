# Tasks: VP Toggle Cerca/Intermedio en Refracción

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 30–50 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | UiState flag + RefraccionSection UI restructure + test update | PR 1 | Single contiguous change; no reason to split |

## Phase 1: Foundation

- [x] 1.1 Add `isVpCerca: Boolean = true` to `EvaluacionUiState` data class in `viewmodel/EvaluacionUiState.kt`

## Phase 2: Core Implementation

- [x] 2.1 Rename card title `"Adición (ADD)"` → `"VP Cerca/Intermedio"` in `AddSection` inside `ui/components/evaluacion/RefraccionSection.kt`
- [x] 2.2 Add Switch toggle Cerca/Intermedio (default Cerca / checked) inside the card, positioned after the title row, above the Adición section
- [x] 2.3 Move the "Adición" subtitle text and A/O Switch row below the new Cerca/Intermedio toggle
- [x] 2.4 DipSection: condition the 2nd field label and value on `uiState.isVpCerca` — `true` → label `"DIP Cerca"`, value `dipCerca`; `false` → label `"DIP Intermedio"`, value `dipIntermedio`

## Phase 3: Testing

- [x] 3.1 Add `assertEquals(true, state.isVpCerca)` to `evaluacionUiState_hasDefaultBooleanValues` in `viewmodel/EvaluacionViewModelTest.kt`
