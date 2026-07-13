# Exploración: Completar diferidos del módulo financiero (Parte B)

**Fecha**: 2026-07-12
**Propósito**: Investigar el estado actual de 5 ítems diferidos del módulo de análisis financiero y recomendar abordaje.

---

## 1. Advertencia de estacionalidad en proyección de caja

### Estado actual

La proyección de caja (`ProyeccionCard` en `AnalisisDetalleScreen.kt`) se muestra sin advertencia de suficiencia de datos:

```kotlin
// AnalisisDetalleScreen.kt:316-332
private fun ProyeccionCard(proyeccion: ProyeccionCaja) {
    // Muestra ingresos, egresos, saldo neto — sin advertencia
}
```

El RPC `rpc_analisis_mensual` **no** retorna un campo `mesesHistoricos`. Solo retorna `ventas_mes_anterior` (un solo mes atrás):

```sql
-- 20260706000000_fase7_rpc_indicadores.sql:79-92
RETURN jsonb_build_object(
    'ventas_mes_anterior', v_ventas_mes_anterior,
    'variacion_ventas_pct', CASE WHEN v_ventas_mes_anterior > 0 THEN ... ELSE NULL END
);
```

El modelo `ProyeccionCaja` no tiene campo para meses de histórico:

```kotlin
// AnalisisMensual.kt:125-129
data class ProyeccionCaja(
    val ingresosEsperados: Double,
    val egresosProgramados: Double,
    val saldoNeto: Double
)
```

**Sin embargo**, ya hay infraestructura parcial:
- `AnalisisNegocioViewModel.kt:136` tiene `mostrarAdvertenciaEstacionalidad` que se activa cuando `ventasMesAnterior == 0.0`
- `AnalisisNegocioScreen.kt:135-153` muestra una tarjeta de advertencia en la pantalla principal

**Problemas con la implementación actual**:
1. La condición `ventasMesAnterior == 0.0` es un proxy pobre — no mide profundidad histórica real
2. La advertencia aparece en `AnalisisNegocioScreen`, no en `AnalisisDetalleScreen` junto a la `ProyeccionCard`
3. No hay campo `mesesHistoricos` en el RPC ni en el modelo

### Impacto

**Bajo**. Ya hay advertencia en la pantalla principal, pero no es precisa ni está donde debería (junto a la proyección). Usuarios nuevos verán la proyección sin contexto sobre su fiabilidad.

### Enfoque recomendado

| Paso | Descripción | Archivos afectados |
|------|-------------|-------------------|
| 1 | Agregar campo `meses_historicos` a `rpc_analisis_mensual` contando meses con datos en `resumen_diario` | `20260706000000_fase7_rpc_indicadores.sql` |
| 2 | Agregar campo `mesesHistoricos: Int` a `ProyeccionCaja` + parseo en `AnalisisMensual.fromJson()` | `AnalisisMensual.kt` |
| 3 | Propagar a `ProyeccionCard` o agregar advertencia dentro de la card en `AnalisisDetalleScreen` | `AnalisisDetalleScreen.kt` |
| 4 | Mejorar la lógica de `mostrarAdvertenciaEstacionalidad` para usar `mesesHistoricos < 3` | `AnalisisNegocioViewModel.kt` |
| 5 | Actualizar tests | `AnalisisNegocioViewModelTest.kt`, `AnalisisMensualMapperTest.kt` |

**Esfuerzo**: Chico (1 sesión)

---

## 2. Redacción margen "de cada S/100 te quedan S/X"

### Estado actual

**YA IMPLEMENTADO**. En `AnalisisNegocioScreen.kt:452`:

```kotlin
"De cada S/ 100 que vendés, te quedan S/ ${Math.round(analisis.margenNetoPct)} (margen neto)"
```

La redacción exacta que pedía el criterio de aceptación existe y funciona. El PARTE-B-COMPLETA.md dice:
> "actualmente se muestra como porcentaje '35%', no como 'de cada S/100, te quedan S/35'"

Pero eso está desactualizado — ya se implementó con la redacción correcta.

### Impacto

**Nulo**. Está listo. Solo falta marcar como ✅ en la documentación.

### Enfoque recomendado

No requiere código. Solo actualizar `PARTE-B-COMPLETA.md`.

**Esfuerzo**: Trivial (minutos)

---

## 3. RPC recalcula márgenes con costos reales

### Estado actual

`recalcular_resumen_diario()` usa `costo_unitario_snapshot` de `ventas`:

```sql
-- 20260705000000_fase6_esquema_analisis.sql:300
SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto_total), 0), COALESCE(SUM(costo_unitario_snapshot), 0)
FROM public.ventas
WHERE optica_id = p_optica_id AND fecha = p_fecha;
```

La migración `20260712000001_costos_matriz.sql` agrega 5 columnas a `dispensacion_items`:

```sql
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_od NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_oi NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_montura NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_biselado NUMERIC;
ALTER TABLE public.dispensacion_items ADD COLUMN IF NOT EXISTS costo_real_lc NUMERIC;
```

