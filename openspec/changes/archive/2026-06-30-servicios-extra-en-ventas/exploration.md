## Exploration: servicios-extra-en-ventas

### Current State

**Entity model:**
- `ServicioExtra` (table: `servicios_extra`) has `id`, `montoTotal`, `aCuenta`, `fecha`, `estado`, `metodoPago`, `opticaId`, `pacienteId`. **No** `montoPagado` field — closest analogue is `aCuenta` (down payment at creation).
- `DispensacionOptica` has both `montoTotal` and `montoPagado`.
- `Pago` has `dispensacionId` (nullable) and `servicioExtraId` (nullable) — a payment can link to either or neither.

**Three broken ViewModels:**

1. **ReportesViewModel** (lines 83-89, 113-134):
   - `totalVendido` — sums only `DispensacionOptica.montoTotal` via `allDispensaciones`. ❌
   - `totalPagado` — sums only `DispensacionOptica.montoPagado` via `allDispensaciones`. ❌
   - `totalCobrado` — sums `Pago.monto` via `getPagosByDateRangeForOptica`. ✅ (includes payments for servicios extra)
   - `cobrosPeriodo` — **BUG**: checks `pago.dispensacionId?.let { dispMap[it]?.fecha }`. If `dispensacionId` is null (true for servicio extra payments), it falls to `else -> pago.monto`, wrongly classifying them as past-due collections. ❌

2. **CierreCajaViewModel** (lines 96-114):
   - `ventasHoy` — classifies pagos via `pago.dispensacionId?.let { dispMap[it]?.fecha }`. When `dispensacionId` is null, the `else -> ventasHoy += pago.monto` branch correctly includes them. ✅
   - `totalVentasHoy` — sums only `DispensacionOptica.montoTotal` from dispensaciones filtered by date. **Misses servicios extra.** ❌
   - `saldoPendiente = totalVentasHoy - ventasHoy` — underestimated because `totalVentasHoy` is missing servicios extra. ❌

3. **BIViewModel** (line 90):
   - `proyectada` — sums only `DispensacionOptica.montoTotal`. Misses servicios extra. ❌
   - `cobrada` — sums `Pago.monto` via `getPagosByDateRangeForOptica`. ✅

**OperacionHoyViewModel:**
- `cobrosHoy` — sums `pagosHoy.sumOf { it.monto }` via `getPagosByDateRangeForOptica`. ✅

### Affected Areas

- `optoapp/src/main/java/com/example/optoapp/viewmodel/ReportesViewModel.kt` — `totalVendido`, `totalPagado`, `cobrosPeriodo` need fixing
- `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt` — `totalVentasHoy`, `saldoPendiente` need fixing; `CierreCajaUiState` maybe needs new field(s)
- `optoapp/src/main/java/com/example/optoapp/viewmodel/BIViewModel.kt` — `recaudacionProyectada` needs fixing
- `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelDiarioTest.kt` — test data needs servicio extra coverage
- `optoapp/src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt` — test data needs servicio extra coverage
- `optoapp/src/main/java/com/example/optoapp/data/OptoRepository.kt` — already exposes `getAllServiciosForOptica(...)` ✅
- `optoapp/src/main/java/com/example/optoapp/data/DispensacionRepository.kt` — already exposes `getAllServiciosForOptica(...)` ✅

### Cierre de Caja Analysis

**Should `totalVentasHoy` include ServicioExtra.montoTotal?** YES. A servicio extra created today is a sale registered today. Its full `montoTotal` should count toward the day's total sales.

**Should there be a separate line for "Servicios Extra" in the UI?** Optionally useful for transparency, but not required for correctness. The simplest approach is to add it to the existing totals. If the UI needs separate breakdown, add a `totalServiciosExtraHoy` field to `CierreCajaUiState`.

**How does `saldoPendiente` need to change?**
Current: `saldoPendiente = totalVentasHoy - ventasHoy`
Proposed: `saldoPendiente = (totalVentasHoy + totalServiciosExtraHoy) - ventasHoy`
- `ventasHoy` already includes payments for servicios extra (via the `else` branch).
- `totalVentasHoy` needs to include servicios extra `montoTotal`.

**Payment classification in CierreCaja:**
Currently, the `ventasHoy` / `cobrosAtrasados` logic uses `dispensacionId` to determine if a payment belongs to today's sales or is a past-due collection. For `servicioExtraId` payments, we need the same logic — check `servicioExtraId?.let { servicioMap[it]?.fecha }`:
- `fecha == today → ventasHoy`
- `fecha < today → cobrosAtrasados`

