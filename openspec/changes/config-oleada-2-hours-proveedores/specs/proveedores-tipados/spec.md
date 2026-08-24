# Spec: proveedores-tipados (NEW)

## Requirement

Proveedores have a `tipo` classifying supply role: monturas, laboratorio, or tecnico.

## Scenarios

### Default tipo

- GIVEN a new proveedor without explicit tipo
- THEN `tipo` is `monturas`

### Form selection

- GIVEN user edits a proveedor
- WHEN they pick laboratorio or tecnico
- THEN save persists that tipo locally (and sync round-trips via DTO)

### Migration backfill

- GIVEN Room DB at version 46
- WHEN migrating to 47
- THEN `proveedores.tipo` exists NOT NULL DEFAULT `monturas`
