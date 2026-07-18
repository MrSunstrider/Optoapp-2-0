# Proposal: Drop broken `rpc_saldo_pendiente` and dead `rpc_count_pendientes`

## Intent

Migration `20260710064319` dropped `public.ventas` but `rpc_saldo_pendiente` still references `FROM public.ventas` in its body — it will crash at runtime if invoked. The function is deprecated (superseded by `rpc_analisis_mensual`), has zero Android callers, and must be removed. `rpc_count_pendientes` is also dead code (no Android callers, no edge function callers) and should be removed alongside it for symmetry.

## Scope

### In Scope
- Create a forward-fixing migration to `DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT)`
- Create a forward-fixing migration to `DROP FUNCTION IF EXISTS public.rpc_count_pendientes(TEXT)`
- Verify both functions absent from `information_schema.routines` after migration

### Out of Scope
- Dropping any other deprecated RPC (`rpc_resumen_financiero` remains callable per spec R26)
- Changing Android code (confirmed zero callers)
- Updating main spec R25 (stale — references `ventas`; function is being dropped, not rewritten)

## Capabilities

### New Capabilities
None — pure cleanup, no new behavior.

### Modified Capabilities
None — dropping dead code with zero callers. Spec R26 already requires dropping `rpc_saldo_pendiente`. R25 (rewrite `rpc_count_pendientes` to use `ventas`) becomes moot since `ventas` was dropped and the function is being removed entirely.

## Approach

Single forward-fixing migration. `rpc_saldo_pendiente` was already `CREATE OR REPLACE`'d on Jul 6 to reference `ventas`; `ventas` was dropped on Jul 10; the function is now broken. `rpc_count_pendientes` was rewritten on Jul 10 to use `dispensaciones` + `servicios_extra` (functional), but confirmed dead code via grep — both Android app and edge functions show zero references.

Create `supabase/migrations/20260715XXXXXX_drop_rpc_saldo_pendiente_count_pendientes.sql`:

```sql
DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT);
DROP FUNCTION IF EXISTS public.rpc_count_pendientes(TEXT);
```

### Caveat: existing migration

Migration `20260714000002_drop_rpc_saldo_pendiente.sql` already drops `rpc_saldo_pendiente`. If that migration has been applied, the new migration only drops `rpc_count_pendientes`. Verify local migration chain before creating — use `supabase migration new` and let Squasher determine the timestamp.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/20260715XXXXXX_*.sql` | New | DROP both RPC functions |
| `openspec/specs/analisis-negocio/spec.md` (R25) | Stale | References `ventas`; needs update after archive |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `rpc_count_pendientes` has an external caller not caught by grep | Low | grep covered Android `.kt` + Supabase `.ts` (edge functions). Web companion lives in a separate repo but does not trigger Supabase RPCs directly |
| Migration timestamp conflicts with existing `20260714000002` | Low | Verify with `supabase migration list` before creating |

## Rollback Plan

Re-create the dropped functions from the source migration (`20260513000000_rpc_financial_aggregates.sql` or the rewritten version from `20260710064319_drop_ventas_rewrite_rpcs.sql` for `rpc_count_pendientes`). Since both are pure functions with no data dependencies, rollback is a simple `CREATE OR REPLACE FUNCTION`.

## Dependencies

- Local Supabase stack running (`supabase start`)
- Migration `20260714000002_drop_rpc_saldo_pendiente.sql` may need to be skipped/merged depending on local state

## Success Criteria

- [ ] New migration applies without error via `supabase db reset`
- [ ] `SELECT COUNT(*) FROM information_schema.routines WHERE routine_name IN ('rpc_saldo_pendiente', 'rpc_count_pendientes')` returns 0
- [ ] `supabase db diff --linked` shows no unexpected schema changes
- [ ] Android `:optoapp:assembleDebug` succeeds (ensures no compilation regression)
