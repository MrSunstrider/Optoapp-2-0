# Proposal: Fix JD Reportes Cluster

## Intent

Remediate all JD Round-1 SUSPECTS (S1–S6) and INFO (I1–I4) on the financial reports cluster. Ledger `#2120` had no dual-confirmed CRITICAL/HIGH; user directed full fix.

## Scope

### In Scope
- **S1/S3**: Static errors in `CierreCajaViewModel` + `SyncFinanzasUseCase` (Log.e keeps raw detail)
- **S4/S6**: `AnalisisNegocioScreen` role `initial=null` + guard; list uses `gastosMes`
- **I2/I1**: `ResumenDiarioDao.deleteAll()` + real DAO test; keep Robolectric; document harness debt
- **I3**: Drop dead `"Este año"` VM branches; align `reportes-financieros` to UI labels
- **S2/S5/I4**: New migrations — inclusive `rpc_cierre_caja_resumen`; restore R23 `stock_estancado`; NULL-safe `v_proyeccion`/egresos (prefer one REPLACE for S5+I4)

### Out of Scope
- Shared non-Robolectric Room harness / all 18 DaoTests
- Re-adding `"Este año"` to UI; editing applied migration history

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cierre-caja`: Static load errors; inclusive `rpc_cierre_caja_resumen`
- `analisis-negocio`: Role gate; gastosMes list; R13.1 deleteAll; R23 stock + NULL-safe proyeccion
- `reportes-financieros`: Periods → `Diario|Semanal|Mensual|Anual|Total`
- `sync`: SyncFinanzasUseCase static `Resource.Error`

## Approach

Strict TDD WU order: (1) S1+S3 (2) S4+S6 (3) I2+I1 (4) I3 (5) S2+S5+I4 SQL + GGA then remote. Chain PRs: Android then SQL. Patterns: `ObtenerAnalisisMensualUseCase`, `CierreCajaScreen` null guard, `20260709000003` CTE.

**Supabase**: yes — new `CREATE OR REPLACE` only (no RLS changes).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `CierreCajaViewModel.kt` (+test) | Modified | Static errorMessage |
| `SyncFinanzasUseCase.kt` (+test) | Modified | Static Resource.Error |
| `AnalisisNegocioScreen.kt` | Modified | Role null + gastosMes |
| `ResumenDiarioDao.kt` (+test) | Modified | deleteAll |
| `ReportesViewModel.kt` (+tests) | Modified | Drop dead branches |
| `reportes-financieros` spec | Modified | Label/range delta |
| `supabase/migrations/*` | New | S2 + S5/I4 RPCs |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Inclusive S2 breaks exclusive callers | Med | Grep optoweb; COMMENT; SQL test |
| REPLACE drops pago_effect | Med | Diff vs converge body |
| >400-line review | High | Chain Android / SQL PRs |

## Rollback Plan

Android: revert PR. SQL: reverse migration restoring bodies from `20260815005859` / `20260815010805`.

## Dependencies

GGA CLEAN before remote push; confirm no exclusive-`p_to` callers before S2 done.

## Success Criteria

- [ ] S1–S6 and I1–I4 fixed with red→green tests where applicable
- [ ] Same-day `rpc_cierre_caja_resumen` returns data when pagos exist
- [ ] `rpc_analisis_mensual` R23 stock + egresos when unpaid=0
- [ ] `testDebugUnitTest` green; GGA CLEAN for SQL PR
