# Spec Delta: `inventario-stock` (NEW)

## ADDED Requirements

### Requirement: Single writer for sale stock effects

A sale's stock effect SHALL be written exactly once, by the local dispensación save path.
Sync SHALL transport movement rows and the montura snapshot; it SHALL NOT derive or re-apply
a stock delta.

#### Scenario: Finanzas upload does not adjust stock
- **Given** a dispensación item with a non-blank `monturaId`
- **When** `UploadSyncCoordinator.uploadDispensaciones` runs
- **Then** no remote stock-adjustment RPC is invoked
- **And** `UploadSyncCoordinator` exposes no stock-adjustment API

#### Scenario: Local save remains the writer
- **Given** a dispensación is saved with a Tienda montura
- **When** the save completes
- **Then** Room holds one `SALIDA_VENTA` movement for `(dispensacionId, monturaId)`
- **And** `monturas.stockActual` is decremented exactly once

### Requirement: Movement identity and replay

`(referenciaId, tipo, monturaId)` SHALL identify one movement fact. A replay of an already
recorded movement SHALL be a no-op, not an error.

#### Scenario: Replay is idempotent
- **Given** a movement already exists for `(referencia_id, tipo, montura_id)`
- **When** `rpc_adjust_montura_stock` is called again with those values
- **Then** it returns `ok = true` with `idempotent = true`
- **And** `monturas.stock_actual` is unchanged
- **And** no duplicate movement row is created

#### Scenario: Unique index is retained
- **When** the schema is inspected
- **Then** `idx_movimientos_conflict (referencia_id, tipo, montura_id)` still exists

### Requirement: Tenant and role guard on stock RPC

`rpc_adjust_montura_stock` is `SECURITY DEFINER` and therefore bypasses RLS. It SHALL verify
the caller holds a write role in the target óptica before mutating anything.

#### Scenario: Caller outside the óptica is denied
- **Given** a caller who is not a member of `p_optica_id`
- **When** `rpc_adjust_montura_stock` is called
- **Then** it returns `ok = false` with `error = 'forbidden'`
- **And** neither stock nor the movement ledger changes

#### Scenario: Insufficient stock is refused without mutation
- **Given** a montura whose `stock_actual + p_delta` would be negative
- **When** `rpc_adjust_montura_stock` is called
- **Then** it returns `ok = false` with `error = 'insufficient'`
- **And** `stock_actual` is unchanged

### Requirement: Phantom sale movements are purged

Movement rows written by the retired second writer SHALL be removed from both Room and the
remote database, without altering `stock_actual` or any `SALIDA_VENTA` row.

#### Scenario: Phantom row is purged
- **Given** a row with `tipo = 'venta'`, `nota = 'venta_dispensacion'`
- **And** a `SALIDA_VENTA` row with the same `(referencia_id, montura_id, optica_id)`
- **When** the purge runs
- **Then** the `venta` row is deleted
- **And** the `SALIDA_VENTA` row and `stock_actual` are unchanged

#### Scenario: Unmatched venta row is preserved
- **Given** a row with `tipo = 'venta'` and no matching `SALIDA_VENTA`
- **When** the purge runs
- **Then** the row is preserved

### Requirement: Deterministic stock reconstruction

Reconstructing `stockActual` from the movement ledger SHALL be deterministic when several
movements share the same `fecha`.

#### Scenario: Same-date movements resolve deterministically
- **Given** two movements for one montura with the same `fecha` and different `stockNuevo`
- **When** `syncStockFromMovimientos` runs twice
- **Then** both runs produce the same `stockActual`
