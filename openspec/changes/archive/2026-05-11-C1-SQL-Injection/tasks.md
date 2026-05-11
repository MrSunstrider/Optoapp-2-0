# Tasks: Fix SQL Injection in Paciente Search

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~70 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Create shared escape utility + test + wire both consumers | PR 1 | Single PR; all changes are under 100 lines |

## Phase 1: Foundation (TDD — RED)

- [x] 1.1 Create failing test `web/src/lib/sanitize.test.ts` covering 6 scenarios from spec: normal search, malicious %, literal %/_, backslash, empty/whitespace, RLS preservation (import from `@/lib/sanitize`)
- [x] 1.2 Implement `escapeIlikeFragment()` in `web/src/lib/sanitize.ts` — escape `\` first, then `%`, then `_`; export named function
- [x] 1.3 Verify tests pass: `npm test` in `web/`

## Phase 2: Integration — Wire consumers

- [x] 2.1 Modify `web/src/lib/pacientes.ts` — remove module-private `escapeIlikeFragment` (lines 31-33), add `import { escapeIlikeFragment } from "@/lib/sanitize"`
- [x] 2.2 Modify `web/src/data/repositories/SupabasePacienteRepository.ts` — add import, escape search term with `escapeIlikeFragment()`, add empty-string guard (skip `.or()` when trimmed result is empty)

## Phase 3: Verification

- [x] 3.1 Run full test suite (`npm test` in `web/`) — confirm all existing tests pass
- [x] 3.2 Verify all 6 acceptance criteria via code review: AC1 (no raw interpolation), AC2 (shared import, no duplicate), AC3 (no `any`), AC4 (no catch-and-silence), AC5 (tests pass), AC6 (filter string preserves 3 fields)
