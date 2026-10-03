# Design: AV Amblyopia Ambidirectional Parse

## Technical Approach

Complete RF-EVA-06 / SRS §8.2 amblyopia auto-detection by normalizing **both** Snellen fractional and decimal AV strings to logMAR before the existing `|ΔlogMAR| ≥ 0.19` comparison.

Maps directly to proposal Approach 2 (locked):

1. Android: add `DiagnosticoCalculator.parseAvToLogMar` (Snellen first, then decimal with locked ceiling); switch `computeOtrosAuto` to it; leave `parseSnellenToLogMar` unchanged.
2. OptoWeb: fix `snellenToLogMar` to `num/den`; add `avToLogMar`; wire `autoAmbliopiaValue` through it.
3. Strict TDD (RED → GREEN) on both repos. No schema, UI redesign, or 6m product work.

Authority: `docs/SRS.md` RF-EVA-06 + §8.2; locked product decisions in `proposal.md` (do not reopen).

## Architecture Decisions

### Decision: New `parseAvToLogMar` / `avToLogMar` vs extending Snellen parsers

**Choice**: New bidirectional entry points (`parseAvToLogMar` Android, `avToLogMar` Web). Keep `parseSnellenToLogMar` / `snellenToLogMar` as Snellen-only primitives.

**Alternatives considered**:
- Overload / extend `parseSnellenToLogMar` to accept decimal (exploration Approach 1)
- Compose Snellen + decimal only inside `computeOtrosAuto` / form (exploration Approach 3)

**Rationale**: Name honesty and characterization isolation. Existing Snellen approval tests (`DiagnosticoCalculatorTest`, `EvaluacionViewModelCatchRefactorTest`, Vitest `snellenToLogMar`) stay green without conflating decimal cases. Call-site migration is intentional and one-line. Web mirrors the same composition for clinical parity.

### Decision: Decimal path requires explicit separator and range `(0, 2.0]`

**Choice**: After Snellen fails: accept only trimmed strings that contain `.` or `,`; normalize comma→dot; parse finite `Double`/`number`; accept only values in **`(0, 2.0]`**. Reject bare integers (`"20"`), zero, negatives, `NaN`/`Infinity`, and values `> 2.0` (e.g. `20.0`, `20,40`).

**Alternatives considered**:
- SRS literal open upper bound `(0, ∞)` (exploration draft)
- Accept bare integers as decimal VA

**Rationale**: Locked clinical safety narrowing. Bare leftovers from incomplete Snellen entry (`"20"`) must not become absurd negative logMAR. Users enter decimal VA as `1.0` / `0,8`, not bare `20`. Ceiling rejects `20,40` (comma without slash) as unparseable → manual preserve (safe default). Snellen path unchanged.

### Decision: Snellen-first parse order

**Choice**: Trim → blank null → try fractional Snellen → else decimal rules → else null.

**Alternatives considered**: Decimal-first; parallel independent parsers with ad-hoc priority in the helper.

**Rationale**: Guarantees `20/40` never becomes a decimal misparse. Matches SRS “both formats” with fractional formula first. Shared order keeps Android/Web equivalence tables identical.

### Decision: Web `snellenToLogMar` uses captured `num/den`

**Choice**: Replace hardcode `20 / den` with `Number(m[1]) / Number(m[2])` (reject non-finite or `den <= 0`).

**Alternatives considered**: Leave web hardcode; document Android-only correctness for non-20 numerators.

**Rationale**: Prior hardcode is a bug vs SRS `−log10(numerador/denominador)`. Fixes `6/6` → `0.0` parity with Android. Existing Vitest cases for `20/x` remain numerically identical.

### Decision: Preserve platform unparseable semantics

**Choice**:
- Android `computeOtrosAuto`: either eye null → keep `state.otrosAmbliopia`; `autoAmbliopia=false` → keep manual regardless.
- Web `autoAmbliopiaValue`: either eye null → return `null`; form already preserves draft when `ambliAuto === null`.

**Alternatives considered**: Unify both platforms to force `false` on parse failure.

**Rationale**: Out of scope to change manual-preserve / null semantics. Only the AV→logMAR parser changes.

## Data Flow

```
  avCcOdLejos (string)          avCcOiLejos (string)
           │                              │
           ▼                              ▼
   parseAvToLogMar / avToLogMar   (same pipeline both eyes)
           │
           ├─ trim; blank? ──────────────► null
           ├─ Snellen /(\d+)\s*\/\s*(\d+)/ ► -log10(num/den)
           ├─ else decimal (sep + (0,2.0]) ► -log10(decimal)
           └─ else ──────────────────────► null
           │                              │
           ▼                              ▼
        logMAR_OD ─────────────────── logMAR_OI
           │                              │
           └──────── abs(Δ) ≥ 0.19 ───────┘
                          │
          ┌───────────────┼───────────────┐
          │ both non-null │ either null   │
          ▼               ▼               │
     amblyopia bool   preserve manual     │
          │           (Android bool /     │
          │            Web null)          │
          ▼                               │
   if autoAmbliopia → write otrosAmbliopia│
   else keep manual ◄─────────────────────┘
```

