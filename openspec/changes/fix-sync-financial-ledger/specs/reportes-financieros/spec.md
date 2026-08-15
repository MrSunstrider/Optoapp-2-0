# Delta for reportes-financieros

## MODIFIED Requirements

### Requirement: Total Cobrado Computation

The system SHALL compute `totalCobrado` as the sum of PagoEffect over every in-range pago returned by the period’s DAO range for the active optica, and MUST NOT depend on `allDispensaciones` as a Flow input. The in-memory `dentroDelPeriodo` filter MUST still exclude any pago whose `fecha` falls outside the range (off-by-one safety net). Legacy Anulación MUST contribute 0; Reverso/Reembolso MUST reduce the total.
(Previously: Summed raw `monto` while excluding `tipo == Anulación`, which diverges once Reverso/Reembolso and non-negative magnitudes are the ledger law.)

#### Scenario: Independent of dispensaciones

- GIVEN `totalCobrado` is being collected
- WHEN `allDispensaciones` emits a new value
- THEN `totalCobrado` MUST NOT recompute as a consequence

#### Scenario: Reverso and Reembolso reduce totalCobrado

- GIVEN period pagos: Abono 200, Reverso 50, Reembolso 25, Anulación 100
- WHEN `totalCobrado` computes
- THEN the value MUST be `125`
- AND Anulación MUST contribute `0`

#### Scenario: Negative control — excluding only Anulación is insufficient

- GIVEN the same period pagos
- WHEN a legacy filter `tipo != Anulación` then `sum(monto)` is evaluated
- THEN that legacy value MUST NOT be accepted as `totalCobrado` if it differs from PagoEffect net

## ADDED Requirements

### Requirement: Cobros Classification Uses PagoEffect Magnitudes

Period cobros classification MUST apply PagoEffect when accumulating included pagos’ contributions. Classification membership rules (venta del período vs cobro anterior) remain as specified; only the cash contribution formula changes to PagoEffect.

#### Scenario: Out-of-range dispensación cobro with Reverso

- GIVEN a pago Reverso linked to an out-of-range dispensación and included in cobrosPeriodo membership
- WHEN cobrosPeriodo sums
- THEN the contribution MUST be the negative effect of that Reverso’s positive monto

#### Scenario: Optica isolation on reportes

- GIVEN identical pago fixtures on optics A and B
- WHEN reportes run for A
- THEN only A’s PagoEffect net MUST be shown
