# Tasks: servicios-extra-en-ventas

## Change Summary

Fold `ServicioExtra` revenue into `ReportesViewModel`, `CierreCajaViewModel`, and `BIViewModel` reactive chains. Add `fechaEntrega` to `ServicioExtra` entity with Room migration 28→29 and Supabase migration.

---

## Phases & Tasks

### Phase 1: Entity + Schema (PR 1)

**Rationale**: Foundation layer — adding `fechaEntrega` column and migration must land before ViewModel logic.

#### Task 1.1 — RED test: MIGRATION_28_29 column add

**Description**: Create `ServicioExtraMigration28To29Test.kt` mirroring `Migration27To28Test` pattern. Test that `MIGRATION_28_29` adds `fecha_entrega` column to `servicios_extra` and preserves existing rows.

**Files to create**:
- `optoapp/src/test/java/com/example/optoapp/data/ServicioExtraMigration28To29Test.kt`

**Tests to write**:
- `migration_28_29_exists_and_targets_correct_versions` — asserts `startVersion = 28`, `endVersion = 29`
- `migration_28_29_is_re_exported_from_opto_database` — asserts `OptoDatabase.MIGRATION_28_29 == com.example.optoapp.data.MIGRATION_28_29`
- `full_migration_chain_6_to_29_is_sequential` — asserts chain includes MIGRATION_28_29; `startVersion = 6`, `endVersion = 29`; each adjacent pair's `endVersion == next.startVersion`
- `migration_28_29_preserves_existing_rows` — creates v28 DB with `servicios_extra` rows, runs migration, asserts all rows survive with `fecha_entrega IS NULL`
- `migration_28_29_allows_fresh_insert_with_fecha_entrega` — post-migration INSERT sets `fecha_entrega = '2026-07-01'`, asserts persisted
- `migration_28_29_on_empty_table_succeeds` — migration on v28 DB with zero rows succeeds; new column is `NULL`

**Verification criteria**:
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.data.ServicioExtraMigration28To29Test"` → RED (test class not yet implemented)

---

#### Task 1.2 — GREEN: Add fechaEntrega to ServicioExtra entity

**Description**: Add `fechaEntrega: LocalDate? = null` to `ServicioExtra` data class in `DispensacionEntity.kt`, with `@SerialName("fechaEntrega")` and `@Serializable(with = LocalDateSerializer::class)` annotations (mirrors `DispensacionOptica.fechaEntrega`).

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt`

**Verification criteria**:
- [x] `ServicioExtra` has `fechaEntrega: LocalDate? = null` at the end of the data class
- [x] Existing `ServicioExtra` construction sites (if any) compile without changes due to default value

---

#### Task 1.3 — GREEN: Implement MIGRATION_28_29

**Description**: Add `MIGRATION_28_29` in `OptoDatabaseMigrations.kt` (after `MIGRATION_27_28`). `ALTER TABLE servicios_extra ADD COLUMN fecha_entrega TEXT` — nullable, no DEFAULT.

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt`

**Verification criteria**:
- [x] `MIGRATION_28_29.startVersion == 28`
- [x] `MIGRATION_28_29.endVersion == 29`
- [x] Single `db.execSQL("ALTER TABLE servicios_extra ADD COLUMN fecha_entrega TEXT")` inside `migrate()`

---

#### Task 1.4 — GREEN: Bump OptoDatabase to version 29