Call sites (unchanged orchestration, parser swap only):

| Platform | Orchestrator | Parser today | Parser TO-BE |
|----------|--------------|--------------|--------------|
| Android | `EvaluacionDiagnosticoHelper.computeOtrosAuto` ← `EvaluacionViewModel.updateOtrosAuto()` | `parseSnellenToLogMar` | `parseAvToLogMar` |
| Web | `nueva-evaluacion-form.tsx` derived state | `snellenToLogMar` via `autoAmbliopiaValue` | `avToLogMar` via `autoAmbliopiaValue` |

No persistence / sync / RLS path: `otrosAmbliopia` boolean (Android) / `"true"|"false"` string (Web draft) shape unchanged.

## File Changes

### OptoApp (this repo)

| File | Action | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/viewmodel/diagnostico/DiagnosticoCalculator.kt` | Modify | Add `parseAvToLogMar`; keep `parseSnellenToLogMar` stable |
| `optoapp/src/main/java/com/example/optoapp/viewmodel/EvaluacionDiagnosticoHelper.kt` | Modify | `computeOtrosAuto` calls `parseAvToLogMar` for both eyes |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/diagnostico/DiagnosticoCalculatorTest.kt` | Modify | RED→GREEN: decimal, comma, equivalence, bare-int reject, ceiling, optional `6/6` characterization |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/EvaluacionDiagnosticoHelperTest.kt` | Modify | Amblyopia paths for `computeOtrosAuto` (mixed, threshold, manual preserve, `autoAmbliopia=false`) |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/EvaluacionViewModelCatchRefactorTest.kt` | Unchanged (expected) | Snellen approval suite stays; touch only if forced |

### OptoWeb (sibling: `../optoweb`)

| File | Action | Description |
|------|--------|-------------|
| `src/lib/evaluacion-constants.ts` | Modify | Fix `snellenToLogMar` to `num/den`; add `avToLogMar`; wire `autoAmbliopiaValue` |
| `src/lib/__tests__/evaluacion-constants.test.ts` | Modify | Numerator-aware Snellen, decimal, equivalence, ceiling/bare-int, mixed `autoAmbliopiaValue` |
| `src/components/pacientes/nueva-evaluacion-form.tsx` | Unchanged (expected) | Already uses `autoAmbliopiaValue` + null→manual preserve |

### Explicitly untouched

- Supabase migrations / RLS / Room entities / sync
- AV UI widgets / format pickers
- Near VA fields beyond `avCcOdLejos` / `avCcOiLejos`
- `docs/SRS.md` product rewrite (status flip is archive/verify)

## Interfaces / Contracts

### Android — `DiagnosticoCalculator`

```kotlin
/** Snellen fractional only — UNCHANGED contract. */
fun parseSnellenToLogMar(snellen: String): Double?

/**
 * Bidirectional AV → logMAR.
 * 1) trim; blank → null
 * 2) parseSnellenToLogMar (fractional wins)
 * 3) else decimal: requires '.' or ',' after trim; comma→dot;
 *    finite value in (0.0, 2.0] → -log10(value); else null
 */
fun parseAvToLogMar(raw: String): Double?
```

`computeOtrosAuto` (behavior preserved except parser):

```kotlin
val logMarOd = DiagnosticoCalculator.parseAvToLogMar(state.avCcOdLejos)
val logMarOi = DiagnosticoCalculator.parseAvToLogMar(state.avCcOiLejos)
val ambliopiaVal = if (logMarOd != null && logMarOi != null) {
    abs(logMarOd - logMarOi) >= 0.19
} else {
    state.otrosAmbliopia
}
// otrosAmbliopia = if (state.autoAmbliopia) ambliopiaVal else state.otrosAmbliopia
```

### Web — `evaluacion-constants.ts`

```ts
/** Snellen fractional; TO-BE uses captured numerator/denominator. */
export function snellenToLogMar(raw: string): number | null;

/** Bidirectional AV → logMAR; same order/rules as Android parseAvToLogMar. */
export function avToLogMar(raw: string): number | null;

/** Uses avToLogMar; returns null when either eye unparseable (caller preserves). */
export function autoAmbliopiaValue(avOd: string, avOi: string): boolean | null;
```

Float comparison tolerance in tests: **0.001** (Android existing) / Vitest `toBeCloseTo(..., 2–5)` matching current suite.

### Equivalence table (shared expected logMAR)

| Input | Path | Expected logMAR (approx) |
|-------|------|--------------------------|
| `20/20` | Snellen | `0.0` |
| `20/40` | Snellen | `≈ 0.3010` (`-log10(0.5)`) |
| `0.5` / `0,5` | Decimal | same as `20/40` |
| `1.0` / `1,0` | Decimal | `0.0` |
| `0.8` / `0,8` | Decimal | `≈ 0.0969` |
| `20 / 40` | Snellen (spaced) | same as `20/40` |
| `6/6` | Snellen | `0.0` (Android already; Web after `num/den` fix) |
| `20` | Reject | `null` (no separator) |
| `20.0` / `20,40` | Reject | `null` (exceeds ceiling) |
| `2.0` / `2,0` | Decimal | `≈ -0.3010` (`-log10(2)`) — at ceiling inclusive |
| `0` / `0.0` / blank / `abc` | Reject | `null` |

