# Proposal: Costos y Gastos

## Intent

Add complete cost-of-goods tracking for everything sold in a dispensacion: lenses (OD + OI, per eye) and frames (per unit). The lab charges per eye based on **cylinder series** (1ra/2da/3ra) for stock lenses; fabrication items and frames have fixed prices. Mode: **hybrid** — matrix auto-estimates from linked refraction, optician overrides per order.

```
costo_dispensacion = costo_items_oftalmicos + costo_items_lc

Donde cada item oftálmico = costo_OD + costo_OI + costo_montura + costo_biselado
Cada item LC = costo_lc (por caja)
```

## Cost Model

### Lentes — Series por CILINDRO

Esfera dentro de ±6.00 = stock (buscar serie por cilindro). Esfera fuera de ±6.00 = fabricación (precio fijo).

| Serie | Cilindro | Stock Monofocal Simple (por ojo) |
|-------|----------|----------------------------------|
| 1ra | 0 a -2.00 | S/ 5.00 |
| 2da | -2.25 a -4.00 | S/ 12.00 |
| 3ra | -4.25 a -6.00 | S/ 24.00 |
| Fabricación | esfera >|±6.00| | S/ 20.00 (Bifocal FT Simple) |

La lista del laboratorio tiene 5 bloques independientes:

| # | Bloque | Filas | Columnas |
|---|--------|-------|----------|
| 1 | Stock Monofocal Resina | 3 series (1ra/2da/3ra) | 10 tratamientos |
| 2 | Stock Bifocal Resina FT/Inv | 2 tipos | 4 tratamientos |
| 3 | Stock Multifocal Resina | 1 tipo | 5 tratamientos |
| 4 | Fabricación Resina Bifocal/Multifocal | 3 tipos | 7 tratamientos |
| 5 | Fabricación Cristal Bifocal/Multifocal | 2 tipos | 3 tratamientos |

### Monturas — Precio fijo por modelo

Sin series ni tratamientos. Costo fijo por unidad (no por ojo).

| Material | Modelo | Costo por unidad |
|----------|--------|-----------------|
| Metal | Wayfarer | S/ 80.00 |
| Acetato | Vw | S/ 18.00 |

Si no hay regla en `costos_productos`, fallback a `monturas.costo`.

### Biselado — Costo de servicio por par

El biselado (corte y montaje del lente a la montura) se cobra aparte. Depende de:

| Variable | Ejemplo |
|----------|---------|
| Material | Cristal es más caro de biselar que resina |
| Tipo de aro | Aro completo, ranurado, al aire, taladro |
| Stock vs fabricación | Stock viene pre-calibrado; fabricación no |
| Serie | Mayor cilindro = lente más grueso = más trabajo |
| Alto índice | Material más duro |
| Proveedor | Laboratorio externo o taller propio |

Se cobra **por par** (ambos ojos). Tabla separada: `costos_biselado`.

### Lentes de Contacto — Cosmético y con medida

Sin series. Costo por caja/unidad. Las especificaciones (radio_base, diametro) vienen de la evaluación linkeada (`lc_radio_base`, `lc_diametro`, `lc_tipo_lente`, `lc_material`, `lc_laboratorio`).

| Tipo | Material | Laboratorio | Costo por caja |
|------|----------|-------------|---------------|
| Cosmético | Hidrogel | Impagurt | S/ 25.00 |
| Con medida | Silicon | Impagurt | S/ 45.00 |

Si no hay evaluación linkeada (LC cosmético sin receta), el optico ingresa tipo+material manualmente. El sistema igual busca el costo en la matriz.

## Scope

### In Scope

1. **`dispensaciones`**: + `evaluacion_id TEXT` FK nullable. Default: última evaluación del paciente.
2. **`dispensacion_items`**: + `alto_indice`, `reduccion_diametro`, `lenticular`, `curva_base` (0-2-4-6-8). Costos: `costo_real_od`, `costo_real_oi`, `costo_real_montura`, `costo_real_biselado` (items oftálmicos), `costo_real_lc` (items LC). Prisma se jala de la evaluación linkeada. Campos de costo auto-calculados, editables solo desde CostosYGastosScreen.
3. **`costos_productos` restructure**: `material`, `tipo_lente` (monofocal|bifocal_ft|...|montura), `stock_o_fabricacion` (stock|fabricacion|montura), `tratamiento`, `serie` (1|2|3|null), `costo_unitario` (por ojo para lentes, por unidad para monturas), `laboratorio_id`, `vigente_desde`, `vigente_hasta`
4. **`costos_biselado` new table**: `material`, `tipo_aro` (aro_completo|ranurado|al_aire|taladro), `stock_o_fabricacion`, `serie`, `alto_indice`, `costo_por_par`, `proveedor`, `vigente_desde`, `vigente_hasta`
5. **Cost calculation logic**: Leer receta del `evaluacion_id` → ¿esfera en ±6.00? → Sí: stock, serie por cilindro → lookup costo. No: fabricación → lookup fijo. Repetir OD/OI. Montura: lookup por material+modelo o fallback `monturas.costo`. Biselado: lookup en `costos_biselado` por material+tipo_aro+stock/fab+serie+alto_indice. LC: lookup por tipo_lente+material en `costos_productos`, specs de la evaluación linkeada.
6. **Costos visibles en pantalla aparte**: El formulario de dispensación muestra items colapsados (2 líneas: tipo + specs). Botón `[Gestionar costos →]` abre `CostosYGastosScreen` filtrado para esa orden. Ahí el optico ve el desglose con matriz aplicada y sobrescribe.
7. **"Costos y Gastos" screen**: Tab 1 — matriz de costos con 8 bloques + vista de costos por dispensación. Tab 2 — gastos operativos.
8. **Supabase + Room migrations**: Nueva migración SQL + bump versión Room + sync `costos_productos` y `costos_biselado`.

