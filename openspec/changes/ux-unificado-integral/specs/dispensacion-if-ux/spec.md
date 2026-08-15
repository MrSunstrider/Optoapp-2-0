# Delta: dispensacion-if-ux (PR-U4)

## MODIFIED Requirements

### Requirement: Dispensación wizard has three steps

`NuevaDispensacionScreen` MUST present a 3-step wizard via `StepIndicator`:

1. **Orden** — fecha, OT, evaluación vinculada, paciente context
2. **Productos** — lentes / montura
3. **Confirmar** — monto total, estado entrega, resumen; save CTA

Regalos MUST NOT appear in any wizard step.

#### Scenario: No regalos in wizard

- GIVEN create or edit dispensación
- WHEN rendering any wizard step
- THEN `RegalosSection` is not composed

### Requirement: IF hub owns pagos and regalos

`InformacionFinancieraScreen` MUST include:

- `PatientContextCard`
- `FinancieraPagosSection` (already)
- `RegalosSection` (moved from wizard)
- entrega estado/fecha

### Requirement: IF save syncs montoPagado

`InformacionFinancieraViewModel.save()` MUST persist `dispensacion.montoPagado` inside the same transaction as pago CRUD, using effect-aware calculation (`CalcularMontoPagadoUseCase` or equivalent DAO sum).

`DispensacionFinancieraRepository` MUST expose `actualizarMontoPagado(dispensacionId, montoPagado, opticaId)`.

#### Scenario: Abono then reembolso updates entity field

- GIVEN dispensación total 300 with Abono 200 and Reembolso 50 saved via IF
- WHEN save completes
- THEN `montoPagado` on the dispensación entity is 150

### Requirement: DispensacionViewModel stops persisting regalos

`DispensacionViewModel.saveDispensacion` MUST NOT insert/update/delete regalos. Stock for regalos MUST go through IF / `RegaloDispensacionViewModel` / ledger writers only.
