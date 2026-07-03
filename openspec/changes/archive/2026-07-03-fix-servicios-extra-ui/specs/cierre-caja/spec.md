# Cierre de Caja Specification

## Purpose

Cierre de caja screen: daily sales summary, payment classification, servicios extra breakdown, and totals by payment method.

## Requirements

### Requirement: TransactionItem Label Classification

`TransactionItem` SHALL classify each pago using a 3-way label: "Dispensación" when `dispensacionId != null`, "Servicio Extra" when `servicioExtraId != null` (and `dispensacionId == null`), and "Pago" for orphan pagos with neither ID.

#### Scenario: Orphan pago renders "Pago"

- GIVEN a pago with `dispensacionId == null` AND `servicioExtraId == null`
- WHEN `TransactionItem` renders
- THEN the label MUST be "Pago"

#### Scenario: Servicio extra pago renders "Servicio Extra"

- GIVEN a pago with `servicioExtraId != null` AND `dispensacionId == null`
- WHEN `TransactionItem` renders
- THEN the label MUST be "Servicio Extra"

#### Scenario: Dispensación pago renders "Dispensación"

- GIVEN a pago with `dispensacionId != null`
- WHEN `TransactionItem` renders
- THEN the label MUST be "Dispensación"

### Requirement: Servicios Extra Section in Cierre de Caja

The system SHALL render a servicios extra section in `CierreCajaScreen` showing today's servicios extra count, individual descriptions with amounts, and the servicios extra total. The section MUST update whenever `serviciosExtraHoy` changes.

#### Scenario: Today has servicios extra

- GIVEN today has 2 servicios extra (montoTotal sum = 150.0)
- WHEN `CierreCajaScreen` renders
- THEN a servicios extra section MUST be visible
- AND it MUST show count = 2
- AND `totalServiciosExtra` MUST be 150.0

#### Scenario: No servicios extra today

- GIVEN today has 0 servicios extra
- WHEN `CierreCajaScreen` renders
- THEN the servicios extra section MUST show count = 0 and total = 0.0

### Requirement: Servicios Extra in Cierre Totals

`CierreCajaViewModel` SHALL compute `totalGeneral = totalVentasHoy + totalServiciosExtra` where `totalVentasHoy` is the sum of dispensación montoTotal and `totalServiciosExtra` is the sum of servicio extra montoTotal for today. `saldoPendiente` MUST equal `totalGeneral - ventasHoy`.

#### Scenario: Mixed dispensaciones and servicios extra

- GIVEN today: dispensaciones montoTotal sum = 300, servicios extra montoTotal sum = 150, pagos today = 200
- WHEN `CierreCajaViewModel` emits
- THEN `totalGeneral` MUST be 450.0 AND `saldoPendiente` MUST be 250.0

#### Scenario: Only dispensaciones

- GIVEN today: dispensaciones montoTotal sum = 300, no servicios extra
- WHEN `CierreCajaViewModel` emits
- THEN `totalGeneral` MUST be 300.0 AND `saldoPendiente` MUST equal `300 - ventasHoy`

### Requirement: Totals by Method Normalization

`CierreCajaViewModel.getTotalesPorMetodo()` SHALL normalize `metodoPago` values using `remotoPagoMetodoToLocal()` (or equivalent) BEFORE grouping. This ensures consistent totals regardless of raw input variations.

#### Scenario: Raw metodoPago values are normalized

- GIVEN pagos with raw metodoPago values: "Efectivo", "efectivo", "EFECTIVO"
- WHEN `getTotalesPorMetodo()` is called
- THEN all three MUST be grouped under the same normalized key
- AND the total for that key MUST equal the sum of all three pagos

#### Scenario: Multiple payment methods

- GIVEN pagos: 2 with normalized "Efectivo" (sum 200) and 1 with normalized "Tarjeta" (100)
- WHEN `getTotalesPorMetodo()` is called
- THEN the result MUST contain exactly 2 entries: "Efectivo" → 200.0, "Tarjeta" → 100.0
