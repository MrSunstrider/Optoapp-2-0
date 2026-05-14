# Proposal: Quality Improvements

## Intent

Improve code maintainability and test coverage: (1) extract single-responsibility helpers from 4 oversized files, (2) add Compose UI tests for critical screens, (3) enforce coverage thresholds in CI via JaCoCo.

## Scope

### In Scope
- **W1: Large File Extraction** — Split: SyncFinanzasUseCase (434 lines), MembershipRepository (418), OptoRepository (402), ConfiguracionScreen (385)
- **W2: UI Tests** — Add Compose UI tests for LoginScreen, MainDrawerScreen, NuevaDispensacionScreen
- **W3: CI Coverage** — Configure JaCoCo with 50% minimum threshold in Gradle

### Out of Scope
- New features or behavior changes
- Unit tests (covered elsewhere)
- E2E or integration tests

## Capabilities
- **New**: None — refactor + testing only
- **Modified**: None — no spec-level changes

## Approach

**W1: Large File Extraction** — Apply single-responsibility. Extract helpers while keeping same package and relative imports. All existing tests must pass post-extraction.

**W2: UI Tests** — Use Jetpack Compose testing with mocked dependencies. Focus on happy path, error states, and loading states for each screen.

**W3: CI Coverage** — JaCoCo already configured. Add `minimum = 0.50` in build.gradle.kts, fail CI when threshold breached.

## Affected Areas

| Area | Impact |
|------|--------|
| `domain/usecase/SyncFinanzasUseCase.kt` | Refactored — extract 3 helpers |
| `data/repository/MembershipRepository.kt` | Refactored — extract 3 data sources |
| `data/repository/OptoRepository.kt` | Refactored — extract 3 coordinators |
| `presentation/screens/settings/ConfiguracionScreen.kt` | Refactored — extract composables |
| `app/src/androidTest/.../LoginScreenTest.kt` | New |
| `app/src/androidTest/.../MainDrawerScreenTest.kt` | New |
| `app/src/androidTest/.../NuevaDispensacionScreenTest.kt` | New |
| `build.gradle.kts` | Modified — add coverage threshold |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| File extraction breaks imports | Medium | Same package, run tests after each |
| UI tests flaky on CI | Medium | Use IdlingResource, mock deps |
| Threshold too strict | Low | Start at 50%, adjust as needed |

## Rollback Plan
1. Revert build.gradle.kts
2. Delete new UI test files
3. Restore original files from git

## Dependencies
- Builds on post-review-cleanup patterns
- JaCoCo already configured

## Success Criteria

- [ ] 4 files reduced to <250 lines
- [ ] ≥3 UI tests per screen (Login, MainDrawer, NuevaDispensacion)
- [ ] JaCoCo threshold enforced in CI