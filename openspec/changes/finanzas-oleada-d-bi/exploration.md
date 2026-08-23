# Exploration: Finanzas Oleada D — config financiera + P&L + resumen_diario

**Change**: `finanzas-oleada-d-bi`
**Issue**: #108 (status:approved) — `feat(finanzas): Oleada D — config financiera + P&L + resumen_diario in-app`
**RDD**: `rdd_mode=disabled/unmanaged`
**Depends on**: Fase 6–9 schema/RPC/AnalisisNegocio already on main; Oleada A read-only gastos on Analisis
**Protect**: `PagoEffect` (do not edit); no cierre export (Oleada C)

**IN**: UI to edit `configuracion_financiera` thresholds; month P&L (ventas − COGS − gastos); in-app `resumen_diario` surface; update Reportes specs to `PagoEffect`
**OUT**: New remote schema unless verify proves missing columns; formal accounting periods; cierre PDF/CSV; changing `PagoEffect` matrix

---

## Current State

### `configuracion_financiera` — data plane only

| Piece | Status |
|-------|--------|
| Room `ConfiguracionFinancieraEntity` / Dao | Done — PK `opticaId`; thresholds (margen neto, ticket, caídas, deuda, stock, min ventas, recalc freq) |
| Download sync | Done — `DownloadSyncCoordinator.downloadConfiguracionFinanciera`; DTO in `SyncFinanzasDto` |
| Upload sync | **Missing** — `UploadSyncCoordinator` has no config upsert (download-only today) |
| Consumer | `GenerarRecomendacionesUseCase` reads thresholds; defaults if null |
| Settings UI | **Missing** — no screen/fields under `ConfiguracionScreen` or Analisis |

Editing locally without upload → wiped on next download. Oleada D must add **upload after upsert** (admin/gerente only; align with existing BI role `canViewBiAndReports` or user-manage gate).

### P&L month — partial via AnalisisNegocio

`AnalisisMensual` already carries `ventasMes`, `gastosMes`, `margenNetoPct`, `margenPorCategoria` (+ `costoDeVentas()` = Σ category costos). Primary path: RPC `rpc_analisis_mensual`. Offline fallback in `ObtenerAnalisisMensualUseCase` sums Room `resumen_diario` for ventas/cobros but **zeros margin / skips COGS+gastos composition**.

`AnalisisNegocioScreen` shows KPI cards + tip copy for margen neto; **no explicit P&L statement** (Ventas − COGS − Gastos = Utilidad). Oleada D should add a month P&L block (online from RPC fields; offline from `resumen_diario` ventas/costos + local `gastos_operativos` for the month — or clearly label offline as limited).

### `resumen_diario` — widget + sync; no in-app list

| Piece | Status |
|-------|--------|
| Entity/Dao | Done — unique `(opticaId, fecha)`; month + day queries |
| Download | Done — RPC `recalcular_resumen_diario` then fetch rows |
| Upload | N/A by design (server-owned) |
| Widget Mi Negocio | Done — Room-only today metrics |
| In-app UI | **Missing** — no calendar/list/detail of daily summaries |

### Reportes specs vs code — PagoEffect drift

`ReportesViewModel` already aggregates with `PagoEffect.signedAmount`. Spec `openspec/specs/reportes-financieros/spec.md` still says `totalCobrado` = raw `sum(monto)` and related formulas use entity `montoPagado`/`aCuenta` caches. Oleada D must **MODIFIED** those requirements to PagoEffect (Abono/Pago completo +, Reembolso/Reverso −, Anulación/unknown 0) without changing production math.

Also stale: `openspec/specs/migration-tests/spec.md` cobros assert excluding only Anulación — should reference effect matrix when touched; keep scoped to Reportes delta unless tasks expand.

---

## Affected Areas

