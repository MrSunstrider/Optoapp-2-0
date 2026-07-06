# Parte B — Análisis de Negocio: Completada

## Resumen

La Parte B implementa el módulo de análisis financiero y recomendaciones de negocio para OptoApp, cubriendo las Fases 6 a 10 del plan `propuesta-ingresos-analisis-financiero.md`. El módulo permite a dueños y gerentes de ópticas ver indicadores clave en lenguaje llano, recibir recomendaciones accionables, y consultar deudores — todo con fallback offline.

## Lo construido (Fases 6-10)

### Fase 6 — Esquema de datos
- **Migración SQL** (`20260705000000_fase6_esquema_analisis.sql`, 300 líneas): 6 tablas nuevas + RLS + función `recalcular_resumen_diario()`
  - `categorias_producto`, `costos_productos`, `configuracion_financiera`, `gastos_operativos`, `margen_por_categoria`, `resumen_diario`
- **Entidades Room**: `Venta`, `CategoriaProducto`, `GastoOperativo`, `ResumenDiario`, `ConfiguracionFinanciera`, `FeedbackRecomendacion`
- **DAOs Room**: una por cada entidad, con queries de agregación offline
- **Migraciones Room**: v31→v32, v32→v33, v33→v34 (BD version 34)

### Fase 7 — Motor de 8 indicadores
- **Migración SQL** (`20260706000000_fase7_rpc_indicadores.sql`, 179 líneas):
  - `rpc_analisis_mensual`: computa 8 indicadores (ventas, cobros, costo, margen neto, margen por categoría, deudores, proyección caja, stock estancado, comparación mes anterior)
  - `rpc_deudores`: lista deudores con aging desde `ventas` + `pagos`
  - Fix: grant execute en `recalcular_resumen_diario` y rewrite de `rpc_count_pendientes`
- **UseCases Android**:
  - `ObtenerAnalisisMensualUseCase` — llama al RPC, fallback a Room offline
  - `ObtenerDeudoresUseCase` — llama al RPC, fallback a Room
- **Modelos**: `AnalisisMensual`, `MargenCategoria`, `DeudoresResumen`, `ProyeccionCaja`, `StockEstancadoItem`, `Deudor`

### Fase 8 — 6 reglas de recomendación
- `GenerarRecomendacionesUseCase` (231 líneas): 6 reglas evaluadas y priorizadas:
  1. **COBRAR** (ALTA) — deudores > umbral de monto o aging
  2. **MEJORAR PRECIO** (MEDIA) — margen < umbral configurable
  3. **LIQUIDAR STOCK** (MEDIA) — stock estancado > días
  4. **VENDER MÁS DE** (BAJA) — categorías con alto margen
  5. **ALERTA CAÍDA** (ALTA) — ventas caen > umbral vs mes anterior
  6. **REDUCIR GASTO** (MEDIA) — gastos/ventas > ratio
- `FeedbackRecomendacionUseCase` — persiste feedback en Room (infraestructura lista, UI pendiente)
- `Recomendacion` data class con `Prioridad` (ALTA/MEDIA/BAJA) + `DatosAccion`

### Fase 9 — UI/UX
- `AnalisisNegocioScreen` (328 líneas): pantalla principal "Mi Negocio"
  - `MonthSwitcher` — navegación entre meses
  - `ResumenCard` — "Vendiste", "Cobraste", "Saldo pendiente", "Margen"
  - `RecomendacionCard` — priorizadas, coloreadas por prioridad, con acción concreta
  - Botón "Ver análisis completo" → `AnalisisDetalleScreen`
  - Indicador offline + reintentar
- `AnalisisDetalleScreen` (315 líneas): análisis detallado por secciones expandibles
  - "Plata que entró y salió" — barras de ventas/cobros/costos/gastos/ganancia
  - "Lo que más te deja" — ranking por categoría con % margen
  - "Clientes que te deben" — lista con nombre, saldo, días de deuda
  - "Productos sin vender" — stock estancado con SKU, días sin venta
  - "Plata que vas a tener" — proyección de caja
- `AnalisisNegocioViewModel` (125 líneas): orquesta UseCases, navegación de meses, estado UI
- Navegación: drawer redirige al módulo nuevo, acceso restringido por `canViewBiAndReports`

