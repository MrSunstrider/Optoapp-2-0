# Resource<T> Usage Audit

> Audit-only deliverable. No code changes proposed. Compiled during Phase 8 (S3) of the `post-review-cleanup` change.

## 1. Scope

This document audits every occurrence of `Resource<T>` in the OptoApp Android module, categorizes each call site by the pattern it uses, compares the type with `kotlin.Result`, and records the decision on whether to migrate, consolidate, or keep both.

The audit is read-only. Migration, if decided later, is explicitly out of scope for the `post-review-cleanup` change.

## 2. Executive summary

| Metric | Value |
|--------|-------|
| `Resource<…>` type-parameter signatures | 29 occurrences across 19 files |
| `Resource.Success` references | 118 |
| `Resource.Error` references | 126 |
| `Resource.Loading` references | 8 |
| `kotlin.Result` references | 73 |
| **Decision** | **Keep `Resource<T>` as the standard async/UI-state wrapper. Keep `kotlin.Result` for synchronous "did it work" helpers. Document the boundary.** |

The actual `Resource<` count is **29** (the task description expected 64 — that estimate was a rough ballpark; the precise number is 29 type-parameter references, plus 252 case-class references, totalling 281). The audit documents the real numbers.

## 3. Type definition

File: `optoapp/src/main/java/com/example/optoapp/data/Resource.kt`

```kotlin
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
```

Three characteristics that drive the design:

1. **Three states**, not two: `Success`, `Error`, `Loading`.
2. **Error is a state, not a Throwable**: the failure reason is a `String?` (Spanish UI text, ready to render).
3. **Both `Error` and `Loading` can carry a `T?`** — enables preserving stale data while a refresh is in flight, or partially-valid error responses.

## 4. Usage inventory

### 4.1 Producer signatures — `Resource<…>` declared in return type

These are the type-parameter references the grep counts as 29.

| File | Count | Pattern |
|------|-------|---------|
| `data/Resource.kt` | 4 | The class definition itself (Success, Error, Loading, base). Excluded from the per-pattern analysis. |
| `data/DispensacionRepository.kt` | 2 | `getDispensacionById`, `getServicioById` |
| `data/PacienteRepository.kt` | 2 | `getPacienteById`, `getEvaluacionById` |
| `data/montura/MonturaInventoryCoordinator.kt` | 3 | `getMonturaById`, `syncStockFromMovimientos`, `registrarSalida` |
| `domain/SyncPacientesUseCase.kt` | 1 | `Resource<PacientesSyncResult>` |
| `domain/SyncHistorialUseCase.kt` | 1 | `Resource<HistorialSyncResult>` |
| `domain/SyncFinanzasUseCase.kt` | 1 | `Resource<FinanzasSyncResult>` |
| `domain/SyncInventarioUseCase.kt` | 1 | `Resource<InventarioSyncResult>` |
| `domain/SyncInventarioFisicoUseCase.kt` | 1 | `Resource<InventarioFisicoSyncResult>` |
| `domain/SyncProveedoresUseCase.kt` | 1 | `Resource<ProveedoresSyncResult>` |
| `domain/SyncOrdenesCompraUseCase.kt` | 1 | `Resource<OrdenesCompraSyncResult>` |
| `domain/SyncInventoryKpisUseCase.kt` | 1 | `Resource<InventoryKpiSummary>` |
| `sync/PostSaveSyncScheduler.kt` | 1 | `block: suspend () -> Resource<*>` (in `runWithRetry`) |
| `viewmodel/SyncViewModel.kt` | 1 | `Resource<*>` (in `syncForEntityTypeWithResult`) |
| `androidTest/.../fakes/FakeDispensacionRepository.kt` | 1 | Test fake |
| `androidTest/.../fakes/FakeEvaluacionRepository.kt` | 1 | Test fake |
| `androidTest/.../fakes/FakePacienteRepository.kt` | 2 | Test fake (paciente + evaluacion) |
| `androidTest/.../fakes/FakeSyncUseCases.kt` | 4 | Test fake (4 use cases) |

**Total type-parameter signatures: 25** (excluding the 4 in `Resource.kt`).

### 4.2 Consumer files (use `Resource.Success` / `Resource.Error` / `Resource.Loading`)

These don't add to the `Resource<` count but consume the type:

