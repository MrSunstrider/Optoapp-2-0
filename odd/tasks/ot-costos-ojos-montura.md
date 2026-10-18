# OT costos: ojos, montura, persistencia

Objective: Fix Costos/Margen so OD/OI appear by eye selection (not only prescription), montura pulls inventory cost, and saved costs reopen correctly.  
Branch: `fix/ot-costos-ojos-montura-persist`  
Delivery: `ask-on-risk`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim): `Por defecto no aparecen od y oi, solo aparece oi`

S2. User (verbatim): `y no guarda correctamente, vuelvo a abrir esa ventana y no queda lo guardado, revisa si lo hace correctamente`

S3. User (verbatim): `deberíamos tener la option de seleccionar 1 sólo ojo se da en muchos casos`

S4. User (verbatim): `el costo de la montura no está jalando correctamente, apesar de haber jalado la montura desde el inventario en dispensaciones`

S5. Assumption (from screenshot evidence): `Origen guardado: faltante` while OD/OI show values — whole-item origin must not collapse to faltante when only some components are missing (use mixto); montura S/ 0.00 with inventory frame must use montura.costo including zero as inventory hit, not treat zero as missing.

## Tasks

- [x] T1 S5 S2 — CostoOrigenResolver MIXTO + calculateCosts sets costoOrigen — route: inline — commit: 418042df
- [x] T2 S4 — Montura inventory cost without takeIf>0 — route: inline — commit: 418042df
- [x] T3 S1 S3 — ojosLente OD|OI|Ambos (schema+UI+calc+captura) — route: inline — commit: 418042df

## Log

L1. User: `Por defecto no aparecen od y oi, solo aparece oi, y no guarda correctamente, vuelvo a abrir esa ventana y no queda lo guardado, revisa si lo hace correctamente, deberíamos tener la option de seleccionar 1 sólo ojo se da en muchos casos, el costo de la montura no está jalando correctamente, apesar de haber jalado la montura desde el inventario en dispensaciones.` (2026-10-17) + screenshot Editar Dispensación Costos/margen Origen faltante, Montura 0.00.

L2. Implemented T1–T3 inline: MIXTO origin, montura inventory cost (incl. 0), ojosLente Ambos|OD|OI (Room 60 + supabase migration + LenteForm + calc/captura). Focused + full `testDebugUnitTest` GREEN (3293 tests). Risk: item 1 (Room/schema + supabase column) and item 3 (sync DTO). Pending remote `ojos_lente` migrate until authorized.
