# Tasks: Fix Servicios Extra UI Rendering and Sync Normalization

## Overview
Six targeted fixes across UI rendering, PDF generation, and sync normalization. Tasks are written in TDD style: RED tests first, then GREEN implementations.

---

## Phase 1: Sync Normalization (Foundation)

### Phase 1.1: PagoRemoto.toEntity() metodoPago normalization
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasDto.kt` - PagoRemoto.toEntity() method

**Test Tasks**:

1. **Task 1.1.1: Verify existing ServicioRemoto normalization**
   - [x] **Red**: Create unit test for `ServicioRemoto.toEntity()` - ensure it calls `.remotoServicioExtraMetodoToLocal()`
   - [x] **Green**: Verify current implementation already does this (from Spec Verification)
   - [x] **Test**: `SyncFinanzasDtoNormalizationTest.servicioRemoto_withSinEspecificar_normalizesToEmptyString`

2. **Task 1.1.2: Identify pago missing normalization**
   - [x] **Red**: Create test showing `PagoRemoto.toEntity()` copies `metodoPago` verbatim (un-normalized)
   - [x] **Green**: Update `PagoRemoto.toEntity()` line 119 to use `.remotoServicioExtraMetodoToLocal()` like `ServicioRemoto`
   - [x] **Test**: `SyncFinanzasDtoNormalizationTest.pagoRemoto_withSinEspecificar_normalizesToEmptyString`

3. **Task 1.1.3: Normalize equals normalization**
   - [x] **Red**: Test that both `PagoRemoto` and `ServicioRemoto` normalization behave identically
   - [x] **Green**: Ensure both use the same `remotoServicioExtraMetodoToLocal()` function
   - [x] **Test**: `SyncFinanzasDtoNormalizationTest.pagoAndServicio_normalizeIdentically`

**Key Files Changed**: `SyncFinanzasDto.kt`
**Lines Changed**: ~1 (normalization in PagoRemoto.toEntity())

---

## Phase 2: UI Fixes (TransactionItem and CierreCajaScreen)

### Phase 2.1: TransactionItem.kt 3-way label logic
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/ui/components/cierre-caja/TransactionItem.kt`

**Test Tasks**:

1. **Task 2.1.1: Orphan pago label should be "Pago"**
   - [x] **Red**: Create test where `dispensacionId == null` AND `servicioExtraId == null` → label should be "Pago"
   - [x] **Green**: Fix TransactionItem.kt line 26 - implement 3-way when: dispensacion → "Dispensación", servicioExtra → "Servicio Extra", else → "Pago"
   - [x] **Test**: `TransactionItemTest.bothIdsNull_labelIsPago`

2. **Task 2.1.2: Servicio extra pago label**
   - [x] **Red**: Test where `servicioExtraId != null` AND `dispensacionId == null` → label "Servicio Extra"
   - [x] **Green**: Already covered in Task 2.1.1 implementation
   - [x] **Test**: `TransactionItemTest.servicioExtraIdSet_labelIsServicioExtra`

3. **Task 2.1.3: Dispensación pago label**
   - [x] **Red**: Test where `dispensacionId != null` → label "Dispensación"
   - [x] **Green**: Already covered in Task 2.1.1 implementation
   - [x] **Test**: `TransactionItemTest.dispensacionIdSet_labelIsDispensacion`

**Key Files Changed**: `TransactionItem.kt`
**Lines Changed**: ~1 (label logic)

### Phase 2.2: CierreCajaScreen.kt servicios extra section
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/ui/screens/CierreCajaScreen.kt`

**Test Tasks**:

1. **Task 2.2.1: Verify servicios extra in UIState**
   - [x] **Red**: Create test confirming `CierreCajaUiState` has `serviciosExtraHoy: List<ServicioExtra>` and `totalServiciosExtra: Double`
   - [x] **Green**: Already present in code (verified in Phase 1.1)

2. **Task 2.2.2: Render servicios extra section**
   - [x] **Red**: Test that `CierreCajaScreen` shows servicios extra section with count and total
   - [x] **Green**: Add servicios extra section in CierreCajaScreen after dispensaciones breakdown (below line 218)

3. **Task 2.2.3: Update UI display**
   - [x] **Red**: Test `CierreCajaScreen` displays servicios extra count, items, and total
   - [x] **Green**: Implement UI section similar to dispensaciones structure

**Key Files Changed**: `CierreCajaScreen.kt`
**Lines Changed**: ~20 (new UI section)

---

## Phase 3: Reportes Fixes (Detail List and PDF)

### Phase 3.1: ReportesScreen.kt collect servicios extra
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/ui/screens/ReportesScreen.kt`

**Test Tasks**:

1. **Task 3.1.1: Add servicios extra collection**
   - [x] **Red**: Test that `ReportesScreen` currently only collects `dispensaciones`
   - [x] **Green**: Add `val allServiciosDelPeriodo by viewModel.allServiciosDelPeriodo.collectAsState()` (line 38)

