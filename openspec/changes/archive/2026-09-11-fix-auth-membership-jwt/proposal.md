# Proposal: fix-auth-membership-jwt

## Intent

Close JD ROUND 2 auth leftovers that fail-open on membership privilege and JWT retry: missing/null `rol` must not become admin; post-refresh sync retry must not succeed without a usable user/token; null GoTrue user and selector fetch errors must not look like empty memberships.

## Scope

### In Scope
- JD2-C6: `UsuarioOpticaDto.rol` default `""` (not `"admin"`); keep `mapRow` blank-skip; do **not** disable global `coerceInputValues`
- JD2-C7: `NetworkRetryHelper.refreshSessionForRetry` after `refreshCurrentSession` MUST verify non-null non-anon `currentUserOrNull` + usable `accessToken` (align `SyncSessionHelper`); else return `false`
- Suspect: `MembershipDataSource` null user → `MembershipFetch.Error` (not `Empty`)
- Suspect: `prepareOpticaSelection` sealed `Ok(hasMultiple)` / `Error`; `MainDrawer` MUST NOT toast “solo una óptica” on fetch Error
- Strict TDD (red → green) for each fix

### Out of Scope
- JD2-C8 PIN logout, CreatePin await
- Deeplink / recovery / ON_RESUME Loading→Idle
- Global Json coerce flip; required-`rol` decode that fails the whole list
- Supabase schema or RLS changes (none)

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `android-auth-onboarding`: Strengthen **Blank Role Fail Closed** (missing/null JSON `rol` via DTO default + coerce must not become admin; row skipped). Strengthen **Membership Fetch Distinguishes Error From Empty** (null GoTrue user → Error; selector/`prepareOpticaSelection` Error ≠ empty / ≠ “solo una óptica”).
- `android-auth`: Small delta — JWT refresh used by `NetworkRetryHelper` MUST fail-closed after `refreshCurrentSession` unless non-anon user + usable accessToken (mirror SyncSessionHelper); prefer sync conventions in design over a new sync capability.

## Approach

Approach 1 — surgical fail-closed quartet (locked):

1. C6: flip DTO default to `""`; decode tests for missing/`null` `rol` → blank → `mapRow` skip
2. C7: post-refresh user + token guard in `refreshSessionForRetry`
3. Null user: DataSource returns `MembershipFetch.Error(...)`
4. Selector: stop collapsing Error to `emptyList()`; sealed outcome + distinct drawer error UX

Reuse existing `MembershipFetch` / `flagsFor` / wait-screen Error→`-1` patterns. No global serializer change.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/MembershipRepositoryDtos.kt` | Modified | `rol` default `""` |
| `data/membership/MembershipDataSource.kt` | Modified | null user → Error |
| `viewmodel/AuthViewModel.kt` | Modified | sealed prepareOpticaSelection |
| `ui/screens/MainDrawerScreen.kt` | Modified | Error toast ≠ single-óptica |
| `domain/NetworkRetryHelper.kt` | Modified | post-refresh session verify |
| `domain/SyncSessionHelper.kt` | Reference | pattern only |
| `di/SupabaseModule.kt` | Unchanged | coerce stays true |
| Unit tests (Dtos, Membership*, Auth*, NetworkRetry*) | Modified | TDD RED→GREEN |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| prepareOpticaSelection API / drawer test churn | Med | Prefer expose `MembershipFetch` or small sealed; migrate Boolean callers |
| Null-user Error vs prior false onboarding | Low | Spec-correct; clear “Sin sesión” message |
| DB rows missing `rol` skipped (lose membership) | Low | Intentional fail-closed; design notes DB constraint read-only |
| C7 surfaces more sync failures | Low | Desired; callers already abort on refresh false |

## Rollback Plan

Revert DTO default, DataSource null branch, NetworkRetryHelper guard, ViewModel/drawer sealed API, and related tests in one commit. No DB migration to undo.

## Dependencies

- Confirmed Approach 1 handoff; exploration `fix-auth-membership-jwt`
- Existing specs: `android-auth-onboarding`, `android-auth`
- Strict TDD + `./gradlew :optoapp:testDebugUnitTest`

## Success Criteria

- [ ] Missing/null JSON `rol` never maps to admin; blank skipped
- [ ] `refreshSessionForRetry` returns false without usable user/token
- [ ] Null GoTrue user → `MembershipFetch.Error`; no onboarding route
- [ ] Drawer never shows “solo una óptica” on membership fetch Error
- [ ] Targeted unit tests green; C8/deeplink/recovery untouched
