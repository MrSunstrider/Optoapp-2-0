# Apply Progress: fix-auth-membership-jwt

**Mode**: Strict TDD  
**Status**: done (15/15 tasks)  
**applyState**: all_done  
**Date**: 2026-09-11  
**Delivery**: single PR (budget risk Low)

## Completed Tasks

- [x] 1.1–1.3 C6 `UsuarioOpticaDto.rol` default `""` + decode/mapRow tests (19/19)
- [x] 2.1–2.3 C7 `refreshSessionForRetry` user+token guard + fail-closed tests (13/13 XML)
- [x] 3.1–3.3 Null GoTrue → `MembershipFetch.Error("Sin sesión")` + flagsFor (16/16 across Error+Repo tests)
- [x] 4.1–4.4 `OpticaSelectionPrep` sealed + MainDrawer Error toast (37/37 AuthViewModelTest)
- [x] 5.1 Scope lock preserved (`createPinAwaitingSuccess` kept; no PIN wipe / deeplink / coerce changes)
- [x] 5.2 Focused Units 1–4 green; full suite XML evidence 2372 tests / 0 fail / 0 err

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1–1.3 | `MembershipRepositoryDtosTest.kt` | Unit | ✅ prior DTO tests | ✅ Written (default/missing/null) | ✅ Passed 19/19 | ✅ empleado preserved | ➖ None needed |
| 2.1–2.3 | `NetworkRetryHelperTest.kt` | Unit | ✅ existing JWT cases | ✅ Written (null/blank/missing) | ✅ XML 13/13 | ✅ happy stubs + 3 fail-closed | ➖ None needed |
| 3.1–3.3 | `MembershipRepositoryErrorTest.kt` + `MembershipRepositoryTest.kt` | Unit | ✅ no-session suite | ✅ Written (Error≠Empty+flags) | ✅ Passed | ✅ both test classes | ➖ None needed |
| 4.1–4.4 | `AuthViewModelTest.kt` | Unit | ✅ wait-screen cases | ✅ Written (Error/Ok multi/single) | ✅ Passed 37/37 | ✅ Empty + single + multi + fallback msg | ➖ None needed |

## Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused Unit 1 | `MembershipRepositoryDtosTest` BUILD SUCCESSFUL — 19 tests, 0 fail |
| Focused Unit 2 | `TEST-…NetworkRetryHelperTest.xml` tests=13 failures=0 errors=0 (Gradle task may exit non-zero on Windows binary results close flake) |
| Focused Units 3–4 | ErrorTest 6, RepoTest 10, AuthViewModelTest 37 — BUILD SUCCESSFUL, 0 fail |
| Full suite | 265 XML suites, **2372 tests, 0 failures, 0 errors** |
| Runtime harness | N/A — unit/MockK boundary only (threat matrix N/A) |
| Rollback boundary | Revert Dtos + NetworkRetryHelper + MembershipDataSource + AuthViewModel OpticaSelectionPrep + MainDrawerScreen + matching tests |

## Files Changed

| File | Action |
|------|--------|
| `data/MembershipRepositoryDtos.kt` | `UsuarioOpticaDto.rol` default `""` |
| `domain/NetworkRetryHelper.kt` | post-refresh user+token guard |
| `data/membership/MembershipDataSource.kt` | null uid → Error Sin sesión |
| `viewmodel/AuthViewModel.kt` | `OpticaSelectionPrep` sealed; `prepareOpticaSelection` returns it; **preserved** `createPinAwaitingSuccess` |
| `ui/screens/MainDrawerScreen.kt` | Error toast; navigate only Ok(true) |
| `data/MembershipRepositoryDtosTest.kt` | RED/GREEN decode+mapRow |
| `domain/NetworkRetryHelperTest.kt` | happy stubs + fail-closed cases |
| `data/MembershipRepositoryErrorTest.kt` | no-session → Error + flagsFor |
| `data/MembershipRepositoryTest.kt` | noSession → Error + flagsFor |
| `viewmodel/AuthViewModelTest.kt` | OpticaSelectionPrep contracts |
| `openspec/changes/fix-auth-membership-jwt/tasks.md` | all `[x]` |

## Deviations from Design

None — implementation matches design. Extra blank `user.id` check is fail-closed compatible with SyncSessionHelper null-user=anon model.

## Issues Found

- Windows/Gradle 9.3 may report `Could not execute test class 'NetworkRetryHelperTest'` after all cases pass (binary in-progress results close). Trust JUnit XML: 0 failures.
- Parallel pin-logout left `createPinAwaitingSuccess` in AuthViewModel; preserved untouched.

## Status

15/15 tasks complete. Ready for verify.
