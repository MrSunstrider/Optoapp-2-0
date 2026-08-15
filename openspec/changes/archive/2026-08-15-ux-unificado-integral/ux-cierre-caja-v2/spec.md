# Delta: ux-cierre-caja-v2

> Supersedes openspec archive `2026-08-14-ux-cierre-caja` where it conflicts with ledger.
> Merges UX from PR #51 with ledger requirement `Cierre Aggregates Use PagoEffect`.

## MODIFIED Requirements

### Requirement: Hero COBRADO vs Ventas Registradas

`CierreCajaScreen` hero MUST show primary total as **sum of PagoEffect** for pagos where `pago.fecha = selectedDate`. Label MUST be dynamic: COBRADO HOY (today), COBRADO AYER (yesterday), TOTAL COBRADO (other). Sub-lines MUST distinguish Ventas registradas (`totalGeneral`), Cobros de ventas del día (`ventasHoy`), Cobros atrasados (if > 0), Pendiente órdenes del día (`saldoPendiente` entity-based).

#### Scenario: Hero uses PagoEffect not raw monto

- GIVEN Abono 100 + Anulación 50 + Reverso 40 Efectivo same day
- WHEN hero renders
- THEN primary total MUST be 60 (PagoEffect net)
- AND MUST NOT use sum of raw montos nor exclude-only-Anulación filter

### Requirement: Cobros Recibidos List

Section **Cobros recibidos (N)** MUST list pagos by `pago.fecha = selectedDate` using `Column.forEach` (no nested LazyColumn). Rows MUST use `TransactionItem` fed from `PagoDisplayItem` (OT label, tipo, chip cobro atrasado, tap navigate).

#### Scenario: Pago listed by payment date not order date

- GIVEN pago.fecha = today linked to yesterday dispensación
- WHEN screen renders
- THEN pago MUST appear under Cobros recibidos
- AND MUST NOT appear under Ventas registradas

### Requirement: Ventas Registradas Cards

Section **Ventas registradas (N)** MUST list entity-fecha dispensaciones/servicios separately from cobros. Cards MUST show OT, product detail, estado chip, total/pagado/saldo from entity fields. `HorizontalDivider` between sections when day has data.

### Requirement: In-Screen Search

Chip **Buscar** MUST toggle filter field. Filter MUST apply to cobros and ventas lists only (not hero nor method cards). Search MUST match OT, paciente nombre, descripción, lente, método, estado (case-insensitive contains).

#### Scenario: Search filters lists preserves hero

- GIVEN active search "García"
- WHEN lists render
- THEN only matching cobros/ventas show
- AND hero totals MUST remain full-day aggregates

### Requirement: Saldo Pendiente Entity-Based

`saldoPendiente` MUST equal sum of `(montoTotal - montoPagado)` dispensaciones + `(montoTotal - aCuenta)` servicios for non-anulado/non-reclamada entities registered selectedDate. MUST NOT use `totalGeneral - ventasHoy`.

### Requirement: Totals by Method via PagoEffect

`getTotalesPorMetodo()` MUST group by normalized `metodoPago` and sum `PagoEffect.signedAmount(tipo, monto)` — same semantics as hero primary total decomposition.

#### Scenario: Method cards match hero decomposition

- GIVEN mixed tipos same day
- WHEN hero and method cards render
- THEN sum of method card values MUST equal hero primary total (within rounding)

## ADDED Requirements

### Requirement: Display Helpers Testable

Pure functions in `CierreCajaVentaDisplay.kt` MUST resolve card titles/subtitles, hero label, and search haystacks/filter — covered by unit tests without Robolectric.
