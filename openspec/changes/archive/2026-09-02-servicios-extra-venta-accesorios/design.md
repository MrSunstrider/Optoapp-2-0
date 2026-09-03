# Design — servicios extra venta accesorios

## Picker

`inventarioParaServicioExtra(items)` → active items only (armazón + ACCESORIO). Dispensación unchanged (`isArmazon`).

## Schema

- Room 50→51: `ALTER TABLE servicios_extra ADD COLUMN monturaId TEXT`
- Supabase: `ALTER TABLE servicios_extra ADD COLUMN montura_id TEXT` (nullable)

## Stock sequence (save)

1. Resolve `previousMonturaId` if edit.
2. Inside transaction: restock old if changed/cleared; deduct new if create or changed; persist servicio with `monturaId`.
3. `referenciaId` sale = `servicioId`; restock = `movimientoReferenciaForServicioExtraReverso(servicioId, monturaId)`.

## Cancel

`CancelServicioExtraUseCase` + `DispensacionStockHelper`: if `monturaId` set, AJUSTE +1 before estado Anulado.
