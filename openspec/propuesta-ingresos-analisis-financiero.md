# Sistema de Ingresos y Análisis Financiero — OptoApp
## Documento de Arquitectura Corregido (v2 — Ledger/Sub-Ledger)

> **Versión original revisada**: `prompt-maestro-completo-ingresos-analisis-financiero-optoapp.md`
> **Cambio arquitectónico principal**: se reemplazan las vistas `UNION ALL` por una tabla canónica `ventas` (ledger) + `dispensaciones`/`servicios_extra` como sub-ledgers de negocio.
> **Schema validado contra**: migraciones reales de `supabase/migrations/` y entities Room en `optoapp/`.

---

## Resumen ejecutivo del cambio de arquitectura

El documento original proponía 3 vistas SQL con `UNION ALL` entre `dispensaciones` y `servicios_extra` para unificar ingresos. **Esta versión va un paso más allá**: crea una tabla canónica `ventas` como ledger financiero, manteniendo `dispensaciones` y `servicios_extra` intactas como sub-ledgers de datos de negocio.

**¿Por qué no vistas UNION ALL?**

- Cada nuevo tipo de ingreso futuro requeriría otro `UNION` en cada vista, RPC, y query del módulo de análisis.
- Los campos financieros tienen nombres distintos entre tablas (`monto_pagado` vs `a_cuenta`), obligando a lógica condicional en cada cálculo.
- `pagos` ya referencia a dos tablas distintas (`dispensacion_id` / `servicio_extra_id`), forzando joins con `OR` o subqueries.
- Construir un módulo de KPIs, márgenes, aging, y proyecciones sobre vistas UNION ALL es frágil y difícil de auditar.

**¿Qué ganamos con el ledger?**

- Una sola tabla `ventas` como fuente de verdad para TODOS los cálculos financieros.
- `pagos` referencia únicamente a `ventas.id` — un solo JOIN.
- Cualquier RPC, reporte, o KPI futuro consulta `ventas`, sin saber si el origen fue una dispensación o un servicio extra.
- Preparado para nuevos tipos de ingreso sin cambiar queries existentes.
- `dispensaciones` y `servicios_extra` no se modifican en su estructura de negocio.

**Costo**: una migración de schema + backfill de datos existentes + refactor de queries de Cierre de Caja/Reportes. Es trabajo acotado y predecible, no diseño especulativo.

---

## Contexto general del negocio (sin cambios)

`dispensaciones` y `servicios_extra` son dos fuentes de negocio distintas pero comparten la misma estructura financiera:

- `monto_total` (venta/servicio total).
- Historial de pagos: cada pago tiene su propio `monto`, `fecha` y `metodo_pago`, editable/eliminable individualmente.
- `saldo_restante` = `monto_total - SUM(pagos)`.
- `estado` (`Pendiente` / `Entregado`).

**No se fusionan las tablas ni las pantallas de captura.** Lo que se unifica es la capa de cálculo de ingresos, materializada en `ventas`.

### Bug raíz que origina la Parte A

Actualmente Cierre de Caja muestra bajo la label **"TOTAL VENTAS DEL DÍA"** solo `dispensaciones`. El ViewModel SÍ calcula servicios_extra por separado (`totalServiciosExtra`), pero la UI no los fusiona. El dato existe, está mal presentado.

---

# PARTE A — UNIFICACIÓN DE INGRESOS (LEDGER)

---

## FASE 1 — Tabla canónica `ventas` + migración de `pagos`

**Objetivo**: crear la fuente de verdad única para ingresos. `dispensaciones` y `servicios_extra` siguen existiendo sin cambios para sus datos de negocio.

### 1.1 Schema real actual (lo que existe hoy)

#### `public.pagos` (única tabla de abonos — NO existen `abonos_dispensacion` ni `abonos_servicio_extra`)

```sql
-- Migración: 20260413052748_remote_schema_20260413.sql
CREATE TABLE public.pagos (
    id                TEXT PRIMARY KEY,
    dispensacion_id   TEXT,              -- referencia débil, sin FK en Postgres
    servicio_extra_id TEXT,              -- referencia débil, sin FK en Postgres
    fecha             DATE,              -- fecha del movimiento de caja
    tipo              TEXT NOT NULL DEFAULT 'Abono',
    monto             NUMERIC,           -- puede ser negativo (anulaciones)
    metodo_pago       TEXT NOT NULL DEFAULT 'Efectivo',
    nota              TEXT,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    optica_id         TEXT NOT NULL DEFAULT 'mi_optica_base',
    updated_by        UUID
);
```

#### `public.dispensaciones` (columnas financieras relevantes)

```
monto_total   NUMERIC
monto_pagado  NUMERIC    -- campo desnormalizado, se sincroniza desde pagos
estado_entrega TEXT      -- CHECK ('Pendiente','Entregado')
fecha         DATE
```

#### `public.servicios_extra` (columnas financieras relevantes)

```
monto_total   NUMERIC
a_cuenta      NUMERIC    -- campo desnormalizado, se sincroniza desde pagos
estado        TEXT       -- CHECK ('Pendiente','Entregado')
fecha         DATE
```

### 1.2 Nueva tabla `ventas` (ledger)

```sql
CREATE TABLE public.ventas (
    id                      TEXT PRIMARY KEY,
    optica_id               TEXT NOT NULL REFERENCES public.opticas(id),
    origen                  TEXT NOT NULL CHECK (origen IN ('dispensacion', 'servicio_extra')),
    origen_id               TEXT NOT NULL,          -- id en la tabla de origen
    paciente_id             TEXT,
    fecha                   DATE NOT NULL,          -- fecha de registro de la venta
    fecha_entrega           DATE,
    monto_total             NUMERIC NOT NULL,
    -- Snapshot de costo al momento de la venta (para margen histórico)
    costo_unitario_snapshot NUMERIC,                -- NULL hasta Fase 6
    estado                  TEXT NOT NULL DEFAULT 'Pendiente' CHECK (estado IN ('Pendiente', 'Entregado')),
    -- Metadatos para trazabilidad
    created_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_by              UUID
);

-- Índices para queries frecuentes
CREATE INDEX idx_ventas_optica_fecha ON public.ventas (optica_id, fecha);
CREATE INDEX idx_ventas_origen ON public.ventas (origen, origen_id);
CREATE INDEX idx_ventas_paciente ON public.ventas (paciente_id);

-- RLS
ALTER TABLE public.ventas ENABLE ROW LEVEL SECURITY;

CREATE POLICY ventas_select ON public.ventas FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));

CREATE POLICY ventas_insert ON public.ventas FOR INSERT
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']));

CREATE POLICY ventas_update ON public.ventas FOR UPDATE
    USING (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']))
    WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id,
        ARRAY['admin','gerente','especialista','asesor','asesora','ventas']));

CREATE POLICY ventas_delete ON public.ventas FOR DELETE
    USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente']));
```

### 1.3 Migración de `pagos` para referenciar `ventas`

```sql
-- Agregar columna venta_id (convive con dispensacion_id/servicio_extra_id durante la transición)
ALTER TABLE public.pagos ADD COLUMN venta_id TEXT;

CREATE INDEX idx_pagos_venta ON public.pagos (venta_id);

-- Backfill: vincular cada pago existente a su venta correspondiente
UPDATE public.pagos p
SET venta_id = v.id
FROM public.ventas v
WHERE v.origen = 'dispensacion'
  AND v.origen_id = p.dispensacion_id
  AND p.dispensacion_id IS NOT NULL;

UPDATE public.pagos p
SET venta_id = v.id
FROM public.ventas v
WHERE v.origen = 'servicio_extra'
  AND v.origen_id = p.servicio_extra_id
  AND p.servicio_extra_id IS NOT NULL;

-- TODO post-transición (después de validar en producción):
-- ALTER TABLE public.pagos DROP COLUMN dispensacion_id;
-- ALTER TABLE public.pagos DROP COLUMN servicio_extra_id;
-- Mantener durante al menos un ciclo de release para rollback seguro.
```

### 1.4 Backfill de `ventas` desde datos existentes

```sql
-- Dispensaciones existentes
INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id, fecha,
                            fecha_entrega, monto_total, estado)
SELECT
    'v_disp_' || id,
    optica_id,
    'dispensacion',
    id,
    paciente_id,
    fecha,
    fecha_entrega,
    monto_total,
    estado_entrega
FROM public.dispensaciones
ON CONFLICT (id) DO NOTHING;

-- Servicios extra existentes
INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id, fecha,
                            fecha_entrega, monto_total, estado)
SELECT
    'v_serv_' || id,
    optica_id,
    'servicio_extra',
    id,
    paciente_id,
    fecha,
    fecha_entrega,
    monto_total,
    estado
FROM public.servicios_extra
ON CONFLICT (id) DO NOTHING;
```

