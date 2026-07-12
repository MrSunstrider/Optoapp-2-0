# Delta for analisis-negocio

## MODIFIED Requirements

### R6: Supabase `costos_productos` Table

System SHALL REPLACE `costos_productos` with matrix schema:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | `PRIMARY KEY` |
| `optica_id` | `TEXT` | `NOT NULL` |
| `material` | `TEXT` | `NOT NULL` |
| `tipo_lente` | `TEXT` | `NOT NULL` |
| `stock_o_fabricacion` | `TEXT` | `NOT NULL`, `CHECK IN ('stock','fabricacion','montura')` |
| `tratamiento` | `TEXT` | nullable |
| `serie` | `INTEGER` | nullable — 1/2/3 or null for fixed-price |
| `costo_unitario` | `NUMERIC` | `NOT NULL` |
| `laboratorio_id` | `TEXT` | nullable |
| `vigente_desde` | `DATE` | `NOT NULL` |
| `vigente_hasta` | `DATE` | nullable |

Index: `CREATE INDEX idx_costos_productos_lookup ON costos_productos(optica_id, material, tipo_lente, stock_o_fabricacion, serie) WHERE vigente_hasta IS NULL`.

(Previously: flat schema with categoria_producto_id, producto_descripcion, costo_unitario)

#### R6.1: RLS on `costos_productos`

| Policy | Command | Using / With Check |
|--------|---------|--------------------|
| `costos_productos_select` | SELECT | `app_private.is_optica_member(auth.uid(), optica_id)` |
| `costos_productos_insert` | INSERT | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])` |
| `costos_productos_update` | UPDATE | Same as insert |
| `costos_productos_delete` | DELETE | `app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'])` |

#### R6.2: Room Entity for `costos_productos`

`CostoProductoEntity` SHALL exist for offline access. DAO SHALL provide lookup queries by block and series. Entity SHALL participate in download AND upload sync. (Previously: no Room entity — server-side only.)

- GIVEN migration applied
- WHEN table inspected
- THEN `costos_productos` has matrix columns
- AND SELECT policy allows optica members
- AND INSERT/UPDATE allow admin/gerente
