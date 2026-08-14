# Delta Spec: UX Cierre de Caja — Cobros vs Ventas

## Purpose

Supplements and replaces outdated requirements in `openspec/specs/cierre-caja/spec.md`. Unchanged requirements from the base spec remain in effect unless explicitly replaced below.

## Modified Requirements

### REQ-UX-CIERRE-001: Saldo Pendiente desde Entidad

**Replaces**: base spec "Servicios Extra in Cierre Totals" saldoPendiente formula.

`CierreCajaViewModel` SHALL compute `saldoPendiente` as `sum(montoTotal - montoPagado)` for each dispensación del día plus `sum(montoTotal - aCuenta)` for each servicio extra del día, using entity-tracked cumulative payment fields regardless of payment date. `totalGeneral` remains `totalDispensacionesHoy + totalServiciosExtra`.

#### Scenario: Historical payments zero out pendiente

- GIVEN dispensación S/300 with montoPagado=300 (partial payments on prior days)
- WHEN cierre emits for today with that dispensación registered today
- THEN saldoPendiente MUST be S/0.00

#### Scenario: Partial entity payment

- GIVEN dispensación S/300 with montoPagado=100
- WHEN cierre emits
- THEN saldoPendiente MUST include S/200.00 for that dispensación

### REQ-UX-CIERRE-002: Hero COBRADO HOY vs Ventas Registradas

`CierreCajaScreen` hero MUST label the primary total **COBRADO HOY** as `sum(pago.monto)` for pagos where `pago.fecha = selectedDate`, excluding tipo Anulación (same rule as OperacionHoy). Sub-line **Ventas registradas** MUST show `totalGeneral` (sum of entity montoTotal for orders registered on selectedDate). Sub-line **Cobros de ventas del día** MUST show `ventasHoy`. Sub-line **Cobros atrasados** MUST show `cobrosAtrasados` when > 0.

#### Scenario: Cobro atrasado increases hero but not ventas registradas

- GIVEN order from yesterday S/150 and pago S/150 today linked to that order
- WHEN screen renders for today
- THEN COBRADO HOY MUST be S/150 AND Ventas registradas sub-line MUST NOT include that order amount unless the order was also registered today

## New Requirements

### REQ-UX-CIERRE-003: Cobros Recibidos por pago.fecha

`CierreCajaScreen` MUST render section **Cobros recibidos (N)** listing every pago where `pago.fecha = selectedDate`, using `Column.forEach` (no nested LazyColumn). Each row MUST use `TransactionItem` fed from `PagoDisplayItem`.

#### Scenario: Payment listed when order is from another day

- GIVEN pago today linked to dispensación registered yesterday
- WHEN screen renders for today
- THEN the pago MUST appear under Cobros recibidos
- AND MUST NOT appear under Ventas registradas

#### Scenario: Empty cobros with ventas

- GIVEN no pagos on selectedDate and 1 dispensación registered that day
- WHEN screen renders
- THEN section MUST show title Cobros recibidos (0) and message "Sin cobros registrados"

### REQ-UX-CIERRE-004: PagoDisplayItem

`CierreCajaViewModel` SHALL expose `pagosDisplay: List<PagoDisplayItem>` where each item contains:

| Field | Rule |
|-------|------|
| `label` | OT for dispensación; servicio descripción for servicio extra; "Pago" for orphan |
| `tipoEntidad` | "Dispensación", "Servicio Extra", or "Pago" |
| `esCobroAtrasado` | true when linked entity fecha < selectedDate |
| Navigation IDs | dispensacionId/servicioExtraId/pacienteId when resolvable |

#### Scenario: Cobro atrasado flagged

- GIVEN pago today for dispensación with fecha yesterday and ot "2026-0040"
- WHEN pagosDisplay builds
- THEN label MUST be "OT 2026-0040" AND esCobroAtrasado MUST be true

### REQ-UX-CIERRE-005: Ventas Registradas Cards

Section **Ventas registradas (N)** MUST list dispensaciones and servicios extra registered on selectedDate (entity fecha), separate from Cobros recibidos. Cards MUST show:

- Dispensación: OT title, subtitle from tipo/material lente or descripcion montura, estadoEntrega chip (Pendiente/Entregado), total/pagado/saldo from entity fields
- Servicio extra: OT line when present, descripcion, estado chip, total/pagado/saldo from entity fields

Chip colors MUST follow ServiciosExtraScreen pattern (positiveGreen Entregado, alertRed/warningAmber Pendiente).

#### Scenario: Empty ventas with cobros only

- GIVEN pagos today and no orders registered today
- WHEN screen renders
- THEN HorizontalDivider MUST separate sections
- AND Ventas registradas (0) MUST show "Sin ventas registradas"

### REQ-UX-CIERRE-006: Venta Display Helpers

Pure functions in `CierreCajaVentaDisplay.kt` MUST resolve dispensación title/subtitle and servicio OT line for card rendering, covered by unit tests without Robolectric.

#### Scenario: Dispensación subtitle from lente fields

- GIVEN tipoLente "Monofocal" and materialLente "CR-39"
- WHEN dispensacionVentaSubtitle runs
- THEN result MUST be "Monofocal · CR-39"
