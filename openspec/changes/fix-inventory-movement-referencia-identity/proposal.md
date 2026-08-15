# Proposal: Fix inventory movement `referenciaId` identity

## Intent

Every `montura_movimientos` row must carry a `referenciaId` that uniquely identifies
the business fact within `(tipo, monturaId)`. Empty or parent-scoped IDs make the
unique index `idx_movimientos_conflict` reject a legitimate second event.

## Evidence

Production (`optica_id=25af5a92-…`):

| tipo | empty `referencia_id` | distinct monturas |
|------|----------------------|-------------------|
| ENTRADA | 10 | 10 |
| SALIDA | 2 | 2 |

No duplicate triples yet — each montura has at most one empty-ref movement of that
tipo. A second manual entrada/salida on those monturas fails with 23505 (local Room
unique index / remote `idx_movimientos_conflict`).

## Defects in the same class

| Writer | Current `referenciaId` | Failure mode |
|--------|------------------------|--------------|
| `MonturasViewModel.registrarSalida/Entrada` | `""` | Second manual move of same tipo on same montura collides |
| `RegaloDispensacionViewModel` | `dispensacionId` | Second regalo of same product, or a sale of that montura on the same dispensación, collides on `SALIDA_VENTA` / `AJUSTE` |
| `OrdenCompraRepository.receiveItems` | `ocId` | Two OC lines for the same montura, or a replayed completion, collide on `ENTRADA` |
| `InventarioFisicoRepository.closeSession` | `sessionId` | Re-close after partial write collides; no estado guard |

## Approach

1. **Identity rule**: `referenciaId` names the *fact*, not a blank or a shared parent
   when multiple facts share that parent.
   - Manual move → movement's own `id`
   - Regalo → `regalo.id`
   - OC receipt → `item.id` (line item)
   - Inventario físico → `detalle.id`, and refuse to close an already `COMPLETADO` session
2. **Backfill**: Room 45→46 and a remote migration set `referencia_id = id` where
   blank/null. Safe because `id` is the primary key (unique), so the conflict index
   stays satisfied.

## Out of scope

Sale movements that correctly use `dispensacionId` for one montura per dispensación
stay as they are (the inventory single-writer change depends on that identity for
the `venta`/`SALIDA_VENTA` alias).