**Problema**: Las ventas pueden ser de dos tipos: dispensaciones (tienen `dispensacion_id` → `dispensacion_items`) y servicios extra (tienen `servicio_extra_id` → sin items). El RPC actual suma `costo_unitario_snapshot` de `ventas`, que fue poblado al momento de crear la venta y no refleja actualizaciones de la matriz de costos.

### Impacto

**Medio**. El cálculo de márgenes usa costos snapshot que pueden estar desactualizados respecto a la matriz de costos. Afecta `rpc_analisis_mensual` (que lee de `resumen_diario`), y por tanto los indicadores de margen, recomendaciones de precio, y proyección.

### Enfoque recomendado

Modificar `recalcular_resumen_diario()` para que calcule el costo real desde `dispensacion_items` cuando existan:

```sql
-- Nuevo: costo real desde dispensacion_items (más preciso)
WITH costos_dispensacion AS (
    SELECT COALESCE(SUM(
        COALESCE(di.costo_real_od, 0) +
        COALESCE(di.costo_real_oi, 0) +
        COALESCE(di.costo_real_montura, 0) +
        COALESCE(di.costo_real_biselado, 0) +
        COALESCE(di.costo_real_lc, 0)
    ), 0) AS total
    FROM public.ventas v
    JOIN public.dispensaciones d ON d.id = v.dispensacion_id
    JOIN public.dispensacion_items di ON di.dispensacion_id = d.id
    WHERE v.optica_id = p_optica_id AND v.fecha = p_fecha
)
```

**Complejidades**:
1. No todas las ventas tienen `dispensacion_id` (servicios extra tienen `servicio_extra_id`)
2. Para servicios extra, no hay `dispensacion_items` — habría que usar `costo_unitario_snapshot` como fallback o determinar el costo del servicio de otra forma
3. Esquema relacional: `ventas.dispensacion_id → dispensaciones.id → dispensacion_items.dispensacion_id`

**Estrategia**: Sumar costos de `dispensacion_items` para dispensaciones + `costo_unitario_snapshot` para servicios extra (o cero si no hay dato). El total del mes es la suma de ambos.

| Paso | Descripción | Archivos afectados |
|------|-------------|-------------------|
| 1 | Modificar `recalcular_resumen_diario()` para sumar costos reales desde `dispensacion_items` | `20260705000000_fase6_esquema_analisis.sql` (nueva migración) |
| 2 | Regenerar `rpc_analisis_mensual` (no toca lógica, solo lee de `resumen_diario` actualizado) | -- |
| 3 | Tests de integración del RPC | Nuevo test SQL o verificación manual |

**Esfuerzo**: Mediano (2 sesiones — requiere migración nueva con cuidado por el ON CONFLICT DO UPDATE)

---

## 4. Widget de acceso rápido (Fase 9.3)

### Estado actual

**No existe ningún código de widget**. Búsqueda confirmó:
- No hay `AppWidgetProvider` ni clases relacionadas
- No hay declaración `<receiver>` en `AndroidManifest.xml`
- No hay layouts XML para widget
- No hay referencias a Glance (Jetpack Glance para widgets con Compose)
- No hay referencias a appwidget en el código fuente

### Impacto

**Nuevo feature completo**. No afecta código existente.

### Enfoque recomendado

Opción A: **Jetpack Glance** (Android 12+ / API 31+)
- Pros: Sintaxis Compose-style, mantenible, moderna
- Cons: Solo funciona en API 31+ (requiere verificar minSdk=24 — fallback a RemoteViews)

Opción B: **RemoteViews tradicional** (todas las API)
- Pros: Compatible con minSdk 24
- Cons: Imperativo, feo, difícil de mantener, sin Compose

**Datos a mostrar**:
- "Hoy: S/ X vendido"
- "Por cobrar: S/ Y"

**Arquitectura necesaria**:
1. `AnalisisWidgetProvider` (AppWidgetProvider)
2. Layout XML `widget_analisis.xml`
3. Declaración en `AndroidManifest.xml` como `<receiver>`
4. Data fetch: Room DAO (`resumen_diario`) o contenido estático si no hay conexión
5. Actualización periódica vía `AlarmManager` o WorkManager

**Consideración de Hilt**: AppWidgetProvider no soporta inyección directa de Hilt. Se necesita un `AppWidgetProvider` con `@AndroidEntryPoint` usando `hilt.android.launcher` artifact, o bien un `BroadcastReceiver` con Hilt.

**Esfuerzo estimado**: 4-5 componentes (provider, layout, update service, manifest, wiring).

### Recomendación

Usar **RemoteViews tradicional** para compatibilidad con minSdk 24, con una capa de servicio (WorkManager) para refrescar datos desde Room cada N minutos.

| Paso | Descripción | Archivos a crear |
|------|-------------|-----------------|
| 1 | Layout XML del widget (`widget_analisis.xml`) | `res/xml/widget_analisis_info.xml`, `res/layout/widget_analisis.xml` |
| 2 | `AnalisisWidgetProvider` (AppWidgetProvider) | Nuevo archivo .kt |
| 3 | Worker para refrescar datos desde Room | Nuevo archivo .kt o usar SyncWorker existente |
| 4 | Declaración en AndroidManifest | `AndroidManifest.xml` — agregar `<receiver>` |
| 5 | Tests | Nuevos archivos de test |

