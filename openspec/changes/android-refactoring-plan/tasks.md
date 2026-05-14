# tasks.md

## 6.3.x — DispensacionStockHelper (A12)

- [x] 6.3.1. Analizar la lógica de ajuste de stock en NuevaDispensacionScreen
- [x] 6.3.2. Crear DispensacionStockHelper.kt con métodos adjustStock, registrarMovimiento, adjustStockAndRegistrarMovimiento
- [x] 6.3.3. 11 characterization tests en DispensacionStockHelperTest.kt
- [x] 6.3.4. Mover lógica de stock a DispensacionStockHelper
- [x] 6.3.5. Inyectar DispensacionStockHelper en DispensacionViewModel (constructor injection + characterization test)
- [x] 6.3.6. Tests unitarios con fakes (FakeMonturaDao, FakeMonturaMovimientoDao)

## 8.0.x — SecurityManager characterization tests

- [x] 8.0.1. Crear `SecurityManagerMigrationCharacterizationTest.kt` (ya existía de batch anterior)
- [x] 8.0.2. Test: migratePinHasBeenSet() con legacy PIN → migración correcta
- [x] 8.0.3. Test: "123456" actual → comportamiento actual
- [x] 8.0.4. Ejecutar tests — 14/15 pass (1 pre-existing failure: `ISecurityManager tiene metodo getUserPin`)

### 8.1.x — Simplificación de migratePinHasBeenSet (ya completado)

- [x] migratePinHasBeenSet() simplificado, caso especial "123456" eliminado

## Phase 1: Critical Tests (New)

- [x] 1.1 RED: Write PostSaveSyncSchedulerTest.kt — debounce + session gating
- [x] 1.2 GREEN: Implement minimal SyncGate interface + fake for testing
- [x] 1.3 RED: Write SubscriptionViewModelTest.kt — tier, canAddPaciente flows
- [x] 1.4 GREEN: Make VM testable with fake SubscriptionManager
- [x] 1.5 RED: Write SubscriptionManagerTest.kt — tier resolution, dev override
- [x] 1.6 GREEN: Add constructor injection for testability

## 8.2.x — SecurityManager migration tests con fakes

- [x] 8.2.1. Crear fakes: `FakeDataStore.kt`, `FakeSharedPreferences.kt`
- [x] 8.2.2. Test: migración sin datos legacy → no hace nada
- [x] 8.2.3. Test: migración con datos legacy → convierte correctamente
- [x] 8.2.4. Test: PIN validation después de migración → funciona
- [x] 8.2.5. Ejecutar tests — 3/3 nuevos tests PASS + 21/21 existentes sin regresión

## 2.5.x — OperacionHoyViewModel characterization tests (RED)

- [x] 2.5.1. Analizar `OperacionHoyViewModel.kt` actual — código ya usa `async+await()` paralelo (refactor pre-existente)
- [x] 2.5.2. Escribir test: verificar que 5 queries se lanzan → `operacionHoyUiState_holdsDataFromAllFiveQuerySources` verifica contrato de datos para 5 fuentes
- [x] 2.5.3. Escribir test: todas las fuentes success → `operacionHoyUiState_combinesAllSourcesWithCorrectFormulas`
- [x] 2.5.4. Escribir test: partial failure → `operacionHoyUiState_partialData_showsAvailableFieldsWithDefaultsForMissing`, `operacionHoyUiState_partialDataWithOnlyStockInfo`
- [x] 2.5.5. Ejecutar tests — 9/9 tests PASS

### GREEN: Refactor to parallel async+awaitAll (A11) — ya implementado en código actual

- [x] Reemplazar `trigger.flatMapLatest { ... combine(5 flows) }` con `async + awaitAll` en viewModelScope.launch
- [x] Estado se publica con `_uiState.value = OperacionHoyUiState(...)` en vez de combine
- [x] Cada query independiente corre en su propio async
- [x] Partial failure: resultados exitosos se muestran, errores se loggean

## Phase 2: Pure Function Extractions from EvaluacionViewModel

- [x] **2.1 RED**: Characterization test for `DipParser.parseDipOrDnp` + `DipParser.formatDipForUi` (17 tests)
- [x] **2.2 MOVE**: Extract `parseDipOrDnp`, `formatDipForUi`, `DipParseResult` → `dip/DipParser.kt`
- [x] **2.3 GREEN**: Delegate VM calls to DipParser, verify tests pass (11 existing + 17 new = 28/28)
- [x] **2.4 RED**: Characterization tests for `DiagnosticoCalculator.parseRefraction`, `calcularDiagnostico`, `parseSnellenToLogMar` (37 tests)
- [x] **2.5 MOVE**: Extract `parseRefraction`, `calcularDiagnostico`, `parseSnellenToLogMar` → `diagnostico/DiagnosticoCalculator.kt`
- [x] **2.6 GREEN**: Update VM imports to delegate to DiagnosticoCalculator, verify full suite — 466/467 pass (1 pre-existing)

