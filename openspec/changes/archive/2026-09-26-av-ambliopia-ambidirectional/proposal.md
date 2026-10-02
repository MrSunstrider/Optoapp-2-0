# Proposal: AV Amblyopia Ambidirectional Parse

## Intent

Amblyopia auto-detection (AV cc lejos OD vs OI) currently parses **Snellen fractional only**. Decimal AV strings (`1.0`, `0.8`, `0,5`) return null, so auto never fires for decimal-only or mixed Snellen/decimal pairs — even though the clinical threshold `|ΔlogMAR| ≥ 0.19` is already implemented.

Authority: `docs/SRS.md` **RF-EVA-06** + **§8.2** (TO-BE **#1**). This change completes bidirectional AV → logMAR normalization so auto-amblyopia works for both input formats on Android, with OptoWeb helper parity in the same change (no UI redesign).

## Scope

### In Scope

- Android: add `DiagnosticoCalculator.parseAvToLogMar` (Snellen first, then decimal) and point `EvaluacionDiagnosticoHelper.computeOtrosAuto` at it
- Keep `parseSnellenToLogMar` API and characterization tests stable
- Strict TDD (RED → GREEN): decimal, comma locale, Snellen↔decimal equivalence, mixed OD/OI, unparseable → manual preserve, `|Δ| ≥ 0.19` boundary, `autoAmbliopia=false` preserve
- Extend `EvaluacionDiagnosticoHelperTest` (or equivalent) for `computeOtrosAuto` amblyopia paths (today: transpose-only)
- OptoWeb helper parity (same change, sibling repo `optoweb`):
  - Fix `snellenToLogMar` hardcode `20/den` → `num/den` per SRS formula
  - Add bidirectional `avToLogMar` (or equivalent) + wire `autoAmbliopiaValue` through it
  - Vitest coverage for decimal, equivalence, and numerator-aware Snellen
- Optional free characterization: Android `6/6` → `0.0` (documents existing num/den behavior; not new product work)

### Out of Scope

- Explicit 6m-notation product work or SRS example expansion (Android already uses captured numerator/denominator)
- UI format pickers, validation widgets, or redesign of AV fields
- Near VA fields (`avCc*` cerca / other eyes beyond `avCcOdLejos` / `avCcOiLejos`)
- Changing amblyopia threshold `0.19` or manual-preserve semantics
- Forcing `otrosAmbliopia` when either eye is unparseable
- Schema, migrations, sync, RLS, Room entities
- OptoWeb form/UI redesign beyond calling the fixed helpers

## Capabilities

> Contract for `sdd-spec`. No existing `openspec/specs/` capability covers amblyopia AV parse (researched 31 main specs; none mention ambliopía / logMAR / Snellen AV).

### New Capabilities

- `av-ambliopia-ambidirectional`: Bidirectional AV cc → logMAR parsing (Snellen fractional + decimal) and amblyopia auto-detection wiring per RF-EVA-06 / §8.2, including OptoWeb helper parity rules

### Modified Capabilities

- None

## Locked Product Decisions

| # | Decision | Lock | Rationale |
|---|----------|------|-----------|
| 1 | Bare integer ambiguity (e.g. `"20"`) | **Decimal path requires an explicit decimal separator** (`,` or `.`) after trim; then comma→dot; accept only finite values in **`(0, 2.0]`**. Bare integers without `/` and without separator → **null** (unparseable → manual preserve). Values with separator but `> 2.0` (e.g. `20.0`, `20,40`) → **null**. | Safer than SRS literal `(0, ∞)` for leftover Snellen numerators; avoids absurd negative logMAR. Users enter decimal VA as `1.0` / `0,8`, not bare `20`. Intentional clinical narrowing of the open upper bound for the decimal path only. Snellen path unchanged. |
| 2 | Web in scope | **YES** — helper parity in this change; no UI redesign | SRS: “alinear Web si aplica”; web already diverges (`20/den` ignores numerator) |
| 3 | Schema / RLS | **NONE affected** | Pure string → logMAR → boolean; no persistence shape change |

**Unresolved:** None (all decisions locked above).

## Approach

Follow exploration **Approach 2** (no hard blocker found):

1. **Android parse API** — Add `DiagnosticoCalculator.parseAvToLogMar(raw: String): Double?`:
   1. Trim; blank → null
   2. Try existing `parseSnellenToLogMar` (fractional wins; `20/40` never decimal-misparsed)
   3. Else decimal per Locked Decision #1 → `-log10(decimal)`
   4. Else null
2. **Call site** — `computeOtrosAuto` uses `parseAvToLogMar` for both eyes; threshold `0.19` and manual-preserve / `autoAmbliopia` gating unchanged
3. **TDD** — RED tests first (`DiagnosticoCalculatorTest` + helper amblyopia cases); GREEN implementation; keep all existing Snellen characterization green
4. **OptoWeb** — Mirror parse order in `avToLogMar`; fix `snellenToLogMar` to `Number(m[1]) / Number(m[2])`; keep `autoAmbliopiaValue` returning `null` when unparseable (caller semantics unchanged)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../diagnostico/DiagnosticoCalculator.kt` | Modified | Add `parseAvToLogMar`; leave `parseSnellenToLogMar` stable |
| `optoapp/.../EvaluacionDiagnosticoHelper.kt` | Modified | `computeOtrosAuto` → `parseAvToLogMar` |
| `optoapp/.../diagnostico/DiagnosticoCalculatorTest.kt` | Modified | RED/GREEN decimal, comma, equivalence, bare-integer reject, ceiling |
| `optoapp/.../EvaluacionDiagnosticoHelperTest.kt` | Modified | Amblyopia: mixed, unparseable→manual, threshold, `autoAmbliopia=false` |
| `optoapp/.../EvaluacionViewModelCatchRefactorTest.kt` | Unchanged (expected) | Snellen characterization stays; touch only if forced |
| `optoweb/src/lib/evaluacion-constants.ts` | Modified | `num/den` fix + bidirectional parse; wire `autoAmbliopiaValue` |
| `optoweb` Vitest for `evaluacion-constants` | Modified | Decimal + numerator-aware Snellen parity |
| Supabase schema / RLS / migrations | None | Explicitly unaffected |
| `docs/SRS.md` | None required | TO-BE already stated; status flip is archive/verify concern |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Decimal ceiling `(0, 2.0]` narrows SRS `(0, ∞)` — edge cases like rare VA `2.5` rejected | Low | Document as locked clinical safety rule; unparseable → manual preserve (safe default) |
| Web `20/den` → `num/den` changes logMAR for non-20 numerators | Med | Add Vitest for `6/6` → `0.0` and `20/20` → `0.0`; treat prior hardcode as bug |
| Ship parse-only without `computeOtrosAuto` coverage | Med | Mandatory helper tests for mixed / manual-preserve / auto flag |
| `20,40` (comma, no slash) hits decimal path → rejected by ceiling | Low | Spec as expected; true Snellen uses `/` |
| Cross-repo PR / review budget (Android + Web) | Low–Med | Helpers+tests only; forecast in tasks; chain if >400 authored lines |

## Rollback Plan

- Revert the Android commit(s) restoring `computeOtrosAuto` → `parseSnellenToLogMar` and removing `parseAvToLogMar` (or leave unused and unused-code lint later)
- Revert OptoWeb helper commit restoring `20/den` + Snellen-only `autoAmbliopiaValue`
- No DB migration to roll back; no feature flag required
- Characterization Snellen tests remain the safety net for regression detection

## Dependencies

- Exploration artifact: `openspec/changes/av-ambliopia-ambidirectional/exploration.md` + Engram `sdd/av-ambliopia-ambidirectional/explore`
- Authority: `docs/SRS.md` RF-EVA-06 + §8.2
- Sibling working tree: `C:\Users\usuario\Desktop\Programacion\OptoServices-SaaS\optoweb` (helper + tests only)
- Strict TDD: `openspec/config.yaml` `strict_tdd: true` / apply `tdd: true`

## Success Criteria

- [ ] Decimal AV (dot and comma) converts to logMAR and participates in amblyopia auto on Android
- [ ] Mixed Snellen/decimal OD–OI pairs evaluate `|ΔlogMAR| ≥ 0.19` correctly
- [ ] Equivalence: `20/40` and `0.5` yield the same logMAR (within float tolerance)
- [ ] Bare integers without separator (e.g. `"20"`) do not parse as decimal; manual amblyopia preserved when either eye unparseable
- [ ] Existing `parseSnellenToLogMar` characterization tests remain green without contract change
- [ ] OptoWeb: `snellenToLogMar` uses `num/den`; decimal path present; tests cover both
- [ ] No schema/RLS/migration changes
- [ ] Android focused unit tests for calculator + helper amblyopia paths pass under strict TDD
