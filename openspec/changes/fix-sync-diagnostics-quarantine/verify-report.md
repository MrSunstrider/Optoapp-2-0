# Verify: fix-sync-diagnostics-quarantine

Date: 2026-08-27

## Diagnostic addressed

| Error | Root cause | Fix |
|-------|------------|-----|
| `dispensaciones_monto_pagado_chk` 23514 on `fd4fbba4-…` | Upload sent `monto_pagado = SUM(pago_effect) < 0` | `safeParentBalanceForUpload` floors to 0 on parent upsert only |
| 4× `parent_missing:dispensacion:fd4fbba4-…` | Parent never reached remote | Unblocked once dispensación uploads |
| 2× `idx_movimientos_conflict` 23505 | PK vs composite-key dedup | `fix-inventory-movimientos-pk-reconcile` (already in worktree) |

## Commands

| Command | Result |
|---------|--------|
| `FinanzasUploadValidatorTest` + `UploadSyncCoordinatorTest` | BUILD SUCCESSFUL |
| Inventario partition/upload/fetch tests | BUILD SUCCESSFUL |
| Full `:optoapp:testDebugUnitTest` | (see gate run) |

## FR mapping

| Requirement | Test |
|-------------|------|
| Negative net → upload `monto_pagado=0` | `dispensacion with negative pagos net uploads monto_pagado zero not quarantined` |
| Non-negative unchanged | `safeParentBalanceForUpload floors negative net to zero` |
| Movimiento PK reconcile | `MovimientoUploadPartitionTest`, `SyncInventarioUseCaseUploadTest` |

## Manual (device)

1. Sync finanzas → dispensación `fd4fbba4-…` must leave quarantine; pagos attempt upload.
2. Edit dispensación with tienda montura → inventario sync → no `23505 / idx_movimientos_conflict`.

## RDD

Invariant: remote parent CHECK `>= 0` satisfied on upload snapshot; local ledger unchanged until pagos trigger recalc. Rollback: revert `FinanzasUploadValidator.kt` + `UploadSyncCoordinator.kt` upload paths.

## Post-sync data note

If pagos net remains negative after all pagos upload, individual reembolso/reverso rows may still quarantine on server trigger — review pagos in Información Financiera for that OT.
