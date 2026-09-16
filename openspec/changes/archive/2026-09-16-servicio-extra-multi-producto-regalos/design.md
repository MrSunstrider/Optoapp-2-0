# Design — servicio-extra multi producto regalos

## Intent

Mirror dispensación’s child-table model for Nuevo Servicio Extra only: multi sold inventory lines + in-wizard regalos on paso 2, without touching dispensación business logic or adding an IF hub for servicios.

## Architecture decisions

### ADR-1: Dedicated child tables (not shared with dispensación)

- Tables: `servicio_extra_items`, `regalos_servicio_extra`.
- Reject JSON-on-header and shared `dispensacion_*` FKs (couples domains; breaks sync/RLS).
- Parent FK: `ON DELETE CASCADE` to `servicios_extra`.

### ADR-2: Header remains backward-compatible denorm

Readers that only know `servicios_extra.monturaId` / `descripcion` / `montoTotal` keep working:

| Header field | Source |
|--------------|--------|
| `monturaId` | First sold line’s `monturaId` (null if none) |
| `descripcion` | First sold line `descripcion` (fallback: joined labels if design needs list readability) |
| `montoTotal` | `sum(items.monto)` |

List/cancel/sync that iterate children are authoritative for multi-line stock.

### ADR-3: Stock identity is per child row

Unique index remains `(referenciaId, tipo, monturaId)` via `DispensacionStockHelper` only.

| Event | tipo | delta | referenciaId |
|-------|------|-------|--------------|
| Sold line sale | `SALIDA_VENTA` | -1 | `item.id` |
| Sold line restock (edit/cancel) | `AJUSTE` | +1 | `movimientoReferenciaForServicioExtraReverso(servicioId, monturaId)` when single montura per servicio historically; for multi-line prefer `"$itemId:rev"` if two lines could share montura — **this change disallows duplicate `monturaId` on sold lines** to keep existing reverso helper safe |
| Regalo sale | `SALIDA_VENTA` | -cantidad | `movimientoReferenciaForRegalo(regalo.id)` |
| Regalo restock | `AJUSTE` | +cantidad | same regalo referencia |

Legacy: sale used `referenciaId = servicio.id`. Backfill forces `item.id == servicio.id` when `monturaId` was set so cancel/edit reverso still resolves against the historical movement pair.

### ADR-4: Regalos replace-all in wizard txn (IF pattern, no hub)

Same as `InformacionFinancieraViewModel` / `DispensacionViewModel` regalos: load initial set → on save restock removed → deduct current → replace DAO rows. Scope: Nuevo/Editar Servicio wizard only.

### ADR-5: MontoDraftFormatting API

```kotlin
object MontoDraftFormatting {
    fun formatDraft(value: Double?): String
    fun formatDraftFromAutofill(precio: Double): String
    fun parseDraft(raw: String): Double?
}
```

- Zero/null → `""` (never `"0.0"`).
- Integer-valued doubles → no fractional suffix.
- Non-integers → `Locale.US` `%.2f`.
- Apply to servicio line montos (and header display if still editable); dispensación deferred.

## Entities

### `ServicioExtraItem` (`servicio_extra_items`)

| Field | Notes |
|-------|--------|
| `id` | PK; **MUST** equal parent `id` on legacy backfill when parent `monturaId` set |
| `servicioExtraId` | FK → `servicios_extra.id` |
| `monturaId` | nullable inventory link |
| `descripcion` | line label |
| `monto` | line sale amount |
| `opticaId` | tenant |
| `updatedAt` / `updatedBy` | sync metadata |

Qty fixed at 1 (no qty column this change).

### `RegaloServicioExtra` (`regalos_servicio_extra`)

Parallel to `RegaloDispensacionEntity`:

| Field | Notes |
|-------|--------|
| `id` | PK; stock referencia |
| `servicioExtraId` | FK |
| `productoId` | montura/accesorio id |
| `cantidad` | ≥ 1 |
| `costoUnitario` | snapshot |
| `descripcion` | display |
| `motivo` | optional |
| `opticaId` | tenant |

## Migration SQL outline

### Room `MIGRATION_53_54`

1. `CREATE TABLE servicio_extra_items (... CASCADE FK ...)` + indexes on `servicio_extra_id`, `optica_id`.
2. `CREATE TABLE regalos_servicio_extra (... CASCADE FK ...)` + index on `servicio_extra_id`.
3. Backfill:

```sql
INSERT INTO servicio_extra_items (id, servicio_extra_id, montura_id, descripcion, monto, optica_id, ...)
SELECT
  CASE WHEN monturaId IS NOT NULL AND trim(monturaId) != '' THEN id ELSE id END, -- id == servicio.id when montura set
  id,
  monturaId,
  descripcion,
  montoTotal,
  opticaId,
  ...
FROM servicios_extra;
```

When `monturaId` is set, `item.id` MUST equal `servicios_extra.id`. When unset, still one row per servicio; `id` MAY equal parent id for simplicity (recommended: always `id = parent.id` for the single backfill row).

4. Register entities + DAOs on `OptoDatabase` version **54**.

### Supabase

