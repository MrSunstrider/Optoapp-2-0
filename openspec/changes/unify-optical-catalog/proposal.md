# Proposal: Unify Optical Catalog

## Intent

Optical constants (materials, lens types, treatments, aro types, series) are duplicated across 4 files with inconsistent values. Auto-calculation fails because treatment names mismatch between cost matrix ("Antireflex") and dispensacion form ("Antireflejo"). Create a single `OpticalCatalog` object as the canonical source of truth.

## Scope

### In Scope
- Create `domain/OpticalCatalog.kt` with canonical lists/maps for: MATERIALES, TIPO_LENTE, TRATAMIENTOS, TIPO_ARO, SERIES
- Replace hardcoded `MaterialesOpticos`/`TIPOS_LENTE`/`TRATAMIENTOS` in `CostosYGastosViewModel.kt` with `OpticalCatalog` references
- Replace hardcoded `listOf(...)` in `LenteForm.kt` with `OpticalCatalog` references
- Unit tests for `OpticalCatalog` values (strict TDD)

### Out of Scope
- Bug fixes (determineTipoLente, determineSerie, normalizeTipoAro) — Phase 2
- Progressive disclosure UI — Phase 7
- costos_lc table — Phase 4
- Any Supabase schema or RLS changes

## Capabilities

### New Capabilities
None — pure refactor, no new spec-level behavior.

### Modified Capabilities
None — no spec-level requirements change. Values are normalized to match existing behavior where possible (e.g., "Resina" kept from LenteForm, not "CR39" from VM).

## Approach

1. Write `OpticalCatalogTest.kt` to assert canonical values (no Robolectric — plain JUnit)
2. Create `OpticalCatalog.kt` under `domain/` as a `object` with 5 top-level properties
3. In `CostosYGastosViewModel.kt`: replace `MaterialesOpticos`, `TIPOS_LENTE`, `TRATAMIENTOS` companion val references with `OpticalCatalog.*`
4. In `LenteForm.kt`: replace inline `listOf(...)` with `OpticalCatalog.MATERIALES`, `OpticalCatalog.TRATAMIENTOS`, `OpticalCatalog.TIPO_LENTE`

Normalization decisions (canonical value chosen):
- Material: "Resina" over "CR39" (LenteForm is user-facing, VM values used for DB lookup)
- Tipo Lente: "Progresivo" kept (existing DB values use it, not "Multifocal")
- Tratamientos: "Antireflejo" over "Antireflex" (matches LenteForm, will fix cost lookup in Phase 2)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/OpticalCatalog.kt` | New | Canonical constants object |
| `CostosYGastosViewModel.kt` | Modified | Replace 3 companion vals with `OpticalCatalog.*` |
| `LenteForm.kt` | Modified | Replace 3 inline `listOf(...)` with `OpticalCatalog.*` |
| `OpticalCatalogTest.kt` | New | Unit tests for catalog integrity |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Normalized value breaks DB/cost lookup | Low | Phase 2 will fix lookup logic — this phase only changes UI options, not lookup |
| Existing saved dispensaciones reference old values | Low | DB stores user-selected strings, not enum refs — no migration needed |

## Rollback Plan

`git checkout` on the 3 modified files + delete the 2 new files. No DB changes, so rollback is instant.

## Dependencies

None.

## Success Criteria

- [ ] `OpticalCatalog` object exists with all 5 properties matching the canonical values
- [ ] `CostosYGastosViewModel` exposes material/lente/treatment lists from `OpticalCatalog` only
- [ ] `LenteForm.kt` dropdowns use `OpticalCatalog` properties exclusively
- [ ] Existing unit tests pass without modification
- [ ] No hardcoded optical constant lists remain in the 3 affected files
