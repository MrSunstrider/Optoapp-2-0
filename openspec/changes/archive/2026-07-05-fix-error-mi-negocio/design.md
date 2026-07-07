# Design: Fix Error Handling in "Mi Negocio" Screen

## 1. Architecture Decisions

### AD-1: `GenerarRecomendacionesUseCase` Accepts Pre-Fetched Data

| Attribute | Decision |
|-----------|----------|
| **Problem** | The use case calls `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` internally (lines 22-26), duplicating the same RPCs the ViewModel already runs. This causes 3× error duplication and wasteful network calls. |
| **Solution** | Change the constructor to inject only `ConfiguracionFinancieraDao`. The `invoke` signature changes from `(opticaId, mes)` to `(analisis: AnalisisMensual?, deudores: List<Deudor>, opticaId: String)`. Business logic (evaluarCobrar, evaluarMejorarPrecio, etc.) remains untouched. |
| **Rationale** | Clean Architecture: a use case orchestrating business logic should not re-fetch data the caller already has. The ViewModel is the data-fetching orchestrator; the use case is a pure business-logic computation. |
| **Trade-offs** | Tests and ViewModel constructor call must update. One fewer layer of indirection for error propagation. |

### AD-2: ViewModel Orchestrates 2 Parallel Calls, Then Feeds Recomendaciones

| Attribute | Decision |
|-----------|----------|
| **Problem** | 3 parallel calls → 1 is redundant (recomendaciones duplicates analisis + deudores RPCs). Errors accumulate as `MutableList<String>` allowing duplicates. |
| **Solution** | Run 2 parallel calls (`obtenerAnalisisMensual` + `obtenerDeudores`), extract their data, pass to `generarRecomendaciones`. Collect errors into `Set<String>` and join. |
| **Rationale** | Eliminates duplicate RPCs. Set deduplicates identical messages for free. Type-safe data passing via sealed class matching. |
| **Trade-offs** | ViewModel gains a data-passing step between async blocks. Slightly more complex but more explicit. |

### AD-3: Error Messages Are Static, Full Exception Logged

| Attribute | Decision |
|-----------|----------|
| **Problem** | `e.localizedMessage` leaks verbose PostgREST JSON to users (HTTP 400, code/hint/details). |
| **Solution** | Each use case returns a static message (e.g. `"No se pudieron cargar los datos de deudores"`). Full exception (message + stack trace) logged to Logcat via `Log.e`. |
| **Rationale** | User-facing text should be meaningful and stable. Diagnostics go to Logcat where developers can read them. |

### AD-4: `CancellationException` Rethrown, Not Swallowed

| Attribute | Decision |
|-----------|----------|
| **Problem** | `runCatching` at line 84 catches all exceptions including `CancellationException`, producing spurious "Error inesperado" on navigation away. |
| **Solution** | Replace `runCatching { coroutineScope { ... } }` with `try { coroutineScope { ... } } catch (e: CancellationException) { throw e } catch (e: Exception) { ... }`. |
| **Rationale** | Kotlin coroutines contract: `CancellationException` must propagate upward to maintain cancellation hierarchy. |

### AD-5: `LocalDate.parse("")` Wrapped in Try/Catch

| Attribute | Decision |
|-----------|----------|
| **Problem** | `ObtenerDeudoresUseCase` line 64 calls `LocalDate.parse(string("venta_fecha"))` where `string()` returns `""` for null/missing fields → `DateTimeParseException`. |
| **Solution** | Wrap `LocalDate.parse()` in try/catch → on failure, set `Deudor.ventaFecha = LocalDate.MIN` and continue parsing remaining rows. |
| **Rationale** | A single bad date in one row must not crash the entire RPC result. Graceful fallback preserves data for valid rows. |

---

## 2. Component Changes

### 2.1 `domain/GenerarRecomendacionesUseCase.kt` — Signature + Constructor Change

**Before:**
```kotlin
open class GenerarRecomendacionesUseCase @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,  // REMOVE
    private val obtenerDeudores: ObtenerDeudoresUseCase,                // REMOVE
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao   // KEEP
) {
    suspend operator fun invoke(
        opticaId: String,        // REMOVE
        mes: LocalDate           // REMOVE
    ): Resource<List<Recomendacion>> {
        // Lines 22-26: re-fetches data internally → DELETE entirely
        val analisisResult = obtenerAnalisisMensual(opticaId, mes)
        if (analisisResult is Resource.Error) return Resource.Error(analisisResult.message!!)
        val deudoresResult = obtenerDeudores(opticaId)
        if (deudoresResult is Resource.Error) return Resource.Error(deudoresResult.message!!)

        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
            ?: return Resource.Error("Configuracion financiera no encontrada")

        val analisis = (analisisResult as Resource.Success).data!!
        val deudores = (deudoresResult as Resource.Success).data!!
        // ... business logic continues
    }
}
```

