# Design: Fix Auth Login Onboarding Recovery

## Technical Approach

WU1: server id, admin iff INSERT, `RETURNS text`, drop `opticas_insert_authenticated`, Android `decodeAs<String>()`. WU2: `completeOnboardingOptica`, keep GoTrue on empty, fail-close fetch/rol. WU3–7: cold start, Google Idle, scoped `user_profiles`, optional PIN, GoTrue. No C3. A10 unused.

**7 WUs stacked-to-main. WU1 before WU2.** WU5 own RLS PR. Strict TDD.

## Architecture Decisions

| Decision | Rejected | Choice |
|----------|----------|--------|
| A2 id | COALESCE client; ignore+`void` | Ignore `p_optica_id`; `RETURNS text`; persist decoded S |
| Attach admin | Always `usuario_optica` | Only if `ROW_COUNT=1`; else RAISE |
| Signature | `CREATE OR REPLACE` | **DROP+CREATE** (`void`→`text`). Same 6 args. GRANT authenticated |
| A3 | Rethrow | Sealed `MembershipFetch` (Ok/Empty/Error). Sync maps Error→empty |
| Empty list | `clearSession` | `saveOnboardingSession`: `isLoggedIn=true`, `opticaId=""` not `mi_optica_base` |
| A12 | `isNotEmpty` | `PostLoginNavigation.dest`; selector iff count>1 |
| Blank rol | `ifBlank{"admin"}` | Skip row |
| Google | Catch-only | Idle on cancel/no-session; Login ON_RESUME `onGoogleAuthAbandoned` if Loading |
| CreatePin | Always (C3) | Iff `isPinRequired && !pinHasBeenSet` |
| A8 | toml only | toml + greppable hosted dashboard note |
| Cap | RPC max=2 | Leave `trg_opticas_limit_guard`; no max in body |

## Data Flow

```mermaid
sequenceDiagram
  participant UI as SinOptica
  participant VM as AuthViewModel
  participant AD as AuthDelegate
  participant DS as OpticaSettingsDataSource
  participant RPC as create_optica_for_current_user
  UI->>VM: completeOnboardingOptica
  VM->>AD: completeOnboardingOptica
  AD->>DS: createOpticaForCurrentUser
  DS->>RPC: rpc (ignore p_optica_id)
  RPC->>RPC: INSERT opticas; ROW_COUNT=1 then admin
  RPC-->>DS: text S
  AD->>AD: saveSession(S)
  VM-->>UI: pin-or-main
```

Known `p_optica_id`: new UUID; no admin on that id. ROW_COUNT=0: RAISE. Post-login: Error keep session; Empty→SinOptica; 1→skip selector; >1→selector. Cold start: `isAuthChecked` then `resolvePostLogin`.

## File Changes

| File | Act | Why |
|------|-----|-----|
| `supabase/migrations/20260819120000_harden_create_optica_return_id.sql` | Create | WU1 RPC + drop INSERT policy |
| `supabase/migrations/20260819120100_user_profiles_select_scoped.sql` | Create | WU5 own-or-shared-óptica admin/gerente |
| `supabase/tests/verify_create_optica_bootstrap.sql` | Create | WU1 RED SQL |
| `supabase/tests/verify_user_profiles_select_rls.sql` | Create | WU5 RED SQL |
| `supabase/config.toml` | Modify | WU7 `password_requirements` |
| `docs/gotrue-hosted-password-policy.md` | Create | WU7 hosted step; A10 note |
| `.../membership/MembershipFetch.kt` | Create | Sealed fetch |
| `.../MembershipDataSource.kt` | Modify | Sealed; skip blank rol |
| `.../OpticaSettingsDataSource.kt` | Modify | Decode S; blank fails |
| `.../MembershipRepository.kt` | Modify | `MembershipFetch`; `asList()` for sync |
| `.../SessionManager.kt` | Modify | `saveOnboardingSession` + fakes |
| `.../auth/PostLoginNavigation.kt` | Create | Pure dest |
| `.../auth/AuthDelegate.kt` | Modify | No clear on 0 |
| `.../AuthViewModel.kt` | Modify | Error/Idle; abandon; resolve after check |
| `.../auth/PinDelegate.kt` | Modify | Empty/`!pinHasBeenSet` false |
| `.../MainActivity.kt` | Modify | `isAuthChecked` restore |
| `.../SinOpticaScreen.kt` | Modify | Owner form → RPC |
| `.../SeleccionOpticaScreen.kt` | Modify | Empty ≠ auto pin/main |
| `.../LoginScreen.kt` `RegisterScreen.kt` | Modify | Dest helper; Google abandon |

RED tests first; no new Robolectric. Do not merge C3. `createAdditionalOptica` unwired.

## Interfaces / Contracts

`MembershipFetch`: `Ok(list)` / `Empty` / `Error(cause)`. RPC `RETURNS text`; `decodeAs<String>()`; blank fails. `DROP FUNCTION create_optica_for_current_user(text×6)`.

## Testing Strategy

Unit: MockK `runTest`. SQL `verify_*.sql`. toml + grep.

| WU | RED |
|----|-----|
| **WU1** | SQL: known id no admin; id≠client; RETURN=`opticas.id`; INSERT policy gone; trigger remains; no max raise. Android: persist S not C; blank decode fails |
| **WU2** | Empty no `clearSession`; IOException≠onboarding; blank rol skipped; dest==1 skip selector; >1 selector; owner calls `completeOnboardingOptica`; no Android `invitaciones` |
| **WU3** | No restore before `isAuthChecked`; session leaves Login; none stays |
| **WU4** | Cancel / no-session → Idle or Error, not Loading |
| **WU5** | Own visible; anon empty; same-óptica admin peer visible; other hidden; empleado own-only |
| **WU6** | Both-empty false; unset never matches; CreatePin iff required&&unset; set→Pin; !required→Main |
| **WU7** | toml min 6 + `lower_upper_letters_digits_symbols`; note greps string, 6, dashboard/Management API |

## Threat Matrix

| Boundary | Applicability | Safe/failure | RED |
|----------|---------------|--------------|-----|
| Routing | **Applicable** | Wait `isAuthChecked`; empty→SinOptica; error≠SinOptica; size==1 skip selector | WU2, WU3, WU6 |
| Auth | **Applicable** | Keep session; Google not Loading; empty PIN false | WU2, WU4, WU6 |
| RLS | **Applicable** | Direct opticas INSERT denied; profiles scoped | WU1, WU5 |
| RPC | **Applicable** | Ignore client id; admin iff INSERT; return S | WU1 |
| Subprocess | **N/A** no spawn | — | — |
| Docs-like exec / git -C / commit / push / PR argv | **N/A** not in-app VCS | — | — |

## Migration / Rollout

WU1→WU7 stacked. WU2 needs WU1 live. WU5 separate. **GGA before remote apply.** No orphan backfill.

**Rollback:** Android WU7→WU2, then WU1/WU5. Restore `void` RPC (`20260716020000`), INSERT policy, global profiles SELECT. Revert toml + hosted.

## Open Questions

None. WU7 needs dashboard.
