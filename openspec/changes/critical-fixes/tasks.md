# Tasks: Critical Fixes

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 750-950 |
| 400-line budget risk | **High** |
| Chained PRs recommended | **Yes** |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |
| Decision needed before apply | No |

### Suggested Work Units

| Unit | Goal | Files | Est. Lines | PR |
|------|------|-------|------------|-----|
| 1 | Serialization migration + deps | 8 files | ~180 | PR #1 |
| 2 | Exception handling (Data layer) | 4 repos | ~120 | PR #2 |
| 3 | Exception handling (Domain layer) | 6 use cases/helpers | ~130 | PR #3 |
| 4 | Repository tests + MockK | 4 test files, build files | ~420 | PR #4 |

**Total**: ~850 lines across 4 PRs

---

## Phase 1: Serialization Migration (Work Unit 1)

Dependencies: None (can start immediately)

- [x] 1.1 Update `gradle/libs.versions.toml`: add `kotlinx-serialization-json` and `mockk` version entries
- [x] 1.2 Update `app/build.gradle.kts`: add `kotlinx-serialization-json` and `mockk` test dependency, verify kotlinx-serialization plugin. **Note**: Kept `implementation(libs.gson)` — Converters.kt and BackupImportValidator.kt still need it; spec says SHOULD remove only if no other code references it.
- [x] 1.3 Modify `data/paciente/PacienteEntity.kt`: replace `@SerializedName` with `@SerialName` (3 fields), add `@Serializable`, add LocalDate serializer annotations
- [x] 1.4 Modify `data/evaluacion/EvaluacionEntity.kt`: replace `@SerializedName` with `@SerialName` (4 fields), add `@Serializable`, add LocalDate serializer annotations
- [x] 1.5 Modify `data/dispensacion/DispensacionEntity.kt`: replace `@SerializedName` with `@SerialName` (14 annotations across 5 classes), add `@Serializable`, add LocalDate serializer annotations
- [x] 1.6 Modify `data/OptoRepository.kt`: replace `@SerializedName` with `@SerialName` on `BackupData` (6 fields), add `@Serializable`, create `BackupDataSerializer` in same file
- [x] 1.7 Create `BackupDataSerializer`: implements `KSerializer<BackupData>` that tries alternate names (`ordenes`/`ventas` → `dispensaciones`, `servicios`/`otrosServicios` → `serviciosExtra`) using JsonObject key patching
- [x] 1.8 Modify `di/DatabaseModule.kt`: remove `provideGson()` function, add `provideBackupJson()` that returns `Json { ignoreUnknownKeys = true; encodeDefaults = true }`
- [x] 1.9 Modify `viewmodel/auth/BackupDelegate.kt`: replace `gson.toJson(...)` with `backupJson.encodeToString(...)`, update constructor to inject `Json` instead of `Gson`
- [x] 1.10 Write unit test `BackupDataSerializationTest`: 8 tests covering roundtrip (3), backward compat with old keys (4), and primary key precedence (1)

---

## Phase 2: Exception Handling — Data Layer (Work Unit 2)

Dependencies: Phase 1 (build integrity)

- [ ] 2.1 Modify `data/MembershipRepository.kt`: refactor 7+ generic `catch (e: Exception)` → specific catches (`IOException`, `PostgrestException`) with `rethrowIfCancellation()`, `Log.e(TAG, context, e)`, and `Result.failure(e)`
- [ ] 2.2 Modify `data/PacienteRepository.kt`: refactor 2 generic catches → typed catches (`IOException`, `DatabaseException`) with logging and Result propagation
- [ ] 2.3 Modify `data/DispensacionRepository.kt`: refactor 2 generic catches → typed catches with logging and Result propagation
- [ ] 2.4 Modify `data/OptoRepository.kt`: refactor 6 generic catches in `restoreBackup()` → typed catches with `Log.e()` (no Result needed, fire-and-forget loop)

---

## Phase 3: Exception Handling — Domain Layer (Work Unit 3)

Dependencies: Phase 2 (data layer patterns established)

- [ ] 3.1 Modify `domain/SyncFinanzasUseCase.kt`: refine 14 catch locations with specific exception types (`IOException`, `TimeoutException`, `JsonParseException`), add `rethrowIfCancellation()`, structured logging with operation context
- [ ] 3.2 Modify `domain/SyncInventarioUseCase.kt`: refactor 6 generic catches → typed catches with logging and Result propagation
- [ ] 3.3 Modify `domain/SyncPacientesUseCase.kt`: refactor 4 generic catches → typed catches with logging and Result propagation
- [ ] 3.4 Modify `domain/SyncHistorialUseCase.kt`: refactor 4 generic catches → typed catches with logging and Result propagation
- [ ] 3.5 Modify `domain/SyncSessionHelper.kt`: refactor 1 generic catch → typed catch with logging
- [ ] 3.6 Modify `domain/command/CommandPatterns.kt`: refactor 1 generic catch → typed catch with logging
- [ ] 3.7 Modify `domain/sync/strategies/DefaultStrategies.kt`: refactor 1 generic catch → typed catch with logging

---

## Phase 4: Repository Test Coverage (Work Unit 4)

Dependencies: Phase 1-3 (all code changes complete)

- [ ] 4.1 Create `data/MembershipRepositoryTest.kt` with MockK:
  - Test `createMembership()` success path
  - Test `getMembership(id)` returns correct data
  - Test `updateMembership()` persists changes
  - Test `deleteMembership()` removes record
  - Test `syncOpticaMembership()` network error returns `Result.failure()`
- [ ] 4.2 Create `data/OptoRepositoryTest.kt` with MockK:
  - Test `insertOpto()` persists data
  - Test `getOptoById()` returns correct optician
  - Test `updateSyncState()` updates timestamp
  - Test sync handles server 500 error without updating state
  - Test `clearAll()` removes all records
- [ ] 4.3 Create `data/PacienteRepositoryTest.kt` with Room in-memory:
  - Test `createPaciente()` persists and returns with ID
  - Test `getPacienteById()` returns correct patient
  - Test `updatePaciente()` updates fields
  - Test `deletePaciente()` removes record
  - Test `observePacientes()` Flow emits on changes
- [ ] 4.4 Create `data/DispensacionRepositoryTest.kt` with Room in-memory:
  - Test `createDispensacion()` persists with items and decrements stock
  - Test `updateStatus()` updates status and timestamp
  - Test stock decrement on dispensacion creation
  - Test insufficient stock returns failure without modifying stock
  - Test `getByDateRange()` filters correctly

---

## Implementation Notes

### Exception Handling Pattern
```kotlin
try {
    operation()
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Log.e(TAG, "operationName: context", e)
    Result.failure(e)
}
```

### Test Naming Convention
Use descriptive camelCase method names per spec: `createMembershipSuccessfully`, `networkErrorDuringSyncReturnsFailure`

### BackupDataSerializer Approach
Custom `KSerializer<BackupData>` that:
1. Tries primary `@SerialName` first
2. Falls back to alternate keys for backward compatibility
3. Handles: `dispensaciones` ← `ordenes`, `ventas` and `serviciosExtra` ← `servicios`, `otrosServicios`
