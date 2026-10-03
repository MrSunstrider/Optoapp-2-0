## Exploration: av-ambliopia-ambidirectional

### Current State

Amblyopia auto-detection on Android is Snellen-only.

**Parse (AS-IS):** `DiagnosticoCalculator.parseSnellenToLogMar` accepts `(\d+)\s*/\s*(\d+)`, rejects blank/invalid/zero denominator, and returns `-log10(numerator/denominator)`. Comments claim web alignment; Android already uses the captured numerator (so `6/6` and `20/20` both yield `0.0`). Existing tests cover 20/x cases, spaced slash (`20 / 40`), and null paths — not decimal AV and not 6m characterization.

**Call site:** `EvaluacionDiagnosticoHelper.computeOtrosAuto` reads `avCcOdLejos` / `avCcOiLejos`, parses both with `parseSnellenToLogMar`, then:

- If both non-null → `otrosAmbliopia = (|ΔlogMAR| ≥ 0.19)` when `autoAmbliopia` is true
- If either null → keeps existing `state.otrosAmbliopia` (manual preserved; does not force false)

Wired from `EvaluacionViewModel.updateOtrosAuto()`. No other production call sites of `parseSnellenToLogMar`.

**Gap vs SRS RF-EVA-06 / §8.2:** Decimal AV (`1.0`, `0.8`, `0,5`, etc.) does not parse → auto never fires for decimal-only or mixed Snellen/decimal pairs. Authority: `docs/SRS.md` §8.2 + RF-EVA-06 (TO-BE #1). Strict TDD via `openspec/config.yaml`.

**Tests today:**

| Area | Coverage |
|------|----------|
| `DiagnosticoCalculatorTest` | Snellen only (7 cases) |
| `EvaluacionViewModelCatchRefactorTest` | Snellen approval/characterization (incl. spaced slash) |
| `EvaluacionDiagnosticoHelperTest` | Transpose only — **no** `computeOtrosAuto` / amblyopia cases |
| Decimal / mixed / threshold / manual-preserve | **Missing** |

**Web parity (optoweb):** `snellenToLogMar` + `autoAmbliopiaValue` in `evaluacion-constants.ts` — also Snellen-only. Important divergence: web uses `20 / den` (hardcoded numerator), ignoring captured num → `6/6` yields wrong logMAR on web. `autoAmbliopiaValue` returns `null` when unparseable (caller decides); Android keeps prior boolean. Vitest coverage exists for Snellen + threshold; no decimal tests. Used from `nueva-evaluacion-form.tsx`.

**6m notation (`docs/diagnostico-integral-optoapp.md` §2.27):** Doc still lists “hardcodes 20ft” as open, but Android code already uses `numerator/denominator`. SRS examples are `20/x`; fractional formula is general. **Out of scope for new behavior in THIS change** — Android already accepts any positive integer fraction. Optional free characterization test (`6/6` → `0.0`) is fine. Web numerator hardcode is a separate parity fix if web is aligned in this change.

No schema / sync / RLS impact. Domain is pure string → logMAR → boolean.

### Affected Areas

- `optoapp/.../diagnostico/DiagnosticoCalculator.kt` — add bidirectional AV→logMAR API; keep `parseSnellenToLogMar` behavior stable
- `optoapp/.../EvaluacionDiagnosticoHelper.kt` — `computeOtrosAuto` must call the bidirectional parser
- `optoapp/.../diagnostico/DiagnosticoCalculatorTest.kt` — RED/GREEN for decimal, comma, order, edges
- `optoapp/.../EvaluacionViewModelCatchRefactorTest.kt` — Snellen characterization stays; add only if API rename forces
- New or extended helper test for `computeOtrosAuto` amblyopia (decimal, mixed, unparseable → manual, threshold)
- `docs/SRS.md` — already states TO-BE; no product rewrite required in explore
- **Optional / recommended sibling:** `optoweb/src/lib/evaluacion-constants.ts` + `__tests__/evaluacion-constants.test.ts` — decimal + fix `20/den` → `num/den` for SRS formula parity
- UI fields (`avCcOdLejos` / `avCcOiLejos`) — no format restriction change required; free-text already

### Approaches

1. **Extend `parseSnellenToLogMar` to accept decimal** — If slash match fails, parse as decimal AV.
   - Pros: Single call-site swap unnecessary if signature reused; smallest call-site diff (zero if helper already calls it).
   - Cons: Name becomes a lie; conflates Snellen characterization tests with decimal; harder to reason about order/priority.
   - Effort: Low

2. **New `parseAvToLogMar` (Snellen first, then decimal)** — Compose: try `parseSnellenToLogMar`; else parse decimal `(0, ∞)` with comma→dot; return null if both fail. Point `computeOtrosAuto` at it. Keep `parseSnellenToLogMar` unchanged.
   - Pros: Clear API; preserves existing Snellen tests; matches SRS “both formats”; single call-site change; easy TDD surface; mirrors a future web `avToLogMar`.
   - Cons: One extra public function; callers must migrate intentionally (good).
   - Effort: Low

3. **Separate `parseDecimalAvToLogMar` + compose only in `computeOtrosAuto`** — No unified entry; helper does `snellen ?: decimal`.
   - Pros: Maximum separation of concerns; Snellen object stays pure.
   - Cons: Composition logic lives in helper (harder to unit-test parse matrix without helper fixtures); web would re-implement the same orchestration; less reusable.
   - Effort: Low–Medium

### Recommendation

**Approach 2:** add `DiagnosticoCalculator.parseAvToLogMar(raw: String): Double?`.

Parse order (must be specified in proposal/spec):

1. Trim; if blank → null  
2. Snellen fractional via existing `parseSnellenToLogMar` (so `20/40` never becomes a decimal misparse)  
3. Else decimal: `replace(',', '.')`, `toDoubleOrNull()`, accept only finite values in `(0, ∞)` → `-log10(decimal)`  
4. Else null  

`computeOtrosAuto` switches both eyes to `parseAvToLogMar`. Threshold and manual-preserve semantics stay as-is.

**Strict TDD (Android first):**

- RED: decimal `1.0`/`0.5`/`0,8`; equivalence `20/40` ↔ `0.5`; mixed OD/OI; unparseable eye keeps `otrosAmbliopia`; `|Δ| ≥ 0.19` boundary  
- GREEN: implement `parseAvToLogMar` + call-site  
- Keep existing Snellen tests green without changing Snellen contract  

**6m:** out of scope as a new requirement; Android already correct. Optional characterization only. Do not expand SRS examples in this change.

**Web:** Recommend including in the same change if line budget allows (~small helper + tests): add `avToLogMar` (or extend usage), fix `snellenToLogMar` to `num/den` per SRS, and add decimal/mixed tests. If PR risk/budget prefers Android-only, proposal MUST list web as explicit follow-up (SRS: “alinear Web si aplica”). Prefer same change for clinical parity.

**Not in scope:** DB migrations, UI format pickers, near VA fields, changing threshold `0.19`, forcing amblyopia when parse fails.

### Risks

- Bare integers like `"20"` (no slash) become decimal VA 20 → large negative logMAR; rare clinically but allowed by SRS `(0, ∞)` — document in spec; do not specially reject unless product later constrains.
- Extending `parseSnellenToLogMar` in place would muddy characterization tests and naming — avoided by Approach 2.
- Web `20/den` vs Android `num/den` already diverges for non-20 numerators; aligning web without tests could surprise if anyone relied on the bug.
- Missing `computeOtrosAuto` amblyopia tests today → easy to ship parse-only without verifying manual-preserve / `autoAmbliopia=false`.
- Mixed locale commas in Snellen (`20/40` fine; `20,40` is not Snellen) — Snellen-first then decimal means `20,40` → decimal 20.40; acceptable edge, note in spec.

### Ready for Proposal

Yes — ready for `sdd-propose`. Scope: Android bidirectional AV→logMAR for amblyopia auto (RF-EVA-06); recommend web helper parity in same change; 6m explicit work out of scope (already works on Android).
