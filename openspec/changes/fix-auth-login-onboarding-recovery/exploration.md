# Exploration: fix-auth-login-onboarding-recovery

Close all integral-auth findings A1–A12 (login, signup, create óptica, recovery, PIN). Diagnosis existed; this phase verified each finding against current code. No new novel.

**RDD**: `rdd_mode=disabled/unmanaged` — record principles only; no receipts.
**TDD**: `openspec/config.yaml` `strict_tdd: true`.
**Delivery**: auto-chain stacked-to-main; **WU1 RPC security before WU2 owner UI**.

---

## Current State

Auth is Android-first: `AuthViewModel` orchestrates `AuthDelegate` (GoTrue email/Google/recovery + membership RPCs), `PinDelegate` (local 6-digit PIN), `BackupDelegate`. Post-login membership comes from `usuario_optica` via `MembershipDataSource`. Owner bootstrap is `create_optica_for_current_user` (SECURITY DEFINER). Employee join is `assign_optica_role_by_email` (no invite-code product). PIN is optional (`SessionManager.isPinRequired` defaults **false**). Recovery (archived C4) is already wired: `optoapp://auth` fragment `type=recovery` → `NewPasswordScreen`.

Existing specs do **not** cover these gaps: `openspec/specs/login-screen/spec.md` is layout-only; archived `2026-06-29-C4-Password-Recovery` is complete recovery, not onboarding. Unfinished sibling `openspec/changes/C3-Default-PIN/` must **not** be revived (product: PIN stays optional).

### Finding verification (A1–A12)

| ID | Severity | Verdict | Evidence |
|----|----------|---------|----------|
| A1 | P0 | **Confirmed + amplified** | `completeOnboardingOptica` / `createAdditionalOptica` exist on VM/delegate; **zero Compose call sites**. `SinOpticaScreen` “Soy dueño / Crear mi óptica” navigates to `SeleccionOptica`. That screen `LaunchedEffect` auto-navigates to pin/main when `pendingMemberships.isEmpty()`. `resolvePostLogin` else-branch calls `sessionManager.clearSession()` then `requiresOnboarding=true`. Global `OptoAppNavigation` guard then pops to Login when `isLoggedIn==false`. Owner path is dead; employee wait races the logout guard. |
| A2 | P0 | **Confirmed** | Prod function `20260716020000_fix_create_optica_no_overwrite.sql`: `v_optica_id := COALESCE(NULLIF(p_optica_id, ''), server uuid)`. `INSERT opticas ON CONFLICT DO NOTHING` then **always** `INSERT usuario_optica (…, 'admin')`. Android always sends `"opt_" + 16 hex` (`OpticaSettingsDataSource` L52). Authenticated caller who knows an existing `opticas.id` joins as admin. Overwrite was fixed; **join-as-admin was not**. RPC returns `void`; client trusts its own id. |
| A3 | P1 | **Confirmed** | `MembershipDataSource.fetchMembershipsForCurrentUser` catch `IOException`/`Exception` → `emptyList()`. `resolvePostLogin` treats empty as onboarding + `clearSession`. Network error looks like “no óptica”. |
| A4 | P1 | **Confirmed** | `NavHost` `startDestination = Route.Login`. `checkExistingSession()` runs in `onCreate` and sets `isAuthChecked=true` in `finally`; **no collector** in `OptoAppNavigation`. Login `LaunchedEffect` only routes on `AuthState.Success`. Cold start with valid JWT stays on Login. `pinHasBeenSet` is also collected and unused (C3 leftover). |
| A5 | P1 | **Confirmed** | `loginWithGoogle()` sets `AuthState.Loading` then `signInWith(Google)`. Cancel Custom Tabs does not complete the coroutine as Error/Idle; `CancellationException` is rethrown. UI stuck on Loading until process death. |
| A6 | P1 | **Confirmed** | `user_profiles_select_access` (`20260423100000_…`): own row **OR** any `usuario_optica` row with `rol in ('admin','gerente')` — **not joined on `optica_id`**. Any admin/gerente can SELECT every email in `user_profiles`. Android members UI uses `optica_members` view filtered by `optica_id`; the table policy is still global. |
| A7 | P1 | **Confirmed** | `PinDelegate.validatePin`: `_pinInput.value == securityManager.userPin.first()`. Unset PIN is `""`. `PinDelegateTest.validatePin both empty returns true`. `PinScreen` OK has **no length gate**. `CreatePinScreen` is registered; **no `navigate(Route.CreatePin)`** anywhere. Product: keep PIN optional; wire CreatePin only if `isPinRequired && !pinHasBeenSet`. |
| A8 | P1 | **Confirmed** | UI (`RegisterScreen`, `NewPasswordScreen`) requires lower+upper+digit+symbol, min 6. `supabase/config.toml` `[auth]`: `minimum_password_length = 6`, `password_requirements = ""`. Hosted GoTrue is dashboard-controlled; repo file alone does not change production. |
| A9 | P2 | **Confirmed** | Latest `opticas_insert_authenticated` (`20260627005400`): authenticated INSERT if `uid` + non-empty `id`/`nombre`. Bypasses RPC; creates orphan `opticas` with no `usuario_optica`. Prod 11 opticas vs 3 memberships is consistent with this hole. RPC is SECURITY DEFINER so it does not need the INSERT policy. |
| A10 | P2 | **Confirmed / out of scope** | Table `invitaciones` exists (RLS added `20260714000006`); Android has no read/write. Product: employee path = existing email + `assign_optica_role_by_email`. **Do not implement invite-by-codigo.** Document as dead table. |
| A11 | P2 | **Confirmed** | `MembershipDataSource` L45: `rol = row.rol.ifBlank { "admin" }`. Blank role becomes admin in session. |
| A12 | P2 | **Confirmed** | `resolvePostLogin` size==1 already `saveSession` and `requiresSelection=false`, but still returns the one membership. Login/Register navigate to `SeleccionOptica` if `pendingMemberships.isNotEmpty()` — extra tap for the only óptica. |

