# CLK-LX3 device verification — WU-5 (task 5.4)

Evidence only. No production data was modified from this run beyond what the app itself uploaded.

| Field | Value |
|-------|-------|
| Device | HONOR CLK-LX3 (`CLK_LX3`, HNCLK-Q), Android 14 |
| ADB serial | `AM4U9X3C26G15605` |
| Package | `com.example.optoapp` |
| Installed build | versionName **1.16.2**, versionCode 51, release-signed (`318fc041`), installed 2026-08-14 01:48 |
| Óptica | `25af5a92-4a2d-4e7a-957f-61bec87a07d8` (Sersa Visual & Preventiva) |
| Run timestamp | 2026-08-14 21:13–21:16 (device local) |
| Raw log | `evidence/clk-lx3-2026-08-14-sync.log` (filtered app-process logcat) |

## Install decision

### Run A (2026-08-14 ~21:13) — pre-ledger release only

A debug APK was **not** installed (shared `applicationId`, different signature → uninstall wipe).
At that moment the verify note incorrectly assumed the production keystore was absent.
Verification ran against shipped **1.16.2** writers (pre WU-2).

### Run B (2026-08-14 ~21:32) — ledger build installed in place

Production keystore **is** available: repo-root `optoapp-release.keystore` +
`local.properties` (`keystore.path` / `keystore.password` / `key.alias` / `key.password`),
consumed by `optoapp/build.gradle.kts` `signingConfigs.release`.

| Step | Result |
|------|--------|
| `./gradlew :optoapp:assembleRelease` | BUILD SUCCESSFUL |
| APK | `optoapp/build/outputs/apk/release/optoapp-release.apk` (~15.8 MB) |
| `adb install -r` on `AM4U9X3C26G15605` | **Success** — same signature `318fc041`, **no uninstall / no data wipe** |
| Post-install | `versionName=1.16.2` `versionCode=51` `lastUpdateTime=2026-08-14 21:32:36` |

Cold start after `am force-stop` landed on the **login** screen (email prefilled
`jaermadera@gmail.com`, password empty). The operator logged in interactively, then ran
two full syncs (21:38 and 21:39 device local).

## Run B result — `pagos_monto_chk` is GONE

Evidence: `evidence/clk-lx3-2026-08-14-runb-sync.log`,
`evidence/clk-lx3-2026-08-14-runb-diagnostics-ok.png`.

```
DEBUG/SyncFinanzas: Finanzas: upload dispensaciones=300
DEBUG/SyncFinanzas: Finanzas: upload servicios_extra=143
DEBUG/SyncFinanzas: Finanzas: upload pagos=586
DEBUG/SyncFinanzas: Finanzas: upload gastos_operativos=1
DEBUG/SyncFinanzas: Finanzas: download pagos=595
DEBUG/SyncInventarioFisico: InventarioFisico: fin OK
```

Zero `23514` and zero `pagos_monto_chk` occurrences in the whole Run B buffer. The Room 43→44
`ABS(monto)` migration plus the new writers removed the poison that aborted `upsert:pagos:chunk8`
in Run A.

Production state after Run B (`optica_id=25af5a92-…`):

| Probe | Value |
|-------|-------|
| `pagos` total | 595 |
| `monto < 0` | **0** |
| `tipo='Anulación'` (legacy, effect 0) | 4 |
| `tipo='Reverso'` / `tipo='Reembolso'` | 0 / 0 |
| `reversa_pago_id IS NOT NULL` | 0 |
| `servicios_extra estado='Anulado'` | 2 |
| `montura_movimientos tipo='venta'` rows / distinct `referencia_id` | 24 / **24** |

Remote telemetry (`sync_telemetry_optica`): `last_status=ok`, `last_stage=completado`,
`last_error=''`, `last_sync_at=2026-08-15 02:40:01+00`.

Diagnostics card after **Verificar sync ahora**: `Estado: OK`, no local sync errors,
no background errors, history `[completado] 14/08 21:40`. The earlier `Estado: ERROR`
the operator saw was the **stale Run A snapshot** (02:14:53), not a live failure.

## Run B open item — inventory stock RPC is not idempotent (pre-existing, separate scope)

48 occurrences in the Run B buffer (24 per sync), SQLSTATE **23505**:

