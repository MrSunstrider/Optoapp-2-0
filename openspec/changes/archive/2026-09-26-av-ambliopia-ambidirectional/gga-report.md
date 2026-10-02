# GGA-eq Report: av-ambliopia-ambidirectional

**Date**: 2026-09-25  
**Mode**: GGA-eq (Cursor 4R lenses)  
**Why not native**: `gentle-ai review start --agent cursor` fails on Gentle AI 3.7.0 — runtime `cursor` does not advertise review transport; supported immutable review runtimes are claude-code / opencode / codex (blocked by Cursor-only policy).

## Verdict: CLEAN (all lenses)

| Lens | Agent | Verdict |
|------|-------|---------|
| R1 Risk | review-risk | CLEAN — no privilege/auth/RLS/network/secrets surface |
| R2 Readability | review-readability | PASS (clean) — naming/complexity/intention clear |
| R3 Reliability | review-reliability | CLEAN — locked parse rules + Android/Web parity + verify evidence |
| R4 Resilience | review-resilience | CLEAN — null→manual preserve; invalid input fail-closed |

## Evidence anchors

- Android: `parseAvToLogMar` + `computeOtrosAuto` wire; focused 72 green; full suite 2430 green
- OptoWeb: `avToLogMar` + `snellenToLogMar` num/den; Vitest 61 green
- Spec/design: decimal `(0, 2.0]` with explicit separator; Snellen-first; threshold `0.19`

## Non-blocking notes

- R2 SUGGESTION: bare `0.19` / `2.0` could become named constants (pre-existing pattern for anisometropia only)
- Delivery still uncommitted; feature-branch-chain: PR1 Android / PR2 OptoWeb
- SRS `docs/SRS.md` RF-EVA-06 still marked P/TO-BE #1 — update on merge/archive closeout

## Refuter

Not required — zero inferential severe findings across lenses.
