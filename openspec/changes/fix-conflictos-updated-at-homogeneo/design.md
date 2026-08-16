# Design: Homogeneous Client Timestamp Policy

## Decision

**One policy for the entire sync surface:** client-provided `updated_at` is
authoritative. Server stamps only when NULL, via `set_updated_audit_fields`.

Drop the five legacy triggers that still call `update_updated_at()` on:
`pacientes`, `evaluaciones`, `dispensaciones`, `pagos`, `servicios_extra`.

Do **not** special-case Android modules. Do **not** reintroduce
`filterConflicts` only for finanzas. The false conflicts are a DB stamp bug;
fix the stamp rule once for all tables.

## Why the prior fix failed

| Function | Behavior | Attached to |
|----------|----------|-------------|
| `set_updated_audit_fields` | Preserve non-null client `updated_at` | All sync tables (intended) |
| `update_updated_at` | Always `updated_at := now()` | Sync tables **and** settings |

Postgres runs BEFORE ROW triggers alphabetically. Names like
`pacientes_updated_at` sort before `trg_pacientes_set_updated_audit`, so the
unconditional overwrite runs first and nullifies the preserve policy.

Migration `20260621010354` / `20260703054244` fixed the function body but left
the competing triggers. Dump `20260713053521_remote_schema.sql` re-asserted them.

## Architecture

```
Android Room (updatedAt = Instant.now())
        │ upsert
        ▼
Supabase table
  BEFORE UPDATE:
    [REMOVED] *_updated_at → update_updated_at()   ← was overwriting
    [KEPT]    trg_*_set_updated_audit → set_updated_audit_fields()
        │
        ▼
stored updated_at == client Instant
        │
        ▼
ConflictHelper.isLocalNewerOrEqual → equal/local newer → no false conflict
```

## Settings exception

`cierres_caja` and `optica_settings` keep `update_updated_at`. They are not part
of the Room conflict_records pipeline. Documented in REQ-5; integrity test
allows only these two.

## Recovery of the existing 406

After migration:
1. User opens Conflictos → "Usar el mío para todos" (or "Usar nube para todos").
2. Keep-mine bumps local `updatedAt` and uploads; with preserve policy, remote
   stamp matches → no re-detection.
3. Accept-cloud clears records and downloads; Room receives remote stamps.

Same buttons for every entity type — no new UI.

## Testing strategy (TDD)

1. **RED** `supabase/tests/test_sync_timestamp_homogeneous.sql`
   - Assert no `update_updated_at` on sync tables
   - Assert `set_updated_audit_fields` present on all sync tables
   - Behavioral: UPDATE with fixed `T` preserves `T` on all five dual-trigger tables
2. **GREEN** migration drops the five triggers
3. Extend `test_schema_integrity.sql` with DOMAIN sync-timestamps (same asserts)
4. Prod verification via `execute_sql` after apply

## Alternatives considered

| Option | Rejected because |
|--------|------------------|
| Fix only pacientes/evaluaciones | Violates homogeneity |
| Change ConflictHelper to ignore small skew | Hides stamp bug; diverges per module |
| Re-add filterConflicts only for finanzas | Second policy; regenerates noise until DB fixed |
| Rewrite all remote `updated_at` to local | Unnecessary once overwrite stops; risky |

## Risks & mitigations

See proposal. Primary residual risk: user must clear the 406 once after migration.
