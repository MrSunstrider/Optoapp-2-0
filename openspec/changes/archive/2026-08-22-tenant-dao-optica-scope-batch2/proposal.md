# Proposal — tenant DAO optica scope batch 2

## Intent

Cerrar helpers Room que aún omiten `opticaId`/`optica_id` tras batch 1 (PK getById): lookups de costos, sums/credits de pagos, lecturas por FK padre, y reassign de pacientes.

## Threat

Room residual tras cambio de cuenta → costo de otra óptica en OT, balances/cancel ledger incorrectos, items/movimientos ajenos.

## Scope

- **IN:** CostoProducto/CostoBiselado lookup*; PagoDao parent/sum/credit/reverso (+ kill legacy date-range/all/reassign sin tenant); DispensacionItem parent reads; MonturaMovimiento.getMovimientosByMontura; PacienteDao reassign* con tenant.
- **OUT:** CategoriaProducto; OrdenCompraItem/InventarioFisicoDetalle/MonturaProveedor (sin columna tenant); mig remotas; RLS.

## Invariants

- INV-1: Toda query sobre tabla con tenant exige predicado de óptica (salvo legacy migration one-shots documentados).
- INV-2: Callers pasan `sessionManager.opticaId` / entity.opticaId coherente.
- INV-3: Single-writer venta / `(referenciaId,tipo,monturaId)` no cambian.
