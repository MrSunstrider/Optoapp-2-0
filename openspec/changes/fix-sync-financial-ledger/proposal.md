# Proposal: Fix Sync Financial Ledger

## Intent

Stop finanzas CHECK 23514 failures and ledger drift. Local Anulado/Reclamada and negative Anulación collide with remote CHECKs; batches abort; deletes resurrect on download; partial errors can look successful. Typed non-negative ledger + truthful quarantine. Keep PRD LWW; no broad conflict guards.

## Scope

### In Scope
- PagoEffect: Abono/Pago completo +monto; Reembolso/Reverso −monto; Anulación 0 legacy audit
- monto≥0; no new Anulación; idempotent linked Reverso; Reembolso positive magnitude
- CHECKs: servicios_extra +Anulado; dispensaciones +Anulado/Reclamada; keep pagos_monto_chk
- reversa_pago_id uniqueness; Room abs(monto) for legacy negative Anulación (keep type)
- Quarantine invalid rows (never skip+success); poison-row isolation
- Pacientes HTTP evidence only; strict TDD; GGA before push/migrate

### Out of Scope
- Negative montos; reinterpret historical cash; restore broad filterConflicts
- fix-sync-pacientes-http until exact status/body; web beyond shared SQL

## Capabilities

### New Capabilities
- `pago-effect-ledger`: effect-by-tipo, reverse/refund, legacy Anulación, reversa_pago_id

### Modified Capabilities
- `pagos-constraints`, `servicio-extra`, `sync`, `sync-conflict`, `sync-state-tracking`, `cierre-caja`, `reportes-financieros`, `analisis-negocio` — ledger effect, Anulado sync, LWW+quarantine, truthful markError, aggregate via PagoEffect

## Approach

DB-first (preflight→CHECKs→SQL effect→triggers/RPCs→reversa FK) → Android writers/migration → readers → upload quarantine → diagnostics. Deploy DB before client. Verify CLK-LX3.

## Causal Invariants

1. Sign only via PagoEffect(tipo); magnitude ≥0.
2. Cancel keeps originals; ≤1 full Reverso each.
3. Invalid rows never synced/remote-ok.
4. Finanzas LWW (PRD).
5. Pacientes is a separate causal chain.

## Affected Areas

supabase migrations/triggers/RPCs; Dispensacion/Servicios writers; PagoDao + cierre/reportes/BI; Upload/Download + SyncFinanzasUseCase; Room migration + DeletionSyncHelper.

## Chained PR Forecast

PR-1 specs+DB (Med–High); PR-2 writers+Room (High); PR-3 readers (Med); PR-4 quarantine (Med–High); PR-5 diagnostics+pacientes evidence (Low–Med). Split any slice >400 authored lines.

`Decision needed before apply: Yes`  
`Chained PRs recommended: Yes`  
`400-line budget risk: High`  
Strategy assumption: `auto-chain`.

## Risks

Mixed clients (H→DB first); reader drift (M→shared tests); quarantine≠conflict (M→spec LWW); unknown legacy counts (M→SQL preflight); pacientes scope creep (L→evidence-only).

## Rollback Plan

DB reverse only if unused; else keep expanded CHECKs and flag writers. App revert by PR; Room forward-only + compensating mig if needed. Quarantine independent. Never allow negative montos.

## Dependencies

Supabase preflight + GGA; stale sync-conflict SDD non-authoritative; CLK-LX3 re-verify.

## RDD Status

**Disabled/unmanaged** — principles only; no receipt authority/kill switch.

## Success Criteria

- [ ] No 23514 on Anulado/Reclamada/Reverso/Reembolso
- [ ] Kotlin↔SQL PagoEffect matrices match
- [ ] One Reverso/cancel; no resurrection
- [ ] Legacy Anulación → abs(monto); cash unchanged
- [ ] 79+1 uploads 79; error remains; remote not ok
- [ ] Tests + lint/diff + GGA green
- [ ] CLK-LX3 remote ok; servicios poison gone
- [ ] Pacientes HTTP captured; fix deferred

## Assumptions

Architecture locked by plan. Open: pacientes HTTP class; Anulación inventory before tipo CHECK tighten.
