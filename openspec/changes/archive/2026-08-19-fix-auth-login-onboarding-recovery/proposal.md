# Proposal: Fix Auth Login Onboarding Recovery

## Intent

Close A1–A12: first-óptica create; employee wait without logout; no join-as-admin or global email SELECT; session/PIN/Google/password fail closed. PIN stays optional.

## Closed Assumptions

1. Employee = email + `assign_optica_role_by_email`. No invite-code.
2. PIN optional. Empty invalid. CreatePin iff `isPinRequired && !pinHasBeenSet`.
3. A8: `lower_upper_letters_digits_symbols` min 6 in `config.toml` and dashboard.
4. No `max_opticas=2` in RPC. Leave `trg_opticas_limit_guard`.
5. Auto-chain stacked-to-main. Seven WUs (400-line budget).
6. A10: `invitaciones` unused — docs only.

## Scope

### In Scope

Strict TDD. WU1 before WU2. WU5 separate RLS PR.

- WU1 A2+A9: server RPC id; admin iff INSERT; return id; drop `opticas_insert_authenticated`; Android uses returned id.
- WU2 A1+A3+A11+A12: `completeOnboardingOptica`; no `clearSession` on 0; fetch error ≠ empty; blank rol ≠ admin; size==1 skip selector.
- WU3 A4: `isAuthChecked` restores pin/main.
- WU4 A5: Google cancel → Idle.
- WU5 A6: `user_profiles` SELECT scoped to caller óptica.
- WU6 A7: empty PIN invalid; CreatePin iff required && unset.
- WU7 A8: `password_requirements` + dashboard note.

### Out of Scope

Invite-code UI; mandatory PIN; RPC max; drop INSERT trigger; orphan cleanup; optoweb except A8 note.

## Capabilities

No main `android-auth`. Do not modify `login-screen` or `optica-config-settings`. Do not merge C3.

### New Capabilities

- `android-auth-onboarding`: owner form; keep session at 0; `MembershipFetch`; blank rol fail-closed; size==1 skip selector; assign-by-email. Document unused `invitaciones`.
- `android-auth`: `isAuthChecked` cold start; Google Idle; optional PIN.
- `supabase-optica-bootstrap`: ignore `p_optica_id`; membership-on-insert-only; RETURNS server id; drop authenticated `opticas` INSERT.
- `supabase-user-profiles-rls`: SELECT peers sharing caller `optica_id`.
- `gotrue-password-policy`: local + hosted min 6 + complexity.

### Modified Capabilities

- None.

## Approach

New migration (not `20260716020000_…`). Decode returned id in WU1. Reuse `completeOnboardingOptica`.

## Affected Areas

- `supabase/migrations/`: RPC replace; drop INSERT policy; rewrite `user_profiles_select_access`.
- `supabase/config.toml`: `password_requirements`.
- Android auth/data/UI: returned id, onboarding, fetch, routing, PIN, Google.

**Supabase schema/RLS: YES** (WU1, WU5). GGA before remote apply.

## Risks

- Client `opt_*` id → WU1 Android decode.
- Hosted GoTrue ignores toml → WU7 dashboard step.
- Google cancel no throw → lifecycle Idle.
- C3 merge would force CreatePin → explicit non-merge.

## Rollback Plan

Revert Android WU7→WU2, then WU1/WU5. Down-migration restores `void` RPC, `opticas_insert_authenticated`, global `user_profiles_select_access`. Revert `config.toml` and dashboard policy. No orphan backfill.

## Dependencies

WU2 needs WU1 deployed. Dashboard access for A8. GGA + `testDebugUnitTest` before push.

## Success Criteria

- [ ] Known `p_optica_id` does not grant admin; RPC returns server id; INSERT policy gone
- [ ] Owner create works; empty memberships keep session; fetch error ≠ SinOptica
- [ ] Cold start leaves Login; Google cancel not Loading; empty PIN false
- [ ] Unrelated `user_profiles` hidden; GoTrue matches UI; `invitaciones` unused in docs
