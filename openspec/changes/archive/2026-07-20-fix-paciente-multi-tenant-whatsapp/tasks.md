# Tasks: Fix Paciente Multi-Tenant WhatsApp Templates

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~400-450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

## Phase 1: Data Layer — Entity, DAO, Migration

- [x] 1.1 RED: Write `OpticaSettingsDaoTest` — upsert & query by opticaId, following `ConfiguracionFinancieraDaoTest` pattern (Room in-memory DB, Robolectric)
- [x] 1.2 GREEN: Create `data/opticasettings/OpticaSettingsEntity.kt` (`@Entity tableName="optica_settings"`, `opticaId` PK, `configJson: String = "{}"`)
- [x] 1.3 GREEN: Create `data/opticasettings/OpticaSettingsDao.kt` — `getByOpticaId(Flow)`, `getByOpticaIdOnce(suspend)`, `upsert(suspend)`
- [x] 1.4 RED: Add v41→v42 migration test to `OptoDatabaseMigrationTest` — verify `optica_settings` table columns exist post-migration
- [x] 1.5 GREEN: Add `MIGRATION_41_42` to `OptoDatabaseMigrations.kt` (CREATE TABLE IF NOT EXISTS), bump OptoDatabase version 41→42, register entity + DAO method + re-export in database, add to migration chain
- [x] 1.6 GREEN: Add `OpticaSettingsDao` provider to `DatabaseModule.kt`

## Phase 2: Data Layer — DataSource & Repository

- [x] 2.1 RED: Write `OpticaSettingsDataSourceTest` — mock Supabase `optica_settings` response, verify `fetchOpticaSettings` returns entity with `configJson` containing `business_hours`
- [x] 2.2 GREEN: Add `fetchOpticaSettings(opticaId): OpticaSettingsEntity?` to `OpticaSettingsDataSource` — reads `optica_settings` table from Supabase via postgrest
- [x] 2.3 GREEN: In `MembershipRepository`, inject `OpticaSettingsDao`, add `getOpticaSettingsFlow(opticaId): Flow<OpticaSettingsEntity?>`, `fetchOpticaSettings(opticaId)`, `upsertOpticaSettings(OpticaSettingsEntity)`

## Phase 3: ViewModel — OpticaHeaderViewModel

- [x] 3.1 RED: Write `OpticaHeaderViewModelTest` — fake `OpticaSettingsDao` returns `configJson` with `business_hours`, assert `uiState.value.horarioAtencion` reflects it; null/missing → blank fallback
- [x] 3.2 GREEN: Add `horarioAtencion: String = ""` to `OpticaHeaderUi` data class; inject `MembershipRepository`/DAO in ViewModel, combine optica name + extracted `business_hours` from `configJson` into uiState

## Phase 4: UI — PacienteWhatsAppActions

- [x] 4.1 RED: Write `PacienteWhatsAppActionsTest` — unit tests for message generation logic; empty name → `"Su óptica"`; empty hours → sentence omitted; non-affected templates unchanged
- [x] 4.2 GREEN: Add `nombreOptica: String` + `horarioAtencion: String = ""` to `PacienteWhatsAppMenu` signature; replace hardcoded strings with `$nombreOptica` / `$horarioAtencion`; conditionally include hours sentence; use `buildString` for composition

## Phase 5: UI Wiring — DetallePacienteScreen

- [x] 5.1 GREEN: Pass `opticaHeaderVm.uiState.value.nombreOptica` + `.horarioAtencion` to `PacienteWhatsAppMenu` call site in `DetallePacienteScreen`

## Phase 6: Verification

- [x] 6.1 GREEN: Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — all tests pass