**Description**: Update `OptoDatabase.kt`:
1. `version = 28` → `version = 29`
2. Add `val MIGRATION_28_29 get() = com.example.optoapp.data.MIGRATION_28_29` in companion
3. Append `MIGRATION_28_29` to the `addMigrations(...)` list

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt`

**Verification criteria**:
- [x] `version == 29`
- [x] `OptoDatabase.MIGRATION_28_29` re-export present
- [x] `addMigrations(...)` list ends with `MIGRATION_28_29`

---

#### Task 1.5 — GREEN: Create Supabase migration

**Description**: Create `supabase/migrations/20260630000000_servicios_extra_fecha_entrega.sql` with `ALTER TABLE public.servicios_extra ADD COLUMN fecha_entrega DATE;` (nullable DATE — back-fills existing rows to NULL).

**Files to create**:
- `supabase/migrations/20260630000000_servicios_extra_fecha_entrega.sql`

**Verification criteria**:
- [x] Migration file exists with single `ALTER TABLE` statement
- [x] `supabase db lint` passes (if `supabase` CLI available locally)

---

#### Task 1.6 — VERIFY Phase 1

**Description**: Run all migration tests to confirm green.

**Verification criteria**:
- [x] ```
./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.data.ServicioExtraMigration28To29Test"
./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.data.Migration27To28Test"
```
Both → GREEN.

---

### Phase 2: ReportesViewModel tests + logic (PR 2)

**Rationale**: `totalVendido`, `totalPagado`, `cobrosPeriodo` dual-ID classification.

#### Task 2.1 — RED test: ReportesViewModelDiarioTest with servicio-extra fixtures

**Description**: Update `ReportesViewModelDiarioTest.kt` with servicio-extra fixtures and new assertions. Add `getAllServiciosForOptica` mock returning `Flow<List<ServicioExtra>>`. Add tests:

**Files to modify**:
- `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelDiarioTest.kt`

**Tests to add / update**:
- `totalVendido_includes_ServicioExtra_montoTotal_when_on_selected_date` — GIVEN one disp (montoTotal=100) + one serv (montoTotal=40) both in range → `totalVendido` MUST be 140.0
- `totalPagado_includes_ServicioExtra_aCuenta_when_on_selected_date` — GIVEN one disp (montoPagado=60) + one serv (aCuenta=20) both in range → `totalPagado` MUST be 80.0
- `cobrosPeriodo_excludes_pago_with_servicioExtraId_in_range` — GIVEN pago with `servicioExtraId` whose serv `fecha` is inside period → pago's `monto` MUST NOT contribute to `cobrosPeriodo`
- `cobrosPeriodo_includes_pago_with_servicioExtraId_out_of_range` — GIVEN pago with `servicioExtraId` whose serv `fecha` is outside period → pago's `monto` MUST contribute to `cobrosPeriodo`
- `cobrosPeriodo_includes_orphan_pago` — GIVEN pago with neither `dispensacionId` nor `servicioExtraId` → contributes to `cobrosPeriodo`
- `cobrosPeriodo_dispensacion_date_wins_when_both_ids_set` — GIVEN pago with both IDs, dispensación date in-range, serv date out-of-range → pago excluded from `cobrosPeriodo`
- Update existing `totalVendido` / `totalPagado` test expected values to include servicio fixtures

**Verification criteria**:
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.viewmodel.ReportesViewModelDiarioTest"` → RED (method not yet added to ViewModel)

---

#### Task 2.2 — GREEN: Implement ReportesViewModel changes

**Description**: Update `ReportesViewModel.kt`:
1. Add `getAllServiciosForOptica` to `combine` chain → produce `allServiciosDelPeriodo` StateFlow (filtered by `dentroDelPeriodo`)
2. `totalVendido = allDispensaciones.sumOf { it.montoTotal } + allServiciosDelPeriodo.sumOf { it.montoTotal }`
3. `totalPagado = allDispensaciones.sumOf { it.montoPagado } + allServiciosDelPeriodo.sumOf { it.aCuenta }`
4. `cobrosPeriodo`: add 3-arg `combine(pagos, todasDisp, todasServ)` with `servMap: Map<String, ServicioExtra>`, dual-ID `when` expression per Decision 2 in design.md

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/viewmodel/ReportesViewModel.kt`

**Verification criteria**:
- [x] All Task 2.1 tests → GREEN
- [x] `cobrosPeriodo` dual-ID `when` follows spec: disp fecha wins when both IDs present; orphan goes to `cobrosPeriodo`

---

#### Task 2.3 — RED test: ReportesViewModelOtrosPeriodosTest with servicio-extra fixtures

**Description**: Update `ReportesViewModelOtrosPeriodosTest.kt` with servicio-extra fixtures for Semanal, Este mes, Este año, Anual, Todo periods. Assert `totalVendido` and `totalPagado` include servicios extra per period.

**Files to modify**:
- `optoapp/src/test/java/com/example/optoapp/viewmodel/ReportesViewModelOtrosPeriodosTest.kt`

**Verification criteria**:
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.viewmodel.ReportesViewModelOtrosPeriodosTest"` → RED

