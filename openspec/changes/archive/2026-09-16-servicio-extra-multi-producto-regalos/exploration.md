# Exploration — servicio-extra-multi-producto-regalos

## Root cause / gap

- Nuevo Servicio Extra is still **single-product**: `ServiciosUiState.monturaId` + one `descripcion`/`montoTotal`; stock always `delta = ±1` via `DispensacionStockHelper`.
- Paso 2/2 is **Pagos + Estado only** — no Regalos UI; no `regalos_servicio_extra` table; cancel/edit restock only header `ServicioExtra.monturaId`.
- Dispensación already has multi-line (`dispensacion_items`) + `regalos_dispensacion` (wizard + IF hub) with parent-header denormalization of first item into `dispensaciones.monturaId`.
- Editable monto fields can surface `"0.0"` via `Double.toString()` / formatted zeros — no shared draft helper yet (`MontoDraftFormatting` does not exist).

## Current state (inventory)

| Area | Path | Role today |
|------|------|------------|
| VM | `viewmodel/ServiciosViewModel.kt` | Save header + pagos in one txn; stock for one `monturaId`; cancel → `CancelServicioExtraUseCase` |
| UI wizard | `ui/screens/NuevoServicioScreen.kt` | Steps `Datos` / `Pagos` only |
| Form | `ui/components/servicio/ServicioForm.kt` | Single `MonturaSearchField`; editable `montoTotal` string |
| Entity | `data/dispensacion/DispensacionEntity.kt` → `ServicioExtra` | Header row; optional `monturaId` (Room 53) |
| DAO | `data/servicio/ServicioExtraDao.kt` | CRUD header only |
| Cancel | `domain/CancelLedgerUseCases.kt` → `CancelServicioExtraUseCase` | Reverso pagos + restock single montura; idempotent if `Anulado` |
| Stock | `util/DispensacionStockHelper.kt` | Sole stock writer; unique `(referenciaId, tipo, monturaId)` |
| Ref ids | `domain/MovimientoReferenciaIdentity.kt` | Servicio reverso: `$servicioId:rev:$monturaId`; regalos IF: `movimientoReferenciaForRegalo(regaloId)` |
| Picker filter | `domain/inventario/InventarioParaServicioExtra.kt` | Active inventory (frames + accessories) |
| Sync | `domain/SyncFinanzasUseCase.kt` + Upload/Download coordinators | Uploads `servicios_extra` + `pagos`; **no** servicio items/regalos tables |
| Room | `data/OptoDatabase.kt` | **version = 53**; entities include `DispensacionItem`, `RegaloDispensacionEntity`, not servicio children |
| Supabase | `servicios_extra.montura_id` (migration `20260902010800_…`) | Header FK only; no child tables |
| Disp parallel | `DispensacionItemEntity`, `RegaloDispensacionEntity`, `DispensacionViewModel`, `DispensacionFormSections.RegalosSection`, `InformacionFinancieraViewModel` | Pattern to mirror for servicios **in-wizard only** (IF hub out of scope) |

## Gaps vs dispensación

| Capability | Dispensación | Servicio Extra (today) |
|------------|--------------|------------------------|
| Multi sold lines | `dispensacion_items` | Missing → **`servicio_extra_items`** |
| Header first-item denorm | `dispensaciones.monturaId` from primer item | Already has `monturaId`; keep as denorm of **first sold line** |
| `montoTotal` | Header field (manual / costs) | Must become **sum of line montos** |
| Regalos | `regalos_dispensacion` + stock in same txn; IF hub edit | Missing → **`regalos_servicio_extra`**; wizard Paso 2 only (no IF hub) |
| Qty per sold line | N/A (lens lines, not inventory qty) | **OUT**: qty always 1 per sold line this change |
| Sync children | upload/download items + regalos after parents | Must extend SyncFinanzas order after `servicios_extra` |
| Cancel restock | Disp cancel does not restock items here; regalos handled on IF/save paths | Must restock **all** sold lines + regalos without double-restock |

