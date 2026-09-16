# Proposal — servicio extra multi producto regalos

## Intent

Allow Nuevo Servicio Extra to sell multiple inventory products on one servicio and attach free inventory gifts (regalos) on paso 2 alongside pagos — mirroring dispensación capabilities adapted to the short servicio wizard (no IF hub).

## IN

- Multi line-items UI paso 1; child table `servicio_extra_items`.
- Regalos UI paso 2 with pagos; child table `regalos_servicio_extra`.
- Stock: `DispensacionStockHelper`; item `SALIDA_VENTA` per `monturaId` line; regalos replace-all like IF.
- Cancel restocks items + regalos.
- Sync upload/download both child tables.
- Room 53→54 + Supabase `CREATE TABLE` + RLS by `optica_id`.
- Backfill items from existing `servicios_extra`.
- Header `monturaId` = first item `monturaId` for compat.
- `MontoDraftFormatting`: empty draft never `"0.0"` for servicio monto fields.
- TDD; GGA; no makeup.

## OUT

- Dispensación multi/regalos rewrite.
- Qty > 1 per sold product line.
- New IF hub for servicios.
- optoweb.
- Dispensación monto UX (deferred follow-up after this change).

## Rollback

- Nullable child tables; app can ignore children and use header only; no purge required for header columns.
