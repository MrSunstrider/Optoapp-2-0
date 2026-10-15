# Frame weighted average cost

`monturas.costo` holds the weighted average purchase cost of a frame. It feeds the cost recorded on
every `SALIDA_VENTA` movement and, through it, the real margin reports.

## When the cost changes

Only a **full** purchase-order receipt (`OrdenCompraRepository.receiveItems`, order reaches
`COMPLETADA`) changes the cost. For each item with a received quantity above zero:

1. Read the frame's stock and cost before any adjustment.
2. Claim the `ENTRADA` movement through its unique key
   (`referenciaId` = purchase-order item id, `tipo = ENTRADA`, `monturaId`). If the claim already exists
   the item was received before and is skipped, so a retried receipt never counts twice.
3. When the item cost is above zero, store the new average.
4. Add the received quantity to the stock.

Everything above, plus the item quantities and the order state, commits in one Room transaction.
Any failure rolls the whole receipt back and the order can be received again.

| Situation | New cost |
|---|---|
| Item cost is zero or negative | Unchanged (stock still moves, `ENTRADA` still recorded) |
| No previous stock (zero or negative) | Item cost |
| Previous cost missing or zero | Item cost (zero means "never costed", not "free") |
| Otherwise | `(stock × cost + qty × item cost) / (stock + qty)`, rounded half up to 2 decimals |

A **partial** receipt (`PARCIAL`) records quantities only: stock and cost stay unchanged until the
order is complete, and the average then uses each item's cumulative received quantity.

Manual stock adjustments (`AJUSTE`, including restocks after a cancellation) never change the cost
and record a movement cost of zero.

## Movement costs

- `ENTRADA`: `costoUnitario` is the purchase-order item cost; `stockPrevio`/`stockNuevo` are the real
  stock before and after the receipt.
- `SALIDA_VENTA`: `costoUnitario` is the frame's average cost at the time of the sale.

## Multi-device limitation

The average is computed on the device that receives the order and synced as a plain value of the
`monturas` row, which resolves conflicts as last-write-wins. If two devices receive different orders
for the same frame before syncing, one average overwrites the other. The `ENTRADA` movements keep
quantity, stock and cost per receipt, so the correct average can be reconstructed from them.
