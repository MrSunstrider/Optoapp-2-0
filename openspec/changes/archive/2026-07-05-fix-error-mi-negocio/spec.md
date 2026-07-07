# Spec: Fix Error Handling in "Mi Negocio" Screen

## Purpose

This delta spec defines behavioral contracts for error handling in the `AnalisisNegocioScreen` ("Mi Negocio") data-loading flow. All requirements describe WHAT the system MUST do — no implementation details.

The base spec at `openspec/specs/analisis-negocio/spec.md` covers data schema and RPC contracts. This spec adds behavioral guarantees for error surface, deduplication, cancellation safety, and null-date resilience on the Android client.

---

## ADDED Requirements

### REQ-1: Error Deduplication

The error accumulator used in the ViewModel MUST produce at most one occurrence of each distinct error message, regardless of how many sources produce the same text.

#### Scenario: Same error from multiple paths appears once

```
GIVEN two data sources both fail with the identical error message "No se pudieron cargar los datos"
 WHEN the ViewModel collects errors from both sources
 THEN the resulting error set contains exactly one occurrence of that message
  AND the error set size equals the count of distinct error messages
```

#### Scenario: Distinct error messages are preserved

```
GIVEN three data sources each fail with different messages
 WHEN the ViewModel collects all errors
 THEN the resulting error set contains all three distinct messages
  AND no message is lost
```

**Test type**: unit (ViewModel with fake failing use cases)

---

### REQ-2: User-Facing Error Messages Are Static

Every domain-layer use case that produces a user-facing error message MUST return a static, user-friendly string. Raw exception text, JSON bodies, and `e.localizedMessage` MUST NOT appear in the error message visible to the UI.

The full exception (message + stack trace) MUST be logged via `Log.e` to Logcat for diagnostics.

#### Scenario: Supabase RPC failure shows static message

```
GIVEN the Supabase RPC `rpc_analisis_mensual` returns HTTP 400 with verbose JSON body
 WHEN `ObtenerAnalisisMensualUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the message does NOT contain "400", "JSON", "localizedMessage", or any raw exception text
  AND the full exception is written to Logcat via `Log.e`
```

#### Scenario: Network failure in deudores shows static message

```
GIVEN the Supabase RPC `rpc_deudores` throws a network exception
 WHEN `ObtenerDeudoresUseCase` catches the exception
 THEN the returned `Resource.Error` message is static text
  AND the full exception is written to Logcat via `Log.e`
```

**Test type**: unit (use case with a fake failing Supabase client + Logcat spy/verification)

---

### REQ-3: `LocalDate.parse` Is Null-Safe for Nullable RPC Fields

The `ObtenerDeudoresUseCase` MUST handle a null or missing `venta_fecha` field in the `rpc_deudores` response without throwing `DateTimeParseException`. When `venta_fecha` is null or empty, the corresponding `Deudor.ventaFecha` field MUST be set to `LocalDate.MIN` and the processing of the remaining rows MUST continue.

#### Scenario: Null venta_fecha does not crash

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = null`
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN the row is included in the result list
  AND `Deudor.ventaFecha` is set to `LocalDate.MIN` for that row
  AND the remaining rows are successfully parsed without exception
```

#### Scenario: Empty venta_fecha does not crash

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = ""` (empty string)
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN `Deudor.ventaFecha` is set to `LocalDate.MIN` for that row
  AND the remaining rows are successfully parsed without exception
```

#### Scenario: Valid venta_fecha parses normally

```
GIVEN the `rpc_deudores` response includes a row with `venta_fecha = "2026-07-01"`
 WHEN `ObtenerDeudoresUseCase` parses that row
 THEN `Deudor.ventaFecha` equals `LocalDate.of(2026, 7, 1)`
```

**Test type**: unit (use case with a fake PreloadedRpcResponse returning controlled data)

---

### REQ-4: `CancellationException` MUST Be Rethrown

The ViewModel data-loading coroutine MUST rethrow `CancellationException` instead of catching it in a generic handler. When the coroutine scope is cancelled (e.g., user navigates away), the error card MUST NOT display a spurious "Error inesperado" message, and the coroutine hierarchy MUST be properly cancelled.

#### Scenario: Navigation away does not show spurious error

```
GIVEN the AnalisisNegocioScreen is visible and loading data
 WHEN the user navigates away before data loading completes
 THEN no "Error inesperado" or similar generic error text appears in the UI
  AND the coroutine hierarchy completes cancellation without raising unhandled exceptions
