## Exploration: fix-auth-membership-jwt

### Scope

Part 3 of Judgment Day ROUND 2 auth leftovers (after Part1 recovery, Part2a recovery-UI, Part2b deeplink-session):

| ID | Severity | Finding |
|----|----------|---------|
| JD2-C6 | CRITICAL | `UsuarioOpticaDto.rol` default `"admin"` + global `coerceInputValues = true` → missing/null JSON `rol` becomes admin |
| JD2-C7 | CRITICAL/HIGH | `NetworkRetryHelper.refreshSessionForRetry` returns `true` after `refreshCurrentSession()` without `currentUserOrNull` / non-anon / accessToken check |
| Suspect | WARN/HIGH | `MembershipDataSource` null user → `MembershipFetch.Empty` (should be Error) |
| Suspect | A-only | `AuthViewModel.prepareOpticaSelection` collapses `Error`/`Empty` to `emptyList()` → drawer toast lies on fetch error |

**Out of scope:** JD2-C8 PIN logout, CreatePin await, deeplink/session (closed in Part2b), recovery UI (Part1/2a), ON_RESUME Loading→Idle.

**Strict TDD:** yes (`openspec/config.yaml`).

**Spec anchors (main):** `openspec/specs/android-auth-onboarding/spec.md`
- **Blank Role Fail Closed** — blank/missing rol MUST NOT become admin; skip or reject
- **Membership Fetch Distinguishes Error From Empty** — fetch error MUST NOT route as empty onboarding

### Current State

#### C6 — Privilege escalation via DTO default + coerce

`UsuarioOpticaDto`:

```kotlin
internal data class UsuarioOpticaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String = "admin",  // fail-open
)
```

`SupabaseModule` Json config sets `coerceInputValues = true`. With kotlinx.serialization, missing or `null` `rol` is coerced to the default `"admin"` before `MembershipFetch.mapRow` runs.

`mapRow` already fail-closes blank roles:

```kotlin
fun mapRow(... rol: String): OpticaMembership? {
    if (rol.isBlank()) return null
    return OpticaMembership(...)
}
```

So blank-string defense exists, but the DTO never surfaces blank for absent/null JSON — privilege escalation bypasses the spec. Contrast: `OpticaMemberRow.rol` defaults to `""` (safe). Characterization test `usuarioOpticaDto_defaultRol` **locks in** the unsafe default (`assertEquals("admin", dto.rol)`).

No decode-null/missing-rol serialization test exists today.

#### C7 — JWT retry refresh is fail-open vs SyncSessionHelper

`NetworkRetryHelper.refreshSessionForRetry`:

```kotlin
supabase.auth.refreshCurrentSession()
true  // no user / accessToken validation
```

`SyncSessionHelper.evaluateSessionBeforeSync` already documents and implements the correct contract: after refresh, reject anon (`currentUserOrNull() == null`) and blank `accessToken`. Existing tests: `anonymous session returns false`, `evaluateSessionBeforeSync reports sin usuario for anonymous`.

`NetworkRetryHelperTest` covers JWT-expired refresh + retry but never asserts post-refresh user presence — so anon-after-refresh still returns success and retries under a broken session.

#### Suspect — null user → Empty (onboarding lie)

`MembershipDataSource.fetchMembershipsForCurrentUser`:

```kotlin
val uid = supabase.auth.currentUserOrNull()?.id ?: return MembershipFetch.Empty
```

`Empty` triggers `AuthDelegate.flagsFor` → `requiresOnboarding = true` / `saveOnboardingSession = true`. A missing GoTrue user is an auth failure, not “zero memberships”. Spec: Error ≠ Empty.

`MembershipRepositoryTest.fetchMembershipsForCurrentUser_noSession_returnsEmptyList` currently asserts Empty — must go RED→GREEN to Error.

Sibling APIs already fail differently: `assignRoleByEmail` / create optica return `Result.failure(IllegalStateException("Sin sesión"))`.

#### Suspect — prepareOpticaSelection collapses Error

`AuthDelegate.prepareOpticaSelection()` correctly returns sealed `MembershipFetch`.

`AuthViewModel.prepareOpticaSelection()`:

```kotlin
val memberships = if (fetch is MembershipFetch.Ok) fetch.memberships else emptyList()
_pendingMemberships.value = memberships
return memberships.size > 1
```

`Error` and `Empty` both become `false` + empty pending list. `MainDrawerScreen` then toasts “Solo tienes una óptica asociada.” on Error — false and silent. Contrast: `refreshMembershipsForWaitScreen` already maps Error → `-1` (compliant pattern).

`MembershipFetch.asList()` still maps Error→emptyList for sync callers only; auth path must not use it for routing.

### Affected Areas

