# Spec Delta — servicio-extra multi producto regalos

## ADDED Requirements

### Requirement: ServicioExtra multi line-items

The system MUST persist sold products for a `ServicioExtra` as child rows in `servicio_extra_items` (not as a single header-only product). Each sold line MUST carry its own `id`, `servicio_extra_id`, optional `montura_id`, `descripcion`, `monto`, and `optica_id`. Quantity per sold line MUST be exactly 1 for this change.

#### Scenario: Save creates one item per sold product

- GIVEN Nuevo Servicio Extra with two inventory products selected on paso 1
- WHEN save succeeds
- THEN exactly two rows MUST exist in `servicio_extra_items` for that servicio
- AND each row MUST reference the parent `servicios_extra.id`

#### Scenario: Empty sold lines rejected when inventory sale intended

- GIVEN paso 1 has zero sold line items and no free-text-only service path requiring items
- WHEN the operator attempts to save a product sale
- THEN save MUST fail without writing header/items/pagos/regalos stock movements

### Requirement: Regalos on paso 2 with pagos

The system MUST allow attaching free inventory gifts (`regalos_servicio_extra`) on wizard paso 2 alongside pagos. Regalo rows MUST persist in the same save transaction as the parent servicio, pagos, and sold items. Each regalo MUST expose `id`, `servicio_extra_id`, `producto_id`, `cantidad`, `costo_unitario`, `descripcion`, optional `motivo`, and `optica_id`.

#### Scenario: Regalo saved with pagos

- GIVEN paso 2 has one pago and one regalo product with cantidad ≥ 1
- WHEN save succeeds
- THEN `regalos_servicio_extra` MUST contain that regalo row linked to the servicio
- AND pagos for the servicio MUST persist in the same transaction

#### Scenario: Zero regalos allowed

- GIVEN paso 2 has pagos but no regalos
- WHEN save succeeds
- THEN the servicio MUST persist with zero `regalos_servicio_extra` rows

### Requirement: Stock rules for items and regalos via DispensacionStockHelper

All inventory mutations for servicio sold lines and regalos MUST go through `DispensacionStockHelper`. Sold lines MUST register `SALIDA_VENTA` with `delta = -1` and `referenciaId = item.id`. Regalos MUST use replace-all semantics (restock removed regalos, deduct new ones) with `referenciaId = movimientoReferenciaForRegalo(regalo.id)`. The system MUST NOT write montura stock outside `DispensacionStockHelper` for these paths.

#### Scenario: Multi-item sale deducts each montura once

- GIVEN two sold lines with distinct `monturaId` values M1 and M2
- WHEN save succeeds
- THEN `DispensacionStockHelper` MUST register one `SALIDA_VENTA` for M1 with `referenciaId = item1.id`
- AND one `SALIDA_VENTA` for M2 with `referenciaId = item2.id`

#### Scenario: Regalos replace-all on edit

- GIVEN an existing servicio with regalo R-old for product P1
- WHEN edit replaces it with regalo R-new for product P2
- THEN stock for P1 MUST be restocked via helper using `movimientoReferenciaForRegalo(R-old.id)`
- AND stock for P2 MUST be deducted via helper using `movimientoReferenciaForRegalo(R-new.id)`
- AND no double-restock MUST occur for unchanged regalo ids kept across the edit

#### Scenario: Sold-line edit restocks removed monturas

- GIVEN edit removes sold line for montura M-old and adds montura M-new
- WHEN save succeeds
- THEN M-old MUST receive `AJUSTE` restock with distinct reverso `referenciaId`
- AND M-new MUST receive `SALIDA_VENTA` with `referenciaId = newItem.id`

### Requirement: Header denormalization from items

On save, `servicios_extra.monturaId` MUST equal the first sold line's `monturaId` (or null when no inventory line). `servicios_extra.descripcion` MUST denormalize from the first sold line description (or joined display policy documented in design). `servicios_extra.montoTotal` MUST equal the sum of sold-line `monto` values.

#### Scenario: First item drives header monturaId

- GIVEN sold lines ordered [itemA montura `m-1`, itemB montura `m-2`]
- WHEN save succeeds
- THEN `servicios_extra.monturaId` MUST be `m-1`

#### Scenario: montoTotal is sum of line montos

- GIVEN two sold lines with montos 40.0 and 25.5
- WHEN save succeeds
- THEN `servicios_extra.montoTotal` MUST equal 65.5

#### Scenario: No sold inventory line clears denorm monturaId

- GIVEN a servicio saved with zero inventory-linked sold lines
- WHEN save succeeds
- THEN `servicios_extra.monturaId` MUST be null

### Requirement: MontoDraftFormatting for editable servicio montos

