# Exploration — servicios extra venta accesorios

## Root cause

- `ServiciosViewModel.monturas` filters `InventarioItemKind.isArmazon(categoria)` — ACCESORIO (líquidos, cofres) excluded.
- `ServicioExtra` has no `monturaId`; picker only copies label/price; no stock movement on save.
- Spec R3 in `inventario-ux-optica-accesorios` intentionally excluded ACCESORIO from OT **and** servicios extra.

## Constraints

- Dispensación picker stays armazón-only.
- Single stock writer: local `DispensacionStockHelper`, not finanzas RPC.
- Movement identity: `(referenciaId, tipo, monturaId)` unique index.
- Room v50; Supabase `servicios_extra` has no `montura_id` yet.

## Risks

- Edit changing linked product: restock old + sale new in one transaction.
- Cancel must restock without double-restock on idempotent Anulado.