### 1.5 Mantener `ventas` en sync (estrategia de sincronización)

**Opción recomendada: trigger en Supabase + upsert desde Android**

```sql
-- Función auxiliar
CREATE OR REPLACE FUNCTION public.sync_venta_from_dispensacion()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id,
                                fecha, fecha_entrega, monto_total, estado)
    VALUES ('v_disp_' || NEW.id, NEW.optica_id, 'dispensacion', NEW.id,
            NEW.paciente_id, NEW.fecha, NEW.fecha_entrega, NEW.monto_total,
            NEW.estado_entrega)
    ON CONFLICT (id) DO UPDATE SET
        monto_total = EXCLUDED.monto_total,
        estado = EXCLUDED.estado,
        fecha_entrega = EXCLUDED.fecha_entrega,
        updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_dispensacion_to_venta
    AFTER INSERT OR UPDATE OF monto_total, estado_entrega, fecha_entrega ON public.dispensaciones
    FOR EACH ROW EXECUTE FUNCTION public.sync_venta_from_dispensacion();

-- Equivalente para servicios_extra
CREATE OR REPLACE FUNCTION public.sync_venta_from_servicio_extra()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.ventas (id, optica_id, origen, origen_id, paciente_id,
                                fecha, fecha_entrega, monto_total, estado)
    VALUES ('v_serv_' || NEW.id, NEW.optica_id, 'servicio_extra', NEW.id,
            NEW.paciente_id, NEW.fecha, NEW.fecha_entrega, NEW.monto_total,
            NEW.estado)
    ON CONFLICT (id) DO UPDATE SET
        monto_total = EXCLUDED.monto_total,
        estado = EXCLUDED.estado,
        fecha_entrega = EXCLUDED.fecha_entrega,
        updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_servicio_to_venta
    AFTER INSERT OR UPDATE OF monto_total, estado, fecha_entrega ON public.servicios_extra
    FOR EACH ROW EXECUTE FUNCTION public.sync_venta_from_servicio_extra();
```

**En Android**: al guardar dispensación o servicio extra, el `UploadSyncCoordinator` ya sube a Supabase → el trigger mantiene `ventas` en sync automáticamente. Para modo offline, Room tendrá su propia tabla `ventas` que se actualiza en el mismo `saveDispensacion()`/`saveServicio()`.

### 1.6 Nueva entidad Room: `Venta`

```kotlin
@Entity(tableName = "ventas")
data class Venta(
    @PrimaryKey val id: String,
    val opticaId: String,
    val origen: String,          // "dispensacion" | "servicio_extra"
    val origenId: String,
    val pacienteId: String?,
    val fecha: LocalDate,
    val fechaEntrega: LocalDate?,
    val montoTotal: Double,
    val costoUnitarioSnapshot: Double? = null,
    val estado: String,
    val updatedAt: String? = null,
    val updatedBy: String? = null
)
```

### 1.7 Distinción conceptual obligatoria (actualizada)

- **"Total Ventas del Día"** = `SUM(monto_total)` de `ventas` donde `fecha` = hoy.
- **"Total Recaudado"** = `SUM(monto)` de `pagos` donde `fecha` = hoy.
- **"Saldo Pendiente"** = de `ventas`: `SUM(monto_total) - SUM(pagos.monto)` agrupado por `venta_id`.
- **"Desglose Efectivo/Móvil-Trans/Tarjeta"** = agrupar `pagos` por `metodo_pago`, filtrado por `fecha` = hoy.

### 1.8 ID de venta: convención de prefijo

Para identificar el origen sin necesidad de JOIN ni columna `origen`:
- Dispensación `abc123` → venta `v_disp_abc123`
- Servicio extra `xyz789` → venta `v_serv_xyz789`

Esta convención permite derivar el `origen_id` desde el `id` de venta con un simple split de string, y viceversa.

**Entregable de Fase 1**:
1. Migración SQL: `CREATE TABLE ventas` + RLS + índices + triggers.
2. Migración SQL: `ALTER TABLE pagos ADD COLUMN venta_id` + backfill.
3. Migración SQL: backfill de `ventas` desde datos existentes.
4. Room: entidad `Venta`, DAO `VentaDao`.
5. Android: lógica de upsert en `ventas` local al guardar dispensación/servicio extra.
6. Android: `UploadSyncCoordinator` incluye `ventas` en el batch de sync.

---

## FASE 2 — Corregir Cierre de Caja y Reportes (consumir `ventas`)

**Objetivo**: eliminar el bug raíz usando la nueva tabla `ventas`.

### 2.1 Diagnóstico real del bug (corregido)

El ViewModel `CierreCajaViewModel` ya calcula ambos orígenes:

```kotlin
// CierreCajaViewModel.kt (~línea 123-127)
val totalVentasHoy = dispensaciones.filter { it.fecha == fecha }.sumOf { it.montoTotal }
val totalServiciosExtra = servicios.filter { it.fecha == fecha }.sumOf { it.montoTotal }
val totalGeneral = totalVentasHoy + totalServiciosExtra  // ← AMBOS incluidos
```

**Pero la UI muestra cada cosa por separado:**
- `CierreCajaScreen.kt`: "TOTAL VENTAS DEL DÍA" → `uiState.totalVentasHoy` (solo dispensaciones)
- Sección separada "Servicios Extra" → `uiState.serviciosExtraHoy`

**Corrección**: cambiar la label "TOTAL VENTAS DEL DÍA" para que use `totalGeneral` (o mejor, que lea de `ventas`). La sección de servicios extra separada puede mantenerse como desglose informativo.

### 2.2 Cambios requeridos

1. **`CierreCajaViewModel`**: reemplazar las queries actuales (que leen `dispensaciones` + `servicios_extra` por separado) por una sola query contra `ventas`.
2. **`CierreCajaScreen`**: la label principal muestra `SUM(monto_total)` de `ventas` filtrado por fecha de hoy.
3. **Desglose por método de pago**: ya funciona correctamente desde `pagos` — solo verificar que incluya pagos de ambos orígenes (ya lo hace, `pagos` no distingue origen en sus queries por fecha).
4. **`ReportesViewModel`**: todas las sumas de ventas/cobros/saldos pasan a consultar `ventas` y `pagos` con `venta_id`.
5. **Dashboard BI (`BIViewModel`)**: KPIs de ingresos pasan a usar `ventas`.
6. **Verificar `rpc_resumen_financiero`**: actualizarlo para que lea de `ventas` en vez de hacer `UNION` manual de dispensaciones y servicios_extra. O mejor: reemplazarlo por queries directas a `ventas`.
7. **`rpc_cierre_caja_resumen`**: ya consulta solo `pagos` → sigue funcionando sin cambios.

### 2.3 Corrección del bug de anulaciones con fecha incorrecta

**Diagnóstico confirmado** (código real en `DispensacionRepository.kt`, línea 170-191):

```kotlin
suspend fun deletePagoRegistrandoAnulacionEnCaja(
    pago: Pago,
    opticaId: String,
    fechaAnulacion: LocalDate = DateUtils.today()  // ← DEFAULT ES HOY
) {
    val reversal = Pago(
        fecha = fechaAnulacion,          // ← USA LA FECHA DE HOY, NO LA ORIGINAL
        tipo = "Anulación",
        monto = -existing.monto,
        nota = "Anula abono ${existing.id.take(8)}… (${DateUtils.formatLocalized(existing.fecha)})"
    )
    pagoDao.insertPago(reversal)         // ← INSERTA reversal con fecha de hoy
    pagoDao.deletePago(pago)             // ← BORRA el original
}
```

**Problema**: el reversal se crea con `DateUtils.today()` (fecha de la eliminación), no con la fecha del pago original. Si eliminás un abono del 29 de junio el 4 de julio, el Cierre de Caja del 4 de julio muestra -S/. 50.00 que no corresponde a ningún movimiento real de ese día.

**Corrección en el método**:

```kotlin
suspend fun deletePagoRegistrandoAnulacionEnCaja(
    pago: Pago,
    opticaId: String
) {
    val existing = pagoDao.getPagoById(pago.id) ?: return
    val reversal = Pago(
        fecha = existing.fecha,          // ← FECHA DEL PAGO ORIGINAL
        tipo = "Anulación",
        monto = -existing.monto,
        metodoPago = existing.metodoPago,
        nota = "Anula abono ${existing.id.take(8)}… (${DateUtils.formatLocalized(existing.fecha)})",
        opticaId = opticaId
    )
    pagoDao.insertPago(reversal)
    pagoDao.deletePago(pago)
}
```

