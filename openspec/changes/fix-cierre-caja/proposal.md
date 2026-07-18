# Proposal: Fix Cierre de Caja — Payment Balance and Data Correctness

## Intent

Daily cash-close screen produces wrong pending balances (ignores historical payments), rounds amounts to integers, loads all tenant records into memory, and has fragile tenant isolation. Fixes all confirmed issues from adversarial review.

## Scope

| In Scope | Out of Scope |
|----------|-------------|
| saldoPendiente formula: entity-tracked `montoPagado`/`aCuenta` | Project-wide `mi_optica_base` removal |
| Per-item from entity fields, not `uiState.pagos` | UI redesign, export, multi-currency |
| ResumenCard: `%.0f` → `%.2f` | AppRoles permission changes |
| Date-filtered DAO queries instead of `getAll*ForOptica` | Other ViewModels using same patterns |
| Future-dated linked: warn group, not silent `ventasHoy` | Suspect-only issues (no dual-judge) |
| Multi-optica isolation test | — |
| Delta spec: saldoPendiente, payment source | — |

## Capabilities

### Modified Capabilities
- `cierre-caja`: saldoPendiente formula, per-item payment source, future-date classification, data-fetch granularity.

## Approach

1. **ViewModel**: inject date-filtered DAO queries. Replace `totalGeneral - ventasHoy` aggregate with per-item `sum(montoTotal - montoPagado)` + `sum(montoTotal - aCuenta)`. Future-date branch: `Log.w` + separate counter.
2. **Screen**: per-item "Pagado" shows `disp.montoPagado` / `serv.aCuenta`; "Saldo" = `montoTotal - pagado`.
3. **ResumenCard**: `String.format(Locale, "%.2f", monto)`.
4. **DAOs**: add `getDispensacionesForOpticaAndDate(start, end, opticaId)` and `getServiciosForOpticaAndDate(start, end, opticaId)`.
5. **Tests**: rewrite with Room in-memory. Add second-optica leakage assertion.

## Affected Areas

| File | Change |
|------|--------|
| `CierreCajaViewModel.kt` | New queries, correct saldoPendiente, future-date branch |
| `CierreCajaScreen.kt` | Per-item from entity-tracked fields |
| `ResumenCard.kt` | `%.0f` → `%.2f` |
| `DispensacionDao.kt` | Add date-range query |
| `ServicioExtraDao.kt` | Add date-range query |
| `DispensacionRepository.kt` | Expose new queries |
| `OptoRepository.kt` | Expose new queries |
| `CierreCajaViewModelTest.kt` | Room in-memory, multi-optica |
| `openspec/specs/cierre-caja/spec.md` | Delta spec |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `montoPagado`/`aCuenta` stale (offline-first lag) | Med | Verify via acceptance: insert pago → sync → assert entity updated |
| Date-filter excludes items that should appear | Low | Keep criteria: created/delivered/has-payment on date |
| Existing tests assert wrong formula | High | Expected — update assertions to correct values |

## Rollback Plan

Revert commit. No schema changes. Per-file revert possible.

## Dependencies

`montoPagado`/`aCuenta` already on entities.

## Success Criteria

- [ ] $300 disp, $200 paid yesterday + $100 today → saldoPendiente = $0.
- [ ] Per-item "Pagado" shows total historical payments.
- [ ] ResumenCard shows "s/. 150.00" not "s/. 150".
- [ ] DAO queries filter by date — no full-table scan.
- [ ] Future-dated link logged, not silently counted.
- [ ] Second-optica test asserts zero leakage.
- [ ] `./gradlew :optoapp:testDebugUnitTest` passes.