---

#### Task 2.4 — GREEN: Ensure multi-period filtering includes servicios extra

**Description**: Verify `allServiciosDelPeriodo` filtering (via `dentroDelPeriodo`) works correctly for all period types. No additional code needed if Task 2.2 is correct — confirm by running Task 2.3 tests.

**Verification criteria**:
- [x] Task 2.3 tests → GREEN

---

### Phase 3: CierreCajaViewModel tests + logic (PR 2)

**Rationale**: New `CierreCajaUiState` fields (`serviciosExtraHoy`, `totalServiciosExtra`, `totalGeneral`), dual-ID payment classification, `saldoPendiente = totalGeneral - ventasHoy`.

#### Task 3.1 — RED test: CierreCajaViewModelTest with servicio-extra fixtures

**Description**: Update `CierreCajaViewModelTest.kt` with servicio-extra fixtures. Add new assertions for desglose fields and dual-ID classification.

**Files to modify**:
- `optoapp/src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt`

**Tests to add / update**:
- `cierreCaja_desglose_includes_serviciosExtraHoy_and_totalServiciosExtra` — GIVEN today has disp (montoTotal sum=300) + serv extra (montoTotal sum=150) → `totalServiciosExtra` MUST be 150.0 AND `serviciosExtraHoy` size matches
- `cierreCaja_totalGeneral_equals_totalVentasHoy_plus_totalServiciosExtra` — GIVEN disp total=300, serv total=150 → `totalGeneral` MUST be 450.0
- `cierreCaja_saldoPendiente_equals_totalGeneral_minus_ventasHoy` — GIVEN totalGeneral=450, ventasHoy=200 → `saldoPendiente` MUST be 250.0
- `cierreCaja_pago_linked_to_today_servicioExtra_contributes_to_ventasHoy` — GIVEN pago with `servicioExtraId` whose serv `fecha == hoy` → pago's `monto` contributes to `ventasHoy`
- `cierreCaja_pago_linked_to_older_servicioExtra_contributes_to_cobrosAtrasados` — GIVEN pago with `servicioExtraId` whose serv `fecha < hoy` → contributes to `cobrosAtrasados`
- `cierreCaja_orphan_pago_contributes_to_ventasHoy` — GIVEN pago with neither ID → contributes to `ventasHoy` (preserved else branch)
- Update existing `ventasHoy`/`cobrosAtrasados` test expected values if needed

**Verification criteria**:
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.viewmodel.CierreCajaViewModelTest"` → RED

---

#### Task 3.2 — GREEN: Implement CierreCajaViewModel changes