### Related constraints (not in A-list; do not expand scope)

- `trg_opticas_limit_guard` / `enforce_optica_limit_for_creator` still raises at **2 memberships** on `opticas` INSERT. Product: **do not reintroduce `max_opticas=2` in the RPC**. Leave trigger as-is unless a later change explicitly drops it.
- `overrideAccessToken` is accepted by `createOpticaForCurrentUser` and never used.
- `site_url` in `config.toml` is `https://optoweb.vercel.app`; `optoapp://auth` is `additional_redirect_urls` only (AGENTS.md `site_url = optoapp://auth` is stale).
- `IMPROVEMENT-PLAN.md` still labels `create_optica_for_current_user` as SAFE — stale vs A2.

---

## Affected Areas

### Android — owner onboarding + post-login routing

- `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt` — stop `clearSession` on 0 memberships; distinguish fetch error vs empty; PIN/session restore consumers.
- `optoapp/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt` — `needsOnboarding`, `isAuthChecked`, Google Loading→Idle, onboarding APIs already present.
- `optoapp/src/main/java/com/example/optoapp/ui/screens/SinOpticaScreen.kt` — owner button must call `completeOnboardingOptica` (new form), not `SeleccionOptica`.
- `optoapp/src/main/java/com/example/optoapp/ui/screens/SeleccionOpticaScreen.kt` — empty-list auto-nav is wrong for onboarding; keep for true multi-membership.
- `optoapp/src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt` / `RegisterScreen.kt` — A12: route size==1 to pin/main; size==0 to `sin_optica` **without** clearing GoTrue session.
- `optoapp/src/main/java/com/example/optoapp/MainActivity.kt` — A4: consume `isAuthChecked`; CreatePin only when required && unset.
- `optoapp/src/main/java/com/example/optoapp/ui/screens/CreatePinScreen.kt` / `PinScreen.kt` / `PinDelegate.kt` — A7.

### Android — data

