# Design

## Trigger

Replace incremental `monto_pagado += delta` with a recompute of both possible parents (including origin-move old/new ids):

```sql
UPDATE dispensaciones
SET monto_pagado = COALESCE((
  SELECT SUM(public.pago_effect(tipo, monto)) FROM pagos WHERE dispensacion_id = v_id
), 0)
WHERE id = v_id;
```

Same for `servicios_extra.a_cuenta`. Existing W1–Wn write-path tests stay valid (they start cache at 0). New W-DUAL starts cache at 100 then inserts Abono 100.

## Cierre

Pure helper `cierreVentaPagado(cache, entityId, ledgerById) = ledgerById[id] ?: cache`.

ViewModel builds `ledgerById` from the already-loaded day's `pagos` via `PagoEffect`. Cards and `saldoPendiente` use the helper. Same-day cobros win over a stale/downloaded cache; no same-day cobro keeps prior-day cache.

## Out of scope here

Nine historical servicios where ledger > `a_cuenta` are duplicate pago rows, not this increment. Do not clamp or delete them in this change.

Remaining cache readers that are not the Cierre flicker (they can show a stale debt until the next parent upsert; the SUM trigger stops new doubling):

- `PacienteDao` debt filter (`montoTotal - montoPagado` / `aCuenta`)
- `DispensacionDao` `SUM(montoPagado)`
- `SyncFinanzasMerge` `maxOf(canonical.montoPagado, duplicate.montoPagado)`

