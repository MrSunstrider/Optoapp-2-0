# Exploration: Finanzas Oleada B — sync costos_lc + UI Biselado/LC

**Change**: `finanzas-oleada-b-costos`  
**Issue**: #106 (status:approved) — `feat(finanzas): Oleada B — sync costos_lc + UI Biselado/LC`  
**RDD**: `rdd_mode=disabled/unmanaged`  
**Depends on**: Oleada A UX (#105) for CostosYGastos role gate + gastos triad (already on `feat/finanzas-oleada-a-ux-ia` / main path)  
**Predecessor**: `openspec/changes/add-costos-lc` (partially applied)

**IN**: `costos_lc` upload/download in SyncFinanzas; UI CRUD tabs Biselado + Lentes Contacto; optional cost snapshot on OT items when missing (esp. LC → `CostoLcDao`)  
**OUT**: cierre export; P&L; config financiera UI  
**Protect**: `PagoEffect` (do not edit); no new remote schema/RLS unless verify proves `costos_lc` missing on prod

---

## Current State

### `add-costos-lc` — half delivered

| Planned (proposal) | Status |
|--------------------|--------|
| `CostoLcEntity` / `CostoLcDao` | Done — Room entity + lookup + Flow + upsertAll |
| `OptoDatabase` + `DatabaseModule` | Done — entity registered; DAO provided |
| Room empty migration 40→41 | Done — comment-only; table auto-created |
| Supabase `20260717020002_create_costos_lc.sql` | Done — table + RLS (admin/gerente write, member read) |
| `CostoLcRemoto` DTO + SyncFinanzas upload/download | **Missing** |
| DAO unit tests | **Missing** (no `CostoLc*Test`) |

`costos_lc` columns: `tipo_lc` ∈ {cosmetico, graduado, terapeutico}, `material_lc`, `modalidad` ∈ {diario, quincenal, mensual, anual}, optional `radio_base`/`diametro`/`laboratorio_id`, `costo_unitario`, vigencia window.

### SyncFinanzas — biselado wired; LC not

Upload order today (`SyncFinanzasUseCase`): dispensaciones → items → servicios → **costos_productos** → **costos_biselado** → pagos → gastos → regalos (8 uploads).

Download: … → costos_productos → costos_biselado → pagos → regalos → gastos (no `costos_lc`).

Pattern to clone for LC (identical shape to biselado):

- `CostoBiseladoRemoto` + `toEntity()` / `toRemoto()` in `SyncFinanzasDto.kt`
- `UploadSyncCoordinator.uploadCostosBiselado` — `getByOpticaIdList` → PostgREST upsert
- `DownloadSyncCoordinator.downloadCostosBiselado` — `downloadTable` + `upsertAll`, `skipDeletions = true`
- Counters on `FinanzasSyncResult`; mocks in `SyncFinanzasUseCaseKtTest` / DTO tests in `SyncFinanzasCostosTest`

`UploadSyncCoordinator` / `DownloadSyncCoordinator` inject `CostoProductoDao` + `CostoBiseladoDao` only — **no `CostoLcDao`**.

### CostosYGastos UI — tabs 1–2 stubs

Tabs: `0 Lentes` | `1 Biselado` | `2 Lentes Contacto` | `3 Gastos Operativos`.

- Tab 0: full matriz CRUD (blocks, edit/new/soft-delete via `vigente_hasta`) — pattern from `add-costos-productos-crud`
- Tabs 1–2: placeholder `Text("… — próximamente")`
- `CostosYGastosViewModel` already injects `CostoBiseladoDao` but **never uses it**; **no `CostoLcDao` injection**
- Role gate: `CostosGastosUiPolicy.resolveAccess` → `AppRoles.canViewBiAndReports` (Oleada A) — keep for new tabs
- Soft-delete convention: upsert with `vigenteHasta = today` (not hard DELETE)

### Cost snapshot on OT items — partial / wrong LC source

`DispensacionViewModel` (link-eval path) already fills `costoRealOd/Oi/Montura/Biselado/Lc` with `?:` override (R6 persist). Biselado uses `CostoBiseladoDao.lookup`. **LC still uses legacy `CostoProductoDao.lookupLc`** with `tipo_lente` keys `lente_contacto_cosmetico|medida` and `stock_o_fabricacion='stock'` — not `CostoLcDao` keys (`tipo_lc` / `modalidad`). Spec `costos-productos` R5 still documents the hack; Oleada B should delta R5 → `costos_lc`.

Fields already exist on `DispensacionItemEntity` + sync DTO (`costo_real_*`). Snapshot work is **retarget lookup + ensure fill-when-null**, not new columns.

### OpticalCatalog gap

`OpticalCatalog` has ophthalmic materiales / tipo_lente / tratamientos / tipo_aro / series — **no LC tipo/modalidad lists**. UI Biselado can reuse MATERIALES + TIPO_ARO (+ stock/fab + serie). LC UI needs small catalog constants (mirror Postgres CHECKs).

### DAO asymmetry

| DAO | Flow list | Suspend list | Soft-delete helper |
|-----|-----------|--------------|--------------------|
| `CostoProductoDao` | `getByBloque` | `getByOpticaIdList` | via upsert in VM |
| `CostoBiseladoDao` | **none** | `getByOpticaIdList` | none |
| `CostoLcDao` | `getByOpticaId` | `getByOpticaIdList` | none |

Recommend adding `Flow` on biselado for UI parity (or one-shot load like `loadBlock`).

---

## Affected Areas

| Path | Why |
|------|-----|
| `domain/SyncFinanzasDto.kt` | Add `CostoLcRemoto` + mappers; extend `FinanzasSyncResult` counters |
| `domain/UploadSyncCoordinator.kt` | Inject `CostoLcDao`; `uploadCostosLc`; TABLE const |
| `domain/DownloadSyncCoordinator.kt` | Inject `CostoLcDao`; `downloadCostosLc` |
| `domain/SyncFinanzasUseCase.kt` | safeUpload/safeDownload after biselado; result fields |
| `data/costolc/CostoLcDao.kt` | Tests; optional soft-delete query if desired |
| `data/costobiselado/CostoBiseladoDao.kt` | Add Flow list for UI (optional but low-cost) |
| `viewmodel/CostosYGastosViewModel.kt` | Inject `CostoLcDao`; Biselado/LC state + CRUD; post-save sync |
| `ui/screens/CostosYGastosScreen.kt` | Replace stubs with list + dialogs (mirror Matriz / Gastos) |
| `domain/OpticalCatalog.kt` | LC tipo/modalidad (+ maybe biselado defaults) |
| `viewmodel/DispensacionViewModel.kt` | Retarget LC snapshot to `CostoLcDao`; map eval fields → tipo/modalidad |
| Tests: `SyncFinanzasCostosTest`, `SyncFinanzasUseCaseKtTest`, `UploadSyncCoordinatorTest`, `DownloadSyncCoordinatorTest`, `CostosYGastosViewModelTest`, new `CostoLcDaoTest`, Dispensacion VM LC tests | Strict TDD |

**Do not touch**: `PagoEffect.kt`; cierre export / P&L / config financiera screens; resumen RPC writers; DrawerSections role policy; new Supabase migrations unless prod missing table.

---

## Approaches

### 1. **Mirror-biselado sync + Lentes-tab CRUD + LC snapshot retarget** (recommended)

Wire `costos_lc` exactly like `costos_biselado`; implement Biselado/LC tabs by cloning Tab 0 CRUD (soft-delete, FAB, dialogs); retarget Dispensacion LC lookup to `CostoLcDao`.

- Pros: Matches existing sync/UI conventions; closes #106 IN fully; fixes R5 debt; low design novelty
- Cons: CostosYGastosScreen/VM grow further (split composables / chained PRs if >400 LOC)
- Effort: Medium

### 2. **Sync-only first; UI stubs remain; snapshot deferred**

Only DTO + coordinators + SyncFinanzas + tests.

- Pros: Smallest PR; unblocks multi-device LC matrix storage
- Cons: Does not close #106 (UI + snapshot still open); users still cannot edit Biselado/LC
- Effort: Low

### 3. **Big-bang catalog unify + deprecate `lookupLc` + block refactor**

Oleada B + OpticalCatalog LC domain + remove `CostoProductoDao.lookupLc` + rewrite costos-productos R5/R1 blocks in one change.

- Pros: Cleanest long-term domain
- Cons: Couples to `unify-optical-catalog`; high churn; risks PagoEffect-adjacent Disp VM regressions; exceeds review budget
- Effort: High

---

## Recommendation

**Approach 1**, sliced for ≤400-line review budget (`auto-chain` / feature-branch-chain, same as Oleada A):

1. **WU-Sync**: `CostoLcRemoto` + upload/download + SyncFinanzas order (after biselado) + DTO/coordinator/use-case tests  
2. **WU-UI-Biselado**: DAO Flow if needed + VM/Screen tab 1 CRUD (mirror productos soft-delete)  
3. **WU-UI-LC**: Inject `CostoLcDao` + tab 2 CRUD + OpticalCatalog LC enums  
4. **WU-Snapshot** (optional but issue-listed): DispensacionViewModel LC → `CostoLcDao`; keep `?:` override; leave biselado path as-is unless gaps found  

Schema: **verify** remote `costos_lc` exists (migration already in repo); **no new migration** if present. Local Room already has entity at v46.

Delivery: Strict TDD (`openspec/config.yaml`); GGA before push; protect `PagoEffect`.

---

## Risks

- **Key mismatch LC**: UI/sync write `tipo_lc=cosmetico` while Disp still queries `lente_contacto_*` on `costos_productos` → empty margins until WU-Snapshot; do not ship UI without snapshot retarget or explicit follow-up issue.
- **Screen/VM size**: CostosYGastos already large — new tabs risk >400 LOC diffs; extract private composables / chain PRs.
- **Sync order / result contract**: Every `SyncFinanzasUseCaseKtTest` mock must gain `uploadCostosLc` / `downloadCostosLc` or tests break.
- **RLS write roles**: Only admin/gerente can mutate remote `costos_lc` — UI already gated by BI/reports role; confirm that set ⊆ RLS roles or uploads fail silently under `safeUpload`.
- **Invariant bleed**: Temptation to touch PagoEffect, cierre export, or P&L — out of scope; refuse in apply.
- **Empty biselado Flow**: One-shot load OK; forgetting refresh after upsert shows stale UI.

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `finanzas-oleada-b-costos` with:

- Approach 1 + WU split above  
- IN/OUT from #106  
- `rdd_mode=disabled/unmanaged`  
- Invariants: protect `PagoEffect`; no cierre export / P&L; schema only if remote missing  
- Spec plan: NEW `costos-lc` (or DELTA sync + DELTA `costos-productos` R5 → `costos_lc`) + DELTA CostosYGastos Biselado/LC CRUD  
- Branch suggestion: `feat/finanzas-oleada-b-costos`  
- Closes #106
