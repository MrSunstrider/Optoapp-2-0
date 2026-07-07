# Exploration: Fix Error Handling in "Mi Negocio" Screen

## Current State

### Architecture Overview

The "Mi Negocio" screen (`AnalisisNegocioScreen.kt`) is powered by `AnalisisNegocioViewModel`, which loads three data sources in parallel via `coroutineScope { async { ... } }`:

1. **`obtenerAnalisisMensual(opticaId, mes)`** — calls Supabase RPC `rpc_analisis_mensual`
2. **`obtenerDeudores(opticaId)`** — calls Supabase RPC `rpc_deudores`
3. **`generarRecomendaciones(opticaId, mes)`** — **composite use case** that internally calls both `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase`

### Error Flow (RPC → UseCase → ViewModel → UI)

```
Supabase RPC fails
  → Postgrest throws exception with verbose JSON (code, details, hint, message)
  → UseCase catches Exception, returns Resource.Error("prefix: ${e.localizedMessage}")
  → ViewModel collects Resource.Error.message into errors list
  → errors.joinToString("; ")
  → UI renders the concatenated string in a red Card
```

### Affected Files

| File | Role |
|------|------|
| `optoapp/.../viewmodel/AnalisisNegocioViewModel.kt` | Orchestrates 3 parallel calls, collects errors |
| `optoapp/.../ui/screens/AnalisisNegocioScreen.kt` | Renders error string in `AlertRed` Card (line 101-116) |
| `optoapp/.../domain/ObtenerAnalisisMensualUseCase.kt` | RPC call, wraps error with `e.localizedMessage` (line 38) |
| `optoapp/.../domain/ObtenerDeudoresUseCase.kt` | RPC call, wraps error with `e.localizedMessage` (line 43), unsafe `LocalDate.parse()` (line 64) |
| `optoapp/.../domain/GenerarRecomendacionesUseCase.kt` | Internally re-calls both use cases (lines 22-26), propagates their errors verbatim |
| `optoapp/.../data/Resource.kt` | Sealed class: `Success<T>`, `Error<T>(message)`, `Loading<T>` |

## Problems Identified

### 1. Error Duplication x3 (Root Cause)

`GenerarRecomendacionesUseCase` (lines 22-26) calls both `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` internally. Meanwhile, the ViewModel also calls all three use cases independently in parallel.

When an RPC fails, the error text propagates through multiple paths:

```
ViewModel.analisisDeferred → ObtenerAnalisisMensualUseCase → Resource.Error(msg1)
ViewModel.deudoresDeferred → ObtenerDeudoresUseCase        → Resource.Error(msg2)
ViewModel.recomendacionesDeferred → GenerarRecomendacionesUseCase
  → internally calls ObtenerAnalisisMensualUseCase → Resource.Error(msg1)  // DUPLICATE
  → returns Resource.Error(msg1)                                           // SAME as analisisDeferred
```

Result: `errors = [msg1, msg2, msg1]` → joined as `"msg1; msg2; msg1"`.

If only one RPC fails (e.g., `rpc_analisis_mensual`), the text still appears twice: once from the direct call and once from `generarRecomendaciones`.

### 2. `e.localizedMessage` Leaked to UI

Each use case interpolates `e.localizedMessage` into the user-facing error string:

- `ObtenerAnalisisMensualUseCase` line 38: `"Error obteniendo analisis mensual: ${e.localizedMessage}"`
- `ObtenerDeudoresUseCase` line 43: `"Error obteniendo deudores: ${e.localizedMessage}"`

Supabase/PostgREST errors produce verbose JSON like:
```
HTTP Error: 400 - {"code":"P0001","details":"...","hint":"...","message":"..."}
```

This raw JSON is meaningless to end users and makes the error card extremely large.

### 3. Unsafe `LocalDate.parse()` in `ObtenerDeudoresUseCase.kt:64`

```kotlin
ventaFecha = LocalDate.parse(string("venta_fecha"))
```

`string("venta_fecha")` returns `""` when the field is null or missing. `LocalDate.parse("")` throws `DateTimeParseException`. This is caught by the generic `catch (e: Exception)` block, but it's fragile — a single bad date in any deudor record fails the entire RPC call result.

### 4. `CancellationException` Swallowed by `runCatching`

`AnalisisNegocioViewModel.kt` line 84:
```kotlin
val result = runCatching { coroutineScope { ... } }
```

`runCatching` catches all exceptions including `CancellationException`. When the coroutine scope is cancelled (e.g., user navigates away), `CancellationException` should be rethrown to properly cancel the coroutine hierarchy. Instead, it falls into the `.onFailure` block at line 145 and shows "Error inesperado: ..." to the user.

## Approaches

### Approach 1: "Stop the Bleeding" — Error Surface Fix Only

**Effort: Low** | **Risk: Low** | **Root cause fixed: No**

#### Changes:
1. **Deduplicate errors** in ViewModel: use a `LinkedHashSet` instead of `MutableList` when collecting errors, so identical messages appear only once.
2. **Sanitize error messages**: Replace `e.localizedMessage` interpolation with static, user-friendly messages (e.g., `"No se pudieron cargar los datos. Verifica tu conexión."`). Log the full error server-side.
3. **Fix `CancellationException`**: Replace `runCatching` with explicit `try/catch` that rethrows `CancellationException`.
4. **Fix `LocalDate.parse()`**: Wrap in try/catch with a fallback default (e.g., `LocalDate.MIN` or skip the record).

