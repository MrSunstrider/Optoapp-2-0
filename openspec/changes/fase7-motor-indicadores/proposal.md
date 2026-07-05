# Proposal: Fase 7 — Motor de 8 indicadores en lenguaje de negocio

## Intent

Transform raw financial data into 8 plain-language business indicators that answer what an optician-owner actually asks: "¿Estoy ganando?", "¿Quién me debe?", "¿Qué me conviene vender?". Each indicator has a natural-language label, a calculable metric, and a comparison baseline (vs last month, vs target). This is the computation engine — UI comes in Fase 9.

## Scope

### In Scope
- **Supabase**: 2 new RPCs (`rpc_analisis_mensual`, `rpc_deudores`) following existing `SECURITY INVOKER` + `GRANT` pattern
- **Supabase fix**: `GRANT EXECUTE ON FUNCTION recalcular_resumen_diario TO authenticated` — missing from Fase 6 migration
- **Supabase update**: `rpc_count_pendientes` rewritten to read from `ventas` instead of old `dispensaciones` + `servicios_extra`
- **Supabase deprecation**: `rpc_resumen_financiero` and `rpc_saldo_pendiente` — retired in favor of new RPCs
- **Android Room**: `Pago` entity gains `ventaId: String? = null` field + Room migration v32→v33 (ALTER TABLE + index)
- **Android UseCases**: `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` following existing pattern (Hilt `@Inject`, `suspend operator fun invoke`, `Resource<T>` return)
- **Android DAO**: `ResumenDiarioDao` gains monthly aggregation queries (SUM by month range)

### Out of Scope
- Fase 8 (recommendation engine — 6 business rules)
- Fase 9 (UI screens for indicators — BI redesign, analytics dashboard)
- Fase 10 (QA, load testing, edge function deployment)
- Room entities for `margen_por_categoria` and `costos_productos` — confirmed server-side only per existing spec (R4.2, R6.2)

## Capabilities

### New Capabilities
- `indicadores-negocio`: Supabase RPCs + Android UseCases that compute the 8 business indicators with month-over-month comparison and plain-language labels

### Modified Capabilities
- `analisis-negocio`: Add GRANT fix, 2 new RPCs (`rpc_analisis_mensual`, `rpc_deudores`), update `rpc_count_pendientes` to use `ventas`, add `ventaId` to `Pago` entity schema

## Approach

1. **Supabase first** — all 4 RPC changes in one migration batch: create `rpc_analisis_mensual`, `rpc_deudores`, fix GRANT, update `rpc_count_pendientes`
2. **Supabase deprecation** — add deprecation comment migrations for `rpc_resumen_financiero` and `rpc_saldo_pendiente` (no DROP — keep backward compatible)
3. **Android Room** — `Pago.ventaId` field + Room migration v32→v33 (ALTER TABLE pagos ADD COLUMN, CREATE INDEX)
4. **Android UseCases** — `ObtenerAnalisisMensualUseCase` calls RPC when online / Room fallback when offline; `ObtenerDeudoresUseCase` same pattern
5. **ResumenDiarioDao** — add monthly SUM queries for offline fallback of monthly aggregates

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/new_*` | New (4 files) | `rpc_analisis_mensual`, `rpc_deudores`, GRANT fix, deprecation notices |
| `supabase/migrations/20260513000000_*` | Modified | `rpc_count_pendientes` updated to use `ventas` |
| `optoapp/.../dispensacion/DispensacionEntity.kt` | Modified | `Pago` data class gains `ventaId` field + `@SerialName` |
| `optoapp/.../data/OptoDatabase.kt` | Modified | version 33, register MIGRATION_32_33, add new DAOs |
| `optoapp/.../data/OptoDatabaseMigrations.kt` | Modified | MIGRATION_32_33: ALTER TABLE pagos ADD ventaId |
| `optoapp/.../data/resumendiario/ResumenDiarioDao.kt` | Modified | Add monthly aggregation queries |
| `optoapp/.../domain/ObtenerAnalisisMensualUseCase.kt` | New | UseCase for indicators 1–4 |
| `optoapp/.../domain/ObtenerDeudoresUseCase.kt` | New | UseCase for indicator 5 |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Room migration v32→v33 fails on devices with large `pagos` table | Low | Use `ALTER TABLE ADD COLUMN` (instant, no data rewrite) |
| Old RPCs still called from existing Android code after deprecation | Med | Keep them working; remove call sites in Android as separate task |
| `rpc_deudores` performance on opticas with 1000+ ventas historicas | Low | Indexes on `ventas(optica_id)`, `pagos(venta_id)` already exist |

## Rollback Plan

1. **Room**: downgrade database version to 32, remove MIGRATION_32_33, drop new UseCases — no data loss (ALTER TABLE ADD COLUMN is additive)
2. **Supabase**: keep old RPCs running (they are not dropped, only deprecated) — clients using new RPCs can revert to old ones
3. **GRANT fix**: no rollback needed — granting execute is additive, revoking would break callers

## Dependencies

- **Fase 6** (resumen_diario table + recalcular_resumen_diario RPC) ✅ complete
- `ventas` ledger table with backfilled data ✅ complete
- `pagos` table with some rows linked via `venta_id` (backfill exists)

## Success Criteria

- [ ] `rpc_analisis_mensual` returns correct JSONB for a month with mixed dispensaciones + servicios extra
- [ ] `rpc_deudores` returns top debtors ordered by aging (highest days first)
- [ ] `recalcular_resumen_diario` callable by `authenticated` role (GRANT fix validated)
- [ ] Android app compiles with Pago.ventaId and passes existing tests
- [ ] Room migration v32→v33 preserves all existing Pago rows