**After:**
```kotlin
open class GenerarRecomendacionesUseCase @Inject constructor(
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(
        analisis: AnalisisMensual?,
        deudores: List<Deudor>,
        opticaId: String
    ): Resource<List<Recomendacion>> {
        // Guard: if both inputs are missing, return error gracefully
        if (analisis == null && deudores.isEmpty()) {
            return Resource.Error("Datos insuficientes para generar recomendaciones")
        }

        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
            ?: return Resource.Error("Configuracion financiera no encontrada")

        val analisisNonNull = analisis ?: return Resource.Error(
            "No hay datos de analisis para generar recomendaciones"
        )

        // Business logic keeps same private methods — all receive data,
        // no RPC calls inside.
        val recommendations = listOfNotNull(
            evaluarCobrar(deudores, config),
            evaluarMejorarPrecio(analisisNonNull.margenPorCategoria, config),
            evaluarLiquidarStock(analisisNonNull.stockEstancado, config),
            evaluarVenderMasDe(analisisNonNull.margenPorCategoria, config),
            evaluarAlertaCaida(analisisNonNull, config),
            evaluarReducirGasto(analisisNonNull)
        )

        val sorted = recommendations.sortedWith(
            compareBy<Recomendacion> { it.prioridad.ordinal }
                .thenBy { it.tipo.ordinal }
        )

        return Resource.Success(sorted.take(5))
    }

    // private methods evaluarCobrar, evaluarMejorarPrecio, etc.
    // — UNCHANGED from current code
}
```

### 2.2 `viewmodel/AnalisisNegocioViewModel.kt` — Orchestration + Error Collection

**Before:**
```kotlin
private fun loadData(mes: LocalDate) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val opticaId = sessionManager.opticaId.first()

        val result = runCatching {                    // ← CancellationException swallowed
            coroutineScope {
                val analisisDeferred = async {
                    runCatching { obtenerAnalisisMensual(opticaId, mes) }
                        .onFailure { Log.e(TAG, "Error fetching analisis mensual", it) }
                        .getOrNull()
                }
                val deudoresDeferred = async {
                    runCatching { obtenerDeudores(opticaId) }
                        .onFailure { Log.e(TAG, "Error fetching deudores", it) }
                        .getOrNull()
                }
                val recomendacionesDeferred = async {   // ← Redundant — duplicates RPCs
                    runCatching { generarRecomendaciones(opticaId, mes) }
                        .onFailure { Log.e(TAG, "Error fetching recomendaciones", it) }
                        .getOrNull()
                }
                Triple(analisisDeferred.await(), deudoresDeferred.await(), recomendacionesDeferred.await())
            }
        }

        result.onSuccess { (analisisResult, deudoresResult, recomendacionesResult) ->
            val errors = mutableListOf<String>()       // ← Duplicates allowed
            val analisis = if (analisisResult is Resource.Success) {
                analisisResult.data
            } else {
                if (analisisResult is Resource.Error) errors.add(analisisResult.message!!)
                null
            }
            // ... same for deudores, recomendaciones
            // error = errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
        }.onFailure { e ->
            Log.e(TAG, "Unexpected error loading data", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error inesperado: ${e.localizedMessage}"  // ← leaks localizedMessage
            )
        }
    }
}
```

**After:**
```kotlin
private fun loadData(mes: LocalDate) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val opticaId = sessionManager.opticaId.first()

        try {                                            // ← Replaces runCatching
            // Stage 1: 2 parallel data fetches
            val analisisDeferred = async {
                obtenerAnalisisMensual(opticaId, mes)
            }
            val deudoresDeferred = async {
                obtenerDeudores(opticaId)
            }

            // Await both
            val analisisResult = analisisDeferred.await()
            val deudoresResult = deudoresDeferred.await()

            // Extract data
            val analisis = (analisisResult as? Resource.Success)?.data
            val deudores = (deudoresResult as? Resource.Success)?.data ?: emptyList()

            // Stage 2: pass data to recomendaciones
            val recomendacionesResult = if (analisis != null || deudores.isNotEmpty()) {
                generarRecomendaciones(analisis, deudores, opticaId)
            } else {
                Resource.Error("Datos insuficientes")
            }

            // Collect errors in a Set → deduplicates identical messages
            val errors = setOfNotNull(
                (analisisResult as? Resource.Error)?.message,
                (deudoresResult as? Resource.Error)?.message,
                (recomendacionesResult as? Resource.Error)?.message
            )

            _uiState.value = _uiState.value.copy(
                analisis = analisis,
                deudores = deudores,
                recomendaciones = (recomendacionesResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                error = errors.joinToString("; ").ifEmpty { null },
                mostrarAdvertenciaEstacionalidad = analisis?.esOffline == true
            )
        } catch (e: CancellationException) {
            throw e                                    // ← ALWAYS rethrow cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading data", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error inesperado al cargar datos"
            )
        }
    }
}
```

