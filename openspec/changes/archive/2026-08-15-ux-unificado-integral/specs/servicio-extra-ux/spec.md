# Delta: servicio-extra-ux (PR-U5)

## MODIFIED Requirements

### Requirement: Servicio form uses two-step wizard

`NuevoServicioScreen` MUST present a 2-step wizard via `StepIndicator`:

1. **Datos** — fecha, OT, paciente (bloqueado si viene desde tab paciente), descripción, monto, inventario
2. **Pagos** — `FinancieraPagosSection` + estado entrega

#### Scenario: Paciente locked from patient tab

- GIVEN navigation via `NuevoServicioPaciente`
- WHEN step Datos renders
- THEN paciente selector is read-only / prefilled

### Requirement: Errors via Snackbar

Save/validation errors on servicio MUST surface through Snackbar (shell or existing host), not only inline Text.

### Requirement: ServiciosExtra list consistency

`ServiciosExtraScreen` cards/empty states MUST align with Opto design tokens used elsewhere (spacing, empty copy).
