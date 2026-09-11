# Proposal — montura-tipo-aro-stock-variants

## Intent

Allow the same montura SKU to exist as multiple rim-type variants, each with its own initial stock at create time, and make search distinguish variants in dispensación and servicios extra. Add **Aluminio** to montura material options.

## Evidence

- User: same marca/modelo/SKU can be Aro Completo and Semi al aire with different stock.
- Code: `Index(sku, opticaId) unique` on Room; `idx_monturas_sku_optica` on Supabase.
- Shared search: `MonturaSearchField` in MonturaForm, LenteForm, ServicioForm.

## Scope

### IN

- Unique key `(optica_id, sku, tipo_aro)` Room 51→52 + Supabase migration.
- Create UX: multi-select tipo aro + stock per type → N inserts.
- `monturaLabel` + search row show tipoAro + stock.
- Material list: Acetato, Metal, Carey, TR-90, Econ, Aluminio (centralize in OpticalCatalog).

### OUT

- Color/talla variants.
- Changing dispensación tipo-aro cost dropdown semantics beyond autofill.
- Create-time ENTRADA ledger rows.

## Approach

Keep one row per variant (`id` UUID). Shared metadata (sku, marca, modelo, costo, precio, stockMinimo). Sync by `id` unchanged.

## Causal invariants

- INV-1: Two active monturas MAY share `(optica, sku)` only if `tipo_aro` differs.
- INV-2: Create with multiple tipos MUST insert one row per selected tipo with that tipo's stock.
- INV-3: Selecting a variant in search MUST bind `monturaId` to that row's stock pool.
- INV-4: Accesorio create path unchanged (`tipoAro=""`).

## Schema / RLS

- Affects unique index only; RLS by `optica_id` unchanged.
- Rollback: recreate `(optica_id, sku)` unique only when no duplicate SKUs exist.

## Rollback plan

1. Delete duplicate-SKU variant rows or merge stock.
2. Drop `(optica_id, sku, tipo_aro)` unique; recreate `(optica_id, sku)`.
3. Revert Room to v51 index.
