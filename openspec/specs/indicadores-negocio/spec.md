# Business Indicators Engine Specification

## Purpose

Android-side engine that fetches, computes, and delivers the 8 business indicators to consumers (ViewModels, UI). It coordinates between Supabase RPCs (online) and Room local data (offline) to provide business answers in plain language: sales, collections, margins, debtors, cash projection, and inventory health.

## Requirements

### R1: Pago Entity — ventaId Field

The `Pago` Room entity SHALL have field `val ventaId: String? = null` with Room column `ventaId`.

#### Scenario: Field exists nullable
- GIVEN the Pago entity compiles
- WHEN the `pagos` table schema is inspected
- THEN column `ventaId` exists, TEXT, nullable
- AND existing rows have NULL in `ventaId`

### R2: Room Migration v32→v33

A migration `MIGRATION_32_33` SHALL: `ALTER TABLE pagos ADD COLUMN ventaId TEXT`, `CREATE INDEX IF NOT EXISTS index_pagos_ventaId ON pagos(ventaId)`. Database version SHALL be 33.

#### Scenario: Migration preserves data
- GIVEN a device at version 32 with 50 pago rows
- WHEN MIGRATION_32_33 runs
- THEN all 50 rows preserved
- AND `ventaId` is NULL for existing rows

### R3: ResumenDiarioDao — Monthly Aggregation

The DAO SHALL provide `getByOpticaAndMonth(opticaId: String, yearMonth: String): suspend fun List<ResumenDiarioEntity>` for offline indicator computation.

#### Scenario: Offline monthly aggregation
- GIVEN 30 cached daily rows for July 2026
- WHEN `getByOpticaAndMonth('o1', '2026-07')` is called
- THEN 30 rows returned for client-side SUM

### R4: getDeudores — Room Debor Query

A DAO query SHALL JOIN `ventas` + `pagos` + `pacientes` locally, filtering `saldo > 0.005`, ordered by `fecha ASC`.

#### Scenario: Local deudores returns matching debtors
- GIVEN local Room has 3 ventas with partial payments
- WHEN the deudores query is called
- THEN 3 rows with `saldo > 0` are returned, oldest first

### R5: ObtenerAnalisisMensualUseCase

A Hilt UseCase SHALL exist. Online: calls `rpc_analisis_mensual` via Supabase client. Offline (IOException): falls back to `resumenDiarioDao.getByOpticaAndMonth()` + client-side SUM, with indicators 5–8 returning 0/empty + offline flag.

#### Scenario: Online returns RPC data
- GIVEN device has network
- WHEN `invoke('o1', '2026-07-01')` is called
- THEN RPC is called and mapped to `AnalisisMensual` domain model

#### Scenario: Offline falls back to Room
- GIVEN device has no network
- WHEN `invoke('o1', '2026-07-01')` is called
- THEN Room aggregation is used for indicators 1–4
- AND indicators 5–8 return 0/empty with offline flag

### R6: ObtenerDeudoresUseCase

A Hilt UseCase SHALL exist. Online: calls `rpc_deudores`. Offline: JOINs local Room tables.

#### Scenario: Online returns deudores
- GIVEN device is online, optica has 2 debtors
- WHEN `invoke('o1')` is called
- THEN 2 `Deudor` objects returned with nombre, telefono, saldo, diasDeuda

#### Scenario: Offline returns local deudores
- GIVEN device is offline, Room has synced ventas+pagos
- WHEN `invoke('o1')` is called
- THEN local JOIN query returns cached debtors

## Out of Scope

- Fase 8 recommendation engine
- Fase 9 UI screens for indicator display
- Room entities for `margen_por_categoria` or `costos_productos` (server-side only)
