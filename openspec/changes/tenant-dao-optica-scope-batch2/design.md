# Design — tenant DAO optica scope batch 2

## Pattern

Mirror `CostoLcDao.lookup(opticaId, …)` and Montura PK scope:

```sql
… AND optica_id = :opticaId   -- snake_case Room columns
… AND opticaId = :opticaId    -- camelCase Room columns
```

Breaking: add `opticaId` as first (or last, matching existing) param; **delete** unscoped overloads used in production.

## PR split

| PR | Branch | Issue | Focus |
|----|--------|-------|-------|
| B1 | `fix/tenant-scope-costo-lookup` | #91 | CostoProducto + CostoBiselado lookup + Disp VM |
| B2 | `fix/tenant-scope-pago-helpers` | #92 | PagoDao sums/credits/parent lists; CalcularMonto/CancelLedger |
| B3 | `fix/tenant-scope-parent-fk-reads` | #93 | DispItem FK reads; Movimiento-by-montura; Paciente reassign |

## Parent → child

Items/movimientos: prefer `AND optica_id = :opticaId` on denormalized column (already on entities). Paciente reassign: add `AND opticaId = :opticaId` to UPDATE WHERE.

## Sync

No bump signature change expected for costos (lookup-only). Pago helpers used by cancel ledger — pass optica from cancel use case.
