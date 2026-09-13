# GGA Report — harden_security_definer_grants

**Target**: `supabase/migrations/20260913212248_harden_security_definer_grants.sql` + prod ACL  
**Date**: 2026-09-13

| Reviewer | Verdict | Notes |
|----------|---------|-------|
| R1 Risk | CLEAN | No severe privilege/breakage findings; intentional 0028/0029 must stay |
| R3 Reliability | CLEAN (parent live) | Native review-binding N/A; call-site + contract SQL GREEN |
| R4 Resilience | CLEAN (parent live) | Fail-closed admin; anon rate-limit retained; rollback documented |

## R3 parent evidence

- Android sync still has `authenticated` EXECUTE on `recalcular_resumen_diario` (DownloadSyncCoordinator).
- optoweb `fetchEliminacionesRestantesHoy` runs after `getActiveOpticaContext` + cookie JWT → authenticated; anon revoke safe.
- optoweb login/register call `checkRateLimit` without JWT → anon EXECUTE required and retained.
- `recalcular_resumen_diario_admin` has zero app callers; locking to service_role cannot break clients.
- Contract test `supabase/tests/verify_security_definer_grants.sql` executed on prod → GREEN.

## R4 parent evidence

- Admin RPC without membership guard is fail-closed for client roles.
- Rollback grants documented in proposal (only if proven 42501 from live caller).
- Remaining advisor WARNs are not resilience defects; they document intentional API surface.

## Aggregate

**GGA: CLEAN** — leave intentional SECURITY DEFINER EXECUTE as-is for app function.
