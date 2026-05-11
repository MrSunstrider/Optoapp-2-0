# Design: Critical Test Coverage

## Technical Approach

Add pure-function unit tests to Web and Android with zero logic changes. Co-locate `.test.ts` next to source files on Web (pattern: `cierre-caja.ts` → `cierre-caja.test.ts`). Android mirrors production packages under `app/src/test/java/`. Existing `__tests__/` folders remain untouched — new tests follow co-location. Coverage via `@vitest/coverage-v8` (Web) and JaCoCo (Android, no threshold gate initially).

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Android private helpers | Extract to `internal` top-level functions (2-line change) | Test through complex public methods (Supabase/DB mocks needed) | **Option A** | Private functions are untestable directly. Making them `internal` is a visibility-only change — zero logic touched. Option B requires full integration mocks (out of scope Phase 1). |
| Web test location | Co-locate `.test.ts` next to source | Use `__tests__/` for new tests too | **Co-locate** | Matches proposal scope. `__tests__/` stays for existing Zod schema tests. |
| Rate-limit isolation | Unique test key per `it()` block | Reset `attempts` Map via `vi.resetModules()` | **Unique keys** | Simpler, no module-reload complexity. Each `it()` uses `checkRateLimit("test-"+crypto.randomUUID())`. |
| EvaluacionBuilder | Test via public `build()` + `EvaluacionSchema.parse` | Expose private methods as `internal` | **Public API** | Builder pattern is the contract. `build()` triggers all coercion via Zod. |
| Coverage thresholds | Per-file 50% for in-scope files only | Global 50% gate on all files | **Per-file only** | Avoids CI blockage on legacy untested code. Scope is 8 files Web + 3 files Android. |

## Data Flow

```
Source file (pure functions)  ──→  .test.ts / Test.kt  ──→  vitest / JUnit runner
                                        │
                                        └── Coverage (v8 / JaCoCo)
```

No production data path affected. Tests import source modules directly. Android internal functions exposed via same-module visibility.

## File Changes

### Web — New Files
| File | Action | Description |
|------|--------|-------------|
| `web/src/lib/cierre-caja.test.ts` | Create | Tests for `normalizeFechaCierre`, `resolveCierrePeriodo`, `money`, `formatFechaLarga`, `canReadCierre`, `canCloseCierre`, `canOverrideCierre`, `mapMedioPago`, `normalizeMoney`, `toCents`, `fromCents`, `dateOnly` |
| `web/src/lib/reportes-financieros.test.ts` | Create | Tests for `resolveRange` (4 periods), `labelForPeriodo`, `toDateOnly` |
| `web/src/lib/roles.test.ts` | Create | Tests for all 6 exported role-checking functions |
| `web/src/lib/config/permissions.test.ts` | Create | Tests for `canManageOpticaSettings`, `isReadOnlyRole`, `canAccessConfigModule`, `isInternalRole` |
| `web/src/lib/rate-limit.test.ts` | Create | Tests for `checkRateLimit`: allow, block, window-reset, remaining count |
| `web/src/lib/inventario.test.ts` | Create | Tests for `computeInventarioSummary`, `soles` |
| `web/src/domain/builders/EvaluacionBuilder.test.ts` | Create | Tests for builder chain + Zod validation via `build()` |

### Web — Modified Files
| File | Action | Description |
|------|--------|-------------|
| `web/package.json` | Modify | Add `"@vitest/coverage-v8": "^4.x"` to devDependencies |
| `web/vitest.config.ts` | Modify | Add coverage config: provider `v8`, per-file threshold via `.thresholds` glob, no global gate |