### Fase 10 — QA y documentación
- **Cobertura JaCoCo**: 8% instrucción (> mínimo 5%) ✅
- **BUILD SUCCESSFUL** — 36 actionable tasks, 0 failures
- **Tests**: 177 archivos, 25,247 líneas totales en el proyecto
  - Tests específicos Parte B: `ObtenerAnalisisMensualUseCaseTest`, `ObtenerDeudoresUseCaseTest`, `GenerarRecomendacionesUseCaseTest`, `FeedbackRecomendacionUseCaseTest`, `RecomendacionTest`, `AnalisisMensualMapperTest`, `AnalisisNegocioViewModelTest`, `AnalisisNegocioScreenTest`, `AnalisisDetalleScreenTest`, `VentaDaoTest`, `CategoriaProductoDaoTest`, `GastoOperativoDaoTest`, `ResumenDiarioDaoTest`, `ConfiguracionFinancieraDaoTest`, `FeedbackRecomendacionDaoTest`, `DispensacionViewModelVentaTest`, `ServiciosViewModelVentaTest` — 17 archivos

## Conteo de archivos y líneas

| Componente | Archivos | Líneas |
|---|---|---|
| Migraciones SQL (Fases 6-7) | 2 (+3 Fase 1) | 479 (+169 Fase 1) |
| Entidades y DAOs Room | 12 | ~450 |
| Modelos de dominio | 3 | ~280 |
| UseCases | 4 | ~352 |
| ViewModels | 1 | 125 |
| Composables (UI) | 2 | 643 |
| Tests (Parte B) | 17 | ~2,900 |
| **Total Parte B** | **41** | **~5,600** |

## Decisiones de arquitectura

1. **Server-first con fallback offline**: los indicadores se calculan en PostgreSQL vía RPC. Si no hay conexión, se usa Room local con datos cacheados de `resumen_diario`.
2. **`margen_por_categoria` y `costos_productos` son server-only**: no se replican en Room. Los márgenes por categoría se reciben del RPC y se muestran en UI sin persistencia local separada.
3. **`recalcular_resumen_diario()` es idempotente** (ON CONFLICT DO UPDATE). Se llama bajo demanda al abrir el análisis, sin pg_cron ni edge functions. Estrategia D del plan.
4. **Recomendaciones locales**: `GenerarRecomendacionesUseCase` evalúa reglas en el dispositivo usando los datos del RPC + `configuracion_financiera` de Room.
5. **Comparación temporal**: `AnalisisMensual` incluye `ventasMesAnterior` y `variacionVentasPct` para la comparación requerida.

## Criterios de aceptación

| # | Criterio | Estado | Nota |
|---|---|---|---|
| 1 | Lenguaje llano: indicadores sin términos financieros | ✅ | "Vendiste", "Cobraste", "Plata que entró y salió", "Lo que más te deja", "Clientes que te deben" |
| 2 | Todo cálculo usa `ventas`, no `dispensaciones`/`servicios_extra` | ✅ | RPCs consultan `resumen_diario` poblado desde `ventas` |
| 3 | Recomendaciones específicas: nombres, montos, acciones | ✅ | `DatosAccion` con pacienteIds, productoIds, montoTotal. Campo `accion` con texto concreto |
| 4 | Recomendaciones priorizadas: ALTA primero, max 5 | ✅ | `sortedWith(compareBy { it.prioridad.ordinal }).take(5)` |
| 5 | Comparación temporal: mes actual vs anterior | ✅ | `ventasMesAnterior` + `variacionVentasPct` en el modelo |
| 6 | Margen histórico usa `costo_unitario_snapshot` | ✅ | `recalcular_resumen_diario()` usa `costo_unitario_snapshot` de `ventas` |
| 7 | Umbrales configurables desde `configuracion_financiera` | ✅ | 9 campos configurables leídos por `GenerarRecomendacionesUseCase` |
| 8 | Modo offline: Room local fallback | ✅ | `ObtenerAnalisisMensualUseCase.fallbackToRoom()`, banner "Datos limitados — sin conexión" |
| 9 | Acceso restringido admin/gerente | ✅ | `canViewBiAndReports()` + lock screen |
| 10 | Feedback loop: 👍/👎 en cada recomendación | ⚠️ | Infraestructura lista (DAO, Entity, UseCase). UI de feedback no implementada todavía |
| 11 | Proyección de caja advierte si histórico insuficiente | ⚠️ | Proyección se muestra pero sin advertencia explícita de estacionalidad |

## Diferido a futuro

