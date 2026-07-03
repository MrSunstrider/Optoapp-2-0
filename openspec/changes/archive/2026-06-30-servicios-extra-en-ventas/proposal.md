# Proposal: Servicios Extra en Ventas

## Intent

Daily sales totals across report ViewModels silently exclude `ServicioExtra` revenue, understating `totalVendido`, `totalPagado`, `totalVentasHoy`, `saldoPendiente`, `recaudacionProyectada`, and misclassifying servicio-extra payments as `cobrosAtrasados`. Restore correct financial reporting by folding servicios extra into the existing reactive flows.

## Scope

### In Scope

- `ReportesViewModel`: `totalVendido` += `ServicioExtra.montoTotal`; `totalPagado` += `ServicioExtra.aCuenta`; `cobrosPeriodo` classifies via `dispensacionId` OR `servicioExtraId`.
- `CierreCajaViewModel`: add `serviciosExtraHoy`, `totalServiciosExtra`, `totalGeneral` to `CierreCajaUiState` (desglose, Option B); `totalVentasHoy` stays disp-only; `saldoPendiente = (totalVentasHoy + totalServiciosExtra) - ventasHoy`; pago classification checks both IDs.
- `BIViewModel`: `recaudacionProyectada` += `ServicioExtra.montoTotal`.
- Update existing tests; add servicio-extra fixtures to `ReportesViewModelDiarioTest`, `ReportesViewModelOtrosPeriodosTest`, `CierreCajaViewModelTest`; create `BIViewModelTest` (currently absent).
- `ServicioExtra` entity: add `fechaEntrega: LocalDate? = null` field (mirroring `DispensacionOptica.fechaEntrega`).
- Room migration `28→29`: add `fecha_entrega` column to `servicios_extra` table.
- Supabase migration: add `fecha_entrega DATE` column to `public.servicios_extra`.

### Out of Scope

- Shared "daily sales" helper (Approach B from exploration) — deferred.
- UI layout changes beyond reading new `CierreCajaUiState` fields.
- `OperacionHoyViewModel` (already correct via `getPagosByDateRangeForOptica`).
- UI screen to input/edit `fechaEntrega` on ServicioExtra.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `reportes-financieros`: `Cobros del Período Classification` MUST consult `servicioExtraId` in addition to `dispensacionId`; new requirement for `Servicios Extra Inclusion` covering `totalVendido`, `totalPagado`, `totalVentasHoy`, `recaudacionProyectada`, and the `CierreCajaUiState` desglose fields.
- `servicio-extra`: entity definition — `ServicioExtra` MUST include optional `fechaEntrega` consistent with `DispensacionOptica.fechaEntrega`.

## Approach

Approach A (minimal fix): add `repository.getAllServiciosForOptica(opticaId)` as a fourth source in each affected ViewModel's `combine` chain. Compute combined totals inline; add a `servicioMap` (and keep `dispMap`) for pago classification. TDD per `openspec/config.yaml` (`tdd: true`): failing tests first, then implementation. No repository changes — `getAllServiciosForOptica` already exists.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/ReportesViewModel.kt` | Modified | `totalVendido`, `totalPagado`, `cobrosPeriodo` include servicios extra |
| `optoapp/.../viewmodel/CierreCajaViewModel.kt` | Modified | New UI state fields + combined totals + dual-ID pago classification |
| `optoapp/.../viewmodel/BIViewModel.kt` | Modified | `recaudacionProyectada` includes servicios extra |
| `optoapp/.../data/dispensacion/DispensacionEntity.kt` | Modified | `ServicioExtra.fechaEntrega` added |
| `optoapp/.../data/OptoDatabase.kt` | Modified | Version 28→29, register `MIGRATION_28_29` |
| `optoapp/.../data/OptoDatabaseMigrations.kt` | Modified | Add `MIGRATION_28_29` |
| `supabase/migrations/<timestamp>_servicios_extra_fecha_entrega.sql` | New | Add column `fecha_entrega DATE` |
| `optoapp/.../test/.../data/ServicioExtraMigration28_29Test.kt` | New | RED test for Room migration |
| `optoapp/.../test/.../viewmodel/*Test.kt` | Modified/Added | Servicio-extra fixtures; new `BIViewModelTest` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Test assertions on `totalVendido`/`totalPagado`/`saldoPendiente` change | High | Update fixtures before implementation; TDD red phase catches deltas |
| `aCuenta` vs `montoPagado` semantic mismatch | Medium | Decision 2: treat `aCuenta` as the down-payment analogue; document in spec |
| Orphan pagos (no dispensacionId, no servicioExtraId) classification drift | Low | Preserve existing `else` branch (cobrosAtrasados) unchanged |
| `combine` chain arity grows (4 sources) | Low | Kotlin `combine` supports up to 5 typed sources; stays readable |
| Room migration 28→29 breaks existing data | Low | `ALTER TABLE ADD COLUMN` with nullable — safe; test with existing migration test pattern |
| Supabase migration conflicts with concurrent changes | Low | New migration file, no existing data affected (nullable column) |

## Rollback Plan

1. ViewModel + test changes: revert the three ViewModel files and their tests.
2. Room migration: revert `OptoDatabase.kt` version to 28, remove `MIGRATION_28_29` registration; revert `ServicioExtra` entity; delete migration test.
3. Supabase: roll back the new migration file (not yet applied to prod) or run `ALTER TABLE public.servicios_extra DROP COLUMN IF EXISTS fecha_entrega` if already applied.

## Dependencies

- `OptoRepository.getAllServiciosForOptica(opticaId)` — already present.
- `PagoDao.getPagosByDateRangeForOptica` — already returns all pagos.
- Room migration `28→29` — depends on existing `MIGRATION_27_28` being stable.

## Success Criteria

- [ ] `totalVendido`, `totalPagado`, `totalVentasHoy`, `totalServiciosExtra`, `totalGeneral`, `saldoPendiente`, `recaudacionProyectada` reflect `ServicioExtra`.
- [ ] Servicio-extra pago with in-range `fecha` is NOT counted as `cobrosPeriodo`.
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` green.
- [ ] Cierre de Caja UI shows desglose (disp + servicios extra + total general).
- [ ] `ServicioExtra` entity has `fechaEntrega: LocalDate?`.
- [ ] Room migration `28→29` adds `fecha_entrega` column (nullable, no data loss).
- [ ] Supabase migration adds `fecha_entrega DATE` to `public.servicios_extra`.