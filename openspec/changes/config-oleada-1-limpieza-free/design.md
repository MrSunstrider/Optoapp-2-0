# Design: config-oleada-1-limpieza-free

**RDD**: disabled/unmanaged · **Schema**: new migration after `20260819120100_…`

## Approach

1. Domain: `maxOpticas(FREE)=1`; `maxPacientes`/`canAddPaciente` always allow; add `canAddOptica(plan, count)`.
2. Auth: before create additional, `fetchMembershipsForCurrentUser` — if Ok with size≥1 or Empty is false with ≥1, fail. Empty/0 → allow first… wait createAdditional implies already has one → always size≥1 → always fail for FREE. Correct: **always block createAdditionalOptica when memberships.size >= maxOpticas(FREE)=1** i.e. when size >= 1.
3. DB: same semantics in trigger.
4. UI: card label; delete About; gate; lab Text banner; `var advancedExpanded by remember { false }`.

## Risks

- PRO_MULTISITE cannot add 2nd óptica until Oleada 3 plan-aware trigger — accepted (no multi customers yet).
- AuthDelegate tests that construct AuthDelegate — no new ctor deps if gate uses MembershipRepository only.
