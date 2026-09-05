# Exploration — montura-tipo-aro-stock-variants

## Problem

Optical shops stock the same frame SKU as multiple rim types (Aro Completo, Semi al aire, Al aire) with independent counts. Today `UNIQUE (optica_id, sku)` forces one stock pool per SKU.

## Current state

- One `monturas` row = one `tipoAro` + one `stockActual`.
- Create form: single dropdown + single stock inicial.
- `MonturaSearchField` used by dispensación and servicios extra; does not show `tipoAro`.
- Movements keyed by `monturaId` (variant-ready if rows split).

## Chosen approach

Same SKU, uniqueness `(optica, sku, tipo_aro)`; multi-select chips + per-type initial stock on create; search shows tipo + stock.

## Risks

- Schema migration on Room + Supabase; rollback only if no duplicate SKUs.
- Empty `tipoAro` for accessories keeps single-SKU uniqueness.
- Edit remains single-row.
