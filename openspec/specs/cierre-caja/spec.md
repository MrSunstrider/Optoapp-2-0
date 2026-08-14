# Cierre de Caja Specification

## Purpose

Cierre de caja screen: daily cash summary distinguishing money collected today (cobros) from orders registered today (ventas), payment classification by method, enriched transaction list, and entity-based pending balance.

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

### Requirement: Hero COBRADO HOY vs Ventas Registradas

`CierreCajaScreen` hero MUST show **COBRADO HOY** as the sum of `pago.monto` for pagos where `pago.fecha` equals the selected date, excluding tipo Anulación. Sub-lines MUST distinguish **Ventas registradas** (`totalGeneral` from orders registered that day), **Cobros de ventas del día** (`ventasHoy`), **Cobros atrasados** (`cobrosAtrasados` when > 0), and **Pendiente (órdenes del día)** (`saldoPendiente` when > 0).

#### Scenario: Hero cobrado excludes anulaciones

- GIVEN pagos today: Efectivo S/100 and Anulación S/-100
- WHEN COBRADO HOY renders
- THEN the hero MUST show S/100.00

### Requirement: Cobros Recibidos List

`CierreCajaScreen` MUST list pagos by `pago.fecha` in section **Cobros recibidos (N)** using `Column.forEach` (no nested LazyColumn). Each row MUST use `TransactionItem` with metadata from `PagoDisplayItem`.

#### Scenario: Abono on old order appears in cobros

- GIVEN a pago today linked to a dispensación registered on a prior day
- WHEN the screen renders for today
- THEN the pago MUST appear under Cobros recibidos
- AND MUST NOT appear under Ventas registradas

#### Scenario: Empty cobros section

- GIVEN no pagos on the selected date
- WHEN the screen renders with ventas that day
- THEN the Cobros recibidos section MUST show count 0 and message "Sin cobros registrados"

### Requirement: PagoDisplayItem

`CierreCajaViewModel` SHALL expose `pagosDisplay: List<PagoDisplayItem>` resolving label (OT or descripción), `tipoEntidad`, `esCobroAtrasado` when linked entity fecha < selected date, and navigation IDs.

#### Scenario: Cobro atrasado metadata

- GIVEN pago today for dispensación with fecha yesterday and ot "2026-0040"
- WHEN `pagosDisplay` is built
- THEN label MUST be "OT 2026-0040" AND `esCobroAtrasado` MUST be true

### Requirement: Ventas Registradas Section

`CierreCajaScreen` MUST render **Ventas registradas (N)** separately from Cobros recibidos, with a `HorizontalDivider` between sections when the day has any cobros or ventas. Cards MUST show OT, product detail (tipo lente/material or descripción), estado chip (Pendiente/Entregado), and total/pagado/saldo from entity fields (`montoPagado` / `aCuenta`).

#### Scenario: Empty ventas section

- GIVEN pagos today and no orders registered today
- WHEN the screen renders
- THEN Ventas registradas (0) MUST show "Sin ventas registradas"

#### Scenario: Dispensación card shows lente detail

- GIVEN dispensación with tipoLente "Monofocal" and materialLente "CR-39"
- WHEN the venta card renders
- THEN subtitle MUST include "Monofocal · CR-39"

### Requirement: Servicios Extra in Cierre Totals

`CierreCajaViewModel` SHALL compute `totalGeneral = totalVentasHoy + totalServiciosExtra` where totals are sums of entity `montoTotal` for orders registered on the selected date. `saldoPendiente` MUST equal `sum(montoTotal - montoPagado)` for dispensaciones plus `sum(montoTotal - aCuenta)` for servicios extra on that date, using entity cumulative payment fields regardless of payment date.

#### Scenario: Mixed dispensaciones and servicios extra

- GIVEN today: dispensaciones montoTotal sum = 300, servicios extra montoTotal sum = 150, montoPagado/aCuenta cover 200 total
- WHEN `CierreCajaViewModel` emits
- THEN `totalGeneral` MUST be 450.0 AND `saldoPendiente` MUST be 250.0

#### Scenario: Full historical payment yields zero pendiente

- GIVEN dispensación S/300 with montoPagado=300
- WHEN cierre emits for today
- THEN `saldoPendiente` MUST be S/0.00 for that dispensación

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
