# Proposal: Remove Arqueo de Caja

## Intent

Remove the daily cash-count ("arqueo de caja") feature entirely. User explicitly requested removal: *"desapareceme todo lo que tenga relacion a arqueo de caja, no lo voy a utilizar, sinceramente me causa mas problemas que los pseudo beneficios que podria traer a futuro"*. The feature introduced data loss bugs (PR #47 audit: `createdAt` destroyed on download, `uploadServicios` marks deduplicated entities as synced) and adds sync complexity with zero current value.

## Scope

### In Scope

- **Delete** 11 files: Room entity, DAO, repo interface, ViewModel, PDF generator, UI composable, 5 test files, 1 upload test
- **Modify** 13 source files + 4 test files: remove arqueo references in `OptoDatabase`, `OptoRepository`, DI module, sync pipeline, CierreCaja screen/VM, DTOs, use cases, coordinators, sync VM, conflict screen, migrations
- **Delete** 2 Supabase migrations; **create** 1 new migration: `DROP TABLE IF EXISTS public.arqueo_caja CASCADE;`
- **Bump** Room version 35 → 36; new `MIGRATION_35_36`: `DROP TABLE IF EXISTS arqueo_caja`

### Out of Scope

- Historical migration files (25→26→27) remain intact — they're part of DB history
- Other features, specs, or capabilities not related to arqueo

## Capabilities

> No spec-level changes. Neither `cierre-caja` nor `sync` specs reference arqueo functionality.

### New Capabilities

None.

### Modified Capabilities

None.

## Approach

1. **TDD-first**: Delete arqueo test files first; update remaining test files to remove arqueo references
2. **Delete domain layer**: Entity, DAO, repo interface, DTO mappings, ViewModel, PDF generator
3. **Modify affected files**: Remove DI bindings, sync pipeline steps, CierreCaja references, conflict screen labels
4. **Database**: Create Room migration + Supabase migration (DROP TABLE)
5. **Build & test**: Verify compilation, run full test suite

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/arqueo/` | Removed | Entity, DAO, repo interface (3 files) |
| `viewmodel/ArqueoCajaViewModel.kt` | Removed | ViewModel |
| `util/ArqueoCajaPdfGenerator.kt` | Removed | PDF generation |
| `ui/components/cierre-caja/ArqueoSection.kt` | Removed | Composable section |
| `data/OptoDatabase.kt` | Modified | Remove entity/DAO, bump version |
| `data/OptoRepository.kt` | Modified | Remove arqueo methods + constructor param |
| `di/DatabaseModule.kt` | Modified | Remove DI bindings |
| `viewmodel/CierreCajaViewModel.kt` | Modified | Remove arqueo fields/methods |
| `ui/screens/CierreCajaScreen.kt` | Modified | Remove ArqueoSection + PDF button + arqueoVM |
| `domain/SyncFinanzasDto.kt` | Modified | Remove ArqueoCajaRemota, toRemota/toLocal, uploadedArqueos |
| `domain/SyncFinanzasUseCase.kt` | Modified | Remove arqueo upload/download |
| `data/upload/UploadSyncCoordinator.kt` | Modified | Remove uploadArqueos |
| `data/download/DownloadSyncCoordinator.kt` | Modified | Remove downloadArqueos |
| `data/delete/DeletionSyncHelper.kt` | Modified | Remove TABLE_ARQUEO_CAJA + mapping |
| `viewmodel/SyncViewModel.kt` | Modified | Remove arqueo_caja bump handling |
| `ui/screens/ConflictosScreen.kt` | Modified | Remove arqueo_caja type label |
| `data/OptoDatabaseMigrations.kt` | Modified | Comment update (historical migrations stay) |
| `test/` (arqueo) | Removed | 5 test files deleted |
| `test/CierreCajaViewModelTest.kt` | Modified | Remove arqueo tests |
| `test/SyncViewModelBumpCoverageTest.kt` | Modified | Remove bumpArqueoCaja test |
| `test/DeletionSyncHelperTest.kt` | Modified | Remove arqueo mapping tests |
| `supabase/migrations/20260617100000_add_arqueo_caja.sql` | Removed | Delete Supabase migration |
| `supabase/migrations/20260620000001_add_arqueo_diferencia_columns.sql` | Removed | Delete Supabase migration |
| `supabase/migrations/` (new) | Added | `DROP TABLE IF EXISTS public.arqueo_caja CASCADE;` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Compilation breakage from stale references | Low | Remove in dependency order (entity→dao→repo→VM→UI), verify build between steps |
| Supabase `arqueo_caja` table has production data | Low | DROP TABLE is destructive — user wants full removal, no data preservation needed |
| CierreCaja screen layout shifts | Low | ArqueoSection is at the bottom; removal will not affect other sections |
| RLS policies reference arqueo_caja | Low | Check via `supabase db lint` before removing table |

## Rollback Plan

1. **Code**: `git revert` the merge commit
2. **Database (Room)**: `MIGRATION_35_36` is destructive — restore via `git revert` and rebuild app
3. **Supabase**: Re-apply the two deleted migration files. Restore table from backup if production data exists

## Dependencies

None.

## Success Criteria

- [ ] Full test suite passes: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [ ] Debug build succeeds: `./gradlew :optoapp:assembleDebug`
- [ ] Zero references to `arqueo`, `ArqueoCaja`, `arqueo_caja` remain in production code
- [ ] Room DB version 36 with clean migration
- [ ] Supabase migration applied, `arqueo_caja` table dropped
- [ ] CierreCaja screen renders correctly without arqueo section
