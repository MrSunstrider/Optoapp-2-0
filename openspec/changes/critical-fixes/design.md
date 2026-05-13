# Design: Critical Fixes — Exception Handling (Phase 3)

## Problem
Domain-layer code uses generic `catch (e: Exception)` blocks with `rethrowIfCancellation(e)` to handle all errors uniformly. This loses information about the error type and makes debugging harder.

## Solution
Replace generic catch blocks with a three-tier pattern:

1. `catch (e: CancellationException) { throw e }` — rethrow coroutine cancellations immediately (cleaner than the previous `rethrowIfCancellation` helper)
2. `catch (e: IOException) { Log.e(...); ... }` — network errors get explicit "Error en red" label with full stack trace
3. `catch (e: Exception) { Log.e(...); ... }` — unexpected errors get "Error inesperado" label with full stack trace

## Architecture Decision

### ADR-3.1: Error label extraction
A pure function `errorLabelForException(e: Throwable): String` was added to `com.example.optoapp.sync.SyncErrorHandler.kt`:
- `IOException` → `"Error en red"`
- Any other exception → `"Error inesperado"`
- `CancellationException` is NOT handled here — callers catch it separately

This function is `internal` (package-level) and directly testable without mocking.

### Files modified
| File | Package | Catch blocks |
|------|---------|-------------|
| `SyncFinanzasUseCase.kt` | domain | 11 |
| `SyncInventarioUseCase.kt` | domain | 5 |
| `SyncPacientesUseCase.kt` | domain | 3 |
| `SyncHistorialUseCase.kt` | domain | 3 |
| `PostSaveSyncScheduler.kt` | sync | 4 |
| `AuthViewModel.kt` | viewmodel | 3 |
| `SyncViewModel.kt` | viewmodel | 1 |
| `SyncSessionHelper.kt` | domain | 1 |
| `DefaultStrategies.kt` | domain/sync/strategies | 1 |
| `CommandPatterns.kt` | domain/command | 1 |
| `SyncErrorHandler.kt` | sync | NEW — utility function |

### Total: 33 catch blocks refactored across 11 files
