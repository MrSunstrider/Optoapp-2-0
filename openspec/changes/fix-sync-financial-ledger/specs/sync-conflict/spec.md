# Delta for sync-conflict

## ADDED Requirements

### Requirement: Finanzas Quarantine Is Not Conflict Detection

Invalid finanzas rows MUST be handled by quarantine/error tracking, not by restoring broad `filterConflicts` / conflict_records gates for the finanzas ledger. PRD last-write-wins (LWW) for finanzas MUST remain authoritative for valid timestamped entities.

#### Scenario: Valid overlapping edits use LWW

- GIVEN local and remote valid pagos differ only by `updatedAt`
- WHEN upload/download reconciliation runs
- THEN the newer timestamp MUST win per LWW
- AND no new conflict_record MUST be required solely for that finanzas pair

#### Scenario: Negative control — poison row does not open conflict UI path

- GIVEN a poison pago fails CHECK/domain validation
- WHEN quarantine records the failure
- THEN the system MUST NOT create a user-facing conflict_record as the primary handling path
- AND the row MUST remain marked error/quarantined

#### Scenario: Stale sync-conflict SDD non-authoritative for this change

- GIVEN archived/stale sync-conflict requirements that expand broad guards
- WHEN implementing fix-sync-financial-ledger
- THEN those broad-guard expansions MUST NOT be treated as required for finanzas quarantine
- AND this delta’s LWW+quarantine rules MUST take precedence for the ledger scope