### Approaches

1. **A: Minimal fix — add ServicioExtra to existing reactive flows**
   - In `ReportesViewModel`: add `repository.getAllServiciosForOptica(opticaId)` as a third source in the combine chain for `allDispensaciones` (or create a separate combined flow). Compute new totals.
   - In `CierreCajaViewModel`: add `repository.getAllServiciosForOptica(opticaId)` to the combine, add servicios extra to `totalVentasHoy`, and fix the pago classification to also check `servicioExtraId`.
   - In `BIViewModel`: add servicios extra to the combine.
   - Pros: Minimal refactor, reuses existing patterns. Fixes all bugs.
   - Cons: Increases complexity of combine chains. Need to handle `montoPagado` equivalent for servicios extra (use `aCuenta` or sum pagos).
   - Effort: Medium

2. **B: Extract a shared "daily sales total" helper**
   - Create a utility function or flow that combines dispensaciones + servicios extra into a unified view of daily sales.
   - All ViewModels would consume this shared flow.
   - Pros: DRY, single source of truth, easier to test.
   - Cons: Requires more refactoring, needs a shared state holder (likely a new repository method or utility).
   - Effort: High

3. **C: Add servicios extra as a separate line item in UI state**
   - Keep existing disp-only totals as-is, add new fields like `totalServiciosExtra` and `totalVentasCombinado` to each UI state.
   - Pros: Backward-compatible UI, clear separation for the user.
   - Cons: More fields in state, more complexity in UI rendering. Doesn't actually fix the bug — just adds data next to it.
   - Effort: Medium

### Recommendation

**Approach A (minimal fix)** is the best starting point. The changes are:

1. **ReportesViewModel**:
   - Add `repository.getAllServiciosForOptica(opticaId)` to the `allDispensaciones` combine chain.
   - `totalVendido`: change sum to `sumOf { it.montoTotal }` from the combined list (both disp + servicio).
   - `totalPagado`: for DispensacionOptica use `montoPagado`, for ServicioExtra use `aCuenta` as equivalent.
   - `cobrosPeriodo`: add a `servicioMap` alongside `dispMap`, and check `servicioExtraId?.let { servicioMap[it]?.fecha }` before classifying as cobro atrasado.

2. **CierreCajaViewModel**:
   - Add `repository.getAllServiciosForOptica(opticaId)` to the combine chain.
   - `totalVentasHoy`: sum both disp and servicio `montoTotal` for the selected date.
   - Payment classification: add `servicioMap` and check `servicioExtraId` alongside `dispensacionId`.
   - `CierreCajaUiState`: optionally add `totalServiciosExtraHoy` field for transparency.

3. **BIViewModel**:
   - Add servicios extra to `recaudacionProyectada` sum.

### Risks

- **ServicioExtra has no `montoPagado`** — using `aCuenta` may not be semantically identical. Validate with the user. Alternative: compute from actual pago records.
- **Reactive flow complexity** — adding another source to `combine` chains increases state update overhead. Use `combine` with up to 3-4 flows, not more.
- **`cobrosPeriodo` / `cobrosAtrasados` logic change** — adding `servicioExtraId` check changes the classification for existing data. Must verify: payments for servicios extra created on the same day as the report period should NOT count as cobros atrasados.
- **Edge case: servicios without pagos, pagos without servicioExtraId** — standalone payments (neither dispensacionId nor servicioExtraId) should continue falling into the `else` branch (ventasHoy).
- **Test data** — all existing mockk setups in tests only provide dispensaciones + pagos. Every test that asserts `totalVendido`, `totalPagado`, `totalVentasHoy`, `saldoPendiente`, or `recaudacionProyectada` will need updated mock data that includes servicios extra. Tests for `cobrosPeriodo` with null dispensacionId will need to explicitly set `servicioExtraId` to differentiate between "payment for servicio extra" and "orphan payment".
- **`aCuenta` vs actual payments** — if a servicio extra has `aCuenta = 0` but later payments exist, the `totalVendido` at the report level would include the full `montoTotal`, but `totalPagado` (using `aCuenta`) would show 0. This matches the existing disp behavior (montoPagado is set at creation time).

### Ready for Proposal
Yes
