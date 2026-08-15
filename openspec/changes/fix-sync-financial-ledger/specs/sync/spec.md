# Delta for sync

## ADDED Requirements

### Requirement: Room Legacy Negative Anulación Normalization

On Room migration for this change, existing local `Anulación` rows with `monto < 0` MUST be normalized to `abs(monto)` while keeping `tipo='Anulación'`. Historical cash totals MUST NOT be reinterpreted (effect of Anulación remains 0). Migration MUST be forward-only and idempotent on re-run after upgrade.

#### Scenario: Negative Anulación becomes absolute magnitude

- GIVEN a pre-migration Room pago `tipo=Anulación, monto=-100`
- WHEN the migration runs
- THEN the row MUST become `tipo=Anulación, monto=100`
- AND PagoEffect for that row MUST remain `0`

#### Scenario: Non-Anulación negatives are not silently rewritten as cash history

- GIVEN a corrupted non-Anulación row with `monto < 0` if any exist
- WHEN migration/quarantine runs
- THEN the system MUST NOT treat it as a historical cash credit/debit rewrite
- AND the row MUST be quarantined or rejected per invalid-row rules

#### Scenario: Cash unchanged after normalization

- GIVEN optica ledger net computed via PagoEffect before migration
- WHEN only Anulación magnitudes are abs-normalized
- THEN post-migration PagoEffect net MUST equal the pre-migration PagoEffect net

### Requirement: Invalid-Row Quarantine Under LWW

Finanzas upload MUST quarantine rows that violate domain/CHECK rules (invalid estado, negative monto, illegal tipo, duplicate reverse link). Quarantined rows MUST NOT be uploaded and MUST NOT be marked remotely successful. Valid sibling rows in the same batch MUST still upload (partial success). Last-write-wins (LWW) timestamp rules for valid rows MUST remain; quarantine MUST NOT become a broad conflict-guard substitute.

#### Scenario: 79 valid + 1 poison uploads 79

- GIVEN 79 valid finanzas entities and 1 poison row for the same optica
- WHEN upload runs
- THEN exactly 79 MUST upload successfully
- AND the poison row MUST remain local-error/quarantined
- AND sync status MUST NOT claim full remote OK for the poison row

#### Scenario: Negative control — skip+success forbidden

- GIVEN a poison row is detected before upsert
- WHEN upload completes
- THEN the system MUST NOT skip the row and report overall success as if it synced
- AND `markError` (or equivalent) MUST record the quarantine reason

#### Scenario: LWW still applies to valid rows

- GIVEN two devices update the same valid pago with different `updatedAt`
- WHEN sync runs without quarantine on either row
- THEN LWW MUST select the newer timestamp
- AND no broad `filterConflicts` restoration MUST be introduced for finanzas solely due to quarantine

#### Scenario: Concurrent uploads quarantine independently

- GIVEN two concurrent upload attempts include the same poison id
- WHEN both complete
- THEN the poison id MUST remain unsynced
- AND valid rows MUST not be rolled back solely because the poison failed

### Requirement: Deletion Non-Resurrection With Quarantine

Pending deletion tombstones for finanzas entities MUST continue to prevent download resurrection. Quarantine of a related pago MUST NOT resurrect a deleted parent sale.

#### Scenario: Deleted sale stays deleted despite quarantine sibling

- GIVEN a deleted dispensacion/servicio with tombstone and a quarantined related pago
- WHEN download runs
- THEN the deleted sale MUST NOT be re-inserted from remote
- AND the quarantined pago MUST remain quarantined locally

### Requirement: Pacientes HTTP Evidence Capture Only

Until exact HTTP status/body evidence exists for pacientes sync failures, this change MUST only capture durable diagnostic evidence for pacientes errors. Targeted pacientes remediation is OUT OF SCOPE.

#### Scenario: Pacientes failure captures evidence without fix

- GIVEN a pacientes sync HTTP failure during/after finanzas work
- WHEN diagnostics run
- THEN status code and sanitized body excerpt MUST be captured if available
- AND no pacientes-specific remediation path MUST ship in this change

### Requirement: Device and Rollout Verification

Rollout MUST deploy DB CHECKs/effect SQL before requiring new client writers. Device verification MUST re-check known poison cases (e.g., CLK-LX3) and confirm Anulado/Reclamada/Reverso/Reembolso sync without 23514 after DB-first deploy.

#### Scenario: DB-first mixed-client safety

- GIVEN expanded remote CHECKs are deployed and an older client still online
- WHEN the older client uploads Anulado/Reclamada
- THEN remote MUST accept those estados
- AND `pagos_monto_chk` MUST still reject negatives

#### Scenario: CLK-LX3 style poison cleared after fix

- GIVEN a previously failing optica/device with servicios poison estado
- WHEN DB+client fix is rolled out and sync re-runs
- THEN remote OK MUST succeed for remediated rows
- AND quarantine MUST remain only for still-invalid rows

#### Scenario: Rollback-safe client revert

- GIVEN DB expanded CHECKs remain and app writers are reverted
- WHEN sync runs on the reverted build
- THEN negative montos MUST still be rejected remotely
- AND no new Anulación writer requirement MUST reappear as a success path
