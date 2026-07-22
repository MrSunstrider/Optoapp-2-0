# Proposal: Fix Migration Security — GGA Review Findings

## Intent

4 confirmed bugs + 1 inconsistency found by GGA dual-judge review of 15 Supabase security/auth migrations. All fixes are new PostgreSQL migrations — no application code changes.

## Scope

### In Scope (5 fixes, 5 new migration files)

| Priority | ID | Fix |
|----------|-----|------|
| P0 | C-1 | Restore `check_rate_limit` dropped by cleanup |
| P0 | C-2 | Idempotent guard for foundational DO block |
| P0 | C-3 | Exclude Anulación from `rpc_cierre_caja_resumen` |
| P0 | C-4 | Add `is_optica_member` guard to `recalcular_resumen_diario` |
| P1 | W-3 | Add anon SELECT policy on `app_releases` |

### Out of Scope
- W-1, W-2 — false alarms, already fixed / not a bug
- No migration rewriting, no Android app changes, no edge function changes

## Capabilities

### New Capabilities
None — pure bug fixes and security hardening.

### Modified Capabilities
None — fixing existing behavior to match intended design. No spec-level behavior changes.

## Approach

5 new migration files, applied in order: C-3 → C-4 → C-1 → C-2 → W-3 (least risky first). Each is idempotent (`CREATE OR REPLACE FUNCTION`, `DROP POLICY IF EXISTS` + `CREATE POLICY`). All DDL, no data migration.

## Affected Areas (Schema)

| Area | Impact | Notes |
|------|--------|-------|
| `supabase/migrations/` | +5 new files | Sequential timestamps |
| `rpc_cierre_caja_resumen` | Modified | Added `AND tipo IS DISTINCT FROM 'Anulación'` |
| `recalcular_resumen_diario` | Modified | Added `is_optica_member` guard at function top |
| `check_rate_limit` | Restored | Verbatim from `20260628000000` |
| `app_releases` | Policy added | `anon_can_read_releases` FOR SELECT TO anon |
| 5 core tables RLS | Guarded | Rerun sentinel for foundational DO block |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| C-3 changes financial totals downward (correct) | Low | Voided payments should not count as revenue |
| C-4 guard rejects legitimate callers | Low | Checks optica membership, not role |
| W-3 exposes version metadata to unauthenticated | Low | APK URLs already public, no tenant data |

## Rollback

Each fix is a standalone DDL statement. Rollback: `DROP FUNCTION IF EXISTS` or `DROP POLICY IF EXISTS`.

## Success Criteria

- [ ] 5 new migration files created, each idempotent
- [ ] All migrations apply to remote Supabase without errors
- [ ] `check_rate_limit` exists and is executable by anon
- [ ] `recalcular_resumen_diario` rejects non-member callers
- [ ] `rpc_cierre_caja_resumen` excludes Anulación payments
- [ ] Foundational DO block has rerun guard