### 2.3 `domain/ObtenerAnalisisMensualUseCase.kt` — Sanitize Error Message

**Before (lines 36-38):**
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "Error obteniendo analisis mensual", e)
    Resource.Error("Error obteniendo analisis mensual: ${e.localizedMessage}")
}
```

**After:**
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "Error obteniendo analisis mensual", e)
    Resource.Error("No se pudieron cargar los datos del mes")
}
```

### 2.4 `domain/ObtenerDeudoresUseCase.kt` — Sanitize Error + Fix LocalDate.parse

**Before (lines 40-44, 64):**
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "Error obteniendo deudores", e)
    Resource.Error("Error obteniendo deudores: ${e.localizedMessage}")
}
...
ventaFecha = LocalDate.parse(string("venta_fecha"))  // ← crashes on null/empty
```

**After:**
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "Error obteniendo deudores", e)
    Resource.Error("No se pudieron cargar los datos de deudores")
}
...
ventaFecha = try {
    val raw = string("venta_fecha")
    if (raw.isBlank()) LocalDate.MIN else LocalDate.parse(raw)
} catch (e: DateTimeParseException) {
    Log.w(TAG, "Invalid venta_fecha for deudor row, using LocalDate.MIN", e)
    LocalDate.MIN
}
```

Also add `import java.time.format.DateTimeParseException` at the top.

### 2.5 `ui/screens/AnalisisNegocioScreen.kt` — No Changes

The screen renders `uiState.error` in a red Card with a "Reintentar" button. After the fix, the error string will contain static deduplicated messages instead of verbose JSON — the rendering logic is unchanged.

### 2.6 `data/Resource.kt` — No Changes

The sealed class stays as-is.

---

## 3. Error Flow

### Before (3 parallel calls → 3 errors → duplicate leakage)

```
Supabase RPC: rpc_analisis_mensual  ──(HTTP 400)──▶  ObtenerAnalisisMensualUseCase
                                                       └─ Resource.Error("Error obteniendo...: HTTP 400 - {"code":"P0001"}")
                                                          ↓
                                                     ViewModel errors.add(msg1)

Supabase RPC: rpc_deudores          ──(HTTP 400)──▶  ObtenerDeudoresUseCase
                                                       └─ Resource.Error("Error obteniendo...: HTTP 400 - {...}")
                                                          ↓
                                                     ViewModel errors.add(msg2)

                                                     GenerarRecomendacionesUseCase
                                                       ├─ llamada a obtenerAnalisisMensual → Resource.Error(msg1)
                                                       ├─ llamada a obtenerDeudores       → Resource.Error(msg2)
                                                       └─ retorna Resource.Error(msg1)     ← SAME as msg1
                                                                                              ↓
                                                     ViewModel errors.add(msg1)              ← DUPLICATE

                              errors = [msg1, msg2, msg1] → "msg1; msg2; msg1" (3 errors, 1 duplicate)
```

### After (2 parallel calls → data passing → Set dedup)

```
Stage 1 (parallel):
  Supabase RPC: rpc_analisis_mensual  ──(HTTP 400)──▶  ObtenerAnalisisMensualUseCase
                                                         └─ Resource.Error("No se pudieron cargar los datos del mes")
                                                            ↓
                                                       ViewModel analisisResult = Resource.Error(msg1)

  Supabase RPC: rpc_deudores          ──(HTTP 400)──▶  ObtenerDeudoresUseCase
                                                         └─ Resource.Error("No se pudieron cargar los datos de deudores")
                                                            ↓
                                                       ViewModel deudoresResult = Resource.Error(msg2)

Stage 2 (sequential):
  analisis = null, deudores = emptyList()

  generarRecomendaciones(null, emptyList(), opticaId)
    └─ Resource.Error("Datos insuficientes para generar recomendaciones")  ← NEW distinct message
                                                                              ↓
  ViewModel errors = setOfNotNull(msg1, msg2, msg3)
                   = ["No se pudieron cargar los datos del mes",
                      "No se pudieron cargar los datos de deudores",
                      "Datos insuficientes para generar recomendaciones"]

  error = "No se pudieron cargar los datos del mes; No se pudieron cargar los datos de deudores; Datos insuficientes para generar recomendaciones"
```

