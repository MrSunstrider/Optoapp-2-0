# Spec Delta: `sync` (MODIFIED)

## ADDED Requirements

### Requirement: Inventory movement upload reconciles primary keys by composite key

When uploading `montura_movimientos`, the client SHALL treat `(referencia_id, tipo, montura_id)` as the identity of a movement fact. If a remote row already exists for that key with matching `stockNuevo` and a different `id`, the client SHALL adopt the remote `id` locally and SHALL NOT insert a second remote row.

#### Scenario: Local UUID differs from remote UUID for the same fact
- **Given** a local movimiento with `id=uuid-new` and a remote movimiento with `id=uuid-old`
- **And** both share `(referenciaId, tipo, monturaId)` and the same `stockNuevo`
- **When** inventario upload runs
- **Then** Room stores `id=uuid-old`
- **And** no POST body includes `id=uuid-new`
- **And** the module does not fail with `23505 / idx_movimientos_conflict`

#### Scenario: New movement has no remote composite match
- **Given** a local movimiento whose composite key is absent remotely
- **When** inventario upload runs
- **Then** the row is upserted to `montura_movimientos`

#### Scenario: Stock differs on the same composite key
- **Given** local and remote movimientos share the composite key but `stockNuevo` differs
- **When** inventario upload runs
- **Then** the local row is not uploaded
- **And** a `conflict_records` row of type `montura_movimiento` is persisted
