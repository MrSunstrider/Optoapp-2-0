# Tasks: fix-dispensacion-issues

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~220 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Remove stock filter from montura dropdown (Bug 1)

- [ ] 1.1 (RED) Write unit test in `DispensacionViewModelVentaTest.kt` proving `monturasActivas` StateFlow includes monturas with `stockActual=0` — documents that the ViewModel does NOT filter them
- [ ] 1.2 (GREEN) Remove `.filter { it.stockActual > 0 }` (lines 164-171) and `if (isSelected) emptyList()` guard (lines 156-158) in `LenteForm.kt`
- [ ] 1.3 (GREEN) Apply same filter removal in `DispensacionFormSections.MonturaInfoSection` (lines 56-68)
- [ ] 1.4 (REFACTOR) Verify all monturas appear regardless of stock; re-selection works after query clear; save-time validation still blocks zero-stock selections

## Phase 2: Stabilize generatedId + prevent crash (Bug 2)

- [ ] 2.1 (RED) Write test in `DispensacionViewModelCharacterizationTest.kt` proving `generatedId` is stable across multiple `.copy()` calls — asserts same value after 3+ state updates
- [ ] 2.2 (RED) Write test proving comma-format input `"1500,50"` parses to `1500.50` in monto-total save path
- [ ] 2.3 (GREEN) Move `generatedId` from data class default (`UUID.randomUUID()`) to ViewModel `init` block; remove default from `DispensacionUiState`
- [ ] 2.4 (GREEN) Add `.replace(",", ".")` in `saveDispensacion()` monto-total parse at line 288
- [ ] 2.5 (GREEN) Widen catch clause in save flow to `Exception` (line ~440) to prevent UI crash from any source
- [ ] 2.6 (REFACTOR) Verify generatedId stable across recomposition; comma-to-dot handles Spanish locale input; try-catch prevents crash surface

## Phase 3: Fix montura_movimientos dedup for composite key (Bug 3)

- [ ] 3.1 (RED) Write test in `SyncInventarioUseCaseKtTest.kt` proving current `distinctBy { it.id }` passes duplicate composite keys `(referenciaId, tipo, monturaId)` with different UUIDs
- [ ] 3.2 (RED) Write test proving `ConflictHelper.detectConflictMovimientos()` is called before upload (mock verification)
- [ ] 3.3 (GREEN) Change `distinctBy { it.id }` → `distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }` in `SyncInventarioUseCase.kt` line ~118
- [ ] 3.4 (GREEN) Add `conflictHelper.filterConflictMovimientos()` call before upload + reconcile local→remote IDs by composite key (follows `uploadMonturas` pattern)
- [ ] 3.5 (REFACTOR) Verify no duplicate uploads for same composite key with different UUIDs; no Supabase error 23505 on edit+sync cycle

## Dependencies

All three phases are independent — no cross-phase coupling. Can be executed in any order or in parallel branches.