- `ProyectarFlujoCajaUseCase`: mencionado en el plan pero no implementado. La proyección de caja viene del RPC como dato, pero no hay un use case separado para análisis avanzado de flujo.
- **UI de feedback (👍/👎)**: la infraestructura de datos está lista pero los botones de feedback en la UI no se implementaron. Queda como tarea pendiente para una fase futura.
- **Widget de acceso rápido** (Fase 9.3 del plan): diferido explícitamente en el plan de riesgos.
- **Margen como "de cada S/ 100, te quedan S/ X"**: actualmente se muestra como porcentaje. La redacción específica del criterio de aceptación podría requerir ajuste fino de UI.
- **Advertencia de estacionalidad en proyección**: no implementada. La proyección se muestra sin validación de suficiencia de datos históricos.

## Archivos relevantes

### Migraciones
- `supabase/migrations/20260704000000_create_ventas_table.sql` — tabla `ventas` (Fase 1)
- `supabase/migrations/20260704000001_backfill_ventas_and_pagos.sql` — backfill + RLS (Fase 1)
- `supabase/migrations/20260704000002_triggers_ventas.sql` — triggers inserción (Fase 1)
- `supabase/migrations/20260705000000_fase6_esquema_analisis.sql` — 6 tablas + RLS + `recalcular_resumen_diario`
- `supabase/migrations/20260706000000_fase7_rpc_indicadores.sql` — `rpc_analisis_mensual`, `rpc_deudores`

### Room (data layer)
- `optoapp/src/main/java/com/example/optoapp/data/venta/Venta.kt` + `VentaDao.kt`
- `optoapp/src/main/java/com/example/optoapp/data/categoriaproducto/` — entity + DAO
- `optoapp/src/main/java/com/example/optoapp/data/gastooperativo/` — entity + DAO
- `optoapp/src/main/java/com/example/optoapp/data/resumendiario/` — entity + DAO
- `optoapp/src/main/java/com/example/optoapp/data/configuracionfinanciera/` — entity + DAO
- `optoapp/src/main/java/com/example/optoapp/data/feedbackrecomendacion/` — entity + DAO
- `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt` — version 34, migraciones v31→v34

### Domain (use cases + models)
- `optoapp/src/main/java/com/example/optoapp/domain/AnalisisMensual.kt` — modelo + parsing JSON
- `optoapp/src/main/java/com/example/optoapp/domain/Recomendacion.kt` — modelo + enums
- `optoapp/src/main/java/com/example/optoapp/domain/Deudor.kt` — modelo deudor
- `optoapp/src/main/java/com/example/optoapp/domain/ObtenerAnalisisMensualUseCase.kt`
- `optoapp/src/main/java/com/example/optoapp/domain/ObtenerDeudoresUseCase.kt`
- `optoapp/src/main/java/com/example/optoapp/domain/GenerarRecomendacionesUseCase.kt`
- `optoapp/src/main/java/com/example/optoapp/domain/FeedbackRecomendacionUseCase.kt`

### Presentation (UI + ViewModel)
- `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt`
- `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisDetalleScreen.kt`
- `optoapp/src/main/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModel.kt`

### Tests
- `optoapp/src/test/java/com/example/optoapp/domain/ObtenerAnalisisMensualUseCaseTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/ObtenerDeudoresUseCaseTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/GenerarRecomendacionesUseCaseTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/FeedbackRecomendacionUseCaseTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/RecomendacionTest.kt`
- `optoapp/src/test/java/com/example/optoapp/domain/AnalisisMensualMapperTest.kt`
- `optoapp/src/test/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModelTest.kt`
- `optoapp/src/test/java/com/example/optoapp/ui/AnalisisNegocioScreenTest.kt`
- `optoapp/src/test/java/com/example/optoapp/ui/AnalisisDetalleScreenTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/VentaDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/categoriaproducto/CategoriaProductoDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/gastooperativo/GastoOperativoDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/resumendiario/ResumenDiarioDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/configuracionfinanciera/ConfiguracionFinancieraDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/data/feedbackrecomendacion/FeedbackRecomendacionDaoTest.kt`
- `optoapp/src/test/java/com/example/optoapp/viewmodel/DispensacionViewModelVentaTest.kt`
- `optoapp/src/test/java/com/example/optoapp/viewmodel/ServiciosViewModelVentaTest.kt`

---

**Fecha**: 2026-07-05
**Build**: SUCCESSFUL — 36 tasks, 0 failures
**Cobertura JaCoCo**: 8% instrucción (mínimo 5%)