**Key improvement**: If only `rpc_analisis_mensual` fails and `rpc_deudores` succeeds, the before scenario produces `[msg1, msg1]` (duplicated), while after produces `[msg1]` (no duplication because `generarRecomendaciones` receives `deudores` successfully and only fails if `analisis` is null).

### CancellationException Flow

```
Before:
  viewModelScope.launch { runCatching { coroutineScope { ... } } }
    └─ User navigates away → coroutine cancelled → CancellationException
       └─ runCatching catches it → .onFailure → "Error inesperado: ..."  ← BAD

After:
  viewModelScope.launch {
      try { coroutineScope { async { ... } ... } }
      catch (e: CancellationException) { throw e }   ← RETHROWN
      catch (e: Exception) { ... }
  }
    └─ User navigates away → CancellationException → rethrown → properly cancels parent
       └─ No error state emitted                      ← GOOD
```

---

## 4. ViewModel Orchestration Details

### State Machine

```
IDLE ──loadData()──▶ LOADING ──▶ COMPLETE
                                  ├── All success   → data populated, error = null
                                  ├── Partial fail   → data for successful sources, error = Set.join
                                  └── All fail       → data = null, error = Set.join
```

### Execution Order

1. Set `isLoading = true`, `error = null`
2. `val analisisDeferred = async { obtenerAnalisisMensual(opticaId, mes) }` (parallel)
3. `val deudoresDeferred = async { obtenerDeudores(opticaId) }` (parallel)
4. `await()` both — blocks until both complete
5. Extract data via `as? Resource.Success` — if error, remains `null` / `emptyList()`
6. `generarRecomendaciones(analisis, deudores, opticaId)` — sequential after both results ready
7. Build `Set<String>` from all 3 results' error messages
8. Emit final UI state

### Error Deduplication Contract

- Each distinct error message appears at most once in the final `error` string
- If the same Supabase RPC fails, the message appears once (not 2× or 3×)
- If different sources produce different messages, all are preserved

---

## 5. Test Impact

### 5.1 `GenerarRecomendacionesUseCaseTest.kt` Requires Constructor Change

| Change | Detail |
|--------|--------|
| **Constructor** | Remove `analisisUseCase` and `deudoresUseCase` fields from class, from `setUp()`, and from `mockDeps()`. |
| **invoke calls** | All `useCase.invoke("optica1", testMes)` → `useCase.invoke(analisis, deudores, "optica1")` |
| **mockDeps helper** | Remove `coEvery` for `analisisUseCase` and `deudoresUseCase`. Keep configDao setup. Change to accept `AnalisisMensual` and `List<Deudor>` directly. |
| **Tests to remove** | `invoke_whenAnalisisError_returnsError` (lines 346-351) — this tested error propagation from inner use cases, which no longer exists. |
| **Tests to add** | `invoke_whenAnalisisIsNull_returnsError` — verify guard clause. `invoke_whenBothNull_returnsError` — verify empty inputs produce error. |

### 5.2 `AnalisisNegocioViewModelTest.kt` Requires Mock + Assertion Changes

| Change | Detail |
|--------|--------|
| **primeUseCases** | Change `coEvery { generarRecomendaciones(opticaId, any()) }` → `coEvery { generarRecomendaciones(any(), any(), any()) }` |
| **Error tests** | Update `all use cases failing` test: now errors are collected in a Set, and `recomendaciones` error is only "Datos insuficientes" when analisis+deudores both fail. The error set contains 3 distinct messages. |
| **Partial fail test** | `error state when analisis use case fails` — after the fix, when `analisis` fails and `deudores` succeeds, `generarRecomendaciones` receives `(null, deudoresList, opticaId)` and returns `Resource.Error("No hay datos de analisis...")`. So the error string will contain BOTH the analisis error AND the recomendaciones error. **Assertions must be updated.** |
| **Cancellation test** | The existing `isLoading is false after successful load` test already verifies base behavior. A new test should verify that a cancelled scope does not emit error state. |

### 5.3 `ObtenerDeudoresUseCaseTest.kt` — New Tests

| Change | Detail |
|--------|--------|
| **Error message assertion** | `offline_ioException_returnsError` and `unexpectedError_returnsResourceError` — update expected message from dynamic `e.localizedMessage` to static text. |
| **New test** | `null_ventaFecha_usesLocalDateMin` — add JSON with `"venta_fecha"` missing or null, verify `Deudor.ventaFecha == LocalDate.MIN`. |

