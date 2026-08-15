# Delta for sync-state-tracking

## ADDED Requirements

### Requirement: Truthful Partial Sync Status for Quarantined Rows

When finanzas upload partially succeeds, SyncStateTracker (or equivalent) MUST `markSynced` only for successfully uploaded entities and MUST `markError` for quarantined/failed entities. Batch-level status MUST remain partial/error while any quarantined row exists in that attempt; it MUST NOT report full success.

#### Scenario: Partial batch marks mixed state

- GIVEN 79 uploaded OK and 1 quarantined
- WHEN state updates flush
- THEN 79 entities MUST be `markSynced`
- AND the quarantined entity MUST be `markError` with a durable reason
- AND batch status MUST NOT claim all-remote-ok

#### Scenario: Negative control — error cleared only after successful re-upload

- GIVEN an entity is in `markError` quarantine
- WHEN a later sync still fails validation
- THEN error state MUST remain
- AND only a subsequent successful upload MUST `markSynced` and clear the error

#### Scenario: Multi-tenant state keys stay isolated

- GIVEN the same entity id namespace used across optics (should not collide) or parallel optica syncs
- WHEN markSynced/markError run for optica A
- THEN optica B sync state MUST be unchanged

### Requirement: Sanitized Durable Diagnostics and Copy-All

Diagnostics for sync/ledger failures MUST persist sanitized fields suitable for support copy-all: entity type/id, optica id, failure class (CHECK/domain/network), SQLSTATE or HTTP status when known, and redacted message. Secrets (JWT, anon keys, passwords, full PII payloads) MUST NOT appear in copy-all output.

#### Scenario: Copy-all omits secrets

- GIVEN a failed upsert that included an Authorization header in the underlying exception
- WHEN the user invokes diagnostics copy-all
- THEN the clipboard text MUST omit bearer tokens and keys
- AND MUST include failure class and entity identifiers

#### Scenario: Durable across process restart

- GIVEN a quarantined row with diagnostics recorded
- WHEN the app process restarts before remediation
- THEN quarantine/error state and sanitized diagnostic summary MUST still be available

#### Scenario: Pacientes evidence fields captured separately

- GIVEN a pacientes HTTP failure evidence capture event
- WHEN diagnostics are exported
- THEN HTTP status and sanitized body excerpt MUST be present when available
- AND no implication that pacientes was fixed by this change MUST appear
