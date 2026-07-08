# Design: Remove Arqueo de Caja

## Technical Approach

Remove the arqueo_caja feature by deleting all dedicated files and stripping arqueo references from shared code, following bottom-up dependency order (entity → DAO → repo → VM → UI → sync → DI). TDD-first: delete or modify tests before production code. Room DB v35→36 with DROP TABLE; Supabase DROP TABLE migration.

## Architecture Decisions

### Decision: Destructive Room Migration

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `DROP TABLE IF EXISTS arqueo_caja` (v35→36) | Destructive — data lost. Simplest path. User wants full removal. | **Chosen** |
| `DROP TABLE` in migration + comment-out entity | Equivalent result. More boilerplate. | Rejected — same effect |

`MIGRATION_25_26` and `MIGRATION_26_27` **stay** — they created `arqueo_caja` as part of migration history. Fresh installs at v36 never create the table.

### Decision: Keep Deleted Data Deletable From Supabase

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Remove arqueo from `DeletionSyncHelper` mapping | Orphaned remote arqueos never deleted. | **Chosen** |
| Keep arqueo in `DeletionSyncHelper` | Code still references deleted type. | Rejected — full removal |

Pending arqueo deletions in the local tombstone queue will be silently skipped (mapped to `null` → `clearDeletionState` without remote DELETE). This is acceptable since the supabase table is also being dropped.

### Decision: Remove `arqueoForFecha` From CierreCajaUiState

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Remove `arqueoForFecha` entirely | CierreCajaScreen loses arqueo display. User wants no arqueo. | **Chosen** |
| Keep field but always null | Dead code. Confuses future readers. | Rejected |

After removal, CierreCajaScreen becomes a pure read-only report (pagos + ventas del día). No PDF export, no "Cerrar Día", no arqueo section.

## Removal Dependency Order

```
Phase 1 — Delete dedicated test files (safe — no deps)
  ArqueoCajaDaoTest.kt
  ArqueoCajaViewModelTest.kt
  ArqueoCajaViewModelHiltWiringTest.kt
  ArqueoCajaPdfGeneratorTest.kt
  UploadSyncCoordinatorArqueosTest.kt

Phase 2 — Modify shared test files (remove arqueo references)
  CierreCajaViewModelTest.kt        → remove tests 4, 5, 7
  SyncViewModelBumpCoverageTest.kt  → remove bumpArqueoCaja_callsUpdateArqueo
  DeletionSyncHelperTest.kt         → remove entityTypeMapping_arqueoCaja test + TABLE_ARQUEO_CAJA assertion

Phase 3 — Delete production files (bottom-up)
  3a. data/arqueo/ArqueoCajaEntity.kt
  3b. data/arqueo/ArqueoCajaDao.kt
  3c. data/arqueo/IArqueoCajaRepo.kt
  3d. viewmodel/ArqueoCajaViewModel.kt
  3e. util/ArqueoCajaPdfGenerator.kt
  3f. ui/components/cierre-caja/ArqueoSection.kt

Phase 4 — Modify shared production files
  4a. data/OptoDatabase.kt
      - Remove ArqueoCaja::class from entities list (line 43)
      - Remove abstract fun arqueoCajaDao() (line 72)
      - Bump version 35→36 (line 51)
      - Add MIGRATION_35_36 to .addMigrations() call (line 122)
      - Add val MIGRATION_35_36 get() = ... to companion (after line 113)
      - Remove import lines 9-10
  4b. data/OptoDatabaseMigrations.kt
      - Add val MIGRATION_35_36 at end: DROP TABLE IF EXISTS arqueo_caja
      - Update comment header (line 11: s/v20→v27/v20→v26/, remove "Arqueo de caja")
  4c. data/OptoRepository.kt
      - Remove arqueoCajaDao constructor param (line 47)
      - Remove : IArqueoCajaRepo (line 53)
      - Remove all arqueo methods (lines 186-238)
      - Remove imports lines 4-6
  4d. di/DatabaseModule.kt
      - Remove provideArqueoCajaDao binding (line 110)
      - Remove arqueoCajaDao param from provideOptoRepository (line 213)
      - Remove arqueoCajaDao arg in constructor call (line 229)
      - Remove provideIArqueoCajaRepo binding (line 274)
      - Remove imports lines 5-6
  4e. viewmodel/CierreCajaViewModel.kt
      - Remove import line 10 (ArqueoCaja)
      - Remove arqueoForFecha from CierreCajaUiState (line 44)
      - Remove _arqueoKey (line 73)
      - Remove observeArqueo() call from init (line 83)
      - Remove observeArqueo() method body (lines 86-96)
      - Remove loadArqueoForDate method (lines 178-179)
      - Remove observeArqueoForDate method (lines 181-183)
  4f. ui/screens/CierreCajaScreen.kt
      - Remove arqueoVM parameter (line 45)
      - Remove arqueoUiState collect (line 48)
      - Remove exportPdf function (lines 71-82)
      - Remove PDF IconButton from topBar actions (lines 96-100)
      - Remove LaunchedEffect observeArqueoForDate call (lines 57-59)
      - Remove ArqueoSection call at bottom (lines 244-256)
      - Remove imports: ArqueoCaja (line 24), ArqueoCajaViewModel (line 25), ArqueoCajaPdfGenerator (line 28), ArqueoSection (line 33), File (line 36)
  4g. domain/SyncFinanzasDto.kt
      - Remove ArqueoCajaRemota data class (lines 425-447)
      - Remove ArqueoCaja.toRemota() (lines 449-471)
      - Remove ArqueoCajaRemota.toLocal() (lines 473-496)
      - Remove import line 8
  4h. domain/SyncFinanzasUseCase.kt
      - Remove arqueosUp variable (line 49)
      - Remove arqueo upload call (lines 64-65)
      - Remove arqueo download call (lines 76-77)
      - Remove uploadedArqueos = arqueosUp from result (line 110)
      - Remove downloadedArqueos arg from FinanzasSyncResult (if parameter exists)
  4i. domain/UploadSyncCoordinator.kt
      - Remove TABLE_ARQUEO_CAJA constant (line 34)
      - Remove uploadArqueos method body and calls to syncStateTracker
  4j. domain/DownloadSyncCoordinator.kt
      - Remove TABLE_ARQUEO_CAJA constant (line 31)
      - Remove downloadArqueos method body and ArqueoCajaRemota references
  4k. domain/DeletionSyncHelper.kt
      - Remove TABLE_ARQUEO_CAJA constant (line 27)
      - Remove "arqueo_caja" mapping branch (line 42)
  4l. viewmodel/SyncViewModel.kt
      - Remove "arqueo_caja" from entity type list (line 168)
      - Remove bumpArqueoCaja case block (lines 356-361)
  4m. ui/screens/ConflictosScreen.kt
      - Remove "arqueo_caja" to "Arqueo de caja" mapping (line 41)

Phase 5 — Database
  5a. Add MIGRATION_35_36: DROP TABLE IF EXISTS arqueo_caja
  5b. Build triggers auto-schema JSON regeneration at v36

Phase 6 — Supabase
  6a. Delete: supabase/migrations/20260617100000_add_arqueo_caja.sql
  6b. Delete: supabase/migrations/20260620000001_add_arqueo_diferencia_columns.sql
  6c. Create: supabase/migrations/20260707000000_drop_arqueo_caja.sql
      → DROP TABLE IF EXISTS public.arqueo_caja CASCADE;

Phase 7 — Verify
  7a. ./gradlew :optoapp:testDebugUnitTest --stacktrace
  7b. ./gradlew :optoapp:assembleDebug
  7c. Grep for zero remaining arqueo/ArqueoCaja/arqueo_caja references
```

