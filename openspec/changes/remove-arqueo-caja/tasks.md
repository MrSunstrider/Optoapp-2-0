# Tasks: Remove Arqueo de Caja

> Based on [proposal.md](./proposal.md) and [design.md](./design.md).
> TDD-first: test-only changes before production changes. All phases sequential by dependency.

---

## Phase 1: Delete Standalone Test Files

_Safe to delete — no production code depends on test files._

### T1.1: Delete ArqueoCajaDaoTest.kt

Delete: `optoapp/src/test/java/com/example/optoapp/data/arqueo/ArqueoCajaDaoTest.kt`

**Verification**: File no longer exists.

---

### T1.2: Delete ArqueoCajaViewModelTest.kt

Delete: `optoapp/src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelTest.kt`

**Verification**: File no longer exists.

---

### T1.3: Delete ArqueoCajaViewModelHiltWiringTest.kt

Delete: `optoapp/src/test/java/com/example/optoapp/viewmodel/ArqueoCajaViewModelHiltWiringTest.kt`

**Verification**: File no longer exists.

---

### T1.4: Delete ArqueoCajaPdfGeneratorTest.kt

Delete: `optoapp/src/test/java/com/example/optoapp/util/ArqueoCajaPdfGeneratorTest.kt`

**Verification**: File no longer exists.

---

### T1.5: Delete UploadSyncCoordinatorArqueosTest.kt

Delete: `optoapp/src/test/java/com/example/optoapp/domain/UploadSyncCoordinatorArqueosTest.kt`

**Verification**: File no longer exists.

---

## Phase 2: Modify Shared Test Files

_Still no production code changed. Tests must still compile against existing production types._

### T2.1: Remove arqueo tests from CierreCajaViewModelTest.kt

**File**: `optoapp/src/test/java/com/example/optoapp/viewmodel/CierreCajaViewModelTest.kt`

**Action**: Remove lines 315–444 (arqueo-related tests: tests 4, 5, 7 per design — `arqueoForFecha` assertions, `observeArqueo` tests, etc.). Remove any `ArqueoCaja` imports no longer needed.

**Done condition**: `CierreCajaViewModelTest` compiles and contains zero `arqueo`/`ArqueoCaja` references.

---

### T2.2: Remove arqueo references from SyncViewModelBumpCoverageTest.kt

**File**: `optoapp/src/test/java/com/example/optoapp/viewmodel/SyncViewModelBumpCoverageTest.kt`

**Action**: Remove `bumpArqueoCaja_callsUpdateArqueo` test method and any arqueo helper/import references.

**Done condition**: File compiles and contains zero `arqueo`/`ArqueoCaja` references.

---

### T2.3: Remove arqueo mapping tests from DeletionSyncHelperTest.kt

**File**: `optoapp/src/test/java/com/example/optoapp/domain/DeletionSyncHelperTest.kt`

**Action**: Remove `entityTypeMapping_arqueoCaja` test and `TABLE_ARQUEO_CAJA`-related assertions.

**Done condition**: File compiles and contains zero `arqueo`/`ArqueoCaja` references.

---

### T2.4: Check for arqueo references in remaining sync test files

**Files**:
- `optoapp/src/test/java/com/example/optoapp/viewmodel/SyncViewModelConflictResolutionTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/SyncFinanzasUseCaseKtTest.kt`
- `optoapp/src/test/java/com/example/optoapp/viewmodel/SyncViewModelThreeWayMergeTest.kt`

**Action**: Grep each file for `arqueo`/`ArqueoCaja`. Remove any references found.

**Done condition**: All three files contain zero `arqueo`/`ArqueoCaja` references.

---

## Phase 3: Verify Tests Pass (Test-Only Changes)

### T3.1: Run unit tests — must pass

```sh
./gradlew :optoapp:testDebugUnitTest --stacktrace
```

**Done condition**: All tests pass. If any fail, fix before proceeding to Phase 4.

---

## Phase 4: Remove Production Code — Bottom-Up

_Entity → DAO → Repo Interface → ViewModel → UI → Sync → DI._

### T4.1: Remove arqueo domain files

**Files:**
- `optoapp/src/main/java/com/example/optoapp/data/arqueo/ArqueoCajaEntity.kt`
- `optoapp/src/main/java/com/example/optoapp/data/arqueo/ArqueoCajaDao.kt`
- `optoapp/src/main/java/com/example/optoapp/data/arqueo/IArqueoCajaRepo.kt`

**Verification**: Files no longer exist.

- [x] Done

---

### T4.2: Remove ArqueoCajaViewModel.kt

Delete: `optoapp/src/main/java/com/example/optoapp/viewmodel/ArqueoCajaViewModel.kt`