| File | States matched | Notes |
|------|----------------|-------|
| `viewmodel/DispensacionViewModel.kt` | Success, Error, Loading | Full 3-state `when` in `loadDispensacion`; Success-only check in `loadPacienteNombre` |
| `viewmodel/EvaluacionViewModel.kt` | Success, Error, Loading | Full 3-state `when` in `loadEvaluacion`; Success-only in `deleteEvaluacion` and `loadPacienteEdadAndCalculateAdd` |
| `viewmodel/ServiciosViewModel.kt` | (imports `Resource`) | Uses generic catch refactor (Phase 1) — not a Resource consumer directly |
| `util/DispensacionStockHelper.kt` | Success, Error, Loading | Converts all 3 → `kotlin.Result` (Loading becomes `Result.failure(IllegalStateException("Cargando"))`) |
| `test/.../ResourceTest.kt` | Success, Error, Loading | 4 unit tests, one per state + "error can carry data" |
| `test/.../util/DispensacionStockHelperTest.kt` | Success, Error, Loading | Exercises the Resource → Result conversion |
| `test/.../viewmodel/SyncViewModelThreeWayMergeTest.kt` | Success, Error | Mocks use cases |
| `test/.../viewmodel/SyncViewModelSilentSyncTest.kt` | Success | Mocks use cases |
| `test/.../viewmodel/SyncViewModelConflictResolutionTest.kt` | Success, Error | Mocks use cases |
| `test/.../viewmodel/SyncViewModelChildBumpTest.kt` | Success, Error | Mocks use cases |
| `test/.../viewmodel/SyncViewModelBumpCoverageTest.kt` | Success, Error | Mocks use cases |
| `androidTest/.../fakes/*` (4 files) | Success, Error | Mirror the real signatures; never emit `Loading` |

## 5. Usage patterns

The 19 production + test source files break down into three patterns. Note: a single file can mix patterns (e.g., `EvaluacionViewModel` does both 3-state and 2-state matches).

### 5.1 Pattern A — Loading + Error + Data wrapper (3-state match)

Exhaustive `when` over all three states, mapping each to a different UI update.

| Site | File | Lines |
|------|------|-------|
| `loadDispensacion` | `viewmodel/DispensacionViewModel.kt` | 131–170 |
| `loadEvaluacion` | `viewmodel/EvaluacionViewModel.kt` | 35–47 |
| `adjustStock` | `util/DispensacionStockHelper.kt` | 18–22 |
| `adjustStockAndRegistrarMovimiento` | `util/DispensacionStockHelper.kt` | 76–80 |

This pattern is the **intended** way to use `Resource<T>`: drive a three-way UI state machine (loading spinner / populated screen / error banner) from a single suspend call.

### 5.2 Pattern B — Error + Data wrapper (2-state match, ignores `Loading`)

Treats the result as "either I got the data, or I got an error message". The `Loading` state is unreachable in practice (the suspend function is awaited, so by the time `when` runs, the coroutine is past any internal loading state).

| Site | File | Snippet |
|------|------|---------|
| `loadPacienteNombre` | `viewmodel/DispensacionViewModel.kt` | `when (val result = repository.getPacienteById(pacienteId)) { is Resource.Success -> { … } else -> Unit }` |
| `deleteEvaluacion` | `viewmodel/EvaluacionViewModel.kt` | `if (result is Resource.Success) { … }` |
| `loadPacienteEdadAndCalculateAdd` | `viewmodel/EvaluacionViewModel.kt` | `if (p is Resource.Success) { … }` |
| `saveEvaluacion` (pResult check) | `viewmodel/EvaluacionViewModel.kt` | `if (pResult is Resource.Success) pResult.data?.nombreCompleto ?: "Paciente" else "Paciente"` |
| `runWithRetry` | `sync/PostSaveSyncScheduler.kt` | `is Resource.Error -> { lastError = IOException(…) } else -> { return }` — **treats `Loading` as success** |

The `PostSaveSyncScheduler.runWithRetry` case is the most subtle: when a use case returns `Resource.Loading` it is treated as a success and `return`s. This is safe in practice (none of the seven sync use cases ever emit `Loading`), but it relies on a contract that is not enforced at the type level. **Recommended follow-up** (out of scope here): add a `runWithRetry` test that returns `Resource.Loading` to lock the behavior down.

### 5.3 Pattern C — Producer-only (signature uses `Resource<T>`, body only emits Success/Error)

These are the data and domain layers. They declare the return type as `Resource<…>` but the implementation only ever constructs `Resource.Success` or `Resource.Error` — `Resource.Loading` is never instantiated by a producer in the codebase.

| Layer | Files |
|-------|-------|
| Repository | `DispensacionRepository`, `PacienteRepository`, `MonturaInventoryCoordinator` |
| Use case | `SyncPacientesUseCase`, `SyncHistorialUseCase`, `SyncFinanzasUseCase`, `SyncInventarioUseCase`, `SyncInventarioFisicoUseCase`, `SyncProveedoresUseCase`, `SyncOrdenesCompraUseCase`, `SyncInventoryKpisUseCase` |
| Sync orchestration | `PostSaveSyncScheduler.runWithRetry`, `SyncViewModel.syncForEntityTypeWithResult` (returns `Resource<*>` from a `when` over entity types) |