```sql
CREATE TABLE public.servicio_extra_items (
  id TEXT PRIMARY KEY,
  servicio_extra_id TEXT NOT NULL REFERENCES public.servicios_extra(id) ON DELETE CASCADE,
  montura_id TEXT NULL,
  descripcion TEXT NOT NULL DEFAULT '',
  monto REAL NOT NULL DEFAULT 0,
  optica_id TEXT NOT NULL,
  updated_at TIMESTAMPTZ NULL,
  updated_by TEXT NULL
);
CREATE INDEX idx_servicio_extra_items_parent ON public.servicio_extra_items(servicio_extra_id);
CREATE INDEX idx_servicio_extra_items_optica ON public.servicio_extra_items(optica_id);

CREATE TABLE public.regalos_servicio_extra (
  id TEXT PRIMARY KEY,
  servicio_extra_id TEXT NOT NULL REFERENCES public.servicios_extra(id) ON DELETE CASCADE,
  producto_id TEXT NOT NULL,
  cantidad INTEGER NOT NULL DEFAULT 1,
  costo_unitario REAL NOT NULL,
  descripcion TEXT NOT NULL,
  motivo TEXT NOT NULL DEFAULT '',
  optica_id TEXT NOT NULL
);
CREATE INDEX idx_regalos_servicio_extra_parent ON public.regalos_servicio_extra(servicio_extra_id);

ALTER TABLE ... ENABLE ROW LEVEL SECURITY;
-- policies: optica_id = jwt/app current óptica (mirror regalos_dispensacion restrictive style)
```

Optional remote backfill from existing `servicios_extra` mirroring Room rule (`id = servicio id` when `montura_id` set).

## Stock sequences

### Save (create)

```
txn {
  persist header (denorm from items)
  replace items
  for each new item with monturaId: SALIDA_VENTA -1 ref=item.id
  persist pagos
  replace regalos: SALIDA_VENTA -qty ref=regalo.id
}
schedule finanzas sync
```

### Save (edit)

```
txn {
  load previous items + regalos
  restock removed item monturas (AJUSTE +1, reverso ref)
  deduct newly added item monturas (SALIDA_VENTA -1, ref=item.id)
  unchanged item ids: no stock rewrite
  replace-all regalos (restock removed, deduct current)
  upsert header denorm + pagos
}
```

### Cancel

```
if estado == Anulado: return
reverse missing pagos (existing)
for each item.monturaId: AJUSTE +1 reverso ref
for each regalo: AJUSTE +qty ref=regalo.id
estado = Anulado
schedule sync
```

## Sync order

Extend `SyncFinanzasUseCase` / coordinators:

**Upload (insert after `servicios_extra`):**  
`… → servicios_extra → servicio_extra_items → regalos_servicio_extra → … pagos …`  
(Exact slot: immediately after parent servicios; before or with pagos is fine if FKs only point at servicios — prefer **items then servicio-regalos before pagos** for clarity.)

**Download:** same parent-before-child order.

`DeletionSyncHelper` entity types: add both child tables. DTOs map snake_case columns.

Do **not** change `recalcular_resumen_diario` cost semantics (servicios cost remains 0 unless a later change scopes it).

## UI

### Paso 1 — Datos / productos

- Multi-line list: add/remove inventory picks via existing `inventarioParaServicioExtra` + `MonturaSearchField`.
- Per line: descripcion + editable monto (`MontoDraftFormatting`).
- Autofill monto from montura `precio` via `formatDraftFromAutofill`.
- Header `montoTotal` read-only sum (or hidden and derived on save).

### Paso 2 — Pagos + Regalos

- Keep existing pagos / estado UI.
- Add `RegalosSection`-style block (reuse dispensación/IF UX patterns, wire to servicio state).
- Persist regalos in same VM save transaction.

OUT: IF hub screens for servicio regalos; qty > 1 on sold lines; dispensación monto draft follow-up.

## Cancel

`CancelServicioExtraUseCase` loads children (or repository aggregate), restocks all item monturas + regalos through helper, then anula. Must not rely solely on header `monturaId` (would miss lines 2..N).

## Risks

| Risk | Mitigation |
|------|------------|
| Unique movement collisions on multi-line | Sale ref = `item.id`; disallow duplicate sold `monturaId`; reverso uses existing helper per montura |
| Header denorm drift | Cancel/edit iterate children; list may still show first product only (accepted compat) |
| Orphan sync children | Upload/download after parent; CASCADE deletes |
| Legacy stock refs | Backfill `item.id = servicio.id` when `monturaId` set |
| Monto `"0.0"` regression | Helper + unit tests; apply all editable servicio monto drafts |
| Double restock on cancel | Idempotent `Anulado` short-circuit; distinct refs per child |
| Partial txn on stock failure | Fail save; no partial header commit (single Room txn) |

## Out of scope (explicit)

- Dispensación rewrite / picker changes  
- qty > 1 sold lines  
- IF hub for servicios  
- optoweb  
- `MontoDraftFormatting` on dispensación / untouched abono dialogs (unless abono field is edited in this wizard and already shares the draft path)