### Out of Scope
- RPC recalcula márgenes con costos reales (deferred)
- UI multi-laboratorio (DB ya soporta `laboratorio_id`)
- Web companion
- Backfill `evaluacion_id` en órdenes existentes (nullable, solo nuevas)

## Capabilities

### New
- `costos-productos`: Gestión de matriz de costos por bloque (8 bloques: lentes stock, fabricación, monturas, biselado, lentes de contacto). Auto-estimado desde refracción + inventario. Override manual por orden. Desglose por ojo + montura + biselado.

### Modified
- `analisis-negocio`: R6 (`costos_productos`) reestructurado con schema matricial.
- `sync`: Nueva entidad `costos_productos` y `costos_biselado` en flujo de sync (solo download para biselado).

## Approach

1. **Supabase**: Migración reemplaza `costos_productos` con schema matricial. ALTER TABLE `dispensaciones` + `dispensacion_items`. RLS copiado del patrón `gastos_operativos`.
2. **Android data**: `CostoProductoEntity` + DAO con queries por bloque y lookup por serie. Campos nuevos en `DispensacionItemEntity` y `DispensacionEntity`. Hilt DI.
3. **Cost calculation**: ViewModel lee `evaluacion` → determina stock vs fabricación → serie por cilindro → lookup matriz lentes → auto-fill `costo_real_od/oi`. Montura: lookup por material+modelo o fallback `monturas.costo`. Biselado: lookup en `costos_biselado` por material+tipo_aro+stock/fab+serie+alto_indice.
4. **UI**: `CostosYGastosScreen` bajo drawer financiero. Tab 1: 8 bloques de matriz + vista de costos por dispensación (acceso directo desde formulario). Tab 2: CRUD de gastos. Formulario de dispensación: items colapsados (2 líneas, toggle Oftálmico/LC por item), botón `[Gestionar costos →]`.
5. **Prisma**: Se autocompleta de `evaluaciones.prisma_od/oi_valor+base` al linkear. Curva base se ingresa manual (depende de la montura).

## Files

| File | Action |
|------|--------|
| `supabase/migrations/202607XX_costos_matriz.sql` | New: DROP+CREATE costos_productos, ALTER dispensaciones+items, RLS |
| `data/costoproducto/CostoProductoEntity.kt` | New |
| `data/costoproducto/CostoProductoDao.kt` | New: queries by bloque, lookup by serie |
| `data/dispensacion/DispensacionEntity.kt` | + `evaluacionId` |
| `data/dispensacion/DispensacionItemEntity.kt` | + alto_indice, reduccion_diametro, lenticular, curva_base, costo_real_od/oi/montura/biselado/lc |
| `domain/SyncFinanzasDto.kt` | + CostoProductoRemoto, CostoBiseladoRemoto |
| `domain/SyncFinanzasUseCase.kt` | + download/upload costos_productos, download costos_biselado |
| `ui/screens/CostosYGastosScreen.kt` | New: 2 tabs, 8 bloques + vista costos por orden |
| `viewmodel/CostosYGastosViewModel.kt` | New |
| `ui/screens/NuevaDispensacionScreen.kt` | + evaluacion_id, items colapsados con toggle Oftálmico/LC, botón Gestionar costos |
| `di/DatabaseModule.kt` | + provideCostoProductoDao() |
| `data/OptoDatabase.kt` | + entities, bump version |
| `data/OptoRepository.kt` | + passthrough costos_productos |

## Risks

| Risk | Mitigation |
|------|------------|
| 8 bloques de matriz difíciles en mobile | Dropdown de bloque + filtros inline; vista tabla simple |
| Override diverge de matriz | Mostrar fecha cambio de matriz; resaltar si cambió post-override |
| `monturas.costo` desactualizado vs `costos_productos` | Prioridad: costos_productos > monturas.costo. Mostrar fuente del costo |
| Biselado sin regla para la combinación | Fallback: mostrar campo vacío, optico ingresa manual |

## Rollback

`DROP TABLE costos_productos` (0 filas), `DROP TABLE costos_biselado` (0 filas), recrear schemas viejos. `ALTER TABLE dispensaciones DROP COLUMN evaluacion_id`. `ALTER TABLE dispensacion_items DROP COLUMN` 9 columnas nuevas. Revertir Room version. Sin pérdida de datos. < 1 min.

## Success Criteria

- [x] CostosYGastosScreen con 2 tabs: matriz 8 bloques + gastos operativos
- [x] Formulario dispensación: items colapsados con toggle Oftálmico/LC
- [x] `[Gestionar costos →]` abre CostosYGastosScreen filtrado por orden
- [x] Lookup lentes: devuelve costo correcto por ojo según serie de cilindro
- [x] Lookup monturas: devuelve costo por modelo, fallback a `monturas.costo`
- [x] Lookup biselado: devuelve costo por par según material+tipo_aro+serie
- [x] Lookup LC: devuelve costo por caja según tipo+material+laboratorio
- [x] Optico sobrescribe costos desde CostosYGastosScreen
- [x] Prisma se autocompleta de la evaluación linkeada
- [x] evaluacion_id preselecciona última evaluación
- [x] Tests existentes pasan; nuevos DAOs con tests Room
- [x] Sync costos_productos (up+down) y costos_biselado (solo down) sin errores
