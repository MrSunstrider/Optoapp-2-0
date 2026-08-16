# Tasks: fix-conflictos-updated-at-homogeneo

## Phase 0 — SDD artifacts
- [x] 0.1 proposal.md (homogeneous scope)
- [x] 0.2 specs/sync-timestamps/spec.md
- [x] 0.3 design.md
- [x] 0.4 tasks.md

## Phase 1 — RED (TDD)
- [x] 1.1 Add `supabase/tests/test_sync_timestamp_homogeneous.sql`
      - Assert sync tables have `set_updated_audit_fields` only
      - Assert `update_updated_at` only on cierres_caja + optica_settings
      - Behavioral preserve-client UPDATE on pacientes, evaluaciones,
        dispensaciones, pagos, servicios_extra (transaction + ROLLBACK)
- [x] 1.2 Extend `test_schema_integrity.sql` with DOMAIN 6 sync-timestamps
- [x] 1.3 Confirm RED against production: preserve probe returned
      `pacientes=false evaluaciones=false dispensaciones=false pagos=false
      servicios_extra=false` (self-reverting probe, nothing persisted)

## Phase 2 — GREEN
- [x] 2.1 Migration: DROP TRIGGER IF EXISTS for the five legacy `*_updated_at`
- [x] 2.2 Comment in migration stating the homogeneous invariant
- [x] 2.3 Re-run preserve probe: all five now `true`

## Phase 3 — Apply & verify
- [x] 3.1 Review gate: GGA unusable (provider `deepseek-v4-pro` requires China
      opt-in). Used Cursor Bugbot instead — 1 finding (invalid SQL fixtures),
      fixed, re-review clean.
- [x] 3.2 Apply migration to production `sflhtihqdhrlryeyrzdo`
- [x] 3.3 Prod trigger inventory matches the invariant exactly
- [ ] 3.4 Device: bulk resolve the 406 conflicts; confirm count stays near zero

## Phase 4 — Archive
- [x] 4.1 verify-report.md
- [ ] 4.2 Archive change after device confirmation
