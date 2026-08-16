# Spec: Homogeneous Sync Timestamps

## Capability: sync-timestamps

### Purpose

Guarantee one timestamp policy across every Room↔Supabase sync table so false
conflict storms cannot regenerate after upload.

### Requirements

#### REQ-1: Single timestamp trigger on sync tables
The system MUST attach exactly one BEFORE UPDATE function that mutates
`updated_at` on each of: `pacientes`, `evaluaciones`, `dispensaciones`, `pagos`,
`servicios_extra`, `monturas`, `montura_movimientos`. That function MUST be
`set_updated_audit_fields`.

#### REQ-2: Preserve client `updated_at`
When a client UPDATE/UPSERT supplies a non-null `updated_at`, the server MUST
persist that value unchanged on every sync table listed in REQ-1.

#### REQ-3: Null fallback only
`set_updated_audit_fields` MUST set `updated_at := timezone('utc', now())` only
when the incoming `updated_at` is NULL.

#### REQ-4: No legacy overwrite on sync tables
The system MUST NOT attach `update_updated_at` as a BEFORE UPDATE trigger on any
table listed in REQ-1.

#### REQ-5: Settings exception (documented)
`update_updated_at` MAY remain on `cierres_caja` and `optica_settings` only.
No other public table MAY use it for `updated_at` mutation.

#### REQ-6: Homogeneous enforcement
Schema integrity tests MUST fail if any single sync table violates REQ-1 or
REQ-4. A partial fix (e.g. only `pacientes`) is NOT acceptable.

#### REQ-7: Shared conflict recovery
Existing bulk conflict actions ("Usar el mío para todos" / "Usar nube para
todos") MUST remain the sole recovery UX for all entity types after the DB fix.
This change MUST NOT introduce entity-specific resolution paths.

### Scenarios

#### Scenario: Client stamp survives upsert (all sync tables)
- **Given** a row exists in each sync table of REQ-1
- **When** an UPDATE sets `updated_at` to a fixed past Instant `T`
- **Then** the stored `updated_at` equals `T` for every table

#### Scenario: Null stamp gets server now
- **Given** a row in `pacientes`
- **When** an UPDATE sets `updated_at` to NULL (if column allows) or the trigger
  sees NULL on NEW
- **Then** `set_updated_audit_fields` assigns `timezone('utc', now())`

#### Scenario: Legacy trigger absent
- **Given** production schema after this change
- **When** listing BEFORE UPDATE triggers that call `update_updated_at`
- **Then** only `cierres_caja` and `optica_settings` appear

#### Scenario: No per-table drift
- **Given** integrity suite DOMAIN sync-timestamps
- **When** any one of the five previously dual-trigger tables still has
  `*_updated_at` → `update_updated_at`
- **Then** the suite FAILS
