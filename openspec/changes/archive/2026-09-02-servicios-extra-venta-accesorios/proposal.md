# Proposal — servicios extra venta accesorios

## Intent

Allow selling inventory accessories (líquidos, cofres, etc.) from servicios extra search picker and decrement/restock stock via the existing local movement writer.

## IN

- Picker lists all active inventory (armazón + ACCESORIO) with stock > 0.
- Persist `monturaId` on `ServicioExtra` (Room + Supabase sync).
- Save: `SALIDA_VENTA` when product linked; cancel/edit: `AJUSTE` restock with `:rev:` referencia.
- TDD strict; GGA before remote migration.

## OUT

- Qty > 1, barcode, OT picker change, second stock writer.

## Rollback

- Nullable `montura_id` column remains; revert app filter/stock code. No data purge required.
