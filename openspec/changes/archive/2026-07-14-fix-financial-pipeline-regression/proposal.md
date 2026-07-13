# Proposal: Fix Financial Pipeline Regression

## Intent

July 10 dropped `ventas` and rewrote 4 financial RPCs to `UNION ALL` on source-of-truth tables. July 13 overwrote 2 RPCs with versions referencing the now-dropped `ventas`. `recalcular_resumen_diario` is broken (silent data corruption — S/803 gap for July). `rpc_analisis_mensual` lost 10 dashboard fields. Financial reports are wrong; core operations (dispensing, payments, caja) unaffected.

## Scope

**In**: W0 integration test (TDD-first — must pass before deploying fixes), W1 restore `recalcular_resumen_diario` (UNION ALL + real-cost from `dispensacion_items.costo_real_*`), W2a restore `rpc_analisis_mensual` (merge July 10 15-field version with July 13 `meses_historicos`), W2b `DROP FUNCTION rpc_saldo_pendiente` (0 callers, deprecated since Jul 6), W3 regenerate `resumen_diario` for all (optica_id, fecha), W4 CHECK constraints on `pagos.tipo` and `pagos.metodo_pago`. Fix legacy tests: `test_schema_integrity.sql` (remove `ventas`, `rpc_resumen_financiero` expectations), `test_cost_recalculation.sql` (rewrite to avoid dropped tables).

**Out**: Recreating `ventas` table, FK `pagos → ventas`, Android app changes, restoring `rpc_resumen_financiero`.

## Approach

TDD-first with W0. UNION ALL architecture from July 10 (proven working). Add real-cost from `dispensacion_items.costo_real_*` with `costo_unitario_snapshot` fallback. Preserve July 13's `meses_historicos`. Deploy migration locally (`supabase start`) → run tests → deploy to remote. No DDL on transactional tables — only function bodies change.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/` | New | Fix migration restoring 2 RPCs + dropping dead function |
| `supabase/tests/test_schema_integrity.sql` | Modified | Remove `ventas`, `rpc_resumen_financiero` expectations |
| `supabase/tests/test_cost_recalculation.sql` | Modified | Rewrite to avoid dropped `ventas` table |
| `supabase/tests/test_financial_pipeline.sql` | New | W0 integration test (output = transactional sums) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Data loss | None | Functions only — no DDL on transactional tables |
| Regeneration slow | Low | Simple aggregate per (optica_id, fecha); seconds at production scale |
| Stale tests missed | Med | W0 explicitly catches regressions; CI fails on broken tests |

## Rollback

`supabase migration repair` to mark fix migration as not applied, then redeploy previous migration. No data impact — only function bodies and tests change.

## Dependencies

None.

## Success Criteria

- [ ] W0 asserts 16 JSON keys in `rpc_analisis_mensual` response
- [ ] W0 asserts `resumen_diario.ventas_monto_total` matches `SUM(dispensaciones.monto_total) + SUM(servicios_extra.monto_total)` within 0.01 tolerance
- [ ] After W3 regeneration, July revenue gap closes to < 0.01%
- [ ] `supabase db reset` + all tests pass in CI
- [ ] `DROP FUNCTION rpc_saldo_pendiente` succeeds

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `analisis-negocio` (R9, R26): `recalcular_resumen_diario` data source changes from `public.ventas` to `public.dispensaciones UNION ALL public.servicios_extra` with real-cost from `dispensacion_items.costo_real_*`. R26 updated: `rpc_saldo_pendiente` changed from deprecation comment to `DROP FUNCTION`.
