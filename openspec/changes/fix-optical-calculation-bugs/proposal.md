# Proposal: Fix Optical Calculation Bugs

## Intent

Three bugs in `DispensacionViewModel` produce wrong cost-matrix lookup keys: spherical stock lenses get `serie=null` (cost lookup fails), high-cylinder lenses get `stock` instead of `fabricacion` (wrong price), and `normalizeTipoAro` maps "Semi al aire" to the nonexistent value `ranurado` (biselado cost misses). These directly affect pricing accuracy at checkout.

## Scope

### In Scope
1. `determineSeriePorCilindro(cil: Double?)` — accept nullable, return `1` for `null` (spherical = 1ra serie)
2. `determineTipoLente(esf: Double, cil: Double?)` — add cylinder param, return `"fabricacion"` when `|cil| > 6.00`
3. `normalizeTipoAro` — map "Semi al aire" → `"semi_aire"`, remove `ranurado`/`taladro` branches
4. Update callers (OD/OI cost blocks) to pass cylinder to `determineTipoLente` and drop the null-guard on `determineSeriePorCilindro`
5. Update test assertions in `DispensacionViewModelCostosTest.kt`

### Out of Scope
- Match exacto tratamientos (Phase 5)
- UI changes (Phase 7)
- OpticalCatalog values (Phase 1 — already done)

## Capabilities

### New Capabilities
None — pure bugfix, no spec-level behavior change.

### Modified Capabilities
None — no requirements contract changes.

## Approach

1. Update tests first (strict TDD) — add null-cylinder, high-cylinder, and semi_aire test cases, update existing assertions
2. Fix `determineSeriePorCilindro` signature `Double?` + early return `1` for `null`
3. Fix `determineTipoLente` signature `(Double, Double?)` + check `|cil| > 6.00`
4. Fix `normalizeTipoAro` logic
5. Update OD/OI callers to pass `odCil`/`oiCil` and remove the `&& odCil != null` guard
6. Run `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `viewmodel/DispensacionViewModel.kt` | Modified | 3 companion functions + 2 caller blocks (lines 767–768, 780–781) |
| `DispensacionViewModelCostosTest.kt` | Modified | Update assertions per new behavior |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Spherical lenses now get serie=1 instead of null | Low | This is the **correct** behavior — null caused failed cost lookups |
| Biselado lookup key changes for semi_aire | Low | Value now matches `costo_biselado` DB rows; previously `ranurado` never matched |

## Rollback Plan

`git checkout` on the 2 modified files. No DB/schema changes.

## Dependencies

None — `OpticalCatalog.kt` (Phase 1) is already merged and its `TIPO_ARO` map has the correct `"semi_aire"` value.

## Success Criteria

- [ ] `determineSeriePorCilindro(null)` returns `1`
- [ ] `determineTipoLente(-2.00, -7.00)` returns `"fabricacion"`
- [ ] `normalizeTipoAro("Semi al aire")` returns `"semi_aire"`
- [ ] `normalizeTipoAro("ranurado")` returns `"aro_completo"` (fallback for unrecognized)
- [ ] `normalizeTipoAro("taladro")` returns `"aro_completo"` (fallback for unrecognized)
- [ ] All existing and new unit tests pass
