# Delta Spec: Fix Cierre de Caja — Payment Balance and Data Correctness

## Purpose

This delta spec corrects the `saldoPendiente` formula, fixes per-item payment display, enforces decimal precision, adds date-filtered queries, classifies future-dated payments, and enforces multi-tenant isolation in tests.

All requirements below supplement or replace the corresponding requirements in `openspec/specs/cierre-caja/spec.md`. Unchanged requirements from that spec REMAIN in effect.

## Modified Requirements

### REQ-CIERRE-001: Saldo Pendiente Correcto

**Replaces**: `openspec/specs/cierre-caja/spec.md` requirement "Servicios Extra in Cierre Totals" (lines 49–63).

`CierreCajaViewModel` SHALL compute `saldoPendiente` as `sum(montoTotal - montoPagado)` for every dispensacion plus `sum(montoTotal - aCuenta)` for every servicio extra, using entity-tracked fields that accumulate ALL historical payments regardless of payment date. `totalGeneral = totalVentasHoy + totalServiciosExtra` remains unchanged.

(Previously: `saldoPendiente = totalGeneral - ventasHoy`, counting only today's payments and ignoring historical payments.)

#### Scenario: Full historical payment yields zero pendiente

- GIVEN a dispensacion S/300, montoPagado=300 (S/200 paid yesterday + S/100 today)
- AND a servicio extra S/150, aCuenta=150 (paid yesterday)
- WHEN cierre de caja emits for today
- THEN saldoPendiente MUST be S/0.00

#### Scenario: Partial payment from prior day

- GIVEN a dispensacion S/300, montoPagado=200 (all paid before today)
- WHEN cierre de caja emits for today
- THEN saldoPendiente MUST be S/100.00

#### Scenario: Same-day full payment

- GIVEN a dispensacion S/300 created today, montoPagado=300 (paid today)
- WHEN cierre de caja emits for today
- THEN saldoPendiente MUST be S/0.00

#### Scenario: Empty data

- GIVEN no dispensaciones and no servicios extra for today
- WHEN cierre de caja emits
- THEN saldoPendiente MUST be S/0.00 AND totalGeneral MUST be S/0.00

## New Requirements

### REQ-CIERRE-002: Pagos por Item desde Entidad

Per-dispensacion "Pagado" display MUST show `dispensacion.montoPagado`. Per-servicio "Pagado" MUST show `servicioExtra.aCuenta`. Per-item "Saldo" MUST equal `montoTotal - pagado`. These entity-tracked fields reflect ALL historical payments, not just today's.

#### Scenario: Dispensacion with partial multi-day payment

- GIVEN dispensacion S/300, montoPagado=250
- WHEN the item renders in cierre de caja
- THEN "Pagado" MUST show S/250.00 AND "Saldo" MUST show S/50.00

#### Scenario: Servicio extra fully paid before today

- GIVEN servicio extra S/80, aCuenta=80 (paid yesterday)
- WHEN the item renders
- THEN "Pagado" MUST show S/80.00 AND "Saldo" MUST show S/0.00

### REQ-CIERRE-003: Precisión Decimal Consistente

All monetary amounts in cierre de caja MUST display with exactly 2 decimal places using locale-aware formatting (`String.format("%,.2f", monto)`).

#### Scenario: Whole amount displays decimals

- GIVEN a dispensacion S/150 with no cents
- WHEN rendered in ResumenCard or item list
- THEN the amount MUST show "S/150.00", not "S/150"

### REQ-CIERRE-004: Consultas con Filtro por Fecha

DAO queries for dispensaciones and servicios extra in cierre de caja MUST accept `startDate` and `endDate` parameters filtering by entity date. Queries MUST NOT load all optica records into memory.

#### Scenario: Date-range filter limits results

- GIVEN optica has 500 dispensaciones across 3 years
- WHEN querying dispensaciones for optica X on date D
- THEN the query MUST return only records within [D_start, D_end]

### REQ-CIERRE-005: Clasificación de Pagos Futuros

Payments linked to entities with future `fechaEntrega` or `createdAt` relative to the selected cierre date MUST be excluded from `ventasHoy`. Such payments MUST be logged via `Log.w` with the linked entity's identifier.

#### Scenario: Future-dated entity payment excluded

- GIVEN a pago linked to dispensacion with `createdAt` = tomorrow
- WHEN cierre de caja computes totals for today
- THEN the pago MUST NOT contribute to `ventasHoy`
- AND a `Log.w` entry MUST be emitted with the dispensacion ID

### REQ-CIERRE-006: Aislamiento Multi-Óptica en Pruebas

Unit tests MUST verify multi-tenant data isolation by inserting data for optica A, querying for optica B, and asserting zero results.

#### Scenario: Cross-optica isolation verified

- GIVEN dispensaciones inserted for opticaId="A" on date D
- WHEN querying dispensaciones for opticaId="B" on date D
- THEN result MUST be empty

#### Scenario: Same-optica returns correct data

- GIVEN dispensaciones inserted for opticaId="A" on date D
- WHEN querying dispensaciones for opticaId="A" on date D
- THEN result MUST contain the inserted records
