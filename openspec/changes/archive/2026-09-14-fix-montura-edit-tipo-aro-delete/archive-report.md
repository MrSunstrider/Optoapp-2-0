# Archive Report: fix-montura-edit-tipo-aro-delete

**Change**: fix-montura-edit-tipo-aro-delete  
**Archived to**: `openspec/changes/archive/2026-09-14-fix-montura-edit-tipo-aro-delete/`  
**Date**: 2026-09-14  
**Artifact store**: hybrid  
**Branch**: fix/montura-edit-tipo-aro-delete  
**Status at close**: archived / SDD cycle complete (intentional-with-warnings for deferred remote CASCADE GGA)

## Final-State Authority

| Source | Role at close |
|--------|----------------|
| Persisted `tasks.md` | All 16 implementation/archive tasks `[x]` including 5.1/5.2 |
| Orchestrator launch prompt | Judgment Day **APPROVED** after R2 (C1 atomic sibling save + S1 inactive UNIQUE exclusion fixed); soft-delete snapshot / edit canEdit remain info; GGA for remote CASCADE still pending |
| `verify-report` (#2112) | Intermediate snapshot: PASS WITH WARNINGS; 5/5 requirements, 13/13 scenarios; suite 2391 passed / 0 failed / 6 skipped; CRITICAL none |

Stale verify claims that tasks 5.1/5.2 and Judgment Day were still pending are **superseded** at close: 5.1 completed by this archive merge; 5.2 completed per orchestrator Judgment Day APPROVED after R2.

## Observation IDs Read (Engram)

| Artifact | Observation ID | Topic |
|----------|----------------|-------|
| proposal | #2107 | `sdd/fix-montura-edit-tipo-aro-delete/proposal` |
| spec | #2108 | `sdd/fix-montura-edit-tipo-aro-delete/spec` |
| design | #2109 | `sdd/fix-montura-edit-tipo-aro-delete/design` |
| tasks | #2110 | `sdd/fix-montura-edit-tipo-aro-delete/tasks` |
| verify-report | #2112 | `sdd/fix-montura-edit-tipo-aro-delete/verify-report` |

Filesystem counterparts under the change folder (pre-move) / archive (post-move) were also read for hybrid consistency.

## Task Completion Gate

- Phases 1–4 were already `[x]` at verify time.
- Tasks 5.1 and 5.2 were unchecked by design (archive merge + Judgment Day).
- **Exceptional reconciliation** (orchestrator-authorized): marked 5.1/5.2 `[x]` before archive move because (a) 5.1 is performed in this phase, (b) launch prompt states Judgment Day APPROVED after R2, (c) verify-report explicitly deferred 5.1/5.2 as non-apply gates with zero CRITICAL findings.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `inventario-stock` | Updated via native compose | `gentle-ai sdd-archive-compose --canonical openspec/specs/inventario-stock/spec.md --delta openspec/changes/fix-montura-edit-tipo-aro-delete/specs/inventario-stock/spec.md --output openspec/specs/inventario-stock/spec.md.compose-tmp` then `mv` to replace canonical. Exit 0. |

Post-compose canonical counts: **8** `### Requirement:` headings, **17** `#### Scenario:` headings.

Delta applied:
- **ADDED** (4): Edit montura rim type and material controls; Soft-delete montura from inventory; Edit spawn sibling rim-type variant; Montura child FKs cascade on hard delete
- **MODIFIED** (1): Multi rim-type create with per-type initial stock (ADR-2 amend for edit sibling spawn)
- **REMOVED/RENAMED**: none (non-destructive; `rules.archive` destructive-warn not triggered)

Unrelated prior requirements preserved (SKU uniqueness, search distinguishes variants, Aluminio material).

## Mechanical Archive Move

- Destination: `openspec/changes/archive/2026-09-14-fix-montura-edit-tipo-aro-delete/`
- `git mv` reported empty-source quirk on Windows; source still present unchanged → plain `mv` fallback after snapshot integrity check
- Active path `openspec/changes/fix-montura-edit-tipo-aro-delete` absent after move (`SOURCE_GONE_OK`)

### Verbatim `diff -r` readback (pre-move snapshot vs destination)

```text
=== DIFF_R_READBACK_START ===
=== DIFF_R_READBACK_END status=0 ===
```

Empty diff (status 0) — byte-identity PASS. Archive-report is additive-only and excluded from that comparison.

## Archive Contents

- proposal.md ✅
- specs/inventario-stock/spec.md ✅
- design.md ✅
- tasks.md ✅ (16/16 complete)
- exploration.md ✅
- apply-progress.md ✅
- verify-report.md ✅

## Shipped Behavior (final)

- Edit: exclusive FilterChips → `form.tipoAro`; material via `OptoDropdownMenuField`; accesorio hides rim/material
- Soft-delete UI only (`activo=false`); Delete gated `canDeleteRecords`; inactive hidden from catalog
- Edit sibling spawn via `updateMontura` then `insertMonturas` (ADR-2 amend); UNIQUE error retained
- Room `MIGRATION_52_53` + entity CASCADE; Supabase file `20260914000000_montura_fk_cascade.sql` in-repo
- Judgment Day: APPROVED after R2 (C1 + S1 fixed)

## Open Follow-ups (not archive blockers)

1. **Remote Supabase CASCADE**: do **not** apply `20260914000000_montura_fk_cascade.sql` until GGA (R1/R3/R4) — noted from verify-report and launch prompt; soft-delete UI does not depend on remote CASCADE.
2. Info-level JD suspects (soft-delete snapshot / edit canEdit) remain informational only.

## SDD Cycle Complete

Planned → designed → tasked → applied → verified (pass with warnings) → Judgment Day approved → specs synced → archived.
No git commit/push performed by archive executor (orchestrator owns delivery).
