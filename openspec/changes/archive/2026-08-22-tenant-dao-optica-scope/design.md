# Design — tenant DAO opticaId scope

## Approach

Mirror `MonturaDao.getMonturaByIdForOptica`:

```sql
SELECT * FROM <table> WHERE id = :id AND opticaId = :opticaId
```

Use `optica_id` when the Room column is snake_case.

- **Breaking API:** replace unscoped methods; do not keep legacy overloads.
- **Children without tenant column:** load parent with scope first; then FK child queries.
- **Children with denormalized `optica_id`:** scope get/delete too (items, regalos).
- **Sync:** `BumpEntityStrategy` already has `opticaId` — pass it into scoped getters.

## Delivery

| PR | Branch | Domain |
|----|--------|--------|
| 1 | `fix/tenant-scope-inventario-fisico` | IF |
| 2 | `fix/tenant-scope-dispensacion-servicio` | Disp/Servicio/Item/Regalo/Pago |
| 3 | `fix/tenant-scope-proveedor-oc-movimiento` | Proveedor/OC/Movimiento |

Base: `origin/main` (independent of inventario #82/#83). Chain PR2→PR1, PR3→PR2.
