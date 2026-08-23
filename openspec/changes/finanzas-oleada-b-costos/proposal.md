# Proposal: Finanzas Oleada B — sync costos_lc + UI Biselado/LC

**Issue**: [Closes #106](https://github.com/MrSunstrider/Optoapp-2-0/issues/106) (`status:approved`)  
**Change**: `finanzas-oleada-b-costos` · **Depends on**: Oleada A (#105) role gate / gastos triad · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/finanzas-oleada-b-costos` (base `main` after #105, or `main` if independent)  
**Delivery**: `auto-chain`, WUs ≤400 · **Schema/RLS**: verify only; no new migration unless prod missing `costos_lc`

## Intent

Finish Oleada B: multi-device `costos_lc` sync, editable Biselado + Lentes Contacto cost matrices, and OT LC cost snapshot from `CostoLcDao`. Predecessor `add-costos-lc` left Entity/DAO/migration done but sync + UI + snapshot incomplete; tabs 1–2 are stubs; Disp still uses legacy `CostoProductoDao.lookupLc`.

## Scope

### In Scope
- `CostoLcRemoto` + upload/download after `costos_biselado`; `FinanzasSyncResult` counters; coordinator/use-case tests
- CostosYGastos tab 1 Biselado CRUD (soft-delete `vigenteHasta`, FAB, post-save sync); optional `CostoBiseladoDao` Flow
- Tab 2 LC CRUD + `OpticalCatalog` tipo_lc/modalidad constants; inject `CostoLcDao`
- Dispensacion LC snapshot → `CostoLcDao` (fill-when-null `?:`); delta `costos-productos` R5
- Strict TDD; GGA before push

### Out of Scope
- Cierre export; P&L; config financiera UI
- `PagoEffect`; new remote schema/RLS unless verify proves table missing
- Deprecate/remove `lookupLc`; unify-optical-catalog rewrite; DrawerSections role edits

## Capabilities

### New Capabilities
- `costos-lc`: LC cost matrix domain — Room/DAO already present; sync DTO; CostosYGastos tab CRUD; OT snapshot lookup keys (`tipo_lc`/`modalidad`)

### Modified Capabilities
- `sync`: Add `costos_lc` upload+download (mirror live biselado path); correct stale “biselado download-only” if touched
- `costos-productos`: MODIFY R5 — LC lookup MUST use `costos_lc`, not `costos_productos` hack keys

## Approach

Exploration **Approach 1**: clone biselado sync pattern; clone Tab 0 CRUD for tabs 1–2; retarget Disp LC lookup. Auto-chain: **WU-Sync → WU-UI-Biselado → WU-UI-LC → WU-Snapshot**. Soft-delete via upsert; role gate stays Oleada A `CostosGastosUiPolicy`.

## Affected Areas

| Area | Impact |
|------|--------|
| `SyncFinanzasDto`, Upload/Download coordinators, `SyncFinanzasUseCase` | Modified — LC sync |
| `CostosYGastosViewModel/Screen`, `OpticalCatalog` | Modified — tabs 1–2 |
| `DispensacionViewModel` | Modified — LC snapshot source |
| `CostoLcDao` / `CostoBiseladoDao` | Modified — tests; optional Flow |
| `PagoEffect`, migrations, cierre/P&L | Untouched |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| UI without snapshot → empty LC margins | High | Ship WU-Snapshot in same change; do not release UI-only |
| CostosYGastos diff >400 LOC | Med | Extract composables; chain PRs |
| Sync mock churn breaks suite | Med | Update all SyncFinanzas* tests in WU-Sync |
| RLS write ⊆ BI role mismatch | Low | Confirm admin/gerente ⊆ gate; safeUpload logs |

## Rollback Plan

Revert chained PRs independently (Sync → UI → Snapshot). No DB rollback if no new migration. Local Room entity already at v46 — leave in place. Feature flags not required.

## Dependencies

- Oleada A (#105) CostosYGastos role gate + triad preferred on base
- Remote `costos_lc` (migration `20260717020002_*` in repo) — verify before apply

## Success Criteria

- [ ] Sync uploads/downloads `costos_lc` after biselado; counters + tests green
- [ ] Tabs Biselado + LC: list/create/edit/soft-delete + post-save sync
- [ ] OT LC snapshot uses `CostoLcDao`; R5 delta reflects it
- [ ] Suite green; INV: PagoEffect / no cierre-P&L / no schema unless missing
- [ ] Closes #106