## Data Flow

```
    ┌─────────────────────────────────────────────────────┐
    │                   Before removal                     │
    │                                                     │
    │  ArqueoSection ──→ ArqueoCajaVM ──→ IArqueoCajaRepo │
    │       │                              ↓              │
    │       │                       OptoRepository        │
    │       │                              ↓              │
    │       │                       ArqueoCajaDao         │
    │       │                              ↓              │
    │       └── PDF generator ←─── Room: arqueo_caja      │
    │                                                     │
    │  Sync: UploadSyncCoordinator.uploadArqueos()        │
    │        DownloadSyncCoordinator.downloadArqueos()    │
    │        SyncFinanzasUseCase (upload+download steps)  │
    │        DeletionSyncHelper (TABLE_ARQUEO_CAJA)       │
    │        SyncViewModel (bumpArqueoCaja)               │
    └─────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────┐
    │                    After removal                     │
    │                                                     │
    │  CierreCajaScreen                                   │
    │    ├── Pagos del día section           (unchanged)  │
    │    ├── Ventas del día section          (unchanged)  │
    │    └── Totals display                  (unchanged)  │
    │                                                     │
    │  Sync pipeline: arqueo steps removed entirely       │
    └─────────────────────────────────────────────────────┘
```

## File Changes Summary

