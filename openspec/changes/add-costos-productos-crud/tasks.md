# Tasks: Add Create/Delete to Cost Matrix

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~397 (ViewModel 85 + Screen 162 + Test 150) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Phase 1: Foundation — UI State Fields

- [ ] 1.1 Add to `CostosYGastosUiState`: `isCostoDialogVisible`, `creatingCosto`, `costoMaterial`, `costoTipoLente`, `costoStockOFabricacion`, `costoTratamiento`, `costoSerie`, `costoCostoUnitario`, `costoSaveError`, `deletingCosto`

## Phase 2: RED — Failing ViewModel Tests

- [ ] 2.1 Create `CostosYGastosViewModelTest.kt` with MockK setup (mock `CostoProductoDao`, `SessionManager`, `PostSaveSyncScheduler` — following `GastosViewModelTest.kt` pattern)
- [ ] 2.2 Test: `saveCosto` creates entity with UUID, opticaId, vigenteDesde=today, calls `upsertAll`, refreshes block — expect it to fail (method doesn't exist yet)
- [ ] 2.3 Test: `saveCosto` rejects empty material — sets `costoSaveError`
- [ ] 2.4 Test: `saveCosto` rejects empty tipoLente — sets `costoSaveError`
- [ ] 2.5 Test: `saveCosto` rejects costoUnitario <= 0 — sets `costoSaveError`
- [ ] 2.6 Test: `deleteCosto` copies entity with `vigenteHasta=today`, calls `upsertAll`, refreshes block, schedules sync
- [ ] 2.7 Test: `deleteCosto` error handling sets error state

## Phase 3: GREEN — ViewModel Methods

- [ ] 3.1 Add `showNewCosto()` and `dismissCostoDialog()` — set/dismiss dialog state with empty fields, reset error
- [ ] 3.2 Add form updaters: `updateCostoMaterial()`, `updateCostoTipoLente()`, `updateCostoTratamiento()`, `updateCostoSerie()`, `updateCostoCostoUnitario()`
- [ ] 3.3 Add `saveCosto()` — validate fields, build `CostoProductoEntity` (UUID, opticaId, vigenteDesde=today, stockOFabricacion from selectedBlock), call `upsertAll`, refresh block, schedule sync
- [ ] 3.4 Add `confirmDeleteCosto(costo)` and `dismissDeleteDialog()` — set/clear `deletingCosto`
- [ ] 3.5 Add `deleteCosto()` — copy entity with `vigenteHasta = DateUtils.today()`, call `upsertAll`, refresh block, schedule sync, handle errors

## Phase 4: GREEN — Screen UI

- [ ] 4.1 Add FAB to `MatrizDeCostosTab` (same position as Tab 1 FAB) — calls `viewModel.showNewCosto()`
- [ ] 4.2 Add create `AlertDialog` in `MatrizDeCostosTab` — fields: material dropdown, tipoLente dropdown, tratamiento dropdown, serie text field (number), costoUnitario text field (decimal), save/cancel buttons. `stockOFabricacion` auto-filled from selected block
- [ ] 4.3 Add delete icon button to `CostoProductoRow` — calls `viewModel.confirmDeleteCosto(costo)`
- [ ] 4.4 Add delete confirmation `AlertDialog` in `MatrizDeCostosTab` — "¿Eliminar costo?" message, confirm/cancel buttons

## Phase 5: Verify

- [ ] 5.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests pass