```
ERROR/SyncFinanzas: RPC adjust stock exception: duplicate key value violates unique
                    constraint "idx_movimientos_conflict"
Details: "Key (referencia_id, tipo, montura_id)=(969ceb93-…, venta, 16f5f6f4-…) already exists."
URL: .../rest/v1/rpc/rpc_adjust_montura_stock
```

Mechanism: `UploadSyncCoordinator` uploads a **full** dispensaciones snapshot and, after each
chunk, calls `rpc_adjust_montura_stock` for every item with a `monturaId`. The RPC decrements
`monturas.stock_actual` and then inserts into `montura_movimientos`; it has no
`ON CONFLICT` / prior-existence check, so the unique index
`(referencia_id, tipo, montura_id)` rejects the replay and the whole function rolls back.

Consequences:

- Stock is **not** corrupted — 24 movements for 24 distinct references confirms the index is
  the only thing preventing repeated decrements, and the rollback keeps `stock_actual` intact.
- The failures are logged and swallowed (`AppLogger.e` inside a per-item `catch`), so they do
  not mark sync state, do not surface in the diagnostics card, and do not block `last_status=ok`.
- Cost is 24 pointless round-trips plus alarming logcat noise on every finanzas cycle.

Fix belongs to its own change (idempotent RPC returning `ok` on replay, and/or client-side skip
for already-adjusted dispensaciones). Not part of the ledger contract.

## Success-checklist status — Run B

| Criterion (task 5.4) | Status |
|---|---|
| No 23514 on Anulado/Reclamada estado domains | **PASS** |
| No 23514 on `pagos_monto_chk` | **PASS** — 586 pagos uploaded clean |
| Remote `last_status=ok` | **PASS** — `ok` / `completado` / empty error |
| Diagnostics truthful (no stale error, no hidden background error) | **PASS** |
| No pago resurrection | **PASS** — 595 remote = 595 downloaded, no negative rows reappear |
| Reverso/Reembolso accepted remotely | **NOT EXERCISED** — needs a real cancel/reclaim on production data |
| Caja net only via Reverso/Reembolso | **NOT EXERCISED** — same reason |
| Pacientes HTTP captured | **N/A** — pacientes 293/293 OK in both runs |

## What was executed

1. `adb logcat -c`, then continuous capture.
2. Launched the app (already authenticated; no PIN prompt) — landed on Configuración → *Diagnóstico de sincronización*.
3. Drawer → **Sincronizar Cloud** (full sync, all 8 modules).
4. Captured screens before/after and re-read the diagnostics card.

## Result 1 — `servicios_extra_estado_domain_chk` is GONE

`servicios_extra` and `dispensaciones` uploaded clean; no 23514 on either estado domain.

```
DEBUG/SyncFinanzas: Finanzas: upload dispensaciones=300
DEBUG/SyncFinanzas: Finanzas: upload dispensacion_items=311
DEBUG/SyncFinanzas: Finanzas: upload servicios_extra=143
DEBUG/SyncFinanzas: Finanzas: upload costos_productos=7
```

Corroborated read-only on production (`sflhtihqdhrlryeyrzdo`):

| Probe | Value |
|-------|-------|
| `pago_effect(text,numeric)` exists | true |
| `pagos.reversa_pago_id` exists | true |
| `servicios_extra` rows with `estado='Anulado'` | **2** (previously rejected with 23514) |
| `pagos` with `monto < 0` | 0 |
| `pagos` total / `tipo='Anulación'` | 579 / 0 |

The WU-1 estado CHECK expansion applied on production is confirmed effective on a real client.

## Result 2 — `pagos_monto_chk` still fails (expected, old writers)

```
E/SyncFinanzas: Error REST en upsert:pagos:chunk8 (400): new row for relation "pagos"
                violates check constraint "pagos_monto_chk"
E/SyncFinanzas: Code: 23514
E/SyncFinanzas: Hint: null
E/SyncFinanzas: Details: null
E/SyncFinanzas: URL: https://sf.../rest/v1/pagos?columns=id%2Cdispensacion_id%2Cservicio_extra_id
                %2Cfecha%2Ctipo%2Cmonto%2Cmetodo_pago%2Cnota%2Coptica_id%2Cupdated_at
                %2Cupdated_by%2Cventa_id
E/SyncFinanzas: Headers: {Authorization=[Bearer ey... (len=1560)], Content-Profile=[public],
                Accept=[application/json], Prefer=[resolution=merge-duplicates,return=minimal],
                apikey=[ey... (len=208)], X-Client-Info=[supabase-kt/3.6.0],
                X-Supabase-Client-Platform=[Android]}
E/SyncFinanzas: Http Method: POST
```

