# Spec: security-definer-grants

## Requirement: Grant matrix for SECURITY DEFINER RPCs

The database MUST enforce the EXECUTE matrix in `design.md` so Android and optoweb keep working while unused admin bypass stays locked.

### Scenario: Pre-auth rate limit stays callable by anon

- **GIVEN** role `anon`
- **WHEN** EXECUTE privilege is checked on `public.check_rate_limit(text, integer, integer)`
- **THEN** the privilege MUST be true

### Scenario: Paciente delete quota not callable by anon

- **GIVEN** role `anon`
- **WHEN** EXECUTE privilege is checked on `public.paciente_eliminaciones_restantes_hoy(text)`
- **THEN** the privilege MUST be false
- **AND** role `authenticated` MUST retain EXECUTE

### Scenario: Admin resumen recalc locked to service_role

- **GIVEN** roles `anon` and `authenticated`
- **WHEN** EXECUTE is checked on `public.recalcular_resumen_diario_admin(text, date)`
- **THEN** both MUST lack EXECUTE
- **AND** `service_role` MUST have EXECUTE

### Scenario: Sync and onboarding RPCs remain for authenticated

- **GIVEN** role `authenticated`
- **WHEN** EXECUTE is checked on `recalcular_resumen_diario`, `create_optica_for_current_user`, and `rpc_adjust_montura_stock`
- **THEN** all three MUST allow EXECUTE
- **AND** `anon` MUST lack EXECUTE on those three
