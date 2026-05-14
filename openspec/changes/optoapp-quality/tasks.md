# Tasks: optoapp-quality

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~650-750 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: W1 file extractions → PR 2: W2 UI tests → PR 3: W3 JaCoCo threshold |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| W1 | Split 4 large files | PR 1 | 4 extractions; tests must pass after each |
| W2 | UI tests | PR 2 | Compose tests for 3 screens; mocks in same commit |
| W3 | Jacoco threshold | PR 3 | One-line Gradle change; CI enforcement |

---

## Design: W1 — File Extraction

### SyncFinanzasUseCase → Extract 3 Helpers
| Helper | Responsibility |
|--------|----------------|
| `DeletionSyncHelper` | `pushPendingDeletions()` + `deletedIds()` |
| `UploadSyncCoordinator` | `uploadDispensaciones/Servicios/Pagos()` |
| `DownloadSyncCoordinator` | `downloadDispensaciones/Servicios/Pagos()` |
| `NetworkRetryHelper` | `retryNetwork()` + `isTransientNetworkError()` |

Keep UseCase as facade; inject helpers via constructor.

### MembershipRepository → Extract 3 Data Sources
| Source | Methods |
|--------|---------|
| `MembershipDataSource` | `fetchMembershipsForCurrentUser()`, `fetchMembersForOptica()`, `assignRoleByEmail()` |
| `OpticaSettingsDataSource` | `fetch/createOptica`, `fetchPlanSettings`, `fetchFiscal/Lab/HeaderSettings`, updates |
| `OpticaQueryHelper` | `fetchOpticaNombre()` (internal) |

### OptoRepository → Extract 3 Coordinators
| Coordinator | Handles |
|-------------|---------|
| `SyncSnapshotCoordinator` | All `get*SnapshotForOptica()` methods |
| `BackupRestoreCoordinator` | `getBackupDataForOptica()`, `restoreBackup()`, `withDefaults()` helpers |
| `MonturaInventoryCoordinator` | `getMonturasByOptica()` through `insertMonturaMovimiento()` |

Note: Repository already delegates Paciente/Evaluacion/Dispensacion/Pagos/Servicios to sub-repos.

### ConfiguracionScreen → Extract Composables
| Section | Extract To |
|---------|------------|
| Dialog state | Keep in screen; too coupled |
| Activity launchers | Keep; need `scope` and `context` |
| `SecuritySection` | Already extracted ✅ |
| `SystemSection` | Already extracted ✅ |
| `LaboratorySection` | Already extracted ✅ |
| `FiscalDataSection` | Already extracted ✅ |
| `PlanManagementSection` | Already extracted ✅ |
| `UsuariosRolesSection` | Already extracted ✅ |
| `SucursalesSection` | Already extracted ✅ |
| `SyncDiagnosticsCard` | Already extracted ✅ |
| `DataManagementCard` | Already extracted ✅ |

Screen state (labNombre, labContacto, pin fields, dialog state) → Move to dedicated `ConfiguracionViewModel` to reduce screen line count.

---

## Phase 1: W1 — Large File Extraction

- [ ] 1.1 Create `domain/sync/DeletionSyncHelper.kt` with `pushPendingDeletions()` + `deletedIds()`
- [ ] 1.2 Create `domain/sync/UploadSyncCoordinator.kt` with upload methods
- [ ] 1.3 Create `domain/sync/DownloadSyncCoordinator.kt` with download methods
- [ ] 1.4 Create `domain/sync/NetworkRetryHelper.kt` with retry logic
- [ ] 1.5 Refactor `SyncFinanzasUseCase.kt` to inject helpers; reduce to <250 lines
- [ ] 1.6 Run tests: `./gradlew :optoapp:testDebugUnitTest`
- [ ] 1.7 Create `data/membership/MembershipDataSource.kt` with membership methods
- [ ] 1.8 Create `data/membership/OpticaSettingsDataSource.kt` with settings methods
- [ ] 1.9 Create `data/membership/OpticaQueryHelper.kt` for internal queries
- [ ] 1.10 Refactor `MembershipRepository.kt` to delegate to sources; reduce to <250 lines
- [ ] 1.11 Run tests
- [ ] 1.12 Create `data/sync/SyncSnapshotCoordinator.kt` with snapshot methods
- [ ] 1.13 Create `data/backup/BackupRestoreCoordinator.kt` with backup/restore logic
- [ ] 1.14 Create `data/montura/MonturaInventoryCoordinator.kt` with montura methods
- [ ] 1.15 Refactor `OptoRepository.kt` to inject coordinators; reduce to <250 lines
- [ ] 1.16 Run tests
- [ ] 1.17 Create `presentation/config/ConfiguracionViewModel.kt` with screen state
- [ ] 1.18 Refactor `ConfiguracionScreen.kt` to use new ViewModel; reduce to <250 lines
- [ ] 1.19 Run tests

## Phase 2: W2 — UI Tests

- [ ] 2.1 Create `app/src/androidTest/.../LoginScreenTest.kt` with: happy path, error state, loading state
- [ ] 2.2 Create `app/src/androidTest/.../MainDrawerScreenTest.kt` with: navigation, drawer items, selection
- [ ] 2.3 Create `app/src/androidTest/.../NuevaDispensacionScreenTest.kt` with: form validation, save flow, error handling
- [ ] 2.4 Add required test dependencies in `build.gradle.kts` if missing
- [ ] 2.5 Run UI tests locally before commit

## Phase 3: W3 — JaCoCo Threshold

- [ ] 3.1 Add `minimum = 0.50` to JaCoCo configuration in `build.gradle.kts`
- [ ] 3.2 Verify CI fails when threshold breached (test by temporarily lowering)
- [ ] 3.3 Document threshold in team wiki/README

## Phase 4: Verification & Cleanup

- [ ] 4.1 Verify all 4 files <250 lines: `find . -name "*.kt" -exec wc -l {} \;`
- [ ] 4.2 Run full test suite: `./gradlew :optoapp:testDebugUnitTest`
- [ ] 4.3 Check JaCoCo report generates with threshold enforced
- [ ] 4.4 Update CHANGELOG.md with refactor summary
