# Exploration: fix-migration-security-gga

> GGA dual-judge review findings on 15 Supabase security/auth migrations.
> 7 findings explored. 4 confirmed real bugs, 2 false alarms, 1 low-severity inconsistency.

---

## C-1: check_rate_limit was dropped by cleanup migration

### Current State

Migration `20260716050000_cleanup_dead_rpcs.sql` drops 7 functions, including:
```sql
DROP FUNCTION IF EXISTS public.check_rate_limit(TEXT, INT, INT);
```

The original function was created/replaced in migration `20260628000000_grant_anon_check_rate_limit.sql`, which explicitly documents:
> "The web app calls check_rate_limit during login (before auth), using the anon key."

The function is `SECURITY DEFINER`, operates only on `pin_attempts` (rate-limiting data, no user data), and is used by the **web companion** (`optoapp-web`, separate repo) during unauthenticated login flows.

The Android app does NOT call this function — it was correctly identified as unused in the Android codebase. But the cleanup scoped itself to "functions no longer used by the Android app" and missed the web companion's dependency.

No edge function exists for `check_rate_limit` — only `track-release` and `verify-purchase` exist in `supabase/functions/`.

### Root Cause

The cleanup migration was **Android-scoped** but the function is consumed by the **web companion** (separate deployment, separate repo). The grep only covered the Android codebase.

### Affected Files

- `supabase/migrations/20260716050000_cleanup_dead_rpcs.sql` — DROP line
- `supabase/migrations/20260628000000_grant_anon_check_rate_limit.sql` — original CREATE OR REPLACE (source of truth for the function body)
- `supabase/migrations/20260627054251_anon_policy_pin_attempts.sql` — created the original function (replaced by the above)

### Fix Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A: Restore function** via new migration | Clean, explicit, one-time fix | Another migration to maintain | Low |
| **B: Revert the DROP IF EXISTS in cleanup** | Would fix if cleanup hasn't run | Migration is already deployed; can't edit committed files | Med |
| **C: Move function to edge function** | Better separation, portable | Overengineering for a rate-limiter; breaks existing web companion | High |

### Recommended Fix

**Approach A** — Create a new migration that re-runs the `CREATE OR REPLACE FUNCTION` from `20260628000000_grant_anon_check_rate_limit.sql` verbatim (SECURITY DEFINER, SET search_path, full body), plus the REVOKE/GRANT statements. Do NOT recreate the anon policy on `pin_attempts` (it was dropped in the grant migration and is not needed with SECURITY DEFINER).

After this fix, the function exists again and the web companion's login flow works.

### Risk Assessment

- **Risk**: Low. The function body is unchanged from its last known-good version. Idempotent operation (CREATE OR REPLACE).
- **Blast radius**: Only affects login rate-limiting for the web companion. Android is not affected.
- **Rollback**: Run DROP FUNCTION IF EXISTS again.

### Ready for Proposal

**Yes** — root cause identified, fix approach clear.

---

## C-2: Foundational DO block drops ALL policies on 5 core tables on rerun

### Current State

Migration `20260414120000_opticas_usuario_optica_rls.sql` contains a DO block (lines 90-100):
```sql
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT policyname, tablename FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN ('pacientes','evaluaciones','dispensaciones','servicios_extra','pagos')
    ) LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', r.policyname, r.tablename);
    END LOOP;
END $$;
```

After dropping all policies, it creates basic member-only policies (any member can CRUD).

Later migrations refine these policies:

1. **`20260507000000_fix_business_tables_rls_policies.sql`** — Replaces with `has_optica_role` restricted to `['admin', 'gerente']` (all CRUD). This was too restrictive and was itself superseded.

2. **`20260518000000_rls_role_based_permissions.sql`** — Final role matrix:
   - **SELECT**: `is_optica_member` (all members can read)
   - **INSERT/UPDATE**: Role-dependent per table
   - **DELETE**: `admin/gerente` only

If the foundational migration is re-run (branch reset, disaster recovery, manual SQL replay), it:
1. Destroys ALL policies from steps 1 and 2
2. Replaces them with basic member-only CRUD
3. Security regression: INSERT/UPDATE/DELETE open to ALL members, not just authorized roles

### Root Cause

The DO block uses `DROP IF EXISTS` with no guard. The migration was designed as a one-time foundational setup, not accounting for being re-run in a chain of migrations.

### Affected Files

- `supabase/migrations/20260414120000_opticas_usuario_optica_rls.sql` — DO block (lines 90-100), basic policy creation (lines 105-154)
- `supabase/migrations/20260507000000_fix_business_tables_rls_policies.sql` — role-restricted policies (now obsolete but still in chain)
- `supabase/migrations/20260518000000_rls_role_based_permissions.sql` — current correct role matrix