**Caso de prueba obligatorio**: eliminar un abono de S/. 50.00 fechado el 29 de junio → el reporte del 29 de junio debe reflejar el ajuste (neto corregido), y el reporte del 4 de julio NO debe mostrar ningún movimiento relacionado.

### 2.4 Espejo local en Room (offline-first)

- **Opción A (preferida)**: `VentaDao` con queries directas. `ventas` se sincroniza igual que cualquier otra entidad (upload → trigger → download). En modo offline, el DAO local ya tiene los datos.
- **Opción B**: queries contra `DispensacionOptica` + `ServicioExtra` locales y suma en memoria. Solo como fallback si `ventas` no está poblada localmente.

### 2.5 Casos de prueba obligatorios (Fase 2)

- 0 dispensaciones + N servicios_extra con pagos parciales → "Total Ventas del Día" incluye servicios_extra.
- Mezcla de dispensaciones y servicios_extra registrados el mismo día → total = suma de ambos `monto_total`.
- Servicio_extra registrado el 24/06 con abonos el 24/06 y 25/06 → "Total Recaudado" correcto en cada fecha.
- Abonos con métodos de pago distintos sobre la misma venta → desglose por método correcto.
- **Anulación con fecha correcta**: eliminar abono del 29/06 → no contamina Cierre de Caja del día de eliminación.
- **Regresión**: números actuales de "Total Recaudado" y desglose por método de pago no cambian.

**Entregable de Fase 2**: Cierre de Caja, Reportes, y Dashboard BI consumiendo `ventas`. Fix de anulaciones. Tests de regresión.

---

## FASE 3 — Pantalla financiera dedicada para Dispensaciones

**Objetivo**: extraer la gestión de pagos de "Editar Dispensación" a una pantalla propia.

**Estado actual**: no existe `InformacionFinancieraScreen`. Los pagos se gestionan inline en `NuevaDispensacionScreen.kt` via el composable `FinancieraInfoSection` (en `DispensacionFormSections.kt`). El usuario debe scrollear hasta el final del formulario.

### 3.1 Encabezado de contexto (obligatorio)

Para evitar ambigüedad sobre a qué orden corresponde el desglose:

- N° OT
- Nombre del paciente
- Fecha de la orden (fecha de registro, no fecha de pago)
- Descripción breve (ej. "Lente progresivo + Montura Metal")

Siempre visible arriba, sticky, solo lectura.

### 3.2 Nueva pantalla y ViewModel

```kotlin
// Nueva ruta: "informacion_financiera/{dispensacionId}"
@Composable
fun InformacionFinancieraScreen(
    dispensacionId: String,
    onNavigateBack: () -> Unit,
    viewModel: InformacionFinancieraViewModel = hiltViewModel()
)

data class ContextoVenta(
    val otNumero: String?,
    val pacienteNombre: String,
    val fechaOrden: LocalDate,
    val descripcionBreve: String
)
```

Contenido:
- Encabezado de contexto (3.1), sticky.
- Monto Total (editable).
- Historial de Pagos: lista con monto, fecha, método de pago; editable/eliminable individualmente.
- Botón "Agregar Pago".
- Saldo Restante (calculado reactivamente: `montoTotal - SUM(pagos)`).
- Estado (Pendiente/Entregado).
- Botón "Guardar Cambios".

### 3.3 Repositorio

```kotlin
interface DispensacionFinancieraRepository {
    suspend fun obtenerDispensacion(dispensacionId: String): DispensacionOptica
    suspend fun obtenerContexto(dispensacionId: String): ContextoVenta
    suspend fun actualizarMontoTotal(dispensacionId: String, monto: Double)
    suspend fun agregarPago(dispensacionId: String, pago: Pago)
    suspend fun editarPago(pago: Pago)
    suspend fun eliminarPago(pago: Pago)
    suspend fun actualizarEstado(dispensacionId: String, estado: String)
    // Mantener ventas en sync localmente
    suspend fun syncVentaFromDispensacion(dispensacionId: String)
}
```

### 3.4 Refactor de pantalla existente

- **"Editar Dispensación"** (`NuevaDispensacionScreen.kt`): reemplazar `FinancieraInfoSection` inline por una **tarjeta resumen** (Monto Total, Saldo Restante, Estado) con botón "Gestionar Pagos" → navega a `InformacionFinancieraScreen`.
- **"Editar Servicio"** (`NuevoServicioScreen.kt`): no se modifica en esta fase. Su gestión de pagos inline se mantiene.
- Offline-first: cambios en Room primero, sincronizan con Supabase según patrón existente.
- Mantener estilo visual OptoApp (`#2C3E50` / `#27AE60`, Cards 12dp, modo oscuro, Material 3).

### 3.5 Casos de prueba (Fase 3)

- Agregar/editar/eliminar un pago desde `InformacionFinancieraScreen` → saldo correcto, persiste al volver.
- Cambios reflejados en Cierre de Caja/Reportes.
- **Encabezado de contexto**: abrir OT 4670 y luego OT 4673 en misma sesión → cada una muestra su propio encabezado.
- "Editar Servicio" sigue funcionando exactamente igual (sin cambios).

**Entregable de Fase 3**: `InformacionFinancieraScreen`, `InformacionFinancieraViewModel`, `DispensacionFinancieraRepository`, refactor de "Editar Dispensación".

---

## FASE 4 — Corregir navegación "Ir a Financiero"

**Objetivo**: conectar la pantalla nueva de Fase 3 al punto de entrada existente.

### 4.1 Bug actual

El botón "Ir a Financiero" existe en `PacienteDispensacionesTab.kt` y `PacientesListScreen.kt`, dentro del `ResumenDispensacionDialog`. Navega a:

```
editarDispensacion/{pacienteId}/{id}?focus=financiero
```

**`?focus=financiero` es un query parameter muerto.** La ruta solo extrae `{pacienteId}` y `{dispId}` — el query param nunca se parsea. Ambos botones ("Editar Completo" e "Ir a Financiero") terminan en el mismo destino.

### 4.2 Corrección

- **"Editar Completo"** → `editarDispensacion/{pacienteId}/{id}` (sin cambios).
- **"Ir a Financiero"** → nueva ruta `informacion_financiera/{dispensacionId}`.

Agregar la ruta al NavHost interno en `MainDrawerScreen.kt`:

```kotlin
composable("informacion_financiera/{dispensacionId}") { backStackEntry ->
    val dispensacionId = backStackEntry.arguments?.getString("dispensacionId") ?: return@composable
    InformacionFinancieraScreen(dispensacionId = dispensacionId, onNavigateBack = { navController.popBackStack() })
}
```

### 4.3 Casos de prueba (Fase 4)

- **Regresión**: "Ir a Financiero" abre `InformacionFinancieraScreen`.
- "Editar Completo" sigue abriendo "Editar Dispensación".
- Confirmar que no se modificó navegación de Servicios Extra.

**Entregable de Fase 4**: nueva ruta + navegación corregida.

---

## FASE 5 — Corregir espaciado excesivo sobre títulos de pantalla

**Diagnóstico corregido**: la investigación muestra que el layout compartido (`MainDrawerScreen.kt`) es estándar y no tiene spacers excesivos. El problema está en el **patrón de doble padding** repetido en múltiples screens individuales:

```kotlin
.padding(padding)   // del Scaffold (incluye top bar + system insets)
.padding(16.dp)     // adicional en cada screen
```

### 5.1 Acciones

1. Auditar las pantallas afectadas (Cierre de Caja, Servicios Varios, Nuevo/Editar Servicio, Editar Dispensación, Pacientes, Ficha del Paciente).
2. Estandarizar el padding a un solo nivel: `padding(padding)` del Scaffold + `padding(horizontal = 16.dp)` donde sea necesario, eliminando el `padding(16.dp)` redundante.
3. Si varias pantallas comparten el mismo patrón, extraer un modifier reutilizable.
4. Verificar el fix visual en al menos 3 pantallas.

**Entregable de Fase 5**: padding estandarizado en todas las pantallas mencionadas.

---

# PARTE B — MÓDULO DE ANÁLISIS FINANCIERO
## Rediseñado para entender, manejar y mejorar el negocio

**Prerrequisito estricto**: Fases 1 y 2 completadas y probadas en producción. Todo cálculo se construye sobre `ventas` y `pagos` (con `venta_id`).

### Principio rector de esta parte

