# Tasks: Critical Test Coverage

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~870 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Web auth/money → PR 2: Web rest → PR 3: Android |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Web infrastructure + cierre-caja + roles + permissions | PR 1 | base: main; tests/docs included |
| 2 | Web rate-limit + reportes + inventario + EvaluacionBuilder | PR 2 | base: main; depends on PR 1 after merge |
| 3 | Android visibility changes + all 3 test files | PR 3 | base: main; depends on PR 2 after merge |

## Phase 1a: Web Foundation & Money/Authorization

- [ ] 1.1 Modify `web/package.json` — add `"@vitest/coverage-v8": "^4.x"` to devDependencies
- [ ] 1.2 Modify `web/vitest.config.ts` — add coverage provider `v8`, per-file 50% threshold glob, no global gate
- [ ] 1.3 Modify `web/src/lib/cierre-caja.ts` — export `normalizeMoney`, `toCents`, `fromCents`, `mapMedioPago`, `dateOnly` (visibility-only, zero logic)
- [ ] 1.4 Create `web/src/lib/cierre-caja.test.ts` — `normalizeMoney` (Infinity→0, NaN→0, rounding), `toCents` (precision), `fromCents` (rounding), `mapMedioPago` (6 variants), `resolveCierrePeriodo` (date calc), `canReadCierre`, `canCloseCierre`, `canOverrideCierre` (all roles + case/whitespace)
- [ ] 1.5 Create `web/src/lib/roles.test.ts` — all 6 exported fns × every role variant (admin, gerente, cajero, especialista, asesor/asesora/ventas, invitado, lectura) includes case/whitespace normalization
- [ ] 1.6 Create `web/src/lib/config/permissions.test.ts` — `canAccessConfigModule` (8 module keys × 5+ roles), `isReadOnlyRole`, `isInternalRole`, `canManageOpticaSettings`

## Phase 1b: Web Security & Domain

- [ ] 2.1 Create `web/src/lib/rate-limit.test.ts` — unique keys per test; first call allowed, 5 calls block 6th, window expiry resets, remaining count decrements
- [ ] 2.2 Create `web/src/lib/reportes-financieros.test.ts` — `resolveRange` (dia/semana/mes/anio date boundaries), `labelForPeriodo` (4 labels)
- [ ] 2.3 Create `web/src/lib/inventario.test.ts` — `computeInventarioSummary` (empty, single, multiple, stock alerts), `soles` (formatting)
- [ ] 2.4 Create `web/src/domain/builders/EvaluacionBuilder.test.ts` — `normalizeSphere` (plano/neutro/pl→0.00), `transpose` (positive cyl→negative, axis wrap), `calculateEE` (spherical equivalent), `build()` via Zod safeParse with full chain

## Phase 1c: Android Infrastructure & Tests

- [x] 3.1 Modify `app/src/main/.../domain/SyncFinanzasUseCase.kt` — `private fun normalizedOtForUnique` → `internal fun`; `private fun isTransientNetworkError` → `internal fun`
- [x] 3.2 Modify `app/src/main/.../viewmodel/SyncDiagnosticsViewModel.kt` — `private fun isTransientNetworkError` → `internal fun`
- [x] 3.3 Modify `app/src/main/.../domain/SyncHistorialUseCase.kt` — `private fun normalizedHistoriaKey` → `internal fun`
- [x] 3.4 Modify `app/src/main/.../domain/SyncPacientesUseCase.kt` — `private fun normalizedHistoriaKey` → `internal fun`
- [x] 3.5 Modify `app/build.gradle.kts` — add `testImplementation(libs.kotlinx.coroutines.test)`
- [x] 3.6 Create `app/src/test/.../domain/SyncFinanzasUseCaseKtTest.kt` — `ServicioRemoto.toEntity()`: negative montoTotal→0, aCuenta>montoTotal→clamped, aCuenta negative→0, normal pass-through, blank opticaId fallback
- [x] 3.7 Create `app/src/test/.../domain/SyncUtilsTest.kt` — `normalizedOtForUnique`: trim+uppercase, blank→null, null→null; `normalizedHistoriaKey`: same logic from both UseCases
- [x] 3.8 Create `app/src/test/.../domain/SyncUtilsKtTest.kt` — `isTransientNetworkError`: timeout, "timed out", "unable to resolve host", "network is unreachable", "connection reset", non-network error→false
- [x] 3.9 Inline factory helpers — `makeServicioRemoto()` in SyncFinanzasUseCaseKtTest.kt; logic helpers in SyncUtilsTest.kt and SyncUtilsKtTest.kt