**Esfuerzo**: Grande (3-4 sesiones)

---

## 5. Doc PARTE-B-COMPLETA.md desactualizado

### Estado actual

El documento `openspec/PARTE-B-COMPLETA.md` tiene dos inexactitudes:

1. **Línea 93**: `| 10 | Feedback loop: 👍/👎 en cada recomendación | ⚠️ | Infraestructura lista... UI no implementada todavía |`
   - Realidad: `AnalisisNegocioScreen.kt:568-611` ya tiene `RecomendacionCard` con botones Útil/No me sirve y estado "Gracias por tu valoración"
   - El feedback está implementado y funcional

2. **Línea 99**: `- **UI de feedback (👍/👎)**: ... botones de feedback en la UI no se implementaron`
   - Realidad: Ya se implementó

### Impacto

**Bajo**. Solo documentación. No afecta código.

### Enfoque recomendado

| Cambio | Línea | Valor actual | Valor nuevo |
|--------|-------|-------------|-------------|
| Criterio 10 | 93 | `⚠️` con nota "UI no implementada" | `✅` con nota "Feedback loop completo: botones Útil/No me sirve + confirmación" |
| Item diferido | 99 | "UI de feedback... no se implementó" | Eliminar o marcar como implementado |

**Esfuerzo**: Trivial (minutos)

---

## Dependencias entre ítems

```
Item 5 (doc) ── independiente ──→ puede hacerse ya
Item 2 (margen) ── independiente ──→ ya está hecho, solo doc
Item 1 (estacionalidad) ──→ depende de RPC rpc_analisis_mensual
Item 3 (costos) ──→ depende de RPC recalcular_resumen_diario
Item 1 e Item 3 ── NO compiten (tocan RPCs diferentes)
Item 4 (widget) ── independiente ──→ pero consume datos de resumen_diario (que Item 3 mejora)
```

Conclusión: Los 5 ítems son ortogonales o débilmente acoplados. No hay dependencias bloqueantes entre ellos.

---

## Agrupación recomendada en fases

### Fase A (doc fix + confirmación) — 1 sesión, esfuerzo trivial

| Item | Cambio | Esfuerzo |
|------|--------|----------|
| **#5** | Actualizar PARTE-B-COMPLETA.md (feedback ✅, eliminar diferido) | Trivial |
| **#2** | Confirmar margen wording ya implementado — solo actualizar doc | Trivial |

### Fase B (RPC + estacionalidad) — 2 sesiones, esfuerzo mediano

| Item | Cambio | Esfuerzo |
|------|--------|----------|
| **#3** | Modificar `recalcular_resumen_diario()` para usar `costo_real_*` de `dispensacion_items` | Mediano |
| **#1** | Agregar `meses_historicos` al RPC + modelo + advertencia en `ProyeccionCard` | Chico |

### Fase C (widget) — 3-4 sesiones, esfuerzo grande

| Item | Cambio | Esfuerzo |
|------|--------|----------|
| **#4** | Widget AppWidgetProvider + RemoteViews + WorkManager | Grande |

### Orden sugerido

```
Semana 1: Fase A (doc — 1h)
Semana 2: Fase B (RPC — 2-3 días)
Semana 3+: Fase C (widget — 1 semana)
```

---

## Resumen de esfuerzo

| Item | Esfuerzo | Prioridad |
|------|----------|-----------|
| #2 (margen wording) | ✅ Ya implementado | — |
| #5 (doc) | ⬜ Trivial | Alta (clarifica estado real) |
| #1 (estacionalidad) | 🟢 Chico | Media (mejora UX) |
| #3 (costos reales) | 🟡 Mediano | Alta (precisión de datos) |
| #4 (widget) | 🔴 Grande | Baja (feature nuevo, diferido originalmente) |

**Recomendación**: Hacer Fase A inmediatamente (documentar lo que ya está listo), luego Fase B (corregir costos + estacionalidad), y evaluar si el widget justifica la inversión.

---

## Archivos consultados

- `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisDetalleScreen.kt`
- `optoapp/src/main/java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt`
- `optoapp/src/main/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModel.kt`
- `optoapp/src/main/java/com/example/optoapp/domain/AnalisisMensual.kt`
- `optoapp/src/main/java/com/example/optoapp/domain/ObtenerAnalisisMensualUseCase.kt`
- `optoapp/src/main/java/com/example/optoapp/AndroidManifest.xml`
- `optoapp/src/test/java/com/example/optoapp/viewmodel/AnalisisNegocioViewModelTest.kt`
- `supabase/migrations/20260705000000_fase6_esquema_analisis.sql`
- `supabase/migrations/20260706000000_fase7_rpc_indicadores.sql`
- `supabase/migrations/20260712000001_costos_matriz.sql`
- `openspec/PARTE-B-COMPLETA.md`
