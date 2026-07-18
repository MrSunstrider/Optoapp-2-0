# Tasks: Fix broken `assign_optica_role_by_email` RPC

## Review Workload Forecast

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: TDD — Write Validation Test First

- [x] 1.1 Run `supabase start` + `supabase db reset` to get a clean local DB state
- [x] 1.2 Write `supabase/tests/verify_assign_optica_role_rpc.sql` — test calls as `authenticated` role expect `42501` (permission denied), test internal role check expects proper reject for non-admin
- [x] 1.3 Run the test SQL to confirm RED state: RPC is NOT callable by `authenticated`

## Phase 2: Migration

- [x] 2.1 `supabase migration new fix_assign_optica_role_rpc` — creates `supabase/migrations/<timestamp>_fix_assign_optica_role_rpc.sql`
- [x] 2.2 Write `GRANT EXECUTE ON FUNCTION public.assign_optica_role_by_email(text, text, text) TO authenticated;` in the new migration file
- [x] 2.3 `supabase db reset` — applies all migrations including the new one

## Phase 3: Verification

- [x] 3.1 Run `supabase/tests/verify_assign_optica_role_rpc.sql` — GREEN: `authenticated` can now call the RPC
- [x] 3.2 Run internal-role-check test — non-admin/gerente caller gets rejected by function body
- [x] 3.3 Run `supabase db diff` — verify only the new migration's GRANT appears in output
- [-] 3.4 **WAITING: `supabase db push`** — deploy to remote (requires human confirmation before proceeding)