**Verification**: File no longer exists.

- [x] Done

---

### T4.3: Remove ArqueoSection.kt UI component

Delete: `optoapp/src/main/java/com/example/optoapp/ui/components/cierre-caja/ArqueoSection.kt`

**Verification**: File no longer exists.

- [x] Done

---

### T4.4: Remove ArqueoCajaPdfGenerator.kt

Delete: `optoapp/src/main/java/com/example/optoapp/util/ArqueoCajaPdfGenerator.kt`

**Verification**: File no longer exists.

- [x] Done

---

### T4.5: Remove arqueo methods from OptoRepository.kt

**File**: `optoapp/src/main/java/com/example/optoapp/data/OptoRepository.kt`

**Actions:**
- Remove `arqueoCajaDao` constructor parameter
- Remove `: IArqueoCajaRepo` from class declaration
- Remove all arqueo methods: `insertArqueo`, `updateArqueo`, `getArqueoById`, `upsertArqueoFromRemote`, `getArqueoByFecha`, `getArqueoByFechaSync`, `getArqueosByOpticaList`, `getArqueosByOptica`
- Remove related imports

**Done condition**: No `arqueo`/`ArqueoCaja`/`IArqueoCajaRepo` references remain in file.

- [x] Done

---

### T4.6: Remove arqueo from OptoDatabase.kt

**File**: `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt`

**Actions:**
- Remove `ArqueoCaja::class` from entities list
- Remove `abstract fun arqueoCajaDao()`
- Remove related imports

**Done condition**: No `arqueo`/`ArqueoCaja` references remain (version and migration changes handled in T5.1–T5.3).

- [x] Done

---

### T4.7: Remove arqueo DI bindings from DatabaseModule.kt

**File**: `optoapp/src/main/java/com/example/optoapp/di/DatabaseModule.kt`

**Actions:**
- Remove `provideArqueoCajaDao` binding
- Remove `arqueoCajaDao` parameter from `provideOptoRepository`
- Remove `arqueoCajaDao` argument in constructor call
- Remove `provideIArqueoCajaRepo` binding
- Remove related imports

**Done condition**: No `arqueo`/`ArqueoCaja`/`IArqueoCajaRepo` references remain.

- [x] Done

---

### T4.8: Remove arqueo from SyncFinanzasDto.kt

**File**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasDto.kt`

**Actions:**
- Remove `ArqueoCajaRemota` data class
- Remove `ArqueoCaja.toRemota()` extension
- Remove `ArqueoCajaRemota.toLocal()` extension
- Remove `uploadedArqueos` from result data class if present
- Remove related import

**Done condition**: No `ArqueoCajaRemota`/`ArqueoCaja`/`arqueo` references remain.

---

### T4.9: Remove uploadArqueos from UploadSyncCoordinator.kt

**File**: `optoapp/src/main/java/com/example/optoapp/domain/UploadSyncCoordinator.kt`

**Actions:**
- Remove `TABLE_ARQUEO_CAJA` constant
- Remove `uploadArqueos` method body and all callers

**Done condition**: No `arqueo`/`ArqueoCaja`/`TABLE_ARQUEO_CAJA` references remain.

---

### T4.10: Remove downloadArqueos from DownloadSyncCoordinator.kt

**File**: `optoapp/src/main/java/com/example/optoapp/domain/DownloadSyncCoordinator.kt`

**Actions:**
- Remove `TABLE_ARQUEO_CAJA` constant
- Remove `downloadArqueos` method body and `ArqueoCajaRemota` references

**Done condition**: No `arqueo`/`ArqueoCaja` references remain.

---

### T4.11: Remove arqueo steps from SyncFinanzasUseCase.kt

**File**: `optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasUseCase.kt`

**Actions:**
- Remove `arqueosUp` variable
- Remove arqueo upload call
- Remove arqueo download call
- Remove `uploadedArqueos = arqueosUp` from result
- Remove `downloadedArqueos` from `FinanzasSyncResult` if parameter exists

**Done condition**: No `arqueo`/`ArqueoCaja` references remain.

---

### T4.12: Remove arqueo from DeletionSyncHelper.kt

**File**: `optoapp/src/main/java/com/example/optoapp/domain/DeletionSyncHelper.kt`

**Actions:**
- Remove `TABLE_ARQUEO_CAJA` constant
- Remove `"arqueo_caja"` mapping branch

**Done condition**: No `arqueo`/`TABLE_ARQUEO_CAJA` references remain.

---

### T4.13: Remove arqueo from SyncViewModel.kt

**File**: `optoapp/src/main/java/com/example/optoapp/viewmodel/SyncViewModel.kt`

**Actions:**
- Remove `"arqueo_caja"` from entity type list
- Remove `bumpArqueoCaja` case block

**Done condition**: No `arqueo_caja`/`bumpArqueoCaja` references remain.

---

### T4.14: Remove arqueo from ConflictosScreen.kt

**File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/ConflictosScreen.kt`

