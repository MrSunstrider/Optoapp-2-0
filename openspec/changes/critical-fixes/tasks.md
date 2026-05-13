# Tasks: Critical Fixes — Exception Handling (Phase 3)

## Phase 3: Exception Handling — Domain Layer

Refactor generic `catch (e: Exception)` blocks to differentiate between:
- `CancellationException` → rethrow immediately
- `IOException` → log as "Error en red" with full stack trace
- Other `Exception` → log as "Error inesperado" with full stack trace

### Test Infrastructure
- [x] 3.0 Create `SyncErrorHandler.kt` with `errorLabelForException` utility function
- [x] 3.0t Write `SyncErrorHandlerTest.kt` with tests for utility function

### SyncFinanzasUseCase.kt (11 catch blocks)
- [x] 3.1 Refactor `invoke()` catch — top-level error handling
- [x] 3.2 Refactor `pushPendingDeletions()` catch — deletion error
- [x] 3.3 Refactor `uploadDispensaciones()` remote lookup catch — OT reconciliation
- [x] 3.4 Refactor `uploadDispensaciones()` upsert catch — batch error
- [x] 3.5 Refactor `uploadServicios()` remote lookup catch — OT reconciliation
- [x] 3.6 Refactor `uploadServicios()` upsert catch — batch error
- [x] 3.7 Refactor `uploadPagos()` upsert catch — batch error
- [x] 3.8 Refactor `retryNetwork()` internal catch — retry logic
- [x] 3.9 Refactor `downloadDispensaciones()` per-item catch
- [x] 3.10 Refactor `downloadServicios()` per-item catch
- [x] 3.11 Refactor `downloadPagos()` per-item catch

### SyncInventarioUseCase.kt (5 catch blocks)
- [x] 3.12 Refactor `invoke()` catch — top-level error handling
- [x] 3.13 Refactor `uploadMonturas()` upsert catch
- [x] 3.14 Refactor `uploadMovimientos()` upsert catch
- [x] 3.15 Refactor `downloadMonturas()` per-item catch
- [x] 3.16 Refactor `downloadMovimientos()` per-item catch

### SyncPacientesUseCase.kt (3 catch blocks)
- [x] 3.17 Refactor `invoke()` catch — top-level error handling
- [x] 3.18 Refactor `upload()` upsert catch
- [x] 3.19 Refactor `download()` per-item catch

### SyncHistorialUseCase.kt (3 catch blocks)
- [x] 3.20 Refactor `invoke()` catch — top-level error handling
- [x] 3.21 Refactor `uploadEvaluaciones()` upsert catch
- [x] 3.22 Refactor `downloadEvaluaciones()` per-item catch

### PostSaveSyncScheduler.kt (4 catch blocks)
- [x] 3.23 Refactor `schedulePacientesSync()` catch
- [x] 3.24 Refactor `scheduleHistorialSync()` catch
- [x] 3.25 Refactor `scheduleFinanzasSync()` catch
- [x] 3.26 Refactor `scheduleInventarioSync()` catch

### AuthViewModel.kt (3 catch blocks)
- [x] 3.27 Refactor `login()` catch
- [x] 3.28 Refactor `loginWithGoogle()` catch
- [x] 3.29 Refactor `checkExistingSession()` catch

### SyncViewModel.kt (1 catch block)
- [x] 3.30 Refactor `performSilentSync()` catch

### Additional domain-layer files (3 catch blocks)
- [x] 3.31 Refactor `SyncSessionHelper.refreshSessionBeforeSync()` catch
- [x] 3.32 Refactor `FullSyncStrategy.executeSync()` catch
- [x] 3.33 Refactor `BackupCommand.execute()` catch
