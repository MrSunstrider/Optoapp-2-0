# Tasks: ux-cierre-caja-v2

## Preconditions

- [ ] P0 Ledger PRs #55 + #56 merged to `main`
- [ ] Close or supersede PR #51 (do not merge as-is)

## T1 — Re-port ViewModel (PagoEffect base)

- [ ] T1.1 Port `PagoDisplayItem`, `buildPagosDisplay`, `pagosDisplay` from PR #51
- [ ] T1.2 Replace `pagosExcludingAnulacion` with `PagoEffect.signedAmount` in hero, ventasHoy, cobrosAtrasados, getTotalesPorMetodo
- [ ] T1.3 Add `getCobradoHoy()` = sum PagoEffect over day pagos
- [ ] T1.4 Entity saldoPendiente + exclude Anulado/Reclamada per ledger
- [ ] T1.5 `pacienteNombres` flow for search
- [ ] T1.6 Tests: port PR #51 scenarios rewritten for PagoEffect matrix

## T2 — Re-port UI

- [ ] T2.1 Port `CierreCajaScreen` layout (hero, method cards, sections, empty states)
- [ ] T2.2 Port `CierreCajaVentaDisplay.kt` + search filters
- [ ] T2.3 Port enriched `TransactionItem`
- [ ] T2.4 Chips Ayer / Hoy / Buscar (date picker stays top-bar)
- [ ] T2.5 Tests: `CierreCajaVentaDisplayTest`

## T3 — Spec sync

- [ ] T3.1 Merge delta into `openspec/specs/cierre-caja/spec.md` (replace legacy saldoPendiente formula)
- [ ] T3.2 Include ledger PagoEffect scenarios alongside UX scenarios
- [ ] T3.3 Archive this change after verify

## T4 — Verify

- [ ] T4.1 `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [ ] T4.2 `./gradlew :optoapp:installRelease` smoke CLK-LX3
- [ ] T4.3 GGA (when provider available)

## Notes

- **Do not** touch InformacionFinanciera/regalos in this change — separate PR after ledger.
- PR #51 commit `2a6b352` is reference implementation for UI; VM logic must follow ledger `CierreCajaViewModel` PagoEffect patterns on current branch.