**Description**: Update `CierreCajaViewModel.kt`:
1. `CierreCajaUiState` add: `serviciosExtraHoy: List<ServicioExtra> = emptyList()`, `totalServiciosExtra: Double = 0.0`, `totalGeneral: Double = 0.0`; update `saldoPendiente` formula
2. `CierreCajaResult` add same three fields
3. `observePagos`: add `getAllServiciosForOptica` as third `combine` source → build `servMap: Map<String, ServicioExtra>`
4. Filter `serviciosExtraHoy = servicios.filter { it.fecha == fecha }`
5. Dual-ID `when` expression for classification per Decision 2 in design.md
6. `saldoPendiente = totalGeneral - ventasHoy`

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt`

**Verification criteria**:
- [x] All Task 3.1 tests → GREEN
- [x] `CierreCajaUiState` interface matches design.md contract

---

### Phase 4: BIViewModel tests + logic (PR 2)

**Rationale**: `recaudacionProyectada` must include `ServicioExtra.montoTotal`.

#### Task 4.1 — RED test: BIViewModelTest for recaudacionProyectada

**Description**: Create `BIViewModelTest.kt` — first-ever test file for `BIViewModel`. Test `recaudacionProyectada` includes servicios extra. Reuse mockk patterns from other ViewModel tests.

**Files to create**:
- `optoapp/src/test/java/com/example/optoapp/viewmodel/BIViewModelTest.kt`

**Tests to write**:
- `recaudacionProyectada_includes_ServicioExtra_montoTotal` — GIVEN disp list (montoTotal=500) + serv list (montoTotal=120) in range → `recaudacionProyectada` MUST be 620.0
- `recaudacionProyectada_with_no_servicios_extra_matches_dispensaciones_only` — GIVEN only disp → matches pre-change behavior
- Period-change re-emission: changing period re-triggers `recaudacionProyectada` calculation

**Verification criteria**:
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.viewmodel.BIViewModelTest"` → RED

---

#### Task 4.2 — GREEN: Implement BIViewModel changes

**Description**: Update `BIViewModel.kt`:
1. Add `servicios: List<ServicioExtra>` to `BiCoreFlows` data class
2. Inner `combine`: add `getAllServiciosForOptica` as fifth source (4→5 arg)
3. `recaudacionProyectada = dispensaciones.sumOf { it.montoTotal } + servicios.sumOf { it.montoTotal }`

**Files to modify**:
- `optoapp/src/main/java/com/example/optoapp/viewmodel/BIViewModel.kt`

**Verification criteria**:
- [x] Task 4.1 tests → GREEN
- [x] `./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.viewmodel.BIViewModelTest"` → GREEN

---

### Phase 5: Final verification (PR 2)

#### Task 5.1 — Full test suite

**Description**: Run the full unit test suite.

**Verification criteria**:
```
./gradlew :optoapp:testDebugUnitTest --stacktrace
```
- [x] → All GREEN.

---

#### Task 5.2 — JaCoCo coverage

**Description**: Run JaCoCo to ensure 5% minimum instruction coverage threshold is maintained.

**Verification criteria**:
```
./gradlew :optoapp:jacocoTestReport
```
- [x] → 5% minimum threshold maintained (no regression).

---

## Summary

| Phase | Tasks | PR |
|-------|-------|-----|
| 1 — Entity + Schema | 1.1–1.6 | PR 1 |
| 2 — ReportesViewModel | 2.1–2.4 | PR 2 |
| 3 — CierreCajaViewModel | 3.1–3.2 | PR 2 |
| 4 — BIViewModel | 4.1–4.2 | PR 2 |
| 5 — Final verification | 5.1–5.2 | PR 2 |

**Total tasks**: 14

---

## Chained PRs

**Yes — 2 chained PRs**

| PR | Content | Lines (est.) |
|----|---------|--------------|
| PR 1 | Tasks 1.1–1.6: `ServicioExtra.fechaEntrega`, `MIGRATION_28_29`, `OptoDatabase` v29, Supabase migration, migration test | ~250 |
| PR 2 | Tasks 2.1–4.2: ViewModel changes + all ViewModel tests + `BIViewModelTest` | ~350 |

**Rationale**: PR 1 lands the schema foundation. PR 2 contains all ViewModel logic and tests — keeps each PR under 400 lines for focused review.

---

## Next Step

Ready for apply (sdd-apply). Start with PR 1 (Phase 1: Entity + Schema).

**Verification command for PR 1**:
```
./gradlew :optoapp:testDebugUnitTest --stacktrace --tests "com.example.optoapp.data.ServicioExtraMigration28To29Test"
```

**Verification command for PR 2**:
```
./gradlew :optoapp:testDebugUnitTest --stacktrace
```
