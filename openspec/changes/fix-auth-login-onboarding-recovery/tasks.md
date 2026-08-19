# Tasks: Fix Auth Login Onboarding Recovery

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1050–1300 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1→7 stacked-to-main = WU1–WU7 |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

`test_command: ./gradlew :optoapp:testDebugUnitTest --stacktrace` (alias G). `pkg=com.example.optoapp`.
Strict TDD. WU1 before WU2. WU5 own RLS PR. No C3. GGA before remote SQL.
All WUs: `rdd_mode=disabled/unmanaged; issue_pr=N/A; unresolved_authority_decisions=none`

### Suggested Work Units

| Unit | Goal | PR | Focused test command | Runtime harness | Rollback |
|------|------|----|----------------------|-----------------|----------|
| WU1 A2+A9 | RPC RETURNS text; admin iff INSERT; drop INSERT policy; decode S | 1→main | G `--tests pkg.data.membership.CreateOpticaReturnedIdTest` + `supabase db reset` + `supabase/tests/verify_create_optica_bootstrap.sql` | local reset+SQL; GGA before remote | revert `20260819120000_harden_create_optica_return_id.sql`, verify SQL, `CreateOpticaReturnedIdTest.kt`, `OpticaSettingsDataSource.kt` |
| WU2 A1+A3+A11+A12 | owner UI; no clearSession on 0; Error≠Empty; skip blank rol; size==1 skip selector | 2→main | G `--tests pkg.viewmodel.auth.PostLoginNavigationTest --tests pkg.data.MembershipFetchTest --tests pkg.viewmodel.AuthDelegateTest` | debug APK after WU1 live | revert `MembershipFetch.kt`, `PostLoginNavigation.kt`, membership/session/auth/UI in WU2 |
| WU3 A4 | pin/main only after `isAuthChecked` | 3→main | G `--tests pkg.viewmodel.AuthViewModelTest --tests pkg.ui.screens.LoginScreenTest` | cold start session vs none | revert `MainActivity.kt` isAuthChecked gate |
| WU4 A5 | Google cancel/no-session → Idle not Loading | 4→main | G `--tests pkg.viewmodel.AuthViewModelTest` | Google cancel + ON_RESUME | revert `onGoogleAuthAbandoned` |
| WU5 A6 | user_profiles SELECT scoped | 5→main | `supabase db reset` then `supabase/tests/verify_user_profiles_select_rls.sql` | same SQL; GGA before remote | revert `20260819120100_user_profiles_select_scoped.sql` + verify SQL |
| WU6 A7 | empty PIN false; CreatePin iff required&&unset | 6→main | G `--tests pkg.viewmodel.PinDelegateTest --tests pkg.viewmodel.auth.PostLoginNavigationTest` | optional vs required unset vs set | revert `PinDelegate.kt` + dest PIN branch |
| WU7 A8 | toml + hosted dashboard note | 7→main | G `--tests pkg.data.GoTruePasswordPolicyTest` | grep note; human dashboard | revert toml, `docs/gotrue-hosted-password-policy.md`, test |

Threat RED: WU1 RPC ignore client id / admin iff INSERT / return S; RLS deny direct opticas INSERT. WU2 empty→SinOptica; error≠SinOptica; size==1 skip selector; keep session. WU3 wait isAuthChecked. WU4 Google not Loading. WU5 profiles scoped. WU6 empty PIN false; CreatePin iff required&&unset.

`src=optoapp/src/main/java/com/example/optoapp` `test=optoapp/src/test/java/com/example/optoapp`

## Phase 1: WU1 A2+A9 (~280)

- [x] 1.1 RED `supabase/tests/verify_create_optica_bootstrap.sql`: known id no admin; id≠client; RETURN=opticas.id; INSERT policy gone; trigger remains; no max raise. RED `test/.../data/membership/CreateOpticaReturnedIdTest.kt` (JUnit+MockK): persist S not C; blank decode fails. Confirm RED.
- [x] 1.2 GREEN `supabase/migrations/20260819120000_harden_create_optica_return_id.sql`: DROP+CREATE RETURNS text; ignore p_optica_id; admin iff ROW_COUNT=1; GRANT; drop opticas_insert_authenticated. GREEN `src/.../data/membership/OpticaSettingsDataSource.kt` decodeAs String; blank fails; persist S. Re-run 1.1. GGA before remote.

## Phase 2: WU2 A1+A3+A11+A12 (~380)

- [x] 2.1 RED `test/.../data/MembershipFetchTest.kt`: IOException→Error not Empty; empty→Empty; blank rol skipped; empleado kept. RED `test/.../viewmodel/auth/PostLoginNavigationTest.kt`: size==1 skip selector; >1 selector; empty→SinOptica. RED `AuthDelegateTest.kt`: empty no clearSession; saveOnboardingSession logged-in opticaId="" not mi_optica_base; owner calls completeOnboardingOptica; no Android invitaciones. Confirm RED.
- [x] 2.2 GREEN MembershipFetch.kt Ok/Empty/Error; MembershipDataSource.kt skip blank rol; MembershipRepository.kt sealed + asList() Error→empty. GREEN SessionManager.kt saveOnboardingSession; AuthDelegate.kt no clear on 0; PostLoginNavigation.kt; AuthViewModel.kt; SinOpticaScreen.kt owner form; SeleccionOpticaScreen.kt empty ≠ pin/main. Re-run 2.1.

## Phase 3: WU3 A4 (~90)

- [x] 3.1 RED AuthViewModelTest/LoginScreenTest: no restore before isAuthChecked; session leaves Login; none stays. Confirm RED. GREEN MainActivity.kt gate isAuthChecked then resolvePostLogin. Re-run GREEN.

## Phase 4: WU4 A5 (~80)

- [x] 4.1 RED AuthViewModelTest: cancel / no-session → Idle or Error, not Loading. Confirm RED. GREEN AuthViewModel.onGoogleAuthAbandoned; LoginScreen.kt ON_RESUME. Re-run GREEN.

## Phase 5: WU5 A6 (~180)

- [x] 5.1 RED `supabase/tests/verify_user_profiles_select_rls.sql`: own visible; anon empty; same-óptica admin peer visible; other hidden; empleado own-only. Confirm RED. GREEN `supabase/migrations/20260819120100_user_profiles_select_scoped.sql`. Re-run. GGA before remote.

## Phase 6: WU6 A7 (~120)

- [ ] 6.1 RED PinDelegateTest: both-empty false; unset never matches. RED PostLoginNavigationTest: CreatePin iff required&&unset; set→Pin; !required→Main. No C3. Confirm RED. GREEN PinDelegate.kt; dest helper; LoginScreen.kt/RegisterScreen.kt. Re-run GREEN.

## Phase 7: WU7 A8 (~70)

- [ ] 7.1 RED `test/.../data/GoTruePasswordPolicyTest.kt`: toml min 6 + lower_upper_letters_digits_symbols; `docs/gotrue-hosted-password-policy.md` greps string, 6, dashboard/Management API. Confirm RED. GREEN set toml; write note. Re-run. Human hosted Auth.

## Cleanup

- [ ] 8.1 Full `./gradlew :optoapp:testDebugUnitTest --stacktrace`. Leave createAdditionalOptica unwired.