### Android — New Files
| File | Action | Description |
|------|--------|-------------|
| `app/src/test/java/.../domain/SyncFinanzasUseCaseKtTest.kt` | Create | `ServicioRemoto.toEntity()` coercion, `normalizedOtForUnique` normalization, `isTransientNetworkError` pattern matching |
| `app/src/test/java/.../viewmodel/SyncDiagnosticsViewModelKtTest.kt` | Create | `isTransientNetworkError` (ViewModel copy, same logic) |
| `app/src/test/java/.../domain/NormalizedHistoriaKeyTest.kt` | Create | `normalizedHistoriaKey` from both UseCases (identical logic) |
| `app/src/test/java/.../domain/SyncPacientesUseCaseKtTest.kt` | Create | `normalizedHistoriaKey` (duplicate copy) |

### Android — Modified Files
| File | Action | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modify | Add `testImplementation(libs.kotlinx.coroutines.test)` (currently only `androidTestImplementation`) |
| `app/build.gradle.kts` | Modify | Add JaCoCo plugin and config block (report only, no threshold) |
| `app/src/main/.../domain/SyncFinanzasUseCase.kt` | Modify | Change `private fun normalizedOtForUnique` → `internal fun` (visibility only) |
| `app/src/main/.../domain/SyncFinanzasUseCase.kt` | Modify | Change `private fun isTransientNetworkError` → `internal fun` |
| `app/src/main/.../viewmodel/SyncDiagnosticsViewModel.kt` | Modify | Change `private fun isTransientNetworkError` → `internal fun` |
| `app/src/main/.../domain/SyncHistorialUseCase.kt` | Modify | Change `private fun normalizedHistoriaKey` → `internal fun` |
| `app/src/main/.../domain/SyncPacientesUseCase.kt` | Modify | Change `private fun normalizedHistoriaKey` → `internal fun` |

## Interfaces / Contracts

**Test data factories** — local to each test file, no shared factories:

```typescript
// Pattern: factory helpers inside the test file
function makeCierreTx(overrides?: Partial<CierreTx>): CierreTx { ... }
function makeMonturaItem(overrides?: Partial<MonturaItem>): MonturaItem { ... }
```

**Android test class pattern**:
```kotlin
class SyncFinanzasUseCaseKtTest {
    @Test
    fun normalizedOtForUnique_trimsAndUppercases() { ... }
}
```

For `ServicioRemoto.toEntity()`, instantiate `ServicioRemoto(...)` directly (data class, all params have defaults except `id`, `fecha`, `opticaId`).

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Web pure functions | Input/output, edge cases, role combos | `describe`/`it` per function, `expect().toBe/toEqual()` |
| Web Zod validation | `EvaluacionBuilder.build()` | `safeParse` + `expect(result.success)` |
| Android pure functions | Normalization, coercion, pattern matching | `@Test` per function variant, `assertEquals`/`assertTrue` |
| Android `toEntity()` | Money coercion edge cases | Instantiate `ServicioRemoto` with extreme values |

**Out of scope**: `fetchCierreCaja`, `fetchCierreFormalStatus`, `fetchReporteFinanciero`, `fetchMonturasInventario`, `queryMonturasSafe`, their Android equivalents (need Supabase/DB mocks).

## Implementation Order

| Phase | Files | Rationale |
|-------|-------|-----------|
| 1a | `cierre-caja.test.ts`, `reportes-financieros.test.ts` | Money calculations: highest risk, purest functions |
| 1b | `roles.test.ts`, `permissions.test.ts`, `rate-limit.test.ts` | Authorization: security-sensitive |
| 1c | `EvaluacionBuilder.test.ts`, `inventario.test.ts` | Clinical domain: complex but lower blast radius |
| 1d | Android `.kt` test files | Android functions: last phase, visibility changes needed first |

## Migration / Rollout

No migration required. All changes are additive (tests) or visibility-only (`private` → `internal`). Rollback: delete new test files, revert `build.gradle.kts` and `vitest.config.ts`, restore `private` on Android helpers.

## Open Questions

- [ ] JaCoCo version: latest `org.jacoco:org.jacoco.core` or `libs.versions.toml` alias? (check `libs.versions.toml` during apply)
- [ ] Should `normalizedHistoriaKey` and `isTransientNetworkError` be extracted to shared utils to avoid testing duplicates? (Deferred — Phase 2 refactor)
