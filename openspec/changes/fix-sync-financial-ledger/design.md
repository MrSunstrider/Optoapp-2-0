# Design: Fix Sync Financial Ledger

## Technical Approach

DB-first typed ledger: `monto ≥ 0` always; sign only via shared `PagoEffect` / `pago_effect(tipo, monto)`. Cancel/reclaim keep originals and insert linked `Reverso` / positive `Reembolso`. Expand remote estado CHECKs. Upload quarantines invalid rows with truthful `markError` (never skip→success). Narrow download skip only for `quarantine:` rows — PRD LWW otherwise. Pacientes: evidence capture only.

Maps proposal invariants 1–5 and plan PR-1…PR-5.

## Architecture Decisions

| Decision | Rejected | Choice + rationale |
|----------|----------|-------------------|
| Negative montos | Relax `pagos_monto_chk` | Keep `monto ≥ 0`; tipo owns sign — stops dual ledgers |
| Cancel semantics | Delete / negative Anulación | Keep originals + one full `Reverso`; idempotent via `reversa_pago_id` |
| Claim refund | Overload Anulación | `Reembolso` positive magnitude; `reversa_pago_id` NULL |
| Effect placement | Duplicate CASE per RPC | Single SQL `pago_effect` + Kotlin `PagoEffect` — matrix-tested |
| Unknown tipo | Crash / treat as Abono | Effect `0`; upload quarantine — never silent credit |
| Quarantine vs conflict | Restore `filterConflicts` on finanzas | Validation quarantine only (`quarantine:` prefix); LWW otherwise |
| Partial upload | Swallow `UploadPartialException` as Success | `Resource.Error` with partial `FinanzasSyncResult` data; remote `last_status=error` |
| Parent-child | Upload orphans blindly | Gate child pagos if parent quarantined/not uploaded this cycle |
| Legacy Anulación | Drop tipo / rewrite cash | Keep tipo; Room `ABS(monto)`; effect `0`; no new writers |
| Delivery | Single PR | `auto-chain` five WUs, each ≤400 authored lines |

## Data Model — `reversa_pago_id`

| Layer | Shape |
|-------|--------|
| Postgres | `reversa_pago_id text NULL`; `FK pagos(id) ON DELETE RESTRICT`; `CHECK ((tipo='Reverso' AND reversa_pago_id IS NOT NULL) OR (tipo<>'Reverso' AND reversa_pago_id IS NULL))`; `UNIQUE INDEX pagos_reversa_pago_id_uidx ON pagos(reversa_pago_id) WHERE tipo='Reverso' AND reversa_pago_id IS NOT NULL` |
| Room `Pago` | `reversaPagoId: String? = null`; `@Index(["reversaPagoId"])`; DB version **43→44** |
| DTO | `PagoRemoto.@SerialName("reversa_pago_id") reversaPagoId`; round-trip in `toRemoto`/`toEntity` |
| Semantics | Points **from Reverso → original**. At most one full Reverso per original. Reembolso never sets this field. |

## Interfaces

```kotlin
object PagoEffect {
  fun signedAmount(tipo: String, monto: Double): Double = when (tipo.trim()) {
    "Abono", "Pago completo" -> monto
    "Reembolso", "Reverso" -> -monto
    else -> 0.0 // includes Anulación + unknown
  }
}
```

```sql
CREATE FUNCTION public.pago_effect(p_tipo text, p_monto numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE btrim(COALESCE(p_tipo,''))
    WHEN 'Abono' THEN p_monto WHEN 'Pago completo' THEN p_monto
    WHEN 'Reembolso' THEN -p_monto WHEN 'Reverso' THEN -p_monto
    ELSE 0 END;
$$;
```

Transactional APIs (ViewModels call these; retain entity/DAO originals):

| API | Behavior |
|-----|----------|
| `CancelServicioExtraUseCase` | `estado=Anulado`; per active Abono/Pago completo without Reverso → insert `Reverso` (same monto/método, `reversaPagoId=original.id`); stamp `updatedAt`; schedule finanzas once; repeat = no-op |
| `CancelDispensacionUseCase` | Same for dispensación `estadoEntrega=Anulado` |
| `ReclaimDispensacionUseCase` | `estadoEntrega=Reclamada`; insert `Reembolso` positive magnitude (claim amount); no `reversaPagoId`; no delete |

