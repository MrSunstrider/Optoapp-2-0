# Proposal: Critical Fixes

## Intent

Address three critical code quality issues in the Android app that threaten system stability and maintainability: (1) 57+ generic exception catches silently swallowing errors, (2) 27 Gson annotations needing migration to Kotlinx Serialization, and (3) four core repositories with 0% test coverage. These issues create hidden bugs, technical debt, and blind spots in business-critical code paths.

## Scope

### In Scope
- **C1: Generic Exception Handling** — Refactor all `catch (e: Exception)` in data/domain layers to catch specific exceptions, add consistent logging, and propagate via Result types where appropriate
- **C2: Gson to Kotlinx Migration** — Replace `@SerializedName` with `@SerialName`, remove `provideGson()` from DatabaseModule, update entity files (PacienteEntity, EvaluacionEntity, DispensacionEntity, OptoRepository data classes)
- **C3: Repository Test Coverage** — Add unit tests for MembershipRepository, OptoRepository, PacienteRepository, and DispensacionRepository

### Out of Scope
- ViewModel exception handling (deferred to separate cleanup)
- Other Gson usages in non-entity data classes
- Integration or instrumented tests for repositories
- Refactoring catch blocks in UI layer

## Capabilities

### New Capabilities
- None — this is a refactoring/quality improvement change

### Modified Capabilities
- None — no spec-level behavior changes

## Approach

### C1: Generic Exception Handling
Analyze each of the 57+ catch blocks, categorize exceptions (IOException, TimeoutException, etc.), and refactor to:
1. Catch specific subtypes
2. Add Timber/Logger calls at ERROR level with context
3. Wrap in Result<T> and propagate to callers for decision

Priority files: SyncFinanzasUseCase.kt (10 catches), MembershipRepository.kt (7 catches), SyncInventarioUseCase.kt (5 catches), SyncPacientesUseCase.kt (3 catches)

### C2: Gson to Kotlinx Migration
1. Add kotlinx-serialization-json dependency to build.gradle.kts
2. Replace @SerializedName with @SerialName in 4 entity files (27 instances)
3. Remove `provideGson()` from DatabaseModule.kt
4. Add @Serializable annotation to affected data classes
5. Verify Room and Retrofit still work with new annotations

### C3: Repository Test Coverage
Create unit tests using MockK and Robolectric (or standard JUnit with mocked dependencies):
- MembershipRepository: membership CRUD, optica membership sync
- OptoRepository: database operations, sync state management
- PacienteRepository: CRUD operations, observability
- DispensacionRepository: create/update workflows, stock operations

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/com/example/optoapp/domain/SyncFinanzasUseCase.kt` | Modified | 10 catch blocks to refine |
| `app/src/main/java/com/example/optoapp/data/MembershipRepository.kt` | Modified | 7 catch blocks + tests |
| `app/src/main/java/com/example/optoapp/data/OptoRepository.kt` | Modified | Gson migration + tests |
| `app/src/main/java/com/example/optoapp/data/PacienteRepository.kt` | Modified | Catches + tests |
| `app/src/main/java/com/example/optoapp/data/DispensacionRepository.kt` | Modified | Catches + tests |
| `app/src/main/java/com/example/optoapp/di/DatabaseModule.kt` | Modified | Remove provideGson() |
| `app/src/main/java/com/example/optoapp/data/paciente/PacienteEntity.kt` | Modified | @SerialName migration |
| `app/src/main/java/com/example/optoapp/data/evaluacion/EvaluacionEntity.kt` | Modified | @SerialName migration |
| `app/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt` | Modified | @SerialName migration |
| `build.gradle.kts` (root/app) | Modified | Add kotlinx-serialization |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Gson removal breaks Room/Retrofit at runtime | Medium | Test all entity serialization paths after migration |
| Repository tests require complex mocks, low ROI | Medium | Start with happy-path tests, expand iteratively |
| Exception refactoring changes error handling behavior | High | Verify existing error paths in UI layer still work |

## Rollback Plan

1. Revert build.gradle.kts changes to remove kotlinx-serialization
2. Restore @SerializedName in entity files
3. Re-add provideGson() in DatabaseModule.kt
4. Delete test files in repository test directories
5. Manually restore catch blocks if behavioral bugs appear

## Dependencies

- Kotlinx Serialization plugin compatible with current Kotlin version (check gradle.properties)
- MockK library already in test dependencies (verify build.gradle.kts)

## Success Criteria

- [ ] All 57+ generic catches in data/domain layers replaced with specific exception types
- [ ] All 27 @SerializedName annotations migrated to @SerialName
- [ ] provideGson() removed from DatabaseModule.kt
- [ ] MembershipRepository: ≥5 unit tests covering core operations
- [ ] OptoRepository: ≥5 unit tests covering database/sync operations
- [ ] PacienteRepository: ≥5 unit tests covering CRUD
- [ ] DispensacionRepository: ≥5 unit tests covering create/update flows
- [ ] All existing tests still pass after migration