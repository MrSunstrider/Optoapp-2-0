# Tasks: Fix Error Handling in "Mi Negocio" Screen

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~140-160 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Use Case Error Sanitization (RED → GREEN)

- [x] **T1** (`ObtenerAnalisisMensualUseCase`): RED — add assertion in `unexpectedError_returnsResourceError` checking static message content; GREEN — replace `"Error obteniendo analisis mensual: ${e.localizedMessage}"` with `"No se pudieron cargar los datos del mes"` (line 38). Verify: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [x] **T2** (`ObtenerDeudoresUseCase`): RED — update `offline_ioException_returnsError` and `unexpectedError_returnsResourceError` to assert static message; add `null_ventaFecha_usesLocalDateMin` test (REQ-3 scenarios). GREEN — replace both error messages with static text (lines 40, 43); wrap `LocalDate.parse()` in try/catch with `LocalDate.MIN` fallback (line 64). Verify: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

**Dependencies**: None. Phase 1 tasks are independent of each other.

## Phase 2: GenerarRecomendacionesUseCase Restructure (RED → GREEN)

- [x] **T3** (`GenerarRecomendacionesUseCaseTest`): RED — remove `analisisUseCase`/`deudoresUseCase` fields, change `setUp()` constructor, update `mockDeps()` to accept data objects, update all 20+ `invoke()` calls to new signature `(analisis, deudores, opticaId)`, remove `invoke_whenAnalisisError_returnsError` test, add `invoke_whenAnalisisIsNull_returnsError` and `invoke_whenBothNull_returnsError` tests. Verify: compile fails or tests fail referencing old API.
- [x] **T4** (`GenerarRecomendacionesUseCase`): GREEN — remove `obtenerAnalisisMensual`/`obtenerDeudores` from constructor; change `invoke` signature to `(analisis: AnalisisMensual?, deudores: List<Deudor>, opticaId: String)`; add null-guards (line 22-32 in design 2.1), remove internal RPC fetch lines 22-26; keep business logic identical. Verify: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

**Dependencies**: T3 (RED) → T4 (GREEN). Both must pass after T4. No dependency on Phase 1.

## Phase 3: ViewModel Orchestration + Cancellation Fix (RED → GREEN)

- [x] **T5** (`AnalisisNegocioViewModelTest`): RED — update `primeUseCases()` to use `coEvery { generarRecomendaciones(any(), any<List<Deudor>>(), any()) }`; update `error state when analisis use case fails` (new expected: `"Error en analisis mensual; No hay datos de analisis para generar recomendaciones"`); update `all use cases failing` (new expected: 3 distinct messages from set); add cancellation test verifying no error state on scope cancel (REQ-4). Verify: tests fail with old ViewModel code.
- [x] **T6** (`AnalisisNegocioViewModel`): GREEN — replace `runCatching` with `try/catch` that rethrows `CancellationException`; run 2 parallel async calls (analisis + deudores), extract data, pass to `generarRecomendaciones`; collect errors in `setOfNotNull()`; remove `async` for recomendaciones (design 2.2). Verify: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

**Dependencies**: T5 (RED) → T6 (GREEN). Depends on T3/T4 (uses new `GenerarRecomendacionesUseCase` signature). Independent of T1/T2.

## Implementation Order

```
Phase 1 (T1, T2) ──parallel──▶  (any order, independent)
Phase 2 (T3 → T4) ──────────▶  (sequential, RED→GREEN)
Phase 3 (T5 → T6) ──────────▶  (sequential, RED→GREEN, after T4)
```

Phases 1 and 2 are independent and can be done in parallel. Phase 3 requires Phase 2 first. Total: 6 tasks across 8 files.