### Fix Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A: Add version guard** in new migration that checks if restrictive policies exist before allowing rerun | Preserves idempotency; doesn't modify old migration | Slightly indirect | Low |
| **B: Wrap DO block in IF NOT EXISTS check** against a sentinel policy (e.g., `pacientes_select` with `has_optica_role`) | Straightforward skip logic | Requires knowing exact policy name from a later migration | Low |
| **C: Document as known risk** | No code change | Doesn't fix the problem; disaster recovery manual step | None |
| **D: New migration that re-applies correct policies after any re-run** | Simple; establishes idempotency | Redundant CREATE OR REPLACE of existing policies every time | Low |

### Recommended Fix

**Approach B** — Create a new migration that checks for a sentinel policy before allowing the destructive drop. Specifically:
```sql
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'pacientes'
          AND policyname = 'pacientes_select'
          AND LOWER(pg_get_expr(polqual, polrelid)::text) LIKE '%has_optica_role%'
    ) THEN
        -- Re-run the foundational DO block logic
        -- (re-create basic member policies)
    ELSE
        RAISE NOTICE 'Restrictive policies already exist — skipping foundational DO block';
    END IF;
END;
$$;
```

Actually, better approach: **Never modify old migrations**. Create a new migration that is safe to rerun and restores correct policies if they are missing. The new migration should:

1. NOT drop anything unconditionally
2. Only create policies if they don't exist (or use CREATE OR REPLACE with DROP IF EXISTS)
3. Be idempotent

### Risk Assessment

- **Risk**: Low-Medium. The guard prevents accidental policy destruction.
- **Blast radius**: Only affects disaster recovery / migration replay scenarios.
- **Rollback**: Revert the guard migration.

### Ready for Proposal

**Yes** — root cause identified, multiple viable fix approaches.

---

## C-3: rpc_cierre_caja_resumen includes Anulación payments

### Current State

In migration `20260716045310_fix_financial_rpcs_security.sql`, the function `rpc_cierre_caja_resumen` calculates payment totals grouped by payment method (efectivo, transferencia, tarjeta). Its WHERE clause (lines 309-311):
```sql
WHERE optica_id = p_optica_id
  AND fecha >= p_from
  AND fecha < p_to;
```

Compare with `rpc_deudores` (same migration, line 53):
```sql
WHERE pg.optica_id = p_optica_id
  AND pg.tipo IS DISTINCT FROM 'Anulación'
```

And `rpc_analisis_mensual` (same migration, line 198):
```sql
WHERE pg.optica_id = p_optica_id
  AND pg.tipo IS DISTINCT FROM 'Anulación'
```

Also, `recalcular_resumen_diario` in migration `20260716012521` correctly filters (line 67):
```sql
AND tipo IS DISTINCT FROM 'Anulación';
```

The `pagos.tipo` column stores payment types where `'Anulación'` means a void/reversal. Including these in cierre_caja inflates the totals — voided payments should not count as collected revenue.

### Root Cause

Oversight during the consolidated fix migration. `rpc_cierre_caja_resumen` was part of the same `20260716045310` migration that fixed the other RPCs, but the `IS DISTINCT FROM 'Anulación'` filter was missed.

### Affected Files

- `supabase/migrations/20260716045310_fix_financial_rpcs_security.sql` — `rpc_cierre_caja_resumen` function body (lines 275-320)

### Fix Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A: Add `AND tipo IS DISTINCT FROM 'Anulación'`** to the WHERE clause | Consistent with all other RPCs; one-line change | Requires CREATE OR REPLACE of the function | Low |
| **B: Re-deploy the whole consolidated migration** | Single deployment | Touches all 3 RPCs; higher blast radius | Med |

### Recommended Fix

**Approach A** — Create a new migration that does a `CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(...)` with the exact same body as the current version, plus the missing filter in the WHERE clause. Keep all security checks, grant statements unchanged.

```sql
-- In the WHERE clause, after "AND fecha < p_to;"
-- Add: AND tipo IS DISTINCT FROM 'Anulación'
```

### Risk Assessment

- **Risk**: Low. One-line change, consistent with 3 other RPCs in the same codebase.
- **Blast radius**: Only affects `rpc_cierre_caja_resumen` output. Financial reports using this function will see slightly lower totals (correctly excluding voided payments).
- **Rollback**: Re-deploy the current version without the filter.

### Ready for Proposal

**Yes** — root cause is a clear oversight; fix is a one-line addition.

---

## C-4: recalcular_resumen_diario SECURITY DEFINER without membership check

### Current State

Migration `20260716012521_fix_recalcular_resumen_security.sql` recreates `recalcular_resumen_diario` as:
```sql
CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER   -- ⚠️ runs as owner
SET search_path = ''                -- ✅ empties search path (safe)
```