The pattern is: **producers return `Resource<T>` as a typed success/failure wrapper; the Loading state is owned by the consumer (UI layer) and is opt-in via a 3-state `when`**.

This is a deliberate split: producers don't have to know whether a caller wants a loading spinner. They just signal "here is the data" or "here is the error message". The caller decides whether to render a spinner based on its own state machine.

## 6. Comparison: `Resource<T>` vs `kotlin.Result`

| Axis | `Resource<T>` | `kotlin.Result` |
|------|---------------|-----------------|
| Number of states | 3 (Success, Error, Loading) | 2 (Success, Failure) |
| Error payload | `String?` — UI-ready message | `Throwable` — typed exception |
| Loading state | Built-in | Not built-in (caller has to combine with another flag) |
| Can carry partial data on error | Yes — `Error(data = T?)` | No |
| Sealed (exhaustive `when`) | Yes | Yes |
| `runCatching {}` | Not applicable | First-class |
| CancellationException awareness | Manual (W1 catches rethrow it) | Convention: rethrow `CancellationException` from `runCatching` is opt-in |
| Standard library | No (custom 7-line type) | Yes (Kotlin stdlib, Kotlin 1.3+) |
| Compose / UI state friendliness | Strong — 3 states map to spinner/content/error | Weak — would need a separate wrapper for loading |
| Migration cost from current state | N/A (current) | High — 25 type signatures, 252 case references, 6 producers, 4 consumers |
| Error message language | Spanish (UI strings) | Locale-agnostic Throwable message |

### 6.1 What `kotlin.Result` would not give us

- A built-in `Loading` state. To preserve the current UX, every consumer that wants a spinner would need an additional `isLoading: Boolean` field on its UI state. `DispensacionViewModel`, `EvaluacionViewModel`, and the post-save sync UI all rely on the third state today.
- A typed "Error with Spanish UI string" payload. Every `Resource.Error("Dispensación no encontrada")` would become `Result.failure(IllegalStateException("Dispensación no encontrada"))` and the message would still be `.message` of a `Throwable` — same string, more allocation, no type-safety win.
- Stale-data preservation. `Resource.Error(data = cachedEntity)` (used by `DispensacionViewModel.loadDispensacion` to keep the form populated when a refresh fails) has no `Result` equivalent.

### 6.2 What `Resource<T>` costs us

- 7 lines of hand-maintained type definition.
- 4 unit tests in `ResourceTest.kt` that the stdlib gives for free.
- Cognitive overhead for newcomers ("why not just `Result`?"). The standard usage pattern below addresses this.

## 7. Decision: keep `Resource<T>`

**`Resource<T>` stays. `kotlin.Result` stays. They serve different roles.**

| Role | Type | Why |
|------|------|-----|
| Asynchronous data fetch with possible UI loading state | `Resource<T>` | 3 states match the UI state machine; error is a localized string; `Error(data = T?)` preserves stale data |
| Synchronous "did this work" check | `kotlin.Result` | Single-value success or typed exception; `runCatching {}` composability; standard library |
| Pure transformations inside a coroutine | `kotlin.Result` (via `runCatching`) | Idiomatic Kotlin |
| Cross-layer async (repo → ViewModel) | `Resource<T>` | The contract between the data layer and the presentation layer |
| Cross-layer sync helper (UI helper that wraps an async fetch + a side effect) | `Result<T>` (with explicit `Resource → Result` conversion) | The helper returns a single value or an exception; UI is not involved. `DispensacionStockHelper` is the model. |

This is consistent with the codebase: `DispensacionStockHelper` already converts `Resource → Result` at the boundary. The pattern is: **Resource for async state, Result for sync outcomes**.

## 8. Standard usage pattern

### 8.1 Producing `Resource<T>`

In a `suspend fun` that fetches data:

```kotlin
suspend fun getPacienteById(id: String): Resource<Paciente> {
    return try {
        val paciente = pacienteDao.getPacienteById(id)
        if (paciente != null) Resource.Success(paciente)
        else Resource.Error("Paciente no encontrado")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.e(TAG, "getPacienteById: id=$id", e)
        Resource.Error("Error de red al obtener paciente")
    } catch (e: Exception) {
        Log.e(TAG, "getPacienteById: id=$id", e)
        Resource.Error(e.message ?: "Error al obtener paciente")
    }
}
```

Rules:

1. **Always return `Resource.Success(data)` or `Resource.Error(localizedMessage)`**. Never `Resource.Loading` from a producer.
2. **Never `throw` from a `Resource`-returning function for `IOException` / generic `Exception`** — wrap in `Resource.Error`. Only `CancellationException` is rethrown.
3. **Error message is a UI-ready Spanish string**, not a Throwable. The ViewModel will render it directly in an error banner.
4. **`Error(data = T?)` is allowed** for "stale data + refresh failed" flows, but only when the caller explicitly wants to keep the previous entity on screen.

