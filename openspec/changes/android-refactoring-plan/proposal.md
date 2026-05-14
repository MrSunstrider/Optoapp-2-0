# Proposal: Android Codebase Refactoring Plan

## Intent

Reduce technical debt across 19 Android source files identified as monolithic, untested, duplicated, or dead code. The codebase has strong foundations (Clean Architecture, Hilt, Compose, Room) but several files violate Single Responsibility — mixing UI, validation, data access, and business logic — making them hard to test and maintain.

## Scope

### In Scope (grouped by priority)

**P0 — CRITICAL (1 item)**
- A1: Split `NuevaEvaluacionScreen.kt` (11KB) extracting presenter/logic layer, keep UI only

**P1 — HIGH (6 items)**
- A2: Add ViewModel tests, refactor `AuthViewModel.kt` (23KB) into domain-specific delegates
- A3: Extract inline DAOs from `OptoDatabase.kt` (33KB) into per-entity DAO files
- A4: Migrate remaining entities from `Entities.kt`/`Daos.kt` to entity-specific packages (already started with `dispensacion/`, `paciente/`, `evaluacion/`)
- A5: Split `SyncHistorialUseCase.kt` applying same pattern as `SyncFinanzasUseCase` split
- A6: Refactor `RecetaEvaluacionPdfGenerator.kt` (22KB) into builder-pattern modules

**P2 — MEDIUM (8 items)**
- A7: Extract sections from `ConfiguracionScreen.kt` (32KB)
- A8: Extract sections from `DetallePacienteScreen.kt` (14KB)
- A9: Add tests + POST_NOTIFICATIONS check for `AppointmentReminderWorker.kt`
- A10: Fix `SecurityManager.kt` PIN migration logic
- A11: Parallelize `OperacionHoyViewModel.kt` with `awaitAll`
- A12: Extract stock logic from `NuevaDispensacionScreen.kt` (25KB)
- A13: Remove or flag `PlayBillingManager.kt` dead code
- A14: Remove or flag `SubscriptionManager.kt` dead code

**P3 — LOW (5 items)**
- A15: Complete `Type.kt` typography scale
- A16: Merge `SyncCancellation.kt` + `SyncGate.kt`
- A17: No changes needed (already well factored)
- A18: Merge `WhatsAppUtils.kt` into `FileShareUtils.kt`
- A19: Remove or archive `OnboardingOpticaScreen.kt`

### Out of Scope
- Web-side refactoring (covered by existing improvement plan)
- Supabase schema changes
- Full-screen UI rewrites (only extraction, not redesign)

## Approach

Execute in priority order (P0 → P1 → P2 → P3). Each item follows the same pattern:
1. **First**: Write tests for existing behavior (characterization tests where none exist)
2. **Then**: Extract/refactor maintaining backward compatibility
3. **Finally**: Verify with tests and build

Refactorings follow established project conventions: Clean Architecture layers, Hilt DI, Compose UI, Repository pattern.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/.../ui/screens/` | Modified | Screen files being split/extracted |
| `app/src/main/.../viewmodel/` | Modified | AuthViewModel refactored, tests added |
| `app/src/main/.../data/` | Modified | OptoDatabase DAOs extracted to per-entity files |
| `app/src/main/.../domain/` | Modified | SyncHistorialUseCase split |
| `app/src/main/.../util/` | Modified | PDF generator refactored, utils merged |
| `app/src/main/.../notifications/` | Modified | Worker tests + permission check |
| `app/src/main/.../billing/` | Modified | Dead code flagged/removed |
| `app/src/main/.../subscription/` | Modified | Dead code flagged/removed |
| `app/src/test/` | New | ViewModel and worker tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Extracted DAOs break Room schema migration | Low | Room DAOs are compile-time checked; schema version unchanged |
| Screen extraction missing UI state | Low | Keep ViewModel layer intact, only move UI into subcomposables |
| Dead code removal breaks build if referenced | Low | grep for imports before removal |
| Test coverage insufficient for refactoring safety | Medium | Write characterization tests BEFORE touching production code |

## Rollback Plan

Each item is atomic — rollback by reverting the specific file changes via git. No migration or data changes involved.

## Dependencies

- Android Studio / Gradle build passing
- Room schema generation for DAO extraction verification

## Success Criteria

- [ ] All P0 + P1 items completed with tests
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew assembleDebug` passes
- [ ] No regressions in app functionality
