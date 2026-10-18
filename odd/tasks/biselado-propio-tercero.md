# Biselado Propio vs Tercero (completo / pulido)

Objective: Propio/Tercero per OT item, optica default + internal rate in configuracion_financiera, auto-gasto on Tercero without double-counting analisis neto.  
Branch: `feat/biselado-propio-tercero` (stack after merging or rebasing `fix/ot-costos-ojos-montura-persist` — Room is already v60)  
Delivery: `auto-chain` / `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. Per item `origen_biselado`: `Propio` | `Tercero`.

S2. Optica default `configuracion_financiera.origen_biselado_default` — new items inherit; existing rows default `Tercero`.

S3. Internal rate `configuracion_financiera.costo_biselado_propio` (default 0): Propio with 0 → no deduct / not faltante; Propio with >0 → deduct that pair rate in Costos/Margen.

S4. Tercero → existing `costos_biselado` matrix + manual override.

S5. On OT save, Tercero with `costoRealBiselado > 0` upserts linked `gastos_operativos`; Propio or 0 removes/cancels that linked gasto (idempotent).

S6. Linked gastos visible in Gastos UI but excluded from analisis neto aggregations (already in OT `costoMes`).

S7. Analysis assumptions locked in plan review (2026-10-18): do not use `tipoGasto=derivado_ot` (conflicts with fijo|variable); use nullable link column + `tipoGasto=variable`. Config financiera has DAO/sync but no edit UI today — add UI section.

## Tasks

- [x] T1 S1 S2 S3 S7 — Schema Room 61 + Supabase: item.origen_biselado, config defaults/rate, gasto.origen_dispensacion_item_id — route: inline — commit: (bundled T1–T4)
- [x] T2 S1 S3 S4 — Calc/Captura/CostoMargen/LenteForm + ViewModel seed from config — route: inline — commit: (bundled T1–T4)
- [x] T3 S2 S3 S7 — Config financiera edit UI + sync DTO — route: inline — commit: (bundled T1–T4)
- [x] T4 S5 S6 — Auto-gasto upsert on saveDispensacion + exclude linked from analisis neto — route: inline — commit: (bundled T1–T4)
- [ ] T5 — Focused + full suite, GGA, remote migrate when authorized, JD (money), PR — route: inline — commit:

## Log

L1. User requested analysis of plan Biselado Propio vs Tercero (completo/pulido) so Gentle AI can run the full ODD cycle (2026-10-18).

L2. Plan review findings (blocking corrections before apply):
- Room already at version 60 (`ojos_lente`); this feature needs **61**.
- Branch today: `fix/ot-costos-ojos-montura-persist` (unpushed). New work should use `feat/biselado-propio-tercero` stacked on that fix or on main after merge.
- `GastoTipoResolver` only allows `fijo`|`variable` — plan’s `tipoGasto=derivado_ot` is invalid. Use `origen_dispensacion_item_id` (nullable) + keep `tipoGasto=variable`; filter analisis by link column NOT null.
- No existing ConfiguracionFinanciera **edit UI** (`ConfiguracionScreen`/`ConfiguracionViewModel` ignore it). T3 must add UI, not wire to a phantom screen.
- `CalcularCostoItemUseCase` must gain config (or rate) dependency for Propio rate.
- Hook point for auto-gasto: `DispensacionViewModel.saveDispensacion` after item persist.
- Risk: high (schema, sync contracts, money/analisis). JD mandatory before merge. Remote migrate needs explicit OK.

L3. T1–T4 implemented on `feat/biselado-propio-tercero` (2026-10-18): Room 61 + Supabase migration, OrigenBiselado, Propio rate in calc, LenteForm dropdown, config UI section, SyncBiseladoGastosDerivados + analisis filter. Focused + full `testDebugUnitTest` BUILD SUCCESSFUL. Pending: work-unit commit, remote migrate OK, GGA/JD, PR.
