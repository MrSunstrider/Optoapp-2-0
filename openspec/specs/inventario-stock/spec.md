# Spec: inventario-stock

## Purpose

Inventory stock rules for monturas and accesorios in OptoApp (Room + Supabase).

## Requirements

### Requirement: Montura SKU uniqueness includes rim type

The system MUST enforce uniqueness of monturas per óptica on `(sku, tipo_aro)` (Room: `sku`, `opticaId`, `tipoAro`; Postgres: `optica_id`, `sku`, `tipo_aro`). The system MUST NOT enforce uniqueness on `(sku)` alone within an óptica.

#### Scenario: Same SKU two rim types allowed

- GIVEN óptica O and SKU `RAY-2140`
- WHEN a montura row exists with `tipoAro = "Aro Completo"`
- AND a second row is inserted with same SKU and `tipoAro = "Semi al aire"`
- THEN both rows MUST persist with independent `stockActual`

#### Scenario: Duplicate SKU and tipoAro rejected

- GIVEN óptica O already has SKU `RAY-2140` with `tipoAro = "Aro Completo"`
- WHEN insert attempts same SKU and tipoAro
- THEN persistence MUST fail with a uniqueness conflict

### Requirement: Multi rim-type create with per-type initial stock

On montura create (not edit), the system MUST allow selecting one or more rim types from the catalog and MUST capture a non-negative initial stock per selected type. The system MUST insert one `monturas` row per selected type sharing sku/marca/modelo/costo/precio/stockMinimo/material and differing in `id`, `tipoAro`, and `stockActual`.

#### Scenario: Create Completo and Semi with stocks

- GIVEN create form with SKU/marca/modelo/material filled
- AND tipos "Aro Completo" (stock 5) and "Semi al aire" (stock 3) selected
- WHEN user saves
- THEN exactly two monturas MUST be inserted
- AND Completo row stockActual = 5
- AND Semi row stockActual = 3

#### Scenario: Create without tipo rejected

- GIVEN montura (not accesorio) create with no tipo selected
- WHEN user saves
- THEN save MUST fail with tipo de aro required
- AND no insert MUST occur

### Requirement: Search distinguishes rim-type variants

`MonturaSearchField` and `monturaLabel` MUST include non-blank `tipoAro` so dispensación and servicios extra operators can pick the correct stock row.

#### Scenario: Search list shows tipo and stock

- GIVEN two monturas same SKU different tipoAro with stock > 0
- WHEN operator opens product search
- THEN each row MUST display tipoAro and stockActual

### Requirement: Montura material includes Aluminio

Montura material dropdowns MUST offer Aluminio alongside Acetato, Metal, Carey, TR-90, Econ from a shared catalog source.

#### Scenario: Aluminio selectable

- GIVEN montura create or dispensación montura material field
- WHEN options are listed
- THEN Aluminio MUST be present
