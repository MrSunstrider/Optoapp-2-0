# Proposal: Finanzas Oleada C — export cierre + Operación Hoy continuity

**Issue**: [Closes #107](https://github.com/MrSunstrider/Optoapp-2-0/issues/107) (`status:approved`)  
**Change**: `finanzas-oleada-c-cierre` · **Depends on**: Oleada A (#105) Cierre triad/role · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/finanzas-oleada-c-cierre` · **Delivery**: `auto-chain`, WUs ≤400  
**Schema/RLS**: none — no `arqueo_caja` revival

## Intent

Close day-end workflow gaps: export Cierre de Caja as PDF/CSV (role-gated), pass fecha from Operación Hoy into Cierre, and optional counted-cash difference vs PagoEffect Efectivo — without reviving synced arqueo.

## Scope

### In Scope
- Dedicated day-close PDF + CSV exporters fed from already-aggregated `CierreCajaUiState` / PagoEffect totals
- Cierre UI export actions gated by `AppRoles.canExportCierreCaja` (fail-closed)
- Optional nav `fecha` (`cierre_caja?fecha=` or path) + Operación Hoy navigate with `uiState.fecha`; VM consumes via `SavedStateHandle`
- Optional counted-cash + diferencia (session or prefs by `(opticaId, fecha)` only)
- Strict TDD; GGA before push

### Out of Scope
- Multi-shift formal arqueo; `arqueo_caja` table/sync/migrations
- P&L / config financiera / resumen_diario UI (Oleada D)
- Editing `PagoEffect`; Reportes PDF overload; DrawerSections role matrix changes

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `cierre-caja`: ADDED export PDF/CSV + role gate; ADDED fecha nav continuity; ADDED optional counted-cash without arqueo persistence

## Approach

Exploration **Approach 1**: clone Reportes share pattern with `CierreCajaPdfGenerator` + new CSV exporter; optional fecha arg; counted-cash UI-only. Chain: **WU-Export → WU-UI-Export → WU-Date → WU-Cash**. INV: exported cobrado/methods MUST equal on-screen PagoEffect aggregates.

## Affected Areas

| Area | Impact |
|------|--------|
| `util/CierreCajaPdfGenerator.kt`, `util/CierreCajaCsvExporter.kt` | New |
| `util/FileShareUtils.kt` | Optional CSV share helper |
| `ui/screens/CierreCajaScreen.kt`, `viewmodel/CierreCajaViewModel.kt` | Export + cash + fecha |
| `ui/navigation/Route.kt`, `MainDrawerScreen`, `OperacionHoyScreen` | Fecha continuity |
| `PagoEffect.kt`, sync, arqueo migrations | Untouched |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Export recomputes raw `sum(monto)` | High | Feed exporters from VM aggregates; characterization tests |
| CSV locale / Excel | Med | Invariant decimals + UTF-8 BOM |
| Retained VM wrong day | Med | Nav fecha + `setFecha` on entry |
| Scope creep to formal arqueo | Med | Refuse; OUT |

## Rollback Plan

Revert chained PRs independently (Export builders → UI → Date → Cash). No DB rollback. Prefs keys for cash are local-only — clear on uninstall.

## Dependencies

- Oleada A (#105) `CierreCajaUiPolicy` + `canExportCierreCaja`
- Existing `FileShareUtils.shareFile` / Reportes PDF share UX

## Success Criteria

- [ ] PDF/CSV export from Cierre; totals match PagoEffect on-screen
- [ ] Export hidden/denied without `canExportCierreCaja`
- [ ] Operación Hoy → Cierre preserves fecha
- [ ] Counted-cash optional; no `arqueo_caja`
- [ ] Suite green; Closes #107