- `optoapp/src/main/java/com/example/optoapp/data/membership/MembershipDataSource.kt` — A3 fail-closed fetch; A11 blank rol.
- `optoapp/src/main/java/com/example/optoapp/data/membership/OpticaSettingsDataSource.kt` — stop trusting client `opt_*` id; consume RPC-returned id (WU1 contract).
- `optoapp/src/main/java/com/example/optoapp/data/MembershipRepository.kt` — thin pass-through; tests already exist for no-session.

### Supabase

- `supabase/migrations/20260716020000_fix_create_optica_no_overwrite.sql` — current RPC body (replace, do not edit in place).
- New migration: ignore client `p_optica_id`, membership only if optica row inserted, **return** `optica_id`, drop `opticas_insert_authenticated`, scope `user_profiles_select_access`.
- `supabase/config.toml` `[auth]` — A8 `password_requirements = "lower_upper_letters_digits_symbols"` (local). Dashboard note for hosted GoTrue.
- `supabase/tests/` — no behavioral tests for create_optica join-as-admin today (`verify_assign_optica_role_rpc.sql` only covers assign-by-email).

### Tests to write first (strict TDD)

- SQL: known `p_optica_id` of existing óptica does **not** insert admin membership; RPC returns server id; INSERT policy gone.
- `MembershipDataSource` / `AuthDelegate.resolvePostLogin`: IOException ≠ onboarding; empty list ⇒ onboarding **without** `clearSession`.
- `PinDelegate.validatePin`: empty never true.
- `AuthViewModel.loginWithGoogle`: cancel/complete-without-session → Idle or Error, not stuck Loading.
- Login/Register dest: memberships.size==1 skips `SeleccionOptica`.

### Out of scope

- Invite-by-codigo / using `invitaciones` (A10 document only).
- Mandatory PIN for all (C3).
- `max_opticas=2` in RPC.
- Orphan óptica cleanup in prod (A9 data; optional later, not this change).
- Web `optoweb` (except dashboard password note).

---

## Approaches

### 1. Stacked WUs — security RPC first, then owner UI (recommended)

Match delivery: auto-chain stacked-to-main.

| WU | Findings | Slice |
|----|----------|--------|
| **WU1** | A2, A6, A9, A8 | Migration: server-generated id only; `GET DIAGNOSTICS` / `FOUND` so membership INSERT runs only if óptica row was inserted; `RETURNS text` (or json) with new id. Drop `opticas_insert_authenticated`. Scope `user_profiles` to peers sharing an `optica_id` where caller is admin/gerente. Android `OpticaSettingsDataSource` consumes returned id (contract, even with no UI). `config.toml` password_requirements + dashboard note. SQL tests RED→GREEN. |
| **WU2** | A1, A3, A4, A12 | Owner form on `SinOptica` (or dedicated screen) calling existing `completeOnboardingOptica`. Do **not** `clearSession` on 0 memberships; keep GoTrue user, set `needsOnboarding`. Fetch errors → `AuthState.Error`, not SinOptica. `isAuthChecked` drives start route. size==1 skips selection. Employee wait stays email + `assign_optica_role_by_email`. |
| **WU3** | A5, A7, A11 | Google cancel → Idle/Error. `validatePin` false if input blank or `!pinHasBeenSet`. Navigate `create_pin` iff `isPinRequired && !pinHasBeenSet`. Blank rol: skip row or fail-closed (not `"admin"`). |
| **Docs** | A10 | Dead `invitaciones` table; no Android surface. |

- Pros: P0 privilege bugs land before any new owner UI can call the RPC; reviewable slices vs 400-line budget; TDD per WU; matches closed product assumptions.
- Cons: WU1 changes RPC return type; WU2 cannot ship owner create until WU1 is on prod (or WU1 includes client decode).
- Effort: Medium (3 stacked PRs + docs). High if treated as one PR.

### 2. Monolithic single PR

All A1–A12 in one branch.

- Pros: one migration + one Android nav rewrite; fewer integration windows.
- Cons: blows 400-line review budget; RPC + Compose + PIN + RLS in one diff; blocked GGA surface.
- Effort: High.

### 3. UI-first owner form, harden RPC later

Wire `completeOnboardingOptica` now; fix A2 after.

