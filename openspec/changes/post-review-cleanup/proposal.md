# Proposal: Post-Review Cleanup

## Intent

Address remaining technical debt items identified in the project review after critical-fixes PRs: (1) 7 remaining generic catches in ViewModels, (2) 15 files exceeding 300 lines requiring extraction, (3) Compose BOM severely outdated, (4) gaps in ViewModel and sync scheduler test coverage, and (5) inconsistent Result vs custom Resource type usage.

## Scope

### In Scope
- **W1: ViewModel Exception Handling** — Refactor remaining generic catches in ViewModels and delegates
- **W2: Large File Extraction** — Extract helpers/delegates/sections from 15 oversized files
- **W3: Compose BOM Update** — Upgrade from 2024.02.02 to current stable version
- **S1: ViewModel Test Coverage** — Add tests for SettingsViewModel, SubscriptionViewModel
- **S2: PostSaveSyncScheduler Tests** — Add unit tests for core sync orchestrator
- **S3: Result Type Unification** — Evaluate migrating custom Resource<T> to kotlin.Result or consolidate types

### Out of Scope
- New feature development
- UI refactoring beyond file extraction
- Full migration of Resource to Result (if deferred)
- Integration or E2E tests

## Capabilities

### New Capabilities
- None — this is a refactoring/quality improvement change

### Modified Capabilities
- None — no spec-level behavior changes

## Approach

### W1: ViewModel Exception Handling
Analyze remaining ~7 generic catches across ViewModels and delegates, replace with specific exception types, add logging. Priority files: AuthDelegate (2 catches), BackupDelegate (1 catch), ServiciosViewModel, MonturasViewModel, PacienteViewModel, FiscalConfigViewModel, EvaluacionViewModel.

### W2: Large File Extraction
Apply single-responsibility principle to 15 oversized files. Prioritize: EvaluacionViewModel (564 lines) → extract helpers/delegates; EvaluacionFormSections (542 lines) → extract composables; MainDrawerScreen (443 lines) → extract drawer sections; NuevaDispensacionScreen (440 lines) → extract form sections; RecetaPdfBuilder (435 lines) → extract PDF sections.

### W3: Compose BOM Update
Update from 2024.02.02 to latest stable (2024.09.00+). Verify compilation and run existing tests.

### S1: ViewModel Test Coverage
Add unit tests for SettingsViewModel and SubscriptionViewModel (0% coverage). Use existing test patterns from DispensacionViewModel/PacienteViewModel.

### S2: PostSaveSyncScheduler Tests
Add unit tests for PostSaveSyncScheduler (core sync orchestrator, 0% coverage). Cover scheduling, retry, error handling.

### S3: Result Type vs Resource Unification
Audit Resource<T> usage, compare with kotlin.Result, decide whether to migrate or document rationale for keeping custom type.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `presentation/viewmodel/ServiciosViewModel.kt` | Modified | Refactor catch block |
| `presentation/viewmodel/MonturasViewModel.kt` | Modified | Refactor catch block |
| `presentation/viewmodel/PacienteViewModel.kt` | Modified | Refactor catch block |
| `presentation/viewmodel/FiscalConfigViewViewModel.kt` | Modified | Refactor catch block |
| `presentation/viewmodel/EvaluacionViewModel.kt` | Modified | Extract helpers, refactor catches |
| `presentation/screens/evaluacion/EvaluacionFormSections.kt` | Modified | Extract composables |
| `presentation/screens/main/MainDrawerScreen.kt` | Modified | Extract drawer sections |
| `presentation/screens/dispensacion/NuevaDispensacionScreen.kt` | Modified | Extract form sections |
| `domain/usecase/RecetaPdfBuilder.kt` | Modified | Extract PDF sections |
| `data/repository/MembershipRepository.kt` | Modified | Already partially addressed |
| `build.gradle.kts` (app) | Modified | Compose BOM update |
| `app/src/test/.../SettingsViewModelTest.kt` | New | ViewModel test coverage |
| `app/src/test/.../SubscriptionViewModelTest.kt` | New | ViewModel test coverage |
| `app/src/test/.../PostSaveSyncSchedulerTest.kt` | New | Sync scheduler tests |
| `common/Result.kt` or `common/Resource.kt` | Modified | Unify or document |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Compose BOM update introduces breaking changes | Medium | Review changelogs, test incrementally |
| File extraction breaks existing imports | Medium | Maintain same package, use relative imports |
| Resource unification changes error handling | Medium | Test all error paths, consider wrapper |

## Rollback Plan

1. Revert build.gradle.kts for Compose BOM
2. Restore original files from git for extracted sections
3. Revert catch block changes for ViewModels
4. Delete new test files
5. Keep Resource<T> as-is if unification deferred

## Dependencies

- Builds on critical-fixes (C1 exception handling, C3 test patterns)
- Kotlin 1.9+ required for compose-bom 2024.09.00+
- MockK already in test dependencies

## Success Criteria

- [ ] All ~7 generic catches in ViewModels replaced with specific types
- [ ] 15 files reduced to <300 lines via extraction
- [ ] Compose BOM updated to latest stable, tests pass
- [ ] SettingsViewModel: ≥5 unit tests
- [ ] SubscriptionViewModel: ≥5 unit tests
- [ ] PostSaveSyncScheduler: ≥5 unit tests
- [ ] Resource/Result decision documented with rationale