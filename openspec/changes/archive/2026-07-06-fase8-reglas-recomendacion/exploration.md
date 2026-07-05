# Exploration: Fase 8 — 6 Reglas de Recomendación

## What we explored

### 1. Domain models from Fase 7 (inputs to the recommendation engine)

**AnalisisMensual** (domain/AnalisisMensual.kt) — 10 fields, all usable for recommendations:

| Field | Type | Used by rule |
|-------|------|-------------|
| `ventasMes` | Double | R5, R6 |
| `cobrosMes` | Double | — (context) |
| `margenNetoPct` | Double | — (fallback) |
| `margenPorCategoria` | List\<MargenCategoria\> | **R2**, **R4** |
| `deudores` | DeudoresResumen (cantidad, saldoTotal) | R1 (summary) |
| `proyeccionCaja` | ProyeccionCaja? | — |
| `stockEstancado` | List\<StockEstancadoItem\> | **R3** |
| `valorInventario` | Double | — (context) |
| `ventasMesAnterior` | Double | R5 (variation calc) |
| `variacionVentasPct` | Double? | **R5** |
| `esOffline` | Boolean | — |

**Deudor** (domain/Deudor.kt) — individual debtor from `ObtenerDeudoresUseCase`:

```
pacienteNombre, pacienteTelefono, ventaId, ventaFecha,
montoTotal, totalPagado, saldo, diasDeuda
```
Directly usable for R1 (per-client debt details).

### 2. ConfiguracionFinancieraEntity — thresholds for all rules

File: `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt`

| Column | Default | Used by |
|--------|---------|---------|
| `deudaViejaAlertaDias` | 30 | R1 |
| `deudaTotalAlertaMonto` | 3000.0 | R1 |
| `caidaVentasAlertaPct` | 10.0 | R5 |
| `stockEstancadoAlertaDias` | 180 | R3 |
| `minVentasParaRecomendar` | 5 | R2, R4 |
| `margenNetoObjetivo` | 15.0 | — (not used by these 6 rules) |

Notable: there is NO `gastosAlertaVentasPct` column — R6's 40% threshold is hardcoded per the plan. No changes needed to the entity.

### 3. Montura / Stock data — for R3

**Montura** entity (`data/dispensacion/DispensacionEntity.kt`): `id`, `sku`, `marca`, `modelo`, `costo`, `precio`, `stockActual`, `stockMinimo`, `categoria`, `activo`.

**StockEstancadoItem** (nested inside AnalisisMensual): `monturaId`, `sku`, `modelo`, `costo`, `stockActual`, `ultimaVenta`, `diasSinVenta`. Already has everything needed for R3 (filter by `diasSinVenta > stockEstancadoAlertaDias`, display modelo + costo).

### 4. Gastos operativos — for R6

The Supabase RPC `rpc_analisis_mensual` (in `20260706000000_fase7_rpc_indicadores.sql`) **already computes** `gastos_mes`:

```sql
SELECT COALESCE(SUM(monto), 0) INTO v_gastos_mes
FROM public.gastos_operativos
WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month';
```

And includes it in the JSONB response (line 83: `'gastos_mes', v_gastos_mes`). However, **AnalisisMensual.kt does NOT parse this field** — it's missing from the domain model. We need to add `gastosMes: Double` to AnalisisMensual and parse it in `fromJson`.

### 5. UseCase pattern established in Fase 7

Both `ObtenerAnalisisMensualUseCase` and `ObtenerDeudoresUseCase` follow:
- `open class ... @Inject constructor(...)` — `open` for test mocking
- `suspend operator fun invoke(...): Resource<T>` — returns `Resource.Success` or `Resource.Error`
- Direct DAO injection (no OptoRepository) for analytical UseCases
- `internal open suspend fun callRpc(...)` — overridable in tests

### 6. Key discovery: MargenCategoria lacks unidadesVendidas

`MargenCategoria` has only `categoria`, `ventas` (monetary), `costos`, `margenPct`. The plan's R2/R4 examples show counts ("Vendiste 8 monturas económicas"), and the config uses `minVentasParaRecomendar` (an integer count). Without adding `unidadesVendidas` to MargenCategoria, we cannot precisely check the count threshold. Two options:
  1. **(Recommended)** Add `unidadesVendidas: Int = 0` to MargenCategoria + update Supabase RPC to include per-category counts.
  2. Use a monetary proxy: check `ventas > 0` instead of count. Simpler but less precise.

### 7. Dependencies identified

- **Fase 7 outputs**: `AnalisisMensual`, `List<Deudor>`, `ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`
- **ConfiguracionFinancieraDao**: already injected via `@Inject` pattern
- **Supabase RPC update**: Minor — add `unidades_vendidas` per category to `rpc_analisis_mensual` +
  add `gastos_mes` parsing to domain model (already in RPC output, just missing from Kotlin side)
- **No new RPCs needed**: All rules evaluate locally from existing data

### 8. No Room migration required

The `configuracion_financiera` table already has all columns needed. No schema changes for Fase 8.
