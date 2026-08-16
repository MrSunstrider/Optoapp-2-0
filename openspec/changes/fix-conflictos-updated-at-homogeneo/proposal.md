# Proposal: Homogeneous Client Timestamp Policy (Stop False Sync Conflicts)

## Intent

The device shows **406 sync conflicts**. They are not real two-device edits. Every
Room↔Supabase sync table that still carries a legacy `*_updated_at` →
`update_updated_at()` trigger has its client `updated_at` overwritten with
`timezone('utc', now())` on every UPSERT. The preserve-client fix in
`set_updated_audit_fields` never wins because Postgres runs the legacy trigger
first (alphabetical order). Local Room keeps the old stamp → next sync marks
conflict → download guard skips the row → conflict never heals.

**Homogeneity rule (non-negotiable):** one timestamp policy for the entire sync
surface. No per-table, per-screen, or per-module special case.

## Evidence (production `sflhtihqdhrlryeyrzdo`, 2026-08-15)

| Table | Rows | Mass-stamped minute (server clock) | Rows in that minute |
|-------|------|------------------------------------|---------------------|
| pacientes | 299 | 2026-08-15 18:24 UTC | **293** |
| evaluaciones | 297 | 2026-08-16 03:21 / 15 17:48 | **182 + 113** |
| dispensaciones | 301 | 2026-08-16 03:22 | **300** |
| pagos | 601 | 2026-08-16 03:23 | **587** |
| servicios_extra | 145 | 2026-08-16 03:22 | **145** |

Each of those five tables has **both**:
1. `<tabla>_updated_at` → `update_updated_at()` (unconditional overwrite)
2. `trg_<tabla>_set_updated_audit` → `set_updated_audit_fields()` (preserve if non-null)

`monturas` / `montura_movimientos` already follow the homogeneous rule (audit
trigger only) and do not mass-stamp.

Conflict detection still runs for pacientes + evaluaciones → ~406 open records
fits the 596-row universe. Finanzas tables suffer the same stamp overwrite but
no longer call `filterConflicts` — still in scope for the **same** DB fix so
future reintroduction of conflict checks cannot diverge.

## Scope

### In Scope
- Drop the five legacy `*_updated_at` triggers on sync tables
- Keep a **single** BEFORE UPDATE timestamp policy on all Room sync tables:
  `set_updated_audit_fields` (preserve client `updated_at`)
- Leave `update_updated_at` only on `cierres_caja` and `optica_settings`
  (settings tables without Room conflict detection — documented exception)
- Schema integrity + behavioral SQL tests that enforce the rule for **all** sync
  tables (fail if any one re-gains the legacy trigger)
- Device recovery via existing bulk conflict actions (same UX for every type)

### Out of Scope
- Different conflict logic per entity type
- Reintroducing `filterConflicts` only for finanzas (or only for pacientes)
- Three-way merge / field-level UI
- Rewriting historical `updated_at` values in cloud rows (not required once
  client stamps stop being overwritten)

## Capabilities

### New Capabilities
- `sync-timestamps`: homogeneous client-preserve `updated_at` for all Room sync tables

### Modified Capabilities
- `sync`: upload stamps survive server round-trip on every sync entity
- `sync-conflict-resolution`: bulk keep-mine / accept-cloud remain the single
  recovery path for existing false conflicts (already shared UI)

## Approach

1. RED: SQL integrity + behavior tests assert the homogeneous rule (fail on current prod schema).
2. GREEN: one migration drops the five legacy triggers; comment documents the rule.
3. Verify on prod: zero `update_updated_at` triggers on sync tables; UPDATE keeps client stamp on all five.
4. On device: "Usar el mío para todos" (or "Usar nube para todos") once; next sync must not regenerate the pile.

## Causal Invariants

1. Every Room↔Supabase sync table has **exactly one** timestamp BEFORE UPDATE
   trigger: `set_updated_audit_fields`.
2. Client-provided non-null `updated_at` is never overwritten by the server on
   those tables.
3. `update_updated_at` exists **only** on `cierres_caja` and `optica_settings`.
4. Conflict detection and resolution UI treat all entity types the same; the DB
   policy that feeds them is also the same.

## Affected Areas

`supabase/migrations/*` (new DROP migration); `supabase/tests/test_schema_integrity.sql`;
`supabase/tests/test_sync_timestamp_homogeneous.sql` (new); production triggers on
pacientes / evaluaciones / dispensaciones / pagos / servicios_extra.

## Chained PR Forecast

Single PR (Low–Med lines). Split only if GGA / review budget requires.

`Decision needed before apply: Yes — remote migration on production`
`Chained PRs recommended: No`
`400-line budget risk: Low`

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Web client relied on server-forced stamps | Low | optoweb only types `updated_at`; no writer dependency found |
| Stale Android build re-uploads after drop | Low | Preserve path already matches Android intent; install after migration |
| Existing 406 linger until user resolves | Med | Document one-shot bulk keep-mine / accept-cloud after fix |
| Dump migration `20260713053521` recreated triggers | High (already happened) | Integrity test prevents silent reintroduction |

## Rollback Plan

Re-create the five `*_updated_at` triggers pointing at `update_updated_at()` —
restores the previous (broken) behavior. Prefer fixing forward; rollback only if
an unexpected writer required server NOW().

## Dependencies

GGA before remote apply (AGENTS.md). ADB device CLK-LX3 for post-fix conflict clear.

## Success Criteria

- [ ] Zero `update_updated_at` triggers on the five sync tables
- [ ] All five still have `set_updated_audit_fields`
- [ ] Behavioral test: client `updated_at` preserved on UPDATE for all five
- [ ] Schema integrity domain for timestamps PASS
- [ ] After bulk resolve on device, conflict count stays near zero across syncs
- [ ] No per-entity special-case code introduced on Android for this fix