| Path | Why |
|------|-----|
| New `ConfiguracionFinancieraScreen` or section in `ConfiguracionScreen` | Threshold editors + validation |
| `ConfiguracionFinancieraViewModel` (new) | Load Flow, upsert, trigger upload/sync |
| `UploadSyncCoordinator` + `SyncFinanzasUseCase` | `uploadConfiguracionFinanciera` (single-row upsert) |
| `AnalisisNegocioScreen` / Detalle | Explicit month P&L block |
| `ObtenerAnalisisMensualUseCase` offline fallback | Optional: compose COGS from resumen + gastos month |
| New `ResumenDiarioScreen` or Analisis subsection | List/month strip of daily rows from Dao Flow |
| `openspec/specs/reportes-financieros/spec.md` | Delta → PagoEffect |
| Possibly `analisis-negocio` / `indicadores-negocio` deltas | P&L + resumen surface requirements |
| Tests: Dao already; VM/UI policy; upload coordinator; offline P&L; spec-driven characterization | Strict TDD |

**Do not touch**: `PagoEffect.kt` implementation; Cierre export / Operación Hoy nav (Oleada C); cost matrix tabs (Oleada B); drawer role matrix beyond config write gate; new Supabase tables.

---

## Approaches

### 1. **Config UI + upload + P&L block + resumen list + Reportes spec delta** (recommended)

Ship settings editors with upload; add P&L section on Analisis; add read-only resumen_diario month list (reuse Dao); update Reportes specs to match PagoEffect code.

- Pros: Closes #108 fully; reuses RPC + Room; small sync addition mirrors other single-row upserts
- Cons: Four concerns — must chain WUs ≤400; offline P&L needs careful labeling
- Effort: Medium

### 2. **Spec-only Reportes + P&L UI; defer config upload and resumen screen**

Document PagoEffect; show P&L from existing RPC fields only.

- Pros: Tiny; low risk
- Cons: Does not close #108 (config UI + resumen in-app still open); local config edits remain download-clobbered if added later without upload
- Effort: Low

### 3. **Client-only P&L from Room (ignore RPC) + full config bidirectional sync rewrite**

Rebuild month P&L purely offline-first; redesign config sync.

- Pros: Strong offline story
- Cons: Duplicates RPC semantics; high drift risk vs server margen; exceeds budget; touches financial invariants near PagoEffect consumers
- Effort: High

---

## Recommendation

**Approach 1**, sliced for ≤400-line review budget (`auto-chain`):

1. **WU-Spec-Reportes**: Delta `reportes-financieros` (and any scenario that asserts raw `sum(monto)`) → PagoEffect; characterization tests already green — docs/tests expectations only
2. **WU-Config**: UI + Dao upsert + `uploadConfiguracionFinanciera` + role gate (admin/gerente)
3. **WU-PnL**: Analisis month P&L block from `AnalisisMensual` (+ optional offline compose)
4. **WU-Resumen**: In-app `resumen_diario` list/detail for selected month (read-only; pull-to-refresh → existing finanzas sync)

Invariant: do not modify `PagoEffect.kt`; Reportes/Cierre/Operación Hoy continue calling `signedAmount`.

---

## Risks

- **Download clobber**: Config UI without upload loses edits — must ship upload in same change as UI
- **RLS write roles**: Confirm Supabase policies allow admin/gerente update on `configuracion_financiera` before relying on upsert
- **Offline P&L lying**: Showing margen 0 offline as if real — label "offline / parcial" or compute from resumen+gastos
- **Double-count COGS**: Mixing RPC category costs with item snapshots incorrectly — prefer RPC fields online
- **PagoEffect "fix" temptation**: Spec update is documentation alignment, not a license to rewrite aggregators
- **Review budget**: Four WUs; keep each PR ≤400 authored lines

---

## Ready for Proposal

**Yes.** Orchestrator should run `sdd-propose` for `finanzas-oleada-d-bi` with:

- Approach 1 + WU split above
- IN/OUT from #108
- `rdd_mode=disabled/unmanaged`
- Invariants: protect `PagoEffect`; no cierre export; no arqueo; schema only if column missing on prod
- Branch suggestion: `feat/finanzas-oleada-d-bi`
- Closes #108
