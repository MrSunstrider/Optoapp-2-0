# Design: Post-Review Cleanup

## Technical Approach

Six independent deliverables sharing build + test infra. W1–W3 are structural (catch refactors, file extractions, BOM bump); S1–S2 add unit tests using existing patterns; S3 is audit-only. All changes are additive or refactoring-only — no new behavior, no API changes.

## Architecture Decisions

### Decision: Exception handling pattern

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Replace `catch (e: Exception)` with specific types + Timber.e | Must audit each block for actual thrown types | **Adopt** — codebase already uses this pattern in SyncViewModel, PostSaveSyncScheduler |
| Wrap in `runCatching` | Loses ability to rethrow CancellationException idiomatically | **Reject** — manual catch with CancellationException rethrow is the established convention |

Rationale: The project already has a documented three-catch pattern (CancellationException → IOException → specific). W1 applies it consistently to the 7 remaining generic catches.

### Decision: ViewModel test strategy

| Option | Tradeoff | Decision |
|--------|----------|----------|
| MockK + constructor args | ViewModels have final class deps (Hilt), requires `open` classes or `mockk(relaxed)` | **Reject** — breaks existing convention |
| Java reflection | Tests contract signatures, not behavior | **Adopt** — matches DispensacionViewModelCharacterizationTest pattern |
| Robolectric + Hilt | Heavy, slow, overkill for 5+5 tests | **Reject** |

Rationale: Existing ViewModel tests use characterization/reflection because Hilt + Android deps make instantiation impractical without DI setup. New tests follow the same pattern.

### Decision: PostSaveSyncScheduler test approach

| Option | Tradeoff | Decision |
|--------|----------|----------|
| MockK with relaxed mocks | Fast, covers scheduling logic | **Adopt** — class is `@Singleton` with injectable deps, `open` not needed for MockK |
| Robolectric | Android deps (Log) require this | **Minimal** — use Robolectric only for `Log` stubbing |

Rationale: PostSaveSyncScheduler depends on injectable interfaces (CoroutineScope, SyncGate, use cases). MockK handles these cleanly. `Log` usage on lines 62–133 requires Robolectric or mockStatic — Robolectric is already in dependencies.

### Decision: Resource unification recommendation

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Migrate to kotlin.Result | 64 usage sites, no Loading state, Throwable-only error type | **Defer** — document rationale for keeping Resource<T> |
| Add Resource.Loading to kotlin.Result | Not possible (sealed class is final) | **N/A** |
| Keep Resource<T> | Covers Loading+Success+Error, typed errors | **Recommend** — document as current convention |

Rationale: Resource<T> has 64 usages across data (repositories), domain (use cases), and presentation (ViewModels). It carries `Loading` state which `kotlin.Result` lacks. The migration cost exceeds benefit — the spec itself acknowledges this.

## Data Flow

