# Spec: config-ux-cleanup

## Requirement: Honest FREE subscription card

Config subscription UI MUST present FREE as “1 sucursal” (or equivalent), not patient caps.

### Scenario: FREE label

- GIVEN planCode FREE on ConfiguracionScreen
- WHEN SubscriptionCard renders
- THEN the plan label mentions 1 sucursal / 1 óptica and does not mention 50 pacientes

## Requirement: Fail-closed Config access

Only admin|gerente MAY view ConfiguracionScreen content.

### Scenario: NavHost gate

- GIVEN user role that fails `AppRoles.canManageUsers`
- WHEN navigating to Route.Configuracion
- THEN the screen does not expose management sections (redirect or access-denied)

## Requirement: Dead About removed

`ConfigAboutSection` MUST NOT exist in main sources.

## Requirement: Lab legacy + Avanzado

Laboratory section MUST show a legacy/migration hint. System (TZ/reminders) and Sync diagnostics MUST be under a collapsed “Avanzado” group by default.
