# Archive Report: Virtual Try-On

**Change**: `virtual-try-on`  
**Archived**: 2026-05-29  
**Branch**: `feature/virtual-try-on`  
**Artifact Store**: `hybrid` (Engram + filesystem)  
**Verify Verdict**: PASS WITH WARNINGS — no CRITICAL issues.

---

## Summary

All **27 tasks** complete across **4 chained PRs** (feature-branch-chain, auto-chain delivery strategy).  
Zero remaining work. Feature shipped and verified.

---

## Engram Artifact Lineage

| Artifact | Topic Key | Observation ID | Status |
|----------|-----------|----------------|--------|
| Spec | `sdd/virtual-try-on/spec` | **#358** | Persisted |
| Design | `architecture/virtual-try-on` | **#359** | Persisted |
| Tasks | `sdd/virtual-try-on/tasks` | **#360** | Persisted |
| Apply Progress | `sdd/virtual-try-on/apply-progress` | **#366** | Persisted |
| Verify Report | `sdd/virtual-try-on/verify-report` | **#370** | Persisted |
| Proposal | `sdd/virtual-try-on/proposal` | *Not in Engram* | Filesystem only |

> **Note**: The proposal artifact was authored in `proposal.md` on the filesystem but was not separately persisted to Engram under its own topic key.

---

## Archive Contents

All files moved to `openspec/changes/archive/2026-05-29-virtual-try-on/`:

| File | Description |
|------|-------------|
| `proposal.md` | Change intent, scope, approach, rollback plan |
| `design.md` | 6 architecture decisions, data flow, class designs, migration, UI flow |
| `tasks.md` | 27 tasks across 5 phases, review workload forecast |
| `apply-progress.md` | Phase-by-phase completion notes, TDD evidence, file changes, deviations |
| `verify-report.md` | Compliance matrix, build/test results, coverage, assertion quality, verdict |

---

## Specs Synced

**None.**  
No delta spec files existed in `openspec/changes/virtual-try-on/specs/`. The authoritative spec lives only in Engram (`sdd/virtual-try-on/spec`, obs #358). No filesystem sync was required.

---

## Implementation Summary

| PR | Phase | Work Unit | Tasks | Key Deliverables |
|----|-------|-----------|-------|------------------|
| PR 1 | Phase 1 | Foundation | 1.1–1.9 | Gradle deps, Room migration v21→v22, Montura entity fields, manifest permissions, Hilt module |
| PR 2 | Phase 2 | Core Domain | 2.1–2.5 | `FaceLandmarkerUseCase`, `FrameOverlayUseCase`, `FaceMeasurementExtractor`, 38 unit tests |
| PR 3 | Phase 3 | UI & Integration | 3.1–3.9 | `VirtualTryOnViewModel`, `VirtualTryOnScreen`, 5 UI components, navigation, drawer entry |
| PR 4 | Phase 4+5 | Testing & Cleanup | 4.1–4.3, 5.1–5.3 | Golden image test, dead code removal, `FacePreviewCanvas` bitmap fix |

---

## Test Summary

- **Total tests**: 864 (including ~800 pre-existing + 88 new)
- **Failures**: 0
- **Errors**: 0
- **New tests added**: 88
  - 19 `FaceMeasurementExtractorTest`
  - 19 `FrameOverlayUseCaseTest`
  - 27 `VirtualTryOnViewModelTest`
  - 22 `VirtualTryOnScreenTest`
  - 1 `FrameOverlayUseCaseGoldenTest`
- **JaCoCo domain coverage**: ~100% on domain layer (`FaceLandmarkerUseCase`, `FaceMeasurementExtractor`, `FrameOverlayUseCase`, `SegmentType`)
- **UI layer coverage**: Acceptable (limited by Robolectric Compose constraints)

---

## Warnings from Verify Report

1. **Assertion quality**: 5 WARNING-level tautologies in `VirtualTryOnScreenTest` (contract tests only; no correctness impact).
2. **Hardcoded confidence**: `FaceLandmarkerUseCase` sets `confidence = 1.0f` because MediaPipe 0.10.14 does not expose per-face confidence.
3. **Broad exception catch**: ViewModel `detectFace()` catches generic `Exception`; should distinguish `NoFaceDetectedException`, `LowConfidenceException`, and system errors.

All warnings are non-blocking and documented for future improvement.

---

## Rollback Plan (from Proposal)

1. Disable feature flag (`BuildConfig.VIRTUAL_TRY_ON_ENABLED = false`).
2. Remove MediaPipe dependency from `libs.versions.toml` and `build.gradle.kts`.
3. Keep Room migration (non-destructive, add-only columns).
4. Revert Montura entity (remove 4 new fields; Room ignores extra columns).

---

## Archive Verification

- [x] Change folder moved to archive (`openspec/changes/archive/2026-05-29-virtual-try-on/`)
- [x] All 5 filesystem artifacts present in archive
- [x] Active `openspec/changes/` directory no longer contains `virtual-try-on/`
- [x] No delta specs to sync (spec lives in Engram only)
- [x] Engram observation IDs recorded for traceability

---

## SDD Cycle Complete

The change has been fully planned, implemented, verified, and archived.  
Ready for the next change.
