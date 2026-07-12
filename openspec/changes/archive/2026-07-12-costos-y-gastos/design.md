# Design: Costos y Gastos

## Technical Approach

Replace flat `costos_productos` with a matrix schema for per-eye/unit/pair cost lookup. Add `costos_biselado` table. Modify `dispensaciones` and `dispensacion_items` with nullable cost and spec columns. Add Room entities, DAOs, and sync DTOs. New `CostosYGastosScreen` (2 tabs) under financial drawer.

## Architecture Decisions

### D1: Supabase Migration — Idempotent DDL

**SQL**: One file `20260712000001_costos_matriz.sql`.

```sql
-- DROP old schema (safe: 0 rows, no RPCs reference it)
DROP TABLE IF EXISTS public.costos_productos CASCADE;

-- Recreate with matrix columns
CREATE TABLE IF NOT EXISTS public.costos_productos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id TEXT NOT NULL REFERENCES public.opticas(id),
    material TEXT NOT NULL,
    tipo_lente TEXT NOT NULL,
    stock_o_fabricacion TEXT NOT NULL CHECK (stock_o_fabricacion IN ('stock','fabricacion','montura')),
    tratamiento TEXT,
    serie INTEGER,
    costo_unitario NUMERIC NOT NULL,
    laboratorio_id TEXT,
    vigente_desde DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta DATE
);
CREATE INDEX IF NOT EXISTS idx_costos_productos_lookup
    ON public.costos_productos (optica_id, material, tipo_lente, stock_o_fabricacion, serie)
    WHERE vigente_hasta IS NULL;

-- New costos_biselado table
CREATE TABLE IF NOT EXISTS public.costos_biselado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id TEXT NOT NULL REFERENCES public.opticas(id),
    material TEXT NOT NULL,
    tipo_aro TEXT NOT NULL CHECK (tipo_aro IN ('aro_completo','ranurado','al_aire','taladro')),
    stock_o_fabricacion TEXT NOT NULL CHECK (stock_o_fabricacion IN ('stock','fabricacion')),
    serie INTEGER,
    alto_indice TEXT,
    costo_por_par NUMERIC NOT NULL,
    proveedor TEXT,
    vigente_desde DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta DATE
);

-- ALTER dispensaciones + dispensacion_items
ALTER TABLE public.dispensaciones ADD COLUMN IF NOT EXISTS evaluacion_id TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS alto_indice TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS reduccion_diametro TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS lenticular TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS curva_base TEXT;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_od NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_oi NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_montura NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_biselado NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_lc NUMERIC;
```

RLS policies follow exact pattern from `gastos_operativos` — `is_optica_member` for SELECT, `has_optica_role(ARRAY['admin','gerente'])` for INSERT/UPDATE, `has_optica_role(ARRAY['admin'])` for DELETE.

### D2: Room Migration 38→39

Pattern from `MIGRATION_37_38` — raw `db.execSQL` in a `Migration(38, 39)` object. Same SQL as Supabase but with Room column naming (camelCase → `ALTER TABLE` targets the actual Room column name).

### D3: Entities

```
CostoProductoEntity(@Entity tableName="costos_productos")
  id: String (PK), opticaId: String, material: String, tipoLente: String,
  stockOFabricacion: String, tratamiento: String?, serie: Int?,
  costoUnitario: Double, laboratorioId: String?,
  vigenteDesde: String, vigenteHasta: String?

CostoBiseladoEntity(@Entity tableName="costos_biselado")
  id: String (PK), opticaId: String, material: String, tipoAro: String,
  stockOFabricacion: String, serie: Int?, altoIndice: String?,
  costoPorPar: Double, proveedor: String?,
  vigenteDesde: String, vigenteHasta: String?

DispensacionOptica: + evaluacionId: String? (nullable)
DispensacionItem: + altoIndice, reduccionDiametro, lenticular, curvaBase,
                    costoRealOd, costoRealOi, costoRealMontura,
                    costoRealBiselado, costoRealLc (all nullable, Double for costs)
```

### D4: DAOs

```kotlin
@Dao interface CostoProductoDao {
    @Query("SELECT * FROM costos_productos WHERE material=:m AND tipoLente=:t AND stockOFabricacion=:s AND tratamiento=:tr AND serie=:se AND vigenteHasta IS NULL")
    suspend fun lookup(m: String, t: String, s: String, tr: String?, se: Int?): CostoProductoEntity?

    @Query("SELECT * FROM costos_productos WHERE opticaId=:o AND stockOFabricacion=:b AND vigenteHasta IS NULL ORDER BY material, tipoLente")
    suspend fun getByBloque(opticaId: String, bloque: String): List<CostoProductoEntity>

    @Upsert suspend fun upsertAll(entities: List<CostoProductoEntity>)
}
```

