# Tasks: Finanzas Oleada B — sync costos_lc + UI Biselado/LC

**Issue**: Closes #106 · **Branch**: `feat/finanzas-oleada-b-costos` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Base**: `main` after #105 (or `main` if independent) · **Delivery**: `auto-chain`  
**Gates**: GGA-eq R1–R4 per PR; `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
**Untouched**: `PagoEffect`; cierre export/P&L/config; DrawerSections; new migrations unless verify fails

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~800–1200 / 4 WUs |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 WU-Sync → PR2 WU-UI-Biselado → PR3 WU-UI-LC → PR4 WU-Snapshot |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test | Runtime | Rollback |
|------|------|-----------|--------------|---------|----------|
| WU1 Sync | `costos_lc` up/down + counters | PR1→feat | `*SyncFinanzas*` `*Upload*` `*Download*` | N/A JUnit | Revert sync DTO/coordinators/UC |
| WU2 UI-Biselado | Tab1 CRUD + Flow | PR2→PR1 | `*CostosYGastosViewModel*` | N/A JUnit | Revert tab1 + BiseladoDao Flow |
| WU3 UI-LC | Tab2 CRUD + catalog | PR3→PR2 | `*CostosYGastos*` `*CostoLcDao*` | N/A JUnit | Revert tab2 + OpticalCatalog LC |
| WU4 Snapshot | Disp → `CostoLcDao` | PR4→PR3 | `*DispensacionViewModel*` | N/A JUnit | Revert Disp LC branch |

## Phase 0 — Preconditions

- [x] 0.1 Verify remote `costos_lc` exists; if missing apply repo migration only (no new SQL).
- [x] 0.2 Create `feat/finanzas-oleada-b-costos` from agreed base.

## Phase 1 — WU1 Sync (≤400)

- [x] 1.1 RED `SyncFinanzasCostosTest`: `CostoLcRemoto` ↔ entity round-trip (`@SerialName` columns).
- [x] 1.2 RED Upload/Download/UseCaseKt: `uploadCostosLc`/`downloadCostosLc` after biselado; empty→0; counters on `FinanzasSyncResult`.
- [x] 1.3 GREEN `SyncFinanzasDto.kt`: DTO + mappers + result fields.
- [x] 1.4 GREEN inject `CostoLcDao` into Upload/Download coordinators; implement up/down (`skipDeletions=true`).
- [x] 1.5 GREEN `SyncFinanzasUseCase.kt`: safeUpload/Download after biselado; update all mocks.
- [x] 1.6 Focused verify + GGA R1–R4; PR1.

## Phase 2 — WU2 UI-Biselado (≤400)

- [x] 2.1 RED `CostoBiseladoDao` Flow list (Room in-memory) + VM: create/edit/soft-delete (`vigenteHasta=today`) + post-save sync scheduled.
- [x] 2.2 GREEN `CostoBiseladoDao.getByOpticaId(): Flow`.
- [x] 2.3 GREEN `CostosYGastosViewModel` tab1 state/CRUD (reuse productos soft-delete pattern).
- [x] 2.4 GREEN `CostosYGastosScreen` replace tab1 stub with list+FAB+dialogs (private composable).
- [x] 2.5 Focused verify + GGA R1–R4; PR2.

## Phase 3 — WU3 UI-LC (≤400)

- [ ] 3.1 RED `CostoLcDaoTest` lookup/list/upsert; VM tab2 CRUD validation (tipo/modalidad CHECK + costo>0).
- [ ] 3.2 GREEN `OpticalCatalog` TIPOS_LC + MODALIDADES_LC (+ materials as needed).
- [ ] 3.3 GREEN inject `CostoLcDao`; tab2 CRUD + soft-delete + post-save sync.
- [ ] 3.4 GREEN Screen tab2 list+dialogs (private composable); keep role gate.
- [ ] 3.5 Focused verify + GGA R1–R4; PR3.

## Phase 4 — WU4 Snapshot (≤400)

- [ ] 4.1 RED Disp VM: LC item null `costoRealLc` → `CostoLcDao.lookup` (map Cosmét→cosmetico, Medida→graduado; modalidad default `mensual`); non-null preserved; no `lookupLc`.
- [ ] 4.2 GREEN inject `CostoLcDao`; retarget LC branch; leave `lookupLc` unused.
- [ ] 4.3 Full `testDebugUnitTest`; INV check (PagoEffect untouched); GGA R1–R4; PR4 Closes #106.