## Data Flow

```
UI cancel/reclaim → UseCase @Transaction → Room (estado + Reverso/Reembolso)
       → schedule finanzas sync
Upload: validate → partition → upsert valid chunks → on 23514 binary-split to 1-row
       → markSynced only successes; markError(quarantine:…) failures
       → if any quarantine/partial → Resource.Error (data may hold counts)
Download: apply remote LWW EXCEPT skip overwrite when local sync_entity_state
       error starts with "quarantine:" for that entityId
```

```mermaid
sequenceDiagram
  participant VM as ViewModel
  participant UC as Cancel/Reclaim UseCase
  participant Room as Room
  participant Up as UploadSyncCoordinator
  participant PG as Postgres
  VM->>UC: cancel/reclaim
  UC->>Room: tx estado + Reverso/Reembolso
  Up->>Up: validate partition
  Up->>PG: upsert valid
  alt 23514 on chunk
    Up->>PG: binary-split to single row
    Up->>Room: markError quarantine poison
  end
  Note over Up: never markSynced invalid; batch not ok if quarantine remains
```

## Convergence Points (all → `pago_effect` / `PagoEffect`)

| Surface | Change |
|---------|--------|
| `trg_pagos_update_monto_pagado` | delta = `pago_effect(NEW…)` − `pago_effect(OLD…)` |
| `recalcular_resumen_diario`, `rpc_cierre_caja*`, `rpc_deudores`, `rpc_analisis_*` | `SUM(pago_effect(tipo,monto))`; keep `estado IS DISTINCT FROM 'Anulado'` on ventas/servicios/disp |
| `PagoDao.sumMonto*` | Sum signed effect in Kotlin or SQL CASE matching effect |
| `CalcularMontoPagadoUseCase`, Cierre/Reportes/Análisis/Dispensacion/Servicios readers | Replace `filter!=Anulación` / signed `sumOf` with `PagoEffect` |
| Tests asserting negative Anulación nets | Rewrite to Reverso/effect matrix |

## Upload / Sync Truth

1. **Validate** before upsert: `monto≥0`, known tipo/método, XOR origen, Reverso↔`reversaPagoId` rules, estado domains.
2. **Partition**: valid upload; invalid → `markError(optica, type, id, "quarantine:…")` — **never** `markSynced`.
3. **Per-row isolation**: on chunk RestException 23514 / constraint name → binary-split; survivors synced; poison quarantined.
4. **Batch**: `markSynced(batch)` only if zero quarantines/failures this run; else `markError(batch,…)`.
5. **Partial**: `safeUpload` must **not** treat `UploadPartialException` as clean success; module returns `Resource.Error` with optional partial counts; `SyncViewModel` → `last_status=error`.
6. **Parent-child**: skip pago upload when parent dispensación/servicio is quarantined or failed this cycle; `markError(pago, "quarantine:parent_missing:…")`.
7. **Download guard**: only `quarantine:` IDs — not general conflict helper (PRD LWW).

## Migration / Compatibility / Preflight

**Order (production):** (1) read-only preflight SQL (2) GGA (3) apply Supabase migs (CHECKs NOT VALID → function/triggers/RPCs → column/FK/unique → VALIDATE when clean) (4) ship app Room 44 + writers (5) CLK-LX3 verify.

**Preflight counts:** invalid estado; `monto<0`; orphan XOR; tipo/método OOD; duplicate Reverso candidates; Anulación sign inventory.

**Repair:** Room `UPDATE pagos SET monto=ABS(monto) WHERE tipo='Anulación' AND monto<0`; bump `updatedAt`. Remote ABS only if preflight finds negatives (unexpected). Do **not** reinterpret historical cash beyond abs + effect 0.

**Mixed clients:** Expanded Anulado/Reclamada CHECKs unlock old soft-deletes immediately. Old negative Anulación writers still fail `pagos_monto_chk` until app upgrade — acceptable. New Reverso column nullable → old clients ignore field.

