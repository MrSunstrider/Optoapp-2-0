# Proposal — tenant DAO opticaId scope

## Intent

Cerrar en Room todos los get/delete por PK que omiten `opticaId`/`optica_id` cuando la entidad lo tiene, para impedir lectura/escritura cross-tenant con filas residuales tras cambio de cuenta.

## Evidence

- GGA en inventario físico: `InventarioFisicoDao.getById` sin tenant.
- Audit: Disp, Servicio, Item, Regalo, Proveedor, OC, Movimiento, Pago legacy.
- Patrón canónico ya en Montura/Paciente/`getPagoByIdForOptica`.

## Scope

- **IN:** Scope PK lookups listados en tasks; plumb repos/VM/BumpEntityStrategy; TDD Room; 3 PRs.
- **OUT:** CategoriaProducto (global); mig remotas; RLS Postgres; batch2 (Pago sums, CostoProducto lookup).

## Invariants

- INV-1: Ningún DAO de entidad con tenant column expone get/delete por PK sin tenant.
- INV-2: Sync bump usa la misma firma scoped.
- INV-3: Single-writer venta / identidad `(referenciaId,tipo,monturaId)` no cambian.