`CostoBiseladoDao` mirrors with `lookup(material, tipoAro, stockOFabricacion, serie, altoIndice)`.

### D5: Sync Integration

Insert `costos_productos` in download/upload order: after ventas → **costos_productos** → pagos. `costos_biselado` is download-only.

```kotlin
@Serializable data class CostoProductoRemoto(
    val id: String, @SerialName("optica_id") val opticaId: String,
    val material: String, @SerialName("tipo_lente") val tipoLente: String,
    @SerialName("stock_o_fabricacion") val stockOFabricacion: String,
    val tratamiento: String? = null, val serie: Int? = null,
    @SerialName("costo_unitario") val costoUnitario: Double,
    @SerialName("laboratorio_id") val laboratorioId: String? = null,
    @SerialName("vigente_desde") val vigenteDesde: String,
    @SerialName("vigente_hasta") val vigenteHasta: String? = null
)
```

## Data Flow

```
NuevaDispensacionScreen
  │ selecciona paciente → última evaluación (evaluacion_id preselect)
  │ ingresa items → toggle Oftálmico/LC
  ▼
DispensacionViewModel
  │ link evaluacion_id → lee receta (esfera, cilindro)
  │ stock? |esf|≤6 → serie por cilindro → lookup CostoProductoDao
  │ fabricación? → lookup CostoProductoDao (serie=null)
  │ montura? → lookup CostoProductoDao fallback Montura.costo
  ▼
Costos dispensacion auto-fill → colapsa items (2 líneas)
  └─ botón [Gestionar costos →] → CostosYGastosScreen (filtered by orden)
       Tab 1: 8 bloques matriz + override manual
       Tab 2: CRUD gastos operativos
```

## File Changes

| File | Action |
|------|--------|
| `supabase/migrations/20260712000001_costos_matriz.sql` | Create |
| `data/costoproducto/CostoProductoEntity.kt` | Create |
| `data/costoproducto/CostoProductoDao.kt` | Create |
| `data/costobiselado/CostoBiseladoEntity.kt` | Create |
| `data/costobiselado/CostoBiseladoDao.kt` | Create |
| `data/dispensacion/DispensacionEntity.kt` | Modify – add `evaluacionId` |
| `data/dispensacion/DispensacionItemEntity.kt` | Modify – add 9 cost/spec fields |
| `data/OptoDatabase.kt` | Modify – bump v38→39, add entities & DAOs |
| `data/OptoDatabaseMigrations.kt` | Modify – add MIGRATION_38_39 |
| `data/OptoRepository.kt` | Modify – passthrough costos DAOs |
| `domain/SyncFinanzasDto.kt` | Modify – add CostoProductoRemoto, CostoBiseladoRemoto |
| `domain/SyncFinanzasUseCase.kt` | Modify – add costos_productos/biselado sync |
| `di/DatabaseModule.kt` | Modify – add provideCostoProducto/BiseladoDao |
| `viewmodel/CostosYGastosViewModel.kt` | Create |
| `ui/screens/CostosYGastosScreen.kt` | Create |
| `ui/screens/NuevaDispensacionScreen.kt` | Modify – evaluacion_id, toggle, Gestionar costos |

Total: **5 new**, **9 modified**, **0 deleted**.

## Testing Strategy (TDD Order)

| # | Layer | RED Test | What it Proves |
|---|-------|----------|----------------|
| 1 | Migration | `Migration38_39Test` – table exists, columns correct, data preserved | Room migration is safe |
| 2 | DAO | `CostoProductoDaoTest` – lookup by (mat,tipo,stock,trat,serie), upsert, getByBloque | Lookup returns correct per-eye cost |
| 3 | DAO | `CostoBiseladoDaoTest` – lookup by (mat,tipoAro,stock,serie,altoIndice), empty fallback | Biselado lookup works |
| 4 | Sync | `SyncFinanzasCostosTest` – CostoProductoRemoto serialization, download order after ventas | DTO mapping + order correct |
| 5 | UI | `CostosYGastosScreenTest` – 2 tabs render, block dropdown works | UI structure correct |
| 6 | Integration | `DispensacionViewModelCostosTest` – auto-fill from evaluacion, override persists | Hybrid cost calc correct |