## Testing Strategy

Strict TDD per `openspec/config.yaml` (`strict_tdd: true`, apply `tdd: true`).

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (Android calculator) | `parseAvToLogMar` matrix: decimal dot/comma, equivalence vs Snellen, bare-int reject, ceiling, blank/invalid; existing Snellen characterization stays green; optional `6/6 → 0.0` | JUnit 4 in `DiagnosticoCalculatorTest` — **RED first**, then implement |
| Unit (Android helper) | `computeOtrosAuto`: mixed Snellen/decimal OD–OI; `|Δ| ≥ 0.19` true/false boundaries; unparseable eye preserves `otrosAmbliopia`; `autoAmbliopia=false` preserves manual even when Δ would fire | Extend `EvaluacionDiagnosticoHelperTest` — **RED first** |
| Unit (Web snellen) | `20/x` unchanged; add `6/6 → 0`; confirm `num/den` | Vitest — RED for `6/6` before fix if desired |
| Unit (Web av + auto) | Same equivalence/ceiling/bare-int matrix; `autoAmbliopiaValue` mixed decimal/Snellen + null preserve | Vitest in `evaluacion-constants.test.ts` |
| Integration / E2E | N/A for this change | Pure helpers; form already wired |
| Regression | Existing Snellen approval suites must remain green without contract edits | Do not rewrite Snellen characterization assertions |

### RED → GREEN sequence (apply order)

1. **Android RED**: add failing `parseAvToLogMar` + `computeOtrosAuto` amblyopia tests.
2. **Android GREEN**: implement `parseAvToLogMar` + switch call site.
3. **Web RED**: add failing `avToLogMar` / numerator / decimal / `autoAmbliopiaValue` cases.
4. **Web GREEN**: fix `snellenToLogMar`, add `avToLogMar`, wire `autoAmbliopiaValue`.
5. Re-run focused suites; keep full Android unit suite green before push/GGA.

### Helper scenarios (minimum)

**`computeOtrosAuto` (Android)** — fixture `EvaluacionUiState` with `autoAmbliopia=true` unless noted:

| Scenario | OD | OI | Prior `otrosAmbliopia` | Expected |
|----------|----|----|------------------------|----------|
| Mixed below threshold | `20/20` | `1.0` | any | `false` |
| Mixed at/above threshold | `20/20` | `0.5` | any | `true` (`Δ≈0.301`) |
| Decimal-only | `1.0` | `0,5` | any | `true` |
| Unparseable OD | `""` / `"20"` | `20/20` | `true` | stays `true` |
| Unparseable OI | `20/20` | `"abc"` | `false` | stays `false` |
| Auto off | `20/20` | `0.5` | `false`, `autoAmbliopia=false` | stays `false` |

**`autoAmbliopiaValue` (Web)** — same parse matrix; expect `null` (not boolean) when either eye unparseable.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Pure string → logMAR → boolean helpers.

## Migration / Rollout

No migration required. No feature flag. No DB / RLS / Room changes.

Delivery note for `sdd-tasks`: touch list is helpers + unit tests across two repos. Forecast authored-line budget; if combined PR exceeds ~400 lines, chain Android then Web (or reverse) as separate reviewable slices. Web form file expected untouched.

## Risks / Mitigations

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Decimal ceiling `(0, 2.0]` narrower than SRS `(0, ∞)` rejects rare VA `> 2.0` | Low | Locked decision; unparseable → manual preserve; document in spec/tasks |
| Web `num/den` changes logMAR for non-20 numerators | Med | Treat prior hardcode as bug; Vitest `6/6` and `20/20` both `0.0`; existing `20/x` stay identical |
| Ship parser without helper coverage | Med | Mandatory `computeOtrosAuto` / `autoAmbliopiaValue` RED tests before GREEN |
| `20,40` hits decimal then ceiling-reject | Low | Spec as expected; real Snellen uses `/` |
| Cross-repo review budget | Low–Med | Helpers+tests only; chain PRs if forecast High |
| Float drift on equivalence | Low | Shared tolerance 0.001 / `toBeCloseTo`; assert OD/OI via same helper |

## Rollback

1. **Android**: revert commit(s) restoring `computeOtrosAuto` → `parseSnellenToLogMar` and removing `parseAvToLogMar` (or leave dead code only if mid-revert — prefer full revert).
2. **Web**: revert helper commit restoring `20/den` hardcode and Snellen-only `autoAmbliopiaValue`.
3. No DB migration / feature flag to undo.
4. Snellen characterization suites are the regression safety net.

## Open Questions

None — all product decisions locked in `proposal.md`. Spec phase may formalize RFC 2119 requirements from this design without reopening ADRs.
