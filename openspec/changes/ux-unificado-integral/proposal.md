# Proposal: UX Unificado Integral (Pacientes + Servicios + Cierre v2)

## Decision

Integrar **Cierre de Caja v2** dentro del change UX unificado (#1714), no como track separado. Todo el refactor UX/UI se construye **sobre el avance reciente** (ledger + decisiones validadas PR #51), sin revertir finanzas ni inventario.

## Preconditions (gates — no código UX hasta cumplir)

| Gate | Acción | Estado |
|------|--------|--------|
| G0 | Merge **#55** `fix/sync-financial-ledger-01-db` → main | Pendiente |
| G1 | Merge **#56** `fix/sync-financial-ledger-02-android-ledger` → main | Pendiente |
| G2 | **Cerrar/superseder PR #51** — no merge as-is | Pendiente |
| G3 | `./gradlew :optoapp:testDebugUnitTest` green en main post-ledger | Pendiente |

## Non-negotiable constraints (desde avance reciente)

1. **PagoEffect** — todos los totales de pagos (`IF`, servicios, cierre, reportes) usan `PagoEffect.signedAmount(tipo, monto)`.
2. **Regalos hub IF** — UI y persistencia solo en `InformacionFinanciera`; `DispensacionViewModel` no guarda regalos.
3. **montoPagado sync** — `InformacionFinancieraViewModel.save()` persiste `montoPagado` tras CRUD pagos.
4. **Inventario single-writer** — regalos/stock vía `DispensacionStockHelper` / writers ledger; sin lógica paralela.
5. **Cancel servicio** — respetar `CancelServicioExtraUseCase` + estado `Anulado`.
6. **Cierre v2** — UX probada PR #51 + VM ledger; spec unificada.

## Scope integrado

| Módulo | Entregable UX |
|--------|----------------|
| **Fundación** | `OptoFormShell`, `PatientContextCard`, `OptoDateField`, `FormErrorHandler`, `FinancieraPagosSection` (PagoEffect + montoPagado) |
| **Cierre v2** | Hero dinámico, cobros/ventas, búsqueda, cards, PagoEffect — *fase propia dentro de este change* |
| **Paciente** | Scroll 3 secciones, Cancel+Guardar abajo |
| **Evaluación** | Wizard 5 pasos + context card + CTAs unificados |
| **Dispensación** | Wizard 3 pasos; hub IF (pagos+regalos+entrega); sin regalos en wizard |
| **Servicios** | Wizard 2 pasos; `FinancieraPagosSection` compartida; Snackbar errores |
| **Pulido** | Empty states, chips, spacing design system |

## Out of scope

- Migraciones Supabase ledger (change `fix-sync-financial-ledger` — ya en PRs #55/#56).
- Merge directo PR #51.

## Branch / PR chain

```
main (+ ledger #55+#56)
  └── feat/ux-unificado-integral
        ├── PR-U1  Fundación + FinancieraPagosSection
        ├── PR-U2  Cierre caja v2          ← absorbs ux-cierre-caja-v2
        ├── PR-U3  Paciente + Evaluación
        ├── PR-U4  Dispensación + IF hub
        └── PR-U5  Servicios + pulido
```

Base branch única: `feat/ux-unificado-integral` (stacked PRs o commits por slice).

## Success criteria

- [ ] Coherencia visual: mismos shells, cards, CTAs, Snackbar en paciente/eval/disp/servicio/cierre.
- [ ] Cero regresiones ledger (PagoEffect, sync, inventario).
- [ ] Cierre UX ≥ PR #51 probado R8 CLK-LX3.
- [ ] Specs openspec actualizadas: `cierre-caja`, deltas paciente/servicio según fase.

## Supersedes

- Engram #1714 (plan original — integrado aquí con constraints).
- PR #51 `feat/ux-cierre-caja`.
- Change standalone `ux-cierre-caja-v2` → **sub-slice PR-U2** de este change.