```
┌─────────────────────────────────────────────────────┐
│  W1: Catch Refactors (7 files, no data flow change) │
│  catch(e:Exception) → catch(SpecificException)      │
│  Same UiState.Error paths, same Timber.e calls      │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│  W2: File Extraction (5 files → ~17 files)          │
│  Original.kt  ──extract──►  FeatureHelper.kt         │
│                            FeatureSection.kt         │
│                            FeatureSection2.kt         │
│  Original.kt keeps imports, delegates calls          │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────┐
│  S1/S2: Tests (no data flow change)  │
│  Test class ──exercises──► contracts │
└──────────────────────────────────────┘
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `gradle/libs.versions.toml` | Modify | Update `composeBom` from `2024.02.02` to `2024.09.00` (or latest) |
| `openspec/changes/post-review-cleanup/specs/result-type-unification/Resource-audit.md` | Create | Audit document: Resource<T> usage count (64), call site list, comparison table, decision |
| **W1 — Exception Handling** | | |
| `viewmodel/auth/AuthDelegate.kt` | Modify | Lines 143, 238: `catch(e:Exception)` → specific types, Timber.e logging |
| `viewmodel/auth/BackupDelegate.kt` | Modify | Line 87: `catch(e:Exception)` → IOException + DatabaseException |
| `viewmodel/ServiciosViewModel.kt` | Modify | Line 208: `catch(e:Exception)` → specific types (SQLiteConstraintException already handled line 201) |
| `viewmodel/MonturasViewModel.kt` | Modify | Line 166: `catch(e:Exception)` → specific types |
| `viewmodel/PacienteViewModel.kt` | Modify | Line 141: `catch(e:Exception)` → IOException + SupabaseException |
| `viewmodel/FiscalConfigViewModel.kt` | Modify | Line 244: `catch(e:Exception)` → specific types |
| `viewmodel/EvaluacionViewModel.kt` | Modify | Line 496: `catch(e:Exception)` → NumberFormatException (parseSnellenToLogMar) |
| **W2 — Large File Extraction** | | |
| `viewmodel/EvaluacionViewModel.kt` | Modify | Extract DIP parsing to `EvaluacionDipHelper.kt`, refraction logic to `EvaluacionDiagnosticoHelper.kt` |
| `viewmodel/EvaluacionDipHelper.kt` | Create | Extracted from EvaluacionViewModel: `parseDipOrDnp()`, `formatDipForUi()` |
| `viewmodel/EvaluacionDiagnosticoHelper.kt` | Create | Extracted: `parseRefraction()`, `calcularDiagnostico()`, `parseSnellenToLogMar()`, `normalizeAndTranspose()` |
| `ui/components/EvaluacionFormSections.kt` | Modify | Extract composables into domain-specific files |
| `ui/components/AnamnesisSection.kt` | Create | Extracted from EvaluacionFormSections |
| `ui/components/AgudezaVisualSection.kt` | Create | Extracted from EvaluacionFormSections |
| `ui/components/RecetaOptometriaSection.kt` | Create | Extracted from EvaluacionFormSections |
| `ui/components/EvaluacionOtrosSection.kt` | Create | Extracted composables |
| **W2 — MainDrawerScreen** | | |
| `presentation/screens/main/MainDrawerScreen.kt` | Modify | Extract drawer sections |
| `presentation/screens/main/DrawerSections.kt` | Create | Extracted drawer composables |
| **W2 — NuevaDispensacionScreen** | | |
| `presentation/screens/dispensacion/NuevaDispensacionScreen.kt` | Modify | Extract form sections |
| `presentation/screens/dispensacion/DispensacionFormSections.kt` | Create | Extracted form composables |
| **W2 — RecetaPdfBuilder** | | |
| `util/RecetaPdfBuilder.kt` | Modify | Extract PDF section builders |
| `util/RecetaPdfSections.kt` | Create | Extracted PDF page builders |
| **S1 — ViewModel Tests** | | |
| `viewmodel/SettingsViewModelTest.kt` | Create | 5+ characterization tests: initial state, load, load failure, update, update failure |
| `viewmodel/SubscriptionViewModelTest.kt` | Create | 5+ tests: initial state, active, expired, error, purchase action |
| **S2 — Sync Scheduler Tests** | | |
| `sync/PostSaveSyncSchedulerTest.kt` | Create | 5+ tests: scheduling, retry backoff, max retries, dedup, error propagation |
| **S3 — Resource Audit** | | |
| (no code changes) | None | Documentation only |

## Interfaces / Contracts

No new public interfaces. Extracted helpers remain `internal` or `private` within their package. Tests verify existing contracts via reflection.

PostSaveSyncScheduler mock contract (for tests):

```kotlin
// Injected dependencies — all mocked:
// CoroutineScope, SyncGate, SupabaseClient
// SyncPacientesUseCase, SyncHistorialUseCase
// SyncFinanzasUseCase, SyncInventarioUseCase
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit — ViewModel contracts | SettingsViewModel, SubscriptionViewModel method signatures + StateFlow fields | Java reflection, JUnit 4, no DI |
| Unit — Sync scheduler | PostSaveSyncScheduler scheduling, retry, dedup, error | MockK, Robolectric for Log, coroutines test |
| Regression — all | Existing tests must still pass after extraction + catch refactors | `./gradlew testDebugUnitTest` |
| Build — Compose BOM | Compilation after BOM bump | `./gradlew assembleDebug` |

### S1 Deliverables
- `SettingsViewModelTest.kt`: 5 tests — class exists, remindersEnabled/userTimeZone fields, setRemindersEnabled/setUserTimeZone method contracts
- `SubscriptionViewModelTest.kt`: 5 tests — tier/planCode fields, canAddPaciente contract, refreshPlanFromServer method

### S2 Deliverables
- `PostSaveSyncSchedulerTest.kt`: 5 tests — schedulePacientesSync enqueues job; transient failure retries with backoff; max retries = permanent failure; duplicate saves dedup; error propagated via Log

## Migration / Rollout

### Compose BOM update
1. Single version change in `gradle/libs.versions.toml`
2. Run `./gradlew assembleDebug` — if failures, scope-expand or revert
3. Run `./gradlew testDebugUnitTest`
4. **Rollback**: `git checkout gradle/libs.versions.toml`

### File extraction
1. Extract one file at a time in dependency order (no cross-dependencies)
2. Run `./gradlew assembleDebug` after each extraction
3. **Rollback**: `git checkout <original-file>` + `git rm <new-file>`

### Catch refactors
1. One file at a time, verify no new test breakage
2. **Rollback**: per-file `git checkout`

### New test files
1. Add tests, run them
2. **Rollback**: `git rm <test-file>`

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Compose BOM 2024.09.x breaks Compose APIs | Pin to 2024.06.00 as intermediate step; read changelog first |
| Extracted files miss imports | `import` same package; Kotlin auto-resolves same-package symbols |
| Catch refactor changes error UI behavior | Spec requires UiState.Error preserved exactly — verify same states set |
| PostSaveSyncScheduler tests flaky (timing) | Use `TestCoroutineDispatcher` / `StandardTestDispatcher`, advanceTimeBy for delay |
| Resource audit incomplete | Grep for `Resource<`, `Resource.Success`, `Resource.Error`, `Resource.Loading` — 64 matches already quantified |

## Open Questions

- None — all decisions resolved by spec requirements and existing codebase patterns.
