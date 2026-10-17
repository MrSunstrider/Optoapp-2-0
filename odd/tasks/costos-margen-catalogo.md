# Costos/margen + catalogo optico por familias

Objective: App derives stock|fab + serie from refraction and product family; costs come from matrices (never hardcoded); wizard Orden→Productos→Gestion(montoTotal)→Costos/Margen; biselado bidirectional.  
Branch: `feat/costos-margen-catalogo`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User (verbatim intent): `quiero que la app haga el calculo correcto en base al tipo de lente, distancia, material, tratamiento` and series from graduacion/refraccion; costos from matrices lentes/LC/biselado.

S2. User (verbatim): `quiero que costos y margen sean el paso 4 de 4 despues de gestion (3 de 3) para que pueda hacer el calculo correcto despues de haber ingresado el monto total del contrato.`

S3. User catalog: product **families** (e.g. Resina antireflejo Blue Def/Prot), not flat Resina + all treatments; extras listed per family; Blue Def = Blue Prot alias.

S4. User (verbatim): `para intermedio y cerca nunca se suele poner filtro fotocromatico, polarizado o transition`.

S5. User: stock series 1era/2da/3era vs fabricacion per family rules; bifocal/multifocal stock windows + optional specialist force-stock ignoring astigmatismo; Blue Def/Prot bifocal/multifocal always fabricacion.

S6. User (verbatim): `olvide agregar que otra variable es la serie del lente, los valores estan definidos pero no los costos` — serie derived by app; costo per serie entered in matrix / paso 4.

S7. User: alto indice / IR manual costs (1.6/1.7 resina; 1.7/1.7 rose/1.8 cristal); Solar needs curva base 4-6-8; discromatopsia requires tipo when selected.

S8. User: biselado costs variable; enter manually; save to CostosYGastos biselado from OT paso 4 or from CostosYGastos UI.

S9. Regalos: include in OT margen (Σ cantidad×costoUnitario); edit in InformacionFinanciera; cost at registration not physical delivery.

S10. Out of scope: MarginFacts RPC reopen; web UI; historical re-snapshot; per-gift delivery flag.

## Tasks

- [x] T1 S3-S7 — LensCatalogRules + tests (WU1) — route: inline — commit: `d2037261`
- [x] T2 S6 — Schema matrices + sync keys (WU2) — route: inline — commit: pending
- [x] T3 S1,S6,S7 — CalcularCostoItemUseCase via rules (WU3) — route: inline — commit: pending
- [x] T4 S2,S9 — Wizard 4 steps + margen regalos (WU4) — route: inline — commit: pending
- [x] T5 S3,S4 — LenteForm cascade (WU5) — route: inline — commit: pending
- [ ] T6 S8 — Biselado bidirectional (WU6) — route: inline — commit:
- [ ] T7 — Sync, GGA, JD, stacked PRs (WU7) — route: inline — commit:

## Log

L1. User authorized: `empezamos con ciclo gentle ai completo, si hay dudas consulta` (2026-10-17).
L2. Feature doc created; branch `feat/costos-margen-catalogo` from main; starting T1/WU1.
L3. T1 done: `d2037261` — LensCatalogRulesTest 15/15 green; GGA PASSED on commit.
L4. User: `hazlo todo, si tienes que mejorar elo uxui hazlo` — authorized full remaining WUs + UX.
L5. T2–T5 implemented: Room 59 + supabase keys; resolver wired; wizard 4 steps; LenteForm cascade; regalos in margen. WU6 biselado writeback still open.
