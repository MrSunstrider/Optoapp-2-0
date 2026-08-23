# Proposal: Finanzas Oleada D — config financiera + P&L + resumen_diario

**Issue**: [Closes #108](https://github.com/MrSunstrider/Optoapp-2-0/issues/108) (`status:approved`)  
**Change**: `finanzas-oleada-d-bi` · **Depends on**: Fase 6–9 schema/RPC + Oleada A read-only gastos on Analisis · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/finanzas-oleada-d-bi` · **Delivery**: `auto-chain`, WUs ≤400  
**Schema/RLS**: verify only; no new tables unless columns missing

## Intent

Ship in-app BI controls: edit `configuracion_financiera` with upload (so downloads do not clobber), explicit month P&L on Analisis, read-only `resumen_diario` list, and align Reportes specs to live `PagoEffect` math.

## Scope

### In Scope
- Config financiera editors (admin/gerente) + Dao upsert + `uploadConfiguracionFinanciera` + post-save sync
- Month P&L block: Ventas − COGS − Gastos = Utilidad (online RPC fields; offline labeled / composed from resumen + gastos)
- In-app `resumen_diario` month list/detail (read-only; refresh via existing finanzas sync)
- Delta `reportes-financieros` → `PagoEffect.signedAmount` (docs/tests; no code rewrite of matrix)
- Strict TDD; GGA before push

### Out of Scope
- Cierre PDF/CSV / Operación Hoy fecha (Oleada C); cost matrix tabs (Oleada B)
- Editing `PagoEffect.kt`; formal accounting periods; new remote schema unless verify fails
- Upload of `resumen_diario` (server-owned)

## Capabilities

### New Capabilities
- `configuracion-financiera`: In-app threshold editors + bidirectional sync for single-row config

### Modified Capabilities
- `reportes-financieros`: MODIFY cobrado/cobros formulas from raw `sum(monto)` → PagoEffect
- `analisis-negocio`: ADDED month P&L UI + resumen_diario surface; MODIFY R7.2/R14/R22 read-only config → writable+upload
- `sync`: ADDED `configuracion_financiera` upload (mirror other single-row upserts)
- `indicadores-negocio`: MODIFY offline fallback to compose COGS+gastos or label partial

## Approach

Exploration **Approach 1**. Chain: **WU-Spec-Reportes → WU-Config → WU-PnL → WU-Resumen**. Ship config UI and upload in the same change. Prefer RPC fields online; label offline P&L.

## Affected Areas

| Area | Impact |
|------|--------|
| Config screen/VM + Dao upsert path | New/Modified |
| `UploadSyncCoordinator`, `SyncFinanzasUseCase`, DTO `toRemoto` | Modified |
| `AnalisisNegocioScreen` / Detalle | P&L block |
| `ObtenerAnalisisMensualUseCase` | Optional offline compose |
| Resumen list screen/subsection | New |
| `openspec/specs/reportes-financieros` | Delta |
| `PagoEffect.kt`, cierre export | Untouched |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| UI without upload → download clobber | High | Same WU/PR for UI+upload |
| Offline P&L looks authoritative | Med | Label "offline / parcial" |
| RLS write denied | Med | Confirm admin/gerente policies before apply |
| Spec "fix" rewrites aggregators | Med | Spec/tests only; protect PagoEffect |

## Rollback Plan

Revert chained PRs (Spec → Config → PnL → Resumen). No DB rollback if no migration. Local config rows remain; remote unchanged if upload never ran.

## Dependencies

- Existing Room/DTO/download for config + resumen; `rpc_analisis_mensual`; Oleada A Analisis gastos read-only
- RLS INSERT/UPDATE admin/gerente on `configuracion_financiera`

## Success Criteria

- [ ] Config editable + uploads; survives download cycle
- [ ] Month P&L visible online; offline labeled/composed
- [ ] resumen_diario list for selected month
- [ ] Reportes specs match PagoEffect; suite green
- [ ] Closes #108