El dueño de una óptica no es contador. Sabe vender lentes y monturas. Este módulo **no le habla en lenguaje financiero** — le habla en lenguaje de negocio. Cada número, cada recomendación, cada pantalla responde una de estas seis preguntas:

1. **¿Estoy ganando plata?** → "De cada S/ 100 que vendés, te quedan S/ 18."
2. **¿De dónde viene y a dónde va?** → "El 60% de tu ganancia viene de lentes progresivos."
3. **¿Quién me debe y hace cuánto?** → "Juan Pérez te debe S/ 1,200 hace 60 días."
4. **¿Qué me conviene vender?** → "Los lentes premium te dejan el triple que los básicos."
5. **¿Qué tengo que hacer hoy?** → "Llamá a estos 3 clientes para cobrar S/ 2,500."
6. **¿Estoy mejor que antes?** → "Este mes vendiste 12% más que el mes pasado."

Si un KPI necesita un glosario para entenderse, está mal diseñado.

### Usuarios

Roles `admin` y `gerente` únicamente (ya definidos en `AppRoles`).

---

## FASE 6 — Esquema de datos para análisis de negocio

**Objetivo**: crear las tablas que permiten responder las 6 preguntas del principio rector.

### 6.1 Categorías de producto (para análisis de margen por tipo)

```sql
CREATE TABLE public.categorias_producto (
    id          TEXT PRIMARY KEY,         -- 'lente_progresivo', 'lente_monofocal', 'montura_premium', etc.
    nombre      TEXT NOT NULL,            -- "Lentes Progresivos"
    familia     TEXT NOT NULL,            -- 'lente', 'montura', 'servicio'
    orden       INTEGER DEFAULT 0         -- para ordenar en UI
);

-- Datos semilla
INSERT INTO public.categorias_producto (id, nombre, familia, orden) VALUES
    ('lente_progresivo',  'Lentes Progresivos',   'lente',    1),
    ('lente_monofocal',   'Lentes Monofocales',   'lente',    2),
    ('lente_bifocal',     'Lentes Bifocales',     'lente',    3),
    ('lente_otro',        'Otros Lentes',         'lente',    9),
    ('montura_premium',   'Monturas Premium',     'montura',  4),
    ('montura_estandar',  'Monturas Estándar',    'montura',  5),
    ('montura_economica', 'Monturas Económicas',  'montura',  6),
    ('servicio_extra',    'Servicios Extra',      'servicio', 7),
    ('servicio_garantia', 'Garantías Extendidas', 'servicio', 8);
```

### 6.2 Agregar `categoria_producto_id` a `ventas`

```sql
ALTER TABLE public.ventas ADD COLUMN categoria_producto_id TEXT
    REFERENCES public.categorias_producto(id);

-- La app asigna la categoría al crear la venta basándose en el tipo de lente + montura
```

### 6.3 Tabla de márgenes pre-calculados por categoría (materializada)

**Por qué**: calcular márgenes por categoría en cada carga del dashboard requeriría escanear todas las ventas históricas con JOIN a costos. Pre-calculamos cada noche y consultamos en milisegundos.

```sql
CREATE TABLE public.margen_por_categoria (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id               TEXT NOT NULL REFERENCES public.opticas(id),
    categoria_producto_id   TEXT NOT NULL REFERENCES public.categorias_producto(id),
    periodo                 TEXT NOT NULL,       -- '2026-07' (mes), '2026-Q3' (trimestre), '2026' (año)
    tipo_periodo            TEXT NOT NULL CHECK (tipo_periodo IN ('mensual', 'trimestral', 'anual')),
    ventas_totales          NUMERIC NOT NULL,    -- suma de monto_total
    costo_total             NUMERIC NOT NULL,    -- suma de costo_unitario_snapshot
    cantidad_ventas         INTEGER NOT NULL,    -- count(*)
    margen_bruto            NUMERIC NOT NULL,    -- ventas_totales - costo_total
    margen_porcentaje       NUMERIC NOT NULL,    -- (margen_bruto / ventas_totales) * 100
    ticket_promedio         NUMERIC NOT NULL,    -- ventas_totales / cantidad_ventas
    calculado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (optica_id, categoria_producto_id, periodo, tipo_periodo)
);

CREATE INDEX idx_margen_cat_opt_per ON public.margen_por_categoria (optica_id, periodo);

ALTER TABLE public.margen_por_categoria ENABLE ROW LEVEL SECURITY;

CREATE POLICY margen_cat_select ON public.margen_por_categoria FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));
```

### 6.4 Actualizar `gastos_operativos` para análisis de negocio

```sql
-- La tabla original tiene CHECK (categoria IN ('fijo', 'variable'))
-- Ampliamos para que refleje cómo piensa un dueño:

ALTER TABLE public.gastos_operativos DROP CONSTRAINT IF EXISTS gastos_operativos_categoria_check;

ALTER TABLE public.gastos_operativos ADD CONSTRAINT gastos_operativos_categoria_check
    CHECK (categoria IN (
        'alquiler',         -- alquiler del local
        'servicios',        -- luz, agua, internet
        'personal',         -- sueldos y cargas sociales
        'proveedores',      -- pagos a laboratorios y proveedores
        'insumos',          -- materiales, herramientas
        'marketing',        -- publicidad, redes sociales
        'impuestos',        -- tasas, licencias
        'otro'              -- cualquier cosa que no encaje
    ));
```

### 6.5 Resumen diario pre-agregado

```sql
CREATE TABLE public.resumen_diario (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id                   TEXT NOT NULL REFERENCES public.opticas(id),
    fecha                       DATE NOT NULL,
    -- Ventas del día
    ventas_cantidad             INTEGER NOT NULL DEFAULT 0,
    ventas_monto_total          NUMERIC NOT NULL DEFAULT 0,
    ventas_costo_total          NUMERIC NOT NULL DEFAULT 0,
    -- Cobros del día
    cobros_cantidad             INTEGER NOT NULL DEFAULT 0,
    cobros_monto_total          NUMERIC NOT NULL DEFAULT 0,
    -- Saldo pendiente acumulado al cierre del día
    saldo_pendiente_total       NUMERIC NOT NULL DEFAULT 0,
    saldo_pendiente_cantidad    INTEGER NOT NULL DEFAULT 0,
    -- Inventario al cierre del día
    inventario_valor            NUMERIC,
    inventario_unidades         INTEGER,
    -- Metadatos
    calculado_en                TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (optica_id, fecha)
);

CREATE INDEX idx_resumen_diario_opt_fecha ON public.resumen_diario (optica_id, fecha);

ALTER TABLE public.resumen_diario ENABLE ROW LEVEL SECURITY;

CREATE POLICY resumen_diario_select ON public.resumen_diario FOR SELECT
    USING (app_private.is_optica_member(auth.uid(), optica_id));
```

**Estrategia de recálculo: bajo demanda + caché (Opción D)**

No se usa `pg_cron` ni edge functions. La función se llama cuando el usuario abre la pantalla de análisis:

```sql
CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY INVOKER
AS $$
DECLARE
    v_ventas_cantidad INTEGER;
    v_ventas_monto NUMERIC;
    v_ventas_costo NUMERIC;
    v_cobros_cantidad INTEGER;
    v_cobros_monto NUMERIC;
    v_saldo_total NUMERIC;
    v_saldo_cantidad INTEGER;
    v_inv_valor NUMERIC;
    v_inv_unidades INTEGER;
BEGIN
    -- Ventas del día
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto_total), 0), COALESCE(SUM(costo_unitario_snapshot), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM public.ventas
    WHERE optica_id = p_optica_id AND fecha = p_fecha;

    -- Cobros del día
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha;

    -- Saldo pendiente acumulado
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(v.monto_total - COALESCE(pg.total_pagado, 0)), 0)
    INTO v_saldo_cantidad, v_saldo_total
    FROM public.ventas v
    LEFT JOIN (
        SELECT venta_id, SUM(monto) AS total_pagado
        FROM public.pagos WHERE optica_id = p_optica_id GROUP BY venta_id
    ) pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
      AND v.monto_total - COALESCE(pg.total_pagado, 0) > 0.005;

    -- Inventario al cierre
    SELECT COALESCE(SUM(costo * stock_actual), 0), COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas WHERE optica_id = p_optica_id;

    -- Upsert idempotente
    INSERT INTO public.resumen_diario (
        optica_id, fecha,
        ventas_cantidad, ventas_monto_total, ventas_costo_total,
        cobros_cantidad, cobros_monto_total,
        saldo_pendiente_total, saldo_pendiente_cantidad,
        inventario_valor, inventario_unidades
    ) VALUES (
        p_optica_id, p_fecha,
        v_ventas_cantidad, v_ventas_monto, v_ventas_costo,
        v_cobros_cantidad, v_cobros_monto,
        v_saldo_total, v_saldo_cantidad,
        v_inv_valor, v_inv_unidades
    )
    ON CONFLICT (optica_id, fecha) DO UPDATE SET
        ventas_cantidad = EXCLUDED.ventas_cantidad,
        ventas_monto_total = EXCLUDED.ventas_monto_total,
        ventas_costo_total = EXCLUDED.ventas_costo_total,
        cobros_cantidad = EXCLUDED.cobros_cantidad,
        cobros_monto_total = EXCLUDED.cobros_monto_total,
        saldo_pendiente_total = EXCLUDED.saldo_pendiente_total,
        saldo_pendiente_cantidad = EXCLUDED.saldo_pendiente_cantidad,
        inventario_valor = EXCLUDED.inventario_valor,
        inventario_unidades = EXCLUDED.inventario_unidades,
        calculado_en = now();
END;
$$;
```