## Confirmed decisions (user)

1. Scope = **Nuevo Servicio Extra only** — do not rewrite dispensación business logic.
2. Paso 1/2: multiple inventory line items (monturas/accesorios); `montoTotal` = sum of line montos.
3. Paso 2/2: existing Pagos + Regalos section (reuse IF/`RegalosSection` UX pattern); persist in **same transaction**.
4. New tables: `servicio_extra_items`, `regalos_servicio_extra` + RLS; Room **53 → 54** + Supabase migrations.
5. Stock **only** via `DispensacionStockHelper`.
6. Header denormalizes first item into `servicios_extra.monturaId` (backward-compatible for list/cancel/sync readers).
7. Introduce `MontoDraftFormatting` — never show `"0.0"` in editable monto fields; **apply in servicios in this change**.
8. **OUT**: qty > 1 per sold line; IF hub for servicios; optoweb; dispensación picker changes.
9. **Deferred**: same monto draft UX in dispensaciones (follow-up after this change).

## Approaches

1. **Mirror dispensación child tables (recommended)** — `servicio_extra_items` + `regalos_servicio_extra` parallel to disp tables; extend wizard + VM transaction + SyncFinanzas; keep header `monturaId`/`descripcion` denorm.
   - Pros: Proven patterns (stock refs, CASCADE, sync order, cancel ledger).
   - Cons: New Room migration + RLS + sync surface area.
   - Effort: Medium–High

2. **Encode lines as JSON on header** — avoid new tables.
   - Pros: Smaller schema.
   - Cons: Breaks sync/RLS/report queries; no clean stock identity per line; fights offline-first model.
   - Effort: Low short-term / High long-term cost — reject

3. **Reuse `dispensacion_items` / `regalos_dispensacion` with nullable servicio FK** — shared child tables.
   - Pros: One schema.
   - Cons: Couples domains; pollutes disp sync/IF hub; high regression risk — reject (also conflicts with “do not rewrite dispensación”)

## Recommendation

**Approach 1.** Model sold lines and regalos as first-class children of `servicios_extra`, mirror IF restock-replace pattern for regalos and multi-montura stock on save/edit/cancel, denorm first sold line onto header, sum line montos into `montoTotal`, add `MontoDraftFormatting` on servicio editable montos, extend SyncFinanzas after parent `servicios_extra`.

## Risks

- **Edit/cancel multi-line stock**: restock removed lines + sell new ones in one txn; movement unique index collisions if `referenciaId` stays parent-only (today sale uses `servicioId` alone — must become **per-line / per-regalo** identity, like `movimientoReferenciaForRegalo`).
- **Header denorm drift**: list/report/cancel paths that only read `servicios_extra.monturaId` miss lines 2..N unless cancel/edit iterate children.
- **Sync order / orphan children**: upload/download must place items+regalos after `servicios_extra` and before/with pagos semantics; deletion sync entity types for new tables.
- **`recalcular_resumen_diario` / cost RPCs**: today servicios cost = 0; multi-product may not change cost unless explicitly designed — confirm no accidental RPC rewrite.
- **Monto draft regression**: empty draft vs parse-on-save; pagos `AbonoDialog` still uses `Double.toString()` — decide whether helper covers header+line montos only or also abono fields in this change (default: servicio line/header montos; abono share if touched).
- **Legacy single-product rows**: migration must backfill one `servicio_extra_items` row from existing `monturaId`+`montoTotal` where present.

## Deferred (explicit)

- Same `MontoDraftFormatting` UX in **dispensaciones** (and any shared IF monto fields not touched here).
- qty > 1 on sold servicio lines.
- Información Financiera hub for servicio regalos/pagos.
- optoweb parity.
- Dispensación picker / OT inventory filter changes.

## Ready for proposal

Yes — scope and out-of-scope are user-confirmed; next phase `sdd-propose`.
