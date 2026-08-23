# Tasks: Finanzas Oleada D — config financiera + P&L + resumen_diario

**Issue**: Closes #108 · **Branch**: `feat/finanzas-oleada-d-bi` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Delivery**: `auto-chain` · **Gates**: GGA-eq R1–R4 per PR; `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
**Untouched**: `PagoEffect`; cierre export; Oleada B cost tabs; resumen upload; new schema unless verify fails

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~900–1300 / 4 WUs |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 WU-Spec-Reportes → PR2 WU-Config → PR3 WU-PnL → PR4 WU-Resumen |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test | Runtime | Rollback |
|------|------|-----------|--------------|---------|----------|
| WU1 Spec-Reportes | Specs+characterization PagoEffect | PR1→feat | `*ReportesViewModel*` | N/A JUnit | Revert spec/test-only |
| WU2 Config | UI+upsert+upload | PR2→PR1 | `*Configuracion*` `*Upload*` `*SyncFinanzas*` | N/A JUnit | Revert config+upload |
| WU3 PnL | Analisis P&L + offline compose | PR3→PR2 | `*Analisis*` `*ObtenerAnalisis*` | N/A JUnit | Revert P&L/offline |
| WU4 Resumen | In-app month list + refresh | PR4→PR3 | `*ResumenDiario*` | N/A JUnit | Revert resumen UI |

## Phase 0 — Preconditions

- [x] 0.1 Verify prod `configuracion_financiera` RLS INSERT/UPDATE for admin/gerente.
- [x] 0.2 Create `feat/finanzas-oleada-d-bi` from agreed base.

## Phase 1 — WU1 Spec-Reportes (≤400)

- [x] 1.1 RED/confirm: Reportes scenarios — Abono/Reverso/Anulación → totalCobrado via PagoEffect (not raw sum).
- [x] 1.2 GREEN: keep/adjust characterization tests only; **do not** edit `PagoEffect.kt`.
- [x] 1.3 Archive-ready delta already in change folder; focused verify + GGA; PR1.

## Phase 2 — WU2 Config (≤400)

- [x] 2.1 RED: `toRemoto` round-trip; upload after upsert; upload-before-download; empty→0; admin/gerente gate.
- [x] 2.2 GREEN: DTO mapper + `uploadConfiguracionFinanciera` + UseCase wire + counter.
- [x] 2.3 GREEN: Config VM/UI editors + Dao upsert + scheduleFinanzasSync.
- [x] 2.4 Focused verify + GGA; PR2 (UI+upload together).

## Phase 3 — WU3 PnL (≤400)

- [x] 3.1 RED: online P&L lines from AnalisisMensual; offline label + compose costo/gastos.
- [x] 3.2 GREEN: optional `costoMes`; offline fallback in `ObtenerAnalisisMensualUseCase`.
- [x] 3.3 GREEN: AnalisisNegocio P&L block UI.
- [x] 3.4 Focused verify + GGA; PR3.

## Phase 4 — WU4 Resumen (≤400)

- [ ] 4.1 RED: month list from Dao Flow; refresh schedules finanzas sync; no upload.
- [ ] 4.2 GREEN: Resumen screen/subsection + nav entry.
- [ ] 4.3 Full `testDebugUnitTest`; INV checks; GGA; PR4 Closes #108.
