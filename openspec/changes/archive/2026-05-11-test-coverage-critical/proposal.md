# Proposal: Critical Test Coverage

## Intent

OptoApp SaaS has zero business-logic unit tests across Web and Android. Only 4 Zod schema tests (Web) and 5 data-class tests (Android) exist. Financial calculations (cierre-caja, KPIs), authorization (roles/permissions), clinical transposition, and sync normalization are all untested. This change adds pure-function unit tests — high impact, low effort, zero production risk.

## Scope

### In Scope
- **Web**: `cierre-caja.ts`, `roles.ts`, `permissions.ts`, `rate-limit.ts`, `inventario.ts` (`computeInventarioSummary`), `reportes-financieros.ts`, `EvaluacionBuilder.ts`
- **Web**: `@vitest/coverage-v8`, mock factories in `web/src/test/factories/`
- **Android**: `ServicioRemoto.toEntity()` money coercion, `normalizedOtForUnique`, `normalizedHistoriaKey`, `isTransientNetworkError`
- **Android**: `testImplementation(kotlinx-coroutines-test)`, `TestDispatcher` rule

### Out of Scope
- Supabase-mocked integration tests (Phase 2)
- Compose UI tests, ViewModel tests
- SyncManager/merge-conflict tests (need heavy mocking)

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

Co-locate `.test.ts` next to source files (e.g., `cierre-caja.ts` → `cierre-caja.test.ts`). Existing `__tests__` folders remain. Android mirrors package structure under `src/test/java/`. JUnit 4 with descriptive camelCase method names. Coverage via `@vitest/coverage-v8` on Web; Gradle/Android coverage on Android. Threshold: 50% minimum for in-scope files, 0% global gate to avoid CI blockage.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `web/src/lib/*.ts` | New | `.test.ts` files for pure functions |
| `web/src/domain/builders/EvaluacionBuilder.ts` | New | `EvaluacionBuilder.test.ts` |
| `web/package.json` | Modified | Add `@vitest/coverage-v8` |
| `web/vitest.config.ts` | Modified | Coverage config |
| `app/build.gradle.kts` | Modified | Add `testImplementation(kotlinx-coroutines-test)` |
| `app/src/test/...` | New | Android unit tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Tests break on refactor | Med | Test pure functions only; avoid mocking internals |
| Coverage threshold blocks CI | Low | Start at 0% global; enforce 50% per in-scope file only |
| Legacy data assumptions wrong | Low | Use existing sample rows from codebase for test data |

## Rollback Plan

Delete new `.test.ts`/`.kt` files and revert `package.json`, `vitest.config.ts`, `build.gradle.kts`. No production code changes.

## Dependencies

None. `kotlinx-coroutines-test` already in `libs.versions.toml`.

## Success Criteria

- [ ] All listed pure functions have passing unit tests
- [ ] `npm test` passes on Web; `gradle test` passes on Android
- [ ] Coverage report generates for both platforms
- [ ] No existing tests regress