**Flujo**:
1. Usuario abre la pantalla de análisis → la app/Web llama a `recalcular_resumen_diario(optica_id, hoy)`.
2. La función es idempotente: `ON CONFLICT DO UPDATE`. Llamarla 10 veces seguidas no duplica nada, solo actualiza con datos frescos.
3. Si el día ya fue calculado ayer, tarda milisegundos (el índice único lo resuelve instantáneamente).
4. Las consultas de KPIs (`rpc_analisis_mensual`) leen de `resumen_diario`, no de las tablas crudas.

**Ventajas**:
- No requiere plan pago de Supabase (sin `pg_cron`).
- Funciona igual desde Android y OptoWeb.
- Auto-sanador: si un día no se calculó, la primera visita lo resuelve.
- Con el volumen de transacciones de una óptica, el cálculo es imperceptible.

### 6.6 Costos de productos (con histórico)

Igual que la versión anterior pero con una mejora: registrar el historial de cambios para auditoría.

```sql
CREATE TABLE public.costos_productos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id           TEXT NOT NULL REFERENCES public.opticas(id),
    categoria_producto_id TEXT NOT NULL REFERENCES public.categorias_producto(id),
    producto_descripcion TEXT,              -- "Tokai 1.67 Progresivo" o "Montura Ray-Ban RX123"
    costo_unitario      NUMERIC NOT NULL,
    vigente_desde       DATE NOT NULL DEFAULT CURRENT_DATE,
    vigente_hasta       DATE,               -- NULL = sigue vigente
    fecha_actualizacion TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_costos_vigentes ON public.costos_productos (optica_id, categoria_producto_id)
    WHERE vigente_hasta IS NULL;

ALTER TABLE public.costos_productos ENABLE ROW LEVEL SECURITY;
-- RLS: SELECT cualquier miembro, INSERT/UPDATE/DELETE admin/gerente
```

### 6.7 Configuración financiera por tenant

```sql
CREATE TABLE public.configuracion_financiera (
    optica_id                    TEXT PRIMARY KEY REFERENCES public.opticas(id),
    -- Objetivos de negocio
    margen_neto_objetivo         NUMERIC DEFAULT 15.0,    -- "de cada S/ 100, quiero que me queden S/ X"
    ticket_promedio_objetivo     NUMERIC,                  -- "quiero que cada venta sea al menos S/ X"
    -- Alertas tempranas
    caida_ventas_alerta_pct      NUMERIC DEFAULT 10.0,    -- "avisame si las ventas bajaron X%"
    deuda_vieja_alerta_dias      INTEGER DEFAULT 30,       -- "avisame si alguien debe hace más de X días"
    deuda_total_alerta_monto     NUMERIC DEFAULT 3000.0,   -- "avisame si la deuda total pasa S/ X"
    stock_estancado_alerta_dias  INTEGER DEFAULT 180,      -- "avisame si una montura no se vende hace X días"
    stock_bajo_alerta_unidades   INTEGER DEFAULT 2,        -- "avisame si quedan menos de X unidades"
    -- Umbrales para recomendaciones
    min_ventas_para_recomendar   INTEGER DEFAULT 5,        -- no recomendar sobre < 5 ventas
    -- Periodicidad
    frecuencia_recalculo_dias    INTEGER DEFAULT 1          -- cada cuántos días recalcular márgenes y resumen
);

ALTER TABLE public.configuracion_financiera ENABLE ROW LEVEL SECURITY;
-- SELECT: cualquier miembro. INSERT/UPDATE/DELETE: admin/gerente.
```

### 6.8 Snapshots de inventario

Igual que la versión anterior. Se puebla automáticamente al calcular `resumen_diario`.

### 6.9 Feedback de recomendaciones

```sql
CREATE TABLE public.feedback_recomendaciones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    optica_id           TEXT NOT NULL REFERENCES public.opticas(id),
    recomendacion_id    TEXT NOT NULL,      -- hash del mensaje de recomendación
    fue_util            BOOLEAN NOT NULL,
    fecha               TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.feedback_recomendaciones ENABLE ROW LEVEL SECURITY;
```

### 6.10 Decisiones a confirmar antes de esta fase

1. **Costos de lentes**: ¿se compran a un laboratorio externo? ¿Hay una lista de precios de cada tipo de lente? Esto determina si `costo_unitario_snapshot` se captura automáticamente o el dueño lo carga manualmente.
2. **Histórico de inventario**: ¿hay movimientos de stock históricos (`montura_movimientos`) para calcular rotación hacia atrás, o solo de ahora en adelante?
3. **Gastos fijos**: ¿el dueño quiere cargar sus gastos mensuales (alquiler, sueldos) para ver ganancia neta real, o prefiere arrancar solo con margen bruto (ventas - costo de productos)?

**Entregable de Fase 6**:
1. Migraciones SQL de todas las tablas + RLS.
2. Room: entidades `CategoriaProducto`, `MargenPorCategoria`, `ResumenDiario`, `CostoProducto`, `ConfiguracionFinanciera`.
3. DAOs correspondientes.
4. Función SQL `recalcular_resumen_diario(optica_id, fecha)` que popula `resumen_diario` y `margen_por_categoria`.

---

## FASE 7 — Motor de indicadores en lenguaje de negocio

**Objetivo**: cada indicador responde una pregunta concreta del dueño, en lenguaje llano, siempre comparado con algo.

### 7.1 Los 8 indicadores principales

Cada indicador se define con: **pregunta que responde → cálculo → cómo se muestra → contra qué se compara**.

---

#### Indicador 1: "¿Cuánta plata entró este mes?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Plata que entró" |
| **Cálculo** | `SUM(ventas.monto_total)` en el mes |
| **Se muestra como** | "Este mes vendiste **S/ 15,400**" |
| **Comparación** | "El mes pasado fueron S/ 13,200 → **subió 17%** ↗" |
| **Fuente SQL** | `resumen_diario.ventas_monto_total` agrupado por mes |

---

#### Indicador 2: "¿Cuánta plata cobré realmente?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Plata que cobraste" |
| **Cálculo** | `SUM(pagos.monto)` en el mes |
| **Se muestra como** | "Cobraste **S/ 12,100** de ese total" |
| **Comparación** | "Quedan **S/ 3,300** por cobrar" |
| **Fuente SQL** | `resumen_diario.cobros_monto_total` agrupado por mes |

---

#### Indicador 3: "¿De cada S/ 100 que vendo, cuánto me queda?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Lo que te queda" |
| **Cálculo** | `(ventas_monto_total - ventas_costo_total - gastos_del_mes) / ventas_monto_total * 100` |
| **Se muestra como** | "De cada S/ 100 que vendés, te quedan **S/ 18** después de pagar productos y gastos" |
| **Comparación** | "Tu objetivo es S/ 15. Vas **3 puntos arriba** 👍" |
| **Fuente SQL** | `resumen_diario` + `gastos_operativos` |
| **Nota** | Si no hay gastos cargados, mostrar solo margen bruto: "De cada S/ 100, S/ 45 son ganancia antes de gastos." |

---

#### Indicador 4: "¿Qué productos me dejan más plata?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Lo que más te deja" |
| **Cálculo** | `margen_por_categoria` ordenado por `margen_bruto DESC` |
| **Se muestra como** | "**Lentes progresivos**: vendiste 12 por S/ 4,800, ganancia S/ 2,160 (45%). **Monturas premium**: 8 por S/ 3,200, ganancia S/ 960 (30%)." |
| **Comparación** | "El mes pasado los progresivos te dejaban 42% → mejoraste 3 puntos." |
| **Fuente SQL** | `margen_por_categoria` |

