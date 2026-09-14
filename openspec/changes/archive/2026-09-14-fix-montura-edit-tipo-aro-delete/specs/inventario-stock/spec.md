# Delta for inventario-stock

## ADDED Requirements

### Requirement: Edit montura rim type and material controls

On non-accesorio montura edit, the system MUST show exclusive catalog FilterChips and MUST persist selection in `form.tipoAro`. Material MUST use `OptoDropdownMenuField`. Accesorio edit MUST hide rim-type and material.

#### Scenario: Exclusive chip updates tipoAro

- GIVEN non-accesorio montura edit open
- WHEN operator selects "Semi al aire"
- THEN `form.tipoAro` MUST be "Semi al aire"
- AND at most one rim-type chip MUST be selected

#### Scenario: Material uses OptoDropdownMenuField

- GIVEN non-accesorio inventory montura form
- WHEN material options open
- THEN they MUST use `OptoDropdownMenuField`
- AND Aluminio MUST remain available

#### Scenario: Accesorio hides rim and material

- GIVEN accesorio edit
- WHEN form is shown
- THEN rim-type chips and material MUST NOT appear

### Requirement: Soft-delete montura from inventory

Inventory delete MUST set `activo=false` via update and MUST NOT hard-DELETE. Delete MUST catch failures and surface errors. Delete UI MUST require `canDeleteRecords` (admin|gerente). Primary catalog MUST hide `activo=false` rows.

#### Scenario: Soft-delete sets activo false

- GIVEN active montura M with FK children
- WHEN authorized operator confirms delete
- THEN M.activo MUST be false and M MUST remain
- AND MUST NOT crash on child FKs

#### Scenario: Delete error surfaced

- GIVEN delete throws (auth or persistence)
- WHEN delete is attempted
- THEN failure MUST be caught and shown

#### Scenario: Delete UI gated to canDeleteRecords

- GIVEN operator without canDeleteRecords
- WHEN montura list or detail is shown
- THEN Delete MUST NOT be available

#### Scenario: Inactive hidden from catalog

- GIVEN montura with `activo=false`
- WHEN primary inventory list loads
- THEN that row MUST NOT appear

### Requirement: Edit spawn sibling rim-type variant

From non-accesorio edit, the system MUST allow adding a sibling row sharing sku/marca/modelo/costo/precio/stockMinimo/material with new `tipoAro` and non-negative initial stock. UNIQUE `(sku, tipoAro)` per óptica MUST reject duplicates with a clear error. Amends ADR-2: create multi-insert unchanged; edit gains sibling spawn.

#### Scenario: Sibling insert from edit

- GIVEN editing SKU `RAY-2140` tipoAro "Aro Completo"
- WHEN operator adds "Al aire" with stock 4 and confirms
- THEN a new row MUST exist with shared attrs, tipoAro "Al aire", stock 4
- AND the original MUST remain

#### Scenario: Duplicate sibling tipo rejected

- GIVEN SKU already has tipoAro "Semi al aire"
- WHEN edit spawn reuses that tipoAro
- THEN insert MUST fail with a clear uniqueness error
- AND no duplicate MUST persist

### Requirement: Montura child FKs cascade on hard delete

Room and Postgres FKs from `orden_compra_items` and `inventario_fisico_detalle` to `monturas` MUST use `ON DELETE CASCADE`. UI delete MUST remain soft-delete and MUST NOT rely on CASCADE.

#### Scenario: Schema CASCADE defined

- GIVEN migrated Room schema and Postgres OC/IF→monturas FKs
- WHEN hard DELETE removes a montura
- THEN dependent OC and IF detalle rows MUST cascade-delete

#### Scenario: UI delete does not hard-delete

- GIVEN CASCADE FKs exist
- WHEN operator deletes in inventory UI
- THEN montura MUST soft-delete (`activo=false`)
- AND CASCADE MUST NOT run

## MODIFIED Requirements

### Requirement: Multi rim-type create with per-type initial stock

On montura create, the system MUST allow one or more catalog rim types with non-negative stock each, and MUST insert one row per type sharing sku/marca/modelo/costo/precio/stockMinimo/material and differing in `id`, `tipoAro`, `stockActual`. Edit-time rim types: Edit spawn sibling rim-type variant.
(Previously: Create-only “(not edit)”; ADR-2 amended for edit sibling spawn.)

#### Scenario: Create Completo and Semi with stocks

- GIVEN create form with SKU/marca/modelo/material filled
- AND tipos "Aro Completo" (5) and "Semi al aire" (3) selected
- WHEN user saves
- THEN two monturas MUST exist with those stocks

#### Scenario: Create without tipo rejected

- GIVEN non-accesorio create with no tipo selected
- WHEN user saves
- THEN save MUST fail with tipo de aro required
- AND no insert MUST occur
