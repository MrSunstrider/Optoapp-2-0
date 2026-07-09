# Proposal: fix-dispensacion-issues

## Intent

Three bugs degrade the dispensation workflow: (1) frames with `stockActual = 0` are invisible in dropdowns, (2) monto-total input may crash on certain state paths, (3) movement uploads hit duplicate-key errors when editing a dispensation. All block normal daily operation.

## Scope

### In Scope
- Remove stock filter from montura dropdowns in `LenteForm.kt` and `DispensacionFormSections.kt`
- Stabilize `generatedId` lifecycle and add locale-safe monto-total parsing in `DispensacionViewModel.kt`
- Fix sync dedup key and add pre-upload conflict detection in `SyncInventarioUseCase.kt`
- Clean up deprecated `FinancieraInfoSection` dead code

### Out of Scope
- Crash reporting infrastructure (Firebase Crashlytics)
- Full refactor of `DispensacionUiState`
- Supabase schema or RLS changes

## Capabilities

### New Capabilities

None — bug fixes only, no new spec-level capability.

### Modified Capabilities

None — `sync-conflict` spec (FR-05, FR-05b) already describes the intended upload conflict behavior; code will be brought in line.

## Approach

| Bug | Strategy | Rationale |
|-----|----------|-----------|
| **1** — Stock filter | Remove `stockActual > 0` filter from dropdown + `if (isSelected) emptyList()`. Save-time validation catches insufficient stock. | Minimal change, preserves existing UX feedback loop at save. |
| **2** — Crash risk | Move `generatedId` from data class default to ViewModel `init`. Add `value.replace(",", ".")` in monto-total `onValueChange`. Wrap save in try-catch. | Fixes speculative crash with zero schema/DI changes. Comma handling fixes Spanish locale keyboards. |
| **3** — Duplicate key | Change `distinctBy { it.id }` → `distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }`. Call `ConflictHelper.detectConflictMovimientos()` before upload. Reconcile local→remote IDs by composite key. | Directly addresses root cause — dedup was on wrong key (PK vs composite). Matches pattern used by `uploadMonturas`. |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `LenteForm.kt` | Modified | Remove stock filter + `isSelected` block |
| `DispensacionFormSections.kt` | Modified | Remove stock filter + deprecated section cleanup |
| `DispensacionViewModel.kt` | Modified | `generatedId` stabilisation, comma-to-dot, try-catch |
| `SyncInventarioUseCase.kt` | Modified | Fix dedup key, add conflict detection |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Bug 1: user selects zero-stock frame | Low | Save-time validation blocks selection |
| Bug 2: crash root cause different from speculated | Med | Try-catch prevents UI crash regardless. Deploy and collect real logs. |
| Bug 3: ID reconciliation mismatch | Low | Match by composite key + test with real duplicate scenarios |

## Rollback Plan

Per-bug revert — no cross-bug coupling:
- **Bug 1**: Revert `LenteForm.kt` + `DispensacionFormSections.kt`
- **Bug 2**: Revert `DispensacionViewModel.kt`
- **Bug 3**: Revert `SyncInventarioUseCase.kt`

## Dependencies

None — all changes self-contained in Android app. No Supabase schema/RLS impact per project rules.

## Success Criteria

- [ ] Monturas with `stockActual = 0` appear in dispensation dropdowns; save-time validation blocks selection if stock insufficient
- [ ] `generatedId` stable across recomposition; comma input in monto-total is handled; try-catch prevents UI crash on save error
- [ ] Editing a dispensation syncs without Supabase error 23505; log shows `ConflictHelper.detectConflictMovimientos` invoked pre-upload
