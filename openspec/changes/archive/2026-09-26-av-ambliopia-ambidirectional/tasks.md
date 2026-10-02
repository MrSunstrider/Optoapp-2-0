# Tasks: AV Amblyopia Ambidirectional Parse

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 420–560 authored (Android ~220–300 + OptoWeb ~200–260; helpers + unit tests) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 Android (calculator + helper + tests) → PR 2 OptoWeb (helpers + Vitest) |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No (locked by orchestrator 2026-09-25)
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

Cross-repo note: OptoWeb lives at `../optoweb` (`C:\Users\usuario\Desktop\Programacion\OptoServices-SaaS\optoweb`). Android and Web MUST stay separate work units / PRs. Threat matrix is N/A — no threat-matrix RED tasks.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Android bidirectional parse + `computeOtrosAuto` wiring | PR 1 (base = main or feature tracker) | `./gradlew :optoapp:testDebugUnitTest --tests "com.example.optoapp.viewmodel.diagnostico.DiagnosticoCalculatorTest" --tests "com.example.optoapp.viewmodel.EvaluacionDiagnosticoHelperTest" --stacktrace` | N/A — pure unit helpers; no device/UI | Revert Android commits restoring `parseSnellenToLogMar` call site and removing `parseAvToLogMar` |
| 2 | OptoWeb `num/den` fix + `avToLogMar` + `autoAmbliopiaValue` | PR 2 (base = PR 1 branch if feature-branch-chain; else main after PR 1) | `cd ../optoweb && npm test -- src/lib/__tests__/evaluacion-constants.test.ts` | N/A — pure Vitest helpers; form already wired | Revert Web helper commit restoring `20/den` and Snellen-only `autoAmbliopiaValue` |

Ask before apply (ask-on-risk): choose `stacked-to-main`, `feature-branch-chain`, or `size-exception`.

---

## Spec → task map

| Spec requirement | Tasks |
|------------------|-------|
| Bidirectional AV to logMAR parse order | 1.1, 1.2, 3.1, 3.2 |
| Decimal AV acceptance and rejection rules | 1.1, 1.2, 3.1, 3.2 |
| Snellen fractional parse stability | 1.2, 1.3, 4.1 |
| Snellen and decimal logMAR equivalence | 1.1, 1.2, 3.1, 3.2 |
| Amblyopia auto integration on Android | 2.1, 2.2 |
| Unparseable eye preserves manual amblyopia | 2.1, 2.2, 3.1, 3.2 |
| OptoWeb helper parity | 3.1, 3.2, 4.2 |

---

## Phase 1: Android — `parseAvToLogMar` (TDD)

- [x] 1.1 **RED** — In `optoapp/src/test/java/com/example/optoapp/viewmodel/diagnostico/DiagnosticoCalculatorTest.kt`, add failing tests for `DiagnosticoCalculator.parseAvToLogMar` (tolerance `0.001`) covering: blank/whitespace → null; `20/40` and `20 / 40` → `-log10(0.5)`; decimals `0.5`, `0,8`, `1.0`, `0.8`, `2.0` → `-log10(...)`; reject `20`, `0.0`, `-0.5`, `20.0`, `20,40`, incomplete `/40`/`20/`/`/`; equivalence `20/40`≡`0.5` and `20/20`≡`1.0`; optional free char `6/6` → `0.0`. Do **not** implement production yet. Confirm RED via focused Gradle test command for this class.
- [x] 1.2 **GREEN** — In `optoapp/src/main/java/com/example/optoapp/viewmodel/diagnostico/DiagnosticoCalculator.kt`, add `parseAvToLogMar(raw: String): Double?`: trim → blank null → `parseSnellenToLogMar` first → else decimal requiring `.` or `,`, comma→dot, finite value in `(0.0, 2.0]` → `-log10(value)` else null. Leave `parseSnellenToLogMar` contract unchanged. Make 1.1 pass.
- [x] 1.3 **REGRESSION** — Re-run existing Snellen characterization in `DiagnosticoCalculatorTest` and `optoapp/src/test/java/com/example/optoapp/viewmodel/EvaluacionViewModelCatchRefactorTest.kt` (read-only unless a forced fix is required). Assert no Snellen contract edits; all prior Snellen cases remain green.