2. **Task 3.1.2: Merge lists chronologically**
   - [x] **Red**: Create test showing `ReportesViewModel` already exposes `allServiciosDelPeriodo` (from exploration)
   - [x] **Green**: Implement merge logic in ReportesScreen (not ViewModel) - combine dispensaciones and servicios for LazyColumn

3. **Task 3.1.3: Update detail LazyColumn**
   - [x] **Red**: Test that `LazyColumn` renders only dispensaciones currently
   - [x] **Green**: Modify LazyColumn to show both dispensaciones and servicios extra items

**Key Files Changed**: `ReportesScreen.kt`
**Lines Changed**: ~15 (collection + rendering)

### Phase 3.2: ReporteFinancieroPdfGenerator.kt servicios extra support
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/util/ReporteFinancieroPdfGenerator.kt`

**Test Tasks**:

1. **Task 3.2.1: Add servicios extra parameter**
   - [x] **Red**: Test PDF generator function signature only accepts `dispensaciones`
   - [x] **Green**: Add `serviciosExtra: List<ServicioExtra>` parameter to `generate()` function (line 20)

2. **Task 3.2.2: Update reporte call site**
   - [x] **Red**: Test `ReportesScreen.kt:85-92` PDF generation only uses dispensaciones
   - [x] **Green**: Update ReportesScreen to pass `serviciosExtra` when calling PDF generator

3. **Task 3.2.3: Render servicios extra in PDF**
   - [x] **Red**: Test PDF detail section doesn't include servicios extra rows
   - [x] **Green**: Add servicios extra rows in PDF generator rendering logic (lines 96-105)

**Key Files Changed**: `ReporteFinancieroPdfGenerator.kt`, `ReportesScreen.kt`
**Lines Changed**: ~15 (parameters + rendering)

---

## Phase 4: ViewModel Normalization (getTotalesPorMetodo)

### Phase 4.1: CierreCajaViewModel.kt normalize before group
**RED TESTS FIRST**

**Location**: `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt`

**Test Tasks**:

1. **Task 4.1.1: Verify raw metodoPago in grouping**
   - [x] **Red**: Create test showing `getTotalesPorMetodo()` uses raw `it.metodoPago` for grouping (line 151)
   - [x] **Green**: Update `getTotalesPorMetodo()` to normalize `metodoPago` before grouping
   - [x] **Test**: `CierreCajaViewModelTest.getTotalesPorMetodo_groupsSinEspecificarWithEmptyString`

2. **Task 4.1.2: Verify normalization function exists**
   - [x] **Red**: Test confirm `remotoServicioExtraMetodoToLocal()` function exists in SyncFinanzasDto (lines 245-246)
   - [x] **Green**: Use this function or equivalent normalization in `getTotalesPorMetodo()`
   - [x] **Test**: `SyncFinanzasDtoNormalizationTest.remotoServicioExtraMetodoToLocal_extension_mapsSinEspecificar`

3. **Task 4.1.3: Test consistent grouping results**
   - [x] **Red**: Test that "efectivo", "Efectivo", "EFECTIVO" should all group as "Efectivo"
   - [x] **Green**: Implement normalization to ensure consistent grouping results
   - [x] **Test**: `CierreCajaViewModelTest.getTotalesPorMetodo_groupsSinEspecificarWithEmptyString`

**Key Files Changed**: `CierreCajaViewModel.kt`
**Lines Changed**: ~2 (normalization before groupBy)

---

## Phase 5: Final Verification

### Phase 5.1: Run full test suite
**RED TESTS FIRST**

**Test Tasks**:

1. **Task 5.1.1: Execute unit tests**
   - [x] **Red**: Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` and verify all tests pass
   - [x] **Green**: If any tests fail, trace back through the 4 phases to fix

2. **Task 5.1.2: Verify coverage**
   - [x] **Red**: Run `./gradlew :optoapp:jacocoTestReport` and check if coverage meets minimum (5%)
   - [x] **Green**: Ensure tests cover all modified code paths

3. **Task 5.1.3: Build APK**
   - [x] **Red**: Run `./gradlew :optoapp:assembleDebug` and verify build success
   - [x] **Green**: Produce debug APK successfully

**Key Actions**: Test execution, build verification
**Risk Mitigation**: Each phase includes failing tests before fixes - no reliance on passing tests mid-implementation

---

## Implementation Summary

**Total Phase Lines Changed**: ~100-120 lines across 6 files
- SyncFinanzasDto.kt: 1 line (normalization)
- TransactionItem.kt: 1 line (label logic)
- CierreCajaScreen.kt: 20+ lines (UI section)
- ReportesScreen.kt: 15+ lines (collection + PDF params)
- ReporteFinancieroPdfGenerator.kt: 10+ lines (rendering + params)
- CierreCajaViewModel.kt: 2 lines (normalization)

**TDD Approach**: All RED tests must pass before GREEN implementations are written - ensures foundational fixes are in place before UI changes build on them.