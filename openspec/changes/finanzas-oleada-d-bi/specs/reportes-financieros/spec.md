# Delta for reportes-financieros

## MODIFIED Requirements

### Requirement: Total Cobrado Computation

The system SHALL compute `totalCobrado` as the sum of `PagoEffect.signedAmount(tipo, monto)` for every pago returned by the period's DAO range (Abono/Pago completo +, Reembolso/Reverso −, Anulación/unknown 0), and MUST NOT depend on `allDispensaciones` as a Flow input. The in-memory `dentroDelPeriodo` filter MUST still exclude any pago whose `fecha` falls outside the range (off-by-one safety net). MUST NOT use raw `sum(monto)` or exclude-only-Anulación.

(Previously: `totalCobrado` = sum of raw `monto` for every in-range pago.)

#### Scenario: Independent of dispensaciones

- GIVEN `totalCobrado` is being collected
- WHEN `allDispensaciones` emits a new value
- THEN `totalCobrado` MUST NOT recompute as a consequence

#### Scenario: PagoEffect signs affect totalCobrado

- GIVEN period has Abono 100 + Reverso 40 + Anulación 50
- WHEN `totalCobrado` is computed
- THEN value MUST be 60
- AND MUST NOT be 190 or 150

### Requirement: Cobros del Período Classification

The system SHALL classify each in-range pago by consulting BOTH its `dispensacionId` and its `servicioExtraId`. If the pago's linked entity (dispensación OR servicio extra) is in-range, the pago is a "venta del período" (excluded from `cobrosPeriodo`); otherwise a "cobro de períodos anteriores" (included). When both IDs are present, the dispensación's date is consulted first; if `dispensacionId` is null or unresolved, the servicio extra's `fecha` is consulted. Pagos with neither ID count as "cobro de períodos anteriores". Amounts contributed to `cobrosPeriodo` MUST use `PagoEffect.signedAmount(tipo, monto)`.

(Previously: contributed amount was raw `monto`.)

#### Scenario: Pago linked to in-range dispensación

- GIVEN a pago whose dispensación date is inside the period
- THEN this pago's signed amount MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range dispensación

- GIVEN a pago whose dispensación date is outside the period
- THEN this pago's signed amount MUST contribute to `cobrosPeriodo`

#### Scenario: Pago linked to in-range servicio extra

- GIVEN a pago with `servicioExtraId` set and `dispensacionId == null` whose servicio `fecha` is inside the period
- THEN this pago's signed amount MUST NOT contribute to `cobrosPeriodo`

#### Scenario: Pago linked to out-of-range servicio extra

- GIVEN a pago with `servicioExtraId` set whose servicio `fecha` is outside the period
- THEN this pago's signed amount MUST contribute to `cobrosPeriodo`

#### Scenario: Pago with no dispensación and no servicio extra (orphan)

- GIVEN a pago where `dispensacionId == null` AND `servicioExtraId == null`
- THEN this pago's signed amount MUST contribute to `cobrosPeriodo`

#### Scenario: Pago with both IDs falls back to dispensación date

- GIVEN a pago with both `dispensacionId` and `servicioExtraId` set
- WHEN the dispensación date is in-range but the servicio extra date is out-of-range
- THEN the pago's signed amount MUST NOT contribute to `cobrosPeriodo` (dispensación date wins)

#### Scenario: Reverso in cobrosPeriodo reduces total

- GIVEN an orphan Reverso 40 classified into `cobrosPeriodo`
- WHEN totals emit
- THEN `cobrosPeriodo` contribution MUST be −40