**Action**: Remove `"arqueo_caja"` to `"Arqueo de caja"` mapping

**Done condition**: No `arqueo` references remain in file.

---

### T4.15: Remove arqueo from CierreCajaViewModel.kt

**File**: `optoapp/src/main/java/com/example/optoapp/viewmodel/CierreCajaViewModel.kt`

**Actions:**
- Remove `ArqueoCaja` import
- Remove `arqueoForFecha` from `CierreCajaUiState`
- Remove `_arqueoKey`
- Remove `observeArqueo()` call from `init`
- Remove `observeArqueo()` method body
- Remove `loadArqueoForDate` method
- Remove `observeArqueoForDate` method

**Done condition**: No `arqueo`/`ArqueoCaja` references remain in file.

---

### T4.16: Remove arqueo from CierreCajaScreen.kt

**File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/CierreCajaScreen.kt`

**Actions:**
- Remove `arqueoVM` parameter
- Remove `arqueoUiState` collect
- Remove `exportPdf` function
- Remove PDF `IconButton` from `topBar` actions
- Remove `LaunchedEffect` `observeArqueoForDate` call
- Remove `ArqueoSection` call at bottom
- Remove imports: `ArqueoCaja`, `ArqueoCajaViewModel`, `ArqueoCajaPdfGenerator`, `ArqueoSection`, `File`

**Done condition**: No `arqueo`/`ArqueoCaja`/`ArqueoSection` references remain in file.

---

## Phase 5: Database Migration (Room)

### T5.1: Bump Room version 35 → 36

**File**: `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt`

**Action**: Change `version = 35` to `version = 36`

**Verification**: `grep "version = 36"` returns the line.

---

### T5.2: Create MIGRATION_35_36

**File**: `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt`

**Action**: Add `val MIGRATION_35_36` with SQL: `DROP TABLE IF EXISTS arqueo_caja`

**Verification**: Migration constant exists and is registered in `.addMigrations()` call.

---

### T5.3: Update migration history comment

**File**: `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt`

**Action**: Remove `"Arqueo de caja"` from comment at line 11 (v20→v27 range).

**Verification**: Comment no longer references arqueo.

---

## Phase 6: Supabase

### T6.1: Delete arqueo_caja creation migration

Delete: `supabase/migrations/20260617100000_add_arqueo_caja.sql`

**Verification**: File no longer exists.

---

### T6.2: Delete arqueo_diferencia_columns migration

Delete: `supabase/migrations/20260620000001_add_arqueo_diferencia_columns.sql`

**Verification**: File no longer exists.

---

### T6.3: Create drop_arqueo_caja migration

Create: `supabase/migrations/20260707000000_drop_arqueo_caja.sql`

**Content:**
```sql
DROP TABLE IF EXISTS public.arqueo_caja CASCADE;
```

**Verification**: File exists with the DROP TABLE statement.

---

## Phase 7: Final Verification

### T7.1: Run full unit test suite

```sh
./gradlew :optoapp:testDebugUnitTest --stacktrace
```

**Done condition**: All tests pass.

---

### T7.2: Assemble debug APK

```sh
./gradlew :optoapp:assembleDebug
```

**Done condition**: Build succeeds without errors.

---

### T7.3: Grep for `arqueo` in production source

```sh
rg -i "arqueo" optoapp/src/main/
```

**Done condition**: Zero results (except migration history comments in `OptoDatabaseMigrations.kt` which reference arqueo only as historical context).

---

### T7.4: Grep for `Arqueo` in all Kotlin files

```sh
rg -i "Arqueo" --include "*.kt" optoapp/src/
```

**Done condition**: Zero results in production and test source.

---

## Success Gates

| Gate | Phase | Criteria |
|------|-------|----------|
| G1 | Phase 3 (T3.1) | All tests pass after Phase 1+2 test modifications |
| G2 | Phase 7 (T7.1) | All tests pass after production removal + DB migrations |
| G3 | Phase 7 (T7.2) | Debug APK builds without errors |
| G4 | Phase 7 (T7.3–T7.4) | Zero `arqueo`/`ArqueoCaja`/`arqueo_caja` references remain in production code |

> **Failure recovery**: If a task causes compilation/test failure, fix before proceeding to the next task. After Phase 2 test changes, do NOT proceed to Phase 4 until T3.1 passes.
