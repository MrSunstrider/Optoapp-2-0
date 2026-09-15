# Archive Report: fix-jd-reportes-cluster

**Archived**: 2026-09-15
**Destination**: `openspec/changes/archive/2026-09-15-fix-jd-reportes-cluster/`
**HEAD (final)**: `2ea060c3235aead14c88dd9057377b61e9103e0b`
**Verdict at close**: PASS WITH WARNINGS (authentic verify admitted)
**Artifact store**: hybrid

## Final-State Authority

| Fact | Source (rank) | Statement |
|------|---------------|-----------|
| Date-picker Todo guard fixed | Orchestrator launch prompt + HEAD `2ea060c` | `reportesShowsDatePicker` = Diario\|Semanal only; Spacer fixed |
| Authentic re-verify replaced hand-edited report | Launch prompt + `verify-report.md` | `evidence_revision` sha256:`5841cf968bb5d0f951b17529925b1d8720df59ecaa2369173b2ba20f99074546`; verdict `pass_with_warnings` |
| CI green on HEAD | Launch prompt + verify-report | unit-tests + lint + build SUCCESS on `2ea060c` |
| Tasks 22/22 complete | Persisted `tasks.md` (gate) | All `[x]`; optional 2.1 and 5.4 SKIPPED with rationale |
| Exclusive S2 RPC contract | verify-report + shipped migrations | Intentional `[p_from, p_to)`; NOT a failure |

Intermediate snapshots (`apply-progress`, earlier fail verify) are historical only; close state follows the table above.

## Observation IDs Read (Engram + filesystem)

| Artifact | Engram ID | Filesystem locator |
|----------|-----------|--------------------|
| proposal | #2123 (`sdd/fix-jd-reportes-cluster/proposal`) | `openspec/changes/fix-jd-reportes-cluster/proposal.md` |
| spec | #2125 (`sdd/fix-jd-reportes-cluster/spec`) | `openspec/changes/fix-jd-reportes-cluster/specs/**/spec.md` |
| design | #2126 (`sdd/fix-jd-reportes-cluster/design`) | `openspec/changes/fix-jd-reportes-cluster/design.md` |
| tasks | #2127 (`sdd/fix-jd-reportes-cluster/tasks`) | `openspec/changes/fix-jd-reportes-cluster/tasks.md` |
| verify-report | #2135 (`sdd/fix-jd-reportes-cluster/verify-report`) | `openspec/changes/fix-jd-reportes-cluster/verify-report.md` |
| apply-progress (context) | #2133 | `openspec/changes/fix-jd-reportes-cluster/apply-progress.md` |

Note: Engram #2123/#2126/#2125/#2127 previews still mention early “inclusive S2” wording; filesystem delta + verify + HEAD ship **exclusive** `[p_from,p_to)`. Archive close follows filesystem + final-state facts.

## Task Completion Gate

- Unchecked implementation tasks: **0**
- Tasks complete: **22/22**
- Gate: **PASS**

## Spec Sync

Native `gentle-ai sdd-archive-compose` results at archive time:

| Domain | Compose result | Details |
|--------|----------------|---------|
| `cierre-caja` | Already applied (exit 1, ADDED exists) | Both ADDED requirements byte-identical in main vs delta (`Static user-facing load errors`, `Exclusive rpc_cierre_caja_resumen date upper bound`) |
| `sync` | Already applied (exit 1, ADDED exists) | ADDED `SyncFinanzasUseCase static Resource.Error messages` byte-identical in main |
| `reportes-financieros` | exit 0, output identical to canonical | MODIFIED Period Selection / Date Picker / Period-Based Pago Date Range already present |
| `analisis-negocio` | exit 1 on MODIFIED name mismatch | ADDED four requirements present and identical after encoding fix; MODIFIED content already present under legacy headings `### REQ-2:` and `### R23:` (delta names `### Requirement: REQ-2 User-Facing…` / `### Requirement: R23 Supabase RPC… stock_estancado restoration` do not rematch). Archive-time shell fix restored double-encoded `Análisis` in role-gate scenario to match delta UTF-8 |

### Specs Synced (content)

| Domain | Action | Details |
|--------|--------|---------|
| cierre-caja | Updated (pre-archive) | +2 ADDED requirements |
| sync | Updated (pre-archive) | +1 ADDED requirement |
| analisis-negocio | Updated (pre-archive + encoding fix) | +4 ADDED; REQ-2 + R23 stock/proyeccion language already merged under legacy IDs |
| reportes-financieros | Updated (pre-archive) | 3 MODIFIED requirements (live period labels + date picker + ranges) |

Destructive REMOVED deltas: none. `rules.archive` warn-before-destructive: N/A.

## Verification at Close

- blockers: 0
- critical_findings: 0
- requirements: 12/12 COMPLIANT
- scenarios: 25/25 COMPLIANT
- focused tests EXIT=0 (97 tests)
- CI: unit-tests + build SUCCESS @ `2ea060c`

### Residual warnings (non-blocking)

1. Optional task 2.1 SKIPPED (no Compose role-null harness)
2. Optional task 5.4 SKIPPED (inclusive ledger assert conflicts with exclusive contract)
3. I1 Robolectric DAO harness debt (IMPROVEMENT-PLAN L9)
4. Delivery `size:exception` (single PR #136)
5. Design Interfaces SQL snippet still shows inclusive `fecha <= p_to` while shipped contract is exclusive (suggestion only)

## Archive Move Readback

```
diff -r <snapshot>/source openspec/changes/archive/2026-09-15-fix-jd-reportes-cluster
diff_exit=0
(empty diff — PASS)
git mv succeeded
```

## Archive Contents

- proposal.md
- design.md
- tasks.md (22/22)
- specs/ (cierre-caja, sync, analisis-negocio, reportes-financieros)
- verify-report.md
- apply-progress.md, apply-notes-s2.md, gga-report.md, exploration.md
- archive-report.md (this file; additive)

## SDD Cycle

Planned → implemented → verified (authentic pass_with_warnings) → archived.
Ready for the next change. No push/merge performed by archive.