## Diagnostics + Pacientes

- Restore durable background-error UI on diagnostics card; copy-all via `SyncErrorSanitizer` (Bearer/apikey redacted; status, PG code, constraint, entity IDs).
- One controlled pacientes sync on CLK-LX3; capture full HTTP; classify; open `fix-sync-pacientes-http` — **no** schema guess in this change.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `supabase/migrations/20YYMMDD*_ledger_pago_effect.sql` | Create | `pago_effect`, trigger rewrite, RPC CASE→effect, estado CHECKs, `reversa_pago_id`+FK+unique |
| `domain/PagoEffect.kt` | Create | Kotlin effect matrix |
| `domain/CancelServicioExtraUseCase.kt` (+ Disp cancel/reclaim) | Create | Transactional ledger ops |
| `data/dispensacion/DispensacionEntity.kt` | Modify | `reversaPagoId` on `Pago` |
| `data/OptoDatabase.kt` + `OptoDatabaseMigrations.kt` | Modify | v44 + ABS Anulación |
| `data/pago/PagoDao.kt` | Modify | effect-aware sums; reversa queries |
| `domain/SyncFinanzasDto.kt` | Modify | DTO field + mapping |
| `viewmodel/ServiciosViewModel.kt`, `DispensacionViewModel.kt` | Modify | Delegate to use cases; remove negative writers |
| `data/DispensacionRepository.kt` | Modify | Stop cancel-via-delete; support Reverso insert |
| `domain/UploadSyncCoordinator.kt` | Modify | Validate, quarantine, binary-split |
| `domain/SyncFinanzasUseCase.kt` | Modify | Truthful partial/error |
| `domain/DownloadSyncCoordinator.kt` | Modify | Narrow quarantine skip |
| `ui/.../ConfigSyncDiagnosticsCard.kt` | Modify | Background errors + copy-all |
| Tests under `src/test/...` | Create/Modify | See TDD map |

## Testing Strategy (strict TDD)

| Layer | RED→GREEN targets |
|-------|-------------------|
| Unit | `PagoEffect` matrix all tipos; Cancel/Reclaim idempotency; sanitizer |
| Room | Migration 43→44 ABS; Reverso unique path via DAO |
| Sync | 79+1: 79 synced, 1 quarantine, remote not ok; parent gate; download skip only quarantine |
| SQL smoke | Anulado/Reclamada/Reverso/Reembolso accept; negative reject; effect matches Kotlin |
| Regression | Rewrite Reportes/CalcularMontoPagado tests off negative Anulación |

## Work Units (chained, ≤400 authored lines each)

| WU | Scope | Verify |
|----|-------|--------|
| **WU-1** | Specs + Supabase preflight+mig (`pago_effect`, CHECKs, RPCs/trigger, `reversa_pago_id`) | db lint/diff + SQL smoke + GGA |
| **WU-2** | `PagoEffect`, Room 44, Cancel/Reclaim use cases, VM/repo writers | unit + migration tests |
| **WU-3** | Reader/DAO/UI aggregate convergence | Reportes/Cierre/MontoPagado tests |
| **WU-4** | Upload quarantine, binary-split, partial Error, parent gate, download skip | UploadSyncCoordinator + SyncFinanzas tests |
| **WU-5** | Diagnostics UI + pacientes HTTP capture (evidence only) | sanitizer + manual CLK-LX3 checklist |

`Decision needed before apply: Yes` · `Chained PRs recommended: Yes` · `400-line budget risk: High` · strategy `auto-chain`.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout / Rollback

**Deploy:** DB → app → device verify (CLK-LX3: no 23514, remote ok, no pago resurrection, caja unchanged except intended Reverso/Reembolso).

**Rollback:** App revert by WU PR. DB: if `reversa_pago_id` unused, reverse mig; else keep expanded CHECKs + `pago_effect`, feature-flag writers. Room forward-only; compensating mig if needed. Never re-allow negative montos. Quarantine rollback independent of schema.

## Open Questions

None — ambiguities resolved above.
