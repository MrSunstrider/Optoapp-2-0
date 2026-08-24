# Spec: configuracion-free

## Requirement: FREE plan allows one óptica only

The FREE plan MUST allow at most one óptica (sucursal) per user. Paciente and dispensación counts MUST NOT be capped by FREE plan product rules.

### Scenario: maxOpticas FREE is 1

- GIVEN plan code FREE
- WHEN `maxOpticas` is queried
- THEN it returns 1

### Scenario: canAddPaciente always true

- GIVEN any tier and any paciente count
- WHEN `canAddPaciente` is evaluated
- THEN it returns true

### Scenario: createAdditionalOptica blocked when user has one membership

- GIVEN the current user already has ≥1 óptica membership
- WHEN `createAdditionalOptica` is invoked
- THEN it fails with a message referring to the 1-óptica free plan limit

### Scenario: DB trigger blocks second óptica

- GIVEN authenticated user with ≥1 row in `usuario_optica`
- WHEN inserting another `opticas` row
- THEN `trg_opticas_limit_guard` raises an exception mentioning 1 óptica / plan gratuito
