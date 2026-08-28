# Proposal: fix-sync-diagnostics-quarantine

## Intent

Unblock finanzas sync when local `PagoEffect` net is negative for a dispensación/servicio. Remote CHECK `monto_pagado >= 0` / `a_cuenta >= 0` rejects the parent upsert; pagos then fail with `parent_missing`.

## Scope

- Clamp parent balance to `0` on upload payload only (`safeParentBalanceForUpload`)
- Same for `servicios_extra.a_cuenta`
- Inventory `idx_movimientos_conflict` already fixed in `fix-inventory-movimientos-pk-reconcile` (ship with this verify pass)

## Out of scope

- Changing Supabase CHECK or trigger floor
- Rewriting historical reembolso rows

## Success

- Dispensación `fd4fbba4-…` uploads with `monto_pagado = 0` when net pagos < 0
- Pagos attempt after parent exists; poison rows quarantine per existing isolating upsert
- No new `23505` on inventario after PK reconcile