---

#### Indicador 5: "¿Quién me debe y hace cuánto?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Clientes que te deben" |
| **Cálculo** | `ventas` con `monto_total - SUM(pagos) > 0`, agrupado por paciente, ordenado por antigüedad |
| **Se muestra como** | "**3 clientes** te deben **S/ 3,300** en total. El más antiguo: **Juan Pérez** — S/ 1,200, hace **60 días**." |
| **Comparación** | "Hace un mes te debían S/ 2,100. La deuda subió 57%." |
| **Fuente SQL** | `ventas` + `pagos` JOIN `pacientes` |
| **Acción inmediata** | "Llamá a Juan al 999-888-777." |

---

#### Indicador 6: "¿Cuánta plata voy a tener disponible?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Plata que vas a tener" |
| **Cálculo** | Proyección 30 días: `SUM(pagos_esperados)` − `SUM(gastos_programados)` |
| **Se muestra como** | "En los próximos 30 días, estimamos que vas a cobrar **S/ 8,000** y pagar **S/ 5,500**. Te quedarían **S/ 2,500**." |
| **Comparación** | — (es proyección) |
| **Fuente SQL** | `pagos` tendencia reciente + `gastos_operativos.fecha_programada` |
| **Warning** | Si histórico < 12 meses: "Este cálculo se basa en pocos meses. Podría no ser preciso." |

---

#### Indicador 7: "¿Hace cuánto tengo esto sin vender?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Productos sin vender" |
| **Cálculo** | Monturas en stock con última venta > `stock_estancado_alerta_dias` |
| **Se muestra como** | "Tenés **4 monturas** que no se venden hace **más de 6 meses**. Te costaron S/ 580 en total." |
| **Comparación** | "El mes pasado eran 6 → vendiste 2." |
| **Fuente SQL** | `monturas` + `montura_movimientos` (última salida) |
| **Acción inmediata** | "Considerá liquidarlas con descuento para recuperar capital." |

---

#### Indicador 8: "¿Cuánto vale mi inventario?"

| Atributo | Valor |
|---|---|
| **Nombre en UI** | "Valor de tu stock" |
| **Cálculo** | `SUM(monturas.costo * stock_actual)` |
| **Se muestra como** | "Tu inventario vale **S/ 12,400** (85 monturas en stock)." |
| **Comparación** | "El mes pasado valía S/ 13,100. Vendiste más de lo que compraste." |
| **Fuente SQL** | `monturas` |

---

### 7.2 Funciones SQL del motor

```sql
-- Función principal: calcular todos los indicadores para un mes
CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(
    p_optica_id TEXT,
    p_mes DATE  -- primer día del mes, ej. '2026-07-01'
) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER
AS $$
DECLARE
    v_ventas_mes NUMERIC;
    v_cobros_mes NUMERIC;
    v_costo_mes NUMERIC;
    v_gastos_mes NUMERIC;
    v_saldo_pendiente NUMERIC;
    v_margen_neto_pct NUMERIC;
    v_ticket_promedio NUMERIC;
    v_cantidad_ventas INTEGER;
    v_mes_anterior DATE;
    v_ventas_mes_anterior NUMERIC;
BEGIN
    v_mes_anterior := p_mes - INTERVAL '1 month';

    -- Ventas, cobros y costos del mes desde resumen_diario
    SELECT COALESCE(SUM(ventas_monto_total), 0),
           COALESCE(SUM(cobros_monto_total), 0),
           COALESCE(SUM(ventas_costo_total), 0),
           COALESCE(SUM(ventas_cantidad), 0)
    INTO v_ventas_mes, v_cobros_mes, v_costo_mes, v_cantidad_ventas
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= p_mes
      AND fecha < p_mes + INTERVAL '1 month';

    -- Ticket promedio
    v_ticket_promedio := CASE WHEN v_cantidad_ventas > 0
        THEN v_ventas_mes / v_cantidad_ventas ELSE 0 END;

    -- Gastos del mes
    SELECT COALESCE(SUM(monto), 0) INTO v_gastos_mes
    FROM public.gastos_operativos
    WHERE optica_id = p_optica_id AND fecha >= p_mes AND fecha < p_mes + INTERVAL '1 month';

    -- Saldo pendiente al cierre del mes
    SELECT COALESCE(saldo_pendiente_total, 0) INTO v_saldo_pendiente
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id AND fecha < p_mes + INTERVAL '1 month'
    ORDER BY fecha DESC LIMIT 1;

    -- Margen neto
    v_margen_neto_pct := CASE WHEN v_ventas_mes > 0
        THEN ROUND(((v_ventas_mes - v_costo_mes - v_gastos_mes) / v_ventas_mes) * 100, 1)
        ELSE 0 END;

    -- Ventas mes anterior para comparación
    SELECT COALESCE(SUM(ventas_monto_total), 0) INTO v_ventas_mes_anterior
    FROM public.resumen_diario
    WHERE optica_id = p_optica_id
      AND fecha >= v_mes_anterior
      AND fecha < p_mes;

    RETURN jsonb_build_object(
        'ventas_mes', v_ventas_mes,
        'cobros_mes', v_cobros_mes,
        'costo_mes', v_costo_mes,
        'gastos_mes', v_gastos_mes,
        'saldo_pendiente', v_saldo_pendiente,
        'margen_neto_pct', v_margen_neto_pct,
        'ticket_promedio', v_ticket_promedio,
        'cantidad_ventas', v_cantidad_ventas,
        'ventas_mes_anterior', v_ventas_mes_anterior,
        'variacion_ventas_pct', CASE WHEN v_ventas_mes_anterior > 0
            THEN ROUND(((v_ventas_mes - v_ventas_mes_anterior) / v_ventas_mes_anterior) * 100, 1)
            ELSE NULL END
    );
END;
$$;

-- Función: quién debe y hace cuánto
CREATE OR REPLACE FUNCTION public.rpc_deudores(
    p_optica_id TEXT
) RETURNS TABLE(
    paciente_nombre TEXT,
    paciente_telefono TEXT,
    venta_id TEXT,
    venta_fecha DATE,
    monto_total NUMERIC,
    total_pagado NUMERIC,
    saldo NUMERIC,
    dias_deuda INTEGER
)
LANGUAGE sql SECURITY INVOKER STABLE
AS $$
    SELECT
        COALESCE(p.nombre_completo, 'Sin paciente'),
        p.telefono,
        v.id,
        v.fecha,
        v.monto_total,
        COALESCE(SUM(pg.monto), 0) AS total_pagado,
        v.monto_total - COALESCE(SUM(pg.monto), 0) AS saldo,
        CURRENT_DATE - v.fecha AS dias_deuda
    FROM public.ventas v
    LEFT JOIN public.pacientes p ON p.id = v.paciente_id
    LEFT JOIN public.pagos pg ON pg.venta_id = v.id
    WHERE v.optica_id = p_optica_id
    GROUP BY v.id, v.fecha, v.monto_total, p.nombre_completo, p.telefono
    HAVING v.monto_total - COALESCE(SUM(pg.monto), 0) > 0.005
    ORDER BY dias_deuda DESC;
$$;
```

### 7.3 Ubicación del cálculo

- **Supabase**: funciones SQL (las de arriba) que devuelven JSON listo para mostrar.
- **Android**: `ObtenerAnalisisMensualUseCase` consulta Room local (`resumen_diario`) offline, y sincroniza desde Supabase al reconectar.
- **OptoWeb**: consume `rpc_analisis_mensual` y `rpc_deudores`.

### 7.4 Actualizar RPCs existentes

| RPC actual | Acción |
|---|---|
| `rpc_resumen_financiero` | Deprecar. Reemplazado por `rpc_analisis_mensual` |
| `rpc_saldo_pendiente` | Deprecar. Reemplazado por JOIN `ventas + pagos` |
| `rpc_cierre_caja_resumen` | Sin cambios |
| `rpc_count_pendientes` | Actualizar para usar `ventas` |

**Entregable de Fase 7**:
1. Funciones SQL: `rpc_analisis_mensual`, `rpc_deudores`, `recalcular_resumen_diario`.
2. Room: `ResumenDiarioDao` con queries de agregación.
3. Android: `ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`.
4. Tests unitarios de cada indicador.

---

## FASE 8 — Recomendaciones que sirven (específicas, priorizadas, accionables)

**Principio**: una recomendación genérica es ruido. Si no dice exactamente qué hacer, con qué cliente, con qué producto, y cuánta plata implica, no sirve.

### 8.1 Estructura de una recomendación