```

#### Scenario: runCatching does not swallow CancellationException

```
GIVEN the ViewModel wraps the data-loading block in error handling
 WHEN a `CancellationException` is thrown inside that block
 THEN the exception is rethrown and propagates up
  AND it is NOT caught by any generic `catch (e: Exception)` or `.onFailure` handler
```

**Test type**: unit (ViewModel with `TestCoroutineDispatcher`, cancelled scope, verify no error state emitted)

---

### REQ-5: `GenerarRecomendacionesUseCase` Accepts Pre-Fetched Data

The use case MUST accept `AnalisisMensual` and `List<Deudor>` data objects as input parameters. It MUST NOT call `ObtenerAnalisisMensualUseCase` or `ObtenerDeudoresUseCase` internally. The business logic (evaluating and generating recommendations) MUST remain identical.

#### Scenario: Pre-fetched data produces same recommendations

```
GIVEN an AnalisisMensual object, a List<Deudor>, and a ConfiguracionFinancieraEntity
 WHEN `GenerarRecomendacionesUseCase.invoke(analisis, deudores, opticaId)` is called
 THEN a `Resource<List<Recomendacion>>` is returned
  AND the recommendations are computed using only the provided data (no additional RPC calls)
```

#### Scenario: Null/empty pre-fetched data returns error gracefully

```
GIVEN `analisis` is null AND `deudores` is null
 WHEN `GenerarRecomendacionesUseCase.invoke(null, null, opticaId)` is called
 THEN a `Resource.Error` is returned
  AND no RPC calls are made
```

**Test type**: unit (use case with injected data, verify with a mock that no RPC calls occur)

---

### REQ-6: ViewModel Orchestration — Data Passing

The ViewModel MUST orchestrate two parallel data calls (`obtenerAnalisisMensual`, `obtenerDeudores`), then pass their results to `generarRecomendaciones`. Errors from all three sources MUST be collected into a unique set.

#### Scenario: All calls succeed

```
GIVEN both `obtenerAnalisisMensual` and `obtenerDeudores` return `Resource.Success`
 WHEN the ViewModel loads data
 THEN `generarRecomendaciones` is called with the fetched `analisis` and `deudores` data
  AND `uiState.analisis` is set to the fetched analisis data
  AND `uiState.deudores` is set to the fetched deudores data
  AND `uiState.error` is null
```

#### Scenario: One call fails, recommendations still computed

```
GIVEN `obtenerAnalisisMensual` returns `Resource.Success` with valid data
  AND `obtenerDeudores` returns `Resource.Error`
 WHEN the ViewModel loads data
 THEN `generarRecomendaciones` is called with `analisis = null` and `deudores = null`
  AND `uiState.deudores` is an empty list
  AND `uiState.error` includes the deudores error message
  AND `uiState.analisis` is set to the fetched analisis data
```

#### Scenario: Both calls fail

```
GIVEN both `obtenerAnalisisMensual` and `obtenerDeudores` return `Resource.Error`
 WHEN the ViewModel loads data
 THEN `uiState.error` contains both error messages (once each)
  AND `uiState.analisis` and `uiState.deudores` are null/empty
  AND recommendations are not computed (or return error)
```

**Test type**: unit (ViewModel with controlled fake use cases, verify state emissions)

---

## MODIFIED Requirements

### REQ-M1: No Redundant RPC Calls from ViewModel

Two views affected by the same RPC response MUST NOT make duplicate calls to that RPC. If `generarRecomendaciones` depends on the same data as the primary data calls, the ViewModel MUST pass that data instead of letting the use case re-fetch it.

This requirement amends the implicit behavior of the existing `analisis-negocio` Android flow. The base spec remains unchanged in its data schema and RPC contracts.

#### Scenario: rpc_analisis_mensual is called at most once per load

```
GIVEN the ViewModel initiates a data load
 WHEN all data loading completes
 THEN `rpc_analisis_mensual` or its local equivalent is called exactly once
  AND `rpc_deudores` is called exactly once
```

**Test type**: integration or unit (count Supabase client invocations via mock)

---

## Out of Scope (from base spec)

- Supabase schema, RLS, or migration changes
- UI layout or Compose rendering changes
- New test coverage beyond updating existing use-case constructor calls
- Global error handling framework (e.g., interceptor or middleware)