This is the negative-`monto` `Anulación` poison written by 1.16.2 and still resident in the device's
Room 43 database. It clears only when the WU-2 build ships: Room 43→44 runs
`UPDATE pagos SET monto = ABS(monto)` and the cancel path stops writing negative rows. Nothing on the
device or in the DB can clear it before that build is installed.

Two consequences visible in this build, both fixed by WU-4 but not yet on the device:

- The whole `pagos` chunk aborts — there is no per-row binary-split quarantine, so valid rows in
  chunk 8 never upload.
- Upload stops at `pagos`; `costos_biselado=0` and the remaining finanzas tables in that batch are
  skipped for the cycle.

## Result 3 — pacientes did NOT fail

The pacientes stage completed cleanly, so **no pacientes HTTP failure body exists for this run**:

```
DEBUG/SyncPacientes: Pacientes: inicio sync (opticaId=25af5a92-…, download=true, skipUpload=false)
DEBUG/SyncPacientes: Upload pacientes: 293/293 filas tras prevalidación de HO
DEBUG/SyncPacientes: Subidos 293 pacientes a Supabase
DEBUG/SyncPacientes: Descargados 293 pacientes desde Supabase
DEBUG/SyncPacientes: Pacientes: fin OK (subidos=293, bajados=293)
```

`fix-sync-pacientes-http` stays unopened: there is no status/body to classify, and the proposal
forbids guessing a schema. If it recurs, the capture path used here reproduces the evidence.

## Result 4 — unrelated pre-existing defect observed

~25 consecutive failures at the start of the finanzas stage, before any table upload:

```
ERROR/SyncFinanzas: RPC adjust stock exception: duplicate key value violates unique constraint
                    "idx_movimientos_conflict"
```

Inventory-movement idempotency, not the ledger. Out of scope for this change; worth its own issue.

## Result 5 — diagnostics UI state on 1.16.2

The background-errors section was **not** removed in 1.16.2 — it renders under *Errores en segundo
plano* with a *Limpiar errores* button. What is missing, and what WU-5 adds:

| Gap in 1.16.2 | WU-5 change |
|---|---|
| No copy action for background errors | Copy button + `SyncDiagnosticsReport.backgroundSection` |
| Section hidden entirely when empty | Always visible with the empty-state string |
| Errors lost on process restart (in-memory only) | `BackgroundErrorStore` persists to SharedPreferences |
| Copy-all emitted raw `entityType/entityId/lastError` | Full report: counts, óptica, status, sanitized detail |
| Oldest-first ordering | Newest-first, capped at 10 with overflow count |

After the sync the card showed: remote status **ERROR**, local error `[upload_pagos] batch →
pagos_monto_chk`, three `[sync:finanzas]` background entries plus `[sync] Full sync completada con
errores`, and matching `Historial de sync` rows.

## Success-checklist status

| Criterion (task 5.4) | Status |
|---|---|
| No 23514 on Anulado/Reclamada estado domains | **PASS** — servicios_extra 143 + dispensaciones 300 uploaded |
| No 23514 on `pagos_monto_chk` | **BLOCKED** — old writers still on device; needs the WU-2+ build |
| Reverso/Reembolso accepted remotely | **NOT EXERCISED** — no client can emit them until WU-2 ships |
| Remote `last_status=ok` | **FAIL (truthful)** — remains `error` while pagos is poisoned |
| No pago resurrection | **NOT OBSERVABLE** — download skip is WU-4, not in 1.16.2 |
| Caja net only via Reverso/Reembolso | **NOT EXERCISED** — same reason |
| Pacientes HTTP captured | **N/A** — pacientes succeeded; nothing to capture |

## Remaining blockers

1. **Release APK still carries the pre-WU-2 writers.** Every finanzas cycle will keep hitting
   `pagos_monto_chk` until a build containing WU-2/3/4 is installed. Signing that build needs the
   production keystore, which is not available in this workspace.
2. Ledger-clean re-verification (Reverso/Reembolso round-trip, quarantine isolation, download skip,
   remote `ok`) must be re-run on CLK-LX3 after that build ships. Until then task 5.4 stays open.
3. `idx_movimientos_conflict` duplicate-key storm is unaddressed and unrelated to this change.