```kotlin
data class Recomendacion(
    val id: String,                  // hash único para feedback
    val tipo: RecomendacionTipo,
    val titulo: String,              // una frase, lenguaje llano
    val detalle: String,             // 2-3 oraciones con nombres y montos concretos
    val impactoEstimado: String?,    // "Impacto estimado: +S/ 320 este mes"
    val prioridad: Prioridad,        // ALTA, MEDIA, BAJA
    val accion: String?,             // qué hacer, paso a paso
    val datosAccion: DatosAccion?    // ids, montos, teléfonos para botones de acción
)

enum class RecomendacionTipo {
    COBRAR, MEJORAR_PRECIO, LIQUIDAR_STOCK,
    VENDER_MAS_DE, REDUCIR_GASTO, ALERTA_CAIDA
}

enum class Prioridad { ALTA, MEDIA, BAJA }

data class DatosAccion(
    val pacienteIds: List<String>? = null,     // para "llamar a estos clientes"
    val productoIds: List<String>? = null,     // para "liquidar estos productos"
    val montoTotal: Double? = null             // para mostrar impacto
)
```

### 8.2 Las 6 reglas de recomendación (reescritas)

#### Regla 1: "Hay plata que deberías cobrar hoy"

| Atributo | Valor |
|---|---|
| **Disparador** | Deuda total > `deuda_total_alerta_monto` O existe cliente con deuda > `deuda_vieja_alerta_dias` |
| **Prioridad** | ALTA |
| **Título** | "Tenés S/ 3,300 para cobrar de 3 clientes" |
| **Detalle** | "Juan Pérez debe S/ 1,200 (60 días). María García debe S/ 800 (45 días). Carlos López debe S/ 1,300 (20 días). Total: S/ 3,300." |
| **Acción** | "Llamalos hoy. Juan: 999-888-777. María: 999-666-555." |
| **Impacto** | "Si cobrás todo, tu caja disponible sube a S/ X." |

#### Regla 2: "Este producto te está haciendo perder plata"

| Atributo | Valor |
|---|---|
| **Disparador** | Categoría con margen < 10% y ≥ `min_ventas_para_recomendar` ventas |
| **Prioridad** | ALTA |
| **Título** | "Monturas Económicas te dejan solo S/ 8 de cada S/ 100" |
| **Detalle** | "Vendiste 8 monturas económicas este mes a S/ 120 c/u. Te costaron S/ 110. Te quedan S/ 10 por cada una. Si las vendieras a S/ 150, ganarías S/ 320 más por mes." |
| **Acción** | "Subí el precio de las monturas económicas a S/ 150. O dejá de comprar ese modelo." |
| **Impacto** | "Impacto estimado: +S/ 320 este mes." |

#### Regla 3: "Tenés plata parada en el inventario"

| Atributo | Valor |
|---|---|
| **Disparador** | Montura sin vender por > `stock_estancado_alerta_dias` |
| **Prioridad** | MEDIA |
| **Título** | "4 monturas no se venden hace más de 6 meses" |
| **Detalle** | "Montura Ray-Ban RX123 (S/ 180), Montura Oakley OX456 (S/ 150), etc. Tenés S/ 580 inmovilizados." |
| **Acción** | "Ofrecelas con 20% de descuento. Recuperás S/ 464. O devolvelas al proveedor si acepta." |
| **Impacto** | "Recuperás S/ 464 que podés usar para comprar modelos que sí se venden." |

#### Regla 4: "Este producto es tu estrella — vendé más"

| Atributo | Valor |
|---|---|
| **Disparador** | Categoría con margen > 35% y margen_bruto > 25% del total, ≥ `min_ventas_para_recomendar` ventas |
| **Prioridad** | MEDIA |
| **Título** | "Lentes Progresivos son tu producto estrella" |
| **Detalle** | "Vendiste 12 pares este mes con 45% de margen. Te dejaron S/ 2,160 — el 35% de toda tu ganancia." |
| **Acción** | "Cuando un paciente viene solo por la consulta, ofrecé un descuento del 10% en progresivos. Cada venta adicional te deja ~S/ 180." |
| **Impacto** | "+3 ventas de progresivos = ~S/ 540 más por mes." |

#### Regla 5: "Las ventas vienen bajando"

| Atributo | Valor |
|---|---|
| **Disparador** | `ventas_mes < ventas_mes_anterior` en > `caida_ventas_alerta_pct` |
| **Prioridad** | ALTA |
| **Título** | "Este mes vendiste 15% menos que el mes pasado" |
| **Detalle** | "Julio: S/ 9,400. Junio: S/ 11,100. La categoría que más bajó fue Monturas Estándar (de S/ 3,200 a S/ 1,800)." |
| **Acción** | "Revisá si tuviste menos pacientes este mes. Si es estacional, bajá gastos discrecionales. Si no, revisá precios de monturas." |
| **Impacto** | — |

#### Regla 6: "Gastos que podrías reducir"

| Atributo | Valor |
|---|---|
| **Disparador** | Gastos del mes > 40% de ventas del mes |
| **Prioridad** | MEDIA |
| **Título** | "Tus gastos se comen el 42% de lo que vendés" |
| **Detalle** | "Este mes gastaste S/ 3,900 en alquiler, personal e insumos. Tus ventas fueron S/ 9,400." |
| **Acción** | "Revisá la categoría 'marketing'. Este mes gastaste S/ 500. ¿Trajo pacientes?" |
| **Impacto** | "Reducir 10% los gastos = +S/ 390 por mes." |

### 8.3 Priorización

El motor genera todas las recomendaciones que apliquen y las ordena:

1. **ALTA**: deuda vieja, caída de ventas, producto con pérdida.
2. **MEDIA**: stock estancado, estrella para vender más, gastos altos.
3. **BAJA**: (fase futura) optimizaciones menores.

Máximo 5 recomendaciones visibles. Si hay más, se muestran las 5 de mayor prioridad.

### 8.4 Feedback loop

Cada recomendación tiene botones 👍 "Útil" / 👎 "No me sirve". El feedback se guarda en `feedback_recomendaciones` y a futuro permite:
- Dejar de mostrar recomendaciones que el dueño siempre marca como inútiles.
- Aprender qué tipo de recomendaciones valora más cada óptica.
- Entrenar un modelo simple de relevancia (fase futura).

**Entregable de Fase 8**:
1. `GenerarRecomendacionesUseCase` con las 6 reglas + priorización.
2. `Recomendacion` data class + `RecomendacionTipo` enum.
3. Tests unitarios: una prueba por regla con datos sintéticos.
4. `FeedbackRecomendacionUseCase` para guardar 👍/👎.

---

## FASE 9 — UI/UX: de los datos a las decisiones

**Principio**: el dueño abre esto en el celular entre paciente y paciente. Tiene 30 segundos. La pantalla principal debe responder "¿cómo estoy?" en 5 segundos y "¿qué hago?" en 30.

### 9.1 Pantalla principal: "Tu negocio en 30 segundos"

```
┌─────────────────────────────────┐
│  [← Menú]     Análisis    [Jul] │  ← mes actual, swipe horizontal para cambiar
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │  Este mes vendiste        │  │  ← card de resumen en lenguaje llano
│  │  S/ 15,400                │  │
│  │  Cobraste S/ 12,100       │  │
│  │  Te quedan S/ 3,300 por   │  │
│  │  cobrar                   │  │
│  │                           │  │
│  │  De cada S/ 100 que       │  │
│  │  vendés, te quedan S/ 18  │  │
│  │  ↑ 3 pts vs mes pasado    │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  ⚠ Llamá a estos clientes │  │  ← recomendación #1 (ALTA prioridad)
│  │  Juan Pérez debe S/ 1,200 │  │
│  │  hace 60 días             │  │
│  │  📞 999-888-777           │  │  ← botón llama directo
│  │         [Ya llamé]        │  │  ← botón dismiss
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  💡 Lentes Progresivos    │  │  ← recomendación #2 (MEDIA)
│  │  son tu producto estrella │  │
│  │  45% margen · 35% de tu   │  │
│  │  ganancia                 │  │
│  │     [Entendido]  👍  👎   │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │  📦 4 monturas sin vender │  │  ← recomendación #3 (MEDIA)
│  │  hace 6+ meses            │  │
│  │  Sugerencia: liquidar con │  │
│  │  20% descuento            │  │
│  │     [Ver cuáles]  👍  👎  │  │
│  └───────────────────────────┘  │
│                                 │
│  [📊 Ver análisis completo]     │
│                                 │
└─────────────────────────────────┘
```

**Densidad**: máximo 3 recomendaciones en la pantalla principal. El resto en "Ver todas".