### 5.4 `ObtenerAnalisisMensualUseCaseTest.kt` — Error Message Assertions

| Change | Detail |
|--------|--------|
| **Error message** | `unexpectedError_returnsResourceError` — update expected message to static text (currently doesn't assert message content, but should verify it's not dynamic). |

---

## 6. Migration Path for Existing Tests

### Step-by-step for `GenerarRecomendacionesUseCaseTest.kt`:

1. Remove `private val analisisUseCase: ObtenerAnalisisMensualUseCase = mockk(relaxed = true)` (line 23)
2. Remove `private val deudoresUseCase: ObtenerDeudoresUseCase = mockk(relaxed = true)` (line 24)
3. Change `setUp()` (line 42) → `useCase = GenerarRecomendacionesUseCase(configDao)`
4. Change `mockDeps()` (lines 95-103) → accept `analisis: AnalisisMensual?` and `deudores: List<Deudor>` as parameters, remove coEvery for use cases, keep configDao
5. Replace all `useCase.invoke("optica1", testMes)` with `useCase.invoke(analisis, deudores, "optica1")`
6. Remove test `invoke_whenAnalisisError_returnsError` (lines 346-351)
7. Add two new tests (null guard clauses)

### Step-by-step for `AnalisisNegocioViewModelTest.kt`:

1. Change `primeUseCases` (line 125): remove `coEvery { generarRecomendaciones(opticaId, any()) }`; replace with `coEvery { generarRecomendaciones(any<AnalisisMensal?>(), any<List<Deudor>>(), any()) } returns recomendaciones`
2. Update `error state when analisis use case fails` test (lines 197-212): now `analisis=Error`, `deudores=Success`, `recomendaciones` receives `(null, deudoresData)` → returns `Resource.Error("No hay datos de analisis...")`. Expected error = `"Error en analisis mensual; No hay datos de analisis para generar recomendaciones"`.
3. Update `all use cases failing` test (lines 232-247): now `analisis=Error("Error A")`, `deudores=Error("Error B")`, `generarRecomendaciones(null, [], opticaId)` returns `"Datos insuficientes para generar recomendaciones"`. Expected error includes all 3.
4. Consider adding a cancellation test with `TestCoroutineDispatcher` + cancelled scope.

---

## 7. Caller Audit

| Caller | Location | Impact |
|--------|----------|--------|
| `AnalisisNegocioViewModel` | `viewmodel/AnalisisNegocioViewModel.kt:102` | **Changes** — new invoke signature |
| `GenerarRecomendacionesUseCaseTest` | `test/.../GenerarRecomendacionesUseCaseTest.kt` | **Changes** — all invoke calls updated |
| `AnalisisNegocioViewModelTest` | `test/.../AnalisisNegocioViewModelTest.kt` | **Changes** — mock signature + assertions updated |

No other callers found in the codebase. **Zero external breakage risk.**

---

## 8. Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| **Regression**: partial-fail behavior changes (e.g. analisis fails but deudores succeeds → recomendaciones now also errors instead of silently skipping) | This is INTENTIONAL per REQ-5/REQ-6 spec scenarios. The new behavior is more predictable: if analisis is null, recomendaciones cannot compute `ALERTA_CAIDA`, `MEJORAR_PRECIO`, etc. The error surface explicitly says "No hay datos de analisis". Tests updated to match. |
| **Null guard too strict**: `analisis == null && deudores.isEmpty()` blocks recomendaciones when only one source has data | The guard is tiered: if both are empty → `"Datos insuficientes"`. If only analisis is null → `"No hay datos de analisis"`. If only deudores is empty but analisis has data → recomienda normally (deudores empty is valid — `evaluarCobrar` returns null, others proceed). |
| **deudores field unused in UI** | The field is kept in UI state per current contract. The exploration flagged it as unused but removal is out of scope for this change. |

---

## 9. Verification Checklist

- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes
- [ ] `rpc_analisis_mensual` called at most once per `loadData()` invocation
- [ ] `rpc_deudores` called at most once per `loadData()` invocation
- [ ] Error card shows each distinct message only once
- [ ] Error messages contain no raw JSON, no `localizedMessage`, no HTTP codes
- [ ] Navigating away from screen during loading does not show "Error inesperado"
- [ ] Null `venta_fecha` in deudores API response produces `LocalDate.MIN` without crash
- [ ] `GenerarRecomendacionesUseCase` has no references to `ObtenerAnalisisMensualUseCase` or `ObtenerDeudoresUseCase`
- [ ] Existing `GenerarRecomendacionesUseCaseTest` all pass with new signature
- [ ] Existing `AnalisisNegocioViewModelTest` all pass with updated mocks and assertions
