# CLK-LX3 Room 46 verification — 2026-08-14

## Install

- APK: signed release `versionName=1.16.2` `versionCode=51`
- `adb install -r` at **23:01:39** (no wipe)
- Includes: inventory single-writer (Room 45) + `referenciaId` identity (Room 46)

## Sync runs (logcat)

Three full orchestrator passes completed ~23:03:52 → 23:05:58.

| Module | Result |
|--------|--------|
| Pacientes | fin OK 293/293 |
| Historial | fin OK 293/293 |
| Finanzas | upload pagos=582, download pagos=595 (no 23514) |
| Proveedores / OC / KPI | fin OK |
| Inventario | fin OK monturas=30 movimientos=37 |
| InventarioFisico | fin OK |

## Error probes (logcat)

| Probe | Count |
|-------|------:|
| `23505` | **0** |
| `idx_movimientos_conflict` | **0** |
| `23514` / `pagos_monto_chk` | **0** (one false hit: accel sensor sample `-0.188514`) |
| `rpc_adjust_montura_stock` | **0** |
| `duplicate key` | **0** |
| Sync WARN/ERROR (app) | only `UpdateChecker` coroutine-scope noise at login |

## UI diagnostics

- Estado remoto: **OK**
- Última sync: `2026-08-15T04:05:58.446+00:00`
- Errores locales de sincronización: **ninguno**

## Remote probes (Sersa `25af5a92-…`)

- `sync_telemetry_optica`: `last_status=ok`, `last_stage=finalizado`, `last_error=''`
- Movements: `ENTRADA=10`, `SALIDA=2`, `SALIDA_VENTA=25` (total 37)
- `venta` phantoms: **0**
- empty `referencia_id`: **0**

## Verdict

**PASSED** — financial ledger + inventory single-writer + referencia identity verified on device.
