# Proposal: UX Cierre de Caja — Cobros vs Ventas Clarity

## Intent

Redesign Cierre de Caja information architecture so optometrists can distinguish money collected today (cobros) from orders registered today (ventas), list every payment by `pago.fecha`, and see enriched venta cards with OT, product detail, and delivery estado chips.

## Scope

| In Scope | Out of Scope |
|----------|-------------|
| List `uiState.pagos` in Cobros recibidos with `TransactionItem` | Export PDF / share cierre |
| Hero COBRADO HOY vs Ventas registradas sub-metrics | Filter cobros by payment method |
| `PagoDisplayItem` metadata (OT, cobro atrasado, navigation) | Edit abonos from cierre screen |
| Venta cards: OT, lente/descripción, estado chip | Multi-moneda |
| Section empty states + HorizontalDivider | AppRoles changes |
| Delta spec correcting saldoPendiente formula | — |

## PR Slices

| Slice | Phases | Deliverable |
|-------|--------|-------------|
| PR-1 | A + B + C | Lista pagos, hero métricas, PagoDisplayItem |
| PR-2 | D + E | Cards ventas, spec delta, display helper tests |

## Rollback

Revert commits. No schema changes. Screen/VM/spec only.

## Success Criteria

- [ ] Every pago with `pago.fecha = D` appears under Cobros recibidos
- [ ] Hero shows COBRADO HOY distinct from Ventas registradas (totalGeneral)
- [ ] Venta cards show OT, subtitle, Pendiente/Entregado chip
- [ ] Empty states per section; divider between cobros and ventas
- [ ] Spec documents entity-based saldoPendiente
- [ ] `./gradlew :optoapp:testDebugUnitTest` passes
