# Proposal: Fix broken `assign_optica_role_by_email` RPC

## Intent

Migration `20260427060000_restrict_authenticated_security_definer_execute.sql` revoked EXECUTE on `assign_optica_role_by_email` from `authenticated`. This broke the role management feature — admin/gerente users cannot assign or modify roles from the Android app. The function has internal role verification (only admin/gerente can assign), so re-granting is safe.

## Scope

### In Scope
- Create one forward-fixing Supabase migration to `GRANT EXECUTE ... TO authenticated` on `assign_optica_role_by_email`
- Local validation via `supabase db reset` + manual SQL test

### Out of Scope
- No changes to other RPCs or functions
- No Android code changes
- No RLS policy changes
- No schema or data migrations

## Capabilities

> No spec-level behavior changes — this is an operational permission fix.

### New Capabilities

None — function already exists and works once permissions are restored.

### Modified Capabilities

None — no requirements or contracts change.

## Approach

1. Run `supabase migration new fix_assign_optica_role_rpc` locally
2. Write single `GRANT EXECUTE ON FUNCTION public.assign_optica_role_by_email(...) TO authenticated;`
3. Validate with `supabase db reset` + test SQL
4. Deploy via `supabase db push`

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `supabase/migrations/YYYYMMDDHHMMSS_fix_assign_optica_role_rpc.sql` | New | Re-grants EXECUTE to authenticated |
| RLS / Schema | None | No RLS or schema changes |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Re-exposing a function that should stay restricted | Low | Function validates caller role internally — only admin/gerente pass |
| Regression if migration order matters | Low | Forward-fix depends only on the parent migration having run first |

## Rollback Plan

Drop the new migration and redeploy: `supabase migration repair --status reverted` + `supabase db push`. The function returns to revoked state.

## Success Criteria

- [ ] `GRANT EXECUTE` migration applied successfully in local `supabase db reset`
- [ ] Admin/gerente users can call `assign_optica_role_by_email` without permission error
- [ ] Non-admin users still correctly rejected by internal role check
