# Tasks: UX Unificado Integral

## Phase 0 — Gates (maintainer)

- [x] P0.1 Merge PR #55 to main
- [x] P0.2 Merge PR #56 to main
- [x] P0.3 Close PR #51 with comment → superseded by `ux-unificado-integral` PR-U2
- [x] P0.4 Verify full unit suite on main (green, rerun forzado 2m40s)
- [x] P0.5 `design.md` (faltaba en el SDD: contratos U1 + decisiones D1-D5)

## Phase 1 — Fundación (PR-U1)

- [x] P1.1 `OptoFormShell` (top bar + scroll + insets + bottomBar/snackbar opcionales)
- [x] P1.2 `PatientContextCard` (extraído del card OT/paciente de IF)
- [x] P1.4 **`FinancieraPagosSection`** + `PagosSectionState` — extraído de IF + ServicioForm
  - [x] `PagoEffect.signedAmount` para saldo/pagado y topes de abono
  - [x] Sin lógica de regalos (siguen IF-only, Fase 4)
- [x] P1.5 Unit tests `PagosSectionStateTest` (9 casos, JUnit puro)
- [x] P1.6 RDD: validación de abonos effect-aware en `ServiciosViewModel` + `DispensacionViewModel`
  - Hallazgo Bugbot: la UI mostraba saldo con `PagoEffect` mientras el save rechazaba con suma cruda
- [ ] P1.3 `OptoDateField`, `FormErrorHandler` → diferidos a PR-U3 (sin call site hasta migrar formularios)

### Fuera de PR-U1 (movido a PR-U4 por D5)

- `actualizarMontoPagado` en `IF.save()` — requiere que IF sea el hub de pagos antes de quitarle la escritura al wizard

## Phase 2 — Cierre caja v2 (PR-U2) ← was ux-cierre-caja-v2

- [x] P2.1 Port `PagoDisplayItem`, `buildPagosDisplay`, `pagosDisplay` (from PR #51 ref)
- [x] P2.2 VM: hero, ventasHoy, cobrosAtrasados, getTotalesPorMetodo, getCobradoHoy — all **PagoEffect**
- [x] P2.3 UI: hero dinámico, method cards, cobros/ventas sections, empty states
- [x] P2.4 `CierreCajaVentaDisplay.kt` + search (Ayer/Hoy/Buscar)
- [x] P2.5 Enriched `TransactionItem` + navigation
- [ ] P2.6 Merge spec `cierre-caja` (UX + ledger scenarios)
- [ ] P2.7 Tests + installRelease R8 smoke CLK-LX3

## Phase 3 — Paciente + Evaluación (PR-U3)

- [ ] P3.1 Paciente: scroll 3 secciones + OptoFormShell
- [ ] P3.2 Evaluación: context card + CTAs unificados (wizard 5 pasos intacto)

## Phase 4 — Dispensación + IF hub (PR-U4)

- [ ] P4.1 Wizard dispensación 3 pasos (Orden/Productos/Confirmar)
- [ ] P4.2 IF hub: pagos (`FinancieraPagosSection`) + **regalos** + entrega
- [ ] P4.3 Remove regalos from `NuevaDispensacionScreen` / `DispensacionViewModel.save`
- [ ] P4.4 Stock regalos via ledger writers only

## Phase 5 — Servicios + pulido (PR-U5)

- [ ] P5.1 Servicio wizard 2 pasos; paciente bloqueado si from tab
- [ ] P5.2 `FinancieraPagosSection` in ServicioForm; Snackbar errors
- [ ] P5.3 ServiciosExtraScreen list/card consistency
- [ ] P5.4 Cross-screen polish (spacing, empty states)

## Phase 6 — Verify + archive

- [ ] P6.1 Full test suite + GGA when provider available
- [ ] P6.2 Archive change + merge specs to `openspec/specs/`

## Reference commits

| Source | Use for |
|--------|---------|
| PR #51 `2a6b352` | Cierre UI layout, search, cards (re-port only) |
| Branch ledger | VM PagoEffect patterns, CancelServicio, estados |
| Engram #1714 | Original UX patterns per entity |

## Budget note

Chain 5 PRs; each slice target ≤400 lines changed. Stop and split if exceeded.