### 8.2 Consuming `Resource<T>`

In a ViewModel that drives a 3-state UI:

```kotlin
fun loadDispensacion(dispensacionId: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = repository.getDispensacionById(dispensacionId)) {
            is Resource.Success -> {
                val d = result.data ?: return@launch
                _uiState.update { it.copy(isLoading = false, dispensacion = d) }
            }
            is Resource.Error -> {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            is Resource.Loading -> { /* unreachable in practice, but exhaustive */ }
        }
    }
}
```

In a ViewModel that only cares about success/failure:

```kotlin
fun deleteEvaluacion(evaluacionId: String, onComplete: () -> Unit) {
    viewModelScope.launch {
        val result = repository.getEvaluacionById(evaluacionId)
        if (result is Resource.Success) {
            result.data?.let { repository.deleteEvaluacion(it) }
            onComplete()
        }
    }
}
```

Rules:

1. **Prefer 3-state exhaustive `when`** when the UI shows a loading spinner, content, or error banner.
2. **`is Resource.Success` / `else` 2-state checks are acceptable** when the UI does not render a separate loading state (the spinner is implicit in the call).
3. **Never wrap a `Resource`-returning call in `try-catch`**. The function contract is "returns `Resource`, never throws except for `CancellationException`". (Phase 1 W1 cleanup enforces this.)
4. **Never call `result.data` outside a `is Resource.Success` branch.** `data` is nullable and meaningless for `Loading`.

### 8.3 Converting `Resource<T>` → `kotlin.Result<T>`

When a synchronous helper wraps a `Resource`-returning async call:

```kotlin
suspend fun adjustStock(monturaId: String, opticaId: String, delta: Int): Result<Int> {
    val montura = when (val r = coordinator.getMonturaById(monturaId)) {
        is Resource.Success -> r.data ?: return Result.failure(IllegalStateException("Montura no encontrada"))
        is Resource.Error -> return Result.failure(IllegalStateException(r.message))
        is Resource.Loading -> return Result.failure(IllegalStateException("Cargando"))
    }
    // …
    return Result.success(affected)
}
```

Rules:

1. **Always map all three Resource states** in the `when`. `Loading` becomes a `Result.failure` with a `"Cargando"` message — defensive, even if unreachable in practice.
2. **Wrap the original `message` in an `IllegalStateException`** so that `Result.failure` carries a typed exception, not a raw string. This is the boundary where the Spanish UI string becomes a JVM exception.

### 8.4 Anti-patterns

| Anti-pattern | Why it's wrong | Where it would bite |
|--------------|----------------|---------------------|
| `return Resource.Loading(data)` from a producer | `Loading` is the caller's signal, not the producer's. A producer that "is in the middle of work" should be `suspend`ing, not returning `Loading`. | Sync use cases, repositories |
| `try { … } catch (e: Exception) { Resource.Error(e.message) }` inside a ViewModel that already receives a `Resource` | Violates the producer contract. Catches should live in the producer. | `DispensacionViewModel`, `EvaluacionViewModel` consumers |
| Returning a `Throwable` as the error payload (mimicking `Result.failure`) | The error message is meant to be rendered directly. A `Throwable` will be `.toString()`-ified in the UI and look bad. | Would only show up after a migration — current code is clean |
| Mixing `Resource<T>` and `Result<T>` in the same function signature | Two different error idioms confuse readers. Pick one based on the rules above. | New helpers in the util layer |

## 9. Out of scope

- A `Resource<T>` → `kotlin.Result<T>` migration. Cost: 25 type signatures, 252 case references, 6 producers, 4 consumers, plus 4 unit tests. Benefit: standardize on the Kotlin stdlib, but lose the `Loading` state and the Spanish-UI-string error payload. **Not worth it.**
- A `kotlin.Result` → `Resource<T>` migration. Cost: same. Benefit: none — `Result` is already only used in 2 production sites (`DispensacionStockHelper.adjustStock`, `DispensacionStockHelper.adjustStockAndRegistrarMovimiento`) where it is the right type.
- A 4-state wrapper (Success / Error / Loading / Empty). Not requested. `Resource.Error` + nullable `data` covers the "empty" use case.

## 10. References

- Definition: `optoapp/src/main/java/com/example/optoapp/data/Resource.kt`
- Spec: `openspec/changes/post-review-cleanup/specs/result-type-unification/spec.md`
- Proposal: `openspec/changes/post-review-cleanup/proposal.md` (S3)
- Phase 1 catch refactor (predecessor): `openspec/changes/post-review-cleanup/tasks.md` (W1)
- Reference converter: `optoapp/src/main/java/com/example/optoapp/util/DispensacionStockHelper.kt`
- Unit tests: `optoapp/src/test/java/com/example/optoapp/ResourceTest.kt`