The function is `SECURITY DEFINER` (runs as `postgres` owner, set via `ALTER FUNCTION ... OWNER TO postgres`). However, there is **no `app_private.is_optica_member(auth.uid(), p_optica_id)` guard**.

Any authenticated user can call this function with ANY `optica_id` and it will recalculate the daily summary for that optica, bypassing the caller's membership.

Compare with `rpc_deudores` and `rpc_analisis_mensual` in migration `20260716045310_fix_financial_rpcs_security.sql`, which both have:
```sql
IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
    RAISE EXCEPTION 'Access denied';
END IF;
```

### Root Cause

The `SECURITY DEFINER` was added intentionally (the migration comment says SECURITY INVOKER → SECURITY DEFINER), but the corresponding membership check was never added. The assumption was that `SET search_path = ''` provides sufficient protection, but it does not prevent cross-tenant writes.

### Affected Files

- `supabase/migrations/20260716012521_fix_recalcular_resumen_security.sql` — full function body (lines 14-139)

### Fix Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A: Add `is_optica_member` + role guard** | Consistent with other SECURITY DEFINER RPCs; prevents cross-tenant writes | Adds precondition check to a batch operation | Low |
| **B: Switch to SECURITY INVOKER** | Removes the privilege escalation entirely | The function modifies `resumen_diario` which needs elevated privileges for summary calculation; may break | Med |
| **C: Add only membership check** | Minimal change; prevents cross-tenant access | No role check (any member of the optica can recalculate) — but this matches the original intent (it's a maintenance function) | Low |

### Recommended Fix

**Approach C** — Add the membership guard at the top of the function body, consistent with the pattern used in `rpc_deudores` and `rpc_analisis_mensual`:

```sql
IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
    RAISE EXCEPTION 'Access denied';
END IF;
```

This prevents cross-tenant data corruption while keeping the function accessible to any optica member (not just admin/gerente), which matches the function's nature as a recalculation utility.

### Risk Assessment

- **Risk**: Low. Adding a precondition check to a function that already takes `p_optica_id` as a parameter.
- **Blast radius**: Only the `recalcular_resumen_diario` call path. Existing callers already pass the correct optica_id.
- **Rollback**: Re-deploy the function without the guard.

### Ready for Proposal

**Yes** — clear root cause, straightforward fix, consistent with existing patterns.

---

## W-1: optica_members view without security_invoker

### Current State

Migration `20260423044000_role_management_membership_admin.sql` creates the view:
```sql
CREATE OR REPLACE VIEW public.optica_members AS
SELECT uo.optica_id, uo.user_id, coalesce(up.email, '') as email, uo.rol, uo.created_at
FROM public.usuario_optica uo
LEFT JOIN public.user_profiles up ON up.user_id = uo.user_id;
```

No `WITH (security_invoker = true)` clause. PostgreSQL < 15 defaults to SECURITY DEFINER (view runs as owner, bypassing RLS on underlying tables).

However, migration `20260423084500_optica_members_security_invoker.sql` (created the SAME day, 84500 vs 44000) already fixes this:
```sql
ALTER VIEW public.optica_members SET (security_invoker = true);
```

### Verdict

**FALSE ALARM / ALREADY FIXED** — The `security_invoker` was set in a follow-up migration deployed in the same day. The view now correctly enforces RLS on the underlying `usuario_optica` and `user_profiles` tables.

### Root Cause

Was a real issue at the time of creation (missing `security_invoker` in the initial view definition), but was caught and fixed ~4 hours later in the same deployment cycle.

### Affected Files (historical only)

- `supabase/migrations/20260423044000_role_management_membership_admin.sql` — creates view without security_invoker
- `supabase/migrations/20260423084500_optica_members_security_invoker.sql` — fixes it

### Recommendation

No action needed. Close as "Already Fixed".

### Ready for Proposal

**No** — this is a non-issue.

---

## W-2: invitaciones RLS without ENABLE ROW LEVEL SECURITY

### Current State

Migration `20260714000006_add_invitaciones_rls.sql` creates 4 policies on `invitaciones` (SELECT, INSERT, UPDATE, DELETE) but does NOT include:
```sql
ALTER TABLE public.invitaciones ENABLE ROW LEVEL SECURITY;
```

However, the original table creation migration `20260516000000_invitaciones.sql` (line 14) already enables RLS:
```sql
ALTER TABLE public.invitaciones ENABLE ROW LEVEL SECURITY;
```

### Verdict

**FALSE ALARM / NOT A BUG** — RLS was already enabled when the table was created (May 16, 2026). The July 14 migration only needed to add policies, not re-enable RLS. The policies are fully active.

### Root Cause

Misreading of the migration chain — policies were added to a table that already had RLS enabled from its creation migration.

### Affected Files (none — no fix needed)

### Recommendation

No action needed. Close as "Not a Bug".

### Ready for Proposal

**No** — this is a non-issue.

---

## W-3: app_releases SELECT grant to anon without RLS policy

### Current State

Migration `20260521000000_app_releases.sql` creates the table with RLS enabled and policies:
- SELECT: `authenticated` only (using true)
- INSERT: `service_role` only
- UPDATE: `service_role` only

Migration `20260714000005_security_hardening_rls_grants.sql` Block 3:
```sql
REVOKE ALL ON public.app_releases FROM anon;
GRANT SELECT ON public.app_releases TO anon;
```

The grant comment says "Public only needs SELECT to check for app updates."

However, the table has RLS enabled and there is NO SELECT policy for `anon`. In PostgreSQL RLS semantics:
1. Table-level: anon has SELECT privilege (via GRANT) ✓
2. RLS check: no policy permits anon to SELECT ✗
3. Result: anon queries return empty results — the GRANT is inert

### Root Cause

The migration intended to allow public (anon) version checks but only modified table-level grants, not RLS policies. With RLS enabled, table-level GRANTs are insufficient — a policy must also permit the row access.

### Impact Assessment

The Android app queries `app_releases` **authenticated** (per the original migration comment: "consulta mediante Supabase client autenticado"). So the missing anon policy does NOT affect the app's ability to check for updates. It only affects potential **unauthenticated** version-checking flows (e.g., a public download page or web companion update check).

### Affected Files

- `supabase/migrations/20260521000000_app_releases.sql` — original table with authenticated-only SELECT policy
- `supabase/migrations/20260714000005_security_hardening_rls_grants.sql` — Block 3 (grants SELECT to anon without policy)

### Fix Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A: Add anon SELECT policy** with `FOR SELECT TO anon USING (true)` | Matches the grant's intent; enables public version checks | Slightly broader access than current model | Low |
| **B: Remove `GRANT SELECT TO anon`** | Clean; removes misleading grant | The GRANT was intentional per migration comment; removing changes intent | Low |
| **C: Both — add policy + keep grant** | Fully aligned with the intent of 20260714000005 | Exposes version data to unauthenticated requests | Low |

### Recommended Fix

**Approach A** — Add an anon SELECT policy to align with the migration's stated intent ("Public only needs SELECT to check for app updates"):
```sql
CREATE POLICY "anon_can_read_releases" ON public.app_releases
  FOR SELECT TO anon
  USING (true);
```

If public version-checking is not desired, **Approach B** is cleaner: remove the `GRANT SELECT TO anon` as dead code. This depends on whether the web companion or any unauthenticated flow needs to check for app updates.

**Decision needed**: Is public (unauthenticated) version-checking intentional?

### Risk Assessment

- **Risk**: Low. `app_releases` contains only version metadata and APK download URLs, no tenant data.
- **Blast radius**: Version information becomes available without auth. APK URLs were already public (they're download links).
- **Rollback**: DROP POLICY IF EXISTS.

### Ready for Proposal

**Yes** — but requires a decision on whether public access is intended.

---

## Overall Summary

| Finding | Severity | Status | Ready for Proposal |
|---------|----------|--------|-------------------|
| **C-1** — check_rate_limit dropped | Critical | **Confirmed bug** | Yes |
| **C-2** — DO block destroys policies on rerun | Critical | **Confirmed bug** | Yes |
| **C-3** — Anulación included in cierre_caja | High | **Confirmed bug** | Yes |
| **C-4** — recalcular_resumen without membership guard | High | **Confirmed bug** | Yes |
| **W-1** — optica_members security_invoker | Warning | **Already fixed** (Apr 23) | No |
| **W-2** — invitaciones ENABLE RLS | Warning | **Not a bug** (RLS was enabled) | No |
| **W-3** — app_releases anon SELECT without policy | Low | **Inconsistency** — needs decision | Yes (blocked on question) |

### Fix Migration Order

For the proposal, the recommended order of applying fixes:

1. **C-3** — rpc_cierre_caja_resumen (`AND tipo IS DISTINCT FROM 'Anulación'`) — one-line, zero risk, financial correctness
2. **C-4** — recalcular_resumen_diario membership guard — clear security hole
3. **C-1** — Restore check_rate_limit — web companion login is broken
4. **C-2** — Add guard for foundational DO block — protects disaster recovery
5. **W-3** — app_releases anon policy or grant removal (pending decision)

### Total Effort Estimate

- 4-5 new migration files (one per fix)
- Average ~30-50 lines each
- All use standard patterns already in the codebase
- No application code changes needed
