# Spec Delta: `sync` (MODIFIED)

## MODIFIED Requirements

### Requirement: Finanzas upload is transport only

Finanzas upload SHALL upsert rows and record sync state. It SHALL NOT invoke side-effecting
RPCs that mutate inventory or any other aggregate outside the tables it transports.

#### Scenario: Dispensaciones upload performs no inventory mutation
- **Given** a full dispensaciones snapshot including items with `monturaId`
- **When** the finanzas upload stage runs
- **Then** only `dispensaciones` upserts and sync-state writes occur
- **And** no `montura_movimientos` row and no `monturas.stock_actual` value changes remotely

## ADDED Requirements

### Requirement: Inventory write failures are reported

An inventory-related write failure during sync SHALL be recorded through
`SyncStateTracker.markError` and SHALL NOT be reduced to a log line while the module reports
success.

#### Scenario: Failure is not swallowed
- **Given** an inventory write fails during a sync module
- **When** the module completes
- **Then** the failure is recorded in sync state
- **And** the module does not report unqualified success