## Phase 2: Android — `computeOtrosAuto` amblyopia wiring (TDD)

- [x] 2.1 **RED** — In `optoapp/src/test/java/com/example/optoapp/viewmodel/EvaluacionDiagnosticoHelperTest.kt`, add failing `computeOtrosAuto` amblyopia cases on `EvaluacionUiState` (`autoAmbliopia=true` unless noted): mixed `20/20`+`1.0` → `otrosAmbliopia=false`; mixed `20/20`+`0.5` / `20/40`+`1.0` → `true`; decimal-only `1.0`+`0,5` → `true`; below-threshold pair → `false`; inclusive `|Δ|=0.19` → `true`; unparseable OD `"20"` with prior `otrosAmbliopia=true` stays `true`; blank OI with prior `false` stays `false`; `autoAmbliopia=false` with `20/20`+`0.5` and prior `false` stays `false`. Confirm RED before production change.
- [x] 2.2 **GREEN** — In `optoapp/src/main/java/com/example/optoapp/viewmodel/EvaluacionDiagnosticoHelper.kt`, switch both eyes in `computeOtrosAuto` from `parseSnellenToLogMar` to `parseAvToLogMar`. Keep threshold `0.19`, null→preserve `state.otrosAmbliopia`, and `autoAmbliopia=false` manual preserve. Make 2.1 pass. Do not touch UI, Room, sync, or near-VA fields.

## Phase 3: OptoWeb — helper parity (TDD)

Sibling repo: `../optoweb` (absolute: `C:\Users\usuario\Desktop\Programacion\OptoServices-SaaS\optoweb`). Edit only helpers + Vitest; do not redesign `nueva-evaluacion-form.tsx`.

- [x] 3.1 **RED** — In `../optoweb/src/lib/__tests__/evaluacion-constants.test.ts`, add failing Vitest cases: `snellenToLogMar("6/6")` → `0.0` (proves `num/den`, not `20/den`); `20/20` → `0.0`; `avToLogMar` matrix matching Android (dot/comma decimals, equivalence `20/40`≡`0.5`, reject `20`/`20.0`/`20,40`/`0.0`, blank null, spaced Snellen); `autoAmbliopiaValue` mixed decimal/Snellen threshold behavior; either-eye unparseable → `null` (not boolean). Confirm RED with `npm test -- src/lib/__tests__/evaluacion-constants.test.ts` from `../optoweb`.
- [x] 3.2 **GREEN** — In `../optoweb/src/lib/evaluacion-constants.ts`: fix `snellenToLogMar` to `Number(m[1]) / Number(m[2])` (reject non-finite or `den <= 0`); add `avToLogMar` with Android-identical order/rules; wire `autoAmbliopiaValue` through `avToLogMar` (still return `null` when either eye unparseable). Make 3.1 pass. Leave `../optoweb/src/components/pacientes/nueva-evaluacion-form.tsx` (read-only) unchanged unless a one-line import rename is forced.

## Phase 4: Verification gate

- [x] 4.1 **Android focused + Snellen suite** — Re-run Unit 1 focused command plus `EvaluacionViewModelCatchRefactorTest`. Confirm all Phase 1–2 tests green and existing Snellen characterization unchanged.
- [x] 4.2 **Web focused + schema untouched** — Re-run Unit 2 Vitest command. Confirm no new/modified files under `supabase/migrations/` (read-only), no RLS/Room entity edits in this change, and form file remains untouched as expected.
- [x] 4.3 **Work-unit closeout** — Mark checkboxes complete per slice; if applying as chained PRs, finish Unit 1 verification before starting Unit 2. Do not push without GGA + full `:optoapp:testDebugUnitTest` per project policy (verify/archive concern, not apply blockers for this task list itself).
