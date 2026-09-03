# Spec delta — servicio-extra venta accesorios

## Requirements

### R1: Picker inventario

Servicios extra MUST list active inventory items (armazón and ACCESORIO) with stock > 0 in the product search picker.

#### Scenario: accesorio activo con stock aparece

- GIVEN a montura row with `categoria=ACCESORIO`, `activo=true`, `stockActual=5`
- WHEN the user opens nuevo servicio extra picker
- THEN the item MUST appear in search results

### R2: monturaId persistido

`ServicioExtra` MUST expose optional `monturaId`. Sync DTO MUST map `montura_id`.

#### Scenario: servicio vinculado guarda monturaId

- GIVEN user selects inventory item `m-liquido`
- WHEN save succeeds
- THEN `servicios_extra.monturaId = 'm-liquido'`

### R3: Stock en venta

WHEN save links `monturaId` and it is new or changed
THEN the system MUST register `SALIDA_VENTA` qty 1 with `referenciaId = servicio.id`.

WHEN cancel or edit removes/changes linked product
THEN the system MUST register `AJUSTE` restock with distinct `:rev:` referencia.

### R4: Dispensación sin cambio

Dispensación picker MUST continue excluding ACCESORIO (`isArmazon` only).