- Pros: unblocks “Crear mi óptica” fastest.
- Cons: new UI would call the join-as-admin RPC; violates delivery and P0 ordering.
- Effort: Medium — **rejected**.

### A2 implementation variants (inside WU1)

| Variant | Pros | Cons |
|---------|------|------|
| **Ignore client id + membership only if INSERT succeeded + return id** | Stops join-existing and id prediction; client cannot save a phantom id | Signature change (`void` → `text`); Android must decode |
| Keep client id, only attach if `row_count=1` | Smaller RPC change | Client can still pick unused ids; Android still trusts client id |
| Ignore client id but keep `void` | Stops join-existing | Android `saveSession` would use a non-existent client id until WU2 fetch |

Choose the first variant.

### A3 implementation variants (inside WU2)

| Variant | Pros | Cons |
|---------|------|------|
| **Sealed `MembershipFetch` (Ok list / Empty / NetworkError)** | UI can show “sin conexión” vs SinOptica; TDD-clear | Small API surface change |
| Rethrow IOException | Minimal | Callers must all catch; easy to regress to emptyList |

Choose sealed/result type at DataSource boundary.

---

## Recommendation

**Approach 1** with A2 variant “ignore client id + insert-membership-only-on-new-row + return id”.

Closed product assumptions to encode in proposal:

1. Employee path = existing email + `assign_optica_role_by_email`. No invite-code.
2. PIN remains optional (`isPinRequired` default false). Fix empty bypass. `CreatePin` only if `isPinRequired && !pinHasBeenSet`. Do not merge unfinished C3-Default-PIN “always create_pin”.
3. A8: align GoTrue to `lower_upper_letters_digits_symbols`, min 6 — `config.toml` **and** hosted dashboard (repo file is not prod).
4. Do **not** put `max_opticas=2` back in the RPC.
5. A10: document dead `invitaciones`; do not build it.

Owner UI (WU2) should reuse `completeOnboardingOptica` (nombre + fiscal fields already on the delegate). `createAdditionalOptica` can stay admin/gerente-only and be wired later in Config if needed; **A1 P0 is first-óptica onboarding**, not sucursales.

`resolvePostLogin` must keep the Supabase session when memberships are empty so `SinOptica` is reachable (today `clearSession` fights the global login guard).

### RDD principles (no receipts)

1. Security-sensitive RPC/RLS lands before UI that depends on it.
2. Fail closed: network ≠ no óptica; blank rol ≠ admin; empty PIN ≠ valid.
3. Optional PIN stays default-off; do not expand to mandatory-PIN-for-all.
4. Employee join stays assign-by-email; unused invite table is documented, not productized.

---

## Risks

- **WU1/WU2 coupling**: if WU1 returns a new id but Android still persists the client `opt_*` hex, first owner create after WU1 (tests/manual RPC) would session-bind the wrong id. WU1 **must** include `OpticaSettingsDataSource` decode.
- **Hosted GoTrue vs `config.toml`**: A8 is incomplete without a dashboard change (or Management API). Proposal must list the dashboard step.
- **`clearSession` race**: fixing A1 without removing `clearSession` on empty memberships will still bounce owners to Login.
- **2-óptica INSERT trigger**: still live; additional sucursales may fail with the free-plan message even though RPC has no max. Do not silently drop it in this change.
- **C3-Default-PIN leftover folder**: specs there contradict optional PIN. Proposal/spec must explicitly not merge those requirements.
- **Orphan opticas (A9 prod data)**: dropping INSERT policy stops new orphans; existing 8 extras need a separate, explicit cleanup (out of scope).
- **Google OAuth cancel**: supabase-kt may not throw; Idle restore may need `Activity` lifecycle / `DisposableEffect`, not only `catch`.
- **GGA / remote migration**: WU1 is a remote DB change; GGA before apply to prod.

---

## Ready for Proposal

**Yes.** Orchestrator should run **sdd-propose** for `fix-auth-login-onboarding-recovery` with Approach 1, WU1→WU2→WU3 stacking, closed product assumptions above, and strict TDD. Do not reopen invite-code, mandatory PIN, or `max_opticas=2` in RPC.
