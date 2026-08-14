# Tasks: UX Cierre de Caja

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450 (both PRs) |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Chain strategy | feature-branch-chain |
| Delivery strategy | auto-chain |

Decision needed before apply: No (PR-2 assigned)
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

## PR-1 — Fases A + B + C

- [x] A.1 Render `pagosDisplay` with `Column.forEach` + `TransactionItem`
- [x] B.1 Hero COBRADO HOY + sub-metrics ventasHoy/cobrosAtrasados/saldoPendiente
- [x] B.2 Date chips Hoy/Ayer + formatted date button
- [x] C.1 `PagoDisplayItem` in ViewModel + buildPagosDisplay
- [x] C.2 TransactionItem enriched (label, tipo, cobro atrasado, tap)
- [x] C.3 Unit tests pagosDisplay + getCobradoHoy

## PR-2 — Fases D + E

- [x] D.1 Section titles Cobros recibidos (N) / Ventas registradas (N) with counts
- [x] D.2 VentaDispensacionCard: OT, lente subtitle, estadoEntrega chip
- [x] D.3 VentaServicioCard: OT, descripcion, estado chip
- [x] D.4 Empty states per section + HorizontalDivider between sections
- [x] E.1 openspec/changes/ux-cierre-caja/ proposal + delta spec + tasks
- [x] E.2 Update openspec/specs/cierre-caja/spec.md with corrected requirements
- [x] E.3 CierreCajaVentaDisplay helpers + unit tests
- [x] E.4 Full unit test suite green
