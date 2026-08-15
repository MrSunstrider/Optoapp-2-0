# Cierre de Caja Specification

## Purpose

Cierre de caja screen: daily sales summary, payment classification by PagoEffect, servicios extra breakdown, cobros vs ventas separation, in-screen search, and totals by payment method.

## Requirements

### Requirement: Hero COBRADO Total via PagoEffect

`CierreCajaScreen` hero MUST show primary total as sum of `PagoEffect.signedAmount` for all pagos where `pago.fecha = selectedDate`. Label MUST be dynamic: COBRADO HOY (today), COBRADO AYER (yesterday), TOTAL COBRADO (other). Sub-lines MUST distinguish Ventas registradas (`totalGeneral`), Cobros de ventas del día (`ventasHoy`), Cobros atrasados (if > 0), Pendiente órdenes del día (`saldoPendiente` entity-based).

#### Scenario: Hero uses PagoEffect not raw monto

- GIVEN Abono 100 + Anulación 50 + Reverso 40 Efectivo same day
- WHEN hero renders
- THEN primary total MUST be 60 (PagoEffect: Abono +100, Anulación 0, Reverso -40)
- AND MUST NOT use sum of raw montos nor exclude-only-Anulación filter

#### Scenario: getCobradoHoy equals sum of getTotalesPorMetodo

- GIVEN any mix of payment types and methods
- WHEN `getCobradoHoy()` and `getTotalesPorMetodo()` are called
- THEN `getCobradoHoy()` MUST equal sum of all `getTotalesPorMetodo()` values

### Requirement: Cobros Recibidos List

Section **Cobros recibidos (N)** MUST list pagos by `pago.fecha = selectedDate` using `Column.forEach` (no nested LazyColumn). Rows MUST use enriched `TransactionItem` fed from `PagoDisplayItem` (OT label, tipo, chip cobro atrasado, tap navigate).

#### Scenario: Pago listed by payment date not order date

- GIVEN pago.fecha = today linked to yesterday dispensación
- WHEN screen renders
- THEN pago MUST appear under Cobros recibidos
- AND MUST show "Cobro atrasado" chip

### Requirement: Ventas Registradas Cards

Section **Ventas registradas (N)** MUST list entity-fecha dispensaciones/servicios separately from cobros. Cards MUST show OT, product detail, estado chip, total/pagado/saldo from entity fields. `HorizontalDivider` between sections when day has data.

### Requirement: Saldo Pendiente Entity-Based

`saldoPendiente` MUST equal sum of `(montoTotal - montoPagado)` dispensaciones + `(montoTotal - aCuenta)` servicios for non-anulado/non-reclamada entities registered selectedDate. MUST NOT use `totalGeneral - ventasHoy`.

#### Scenario: Entity-based saldoPendiente

- GIVEN disp montoTotal=300 montoPagado=100, serv montoTotal=100 aCuenta=75
- WHEN saldoPendiente computed
- THEN saldoPendiente MUST be 225.0

### Requirement: Totals by Method via PagoEffect

`getTotalesPorMetodo()` MUST group by normalized `metodoPago` and sum `PagoEffect.signedAmount(tipo, monto)`. "Sin especificar" normalizes to empty string key.

#### Scenario: Method cards match hero decomposition

- GIVEN Abono 100 Efectivo + Reverso 40 Efectivo + Anulación 50 Efectivo + Reembolso 10 Tarjeta
- WHEN hero and method cards render
- THEN Efectivo = 60 (Abono +100, Reverso -40, Anulación 0), Tarjeta = -10
- AND sum of method card values MUST equal hero primary total

### Requirement: In-Screen Search

Chip **Buscar** MUST toggle filter field. Filter MUST apply to cobros and ventas lists only (not hero nor method cards). Search MUST match OT, paciente nombre, descripción, lente, método, estado (case-insensitive contains).

#### Scenario: Search filters lists preserves hero

- GIVEN active search "García"
- WHEN lists render
- THEN only matching cobros/ventas show
- AND hero totals MUST remain full-day aggregates

### Requirement: TransactionItem Label Classification

`TransactionItem` SHALL classify each pago using `PagoDisplayItem` enrichment: OT-based label for dispensaciones, description for servicios, "Pago" for orphans. Display MUST show tipo, método, cobro atrasado chip, and signed-amount color.

#### Scenario: Dispensación pago renders OT label

- GIVEN a pago linked to dispensación with OT "2026-0042"
- WHEN `TransactionItem` renders
- THEN the label MUST be "OT 2026-0042"

### Requirement: Display Helpers Testable

Pure functions in `CierreCajaVentaDisplay.kt` MUST resolve card titles/subtitles, hero label, and search haystacks/filter — covered by unit tests without Robolectric.

### Requirement: Ventas Registradas Entity Filtering

`dispensacionesHoy` MUST exclude estado "Anulado" and "Reclamada". `serviciosExtraHoy` MUST exclude estado "Anulado". Exclusion applies to entity-level totals (`totalGeneral`, `saldoPendiente`) and ventas registradas display list.

#### Scenario: Anulado dispensacion excluded from totals

- GIVEN 2 dispensaciones: d1 Anulado montoTotal=300, d2 active montoTotal=200
- WHEN CierreCajaViewModel emits
- THEN totalGeneral MUST be 200 AND dispensacionesHoy MUST contain only d2
