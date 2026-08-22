# Evidence — PR3 Proveedor / OC / Movimiento

## Change

- Scoped DAO PK lookups: `ProveedorDao.getById`, `OrdenCompraDao.getById`, `MonturaMovimientoDao.getMovimientoById` — `WHERE id = :id AND opticaId = :opticaId`
- Plumb: ProveedorRepository, OrdenCompraRepository, MonturaInventoryCoordinator, OptoRepository, BumpEntityStrategy, ProveedoresViewModel, OrdenesCompraViewModel
- `receiveItems` parent-gates with scoped `getById` **before** item writes
- `ProveedoresViewModel.delete` uses session `opticaId` (not entity)
- Foreign-optica Room/repo tests

## Methods touched

| Layer | Method |
|-------|--------|
| ProveedorDao | `getById(id, opticaId)` |
| OrdenCompraDao | `getById(id, opticaId)` |
| MonturaMovimientoDao | `getMovimientoById(id, opticaId)` |
| ProveedorRepository | `getById`, `softDelete(id, opticaId)`, upsert path |
| OrdenCompraRepository | `getById`, `updateEstado`, `receiveItems` (parent gate first), `delete` |
| MonturaInventoryCoordinator / OptoRepository | `getMovimientoMonturaById(id, opticaId)` |
| BumpEntityStrategy | proveedor / orden_compra / montura_movimiento / orden_compra_item |
| ProveedoresViewModel | `delete` → session opticaId |

## Commands

```
./gradlew :optoapp:testDebugUnitTest
```

Result: PASS (EXIT:0)

## GGA-eq (live generalPurpose R1–R4)

| Round | Verdict |
|-------|---------|
| R1 Risk | APPROVED (after receiveItems + delete fixes) |
| R2 Readability | APPROVED |
| R3 Reliability | APPROVED |
| R4 Resilience | APPROVED |

## Notes

- No remote migrations
- Issue #86
- Base: PR2 `fix/tenant-scope-dispensacion-servicio`
- Account-switch residual: foreign `opticaId` → null at DAO; no item mutation without parent gate
