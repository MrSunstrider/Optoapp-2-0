# Proposal: Fix Error Handling in "Mi Negocio" Screen

## Intent

`AnalisisNegocioScreen` shows duplicated verbose JSON errors when Supabase RPCs fail — 4 root causes: redundant RPC calls in `GenerarRecomendacionesUseCase` (3x duplication), `e.localizedMessage` leaked to UI, unsafe `LocalDate.parse("")`, and swallowed `CancellationException`.

## Scope

### In Scope
- Restructure `GenerarRecomendacionesUseCase` to accept pre-fetched data, not re-call RPCs
- ViewModel deduplicates errors via `Set<String>`
- Replace `e.localizedMessage` with static messages; log full errors to Logcat
- Fix `LocalDate.parse("")` in `ObtenerDeudoresUseCase` — try/catch + fallback
- Fix swallowed `CancellationException` — rethrow in ViewModel
- Update existing tests for new use case signature

### Out of Scope
- Supabase schema, RLS, or migrations
- UI layout changes
- Global error handling framework or new test coverage

## Capabilities

### New Capabilities
None — pure error-handling refactor, no new spec-level behavior.

### Modified Capabilities
None — error handling is implementation detail; `analisis-negocio` spec covers data schema only.

## Approach

**Approach 2: "Fix the Architecture"** (from exploration):
1. `GenerarRecomendacionesUseCase` accepts `AnalisisMensual` + `List<Deudor>` directly; remove RPC deps from constructor.
2. ViewModel runs 2 parallel calls (`obtenerAnalisisMensual` + `obtenerDeudores`), passes results to recomendaciones. Errors collected in `Set<String>`.
3. Each use case logs full error via `Log.e`, returns static message (e.g. `"No se pudieron cargar los datos"`).
4. `LocalDate.parse()` wrapped in try/catch, falls back to `LocalDate.MIN`.
5. `runCatching` replaced with `try/catch` that rethrows `CancellationException`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/GenerarRecomendacionesUseCase.kt` | Modified | Accept data objects; remove RPC deps |
| `viewmodel/AnalisisNegocioViewModel.kt` | Modified | 2 parallel calls + data passing; `Set` errors; rethrow cancellation |
| `domain/ObtenerDeudoresUseCase.kt` | Modified | Fix `LocalDate.parse`; sanitize error |
| `domain/ObtenerAnalisisMensualUseCase.kt` | Modified | Sanitize error message |
| `data/Resource.kt` | None | Unchanged |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Other callers of `GenerarRecomendacionesUseCase` break | Low | Search codebase before changing |
| Existing tests need update | Low | Update `invoke()` calls to new signature |
| Regression in error/partial-data behavior | Low | Preserve current: null data when source fails |

## Supabase Schema / RLS Impact

**None.** All changes are in Android Kotlin code only.

## Rollback Plan

Revert per-file via `git checkout` or revert the entire PR branch — no data migration or schema rollback needed.

## Dependencies

None.

## Success Criteria

- [ ] Each distinct error appears at most once in the error card
- [ ] Error messages are static text (no JSON or `localizedMessage`)
- [ ] Null `venta_fecha` in `rpc_deudores` does not crash the RPC result
- [ ] Navigating away cancels properly (no spurious "Error inesperado")
- [ ] `./gradlew :optoapp:testDebugUnitTest --stacktrace` passes
