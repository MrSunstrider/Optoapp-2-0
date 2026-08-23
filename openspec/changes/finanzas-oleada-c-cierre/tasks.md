# Tasks: Finanzas Oleada C — export cierre + Operación Hoy continuity

**Issue**: Closes #107 · **Branch**: `feat/finanzas-oleada-c-cierre` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Delivery**: `auto-chain` · **Gates**: GGA-eq R1–R4 per PR; `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
**Untouched**: `PagoEffect`; arqueo_caja; P&L/config/resumen; DrawerSections

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~700–1100 / 4 WUs |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 WU-Export → PR2 WU-UI-Export → PR3 WU-Date → PR4 WU-Cash |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test | Runtime | Rollback |
|------|------|-----------|--------------|---------|----------|
| WU1 Export | PDF/CSV pure builders | PR1→feat | `*CierreCajaPdf*` `*CierreCajaCsv*` | N/A JUnit | Revert generators |
| WU2 UI-Export | Menu + role gate + share | PR2→PR1 | `*CierreCaja*` role/export | N/A JUnit | Revert Screen/VM export |
| WU3 Date | Route fecha + Operación Hoy | PR3→PR2 | `*CierreCajaViewModel*` fecha | N/A JUnit | Revert Route/nav |
| WU4 Cash | Contado + diferencia prefs | PR4→PR3 | `*CierreCaja*` cash | N/A JUnit | Revert cash state/prefs |

## Phase 0 — Preconditions

- [ ] 0.1 Create `feat/finanzas-oleada-c-cierre` from agreed base (after #105 preferred).

## Phase 1 — WU1 Export (≤400)

- [ ] 1.1 RED: CSV/PDF from fixture `CierreCajaUiState` — hero/methods equal inputs (PagoEffect numbers), not raw sum.
- [ ] 1.2 GREEN: `CierreCajaCsvExporter` (UTF-8 BOM, invariant decimals).
- [ ] 1.3 GREEN: `CierreCajaPdfGenerator` day-close layout from state.
- [ ] 1.4 Focused verify + GGA; PR1.

## Phase 2 — WU2 UI-Export (≤400)

- [ ] 2.1 RED: export hidden when rol null / unauthorized; visible for admin/gerente/especialista.
- [ ] 2.2 GREEN: Cierre overflow/menu → generate + `FileShareUtils` (optional `shareCsv`).
- [ ] 2.3 GREEN: wire generators to current `uiState` only (no re-aggregate).
- [ ] 2.4 Focused verify + GGA; PR2.

## Phase 3 — WU3 Date (≤400)

- [ ] 3.1 RED: VM applies SavedStateHandle fecha; missing → today.
- [ ] 3.2 GREEN: `Route.CierreCaja` optional `fecha`; NavHost arg.
- [ ] 3.3 GREEN: `OperacionHoyScreen` navigate with `uiState.fecha`.
- [ ] 3.4 Focused verify + GGA; PR3.

## Phase 4 — WU4 Cash (≤400)

- [ ] 4.1 RED: diferencia = contado − Efectivo PagoEffect net; empty contado hides delta; no arqueo writes.
- [ ] 4.2 GREEN: VM/prefs `(opticaId,fecha)`; Screen field.
- [ ] 4.3 Full `testDebugUnitTest`; INV PagoEffect untouched; GGA; PR4 Closes #107.
