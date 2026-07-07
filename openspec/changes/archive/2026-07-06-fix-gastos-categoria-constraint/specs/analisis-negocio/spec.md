# Delta for analisis-negocio

## ADDED Requirements

### Requirement: R30 — Android Category Values Match DB CHECK Constraint

The `GastosViewModel.categorias` list MUST contain exactly the 8 values permitted by the PostgreSQL CHECK constraint on `gastos_operativos.categoria`: `alquiler`, `servicios`, `personal`, `proveedores`, `insumos`, `marketing`, `impuestos`, `otro`. No other value SHALL be present in the list. The default `categoria` in `GastosUiState` MUST be `"alquiler"`.

#### Scenario: ViewModel category list is DB-compliant

```
GIVEN the GastosViewModel.categorias list is defined
 WHEN the ViewModel is first initialized
 THEN the list contains exactly ["alquiler", "servicios", "personal", "proveedores", "insumos", "marketing", "impuestos", "otro"]
  AND every value in the list is a member of the DB CHECK constraint set
```

#### Scenario: Default categoria is a valid DB value

```
GIVEN a new GastosUiState instance is created with default values
 WHEN the default categoria field is read
 THEN it equals "alquiler"
  AND it is a member of the DB CHECK constraint set
```

### Requirement: R31 — Sync Upload Succeeds With Valid Category

The `UploadSyncCoordinator.uploadGastosOperativos()` flow MUST succeed when a `GastoOperativoEntity` has a `categoria` value that matches the DB CHECK constraint. The failure mode for entities with non-compliant categories MUST NOT change — they SHALL continue to fail with a CHECK constraint violation, same as before this change.

#### Scenario: Valid category uploads successfully

```
GIVEN a GastoOperativoEntity with categoria = "alquiler" is pending upload
 WHEN UploadSyncCoordinator.uploadGastosOperativos() processes it
 THEN the Supabase INSERT completes without CHECK constraint error
  AND the entity is marked as synced
```

#### Scenario: Old invalid category still fails (no regression)

```
GIVEN a GastoOperativoEntity saved with the old categoria value "Local" is pending upload
 WHEN UploadSyncCoordinator.uploadGastosOperativos() processes it
 THEN the Supabase INSERT fails with a CHECK constraint violation
  AND the failure mode is identical to the current (pre-fix) behavior
```
