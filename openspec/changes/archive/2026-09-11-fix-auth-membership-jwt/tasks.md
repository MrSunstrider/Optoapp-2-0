# Tasks: Fix Auth Membership JWT

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 160–260 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | C6 DTO rol fail-closed | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.MembershipRepositoryDtosTest --stacktrace` | N/A — Json DTO unit decode; no device | Revert `MembershipRepositoryDtos.kt` + `MembershipRepositoryDtosTest.kt` |
| 2 | C7 JWT retry user+token | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.domain.NetworkRetryHelperTest --stacktrace` | N/A — MockK Auth stubs; no sync runtime | Revert `NetworkRetryHelper.kt` + `NetworkRetryHelperTest.kt` |
| 3 | Null GoTrue → Error | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.data.MembershipRepositoryErrorTest --tests com.example.optoapp.data.MembershipRepositoryTest --stacktrace` | N/A — repository Fake/MockK; no network | Revert `MembershipDataSource.kt` + MembershipRepository*Test null-session asserts |
| 4 | OpticaSelectionPrep + drawer | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.viewmodel.AuthViewModelTest --stacktrace` | Manual drawer Error toast vs “solo una óptica” (optional smoke) | Revert `AuthViewModel.kt` (`OpticaSelectionPrep`) + `MainDrawerScreen.kt` + prep tests |
| 5 | Scope lock + full suite | PR 1 | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | N/A — full unit suite is verify gate | Revert Units 1–4 as one set |

## Phase 1: C6 DTO rol default (Blank Role Fail Closed)

- [x] 1.1 RED — In `optoapp/src/test/java/com/example/optoapp/data/MembershipRepositoryDtosTest.kt`, flip `defaultRol` expect `""`; assert missing/`null` JSON `rol` decodes blank and `mapRow` skips (not admin); keep valid `empleado` preserved.
- [x] 1.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/data/MembershipRepositoryDtos.kt`, set `UsuarioOpticaDto.rol` default `""`; do not touch `di/SupabaseModule.kt` coerce.
- [x] 1.3 Confirm Unit 1 command green.

## Phase 2: C7 JWT sync retry fail-closed

- [x] 2.1 RED — In `optoapp/src/test/java/com/example/optoapp/domain/NetworkRetryHelperTest.kt`, add cases: post-refresh null/anon user → `refreshSessionForRetry` false; blank/missing accessToken → false; update happy stubs to provide non-anon user + usable token so existing retry still succeeds.
- [x] 2.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/domain/NetworkRetryHelper.kt`, after `refreshCurrentSession`, require non-null non-anon `currentUserOrNull` and non-blank `currentSessionOrNull()?.accessToken`; else return false. Do not change `domain/SyncSessionHelper.kt` (read-only pattern).
- [x] 2.3 Confirm Unit 2 command green.

## Phase 3: Null GoTrue → MembershipFetch.Error

- [x] 3.1 RED — In `optoapp/src/test/java/com/example/optoapp/data/MembershipRepositoryErrorTest.kt` (and `MembershipRepositoryTest.kt` noSession), assert null GoTrue user → `MembershipFetch.Error` with Sin sesión; not Empty; no onboarding/clearSession via `flagsFor`.
- [x] 3.2 GREEN — In `optoapp/src/main/java/com/example/optoapp/data/membership/MembershipDataSource.kt`, null uid → `MembershipFetch.Error(IllegalStateException("Sin sesión"))`.
- [x] 3.3 Confirm Unit 3 command green.

## Phase 4: OpticaSelectionPrep + MainDrawer Error UX

- [x] 4.1 RED — In `optoapp/src/test/java/com/example/optoapp/viewmodel/AuthViewModelTest.kt`, assert `prepareOpticaSelection` returns `OpticaSelectionPrep.Error` on fetch Error (not `Ok(false)`); `Ok(true)`/`Ok(false)` for multi/single Empty|Ok lists.
- [x] 4.2 GREEN — Colocate sealed `OpticaSelectionPrep` next to `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt`; map Error→`Error(message)`; Empty/Ok→`Ok(size>1)`. Leave `AuthDelegate.prepareOpticaSelection` unchanged.
- [x] 4.3 GREEN — In `optoapp/src/main/java/com/example/optoapp/ui/screens/MainDrawerScreen.kt`, Error → error toast (cause/`"Error al cargar ópticas"`); navigate only `Ok(true)`; keep `Ok(false)` “solo una óptica” toast.
- [x] 4.4 Confirm Unit 4 command green.

## Phase 5: Scope Lock + Verify

- [x] 5.1 Scope lock: no C8 PIN logout / CreatePin await; no deeplink/recovery / ON_RESUME Loading→Idle; no global Json coerce flip; no required-`rol` decode; no Supabase schema/RLS.
- [x] 5.2 Focused Units 1–4 green; then full `./gradlew :optoapp:testDebugUnitTest --stacktrace`.