**Swipe horizontal** en el selector de mes (Jun ← **Jul** → Ago) para navegar en el tiempo.

### 9.2 Pantalla secundaria: "Entendé tu negocio"

Accesible desde "Ver análisis completo". Organizada en secciones expandibles:

```
┌─────────────────────────────────┐
│  ← Análisis               Julio │
├─────────────────────────────────┤
│                                 │
│  ▼ PLATA QUE ENTRÓ Y SALIÓ     │  ← sección expandida por defecto
│  ┌───────────────────────────┐  │
│  │  ████████████ S/ 15,400   │  │  ← barra de ventas
│  │  ██████████   S/ 12,100   │  │  ← barra de cobros
│  │  ██████       S/ 9,800    │  │  ← barra de costos
│  │  ███          S/ 3,900    │  │  ← barra de gastos
│  │  ─────────────────────    │  │
│  │  ██           S/ 1,700    │  │  ← ganancia neta
│  └───────────────────────────┘  │
│                                 │
│  ▶ LO QUE MÁS TE DEJA          │  ← colapsada, expande al tocar
│                                 │
│  ▶ CLIENTES QUE TE DEBEN       │  ← colapsada
│                                 │
│  ▶ PRODUCTOS SIN VENDER        │  ← colapsada
│                                 │
│  ▶ PLATA QUE VAS A TENER       │  ← colapsada, proyección 30 días
│                                 │
└─────────────────────────────────┘
```

Cada sección expandida muestra el detalle con nombres, montos y —cuando aplica— un botón de acción ("Llamar", "Ver producto", "Ajustar precio").

### 9.3 Widget de acceso rápido (fase futura deseable)

Un widget en el launcher del celular que muestre:
- "Hoy: S/ X vendido"
- "Por cobrar: S/ Y"
- Toque → abre la pantalla principal del módulo.

### 9.4 Estilo visual

- Colores OptoApp existentes: `#2C3E50` (texto principal), `#27AE60` (positivo/ganancia), `#E74C3C` (alertas/deuda).
- Cards con 12dp de radio.
- Tipografía: un solo tamaño grande para el número principal, cuerpo para el detalle. Sin tablas densas — esto es mobile, no Excel.
- Modo oscuro compatible (ya implementado en OptoApp).
- Material 3.

### 9.5 Restricción de acceso

El módulo completo solo es visible para roles `admin` y `gerente`. La sección del drawer `"estadisticas_bi"` —ya existente— se renombra a `"Mi Negocio"` o se crea una nueva entrada `"analisis"` que reemplace a la actual de BI.

**Entregable de Fase 9**:
1. `AnalisisNegocioScreen` (pantalla principal).
2. `AnalisisDetalleScreen` (pantalla secundaria con secciones expandibles).
3. `AnalisisNegocioViewModel` que orquesta los UseCases de Fase 7 y 8.
4. Refactor del drawer: la entrada BI actual redirige al nuevo módulo.

---

## FASE 10 — Entregables finales y criterios de aceptación

### Entregables completos (Parte A + B)

1. Migraciones SQL: `ventas` + triggers + backfill + RLS (Fase 1).
2. Migraciones SQL: `pagos.venta_id` + backfill (Fase 1).
3. Migraciones SQL: `categorias_producto`, `margen_por_categoria`, `resumen_diario`, `costos_productos`, `configuracion_financiera`, `feedback_recomendaciones` + RLS (Fase 6).
4. Room: `Venta`, `CategoriaProducto`, `MargenPorCategoria`, `ResumenDiario`, `CostoProducto`, `ConfiguracionFinanciera`, `Recomendacion`.
5. DAOs con queries de agregación para modo offline.
6. Funciones SQL: `rpc_analisis_mensual`, `rpc_deudores`, `recalcular_resumen_diario`.
7. Use cases Android: `ObtenerAnalisisMensualUseCase`, `ObtenerDeudoresUseCase`, `GenerarRecomendacionesUseCase`, `ProyectarFlujoCajaUseCase`, `FeedbackRecomendacionUseCase`.
8. Composables: `AnalisisNegocioScreen`, `AnalisisDetalleScreen`.
9. Tests unitarios: todos los indicadores de Fase 7 y reglas de Fase 8.
10. Documentación para el usuario (en lenguaje llano): cómo interpretar cada sección del análisis.

### Criterios de aceptación

- ✅ **Lenguaje llano**: ningún KPI usa términos financieros sin explicación. "Margen" siempre se muestra como "de cada S/ 100, te quedan S/ X".
- ✅ **Todo cálculo usa `ventas`**: nunca se consulta `dispensaciones`/`servicios_extra` para ingresos.
- ✅ **Recomendaciones específicas**: mencionan nombres de clientes, productos, montos y acciones concretas.
- ✅ **Recomendaciones priorizadas**: las de mayor impacto económico aparecen primero.
- ✅ **Comparación temporal**: cada indicador se compara contra el mes anterior y muestra variación.
- ✅ **Margen histórico usa `costo_unitario_snapshot`**, no costo vigente.
- ✅ **Umbrales configurables**: todos los disparadores se leen de `configuracion_financiera`.
- ✅ **Proyección de caja advierte** si el histórico es insuficiente para estacionalidad.
- ✅ **Modo offline**: consulta Room local, sincroniza al reconectar. Las recomendaciones se generan localmente con datos cacheados.
- ✅ **Acceso restringido** a roles admin/gerente.
- ✅ **Feedback loop**: 👍/👎 en cada recomendación, persistido.

---

## Resumen del orden de implementación

| Fase | Parte | Qué se hace | Depende de |
|---|---|---|---|
| 1 | A | Tabla `ventas` + migración `pagos` + triggers + Room | — |
| 2 | A | Cierre de Caja, Reportes, BI → `ventas` + fix anulaciones | Fase 1 |
| 3 | A | `InformacionFinancieraScreen` (solo Dispensaciones) | Fase 1 |
| 4 | A | Corregir navegación "Ir a Financiero" | Fase 3 |
| 5 | A | Fix espaciado en pantallas | Independiente |
| 6 | B | Esquema de negocio (categorías, márgenes pre-calculados, resumen diario, costos, gastos, config) | Fases 1–2 |
| 7 | B | Motor de 8 indicadores en lenguaje llano + funciones SQL | Fase 6 |
| 8 | B | 6 reglas de recomendación específicas y priorizadas | Fase 7 |
| 9 | B | UI/UX: "tu negocio en 30 segundos" + análisis detallado | Fase 8 |
| 10 | B | QA, tests, documentación y criterios de aceptación | Fases 6–9 |

**Puntos de corte para liberar en producción**: tras Fase 2 (fix del bug visible + ledger funcionando) y tras Fase 4 (UX de pagos completa). Parte B no inicia hasta que Fase 2 esté probada en producción.

---

## Riesgos y decisiones pendientes

| # | Riesgo / Decisión | Estado |
|---|---|---|
| 1 | Origen de costos de lentes: ¿lista de precios de laboratorio disponible? | 🔲 Confirmar antes de Fase 6 |
| 2 | ¿El dueño va a cargar gastos fijos (alquiler, sueldos) o arrancamos solo con margen bruto? | 🔲 Confirmar antes de Fase 6 |
| 3 | Histórico de inventario suficiente para rotación retroactiva | 🔲 Confirmar antes de Fase 6 |
| 4 | `dispensacion_items` no tiene `costo_unitario` — ¿se captura desde `monturas.costo` al crear la venta? | 🔲 Decidir en Fase 6 |
| 5 | Estrategia de recálculo de `resumen_diario` | ✅ Decidido: bajo demanda + caché (Opción D). Sin pg_cron, sin edge functions. La app/dispositivo llama a `recalcular_resumen_diario(optica_id, fecha)` al abrir el análisis. Idempotente con ON CONFLICT DO UPDATE. |
| 6 | Widget de acceso rápido (Fase 9.3) — ¿se prioriza o va a fase futura? | 🔲 Fase futura |
| 7 | `IMPROVEMENT-PLAN.md` referenciado en AGENTS.md no existe | 🔲 Crear o eliminar referencia |
| 8 | Convivencia `pagos.dispensacion_id`/`servicio_extra_id` con `pagos.venta_id` | ✅ Planificado (Fase 1.3) |
| 9 | `?focus=financiero` dead parameter — se elimina en Fase 4 | ✅ Corregido |
| 10 | `monto_pagado`/`a_cuenta` como campos desnormalizados — ¿se eliminan? | 🔲 Post-Fase 2 si `ventas` + `pagos.venta_id` son estables |
