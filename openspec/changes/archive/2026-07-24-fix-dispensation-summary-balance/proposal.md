# Proposal: Fix Dispensation Summary Balance

## Intent

The "Resumen de Dispensación" dialog shows incorrect financial data — it includes cancelled payments (type "Anulación") when computing "A Cuenta" and "Saldo Restante", inflating the paid amount and understating the remaining balance. Same bug exists in `ReportesViewModel`. All other ViewModels in the codebase correctly filter out "Anulación" payments. This change aligns the 4 broken flows with the established pattern.

## Scope

### In Scope
- Add `.filter { it.tipo != "Anulación" }` to `pagosSumByDispensacion` in `DispensacionViewModel.kt`
- Add the same filter to `aCuentaSumByServicio` in `DispensacionViewModel.kt`
- Add the same filter to both equivalent flows in `ReportesViewModel.kt`
- Cover all 4 locations with unit tests verifying "Anulación" payments are excluded

### Out of Scope
- No schema changes — Supabase tables, RLS, and migrations are untouched
- No spec-level requirement changes — filtering is implied by correct financial reporting
- No new capabilities or UI changes

## Capabilities

### New Capabilities
None — this is a pure bugfix, no new capability introduced.

### Modified Capabilities
None — the existing specs (`reportes-financieros`, `pagos-constraints`) already define correct financial behavior. This fix aligns implementation with existing requirements.

## Approach

Add `.filter { it.tipo != "Anulación" }` before the `.sumOf { ... }` call in 4 locations, matching the pattern already used in `ServiciosViewModel`, `InformacionFinancieraViewModel`, and `CalcularMontoPagadoUseCase`. No structural refactoring needed — the fix is a one-line addition per location.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `viewmodel/DispensacionViewModel.kt:139-147` | Modified | Add filter to `pagosSumByDispensacion` |
| `viewmodel/DispensacionViewModel.kt:150-158` | Modified | Add filter to `aCuentaSumByServicio` |
| `viewmodel/ReportesViewModel.kt:250-258` | Modified | Add filter to `pagosSumByDispensacion` |
| `viewmodel/ReportesViewModel.kt:260-269` | Modified | Add filter to `aCuentaSumByServicio` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Different filter predicate used across locations | Low | All 4 use the identical `.filter { it.tipo != "Anulación" }` pattern, verified against 4 correct references |
| Missed additional flows | Low | Exploration already searched all ViewModels; only these 4 broke the pattern |

## Rollback Plan

Revert the 4 one-line additions. The change is minimal and fully contained — rollback is a single `git revert`.

## Dependencies

None.

## Success Criteria

- [ ] Dispensación Summary dialog shows "A Cuenta" = s/. 100.00 (not s/. 0.00) when an abono of s/. 100.00 exists, matching `InformacionFinanciera`
- [ ] Dispensación Summary dialog shows "Saldo Restante" = s/. 120.00 (not s/. 220.00) for the same scenario
- [ ] All 4 flows exclude "Anulación" payments from sum computations
- [ ] Existing unit tests pass (no regressions)
- [ ] New unit tests verify "Anulación" exclusion per flow
