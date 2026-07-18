# Proposal: Fix `recalcular_resumen_diario` — Silent Write Failure + Calculation Bugs

## Intent

The function `recalcular_resumen_diario` runs as `SECURITY INVOKER`, but `resumen_diario` only has a SELECT policy (no INSERT/UPDATE). Result: authenticated callers get **silent write failures** — the RPC computes everything correctly, then the final INSERT/UPDATE silently fails. Additionally, the function doesn't exclude `"Anulado"` estado from ventas, and uses `SET search_path = public` (injection risk).

## Scope

### In Scope
- Rewrite function as `SECURITY DEFINER` with `SET search_path = ''` to bypass RLS safely
- Add `estado IS DISTINCT FROM 'Anulado'` to all ventas queries
- `ALTER OWNER TO postgres` + `GRANT EXECUTE TO authenticated, service_role`
- New migration (`supabase/migrations/20260715000003_*`), fix-forward only

### Out of Scope
- `resumen_diario` table schema or RLS policy changes (function bypasses via SECURITY DEFINER)
- Android client changes (same signature, transparent upgrade)
- Other RPCs listed in the diagnosis (separate fixes)

## Capabilities

### New Capabilities
None — pure backend bugfix, no new feature.

### Modified Capabilities
None — behavior is equivalent from the caller's perspective.

## Approach

`CREATE OR REPLACE FUNCTION` with:
1. `SECURITY DEFINER` — bypass RLS on the final upsert
2. `SET search_path = ''` — prevent search-path privilege escalation
3. Exclude `estado IS DISTINCT FROM 'Anulado'` on dispensaciones and servicios_extra queries
4. `COALESCE` on all `SUM()` expressions
5. `ON CONFLICT (optica_id, fecha) DO UPDATE` — idempotent upsert (already present)
6. `ALTER FUNCTION ... OWNER TO postgres` — required for SECURITY DEFINER
7. `GRANT EXECUTE TO authenticated, service_role` — app & backend access

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/20260715000003_fix_recalcular_resumen_diario_security.sql` | New | Migration with `CREATE OR REPLACE FUNCTION` |
| `public.recalcular_resumen_diario` | Modified | Function body: SECURITY DEFINER, Anulado filter |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SECURITY DEFINER escalates privileges beyond intent | Low | `SET search_path = ''` prevents search-path hijack; function only writes to `resumen_diario` |
| Callers break due to different function signature | Low | Same `(text, date) → void` signature — verified |
| Existing grants revoked by new migration | Low | Explicit `GRANT EXECUTE` covers all current roles |

## Rollback Plan

```sql
DROP FUNCTION IF EXISTS public.recalcular_resumen_diario(text, date);
-- Then re-apply the previous migration's CREATE OR REPLACE
```

## Dependencies

- Supabase local CLI running (`supabase start`)
- Previous migration `20260714000000_fix_recalcular_resumen_diario` already applied
- No external dependencies

## Success Criteria

1. `SELECT recalcular_resumen_diario('test_optica_id', CURRENT_DATE)` returns void (no error)
2. `SELECT * FROM resumen_diario` includes a row for that optica+date with non-null values
3. The function survives `supabase db reset` (migration replay)
4. Anulado dispensaciones/servicios are excluded from totals