#### Pros:
- Minimal changes, low risk of regression
- Quick to implement and test
- Fixes the visible symptom immediately

#### Cons:
- Does NOT fix the root cause (duplicate RPC calls)
- `GenerarRecomendacionesUseCase` still makes redundant network calls — wasted bandwidth and latency
- The ViewModel still waits for 3+ calls where 2 would suffice with proper restructuring

---

### Approach 2: "Fix the Architecture" — Restructure + Sanitize (Recommended)

**Effort: Medium** | **Risk: Low-Medium** | **Root cause fixed: Yes**

#### Changes:
1. **Restructure `GenerarRecomendacionesUseCase`**: Change `invoke(opticaId, mes)` to `invoke(analisis: AnalisisMensual, deudores: List<Deudor>, config: ConfiguracionFinancieraEntity)` — accept already-fetched data instead of re-fetching internally. Remove the `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` dependencies from its constructor.
2. **ViewModel parallel calls**: Make all 3 calls in parallel, then pass `analisis` and `deudores` results to `generarRecomendaciones`. No duplicated RPC calls.
3. **Sanitize error messages**: Same as Approach 1 — use static user-friendly messages, log full errors server-side.
4. **Fix `CancellationException`**: Same as Approach 1 — rethrow in ViewModel.
5. **Fix `LocalDate.parse()`**: Same as Approach 1 — try/catch with graceful fallback.

#### Pros:
- Fixes the ROOT CAUSE: no duplicate RPC calls
- Removes wasted network bandwidth (1 fewer RPC for analisis mensual, 1 fewer for deudores)
- Cleaner dependency graph — `GenerarRecomendacionesUseCase` becomes a pure business logic orchestrator
- Better separation of concerns: ViewModel controls data fetching, use case controls business logic
- All the benefits of Approach 1 are included

#### Cons:
- Changes the public API of `GenerarRecomendacionesUseCase` — tests and any other callers need updating
- ViewModel becomes slightly more complex (must manage passing data between calls)
- Medium effort — touches more files than Approach 1

#### Detailed Design Sketch

```kotlin
// GenerarRecomendacionesUseCase — new signature
class GenerarRecomendacionesUseCase @Inject constructor(
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(
        analisis: AnalisisMensual,
        deudores: List<Deudor>,
        opticaId: String
    ): Resource<List<Recomendacion>> {
        val config = configuracionFinancieraDao.getByOpticaIdOnce(opticaId)
            ?: return Resource.Error("Configuración financiera no encontrada")
        // ... same business logic, no RPC calls
    }
}
```

```kotlin
// ViewModel — restructured parallel calls
private fun loadData(mes: LocalDate) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val opticaId = sessionManager.opticaId.first()

        try {
            val analisisDeferred = async { obtenerAnalisisMensual(opticaId, mes) }
            val deudoresDeferred = async { obtenerDeudores(opticaId) }

            val (analisisResult, deudoresResult) = coroutineScope {
                awaitAll(analisisDeferred, deudoresDeferred)
            }

            val analisis = (analisisResult as? Resource.Success)?.data
            val deudores = (deudoresResult as? Resource.Success)?.data

            val recomendacionesResult = if (analisis != null && deudores != null) {
                generarRecomendaciones(analisis, deudores, opticaId)
            } else {
                Resource.Error(null)
            }

            val errors = setOfNotNull(
                (analisisResult as? Resource.Error)?.message,
                (deudoresResult as? Resource.Error)?.message,
                (recomendacionesResult as? Resource.Error)?.message
            )

            _uiState.value = _uiState.value.copy(
                analisis = analisis,
                deudores = deudores ?: emptyList(),
                recomendaciones = (recomendacionesResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                error = errors.joinToString("; ").ifEmpty { null },
                mostrarAdvertenciaEstacionalidad = analisis?.esOffline == true
            )
        } catch (e: CancellationException) {
            throw e  // always rethrow cancellation
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

## Recommendation

**Approach 2: "Fix the Architecture"** — recommended.

Approach 1 only masks the symptom (duplicated + verbose error text) without addressing the root cause. The duplicate RPC calls are wasteful and the architectural coupling between `GenerarRecomendacionesUseCase` and the primitive use cases is incorrect for Clean Architecture: a composite use case should orchestrate business logic, not re-fetch data that the caller already has.

Approach 2 fixes both the symptom AND the root cause with reasonable effort (Medium). The risk is low because:
- The business logic in `GenerarRecomendacionesUseCase` (evaluarCobrar, evaluarMejorarPrecio, etc.) stays unchanged — only the inputs change.
- Error message sanitization is a localized change in each use case.
- `CancellationException` fix follows Kotlin coroutines best practices.
- `LocalDate.parse()` fix prevents a crash-on-bad-data scenario.

## Risks

- **Test coverage**: If tests exist for `GenerarRecomendacionesUseCase`, they need updating for the new signature. If the ViewModel has tests, they also need updating. Check before starting.
- **Other callers**: Search for other usages of `GenerarRecomendacionesUseCase` to ensure no breakage.
- **Regression in error behavior**: Existing behavior suppresses data when `analisis` or `deudores` fails (shows null). This must be preserved or intentionally changed.
- **`deudores` unused in UI**: `uiState.deudores` is held in state but not rendered in `AnalisisNegocioScreen`. Confirm it's not used elsewhere (e.g., navigation or other screens) before potentially removing it.

## Ready for Proposal

Yes. The approach is clear, the affected files are identified, and the tradeoffs are understood. Move to `sdd-propose`.
