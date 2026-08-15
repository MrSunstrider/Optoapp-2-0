# Delta for cierre-caja

## ADDED Requirements

### Requirement: Cierre Aggregates Use PagoEffect

`CierreCajaViewModel` cash totals by method and day MUST sum `PagoEffect(tipo) *` magnitude semantics (equivalently apply the shared PagoEffect matrix) over in-range pagos for the active `opticaId`. Legacy `Anulación` MUST contribute `0`. Reverso/Reembolso MUST reduce totals. Filtering solely by excluding `tipo != "Anulación"` while summing raw signed montos MUST NOT remain the cash definition.

#### Scenario: Mixed tipos net correctly in cierre

- GIVEN today for optica A: Abono 100 Efectivo, Reverso 40 Efectivo, Reembolso 10 Tarjeta, Anulación 50
- WHEN cierre totals by method compute
- THEN Efectivo MUST be `60` and Tarjeta MUST be `−10` (or equivalent signed presentation of effects)
- AND Anulación MUST not change cash

#### Scenario: Negative control — raw monto sum without effect diverges

- GIVEN the same fixture as above
- WHEN a naive `sum(monto)` ignoring tipo is compared to PagoEffect net
- THEN the values MUST differ unless the fixture has only +effect tipos
- AND the UI MUST show the PagoEffect net

#### Scenario: Multi-tenant cierre isolation

- GIVEN optica B has large Abonos today
- WHEN cierre runs for optica A
- THEN optica B pagos MUST NOT appear in optica A totals