| File | Action | Lines |
|------|--------|-------|
| `optoapp/src/main/java/.../data/arqueo/ArqueoCajaEntity.kt` | Delete | 35 |
| `optoapp/src/main/java/.../data/arqueo/ArqueoCajaDao.kt` | Delete | 40 |
| `optoapp/src/main/java/.../data/arqueo/IArqueoCajaRepo.kt` | Delete | 5 |
| `optoapp/src/main/java/.../viewmodel/ArqueoCajaViewModel.kt` | Delete | 152 |
| `optoapp/src/main/java/.../util/ArqueoCajaPdfGenerator.kt` | Delete | 114 |
| `optoapp/src/main/java/.../ui/components/cierre-caja/ArqueoSection.kt` | Delete | 207 |
| `optoapp/src/main/java/.../data/OptoDatabase.kt` | Modify | Remove entity+dao, v35→36 |
| `optoapp/src/main/java/.../data/OptoDatabaseMigrations.kt` | Modify | Add MIGRATION_35_36 |
| `optoapp/src/main/java/.../data/OptoRepository.kt` | Modify | Remove ~52 arqueo lines |
| `optoapp/src/main/java/.../di/DatabaseModule.kt` | Modify | Remove 3 bindings |
| `optoapp/src/main/java/.../viewmodel/CierreCajaViewModel.kt` | Modify | Remove arqueo fields/methods |
| `optoapp/src/main/java/.../ui/screens/CierreCajaScreen.kt` | Modify | Remove arqueoVM + PDF + ArqueoSection |
| `optoapp/src/main/java/.../domain/SyncFinanzasDto.kt` | Modify | Remove ArqueoCajaRemota + mappers |
| `optoapp/src/main/java/.../domain/SyncFinanzasUseCase.kt` | Modify | Remove arqueo steps |
| `optoapp/src/main/java/.../domain/UploadSyncCoordinator.kt` | Modify | Remove uploadArqueos |
| `optoapp/src/main/java/.../domain/DownloadSyncCoordinator.kt` | Modify | Remove downloadArqueos |
| `optoapp/src/main/java/.../domain/DeletionSyncHelper.kt` | Modify | Remove TABLE_ARQUEO_CAJA |
| `optoapp/src/main/java/.../viewmodel/SyncViewModel.kt` | Modify | Remove arqueo bump case |
| `optoapp/src/main/java/.../ui/screens/ConflictosScreen.kt` | Modify | Remove arqueo mapping |
| `optoapp/src/test/java/.../data/arqueo/ArqueoCajaDaoTest.kt` | Delete | Full |
| `optoapp/src/test/java/.../viewmodel/ArqueoCajaViewModelTest.kt` | Delete | Full |
| `optoapp/src/test/java/.../viewmodel/ArqueoCajaViewModelHiltWiringTest.kt` | Delete | Full |
| `optoapp/src/test/java/.../util/ArqueoCajaPdfGeneratorTest.kt` | Delete | Full |
| `optoapp/src/test/java/.../domain/UploadSyncCoordinatorArqueosTest.kt` | Delete | Full |
| `optoapp/src/test/java/.../viewmodel/CierreCajaViewModelTest.kt` | Modify | Remove 3 arqueo tests |
| `optoapp/src/test/java/.../viewmodel/SyncViewModelBumpCoverageTest.kt` | Modify | Remove arqueo test + helper |
| `optoapp/src/test/java/.../domain/DeletionSyncHelperTest.kt` | Modify | Remove arqueo assertions |
| `supabase/migrations/20260617100000_add_arqueo_caja.sql` | Delete | Supabase migration |
| `supabase/migrations/20260620000001_add_arqueo_diferencia_columns.sql` | Delete | Supabase migration |
| `supabase/migrations/20260707000000_drop_arqueo_caja.sql` | Create | DROP TABLE CASCADE |

## Testing Strategy

| Phase | What | Command |
|-------|------|---------|
| Phase 2 → 3 (after test changes) | Verify remaining tests pass without arqueo | `./gradlew :optoapp:testDebugUnitTest --stacktrace` |
| Phase 7 (final) | Full test suite + build | `./gradlew :optoapp:testDebugUnitTest --stacktrace && ./gradlew :optoapp:assembleDebug` |
| Post-removal | Zero arqueo references grep | `rg -i "arqueo" optoapp/src/main/` should return 0 |

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Test phase fails after modifying shared test files | Don't proceed to Phase 3 (production delete) until Phase 2 tests pass |
| Compilation breakage from missed reference | `rg -i "arqueo"` after each Phase 4 file edit; Kotlin compiler catches mismatches on build |
| Sync pipeline still references removed types | `rg -i "ArqueoCajaRemota\|ArqueoCaja\|arqueo_caja" optoapp/src/main/` before build |
| CierreCajaScreen layout broken | ArqueoSection is the last composable at bottom; removal leaves column intact |
| Supabase table has RLS policies referencing it | `DROP TABLE ... CASCADE` removes dependent objects; validate with `supabase db lint` before applying |

## Rollback Plan

1. **Code**: `git revert` the merge commit
2. **Room**: `MIGRATION_35_36` is destructive — restore via `git revert` and rebuild; devices on v36 that never had v35 cannot revert (need clean reinstall)
3. **Supabase**: Re-apply the two deleted migration files; restore table from backup if production data exists