Editable servicio monto draft fields MUST use `MontoDraftFormatting` and MUST NEVER display `"0.0"` (or equivalent forced zero draft) for empty or zero drafts. Autofill from inventory price MUST also go through the helper.

#### Scenario: Empty draft stays empty

- GIVEN a new sold-line monto field with no value entered
- WHEN the draft is rendered
- THEN the displayed text MUST be empty
- AND MUST NOT be `"0.0"`

#### Scenario: Non-zero draft formats without trailing junk

- GIVEN a line monto value of `50.0`
- WHEN `MontoDraftFormatting.formatDraft` is applied
- THEN the draft MUST be `"50"` (integer-valued doubles without forced `.0`)

#### Scenario: Parse empty draft yields null

- GIVEN draft text `""`
- WHEN `MontoDraftFormatting.parseDraft` runs
- THEN the result MUST be `null`

### Requirement: Room 53→54 and Supabase child tables with RLS

The system MUST ship Room `MIGRATION_53_54` creating local `servicio_extra_items` and `regalos_servicio_extra`, bump `OptoDatabase.version` to 54, and register the migration. The system MUST ship a Supabase migration creating `public.servicio_extra_items` and `public.regalos_servicio_extra` with FK CASCADE to `servicios_extra`, indexes on parent ids, and RLS policies scoped by `optica_id`.

#### Scenario: Room migration creates child tables

- GIVEN a v53 database with existing `servicios_extra` rows
- WHEN `MIGRATION_53_54` runs
- THEN tables `servicio_extra_items` and `regalos_servicio_extra` MUST exist
- AND all pre-existing `servicios_extra` rows MUST survive

#### Scenario: Supabase RLS denies cross-óptica child access

- GIVEN RLS enabled on both child tables
- WHEN a session scoped to óptica A queries rows with `optica_id = B`
- THEN those rows MUST NOT be returned

### Requirement: Sync upload and download of child tables

`SyncFinanzasUseCase` MUST upload and download `servicio_extra_items` and `regalos_servicio_extra` after parent `servicios_extra` (parents before children). Deletion sync MUST include the new entity types. Child sync MUST NOT orphan rows relative to missing parents within a successful sync pass.

#### Scenario: Upload order places children after servicios_extra

- GIVEN pending local servicios with items and regalos
- WHEN finanzas upload runs
- THEN `servicios_extra` MUST upload before `servicio_extra_items`
- AND `servicios_extra` MUST upload before `regalos_servicio_extra`

#### Scenario: Download inserts children after parents

- GIVEN remote children referencing servicio S
- WHEN finanzas download runs
- THEN local `servicios_extra` row S MUST exist before child upserts for S succeed

### Requirement: Cancel restocks items and regalos

`CancelServicioExtraUseCase` MUST restock every sold-line montura and every regalo product via `DispensacionStockHelper` before marking the servicio `Anulado`. Cancel MUST remain idempotent when the servicio is already `Anulado` (no second restock).

#### Scenario: Cancel restocks all sold lines and regalos

- GIVEN an active servicio with two sold monturas and one regalo
- WHEN cancel runs
- THEN each sold montura MUST receive one `AJUSTE` restock
- AND the regalo product MUST be restocked via helper using its regalo referencia
- AND servicio estado MUST become `Anulado`

#### Scenario: Second cancel is no-op for stock

- GIVEN a servicio already `Anulado`
- WHEN cancel is invoked again
- THEN no additional stock movements MUST be written

### Requirement: Backfill one item per existing servicio

Room `MIGRATION_53_54` (and equivalent Supabase backfill when required) MUST create exactly one `servicio_extra_items` row per existing `servicios_extra` row that needs a sold-line representation. When the parent has a non-null non-blank `monturaId`, the backfilled item's `id` MUST equal the parent `servicios_extra.id` so legacy `SALIDA_VENTA` movements that used `referenciaId = servicio.id` remain valid under the unique `(referenciaId, tipo, monturaId)` index.

#### Scenario: Legacy servicio with monturaId backfills item id equals servicio id

- GIVEN a v53 row `servicios_extra.id = 'svc-1'` with `monturaId = 'm-1'` and `montoTotal = 80`
- WHEN migration to 54 completes
- THEN exactly one `servicio_extra_items` row MUST exist with `id = 'svc-1'`, `servicio_extra_id = 'svc-1'`, `montura_id = 'm-1'`, and `monto = 80`

#### Scenario: Legacy servicio without monturaId still gets one descriptive item

- GIVEN a v53 row `servicios_extra.id = 'svc-2'` with `monturaId` null and non-blank `descripcion`
- WHEN migration to 54 completes
- THEN exactly one `servicio_extra_items` row MUST exist for `svc-2`
- AND that item's `montura_id` MUST be null
- AND the item `descripcion` MUST equal the parent `descripcion`
- AND the item `monto` MUST equal the parent `montoTotal`
