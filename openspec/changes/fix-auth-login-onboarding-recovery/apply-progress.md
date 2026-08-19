# Apply progress: fix-auth-login-onboarding-recovery

STRICT TDD. `test_command: ./gradlew :optoapp:testDebugUnitTest --stacktrace`

## TDD Cycle Evidence

| Task | RED | GREEN | TRIANGULATE | SAFETY NET | REFACTOR |
|------|-----|-------|-------------|------------|----------|
| 1.1–1.2 WU1 RPC id | ✅ Written `CreateOpticaReturnedIdTest` + `verify_create_optica_bootstrap.sql` | ✅ Passed (PR #67) | ✅ 2 files | ✅ existing auth tests | DROP+CREATE RETURNS text |
| 2.1–2.2 WU2 onboarding | ✅ Written `MembershipFetchTest` `PostLoginNavigationTest` `AuthDelegateTest` | ✅ Passed (PR #68) | ✅ N cases | ✅ suite | sealed fetch + owner form |
| 3.1 WU3 cold start | ✅ Written `ColdStartNavigationTest` | ✅ Passed (PR #69) | ✅ 3 scenarios | ✅ | isAuthChecked gate |
| 4.1 WU4 Google Idle | ✅ Written `GoogleAuthAbandonTest` | ✅ Passed (PR #70) | ✅ Single A5 | ✅ | ON_RESUME |
| 5.1 WU5 profiles RLS | ✅ Written `verify_user_profiles_select_rls.sql` | ✅ Passed (PR #71) | ✅ SQL scenarios | N/A (new SQL) | scoped SELECT |
| 6.1 WU6 PIN | ✅ Written inverted both-empty + CreatePin dest | ✅ Passed (PR #72) | ✅ unset non-empty added after verify FAIL | ✅ PinDelegateTest | empty guards |
| 7.1 WU7 GoTrue | ✅ Written `GoTruePasswordPolicyTest` (toml empty RED) | ✅ Passed (PR #73) | ✅ weak/strong `meets` after verify FAIL | ✅ | policy object |
| 8.1 full suite | N/A (run) | ✅ Passed 2156 | ➖ | ✅ | leave createAdditionalOptica unwired |
| Verify coverage | ✅ SinOpticaUiStateTest + PIN unset + weak/strong | ✅ this WU | ✅ 2+ cases each | ✅ | extract helpers |

RED confirmed for WU7 toml (`password_requirements=""` failed before GREEN). WU6 both-empty was inverted from true→false.

## Notes

- Hosted GoTrue dashboard remains a human step (`docs/gotrue-hosted-password-policy.md`).
- Local GoTrue HTTP submit is not run when Docker is down; `GoTruePasswordPolicy.meets` encodes the same class set as `lower_upper_letters_digits_symbols` min 6.
- SQL catalog files exist; Docker harness may be WARNING.