- `optoapp/src/main/java/com/example/optoapp/data/MembershipRepositoryDtos.kt` — C6 default `rol`
- `optoapp/src/main/java/com/example/optoapp/di/SupabaseModule.kt` — coerce context (do not disable globally)
- `optoapp/src/main/java/com/example/optoapp/data/membership/MembershipFetch.kt` — mapRow already OK; keep
- `optoapp/src/main/java/com/example/optoapp/data/membership/MembershipDataSource.kt` — null-user Empty→Error
- `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt` — prepareOpticaSelection fail-closed
- `optoapp/src/main/java/com/example/optoapp/ui/screens/MainDrawerScreen.kt` — toast/error UX for fetch Error
- `optoapp/src/main/java/com/example/optoapp/domain/NetworkRetryHelper.kt` — C7 post-refresh user check
- `optoapp/src/main/java/com/example/optoapp/domain/SyncSessionHelper.kt` — pattern to mirror (read-only reference)
- Tests: `MembershipRepositoryDtosTest`, new decode tests, `MembershipFetchTest`, `MembershipRepositoryTest` / DataSource, `AuthViewModelTest`, `NetworkRetryHelperTest`, possibly `AuthDelegateTest`
- Spec delta likely: `android-auth-onboarding` (strengthen Blank Role + Error≠Empty for null session / selector refresh); optional small JWT-retry note if a sync/auth JWT domain exists — prefer onboarding + NetworkRetry behavior in design

**Not affected (locked out):** PIN/`CreatePin`, recovery screens, `MainActivity` deeplink/`ON_RESUME`, `SignOutScope`.

### Approaches

1. **Surgical fail-closed quartet (recommended)** — Four coordinated TDD fixes, no serializer global change.
   - C6: change `UsuarioOpticaDto.rol` default to `""` (align `OpticaMemberRow`); add kotlinx decode tests for missing and `"rol":null` → blank → `mapRow` skips
   - C7: after `refreshCurrentSession()`, require `currentUserOrNull() != null` and non-blank `accessToken` (mirror SyncSessionHelper); else return `false`
   - Null user: return `MembershipFetch.Error(IllegalStateException("Sin sesión"))` (or equivalent)
   - prepareOpticaSelection: stop collapsing Error; return sealed outcome (prefer expose `MembershipFetch` or tri-state) and MainDrawer distinct toast/error
   - Pros: Matches existing specs; minimal blast radius; reuses SyncSessionHelper contract; strict TDD clear RED targets
   - Cons: prepareOpticaSelection API change touches drawer caller; DtosTest default assertion flips
   - Effort: Low–Medium

2. **DTO-required rol + decode failure as Error** — Remove default; missing/null `rol` fails whole `decodeList`.
   - Pros: Strongest fail-closed at decode
   - Cons: One bad row fails entire membership fetch → may strand multi-optica users; larger behavior change than skip-row; conflicts with mapRow skip semantics already shipping
   - Effort: Medium

3. **Disable coerceInputValues globally** — Flip Supabase Json flag.
   - Pros: Stops null→default for all DTOs
   - Cons: High collateral (every DTO relying on coerce); out of proportion to C6; risk of sync decode regressions
   - Effort: High

4. **Defer suspects; C6+C7 only** — Ship privilege + JWT retry only.
   - Pros: Smaller PR
   - Cons: Leaves Error↔Empty and drawer lie; JD ledger already elevated Membership null→Empty; incomplete vs user IN SCOPE
   - Effort: Low (rejected for incomplete scope)

### Recommendation

**Approach 1 — Surgical fail-closed quartet**, strict TDD order:

1. **RED/GREEN C6** — Decode tests prove missing/null `rol` ≠ admin; flip DTO default to `""`; update `usuarioOpticaDto_defaultRol`; keep `mapRow` skip.
2. **RED/GREEN C7** — Test refresh-succeeds-but-user-null returns false and does not retry as success; implement SyncSessionHelper-aligned checks in `refreshSessionForRetry`.
3. **RED/GREEN null-user Error** — Flip DataSource + repository test Empty→Error; confirm `flagsFor(Error)` still no onboarding / no clearSession.
4. **RED/GREEN prepareOpticaSelection** — ViewModel must not treat Error as empty; MainDrawer must not show “solo una óptica” on Error (error toast or no-op with message). Prefer returning `MembershipFetch` (or a small UI sealed) over overloading Boolean.

Do **not** change global `coerceInputValues`. Do **not** reopen C8/deeplink/recovery/ON_RESUME.

Rollback: revert DTO default + DataSource null branch + NetworkRetryHelper check + ViewModel/drawer API; tests pin prior behavior if needed.

### Risks

- Changing `prepareOpticaSelection` return type may touch characterization tests (`AuthDelegateCharacterizationTest`) and drawer UX — keep Boolean-only callers migrating carefully.
- Null-user → Error may surface Error UI where users previously saw wait-or-create; that is correct per spec but needs a clear message (“Sin sesión”) not onboarding.
- If any production rows truly omit `rol` in DB, they will be skipped (lose membership) instead of becoming admin — intentional fail-closed; verify DB constraint/default for `usuario_optica.rol` in design/propose (read-only).
- C7 false-after-anon may increase visible sync failures vs silent retry — desirable fail-closed; callers already abort on refresh failure path.

### Ready for Proposal

Yes — orchestrator should run **sdd-propose** for `fix-auth-membership-jwt` with Approach 1 locked, IN SCOPE = C6+C7+null-user Error+prepareOpticaSelection fail-closed, OUT = C8/CreatePin/deeplink/recovery/ON_RESUME, strict TDD.
