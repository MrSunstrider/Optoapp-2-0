# Proposal — un solo writer para pagado/saldo

## Intent

Eliminar el doble writer de `dispensaciones.monto_pagado` y `servicios_extra.a_cuenta` que infla Pagado en Cierre de Caja tras el primer sync (OT 4582: 200 con un solo Abono 100).

## Evidence

- Cierre 11:07 Perú: tarjeta Pagado 200 / pie "Pagado"; cobros: un Abono 100. Remote: 1 pago; `updated_at` 16:07 UTC.
- Cierre 11:20: Pagado 100 / Saldo 70, tras un segundo sync (servicio Brazos 16:19 UTC).
- Upload: padres con `toRemoto(pagosSum)` **luego** INSERT pagos. Trigger `+= pago_effect` duplica. Download de padres **antes** de pagos escribe el cache inflado en Room.
- El mismo pipeline aplica a **servicios extra** (`uploadServicios` + `a_cuenta` + el mismo trigger).
- Cierre es el único UI que pinta Pagado/Saldo desde el cache. Detalle paciente, reportes, PDF, IF y lista de servicios ya usan `PagoEffect`.
- RPCs (`rpc_deudores`, `rpc_cierre_caja_resumen`, `recalcular_resumen_diario`) ya usan `pago_effect`.
- `PacienteDao` deuda aún filtra por `montoTotal - montoPagado` (cache).

## Scope

- **IN**: trigger de `pagos` (SET desde SUM), Cierre (tarjetas + `saldoPendiente`) para dispensaciones y servicios.
- **OUT**: backfill de 9 servicios con pagos duplicados (cache < ledger; otra clase). No parche por OT.

## Approach

Un writer: el trigger **recomputa** `SUM(pago_effect)` del padre (idempotente). Cierre prefiere el ledger del día cuando hay cobros de esa orden.

## Causal invariant

INV-1: Tras INSERT/UPDATE/DELETE de un pago, `monto_pagado`/`a_cuenta` del padre = `SUM(pago_effect)` de sus pagos, aunque el upsert previo ya llevara esa suma.
