# Costos Lentes = productos + Reportes Ventas crash

Objective: Costos y Gastos Lentes form mirrors dispensación product entry (tipo/subtipo/distancia/material/tratamientos) so OT Costos y margen saves match matrix rows; fix Reportes Ventas crash; install R8 APK.  
Branch: `feat/ui-theme-toggle-disp-chip`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim): `costos y gastos deberia ser igual que productos(dispensaciones) conteniendo tipo de lente, con sus subtipos, material y tratamientos, deberia sera una copia exacta para poder guardar los costos desde dispensaciones (costos y margen donde se ingresan) y esto se guarde en lentes-costos y gastos.`

S2. User (verbatim): `revisando los cuadros en reportes, en ventas se me cerro la app en alguno de esos reportes revisalo, solucionalo hasta que quede ok.`

S3. User (verbatim): `installo en mi movil con r8`

## Tasks

- [x] T1 S1 — Costos Lentes form = LenteForm catalog (distancia, subtipo, familia, multi-tratamientos, discromatopsia, conditional IR/curva); saveCosto via LensCatalogRules / matrix keys — route: inline — commit: pending
- [ ] T2 S2 — Reportes Ventas crash: periodComparativo off main + empty/zero bar guards + year parse — route: delegated — commit:
- [ ] T3 S3 — assembleRelease (R8) + adb install -r — route: inline — commit: n/a

## Log

L1. User (2026-10-21): full request verbatim in S1–S3. Explore: Costos save skips LensCatalogRules; LenteForm has distancia/multi-tratamientos; Ventas crash likely main-thread periodComparativo + Vico on empty/negative series.
L2. User (2026-10-21): Implemented the Costos Lentes dialog parity slice. The new dialog now mirrors the lens catalog flow with distance, subtype, family, multi-treatment chips, discromatopsia, and conditional IR/curva, and saveCosto now persists lookup-compatible matrix keys.
