# Costos nuevo: stock/fab, serie, parity, dedupe

Objective: Nuevo costo Lentes matches dispensación catalog options; Stock|Fabricación replaces Distancia; serie required unless fabricación; open-row upsert prevents duplicate Resina·serie rows.  
Branch: `feat/costos-nuevo-stock-serie-parity`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim): `en nuevo costo en ves de distancia, mejor seria seleccionar stock o fabricacion`

S2. User (verbatim): `serie es obligatorio a menos que sea fabricacion`

S3. User (verbatim): `veo algunos costos duplicados como resina 1era serie...`

S4. User (verbatim): `ademas revisa ese tema completo, ya que no estan las opciones que tengo en productos de dispensacion`

Assumptions (documented): Distancia stays out of the matrix key (OT lookup never uses it); Costos familias use `distancia=null` so monofocal foto families still appear for pricing. Nuevo costo persists the same keys `CalcularCostoItemUseCase` / `lookupVigente` use: material, tipo, stock|fab, tratamiento, serie, subtipo, IR, curva. Existing duplicate DB rows are not mass-deleted; new saves upsert the open vigente row (same as OT). Fabricación Resina/Cristal blocks collapse to one Fabricación list so Policarbonato/Trivex/Ocupacional rows are visible.

## Tasks

- [x] T1 S1–S2 — Form: Stock|Fabricación selector; remove Distancia; serie required when stock — route: inline — commit: 2c7eab45
- [x] T2 S3 — saveCosto upserts open lookupVigente row via MatrizVigenciaUseCase — route: inline — commit: 2c7eab45
- [x] T3 S4 — Catalog parity: Fabricación block unfiltered by material; tipos/familias aligned with LensProductCatalog (excl. LC) — route: inline — commit: 2c7eab45

## Log

L1. User (2026-10-22): full request verbatim in S1–S4. Explore: Distancia UI-only; stock/fab locked to block; serie optional; save always new UUID; Fabricación Resina/Cristal hides Policarbonato.
L2. User (2026-10-22): `recuerda que es de aqui de donde va a jalar los costos en dispensaciones, dependiendo de si es stock o fabricacion, material, tipo, serie, tratamiento etc` — matrix is the pricing source of truth for dispensación.
L3. Implemented T1–T3: Nuevo costo selects stock|fab (lookup key), serie required for stock / null for fab, upsert open `lookupVigente` via MatrizVigenciaUseCase, single Fabricación block shows Policarbonato/etc. Focused tests BUILD SUCCESSFUL.
L4. Judgment Day JD-C1 (2026-10-22): `saveCosto` now persists fabricacion+null serie when `LensCatalogRules.forcesFabricacionIndependentOfRx` (FORCE extras excl. UV_400, ALWAYS_FABRICACION families, cristal alto índice IR≠1.7/1.70); form auto-locks modality and hides serie; `puedeEditarMatrices` guard on save.
