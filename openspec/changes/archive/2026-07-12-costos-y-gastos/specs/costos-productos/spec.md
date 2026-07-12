# costos-productos Specification

## Purpose

Cost-of-goods tracking for every dispensacion. Matrix-based pricing: lookup cost per eye (lentes), per unit (monturas), per pair (biselado), per box (LC). Hybrid: auto-estimate from linked refraction, manual override.

## Requirements

### R1: Cost Matrix — 8 Blocks

System SHALL support 8 cost blocks organized by product category. Each block SHALL have its own columns and lookup logic. Blocks: Stock Monofocal (by serie), Stock Bifocal (by type×treatment), Stock Multifocal, Fabricacion Resina, Fabricacion Cristal, Monturas, Biselado, Lentes Contacto.

- GIVEN CostosYGastosScreen Tab 1
- WHEN user selects a block from dropdown
- THEN matrix grid shows correct columns for that block

### R2: Lentes Cost Lookup — Per Eye

System SHALL determine lente cost per eye. Logic: |esfera| ≤ 6.00 → stock → find serie (1/2/3) by |cilindro| range → lookup `costos_productos`. |esfera| > 6.00 → fabricacion → lookup by material+tipo_lente+tratamiento (serie=null). Each eye (OD/OI) SHALL be calculated independently.

- GIVEN receta OD: esf -3.00, cil -2.50, lente Monofocal Resina Antireflex
- WHEN cost calculation runs
- THEN esfera in stock range, cil -2.50 in 2da serie (-2.25 to -4.00) → lookup returns S/ 18.00

- GIVEN receta OD: esf -7.00, cil -1.00, lente Bifocal FT Resina Simple
- WHEN cost calculation runs
- THEN esfera exceeds ±6.00 → fabricacion → lookup returns S/ 20.00 (fixed)

### R3: Montura Cost Lookup

System SHALL lookup montura cost by material+modelo in `costos_productos` where `stock_o_fabricacion='montura'`. If no rule found, SHALL fallback to `monturas.costo`.

- GIVEN dispensacion with montura Wayfarer Metal
- WHEN cost calculation runs
- THEN lookup returns S/ 80.00 from costos_productos
- AND no rule exists → fallback to monturas.costo

### R4: Biselado Cost Lookup

System SHALL lookup biselado cost in `costos_biselado` by material, tipo_aro, stock_o_fabricacion, serie, alto_indice. Cost SHALL be per pair. If no rule matches, SHALL leave field empty for manual entry.

- GIVEN dispensacion: Resina, Aro Completo, stock, 2da serie, indice 1.50
- WHEN cost calculation runs
- THEN lookup returns matching costo_por_par

### R5: LC Cost Lookup

System SHALL lookup LC cost in `costos_productos` where `tipo_lente IN ('lente_contacto_cosmetico','lente_contacto_medida')` by material+laboratorio. Specs SHALL auto-populate from linked evaluation's LC fields.

- GIVEN LC con medida, Hydrogel, lab Impagurt, evaluacion linkeada
- WHEN cost calculation runs
- THEN lookup returns costo_unitario per box

### R6: Hybrid Override

System SHALL auto-fill `costo_real_od`, `costo_real_oi`, `costo_real_montura`, `costo_real_biselado` from matrix lookup. Optician MAY manually edit each field. Manual value SHALL persist even if matrix changes later.

- GIVEN matrix returns S/ 18.00 for OD
- WHEN optician edits campo costo_real_od to S/ 20.00
- THEN costo_total reflects S/ 20.00 for OD
- AND override is preserved on save

### R7: evaluacion_id Link

System SHALL support nullable `evaluacion_id` FK on `dispensaciones`. When creating dispensacion, SHALL default to patient's latest evaluation. Optician MAY select a different one or none.

- GIVEN patient with 3 evaluations (Jan, Mar, Jul 2026)
- WHEN creating new dispensacion
- THEN evaluacion_id dropdown preselects July 2026

### R8: Prisma Auto-Populate

System SHALL auto-populate prisma values from linked evaluation (`prisma_od/oi_valor`, `prisma_od/oi_base`). Prisma SHALL NOT be duplicated in dispensacion_items — SHALL be read-only from evaluation.

- GIVEN dispensacion with evaluacion_id linked
- WHEN evaluation has prisma_od_valor=2, prisma_od_base='temporal'
- THEN form shows prisma OD: 2Δ base temporal (read-only)
