# Judgment Day — analisis-financiero-fase3 — Round 1 ledger

target_identity: sha256=3b70992aa93d9043baa833c1bbb0e98b52a176caeef6877f34d94fc87dcd3510  
baseline: 7ca7d722  
head: 2d5f9eb6  
branch: feat/analisis-financiero-fase3  
round: 1

## Confirmed (both judges)

| ID | Severity | Claim | Status |
|----|----------|-------|--------|
| JD-C1 | CRITICAL | Upload upserts all local seguimiento rows without remote `updated_at` compare → stale device overwrites newer remote | fixed |
| JD-C2 | CRITICAL | `marcarSeguimiento` never `markLocallyModified`; download `keepLocal` requires `!isSynced` → skipUpload/partial upload wipes newer local with older remote | fixed |

Locations: `SyncSeguimientoUseCase.kt` upload/download; `FeedbackRecomendacionUseCase.kt` marcarSeguimiento.

## Suspect (single-judge / verify)

None beyond the shared CRITICALs (both judges named the same two defects with different primary locations).

## INFO / WARNING (auto-fix per standing JD authorization)

| ID | Severity | Claim | Status |
|----|----------|-------|--------|
| JD-W1 | WARNING | Offline `ObtenerAnalisisMensualUseCase.fallbackToRoom` leaves category lists empty → MIX omitted offline while online RPC has priors | fixed |
| JD-W2 | WARNING | SyncSeguimiento tests only cover blank skip; no LWW regression tests (Judge A) | fixed |
| JD-W3 | WARNING | Legacy remote UUID rows with `periodo=''` orphan beside stableId rows (Judge B) | fixed |

## Contradictions

none

## Correction plan (round 1)

1. JD-C2: `marcarSeguimiento` → `syncStateTracker.markLocallyModified(opticaId, "feedback_recomendacion", stableId)` after upsert.
2. JD-C1: Upload only pending/locally-modified rows OR fetch remote updated_at and skip when remote newer (mirror `uploadLocallyModified` / ConflictHelper).
3. JD-C2 download: align keepLocal with `keepPendingLocalEdit` pattern (pending edit + local newer wins even when comparing timestamps correctly).
4. JD-W2: unit tests for stale-upload skip and download keepLocal after markLocallyModified.
5. JD-W1: offline category priors from resumen_diario if data exists; otherwise document N/A if Room has no category breakdown — prefer real offline parity if facts available.
6. JD-W3: migration cleanup OR download/delete orphans — prefer safe SQL delete/update of empty-periodo legacy rows for same optica when stableId row exists, or one-time DELETE WHERE periodo='' after columns exist (verify no production consumers need UUID ids).

## Judge refs

- Judge A round1: 92900d5d-4f95-4abb-8d34-8da007a918be
- Judge B round1: f45366af-7ad9-4f0c-9457-ee9275e2758e
- Fix actor: 2fe1104f-0344-4f48-9983-595324877640 → 38378cd8, 93e831a4, df2778f8
- Rejudge A: 6d0a054b-dbb0-4339-8bd1-6cae2a53f3aa findings:[]
- Rejudge B: 3b3735ae-30c7-45f0-a150-2f5731409088 findings:[]

## Terminal

scoped_rejudgment: approved  
terminal_state: approved  
JUDGMENT: APPROVED
