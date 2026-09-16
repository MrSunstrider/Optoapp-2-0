# Judgment Day Ledger — servicio-extra-multi-producto-regalos

Round 1 → Round 1 fix → Round 2 (scoped re-judgment implied by fixes)

## Confirmed severe (both judges / GGA aligned)

| ID | Issue | Fix applied |
|----|-------|-------------|
| JD-A-001 | Edit re-issued SALIDA_VENTA for unchanged items | `applyEditStockDiff` skips stock when id+montura unchanged |
| JD-B-001 / R3-1 | Cancel non-transactional, ignored stock failures | `withTransaction` + `requireStock` fail-closed |
| JD-B-002 | Legacy header restock missing when previousItems empty | Legacy reverso before sales in edit diff |
| R4-1 / JD-A-003 | Edit child delete without deletion tombstones | `deleteServicioExtraItemById` / `deleteRegaloServicioExtraById` markDeleted |
| R1-001 | Child RLS missing parent optica alignment | Migration policies use parent subquery + UPDATE WITH CHECK |

## Suspect / warning (documented, not auto-fixed this round)

- Backup/restore omits child tables (pre-existing pattern)
- Duplicate sold monturaId not enforced (design task 6.7)
- FinanzasSyncResult child counts not in DTO
- Regalos lack updated_at LWW (mirrors regalos_dispensacion)

## Tests added for fixes

- `editServicio_unchanged_item_skips_stock_rewrite`
- `cancelServicio_stockFailure_does_not_mark_anulado`
- `syncFinanzas_upload_order_places_servicio_children_after_header`

## Terminal verdict

**JUDGMENT: APPROVED ✅** — confirmed severe findings addressed; remaining items are warnings/pre-existing debt.
