# pago-effect-ledger Specification

## Purpose

Typed cash-effect ledger for `pagos`: signed effect derives only from `tipo`; stored `monto` is a non-negative magnitude. Defines reverse/refund linkage, legacy Anulación audit semantics, and Kotlin↔SQL convergence. Strict TDD: failing tests MUST exist before production changes.

## Requirements

### Requirement: PagoEffect Matrix With Non-Negative Magnitudes

The system MUST compute cash effect solely via `PagoEffect(tipo)` applied to `monto` where `monto ≥ 0`. Sign MUST NOT be stored in `monto`.

| tipo | effect |
|------|--------|
| `Abono` | `+monto` |
| `Pago completo` | `+monto` |
| `Reembolso` | `−monto` |
| `Reverso` | `−monto` |
| `Anulación` | `0` (legacy audit only) |

#### Scenario: Abono and Pago completo increase cash

- GIVEN pagos with `tipo=Abono, monto=100` and `tipo=Pago completo, monto=50` for the same `optica_id`
- WHEN PagoEffect is evaluated
- THEN effects MUST be `+100` and `+50`

#### Scenario: Reembolso and Reverso decrease cash with positive magnitudes

- GIVEN `tipo=Reembolso, monto=40` and `tipo=Reverso, monto=60`
- WHEN PagoEffect is evaluated
- THEN effects MUST be `−40` and `−60`
- AND stored `monto` values MUST remain `≥ 0`

#### Scenario: Negative control — negative monto rejected

- GIVEN an insert or update with `monto < 0`
- WHEN the write is attempted
- THEN the operation MUST fail (`pagos_monto_chk` or equivalent local validation)
- AND no row MUST be persisted

#### Scenario: Multi-tenant isolation

- GIVEN optica A has Abono 100 and optica B has Reverso 100
- WHEN aggregates run for optica A
- THEN optica B rows MUST NOT affect optica A totals

### Requirement: Linked Idempotent Reverso and Positive Reembolso

Cancel flows MUST keep original credit pagos. A full cancel MUST create at most one linked `Reverso` per original credit pago (`reversa_pago_id` uniqueness). New `Reembolso` writes MUST use `monto > 0`. Concurrent cancel attempts MUST be idempotent (second attempt creates no additional Reverso).

#### Scenario: First cancel creates linked Reverso

- GIVEN an original `Abono` id=`P1` with `monto=80` and no existing Reverso
- WHEN the sale is cancelled
- THEN exactly one `Reverso` with `monto=80` and `reversa_pago_id=P1` MUST exist
- AND original `P1` MUST remain

#### Scenario: Concurrent cancel is idempotent

- GIVEN original `P1` already has a linked Reverso
- WHEN a second cancel (or concurrent cancel) runs
- THEN no additional Reverso for `P1` MUST be created
- AND uniqueness on `reversa_pago_id` MUST hold

#### Scenario: Reembolso requires positive magnitude

- GIVEN a refund request with `monto=0` or `monto<0`
- WHEN the write is attempted
- THEN it MUST be rejected
- AND no Reembolso row MUST be created

#### Scenario: Rollback-safe cancel abort

- GIVEN cancel starts but remote/local commit fails mid-flight
- WHEN the operation rolls back
- THEN no orphan Reverso MUST remain committed without its paired cancel state
- AND a retry MUST still obey ≤1 Reverso per original

### Requirement: Legacy Anulación Effect Zero and No New Writes

Existing `Anulación` rows MUST contribute effect `0`. Writers MUST NOT create new `Anulación` rows. Cancel MUST use Reverso (and refunds use Reembolso), not Anulación.

#### Scenario: Legacy Anulación does not move cash

- GIVEN a legacy `Anulación` with `monto=100` (after magnitude normalization)
- WHEN PagoEffect is evaluated
- THEN effect MUST be `0`

#### Scenario: Negative control — new Anulación blocked

- GIVEN a cancel or adjust path that previously wrote `tipo=Anulación`
- WHEN that path runs after this change
- THEN it MUST NOT insert a new `Anulación`
- AND it MUST use `Reverso` (or reject) instead

### Requirement: Kotlin and SQL Aggregate Convergence

Kotlin readers and SQL functions/RPCs that sum pagos MUST apply the same PagoEffect matrix. Shared golden fixtures MUST prove identical nets for the same input set.

#### Scenario: Shared fixture nets match

- GIVEN the fixture set {Abono 100, Reverso 100, Reembolso 25, Anulación 50}
- WHEN Kotlin and SQL aggregates compute net effect for the same `optica_id` and date window
- THEN both MUST return net `−25`
- AND Anulación MUST contribute `0` on both sides

#### Scenario: Strict TDD gate

- GIVEN a PagoEffect matrix or convergence test is missing or red
- WHEN apply attempts production code or migration for that behavior
- THEN the change MUST NOT proceed until the test exists and fails for the right reason, then passes
