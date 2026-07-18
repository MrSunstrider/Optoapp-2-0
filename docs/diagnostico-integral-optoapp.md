# OptoApp — Diagnóstico Integral y Plan de Tratamiento

> **Auditoría**: Judgment Day — 14-15 Julio 2026
> **Total**: ~290 hallazgos en 17 dominios · 280+ archivos
> **Método**: Dual Blind Review con 2 jueces R1 Security por slice, 34 jueces en total

---

## Índice

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Diagnóstico por Capa](#2-diagnóstico-por-capa)
3. [Patrones Sistémicos](#3-patrones-sistémicos)
4. [Matriz de Riesgo](#4-matriz-de-riesgo)
5. [Plan de Tratamiento](#5-plan-de-tratamiento)
6. [Fase 1: Cirugía de Emergencia (BLOCKERs)](#fase-1-cirugía-de-emergencia)
7. [Fase 2: Estabilización (CRITICALs)](#fase-2-estabilización)
8. [Fase 3: Rehabilitación (HIGH)](#fase-3-rehabilitación)
9. [Fase 4: Fortalecimiento (MEDIUM/LOW)](#fase-4-fortalecimiento)
10. [Recomendaciones Arquitectónicas](#10-recomendaciones-arquitectónicas)

---

## 1. Resumen Ejecutivo

OptoApp es un SaaS de gestión óptica con arquitectura offline-first (Room → Supabase) y multi-tenancy por `optica_id`. Tras dos auditorías exhaustivas que cubrieron **280+ archivos en 17 dominios**, el diagnóstico revela un proyecto con **fundamentos sólidos pero con deuda técnica crítica acumulada** que compromete la seguridad multi-tenant, la integridad financiera y la estabilidad operativa.

### Hallazgos por severidad (Judgment Day solamente)

| Severidad | Count | Descripción |
|-----------|:---:|-------------|
| 🔴 BLOCKER | 18 | Service key en APK, PIN bypass, notificaciones PII, backup corrupto, feature roto (roles), RPCs rotos, billing client-only, delete masivo regalos |
| 🔴 CRITICAL | 46 | Multi-tenant (15+ DAOs), costo=0 sistémico, merge no atómico, race conditions, tokens en logs, role default admin, RLS permisivo |
| 🟠 WARNING | 150+ | `runBlocking` (5+ sitios), `Double` para dinero, sync state no transaccional, authz frontend-only, logs con datos sensibles |
| 🟡 SUGGESTION | 70+ | Dead code (248 líneas), `fmt()` duplicado, accesibilidad, god objects, backoff lineal

### Salud del proyecto por dimensión

| Dimensión | Estado | Tendencia |
|-----------|--------|-----------|
| **Seguridad Multi-tenant** | 🔴 Crítico | 30+ DAOs sin filtro `optica_id`, datos entre ópticas accesibles |
| **Integridad Financiera** | 🔴 Crítico | `costo=0.0` sistémico, audit trail corrupto, merge no atómico |
| **Autenticación** | 🟠 Alto | PIN bypass en app kill, tokens en logs, roles default a admin |
| **Sync Offline-First** | 🟠 Alto | Race conditions, TOCTOU, dead merge code, 248 líneas muertas |
| **Backend Supabase** | 🟠 Alto | RPCs rotos, RLS permisivo, tabla `ventas` droppeada con referencias vivas |
| **Arquitectura** | 🟡 Medio | 4 god objects, `runBlocking` en 5+ sitios, Clean Architecture violada |
| **Testing** | 🟡 Medio | 25 stubs `assertTrue(true)`, migraciones no cubiertas, 8.9% cobertura |
| **UX/Accesibilidad** | 🟡 Medio | Sin loading states, sin offline banner, contentDescription nulo, touch targets < 48dp |
| **Schema Drift** | 🟠 Alto | 7 tablas con UUID vs TEXT PK, 7 boolean mismatches, defaults divergentes |
| **Monetización** | 🔴 Crítico | Suscripción client-only, sin validación backend, service key en APK |

---

## 2. Diagnóstico por Capa

### 2.1 Capa de Datos (Room DAOs + Entidades)

**Problema raíz**: El contrato multi-tenant (`optica_id`) no se aplica consistentemente. De ~60 DAOs, al menos **35 tienen métodos sin filtro `optica_id`**.

| Tipo de brecha | Cantidad | Ejemplos |
|----------------|:---:|----------|
| `getById` sin `opticaId` | 15+ | PacienteDao, EvaluacionDao, DispensacionDao, ServicioExtraDao, ProveedorDao, OrdenCompraDao, InventarioFisicoDao |
| Métodos deprecated sin reemplazo | 14+ | `getAllPacientes`, `getAllDispensaciones`, `searchPacientes`, `getAllServicios` |
| DELETE sin `opticaId` | 6 | PacienteDao, DispensacionItemDao, GastoOperativoDao, DispensacionDao, OrdenCompraItemDao |
| `deleteAll()` sin WHERE | 9 | Varios DAOs — wipe total de datos si se invoca |
| Lookup queries sin `opticaId` | 3 | CostoProductoDao.lookup(), CostoBiseladoDao.lookup(), CostoProductoDao.lookupLc() |
| Reassign queries sin `opticaId` | 3 | PacienteDao (evaluaciones, dispensaciones, servicios_extra) |

**Impacto**: Un usuario autenticado de Óptica A puede leer, modificar y potencialmente eliminar datos de Óptica B si conoce un UUID. En producción con múltiples ópticas activas, esto es una **brecha de datos multi-tenant activa**.

**Otros problemas de capa de datos**:
- Entidades monetarias usan `Double` (IEEE 754) → error de redondeo acumulado en sumas
- `ResumenDiarioEntity` sin unique constraint `(opticaId, fecha)` → duplicados en fresh install
- `ConflictRecord` PK es solo `entityId` → cross-tenant overwrite de conflictos
- `@Update` matchea por PK sin `opticaId` → bypass de tenant en updates
- FKs sin `onDelete` en `OrdenCompraItem`, `MonturaProveedor` → huérfanos

### 2.2 Capa de Dominio (Use Cases + Modelos)

**Problema raíz**: El modelo financiero canónico `MovimientoFinanciero` tiene `costo = 0.0` hardcodeado. **Todos los cálculos de margen son incorrectos**.

- `ObtenerMovimientosFinancierosUseCase`: costo=0 para dispensaciones y servicios (líneas 44, 61)
- `TipoMovimiento.REGALO` y `Origen.REGALO` declarados pero **nunca poblados** — regalos invisibles para márgenes
- `parseSnellenToLogMar` hardcodea 20ft → diagnóstico de ambliopía incorrecto para usuarios de notación 6m
- `autoPresbicia` y `autoAnisometropia` descartados en `toEvaluacionClinica()` → datos clínicos perdidos
- `proximaFechaControl` nunca poblado desde Android → pérdida en sync bidireccional
- `SyncFinanzasUploaders.kt` (248 líneas) es código muerto — sin `@Inject`, nunca instanciado

### 2.3 Capa de Sincronización

**Problema raíz**: Infraestructura sólida conceptualmente pero con bugs que producen pérdida de datos y estados inconsistentes.

**Upload**:
- `CostosBiselado` tiene `toRemoto()` pero **falta `safeUpload("costos_biselado")`** → nunca se suben
- `safeUpload` atrapa `RestException` antes que `IOException` si heredan → 401/403 silenciados
- `uploadSessions` reporta `list.size` en vez de `safeRows.size` → métricas falsas
- Subida de entidades hijo (pagos) continúa aunque padre (dispensacion) falló → FK violations silenciosas
- Sync state no se marca tras batch parcial → re-upload innecesario

**Download**:
- `downloadResumenDiario` y `downloadConfiguracionFinanciera` sin `networkRetryHelper` → fallan en redes flojas
- `downloadDetalles` ignora `sessionIds` → descarga TODOS los detalles, no solo los relevantes
- `upsertServicioFromRemote` usa `insertServicio` en vez de upsert → crash en re-download

**Conflict Resolution**:
- `ThreeWayMerge` (87 líneas) es código muerto: `baseSnapshot` siempre `"{}"` → nunca se ejecuta
- `ConflictRecord` PK solo `entityId` → cross-tenant overwrite
- `resolveKeepMine` hace bump fuera del `syncGate` mutex → race condition
- `acceptAllCloud` borra conflictos **antes** de verificar que el download funciona → pérdida irreversible
- Conflict resolution usa `sessionManager.opticaId` en vez del `ConflictRecord.opticaId` → falla silenciosa al cambiar de óptica

**Infraestructura**:
- `SyncViewModel` (850 líneas) — god object con 8+ responsabilidades
- `PostSaveSyncScheduler`: TOCTOU en `suppressSync` — sync se cuela durante download
- `SyncStateTracker` no transaccional con escrituras de entidad
- `NetworkRetryHelper`: backoff lineal (no exponencial), sin jitter adecuado
- `ensureSyncContext` permite download sin verificar membresía en IOException

### 2.4 Capa de Presentación (ViewModels + Screens)

**Problema raíz**: Autorización delegada a la UI, sin enforcement en ViewModels. Errores silenciados.

- **15+ ViewModels sin verificación de rol**: `savePaciente`, `saveEvaluacion`, `deleteMontura`, `registrarSalida/Entrada` no validan `canEdit/create/delete`
- **`save()` en `InformacionFinancieraViewModel` sin error handling**: crash o UI congelada permanente
- **Doble-tap en save sin guard**: pagos duplicados, doble navegación
- **`runBlocking` en 5+ ViewModels**: `InformacionFinancieraVM`, `DispensacionVM`, `ServiciosVM`, `CostosYGastosVM` — bloquea IO dispatcher
- **Estados stale en stock**: `MonturasViewModel` usa objeto de UI (stale) para calcular `stockPrevio/Nuevo`
- **Regalos**: borrar un regalo borra TODOS (`deleteByDispensacionId` en vez de `deleteById`)
- **DispensacionViewModel**: delete/anular sin transacción, reclamo sin items, costos silenciosamente null
- **`collectAsState(initial = "admin")`** en 9+ pantallas → default-allow durante carga de rol

### 2.5 Capa de Autenticación y Seguridad

**PIN**:
- **BLOCKER**: Contador de fuerza bruta en memoria → reset en kill de app → intentos ilimitados
- PIN en `StateFlow<String>` → comparable en memoria → extraíble con volcado de proceso
- `pinHasBeenSet` en DataStore sin encriptar → metadata leak
- `clearSession()` no resetea `PIN_HAS_BEEN_SET`

**Sesión**:
- `SessionManager.getSecureOpticaRol()` default `"admin"` → escalación de privilegios si keystore falla
- `LEGACY_OPTICA_ID = "mi_optica_base"` hardcodeado → cross-tenant si el opticaId es blank
- `lastLoginTimestamp` en DataStore sin encriptar → manipulable
- `enableLifecycleCallbacks = false` → refresh manual de token con cobertura incompleta

**Logging**:
- Token de recovery en `Log.d` (deep link URL completa)
- Token OAuth en `Log.d`
- Access token en campo `private var` del singleton AuthDelegate
- Email/UID en `Log.e` (MembershipDataSource)
- Patient IDs, opticaId en `Log.e` (múltiples repositorios)

**Deep Links**:
- Sin validación de origen en recovery/OAuth deep links — cualquier app puede inyectar tokens

### 2.6 Backend Supabase

**Triggers y RPCs**:
- `rpc_saldo_pendiente` referencia `public.ventas` (tabla droppeada) → crash en runtime
- `assign_optica_role_by_email` **revocado de `authenticated`** → feature de roles roto en producción
- `recalcular_resumen_diario` es `SECURITY INVOKER` + tabla sin INSERT policy → writes silenciosamente fallan
- `trg_pagos_update_monto_pagado`: race condition en `SUM(monto)` sin `FOR UPDATE`
- `rpc_analisis_mensual.proyeccion_caja`: sin fallback `COALESCE` para `venta_id` → pagos históricos excluidos
- `create_optica_for_current_user`: `ON CONFLICT DO UPDATE` → si se conoce ID, se sobrescribe óptica existente
- `opticas_update_member` RLS permite UPDATE a **cualquier miembro** → datos fiscales/lab modificables por invitado

**RLS**:
- `resumen_diario`: solo SELECT policy — sin INSERT/UPDATE/DELETE
- `regalos_dispensacion`: DELETE policy permite a **cualquier member** (incluyendo invitado)
- `margen_por_categoria`: solo SELECT — sin INSERT
- 11 RPCs muertas en DB con grants activos

**Schema Drift (Room ↔ Supabase)**:
- 7 tablas con UUID vs TEXT PK → inserts romperán
- 7 boolean columns: Room INTEGER (0/1), Supabase boolean → coerción puede fallar
- 4 columnas faltantes en Supabase (`monturas`: ancho_mm, puente_mm, etc.)
- Defaults divergentes en 4+ tablas

### 2.7 Monetización y Distribución

- **BLOCKER**: `SUPABASE_TEST_SERVICE_KEY` en `defaultConfig` → compilada en **todos los builds incluyendo release**
- **BLOCKER**: Suscripción client-only — `setProFromLocalCache()` sin validación backend
- Pending purchases configurado para one-time products pero el producto es subscription
- `acknowledgePurchase` callback vacío → PRO se concede aunque falle
- `FREE_MAX_PACIENTES = Int.MAX_VALUE` → tiers cosméticos, sin diferenciación real
- APK download sin HTTPS enforcement ni verificación de firma (UpdateChecker)

### 2.8 Widget, Notificaciones y UI Compartida

- **BLOCKER**: Notificaciones muestran nombres de pacientes en lock screen (`IMPORTANCE_HIGH`, sin `VISIBILITY_PRIVATE`)
- Widget: key mismatch (`optica_id` vs `saas_optica_id`) → widget muestra S/ 0.00 siempre
- Widget: `widgetCategory="home_screen"` sin keyguard → datos financieros visibles sin desbloqueo
- `CoroutineScope(Dispatchers.IO)` sin parent Job en AppWidgetProvider → corrutinas leaks
- Drawer navigation: rutas protegidas solo por frontend, `opticaRol` recibido pero no usado
- `OptoSegmentedSelector`: hardcoded 13.sp, 10.dp, sin accessibility semantics, sin keyboard nav
- `contentDescription = null` en todos los iconos del drawer y KPIs
- Touch targets < 48dp en múltiples IconButtons

---

## 3. Patrones Sistémicos

### 3.1 Multi-tenancy Frágil (Patrón #1)
**35+ ubicaciones** donde `optica_id` no se aplica. Causa raíz: los DAOs se escribieron sin el contrato multi-tenant como requisito fundacional. Se agregó `opticaId` como columna pero los queries no se actualizaron consistentemente. Los métodos deprecated quedaron como "deuda técnica documentada" en vez de eliminarse.

### 3.2 Sincronización Fantasma (Patrón #2)
Múltiples paths persisten datos localmente sin disparar sync, o disparan sync tras un rollback. Causa raíz: `PostSaveSyncScheduler` no es parte del contrato de los repositorios — cada ViewModel decide si llamarlo o no.

### 3.3 `costo = 0.0` Sistémico (Patrón #3)
El dominio financiero no calcula costos. `MovimientoFinanciero.costo` siempre es 0. Los regalos no entran al modelo. Causa raíz: el modelo se diseñó sin la capa de costos integrada — las entidades no tienen campo `costo`, los lookups son opcionales y fallan silenciosamente.

### 3.4 Autorización Frontend-Only (Patrón #4)
15+ ViewModels sin verificación de rol. La app confía en que la UI oculta botones, pero los ViewModels ejecutan las operaciones sin validar permisos. Causa raíz: no existe un interceptor/autorizador central en la capa de dominio.

### 3.5 `Double` para Dinero (Patrón #5)
IEEE 754 en todos los campos monetarios. `sumOf` acumula error. Umbrales comparados con floats. Causa raíz: elección de tipo de dato sin considerar precisión financiera.

### 3.6 `runBlocking` Anti-patrón (Patrón #6)
5+ ViewModels usan `runBlocking` dentro de `runInTransaction` + `withContext(IO)`. Causa raíz: los DAOs son `suspend` pero las transacciones de Room usan callbacks no-suspend. La solución (`database.withTransaction {}`) existe en `OptoRepository` pero no se reusa.

### 3.7 Auditoría de Inventario Corrupta (Patrón #7)
`cantidad = delta.coerceAtLeast(0)` fuerza a 0 todos los movimientos de salida. Confirmado por **4 jueces independientes** en Slices 5, 8, Inventory, y Dispensations. Causa raíz: `DispensacionStockHelper.adjustStockAndRegistrarMovimiento` no fue diseñado para deltas negativos.

---

## 4. Matriz de Riesgo

| Riesgo | Probabilidad | Impacto | Severidad |
|--------|:---:|:---:|:---:|
| Service key en APK release | Alta | Catastrófico | 🔴 |
| Brecha multi-tenant (35+ DAOs) | Alta | Crítico | 🔴 |
| Suscripción sin backend | Alta | Crítico | 🔴 |
| `costo=0.0` en reporting financiero | Certeza | Alto | 🔴 |
| PIN bypass (force kill) | Media | Alto | 🔴 |
| RPCs rotos en producción | Certeza | Alto | 🔴 |
| Backup SQLite corrupto | Alta | Alto | 🔴 |
| PII en lock screen | Alta | Alto | 🔴 |
| Token recovery/OAuth en logs | Media | Alto | 🔴 |
| Role default a "admin" | Media | Alto | 🔴 |
| Schema drift (7 UUID vs TEXT) | Alta | Alto | 🟠 |
| dead ThreeWayMerge (conflictos sin resolver) | Certeza | Medio | 🟠 |
| Non-atomic stock mutations | Alta | Alto | 🟠 |
| Race condition `monto_pagado` trigger | Media | Alto | 🟠 |
| `Double` para dinero (error acumulado) | Certeza | Medio | 🟡 |

---

## 5. Plan de Tratamiento

El plan se organiza en 4 fases. Cada fase es autocontenida y puede desplegarse independientemente. El orden es **estrictamente secuencial** — cada fase desbloquea la siguiente.

### Principios del tratamiento

1. **No romper lo que funciona**: 1,780 tests pasando, app en producción. Los fixes deben ser incrementales.
2. **Backend primero**: Los fixes de RLS/RPCs/Schema protegen a todos los clientes inmediatamente.
3. **Una cosa a la vez**: Cada fix en su propio commit/PR. Nada de "aprovecho y refactorizo".
4. **Conventional commits**: `fix:`, `feat:`, `security:`, `refactor:` — sin AI attribution.
5. **Test antes del fix**: Escribir test que reproduzca el bug, después fix, verificar que pasa.

### 🔄 Registro de Consolidación (2026-07-16)

Durante la implementación de Fase 1 se detectó que varios fixes del plan tocan las MISMAS funciones/RPCs con `CREATE OR REPLACE`. Para evitar regresiones (cada fix pisando al anterior), se consolidaron en migraciones únicas:

| Fix Canónico | Absorbe | Función(es) | Motivo |
|-------------|---------|-------------|--------|
| **1.4** | 2.21 (excluye anuladas) | `recalcular_resumen_diario` | Misma función, `CREATE OR REPLACE` secuencial pisaría cambios |
| **2.29** (Fase 2) | 2.13 (auth.uid) + 2.20 (COALESCE) | `rpc_analisis_mensual`, `rpc_deudores`, `rpc_cierre_caja_resumen` | Tres fixes tocan las mismas 3 RPCs |
| **1.3** | 4.6 (excluir de DROP list) | `rpc_count_pendientes` | Ya droppeada, Fix 4.6 no debe re-droppear |
| **1.2** | — | `assign_optica_role_by_email` | Sin conflicto, fix independiente |
| **1.3** | — | `rpc_saldo_pendiente` | Ya existía migración `20260714000002`, sin acción |

**Migraciones canceladas**: `20260715000003`, `20260715000008`, `20260715000009`, `20260715000010`.
**Neto**: 6 fixes → 3 migraciones. Reducción de 3 archivos.

---

## Fase 1: Cirugía de Emergencia (BLOCKERs — 2 semanas)

**Objetivo**: Eliminar vulnerabilidades con pérdida de datos activa, brechas de seguridad explotables, o features rotos en producción. Cada fix incluye código exacto, migración SQL completa, test de validación y checklist.

---

### 1.1 🔴 Service key compilada en APK Release

**Severidad**: BLOCKER | **Riesgo**: Acceso admin total a Supabase si se extrae del APK
**Archivo**: `optoapp/build.gradle.kts` línea 57

**Código ACTUAL (línea 57 dentro de `defaultConfig`)**:
```kotlin
defaultConfig {
    // ... otras configs ...
    buildConfigField("String", "SUPABASE_TEST_SERVICE_KEY", "\"${prop("supabase.test.service.key", "")}\"")
}
```

**Código CORREGIDO**:
```kotlin
// ELIMINAR la línea de defaultConfig
defaultConfig {
    // ... otras configs ...
    // ❌ ELIMINADO: buildConfigField("String", "SUPABASE_TEST_SERVICE_KEY", ...)
}

// AGREGAR solo en debug:
buildTypes {
    debug {
        buildConfigField("String", "SUPABASE_TEST_SERVICE_KEY", "\"${prop("supabase.test.service.key", "")}\"")
    }
    release {
        // Service key NUNCA en release. Si el código la referencia, usar valor vacío.
        buildConfigField("String", "SUPABASE_TEST_SERVICE_KEY", "\"\"")
    }
}
```

**Validación**:
```bash
./gradlew :optoapp:assembleRelease
# Descompilar APK y verificar:
# 1) jadx app-release.apk
# 2) Buscar "SUPABASE_TEST_SERVICE_KEY" en BuildConfig
# 3) Debe ser "" (string vacío) en release
# 4) Verificar que ningún código en main/ depende de esta constante en release
```

**Riesgo de regresión**: Si algún código en `main/` (no `test/` o `androidTest/`) usa `BuildConfig.SUPABASE_TEST_SERVICE_KEY`, compilará con string vacío. Verificar con grep antes:
```bash
grep -r "SUPABASE_TEST_SERVICE_KEY" optoapp/src/main/
```
Si hay referencias en main, migrarlas a usar la anon key o envolver en `if (BuildConfig.DEBUG)`.

---

### 1.2 🔴 RPC `assign_optica_role_by_email` roto — feature de roles no funciona

**Severidad**: BLOCKER | **Riesgo**: Ningún admin/gerente puede asignar roles
**Archivo afectado**: `supabase/migrations/20260427060000_restrict_authenticated_security_definer_execute.sql:9`
**Archivo a crear**: `supabase/migrations/20260715000001_fix_assign_role_grant.sql`

**Migración SQL completa**:
```sql
-- Fix: Re-grant EXECUTE on assign_optica_role_by_email to authenticated
-- Root cause: Migration 20260427060000 revoked this permission,
-- but it's needed by the Android app's role management feature.

-- Verificar estado actual antes de aplicar:
-- SELECT proname, proacl FROM pg_proc WHERE proname = 'assign_optica_role_by_email';

-- Re-otorgar permiso
GRANT EXECUTE ON FUNCTION public.assign_optica_role_by_email(
    p_optica_id text,
    p_email text,
    p_rol text
) TO authenticated;

-- La función tiene su propia verificación de roles internamente
-- (solo admin/gerente pueden asignar), así que es seguro.
```

**Rollback** (por si hay que revertir):
```sql
REVOKE EXECUTE ON FUNCTION public.assign_optica_role_by_email(text, text, text) FROM authenticated;
```

**Test de validación**:
```sql
-- 1. Autenticarse como usuario admin de una óptica
-- 2. Llamar la RPC:
SELECT assign_optica_role_by_email('tu_optica_id', 'test@example.com', 'especialista');
-- 3. Debe retornar sin error (no 403 Permission Denied)
-- 4. Verificar en usuario_optica que el rol se asignó:
SELECT * FROM usuario_optica WHERE optica_id = 'tu_optica_id' AND user_id = (SELECT id FROM auth.users WHERE email = 'test@example.com');
```

---

### 1.3 🔴 `rpc_saldo_pendiente` referencia tabla fantasma

**Severidad**: CRITICAL | **Riesgo**: Crash en runtime si alguien la invoca
**Archivo**: `supabase/remote_schema_dump.sql` líneas 1055-1084
**Archivo a crear**: `supabase/migrations/20260715000002_drop_rpc_saldo_pendiente.sql`

**Diagnóstico**: La migración `20260710064319` droppeó `public.ventas` pero `rpc_saldo_pendiente` no fue actualizada ni droppeada. Su cuerpo referencia `FROM public.ventas` en líneas 1062 y 1072. El Android app no la llama (está deprecated), así que es seguro droppearla.

**Migración SQL**:
```sql
-- Drop rpc_saldo_pendiente — deprecated, replaced by rpc_analisis_mensual
-- References dropped public.ventas table (dropped in 20260710064319)
-- Not called by Android app (confirmed via grep audit 2026-07-15)
DROP FUNCTION IF EXISTS public.rpc_saldo_pendiente(TEXT);

-- También droppear la función count_pendientes que depende de la misma lógica
DROP FUNCTION IF EXISTS public.rpc_count_pendientes(TEXT);
```

**Validación**:
```sql
-- Verificar que ya no existe:
SELECT proname FROM pg_proc WHERE proname IN ('rpc_saldo_pendiente', 'rpc_count_pendientes');
-- Debe retornar 0 filas.
```

---

### 1.4 🔴 `recalcular_resumen_diario` — INSERT bloqueado por RLS + anuladas no filtradas

> ⚠️ **CONSOLIDADO (2026-07-16)**: Este fix ahora absorbe el Fix 2.20 original. El cuerpo de la función ya fue reparado por `20260714000000` (UNION ALL en vez de `ventas`, costo real, ON CONFLICT upsert). Este fix solo cambia el header (SECURITY DEFINER) y agrega el filtro `estado IS DISTINCT FROM 'Anulado'`. Fix 2.20 queda CANCELADO como fix independiente.

**Severidad**: CRITICAL | **Riesgo**: Resúmenes diarios nunca se persisten desde cliente authenticated
**Archivo actual**: `supabase/migrations/20260714000000_fix_recalcular_resumen_diario.sql` (cuerpo ya reparado)
**Archivo a crear**: `supabase/migrations/20260716000000_fix_recalcular_resumen_security.sql`

**Lo que YA existe en `20260714000000`** (no tocar):
- UNION ALL de `dispensaciones` + `servicios_extra` (sin `ventas`)
- Costo real desde `dispensacion_items.costo_real_*`
- `ON CONFLICT (optica_id, fecha) DO UPDATE` idempotente
- Exclusión de `Anulación` en pagos
- Columnas completas: `cobros_cantidad`, `saldo_pendiente_cantidad`, `inventario_valor`, `inventario_unidades`

**Lo que FALTA (este fix)**:
1. `SECURITY INVOKER` → `SECURITY DEFINER` (el INSERT/UPDATE lo bloquea RLS)
2. `SET search_path = public` → `SET search_path = ''`
3. `ALTER FUNCTION ... OWNER TO postgres`
4. `estado IS DISTINCT FROM 'Anulado'` en las queries de ventas (absorbido de Fix 2.20)

**Migración SQL** (CREATE OR REPLACE preservando el cuerpo de `20260714000000`):
```sql
-- Fix: recalcular_resumen_diario — SECURITY DEFINER + anulado filter
-- Body preserved from 20260714000000 (already correct)
-- Changes: SECURITY DEFINER, search_path='', OWNER TO postgres,
--           estado IS DISTINCT FROM 'Anulado' on ventas queries
CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario(
    p_optica_id TEXT,
    p_fecha DATE
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = ''
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
    -- Daily sales: UNION ALL + estado IS DISTINCT FROM 'Anulado'
    WITH daily_ventas AS (
        SELECT d.monto_total,
            COALESCE((
                SELECT SUM(
                    COALESCE(di.costo_real_od, 0) +
                    COALESCE(di.costo_real_oi, 0) +
                    COALESCE(di.costo_real_montura, 0) +
                    COALESCE(di.costo_real_biselado, 0) +
                    COALESCE(di.costo_real_lc, 0)
                ) FROM public.dispensacion_items di
                WHERE di.dispensacion_id = d.id
            ), 0) AS costo
        FROM public.dispensaciones d
        WHERE d.optica_id = p_optica_id AND d.fecha = p_fecha
          AND d.estado IS DISTINCT FROM 'Anulado'
        UNION ALL
        SELECT se.monto_total, 0::numeric AS costo
        FROM public.servicios_extra se
        WHERE se.optica_id = p_optica_id AND se.fecha = p_fecha
          AND se.estado IS DISTINCT FROM 'Anulado'
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(monto_total), 0),
           COALESCE(SUM(costo), 0)
    INTO v_ventas_cantidad, v_ventas_monto, v_ventas_costo
    FROM daily_ventas;

    -- Daily payments (exclude Anulaciones)
    SELECT COALESCE(COUNT(*), 0), COALESCE(SUM(monto), 0)
    INTO v_cobros_cantidad, v_cobros_monto
    FROM public.pagos
    WHERE optica_id = p_optica_id AND fecha = p_fecha
      AND tipo IS DISTINCT FROM 'Anulación';

    -- Accumulated pending balance via namespace-keyed LEFT JOIN
    WITH pagos_dedup AS (
        SELECT COALESCE(pg.venta_id,
               'v_disp_' || pg.dispensacion_id,
               'v_serv_' || pg.servicio_extra_id) AS venta_id_match,
               pg.monto
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id
          AND pg.tipo IS DISTINCT FROM 'Anulación'
    ), all_ventas AS (
        SELECT 'v_disp_' || id AS venta_id, monto_total
        FROM public.dispensaciones
        WHERE optica_id = p_optica_id
          AND estado IS DISTINCT FROM 'Anulado'
        UNION ALL
        SELECT 'v_serv_' || id AS venta_id, monto_total
        FROM public.servicios_extra
        WHERE optica_id = p_optica_id
          AND estado IS DISTINCT FROM 'Anulado'
    )
    SELECT COALESCE(COUNT(*), 0),
           COALESCE(SUM(v.monto_total - COALESCE(pd.total_pagado, 0)), 0)
    INTO v_saldo_cantidad, v_saldo_total
    FROM all_ventas v
    LEFT JOIN (
        SELECT venta_id_match, SUM(monto) AS total_pagado
        FROM pagos_dedup
        GROUP BY venta_id_match
    ) pd ON pd.venta_id_match = v.venta_id
    WHERE v.monto_total - COALESCE(pd.total_pagado, 0) > 0.005;

    -- Inventory snapshot
    SELECT COALESCE(SUM(costo * stock_actual), 0),
           COALESCE(SUM(stock_actual), 0)
    INTO v_inv_valor, v_inv_unidades
    FROM public.monturas
    WHERE optica_id = p_optica_id;

    -- Idempotent upsert (SECURITY DEFINER bypasses RLS)
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

-- Ownership: postgres (needed for SECURITY DEFINER)
ALTER FUNCTION public.recalcular_resumen_diario(text, date) OWNER TO postgres;

-- Grant execute (may already be granted, IF NOT EXISTS style)
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(text, date) TO authenticated;
GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario(text, date) TO service_role;
```

**Validación**:
```sql
-- 1. Autenticarse como usuario de una óptica
-- 2. Ejecutar:
SELECT recalcular_resumen_diario('id_de_tu_optica', CURRENT_DATE);
-- 3. Verificar que se insertó la fila con todas las columnas:
SELECT * FROM resumen_diario WHERE optica_id = 'id_de_tu_optica' AND fecha = CURRENT_DATE;
-- Debe retornar 1 fila con datos no-nulos y ventas sin anuladas.
```

---

### 1.5 🔴 RLS `regalos_dispensacion` — cualquier miembro puede modificar/eliminar

**Severidad**: CRITICAL | **Riesgo**: Usuario con rol `invitado` puede borrar regalos y distorsionar márgenes
**Archivo a crear**: `supabase/migrations/20260715000004_fix_regalos_rls_restrictive.sql`

**Migración SQL completa**:
```sql
-- Fix: regalos_dispensacion RLS policies are too permissive.
-- Currently any member (including 'invitado') can INSERT/UPDATE/DELETE.
-- Should match pagos pattern: only admin/gerente can delete.

-- === DROP existing permissive policies ===
DROP POLICY IF EXISTS "regalos_dispensacion_select" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_insert" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_update" ON public.regalos_dispensacion;
DROP POLICY IF EXISTS "regalos_dispensacion_delete" ON public.regalos_dispensacion;

-- === SELECT: any member can view (matching other financial tables) ===
CREATE POLICY "regalos_dispensacion_select" ON public.regalos_dispensacion
FOR SELECT USING (app_private.is_optica_member(auth.uid(), optica_id));

-- === INSERT: staff roles can add gifts ===
CREATE POLICY "regalos_dispensacion_insert" ON public.regalos_dispensacion
FOR INSERT WITH CHECK (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
);

-- === UPDATE: staff roles can edit gifts ===
CREATE POLICY "regalos_dispensacion_update" ON public.regalos_dispensacion
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente', 'especialista', 'asesor', 'ventas'])
);

-- === DELETE: only admin/gerente (matching pagos_delete pattern) ===
CREATE POLICY "regalos_dispensacion_delete" ON public.regalos_dispensacion
FOR DELETE USING (
    app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin', 'gerente'])
);
```

**Validación**:
```sql
-- 1. Autenticarse como usuario con rol 'invitado'
-- 2. Intentar DELETE:
DELETE FROM regalos_dispensacion WHERE id = 'cualquier-id';
-- Debe fallar con "policy violation"
-- 3. Autenticarse como admin, repetir — debe funcionar
```

---

### 1.6 🔴 `create_optica_for_current_user` — ON CONFLICT DO UPDATE permite toma de control

**Severidad**: CRITICAL | **Riesgo**: Si se conoce un `optica_id`, se sobrescribe la óptica existente
**Archivo**: `supabase/migrations/20260627005400_remove_free_plan_restrictions.sql` líneas 78-94
**Archivo a crear**: `supabase/migrations/20260715000005_fix_create_optica_no_overwrite.sql`

**Migración SQL completa**:
```sql
-- Fix: create_optica_for_current_user uses ON CONFLICT DO UPDATE
-- which allows overwriting existing opticas if the ID is known.
-- Change to DO NOTHING and generate ID server-side.

CREATE OR REPLACE FUNCTION public.create_optica_for_current_user(
    p_optica_id text,
    p_nombre text,
    p_fiscal_doc_tipo text DEFAULT '',
    p_fiscal_doc_numero text DEFAULT '',
    p_razon_social text DEFAULT '',
    p_direccion_fiscal text DEFAULT ''
) RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id uuid;
    v_optica_id text;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Usuario no autenticado';
    END IF;

    -- Use server-generated UUID instead of client-provided ID
    v_optica_id := COALESCE(NULLIF(p_optica_id, ''), 'opt_' || replace(gen_random_uuid()::text, '-', ''));

    -- Insert optica — DO NOTHING on conflict (don't overwrite existing)
    INSERT INTO public.opticas (id, nombre, fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal)
    VALUES (v_optica_id, p_nombre, p_fiscal_doc_tipo, p_fiscal_doc_numero, p_razon_social, p_direccion_fiscal)
    ON CONFLICT (id) DO NOTHING;  -- ← CAMBIO CLAVE: antes era DO UPDATE

    -- Register caller as admin of the new optica
    INSERT INTO public.usuario_optica (user_id, optica_id, rol)
    VALUES (v_user_id, v_optica_id, 'admin')
    ON CONFLICT (user_id, optica_id) DO NOTHING;
END;
$$;

ALTER FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.create_optica_for_current_user(text, text, text, text, text, text) TO authenticated;
```

**Cambio en Android** (`OpticaSettingsDataSource.kt:52`):
```kotlin
// Antes:
val opticaId = "opt_" + UUID.randomUUID().toString().replace("-", "").take(16)
// Después: enviar string vacío para que el servidor genere el ID
val opticaId = ""  // El servidor usa gen_random_uuid()
```

**Validación**:
```sql
-- 1. Crear óptica normalmente
SELECT create_optica_for_current_user('', 'Optica Test');
-- 2. Intentar sobrescribir con mismo nombre pero diferente ID (debe fallar o crear nueva)
-- 3. Verificar que la óptica original no fue modificada
```

---

### 1.7 🔴 `trg_pagos_update_monto_pagado` — race condition

**Severidad**: CRITICAL | **Riesgo**: Pagos concurrentes corrompen `monto_pagado` y `a_cuenta`
**Archivo**: `supabase/migrations/20260706205131_jd_fix3_exclude_anulaciones_from_financial_calcs.sql` líneas 7-39
**Archivo a crear**: `supabase/migrations/20260715000006_fix_monto_pagado_atomic.sql`

**Migración SQL completa**:
```sql
-- Fix: trg_pagos_update_monto_pagado uses SELECT SUM(monto) without FOR UPDATE,
-- causing lost updates under concurrent INSERTs. Replace with atomic increment/decrement.

CREATE OR REPLACE FUNCTION public.trg_pagos_update_monto_pagado()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    -- Handle dispensacion monto_pagado
    IF (TG_OP = 'INSERT' AND NEW.dispensacion_id IS NOT NULL AND NEW.tipo IS DISTINCT FROM 'Anulación') THEN
        UPDATE public.dispensaciones
        SET monto_pagado = monto_pagado + NEW.monto
        WHERE id = NEW.dispensacion_id;

    ELSIF (TG_OP = 'DELETE' AND OLD.dispensacion_id IS NOT NULL AND OLD.tipo IS DISTINCT FROM 'Anulación') THEN
        UPDATE public.dispensaciones
        SET monto_pagado = monto_pagado - OLD.monto
        WHERE id = OLD.dispensacion_id;

    ELSIF (TG_OP = 'UPDATE' AND NEW.dispensacion_id IS NOT NULL) THEN
        -- Handle both old and new tipo/anulacion states
        IF OLD.tipo IS DISTINCT FROM 'Anulación' AND NEW.tipo IS DISTINCT FROM 'Anulación' THEN
            UPDATE public.dispensaciones
            SET monto_pagado = monto_pagado - OLD.monto + NEW.monto
            WHERE id = NEW.dispensacion_id;
        ELSIF OLD.tipo IS DISTINCT FROM 'Anulación' AND NEW.tipo = 'Anulación' THEN
            UPDATE public.dispensaciones
            SET monto_pagado = monto_pagado - OLD.monto
            WHERE id = NEW.dispensacion_id;
        ELSIF OLD.tipo = 'Anulación' AND NEW.tipo IS DISTINCT FROM 'Anulación' THEN
            UPDATE public.dispensaciones
            SET monto_pagado = monto_pagado + NEW.monto
            WHERE id = NEW.dispensacion_id;
        END IF;
    END IF;

    -- Handle servicio_extra a_cuenta (same pattern)
    IF (TG_OP = 'INSERT' AND NEW.servicio_extra_id IS NOT NULL AND NEW.tipo IS DISTINCT FROM 'Anulación') THEN
        UPDATE public.servicios_extra
        SET a_cuenta = a_cuenta + NEW.monto
        WHERE id = NEW.servicio_extra_id;

    ELSIF (TG_OP = 'DELETE' AND OLD.servicio_extra_id IS NOT NULL AND OLD.tipo IS DISTINCT FROM 'Anulación') THEN
        UPDATE public.servicios_extra
        SET a_cuenta = a_cuenta - OLD.monto
        WHERE id = OLD.servicio_extra_id;

    ELSIF (TG_OP = 'UPDATE' AND NEW.servicio_extra_id IS NOT NULL) THEN
        IF OLD.tipo IS DISTINCT FROM 'Anulación' AND NEW.tipo IS DISTINCT FROM 'Anulación' THEN
            UPDATE public.servicios_extra
            SET a_cuenta = a_cuenta - OLD.monto + NEW.monto
            WHERE id = NEW.servicio_extra_id;
        ELSIF OLD.tipo IS DISTINCT FROM 'Anulación' AND NEW.tipo = 'Anulación' THEN
            UPDATE public.servicios_extra
            SET a_cuenta = a_cuenta - OLD.monto
            WHERE id = NEW.servicio_extra_id;
        ELSIF OLD.tipo = 'Anulación' AND NEW.tipo IS DISTINCT FROM 'Anulación' THEN
            UPDATE public.servicios_extra
            SET a_cuenta = a_cuenta + NEW.monto
            WHERE id = NEW.servicio_extra_id;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

ALTER FUNCTION public.trg_pagos_update_monto_pagado() OWNER TO postgres;

-- Asegurar que el trigger existe
DROP TRIGGER IF EXISTS trg_pagos_maintain_monto_pagado ON public.pagos;
CREATE TRIGGER trg_pagos_maintain_monto_pagado
AFTER INSERT OR DELETE OR UPDATE ON public.pagos
FOR EACH ROW EXECUTE FUNCTION public.trg_pagos_update_monto_pagado();
```

**Test de validación** (concurrente):
```sql
-- Sesión 1:
BEGIN;
INSERT INTO pagos (id, optica_id, dispensacion_id, monto, tipo, fecha, metodo_pago)
VALUES (gen_random_uuid()::text, 'optica_test', 'disp_test_id', 50, 'Abono', CURRENT_DATE, 'Efectivo');
-- NO hacer COMMIT todavía

-- Sesión 2 (en paralelo):
BEGIN;
INSERT INTO pagos (id, optica_id, dispensacion_id, monto, tipo, fecha, metodo_pago)
VALUES (gen_random_uuid()::text, 'optica_test', 'disp_test_id', 75, 'Abono', CURRENT_DATE, 'Transferencia');
COMMIT;

-- Sesión 1:
COMMIT;

-- Verificar:
SELECT monto_pagado FROM dispensaciones WHERE id = 'disp_test_id';
-- Debe ser 125 (50 + 75), no 75 ni 50.
```

---

### 1.8 🔴 `opticas_update_member` RLS — cualquier miembro modifica datos fiscales

**Severidad**: CRITICAL | **Riesgo**: Invitado modifica razón social, CUIT, dirección fiscal
**Archivo a crear**: `supabase/migrations/20260715000007_fix_opticas_update_rls.sql`

```sql
-- Fix: opticas_update_member currently allows ANY member to UPDATE opticas
-- (change nombre, fiscal_doc_tipo, fiscal_doc_numero, razon_social, direccion_fiscal).
-- Restrict to admin/gerente only.

DROP POLICY IF EXISTS "opticas_update_member" ON public.opticas;

CREATE POLICY "opticas_update_member" ON public.opticas
FOR UPDATE USING (
    app_private.has_optica_role(auth.uid(), id, ARRAY['admin', 'gerente'])
);

-- Also fix SELECT: any member can view, but add defense-in-depth
DROP POLICY IF EXISTS "opticas_select_member" ON public.opticas;
CREATE POLICY "opticas_select_member" ON public.opticas
FOR SELECT USING (
    app_private.is_optica_member(auth.uid(), id)
);
```

---

### 1.9 🔴 Notificaciones con PII en lock screen

**Severidad**: BLOCKER | **Riesgo**: Nombres de pacientes visibles sin desbloquear el dispositivo
**Archivo**: `optoapp/src/main/java/com/example/optoapp/notifications/NotificationHelper.kt`

**Código ACTUAL (líneas 73-82)**:
```kotlin
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Recordatorio de Cita")
    .setContentText("Hoy tiene una cita con el paciente: $patientName")  // ← PII en lockscreen
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .build()
```

**Código CORREGIDO**:
```kotlin
// Notificación pública (visible en lock screen) — sin PII
val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Recordatorio de Cita")
    .setContentText("Tiene citas programadas para hoy")  // ← SIN nombres de pacientes
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .build()

// Notificación privada (visible solo al desbloquear) — con detalles
val privateNotification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Recordatorio de Cita")
    .setContentText("Hoy tiene una cita con el paciente: $patientName")  // ← PII aquí
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .build()

val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Recordatorio de Cita")
    .setContentText("Hoy tiene una cita con el paciente: $patientName")
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)  // ← OCULTAR en lockscreen
    .setPublicVersion(publicNotification)                   // ← Versión pública sin PII
    .build()
```

**También cambiar** el canal de notificación (línea 39):
```kotlin
// Antes:
val importance = NotificationManager.IMPORTANCE_HIGH
// Después: IMPORTANCE_HIGH está bien pero combinado con VISIBILITY_PRIVATE
// Si se quiere ser más conservador:
val importance = NotificationManager.IMPORTANCE_DEFAULT  // Sin heads-up en lockscreen
```

---

### 1.10 🔴 PIN brute-force — contador resetea al matar la app

**Severidad**: BLOCKER | **Riesgo**: Ataque de fuerza bruta ilimitado matando y reabriendo la app
**Archivos involucrados**:
- `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/PinDelegate.kt`
- `optoapp/src/main/java/com/example/optoapp/data/SecurityManager.kt`

**Paso 1: Agregar persistencia en SecurityManager.kt**

Agregar al final de la clase `SecurityManager`:
```kotlin
// === Persistencia de protección anti fuerza bruta ===

fun getPinFailedAttempts(): Int {
    return try {
        encryptedPrefs.getInt("pin_failed_attempts", 0)
    } catch (e: Exception) {
        0
    }
}

fun setPinFailedAttempts(value: Int) {
    try {
        encryptedPrefs.edit { putInt("pin_failed_attempts", value) }
    } catch (e: Exception) {
        Log.e(TAG, "Error persistiendo pin_failed_attempts", e)
    }
}

fun getPinCooldownUntil(): Long {
    return try {
        encryptedPrefs.getLong("pin_cooldown_until", 0L)
    } catch (e: Exception) {
        0L
    }
}

fun setPinCooldownUntil(value: Long) {
    try {
        encryptedPrefs.edit { putLong("pin_cooldown_until", value) }
    } catch (e: Exception) {
        Log.e(TAG, "Error persistiendo pin_cooldown_until", e)
    }
}

fun resetPinBruteForceProtection() {
    try {
        encryptedPrefs.edit {
            remove("pin_failed_attempts")
            remove("pin_cooldown_until")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error reseteando proteccion anti fuerza bruta", e)
    }
}
```

**Paso 2: Modificar PinDelegate.kt**

```kotlin
// ANTES (líneas 33-34):
private var failedAttempts = 0
private var cooldownUntil: Long = 0L

// DESPUÉS:
private var failedAttempts: Int
    get() = securityManager.getPinFailedAttempts()
    set(value) { securityManager.setPinFailedAttempts(value) }

private var cooldownUntil: Long
    get() = securityManager.getPinCooldownUntil()
    set(value) { securityManager.setPinCooldownUntil(value) }
```

**Paso 3: Resetear en validación exitosa (agregar después de línea 63)**:
```kotlin
if (isValid) {
    failedAttempts = 0
    cooldownUntil = 0L
    securityManager.resetPinBruteForceProtection()  // ← limpiar al éxito
    _pinInput.value = ""
    _pinError.value = null
    return
}
```

**Paso 4: También resetear en `clearSession` de SessionManager**:
```kotlin
// Agregar después de encryptedPrefs.edit { clear() }
securityManager.resetPinBruteForceProtection()
```

**Test de validación (Robolectric)**:
```kotlin
@Test
fun `pin brute force protection survives process death`() {
    // 4 intentos fallidos
    repeat(4) { pinDelegate.validatePin("000000") }
    assertTrue(pinDelegate.failedAttempts == 4)

    // Simular kill de app (nueva instancia de SecurityManager con misma encrypted prefs)
    val newSecurityManager = SecurityManager(context)
    val newPinDelegate = PinDelegate(newSecurityManager, ...)

    // El 5to intento debe seguir bloqueado
    assertEquals(4, newPinDelegate.failedAttempts)
    // NO debería resetearse a 0
}
```

---

### 1.11 🔴 Tokens en logs — recovery y OAuth

**Severidad**: CRITICAL | **Riesgo**: Token de recuperación/Auth extraíble vía logcat o crash reporters
**Archivo**: `optoapp/src/main/java/com/example/optoapp/viewmodel/auth/AuthDelegate.kt`

**Fix 1 — Línea 304 (recovery deep link)**:
```kotlin
// ANTES:
Log.d(TAG, "Recibido deeplink recovery: $deepLink")
// deepLink = "optoapp://auth#access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

// DESPUÉS:
Log.d(TAG, "Recibido deeplink recovery: ${deepLink.scheme}://${deepLink.host}")  // Solo "optoapp://auth"
```

**Fix 2 — Línea 121 (OAuth deep link)**:
```kotlin
// ANTES:
Log.d(TAG, "Recibido deeplink OAuth: $deepLink")

// DESPUÉS:
Log.d(TAG, "Recibido deeplink OAuth")  // Sin URL
```

**Fix 3 — Líneas 109-111 y 244-248 (access token en memoria)**:
```kotlin
// ELIMINAR el campo:
// private var pendingAccessToken: String = ""

// ELIMINAR las asignaciones en checkExistingSession() y onboardingOptica()
// Si onboardingOptica necesita el token, usar:
val token = supabase.auth.currentSessionOrNull()?.accessToken
// directamente en el call site, sin almacenarlo en un campo de clase.
```

---

### 1.12 🔴 Role default "admin" — escalación silenciosa

**Severidad**: CRITICAL | **Riesgo**: Si keystore falla, usuario hereda rol admin
**Archivo**: `optoapp/src/main/java/com/example/optoapp/data/SessionManager.kt`

**Fix 1 — Línea 81 (`getSecureOpticaRol`)**:
```kotlin
// ANTES:
private fun getSecureOpticaRol(): String {
    return encryptedPrefs.getString("saas_optica_rol", "admin") ?: "admin"
}

// DESPUÉS:
private fun getSecureOpticaRol(): String {
    return encryptedPrefs.getString("saas_optica_rol", "") ?: ""
    // String vacío = sin rol. Las verificaciones de permisos tratan "" como "sin acceso".
}
```

**Fix 2 — Línea 159 (`clearSession`)**:
```kotlin
// ANTES:
_opticaRolFlow.value = "admin"

// DESPUÉS:
_opticaRolFlow.value = ""  // Sin rol post-logout
```

**Fix 3 — Verificar consumers de `opticaRol`** (buscar `collectAsState(initial = "admin")`):
```bash
grep -rn "collectAsState.*initial.*admin" optoapp/src/main/
```
Cambiar TODOS los `initial = "admin"` a `initial = ""`:
```kotlin
// ANTES:
val opticaRol by authViewModel.opticaRol.collectAsState(initial = "admin")

// DESPUÉS:
val opticaRol by authViewModel.opticaRol.collectAsState(initial = "")
// Agregar guard:
if (opticaRol.isEmpty()) return@Composable  // No renderizar hasta tener rol real
```

---

### 1.13 🔴 Backup SQLite corrupto — copia de archivos vivos

**Severidad**: BLOCKER | **Riesgo**: Backups inservibles — falsa seguridad, pérdida de datos en recuperación
**Archivo**: `optoapp/src/main/java/com/example/optoapp/util/LocalDatabaseBackupManager.kt`

**Código ACTUAL (líneas 25-32)**:
```kotlin
fun createBackup(context: Context): File? {
    return try {
        val dbPath = context.getDatabasePath("optoapp.db")
        val backupDir = File(context.filesDir, "db_backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val stamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "optoapp.db-$stamp.db")

        // ❌ PROBLEMA: copia archivos vivos sin snapshot atómico
        dbPath.copyTo(backupFile, overwrite = true)
        val walFile = File("${dbPath.absolutePath}-wal")
        if (walFile.exists()) walFile.copyTo(File(backupDir, "${walFile.name}-$stamp"), overwrite = true)
        val shmFile = File("${dbPath.absolutePath}-shm")
        if (shmFile.exists()) shmFile.copyTo(File(backupDir, "${shmFile.name}-$stamp"), overwrite = true)

        Log.i(TAG, "Backup created: ${backupFile.absolutePath}")
        backupFile
    } catch (e: Exception) {
        Log.w(TAG, "Database backup failed", e)
        null
    }
}
```

**Código CORREGIDO**:
```kotlin
fun createBackup(context: Context): File? {
    return try {
        val dbPath = context.getDatabasePath("optoapp.db")
        val backupDir = File(context.filesDir, "db_backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "optoapp-backup-$stamp.db")

        // ✅ CORRECTO: SQLiteDatabase.backup() = snapshot atómico
        val db = SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        db.use { database ->
            database.backup(backupFile.absolutePath)
        }

        // Guardar metadata junto al backup
        val metaFile = File(backupDir, "optoapp-backup-$stamp.json")
        metaFile.writeText(buildString {
            appendLine("{")
            appendLine("  \"timestamp\": $stamp,")
            appendLine("  \"date\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(stamp))}\",")
            appendLine("  \"file\": \"${backupFile.name}\",")
            appendLine("  \"version\": \"${BuildConfig.VERSION_NAME}\"")
            appendLine("}")
        })

        Log.i(TAG, "Backup created successfully: ${backupFile.absolutePath}")
        backupFile
    } catch (e: Exception) {
        Log.w(TAG, "Database backup failed", e)
        null
    }
}
```

**Eliminar método obsoleto** (si existe):
```kotlin
// ❌ ELIMINAR: copySafe() y cualquier referencia a copia de -wal/-shm
```

**Test de validación**:
```kotlin
@Test
fun `backup is consistent while database is being written`() {
    // 1. Iniciar escritura intensiva en DB (100 inserts en loop)
    val writeJob = scope.launch {
        repeat(100) { repository.insertTestData() }
    }
    // 2. Crear backup mientras se escribe
    val backupFile = backupManager.createBackup(context)
    // 3. Verificar que el backup es válido
    val backupDb = Room.databaseBuilder(context, OptoDatabase::class.java, backupFile!!.absolutePath).build()
    val count = backupDb.pacienteDao().getCount()
    backupDb.close()
    // 4. El backup debe contener datos (no estar corrupto)
    assertTrue(count > 0, "Backup should contain data")
    writeJob.cancel()
}
```

---

### Resumen Fase 1 — Checklist de despliegue

| # | Fix | Tipo | Archivo | Validación |
|---|-----|------|---------|------------|
| 1.1 | Service key en APK | Android | `build.gradle.kts` | Descompilar release → key = "" |
| 1.2 | RPC roles roto | Supabase | Nueva migración | Llamar RPC como authenticated → sin 403 |
| 1.3 | rpc_saldo_pendiente | Supabase | Nueva migración | `DROP FUNCTION` → 0 filas en pg_proc |
| 1.4 | recalcular_resumen_diario | Supabase | Nueva migración | Llamar RPC → fila en resumen_diario |
| 1.5 | RLS regalos | Supabase | Nueva migración | Invitado no puede DELETEr |
| 1.6 | create_optica overwrite | Supabase + Android | Migración + DataSource | No sobrescribe existente |
| 1.7 | trigger monto_pagado | Supabase | Nueva migración | INSERTs concurrentes → suma correcta |
| 1.8 | RLS opticas_update | Supabase | Nueva migración | Invitado no puede modificar fiscales |
| 1.9 | Notificaciones PII | Android | `NotificationHelper.kt` | Lockscreen muestra texto genérico |
| 1.10 | PIN bypass | Android | `PinDelegate.kt` + `SecurityManager.kt` | Kill app → contador persiste |
| 1.11 | Tokens en logs | Android | `AuthDelegate.kt` | Log.d sin fragment/query |
| 1.12 | Role default admin | Android | `SessionManager.kt` + screens | Rol vacío hasta carga real |
| 1.13 | Backup corrupto | Android | `LocalDatabaseBackupManager.kt` | Backup durante escritura → válido |

---

## Fase 2: Estabilización (CRITICALs — 3 semanas)

**Objetivo**: Cerrar las brechas multi-tenant, restaurar integridad financiera, y eliminar bugs que causan pérdida silenciosa de datos.

---

### 2.1 Multi-tenancy: Agregar `opticaId` a TODOS los `getById`

**Archivos**: 15 DAOs. Lista completa:

| DAO | Método | Archivo | Línea |
|-----|--------|---------|-------|
| PacienteDao | `getPacienteById` | `data/paciente/PacienteDao.kt` | 38 |
| EvaluacionDao | `getEvaluacionById` | `data/evaluacion/EvaluacionDao.kt` | 13 |
| EvaluacionDao | `getEvaluacionesByPaciente` | `data/evaluacion/EvaluacionDao.kt` | 10 |
| EvaluacionDao | `getLastEvaluacionByPacienteId` | `data/evaluacion/EvaluacionDao.kt` | 62 |
| DispensacionDao | `getDispensacionById` | `data/dispensacion/DispensacionDao.kt` | 36 |
| DispensacionDao | `getLastDispensacionByPacienteId` | `data/dispensacion/DispensacionDao.kt` | 83 |
| ServicioExtraDao | `getServicioById` | `data/servicio/ServicioExtraDao.kt` | 23 |
| ServicioExtraDao | `getServiciosByPaciente` | `data/servicio/ServicioExtraDao.kt` | 20 |
| PagoDao | `getPagosByServicioExtra` | `data/pago/PagoDao.kt` | 24 |
| ProveedorDao | `getById` | `data/proveedor/ProveedorDao.kt` | 19 |
| OrdenCompraDao | `getById` | `data/ordencompra/OrdenCompraDao.kt` | 18 |
| InventarioFisicoDao | `getById` | `data/inventariofisico/InventarioFisicoDao.kt` | buscar |
| MonturaMovimientoDao | `getMovimientoById` | `data/montura/MonturaMovimientoDao.kt` | 24 |
| MonturaMovimientoDao | `getMovimientosByMontura` | `data/montura/MonturaMovimientoDao.kt` | 14 |
| RegaloDispensacionDao | `getByDispensacionId` | `data/regalodispensacion/RegaloDispensacionDao.kt` | 17 |

**Patrón de fix para cada DAO**:

```kotlin
// ===== ANTES =====
@Query("SELECT * FROM pacientes WHERE id = :id")
suspend fun getPacienteById(id: String): Paciente?

// ===== DESPUÉS =====
@Query("SELECT * FROM pacientes WHERE id = :id AND opticaId = :opticaId")
suspend fun getPacienteById(id: String, opticaId: String): Paciente?
```

**Propagación en repositorios** (ejemplo `PacienteRepository`):
```kotlin
// ===== ANTES =====
suspend fun getPacienteById(id: String): Resource<Paciente> {
    return try {
        val paciente = pacienteDao.getPacienteById(id)
        if (paciente != null) Resource.Success(paciente)
        else Resource.Error("Paciente no encontrado")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Error al obtener paciente")
    }
}

// ===== DESPUÉS =====
suspend fun getPacienteById(id: String, opticaId: String): Resource<Paciente> {
    return try {
        val paciente = pacienteDao.getPacienteById(id, opticaId)
        if (paciente != null) Resource.Success(paciente)
        else Resource.Error("Paciente no encontrado")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Error al obtener paciente")
    }
}
```

**Propagación en ViewModels** (ejemplo `PacienteViewModel`):
```kotlin
// ===== ANTES =====
fun loadPaciente(pacienteId: String) {
    viewModelScope.launch {
        val result = repository.getPacienteById(pacienteId)
        // ...
    }
}

// ===== DESPUÉS =====
fun loadPaciente(pacienteId: String) {
    viewModelScope.launch {
        val currentOpticaId = sessionManager.opticaId.first()
        val result = repository.getPacienteById(pacienteId, currentOpticaId)
        // ...
    }
}
```

**Test para cada DAO** (ejemplo):
```kotlin
@Test
fun `getPacienteById respects opticaId`() = runTest {
    val pacienteA = Paciente(id = "p1", opticaId = "optica_A", nombreCompleto = "Juan")
    val pacienteB = Paciente(id = "p2", opticaId = "optica_B", nombreCompleto = "Maria")
    pacienteDao.insertPaciente(pacienteA)
    pacienteDao.insertPaciente(pacienteB)

    // Buscar con opticaId correcto → encuentra
    val found = pacienteDao.getPacienteById("p1", "optica_A")
    assertEquals("Juan", found?.nombreCompleto)

    // Buscar con opticaId incorrecto → null
    val notFound = pacienteDao.getPacienteById("p1", "optica_B")
    assertNull(notFound)
}
```

---

### 2.2 Cost lookups: Agregar `opticaId` a costo producto y biselado

**Archivos**:
- `data/costoproducto/CostoProductoDao.kt` — `lookup()` línea 10, `lookupLc()` línea 48
- `data/costobiselado/CostoBiseladoDao.kt` — `lookup()` línea 9
- `viewmodel/DispensacionViewModel.kt` — `calculateCosts()` línea 713

**Fix 1 — `CostoProductoDao.lookup()` con `opticaId`**:
```kotlin
@Query("""
    SELECT * FROM costos_productos 
    WHERE optica_id = :opticaId
      AND (material IS NULL OR material = :material OR material = '')
      AND (tipoLente IS NULL OR tipoLente = :tipoLente OR tipoLente = '')
      AND (stockOFabricacion IS NULL OR stockOFabricacion = :stockOFabricacion OR stockOFabricacion = '')
      AND (tratamiento IS NULL OR tratamiento = :tratamiento OR tratamiento = '')
      AND (serie IS NULL OR serie = :serie)
      AND (altoIndice IS NULL OR altoIndice = :altoIndice OR altoIndice = 0)
      AND (diseno IS NULL OR diseno = :diseno OR diseno = '')
    LIMIT 1
""")
suspend fun lookup(
    opticaId: String,     // ← NUEVO
    material: String?,
    tipoLente: String?,
    stockOFabricacion: String?,
    tratamiento: String?,
    serie: Int?,
    altoIndice: Double?,
    diseno: String?
): CostoProductoEntity?
```

**Fix 2 — `CostoProductoDao.lookupLc()` con `opticaId` y fix de NULL logic**:
```kotlin
@Query("""
    SELECT * FROM costos_productos 
    WHERE optica_id = :opticaId
      AND tipoLente = 'Contacto'
      AND material = :material
      AND ((laboratorio_id IS NULL AND :laboratorioId IS NULL) OR laboratorio_id = :laboratorioId)
      AND serie = :serie
    LIMIT 1
""")
suspend fun lookupLc(
    opticaId: String,
    material: String,
    laboratorioId: String?,
    serie: Int?
): CostoProductoEntity?
```

**Fix 3 — `CostoBiseladoDao.lookup()` con `opticaId`**:
```kotlin
@Query("""
    SELECT * FROM costos_biselado 
    WHERE optica_id = :opticaId
      AND material = :material
      AND tipoAro = :tipoAro
      AND stockOFabricacion = :stockOFabricacion
      AND serie = :serie
      AND (altoIndice IS NULL OR altoIndice = :altoIndice)
    LIMIT 1
""")
suspend fun lookup(
    opticaId: String,
    material: String,
    tipoAro: String,
    stockOFabricacion: String,
    serie: Int,
    altoIndice: Double?
): CostoBiseladoEntity?
```

**Fix 4 — `DispensacionViewModel.calculateCosts()` — pasar `opticaId`**:
```kotlin
// Dentro de calculateCosts(), obtener opticaId UNA SOLA VEZ:
val currentOpticaId = sessionManager.opticaId.first()

// Para cada lookup, pasar currentOpticaId:
val costoOd = repository.lookupCostoProducto(
    opticaId = currentOpticaId,  // ← AGREGAR
    material = item.materialOd?.lowercase(),
    // ... resto de params
)
```

---

### 2.3 Eliminar métodos deprecated sin `opticaId`

**Estrategia**: Un PR por DAO. Para cada DAO con métodos deprecated, verificar callers, eliminar método, y si hay callers migrarlos a la versión con `opticaId`.

**DAO por DAO**:

**PacienteDao** — 4 métodos deprecated:
```kotlin
// ❌ ELIMINAR:
@Deprecated("Use getPacientesByOptica(opticaId)")
@Query("SELECT * FROM pacientes ORDER BY nombreCompleto ASC")
fun getAllPacientes(): Flow<List<Paciente>>

@Deprecated("Use searchPacientesByOptica(opticaId, query)")
@Query("SELECT * FROM pacientes WHERE nombreCompleto LIKE '%' || :query || '%' OR ...")
fun searchPacientes(query: String): Flow<List<Paciente>>

@Deprecated("Use getPacientesWithPendingBalanceForOptica(opticaId)")
@Query("SELECT * FROM pacientes WHERE ...")
fun getPacientesWithPendingBalance(): Flow<List<Paciente>>

@Deprecated("Use getPacientesWithPendingDeliveryForOptica(opticaId)")
fun getPacientesWithPendingDelivery(): Flow<List<Paciente>>
```

**DispensacionDao** — 5 métodos:
```kotlin
// ❌ ELIMINAR: getAllDispensaciones, getTotalVendido, getTotalPagado, 
//             getAllDispensacionesList, getDispensacionesByDateRange
```

**ServicioExtraDao** — 1 método:
```kotlin
// ❌ ELIMINAR: getAllServicios (ya existe getAllServiciosForOptica)
```

**GastoOperativoDao** — 1 método:
```kotlin
// ❌ ELIMINAR: getAll (ya existe getByOpticaId)
```

**MonturaDao** — 1 método:
```kotlin
// ❌ ELIMINAR: getMonturaById (ya existe getMonturaByIdForOptica)
```

**Y sus wrappers en repositorios**:
```kotlin
// ❌ ELIMINAR en PacienteRepository:
@Suppress("DEPRECATION")
fun getAllPacientes(): Flow<List<Paciente>> = pacienteDao.getAllPacientes()
// etc.
```

**Verificación pre-eliminación**:
```bash
# Para cada método a eliminar, verificar callers:
grep -rn "getAllPacientes()" optoapp/src/main/
grep -rn "searchPacientes(" optoapp/src/main/
grep -rn "getAllDispensaciones()" optoapp/src/main/
grep -rn "getAllServicios()" optoapp/src/main/
grep -rn "getAll\(\)" optoapp/src/main/java/com/example/optoapp/data/gastooperativo/
grep -rn "getMonturaById(" optoapp/src/main/
# Si hay callers en main/, migrarlos a la versión ForOptica antes de eliminar
```

---

### 2.4 DELETE queries con `opticaId`

**Archivos**: 6 DAOs con queries DELETE sin `opticaId`: PacienteDao, DispensacionItemDao, GastoOperativoDao, DispensacionDao, OrdenCompraItemDao, y cualquier DAO con `@Delete` sin filtro.

**Patrón**:
```kotlin
// ===== ANTES (DispensacionDao) =====
@Delete
suspend fun deleteDispensacion(dispensacion: DispensacionOptica)

// ===== DESPUÉS =====
@Query("DELETE FROM dispensaciones WHERE id = :id AND opticaId = :opticaId")
suspend fun deleteDispensacion(id: String, opticaId: String): Int
```

**Actualizar callers** (`DispensacionRepository`):
```kotlin
// ===== ANTES =====
suspend fun deleteDispensacion(dispensacion: DispensacionOptica) {
    dispensacionDao.deleteDispensacion(dispensacion)
}

// ===== DESPUÉS =====
suspend fun deleteDispensacion(dispensacion: DispensacionOptica) {
    val deleted = dispensacionDao.deleteDispensacion(dispensacion.id, dispensacion.opticaId)
    if (deleted == 0) throw IllegalStateException("Dispensacion no encontrada o no pertenece a la optica")
}
```

---

### 2.5 `ConflictRecord` — PK compuesta `(entityId, opticaId)`

**Paso 1: Entity** (`data/sync/ConflictRecord.kt:17`):
```kotlin
// ===== ANTES =====
@Entity(tableName = "conflict_records")
data class ConflictRecord(
    @PrimaryKey val entityId: String,
    val opticaId: String,
    // ...
)

// ===== DESPUÉS =====
@Entity(
    tableName = "conflict_records",
    primaryKeys = ["entityId", "opticaId"]  // ← PK compuesta
)
data class ConflictRecord(
    val entityId: String,   // ← ya no @PrimaryKey individual
    val opticaId: String,   // ← ahora parte de PK
    // ...
)
```

**Paso 2: Migración Room** (en `OptoDatabaseMigrations.kt`):
```kotlin
val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Crear nueva tabla con PK compuesta
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conflict_records_new (
                entityId TEXT NOT NULL,
                opticaId TEXT NOT NULL,
                entityType TEXT NOT NULL,
                localData TEXT NOT NULL DEFAULT '',
                remoteData TEXT NOT NULL DEFAULT '',
                baseSnapshot TEXT NOT NULL DEFAULT '{}',
                detectedAt TEXT NOT NULL,
                resolvedAt TEXT,
                resolution TEXT,
                PRIMARY KEY (entityId, opticaId)
            )
        """)
        // Migrar datos existentes (si hay duplicados, mantener el más reciente)
        db.execSQL("""
            INSERT OR IGNORE INTO conflict_records_new 
            SELECT entityId, opticaId, entityType, localData, remoteData, baseSnapshot, detectedAt, resolvedAt, resolution
            FROM conflict_records
        """)
        db.execSQL("DROP TABLE conflict_records")
        db.execSQL("ALTER TABLE conflict_records_new RENAME TO conflict_records")
    }
}
```

**Paso 3: Actualizar DAO** (`ConflictDao`):
```kotlin
// La query de upsert ya usa INSERT OR REPLACE, funciona con PK compuesta.
// La query de resolución ya usa WHERE entityId = :entityId AND opticaId = :opticaId.
// Solo verificar que clearConflicts también use opticaId:
@Query("DELETE FROM conflict_records WHERE opticaId = :opticaId")
suspend fun clearConflicts(opticaId: String)
```

---

### 2.6 `ResumenDiarioEntity` — unique constraint `(opticaId, fecha)`

**Paso 1: Entity**:
```kotlin
@Entity(
    tableName = "resumen_diario",
    indices = [Index(value = ["opticaId", "fecha"], unique = true)]
)
data class ResumenDiarioEntity(
    @PrimaryKey val id: String,
    val opticaId: String,
    val fecha: String,
    // ...
)
```

**Paso 2: Migración Room** (si el schema version aumentó):
```kotlin
db.execSQL("""
    CREATE UNIQUE INDEX IF NOT EXISTS idx_resumen_diario_unique 
    ON resumen_diario(opticaId, fecha)
""")
```

**Paso 3: Limpiar duplicados existentes** (en `onOpen` o migración):
```kotlin
db.execSQL("""
    DELETE FROM resumen_diario WHERE id NOT IN (
        SELECT MIN(id) FROM resumen_diario GROUP BY opticaId, fecha
    )
""")
```

---

### 2.7 `cantidad = delta.coerceAtLeast(0)` — audit trail corrupto

**Archivo**: `optoapp/src/main/java/com/example/optoapp/util/DispensacionStockHelper.kt` línea 104

**Código CORREGIDO**:
```kotlin
// ===== ANTES (línea 104) =====
cantidad = delta.coerceAtLeast(0),

// ===== DESPUÉS =====
cantidad = abs(delta),  // Siempre positivo, representando unidades movidas
```

**Afecta 4 call sites** (todos dentro de `adjustStockAndRegistrarMovimiento`):
- Línea 104 del helper (el fix principal)
- Cualquier código que lea `cantidad` de `MonturaMovimiento` para reportes
- `SyncInventarioUseCase` que reconstruye stock desde movimientos
- `MonturaDashboardKpiRepository` que agrega movimientos

**Test**:
```kotlin
@Test
fun `SALIDA_VENTA records correct cantidad`() = runTest {
    val monturaId = "montura_test"
    // Setup: insertar montura con stock=10
    monturaDao.insertMontura(Montura(id = monturaId, stockActual = 10, ...))

    // Act: deducir 2 unidades
    stockHelper.adjustStockAndRegistrarMovimiento(
        monturaId = monturaId,
        opticaId = "test_optica",
        delta = -2,
        tipo = "SALIDA_VENTA",
        referenciaId = "ref_001",
        nota = "Venta test"
    )

    // Assert: movimiento debe tener cantidad=2 (no 0)
    val movs = monturaMovimientoDao.getMovimientosByMontura(monturaId).first()
    assertEquals(2, movs.first().cantidad)
    assertEquals(10, movs.first().stockPrevio)  // stock antes
    assertEquals(8, movs.first().stockNuevo)     // stock después
}
```

---

### 2.8 Merge no atómico en `SyncFinanzasMerge`

**Archivo**: `domain/SyncFinanzasMerge.kt` líneas 54-56 y 88-89

**Código CORREGIDO**:
```kotlin
// ===== ANTES (mergeLocalDispensacionConflict, línea 54-56) =====
repository.updateDispensacion(merged)
repository.reassignPagosDispensacion(duplicate.id, merged.id, ...)
repository.deleteDispensacionById(duplicate.id, ...)

// ===== DESPUÉS =====
repository.withTransaction {
    repository.updateDispensacion(merged)
    repository.reassignPagosDispensacion(duplicate.id, merged.id, ...)
    // También reasignar DispensacionItem y RegaloDispensacion antes de borrar
    repository.reassignItemsDispensacion(duplicate.id, merged.id)
    repository.reassignRegalosDispensacion(duplicate.id, merged.id)
    repository.deleteDispensacionById(duplicate.id, merged.opticaId)
}
```

**Mismo patrón para `resolveLocalDuplicateDispensaciones` (línea 88-89)**:
```kotlin
repository.withTransaction {
    // ... mismas operaciones
}
```

**Requiere**: Agregar `reassignItemsDispensacion` y `reassignRegalosDispensacion` al DAO:
```kotlin
@Query("UPDATE dispensacion_items SET dispensacionId = :targetId WHERE dispensacionId = :sourceId")
suspend fun reassignItemsDispensacion(sourceId: String, targetId: String): Int

@Query("UPDATE regalos_dispensacion SET dispensacionId = :targetId WHERE dispensacionId = :sourceId")
suspend fun reassignRegalosDispensacion(sourceId: String, targetId: String): Int
```

---

### 2.9 `save()` en `InformacionFinancieraViewModel` sin error handling

**Archivo**: `viewmodel/InformacionFinancieraViewModel.kt` líneas 119-157

**Código CORREGIDO**:
```kotlin
fun save(onComplete: () -> Unit) {
    if (_uiState.value.isLoading) return  // ← GUARD contra doble-tap

    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val s = _uiState.value
            val opticaId = sessionManager.opticaId.first()

            repository.withTransaction {
                repository.actualizarMontoTotal(s.dispensacionId, s.montoTotal, opticaId)
                repository.actualizarEstado(s.dispensacionId, s.estadoEntrega, s.fechaEntrega, opticaId)

                s.pagos.filter { it.id !in s.initialPagoIds }.forEach { pago ->
                    repository.insertPago(pago)
                }
                s.pagos.filter { it.id in s.initialPagoIds }.forEach { pago ->
                    repository.updatePago(pago)
                }
                s.pagosToDelete.forEach { pagoId ->
                    repository.deletePago(pagoId, opticaId)
                }
            }

            postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
            _uiState.update { it.copy(isLoading = false, pagosToDelete = emptyList()) }
            onComplete()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving financial info", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}"
                )
            }
        }
    }
}
```

---

### 2.10 `removeRegaloAndRestoreStock` — borra TODOS los regalos

**Archivo**: `viewmodel/RegaloDispensacionViewModel.kt` línea 49

**Paso 1: Agregar `deleteById` al DAO** (`RegaloDispensacionDao.kt`):
```kotlin
@Query("DELETE FROM regalos_dispensacion WHERE id = :id")
suspend fun deleteById(id: String): Int
```

**Paso 2: Corregir ViewModel**:
```kotlin
// ===== ANTES (línea 49) =====
repository.deleteRegalosByDispensacionId(regalo.dispensacionId)

// ===== DESPUÉS =====
repository.deleteRegaloById(regalo.id)  // Solo borra este regalo específico
```

**Paso 3: Verificar otros callers de `deleteRegalosByDispensacionId`**:
```bash
grep -rn "deleteRegalosByDispensacionId" optoapp/src/main/
```
Si hay callers legítimos que necesitan borrar todos los regalos de una dispensación (ej: al eliminar la dispensación completa), mantener el método pero renombrarlo para claridad:
```kotlin
// Renombrar para que el nombre refleje el comportamiento:
@Query("DELETE FROM regalos_dispensacion WHERE dispensacionId = :dispensacionId")
suspend fun deleteAllByDispensacionId(dispensacionId: String): Int
```

---

### 2.11 Client-only billing → validación backend con Edge Function

**Paso 1: Crear Edge Function** (`supabase/functions/verify-purchase/index.ts`):
```typescript
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const PACKAGE_NAME = "com.example.optoapp"

serve(async (req: Request) => {
  try {
    const { purchaseToken, opticaId } = await req.json()
    const authHeader = req.headers.get("Authorization")!
    
    // Verify caller is authenticated
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    )
    const { data: { user }, error: authError } = await supabase.auth.getUser(
      authHeader.replace("Bearer ", "")
    )
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
    }

    // Verify purchase with Google Play Developer API
    const accessToken = await getGoogleAccessToken()
    const response = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/purchases/subscriptions/${purchaseToken}`,
      { 
        headers: { 
          Authorization: `Bearer ${accessToken}`,
          Accept: "application/json"
        } 
      }
    )
    
    if (!response.ok) {
      return new Response(JSON.stringify({ 
        valid: false, 
        error: "Purchase verification failed" 
      }), { status: 400 })
    }

    const purchase = await response.json()
    const isValid = purchase.purchaseState === 0 || purchase.purchaseState === 1
    
    if (isValid) {
      // Update optica plan to PRO
      await supabase
        .from("opticas")
        .update({ plan: "pro", plan_updated_at: new Date().toISOString() })
        .eq("id", opticaId)
    }

    return new Response(JSON.stringify({ valid: isValid }))
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }
})

async function getGoogleAccessToken(): Promise<string> {
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: await createJwt()
    })
  })
  const data = await response.json()
  return data.access_token
}
```

**Paso 2: Modificar Android** (`PlayBillingManager.kt`):
```kotlin
private fun acknowledgeAndGrant(purchase: Purchase) {
    ioScope.launch {
        val c = client ?: return@launch
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            c.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Solo después de acknowledge exitoso, verificar con backend
                    verifyPurchaseOnBackend(purchase.purchaseToken)
                }
            }
        }
    }
}

private suspend fun verifyPurchaseOnBackend(purchaseToken: String) {
    try {
        val opticaId = sessionManager.opticaId.first()
        val result = supabase.functions.invoke("verify-purchase", buildJsonObject {
            put("purchaseToken", purchaseToken)
            put("opticaId", opticaId)
        })
        val body = result.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject
        
        if (json["valid"]?.jsonPrimitive?.boolean == true) {
            subscriptionManager.setProConfirmed(json["tier"]?.jsonPrimitive?.content ?: "pro")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Purchase verification failed", e)
        // NO conceder PRO si la verificación falla
    }
}
```

---

### 2.12 `auth.uid()` validation en RPCs financieros

> ⚠️ **CONSOLIDAR en Fase 2 (2026-07-16)**: Este fix se consolida con 2.19 (COALESCE proyeccion_caja) y 2.29 (BI role gate) en UNA sola migración que toca `rpc_analisis_mensual`, `rpc_deudores` y `rpc_cierre_caja_resumen`. No crear archivo separado. Al llegar a Fase 2, los tres fixes se implementan juntos.

**Archivo a crear**: ~~`supabase/migrations/20260715000008_add_auth_checks_to_financial_rpcs.sql`~~ → CANCELADO, consolidar con 2.19+2.29

```sql
-- Add auth.uid() membership verification to all financial RPCs
-- This is defense-in-depth: RLS already protects, but explicit check is safer

-- ===== rpc_analisis_mensual =====
CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(
    p_optica_id text,
    p_mes date
) RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ''
AS $$
BEGIN
    -- AUTH CHECK
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied: user % is not a member of optica %', auth.uid(), p_optica_id;
    END IF;
    -- ... resto del cuerpo existente ...
END;
$$;

-- ===== rpc_deudores (mismo patrón) =====
CREATE OR REPLACE FUNCTION public.rpc_deudores(p_optica_id text) ...
-- Agregar: IF NOT app_private.is_optica_member(...) THEN RAISE EXCEPTION ...

-- ===== rpc_cierre_caja_resumen (mismo patrón) =====
CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(p_optica_id text, p_from date, p_to date) ...
-- Agregar: IF NOT app_private.is_optica_member(...) THEN RAISE EXCEPTION ...
```

---

### 2.13 `FREE_MAX_PACIENTES = Int.MAX_VALUE` → límites reales

**Archivo**: `SubscriptionManager.kt`

```kotlin
// ===== ANTES =====
const val FREE_MAX_PACIENTES = Int.MAX_VALUE

// ===== DESPUÉS =====
const val FREE_MAX_PACIENTES = 50
const val FREE_MAX_DISPENSACIONES_MENSUALES = 20
const val FREE_MAX_USUARIOS = 2  // admin + 1 staff

// En canAddPaciente:
fun canAddPaciente(tier: SubscriptionTier, currentPacienteCount: Int): Boolean {
    return when (tier) {
        SubscriptionTier.FREE -> currentPacienteCount < FREE_MAX_PACIENTES
        SubscriptionTier.PRO -> true
    }
}
```

**Backend enforcement** (agregar a RPC de creación de paciente o a trigger):
```sql
CREATE OR REPLACE FUNCTION public.check_patient_limit()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_count int;
    v_plan text;
BEGIN
    SELECT plan INTO v_plan FROM public.opticas WHERE id = NEW.optica_id;
    SELECT COUNT(*) INTO v_count FROM public.pacientes WHERE optica_id = NEW.optica_id;
    
    IF v_plan = 'free' AND v_count >= 50 THEN
        RAISE EXCEPTION 'Free plan limit reached: 50 patients maximum';
    END IF;
    
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_check_patient_limit
BEFORE INSERT ON public.pacientes
FOR EACH ROW EXECUTE FUNCTION public.check_patient_limit();
```

---

### 2.14 `CostosBiselado` nunca se suben — data loss silencioso

**Archivo**: `domain/SyncFinanzasUseCase.kt` (entre líneas 62-77 donde están las demás entidades)

**Diagnóstico**: La secuencia de upload en `invoke()` incluye `uploadCostosProducto` pero NO `uploadCostosBiselado`. `CostoBiseladoEntity.toRemoto()` existe en `SyncFinanzasDto.kt:370` — el código de serialización está listo pero nunca se invoca.

**Fix**: Agregar inmediatamente después del upload de `costos_producto`:
```kotlin
val costosUp = safeUpload("costos_producto", finanzasSyncResult) {
    uploadSyncCoordinator.uploadCostosProducto(opticaId)
}

// ===== AGREGAR ESTE BLOQUE =====
val biseladoUp = safeUpload("costos_biselado", finanzasSyncResult) {
    uploadSyncCoordinator.uploadCostosBiselado(opticaId)
}
```

**Agregar método en `UploadSyncCoordinator`**:
```kotlin
suspend fun uploadCostosBiselado(opticaId: String): Int {
    val rows = costoBiseladoDao.getByOpticaIdList(opticaId)
    if (rows.isEmpty()) return 0
    return networkRetryHelper.retryNetwork("upload-costos_biselado") {
        supabase.postgrest["costos_biselado"].upsert(rows.map { it.toRemoto() })
        rows.size
    }
}
```

**Actualizar `FinanzasSyncResult`**:
```kotlin
data class FinanzasSyncResult(
    // ... existing fields ...
    val uploadedCostosBiselado: Int = 0,
)
// En invoke(), agregar al constructor:
uploadedCostosBiselado = biseladoUp,
```

**Test de regresión**:
```kotlin
@Test
fun `costos biselado are uploaded during finanzas sync`() = runTest {
    costoBiseladoDao.insert(CostoBiseladoEntity(id = "cb1", opticaId = "test", material = "Resina", costoPorPar = 5.0))
    val result = syncFinanzasUseCase.invoke("test")
    assertEquals(1, result.uploadedCostosBiselado)
}
```

---

### 2.15 `uploadServicios` — last-write-wins descarta duplicados

**Archivo**: `UploadSyncCoordinator.kt` (método `uploadServicios`)

**Fix**: Agregar detección de colisiones con warning y merge por `updatedAt`:
```kotlin
val uniqueRows = LinkedHashMap<String, ServicioRemoto>()
val collisionLog = mutableListOf<String>()

for (row in rows.map { it.toRemoto() }) {
    val dedupeKey = row.ot?.trim()?.takeIf { it.isNotBlank() } ?: "id:${row.id}"
    val existing = uniqueRows[dedupeKey]
    if (existing != null && existing.id != row.id) {
        val winner = if ((row.updatedAt ?: "") > (existing.updatedAt ?: "")) row else existing
        uniqueRows[dedupeKey] = winner
        collisionLog.add("OT collision '$dedupeKey': kept ${winner.id}, dropped ${if (winner == row) existing.id else row.id}")
    } else {
        uniqueRows[dedupeKey] = row
    }
}
if (collisionLog.isNotEmpty()) Log.w(TAG, "Servicio upload collisions: ${collisionLog.joinToString("; ")}")
```

---

### 2.16 `SyncFinanzasUploaders.kt` — 248 líneas de código muerto

**Diagnóstico**: Clase `internal`, sin `@Inject`, sin callers en `main/`. Duplica `UploadSyncCoordinator` con bugs sutiles.

**Fix**:
```bash
# Verificar:
grep -rn "SyncFinanzasUploaders" optoapp/src/main/ --include="*.kt"  # → 0 resultados

# Eliminar:
git rm optoapp/src/main/java/com/example/optoapp/domain/SyncFinanzasUploaders.kt
git rm optoapp/src/test/java/com/example/optoapp/domain/SyncFinanzasUploadersTest.kt

git commit -m "refactor: remove dead SyncFinanzasUploaders (248 lines, never instantiated)"
```

---

### 2.17 Fetch reconciliación falla → duplicados en Supabase

**Archivo**: `UploadSyncCoordinator.kt` (métodos `uploadDispensaciones`, `uploadServicios`)

**Fix**: No proceder con `emptyList()` si falla el fetch de reconciliación:
```kotlin
val remotosExistentes = try {
    networkRetryHelper.retryNetwork("fetch-remote-for-reconcile") {
        supabase.postgrest[table].select { filter { eq("optica_id", opticaId) } }.decodeList<T>()
    }
} catch (e: CancellationException) { throw e }
  catch (e: Exception) {
    Log.e(TAG, "FATAL: Cannot reconcile with remote. Aborting to prevent duplicates.")
    throw UploadPreCheckFailedException("Reconciliation fetch failed", e)
}
```

---

### 2.18 Download no aislado por entidad

**Archivo**: `SyncFinanzasUseCase.kt` líneas 89-109

**Fix**: Envolver cada download en try-catch individual:
```kotlin
private suspend fun safeDownload(name: String, block: suspend () -> Int): Int = try {
    block()
} catch (e: CancellationException) { throw e }
  catch (e: Exception) { Log.e(TAG, "Download $name failed", e); 0 }

// Uso:
val dispDl = safeDownload("dispensaciones") { downloadSyncCoordinator.downloadDispensaciones(opticaId) }
val itemsDl = safeDownload("items") { downloadSyncCoordinator.downloadDispensacionItems(opticaId) }
// ... todas las entidades con el mismo patrón
```

---

### 2.19 `proyeccion_caja` sin fallback `venta_id`

> ⚠️ **CONSOLIDAR en Fase 2 (2026-07-16)**: Este fix se consolida con 2.12 (auth.uid) y 2.29 (BI role gate) en UNA sola migración. No crear archivo separado.

**Archivo a crear**: ~~`supabase/migrations/20260715000009_fix_proyeccion_caja_coalesce.sql`~~ → CANCELADO, consolidar con 2.12+2.29

```sql
CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(p_optica_id text, p_mes date) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER SET search_path = '' AS $$
DECLARE
    v_result jsonb;
BEGIN
    IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    WITH proyeccion_caja AS (
        SELECT
            COALESCE(pg.venta_id,
                'v_disp_' || pg.dispensacion_id,
                'v_serv_' || pg.servicio_extra_id
            ) AS venta_id_match,  -- ← FIX: COALESCE fallback
            SUM(pg.monto) AS total_pagado
        FROM public.pagos pg
        WHERE pg.optica_id = p_optica_id AND pg.tipo IS DISTINCT FROM 'Anulación'
        GROUP BY 1
    )
    -- ... resto del SELECT que construye v_result ...
    SELECT jsonb_build_object(...) INTO v_result FROM ...
    RETURN v_result;
END;
$$;
```

---

### 2.20 ~~`recalcular_resumen_diario` — ventas incluyen anuladas~~ → ABSORBIDO en Fix 1.4

> ⚠️ **CANCELADO (2026-07-16)**: El filtro `estado IS DISTINCT FROM 'Anulado'` ya se incluyó en Fix 1.4 consolidado. La función `recalcular_resumen_diario` queda completa con SECURITY DEFINER + anulado filter + cuerpo de `20260714000000`. No se requiere migración adicional.

---

### 2.21 `costo = 0.0` sistémico — márgenes falsos

**Archivo**: `domain/ObtenerMovimientosFinancierosUseCase.kt`

**Fix**: Popular `costo` desde `dispensacion_items` y `servicios_extra`:
```kotlin
// Obtener costos agrupados
val costosByDisp = repository.getCostosByDispensacionIds(dispensaciones.map { it.id }.toSet())

val movs = dispensaciones.map { d ->
    MovimientoFinanciero(
        // ...
        costo = costosByDisp[d.id] ?: 0.0,  // ← real cost from items
    )
}
```

**Nuevo query en DispensacionItemDao**:
```kotlin
@Query("SELECT dispensacionId, COALESCE(SUM(costoReal), 0) FROM dispensacion_items WHERE dispensacionId IN (:ids) GROUP BY dispensacionId")
suspend fun getCostosByDispensacionIds(ids: Set<String>): Map<String, Double>
```

---

### 2.22 Regalos excluidos del modelo financiero

**Archivo**: `domain/ObtenerMovimientosFinancierosUseCase.kt`

**Fix**: Agregar regalos al stream de movimientos:
```kotlin
val regalos = repository.getRegalosSnapshotForOptica(opticaId, start, end) ?: emptyList()
val movsRegalos = regalos.map { r ->
    MovimientoFinanciero(
        id = r.id, fecha = r.fecha,
        tipo = TipoMovimiento.REGALO, origen = Origen.REGALO,
        montoTotal = 0.0, montoPagado = 0.0,
        costo = (r.costoUnitario ?: 0.0) * r.cantidad,
        descripcion = "Regalo: ${r.productoNombre ?: "Producto"} x${r.cantidad}",
        vinculadoA = r.dispensacionId
    )
}
return movsDispensaciones + movsServicios + movsRegalos
```

---

### 2.23 PDF infla `montoPagado` al excluir anulaciones

**Archivo**: `util/ReporteFinancieroPdfGenerator.kt`

**Fix A**: Incluir anulaciones en el cálculo para netear:
```kotlin
val pagosNetosByDisp = pagos
    .filter { it.dispensacionId != null }
    .groupBy { it.dispensacionId!! }
    .mapValues { (_, list) -> list.sumOf { it.monto } }  // anulaciones con monto negativo netean
```

**Fix B**: Saldo negativo en rojo:
```kotlin
val saldo = disp.montoTotal - montoPagadoNeto
val saldoColor = if (saldo < 0) android.graphics.Color.RED else android.graphics.Color.BLACK
```

---

### 2.24 `crearReclamo` — refund sin validación

**Archivo**: `viewmodel/DispensacionViewModel.kt`

**Fix**: Validar `totalPagadoOriginal` antes de crear refund:
```kotlin
val totalPagadoOriginal = calcularMontoPagadoUseCase(originalDispensacionId)
require(totalPagadoOriginal > 0) { "No hay pagos registrados para crear un reclamo" }

val diff = nuevoMontoTotal - totalPagadoOriginal
if (diff < 0) {
    val refundAmount = abs(diff)
    require(refundAmount <= totalPagadoOriginal) { "El monto de reembolso excede lo pagado" }
    // Crear pago de anulación con monto positivo (no negativo)
    val refundPago = Pago(
        monto = refundAmount,
        tipo = "Anulación",
        nota = "Reclamo: ajuste de S/ $nuevoMontoTotal (pagado: S/ $totalPagadoOriginal)"
    )
    repository.insertPago(refundPago)
}
```

---

### 2.25 Biselado lookup — `stockOFabricacion` hardcodeado

**Archivo**: `viewmodel/DispensacionViewModel.kt` línea 803

**Fix**: Derivar `stockOFabricacion` de los parámetros del lente:
```kotlin
val esFabricacion = (item.esferaOd?.let { abs(it) } ?: 0.0) > 6.0
                 || (item.esferaOi?.let { abs(it) } ?: 0.0) > 6.0
val stockOFabricacion = if (esFabricacion) "fabricacion" else "stock"
val serie = item.serieOd ?: item.serieOi ?: 1

val biseladoLookup = repository.lookupCostoBiselado(
    opticaId = currentOpticaId,
    material = item.materialMontura.ifBlank { "Resina" },
    tipoAro = tipoAro,
    stockOFabricacion = stockOFabricacion,  // ← dinámico
    serie = serie,
    altoIndice = item.indiceOd ?: item.indiceOi
)
```

---

### 2.26 Evaluaciones — `autoPresbicia`/`autoAnisometropia` descartados

**Archivo**: `viewmodel/EvaluacionMapping.kt` línea 70-76

**Fix**: Agregar los campos faltantes en `toEvaluacionClinica()`:
```kotlin
// ===== ANTES =====
autoAmbliopia = s.autoAmbliopia,
// faltan autoPresbicia y autoAnisometropia

// ===== DESPUÉS =====
autoPresbicia = s.autoPresbicia,          // ← AGREGAR
autoAnisometropia = s.autoAnisometropia,  // ← AGREGAR
autoAmbliopia = s.autoAmbliopia,
```

**Test**:
```kotlin
@Test
fun `autoPresbicia and autoAnisometropia survive roundtrip`() {
    val uiState = EvaluacionUiState(autoPresbicia = false, autoAnisometropia = true)
    val entity = uiState.toEvaluacionClinica("id", "pacId", "optId", null)
    assertEquals(false, entity.autoPresbicia)
    assertEquals(true, entity.autoAnisometropia)
}
```

---

### 2.27 Evaluaciones — `parseSnellenToLogMar` hardcodea 20ft

**Archivo**: `viewmodel/diagnostico/DiagnosticoCalculator.kt` línea 79

**Fix**: Usar el numerador capturado por el regex:
```kotlin
fun parseSnellenToLogMar(snellen: String): Double? {
    val regex = Regex("""(\d+)\s*/\s*(\d+)""")
    val m = regex.find(snellen.trim()) ?: return null
    val numerator = m.groupValues[1].toDoubleOrNull() ?: return null
    val denominator = m.groupValues[2].toDoubleOrNull() ?: return null
    if (denominator <= 0) return null
    val decimalAV = numerator / denominator  // ← ANTES: 20.0 / denominator
    return -log10(decimalAV)
}
```

**Test**:
```kotlin
@Test
fun `6m notation produces correct LogMAR`() {
    assertEquals(0.0, parseSnellenToLogMar("6/6")!!, 0.01)    // 6/6 = 0.0 LogMAR
    assertEquals(0.30, parseSnellenToLogMar("6/12")!!, 0.01)  // 6/12 ≈ 0.30
    assertEquals(0.0, parseSnellenToLogMar("20/20")!!, 0.01)   // 20/20 = 0.0 (unchanged)
}
```

---

### 2.28 Evaluaciones — `proximaFechaControl` nunca poblado

**Archivo**: `viewmodel/EvaluacionUiState.kt` + `viewmodel/EvaluacionMapping.kt`

**Fix**: Agregar campo al UiState y mapear bidireccionalmente:
```kotlin
// EvaluacionUiState.kt — agregar campo
val proximaFechaControl: String = "",

// EvaluacionMapping.kt — toEvaluacionClinica()
proximaFechaControl = s.proximaFechaControl,

// EvaluacionMapping.kt — toEvaluacionUiState()
proximaFechaControl = e.proximaFechaControl,
```

---

### 2.29 Analytics — BI role gate backend enforcement

> ⚠️ **CONSOLIDAR en Fase 2 (2026-07-16)**: Este fix es el CANONICAL para la consolidación de 2.12+2.19+2.29. Al llegar a Fase 2, UNA sola migración implementa los tres cambios (auth.uid + COALESCE + BI role gate) para `rpc_analisis_mensual`, `rpc_deudores` y `rpc_cierre_caja_resumen`.

**Archivo**: ~~`supabase/migrations/20260715000011_add_bi_role_check.sql`~~ → reemplazar por archivo único consolidado

```sql
-- RPCs de analytics deben verificar rol además de membresía
CREATE OR REPLACE FUNCTION public.rpc_analisis_mensual(p_optica_id text, p_mes date) RETURNS jsonb
LANGUAGE plpgsql SECURITY INVOKER SET search_path = '' AS $$
DECLARE
    v_rol text;
BEGIN
    SELECT lower(trim(rol)) INTO v_rol FROM public.usuario_optica
    WHERE user_id = auth.uid() AND optica_id = p_optica_id;
    
    IF v_rol NOT IN ('admin', 'gerente') THEN
        RAISE EXCEPTION 'Access denied: BI access requires admin or gerente role';
    END IF;
    -- ... resto del cuerpo
END; $$;

-- Mismo patrón para rpc_deudores y rpc_cierre_caja_resumen
```

---

### 2.30 `GenerarRecomendacionesUseCase` — seasonal suppression usa mes real, no el consultado

**Archivo**: `domain/GenerarRecomendacionesUseCase.kt` línea 211-212

**Fix**: Usar el mes del análisis, no `LocalDate.now()`:
```kotlin
// ===== ANTES =====
val hoy = LocalDate.now()
val currentMonth = hoy.monthValue
if (currentMonth in listOf(1, 2)) return null

// ===== DESPUÉS =====
// El mes analizado viene en los datos, no del reloj
val viewedMonth = analisis.mes.monthValue  // o pasar mesSeleccionado como parámetro
if (viewedMonth in listOf(1, 2)) return null
```

---

### 2.31 Suppliers — `create()` y `receiveItems()` no transaccionales

**Archivo**: `data/OrdenCompraRepository.kt`

**Fix `create()`**:
```kotlin
open suspend fun create(oc: OrdenCompra, items: List<OrdenCompraItem>) {
    database.withTransaction {
        ocDao.insert(oc)
        items.forEach { itemDao.insert(it) }
    }
}
```

**Fix `receiveItems()`**:
```kotlin
open suspend fun receiveItems(ocId: String, receivedItems: Map<String, Int>) {
    database.withTransaction {
        val oc = ocDao.getById(ocId) ?: throw IllegalStateException("OC not found")
        var allReceived = true
        for ((itemId, qty) in receivedItems) {
            itemDao.updateRecibido(itemId, qty)
            if (qty == 0) allReceived = false
        }
        ocDao.update(oc.copy(
            estado = if (allReceived) "COMPLETADA" else "PARCIAL",
            updatedAt = Instant.now().toString()
        ))
        // Stock + movimiento en la misma transacción
        for ((itemId, qty) in receivedItems) {
            val item = itemDao.getById(itemId) ?: continue
            val montura = monturaDao.getMonturaByIdForOptica(item.monturaId, oc.opticaId) ?: continue
            monturaDao.adjustStock(item.monturaId, oc.opticaId, qty)
            movimientoDao.insertMovimiento(MonturaMovimiento(
                monturaId = item.monturaId,
                tipo = "ENTRADA_COMPRA",
                cantidad = qty,
                stockPrevio = montura.stockActual,
                stockNuevo = montura.stockActual + qty,
                referenciaId = ocId,
                userId = oc.updatedBy ?: "system",
                nota = "Recepción OC $ocId"
            ))
        }
    }
}
```

---

### 2.32 Suppliers — `stockPrevio=0` / `stockNuevo=0` en movimientos

**Archivo**: `data/OrdenCompraRepository.kt` líneas 94-100

**Fix**: Capturar stock real antes y después:
```kotlin
val montura = monturaDao.getMonturaByIdForOptica(item.monturaId, oc.opticaId) ?: continue
val stockAntes = montura.stockActual
monturaDao.adjustStock(item.monturaId, oc.opticaId, qty)  // aplica el cambio
val stockDespues = stockAntes + qty

movimientoDao.insertMovimiento(MonturaMovimiento(
    stockPrevio = stockAntes,   // ← real
    stockNuevo = stockDespues,  // ← real
    // ...
))
```

---

### 2.33 Services — `confirmDelete` no transaccional

**Archivo**: `viewmodel/ServiciosViewModel.kt` líneas 279-308

**Fix**: Envolver en transacción:
```kotlin
fun confirmDelete() {
    val servicio = _servicioToDelete.value ?: return
    viewModelScope.launch {
        try {
            repository.runInTransaction {
                val existingPagos = repository.getPagosByServicioExtra(servicio.id).first()
                    .filter { it.tipo != "Anulación" }
                existingPagos.forEach { pago ->
                    repository.insertPago(pago.copy(
                        id = UUID.randomUUID().toString(),
                        tipo = "Anulación",
                        monto = abs(pago.monto),  // positivo
                        nota = "Anulación de servicio ${servicio.descripcion.take(24)}"
                    ))
                }
                repository.updateServicio(servicio.copy(estado = "Anulado"))
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Error al anular servicio: ${e.message}") }
        }
    }
}
```

---

### 2.34 Physical Inventory — `closeSession` no transaccional + O(n²)

**Archivo**: `data/InventarioFisicoRepository.kt` líneas 79-108

**Fix**: Transacción + bulk update:
```kotlin
suspend fun closeSession(session: InventarioFisico) {
    database.withTransaction {
        val detalles = ifDao.getDetalles(session.id)
        for (d in detalles) {
            val diff = d.diferencia ?: 0
            if (diff != 0) {
                val montura = monturaDao.getMonturaByIdForOptica(d.monturaId, session.opticaId)
                    ?: continue
                monturaDao.adjustStock(d.monturaId, session.opticaId, diff)
                movimientoDao.insertMovimiento(MonturaMovimiento(
                    monturaId = d.monturaId,
                    tipo = "AJUSTE_INVENTARIO",
                    cantidad = abs(diff),
                    stockPrevio = montura.stockActual,
                    stockNuevo = montura.stockActual + diff,
                    referenciaId = session.id,
                    nota = "Cierre de inventario físico"
                ))
            }
        }
        ifDao.updateSession(session.copy(estado = "COMPLETADO", updatedAt = Instant.now().toString()))
    }
}
```

**Fix O(n²) en `upsertDetalle`**:
```kotlin
// ===== ANTES =====
val existing = ifDao.getDetalles(detalle.inventarioId).find { it.id == detalle.id }

// ===== DESPUÉS =====
val existing = ifDao.getDetalleById(detalle.id)  // query directa, O(1)

// Nuevo método en DAO:
@Query("SELECT * FROM inventario_fisico_detalle WHERE id = :id")
suspend fun getDetalleById(id: String): InventarioFisicoDetalle?
```

---

### 2.35 Physical Inventory — `uploadSessions` reporta count incorrecto

**Archivo**: `domain/SyncInventarioFisicoUseCase.kt` línea 91

**Fix**: Retornar `safeRows.size` en vez de `list.size`:
```kotlin
// ===== ANTES =====
return list.size  // incluye rows filtradas por conflicto

// ===== DESPUÉS =====
return safeRows.size  // solo las que realmente se subieron
```

---

### 2.36 Hilt/DI — `SupabaseObserver` doble instancia

**Archivo**: `di/ObserverModule.kt` líneas 16-22

**Fix**: Proveer una sola instancia y bindear ambas interfaces:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ObserverModule {
    
    @Provides
    @Singleton
    fun provideSupabaseObserver(supabase: SupabaseClient): SupabaseObserver {
        return SupabaseObserver(supabase)
    }

    @Binds
    @Singleton
    abstract fun bindMembershipObserver(observer: SupabaseObserver): MembershipObserver

    @Binds
    @Singleton
    abstract fun bindTableObserver(observer: SupabaseObserver): TableObserver
}
```

---

### 2.37 Hilt/DI — DAO providers sin `@Singleton`

**Archivo**: `di/DatabaseModule.kt` líneas 63-132

**Fix**: Agregar `@Singleton` a todos los providers de DAO:
```kotlin
@Provides @Singleton
fun providePacienteDao(db: OptoDatabase): PacienteDao = db.pacienteDao()

@Provides @Singleton
fun provideEvaluacionDao(db: OptoDatabase): EvaluacionDao = db.evaluacionDao()
// ... repetir para los 24 DAOs
```

---

### 2.38 `OptoSegmentedSelector` — theme bypass + accesibilidad

**Archivo**: `ui/components/OptoSegmentedSelector.kt`

**Fix**:
```kotlin
@Composable
fun OptoSegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)    // ← usar theme
            .semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { index, option ->
            Surface(
                onClick = {
                    onSelectionChange(index)
                    role = Role.RadioButton
                },
                color = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                contentColor = if (index == selectedIndex) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .semantics {
                        selected = (index == selectedIndex)
                        role = Role.Tab
                        stateDescription = if (index == selectedIndex) "Seleccionado" else ""
                    }
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,  // ← usar theme, no 13.sp
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
```

---

### 2.39 Drawer navigation — `contentDescription` + accessibility

**Archivo**: `ui/components/MainDrawerContent.kt`

**Fix**: Agregar `contentDescription` a TODOS los iconos y `heading()` a secciones:
```kotlin
// Secciones con heading
Text("GESTIÓN", modifier = Modifier.semantics { heading() }, ...)
Text("FINANZAS", modifier = Modifier.semantics { heading() }, ...)

// Iconos con contentDescription
Icon(Icons.Default.People, contentDescription = "Pacientes", ...)
Icon(Icons.Default.Inventory, contentDescription = "Monturas", ...)
Icon(Icons.Default.ShoppingCart, contentDescription = "Dispensaciones", ...)
// ... todos los iconos
```

---

### 2.40 `collectAsState(initial = "admin")` en 9+ pantallas

**Fix global**:
```bash
# Buscar todas las ocurrencias:
grep -rn "collectAsState.*initial.*\"admin\"" optoapp/src/main/

# Reemplazar en cada archivo:
# ANTES: val opticaRol by viewModel.opticaRol.collectAsState(initial = "admin")
# DESPUÉS: val opticaRol by viewModel.opticaRol.collectAsState(initial = "")
# AGREGAR: if (opticaRol.isEmpty()) return  // no renderizar hasta tener rol real
```

---

### 2.41 `LaboratorioConfigViewModel.save()` sin error handling

**Archivo**: `viewmodel/LaboratorioConfigViewModel.kt` líneas 68-74

**Fix**:
```kotlin
fun save(laboratorioNombre: String, laboratorioContacto: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val oid = sessionManager.opticaId.first()
            val result = membershipRepository.updateOpticaLaboratorioSettings(
                oid, laboratorioNombre.trim(), laboratorioContacto.trim()
            )
            if (result.isSuccess) {
                settings.save(oid, laboratorioNombre, laboratorioContacto)
                _uiState.update { it.copy(isLoading = false, success = "Configuración guardada") }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
        }
    }
}
```

---

## Fase 3: Rehabilitación (HIGH — 3 semanas)

**Objetivo**: Corregir Schema Drift Room↔Supabase, endurecer Sync, mejorar UX y eliminar fugas de datos en logs.

---

### 3.1 Schema Drift: Alinear 7 tablas UUID vs TEXT PK

**Tablas afectadas**: `proveedores`, `ordenes_compra`, `inventario_fisico`, `montura_proveedor`, `categorias_montura`, `inventario_fisico_detalle`, `orden_compra_items`

**Diagnóstico**: Room usa `String` (TEXT) como PK. Supabase usa `UUID DEFAULT gen_random_uuid()`. Al hacer INSERT desde Android con un ID string, Supabase espera UUID y puede rechazarlo o coercionarlo incorrectamente.

**Solución — Migración Supabase**:
```sql
-- Migración: 20260715000012_align_uuid_pks_to_text.sql

-- Para cada tabla afectada, agregar columna text_id y trigger de sincronización
DO $$
DECLARE
    tbl text;
BEGIN
    FOR tbl IN 
        SELECT unnest(ARRAY['proveedores','ordenes_compra','inventario_fisico',
                             'montura_proveedor','categorias_montura',
                             'inventario_fisico_detalle','orden_compra_items'])
    LOOP
        -- Agregar columna text_id si no existe
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS text_id text', tbl);
        
        -- Crear índice único en text_id
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS idx_%I_text_id ON public.%I(text_id)', tbl, tbl);
        
        -- Trigger: al insertar, si id es UUID, copiar a text_id; si es texto, usar tal cual
        EXECUTE format('
            CREATE OR REPLACE FUNCTION public.sync_%I_text_id() RETURNS trigger AS $fn$
            BEGIN
                IF NEW.text_id IS NULL OR NEW.text_id = '''' THEN
                    NEW.text_id := NEW.id::text;
                END IF;
                RETURN NEW;
            END;
            $fn$ LANGUAGE plpgsql;
        ', tbl);
        
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_sync_%I_text_id ON public.%I;
            CREATE TRIGGER trg_sync_%I_text_id BEFORE INSERT OR UPDATE ON public.%I
            FOR EACH ROW EXECUTE FUNCTION public.sync_%I_text_id();
        ', tbl, tbl, tbl, tbl, tbl);
        
        -- Backfill: copiar id::text a text_id para filas existentes
        EXECUTE format('UPDATE public.%I SET text_id = id::text WHERE text_id IS NULL', tbl);
    END LOOP;
END;
$$;
```

**Solución — Android (largo plazo)**: Migrar Room entities a usar `@PrimaryKey val id: String = UUID.randomUUID().toString()` y que el ID sea generado por el cliente como string desde el inicio, consistente con Supabase `text_id`.

---

### 3.2 Schema Drift: Alinear 7 columnas boolean (INTEGER vs boolean)

**Tablas afectadas**: `evaluaciones` (4 cols), `monturas`, `proveedores`, `feedback_recomendaciones`

**Diagnóstico**: Room mapea `Boolean` → SQLite `INTEGER` (0/1). Supabase usa `boolean` nativo. La coerción automática de PostgREST funciona para 0/1 → true/false, pero `NULL` o valores atípicos pueden fallar.

**Solución — TypeConverter en Room** (más seguro que migrar Supabase):
```kotlin
// Nuevo archivo: data/converter/BooleanTypeConverter.kt
class BooleanTypeConverter {
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? = when (value) {
        true -> 1
        false -> 0
        null -> null
    }

    @TypeConverter
    fun toBoolean(value: Int?): Boolean? = when (value) {
        1 -> true
        0 -> false
        null -> null
    }
}

// Agregar a OptoDatabase:
@TypeConverters(BooleanTypeConverter::class, /* otros converters */)
abstract class OptoDatabase : RoomDatabase() { ... }
```

**O alternativamente — Migración Supabase** (si se prefiere consistencia server-side):
```sql
-- Convertir booleans a INTEGER en Supabase
ALTER TABLE public.evaluaciones 
  ALTER COLUMN auto_presbicia TYPE integer USING CASE WHEN auto_presbicia THEN 1 ELSE 0 END,
  ALTER COLUMN auto_anisometropia TYPE integer USING CASE WHEN auto_anisometropia THEN 1 ELSE 0 END,
  ALTER COLUMN auto_ambliopia TYPE integer USING CASE WHEN auto_ambliopia THEN 1 ELSE 0 END;
-- ... repetir para cada columna boolean en cada tabla
```

---

### 3.3 Extender migration test (v30 → v36+)

**Archivo**: `OptoDatabaseMigrationTest.kt`

**Fix**: Agregar aserciones para migraciones 30 a 37:
```kotlin
@Test
fun `migrate 30 to 37 preserves all data`() {
    // Crear DB en v30 con datos de prueba
    val dbV30 = helper.createDatabase(TEST_DB, 30)
    // Insertar datos representativos
    dbV30.execSQL("INSERT INTO pacientes (id, opticaId, nombreCompleto) VALUES ('p1', 'opt1', 'Juan')")
    dbV30.close()

    // Migrar a v37
    val dbV37 = helper.runMigrationsAndValidate(TEST_DB, 37, true,
        MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
        MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37
    )

    // Verificar que los datos sobrevivieron
    val cursor = dbV37.query("SELECT * FROM pacientes WHERE id = 'p1'")
    assertTrue(cursor.moveToFirst())
    assertEquals("Juan", cursor.getString(cursor.getColumnIndexOrThrow("nombreCompleto")))
    assertEquals("opt1", cursor.getString(cursor.getColumnIndexOrThrow("opticaId")))
    cursor.close()
    dbV37.close()
}
```

---

### 3.4 Sync: `NetworkRetryHelper` — exponential backoff + 5xx/429

**Archivo**: `domain/NetworkRetryHelper.kt`

**Código CORREGIDO**:
```kotlin
class NetworkRetryHelper @Inject constructor() {
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 400L
        private const val MAX_DELAY_MS = 16000L
    }

    suspend fun <T> retryNetwork(
        operationName: String,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRIES) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (!isRetryable(e) || attempt == MAX_RETRIES - 1) throw e
                
                // Exponential backoff: 400ms → 800ms → 1600ms (con jitter)
                val backoffMs = min(BASE_DELAY_MS * (1 shl attempt), MAX_DELAY_MS)
                val jitter = Random.nextLong(0, backoffMs / 4)
                val delayMs = backoffMs + jitter
                
                Log.w(TAG, "$operationName attempt ${attempt + 1}/$MAX_RETRIES failed, retrying in ${delayMs}ms: ${e.message}")
                delay(delayMs)
            }
        }
        throw lastException!!
    }

    private fun isRetryable(e: Exception): Boolean {
        // HTTP errors: retry 429 (rate limit) and 5xx (server errors)
        if (e is RestException) {
            return e.statusCode in 429..599
        }
        // Network errors: timeout, connection reset, DNS failure
        if (e is IOException) return true
        // Supabase-specific transient errors
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("timeout") || msg.contains("timed out") ||
               msg.contains("connection reset") || msg.contains("connect") ||
               msg.contains("network") || msg.contains("socket")
    }
}
```

---

### 3.5 Sync: `PostSaveSyncScheduler` — fix TOCTOU

**Archivo**: `sync/PostSaveSyncScheduler.kt`

**Código ACTUAL** (línea 76):
```kotlin
fun scheduleDebounced(key: String, delayMs: Long = 800L, block: suspend () -> Unit) {
    if (suppressSync) return  // ← chequeado fuera del mutex
    applicationScope.launch {
        scheduleMutex.withLock { ... }
    }
}
```

**Código CORREGIDO**:
```kotlin
fun scheduleDebounced(key: String, delayMs: Long = 800L, block: suspend () -> Unit) {
    applicationScope.launch {
        scheduleMutex.withLock {
            if (suppressSync) return@withLock  // ← chequeado DENTRO del mutex
            // Cancelar job previo con la misma key
            pendingJobs[key]?.cancel()
            val job = launch {
                delay(delayMs)
                if (!suppressSync) block()
            }
            pendingJobs[key] = job
        }
    }
}
```

---

### 3.6 Sync: `acceptAllCloud()` — borrar conflictos DESPUÉS de download exitoso

**Archivo**: `viewmodel/SyncViewModel.kt` líneas 386-396

**Código CORREGIDO**:
```kotlin
fun acceptAllCloud() {
    viewModelScope.launch {
        val opticaId = sessionManager.opticaId.first()
        _syncState.value = SyncState.Syncing
        
        try {
            // 1. Download first
            performFullDownload()
            
            // 2. Only clear conflicts AFTER successful download
            conflictDao.clearConflicts(opticaId)
            syncEntityStateDao.deleteConflictedForOptica(opticaId)
            _conflicts.value = emptyList()
            _conflictCount.value = 0
            
            _syncState.value = SyncState.Idle
            refreshConflicts()
        } catch (e: Exception) {
            _syncState.value = SyncState.Error
            _errorState.value = "Error al resolver conflictos: ${e.message}"
        }
    }
}
```

---

### 3.7 Sync: `resolveKeepMine` — mover bump dentro del mutex

**Archivo**: `viewmodel/SyncViewModel.kt` líneas 187-206

**Código CORREGIDO**:
```kotlin
fun resolveKeepMine(entity: ConflictRecord) {
    viewModelScope.launch {
        try {
            val resolvedOpticaId = entity.opticaId  // Usar el opticaId del registro, no de la sesión
            val syncResult = syncGate.mutex.withLock {
                // Bump DENTRO del lock
                bumpEntityUpdatedAt(entity.entityId, entity.entityType)
                syncForEntityTypeWithResult(resolvedOpticaId, entity.entityType, skipUpload = false)
            }
            if (syncResult !is Resource.Error) {
                conflictDao.resolveConflict(entity.entityId, resolvedOpticaId)
                refreshConflicts()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving conflict keep-mine", e)
        }
    }
}
```

---

### 3.8 UX: Banner offline persistente

**Archivo nuevo**: `ui/components/OfflineBanner.kt`

```kotlin
@Composable
fun OfflineBanner(networkMonitor: NetworkMonitor) {
    val isOnline by networkMonitor.isOnline.collectAsState()
    
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = "Sin conexión")
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sin conexión — datos podrían estar desactualizados",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

**Integrar en MainActivity**:
```kotlin
// MainActivity.kt — dentro del Scaffold
Scaffold(
    topBar = { /* ... */ }
) { padding ->
    Column(Modifier.padding(padding)) {
        OfflineBanner(networkMonitor)  // ← AGREGAR
        NavHost(...) { ... }
    }
}
```

---

### 3.9 UX: Loading indicators en pantallas sin ellos

**Pantallas afectadas**: `PacientesListScreen`, `ReportesScreen`, `CierreCajaScreen`, `GastosScreen`, `ServiciosExtraScreen`

**Patrón de fix** (ejemplo PacientesListScreen):
```kotlin
@Composable
fun PacientesListScreen(viewModel: PacienteViewModel, ...) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorRetryView(message = uiState.error!!, onRetry = { viewModel.refresh() })
            }
            uiState.pacientes.isEmpty() -> {
                EmptyStateView(message = "No hay pacientes registrados")
            }
            else -> {
                LazyColumn { /* lista de pacientes */ }
            }
        }
    }
}
```

---

### 3.10 UX: Accesibilidad — `contentDescription` en TODOS los iconos

**Fix masivo** (aplicar en todos los `Icon` y `IconButton`):
```kotlin
// ===== ANTES (drawer, KPIs, componentes) =====
Icon(Icons.Default.Home, contentDescription = null, ...)
IconButton(onClick = { ... }) { Icon(Icons.Default.Search, null) }

// ===== DESPUÉS =====
Icon(Icons.Default.Home, contentDescription = "Inicio", ...)
IconButton(onClick = { ... }) { 
    Icon(Icons.Default.Search, contentDescription = "Buscar") 
}

// Touch targets: mínimo 48.dp
IconButton(
    onClick = { ... },
    modifier = Modifier.size(48.dp)  // ← garantizar 48dp
) { ... }
```

---

### 3.11 Sanitización de logs — eliminar datos sensibles

**Archivos a modificar**:

**AuthDelegate.kt**:
```kotlin
// ❌ Log.d(TAG, "Recibido deeplink recovery: $deepLink")
// ❌ Log.d(TAG, "Recibido deeplink OAuth: $deepLink")
// ✅ Log.d(TAG, "Deep link received: type=${deepLink.scheme}")
```

**PacienteRepository.kt**:
```kotlin
// ❌ Log.e(TAG, "getPacienteById: id=$id", e)
// ✅ Log.e(TAG, "getPacienteById failed", e)
```

**MembershipDataSource.kt**:
```kotlin
// ❌ Log.e(TAG, "email=$normalizedEmail")
// ✅ Log.e(TAG, "fetchMembersForOptica failed")
```

**OpticaSettingsDataSource.kt**:
```kotlin
// ❌ Log.e(TAG, "updateOpticaFiscalSettings: opticaId=$opticaId", e)
// ✅ Log.e(TAG, "updateOpticaFiscalSettings failed", e)
```

**Regla general**: Los mensajes de `Log.w` y `Log.e` nunca deben incluir IDs, emails, tokens, URLs completas, ni nombres. Solo el contexto de la operación y el exception message sanitizado.

---

### 3.12 Eliminar `assertTrue(true)` stubs

**Fix**: Reemplazar 25 stubs con tests reales o eliminarlos:
```bash
grep -rn "assertTrue(true)" optoapp/src/test/
```
Para cada archivo con stubs:
1. Si el test no verifica nada útil → eliminar el archivo de test
2. Si el test es placeholder para funcionalidad futura → agregar `@Ignore("Not yet implemented")`
3. Si el test debe existir → implementar aserciones reales

---

## Fase 4: Fortalecimiento (MEDIUM/LOW — 2 semanas)

**Objetivo**: Elevar calidad sistémica — precisión financiera, cobertura de tests, eliminación de deuda técnica residual.

---

### 4.1 `Double` → `BigDecimal` para campos monetarios

**Estrategia incremental**:

**Paso 1: Crear TypeConverters** (`data/converter/BigDecimalConverters.kt`):
```kotlin
class BigDecimalConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
```

**Paso 2: Migrar `ResumenDiarioEntity`** (el que más suma acumula):
```kotlin
// ===== ANTES =====
val ventasMontoTotal: Double = 0.0,
val ventasCostoTotal: Double = 0.0,
val cobrosMontoTotal: Double = 0.0,
val saldoPendienteTotal: Double = 0.0,

// ===== DESPUÉS =====
val ventasMontoTotal: BigDecimal = BigDecimal.ZERO,
val ventasCostoTotal: BigDecimal = BigDecimal.ZERO,
val cobrosMontoTotal: BigDecimal = BigDecimal.ZERO,
val saldoPendienteTotal: BigDecimal = BigDecimal.ZERO,
```

**Migración Room**: Agregar `@TypeConverters(BigDecimalConverters::class)` a `OptoDatabase`.

**Paso 3: Actualizar sumas**:
```kotlin
// ===== ANTES =====
val total = rows.sumOf { it.ventasMontoTotal }

// ===== DESPUÉS =====
val total = rows.map { it.ventasMontoTotal }.fold(BigDecimal.ZERO) { acc, v -> acc + v }
```

**Paso 4: Repetir para `GastoOperativoEntity`, `CostoProductoEntity`, `CostoBiseladoEntity`, `ConfiguracionFinancieraEntity`, `Pago`, `DispensacionOptica`, `ServicioExtra`**.

---

### 4.2 Eliminar `runBlocking` restantes (5+ sitios)

**Patrón de fix para TODOS**:
```kotlin
// ===== ANTES =====
kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    repository.runInTransaction {
        kotlinx.coroutines.runBlocking {
            repository.updateServicio(servicio)
            repository.insertPago(pago)
        }
    }
}

// ===== DESPUÉS =====
repository.withTransaction {
    repository.updateServicio(servicio)
    repository.insertPago(pago)
}
```

**Archivos a modificar**:
1. `InformacionFinancieraViewModel.kt` — save()
2. `ServiciosViewModel.kt` — saveServicio()
3. `DispensacionViewModel.kt` — saveDispensacion()
4. `CostosYGastosViewModel.kt` — saveGasto()
5. `RegaloDispensacionViewModel.kt` — saveRegaloAndDeductStock()

**Pre-requisito**: Agregar `withTransaction` a los repositorios que no lo tengan (delegando a `OptoRepository.withTransaction` o directamente a `database.withTransaction`).

---

### 4.3 Centralizar `fmt()` duplicado

**Archivo**: `util/FormatUtils.kt` — agregar extension function:
```kotlin
fun Double.formatAsCurrency(): String = "S/ %.2f".format(this)

fun BigDecimal.formatAsCurrency(): String = "S/ %.2f".format(this)

fun Int.formatAsInteger(): String = "%,d".format(this)
```

**Reemplazar en 5+ archivos**:
```bash
grep -rn "S/ %.2f".format" optoapp/src/main/
grep -rn '"%,d".format' optoapp/src/main/
```
Migrar cada ocurrencia a `value.formatAsCurrency()` o `count.formatAsInteger()`.

---

### 4.4 Extender cobertura de tests (8.9% → 20%)

**Prioridad**:
1. **DAOs con queries `getById` nuevas** (15 DAOs) — test de filtro `opticaId`:
```kotlin
@Test
fun `query respects opticaId filter`() = runTest {
    val dao = db.pacienteDao()
    dao.insertPaciente(Paciente(id = "p1", opticaId = "A", nombreCompleto = "Juan"))
    dao.insertPaciente(Paciente(id = "p2", opticaId = "B", nombreCompleto = "Maria"))
    
    assertNotNull(dao.getPacienteById("p1", "A"))
    assertNull(dao.getPacienteById("p1", "B"))
}
```

2. **`SyncFinanzasUseCase`** — test de upload de costos biselado
3. **`NetworkRetryHelper`** — test de exponential backoff
4. **`PinDelegate`** — test de persistencia de brute-force

---

### 4.5 Agregar `forbidOnly` en CI

**Archivo**: `.github/workflows/android-ci.yml`
```yaml
- name: Run tests with forbidOnly
  run: ./gradlew :optoapp:testDebugUnitTest -PforbidOnly=true
```

**En `build.gradle.kts`**:
```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            if (project.hasProperty("forbidOnly")) {
                all { test ->
                    test.systemProperty("junit.jupiter.execution.only.enabled", "false")
                }
            }
        }
    }
}
```

---

### 4.6 Cleanup RPCs muertas en Supabase

**Migración SQL**:
```sql
-- Drop RPCs confirmed as unused (grep audit 2026-07-15)
-- NOTA: rpc_count_pendientes YA fue droppeada por Fix 1.3 (migración 20260714000002)
DROP FUNCTION IF EXISTS public.rpc_pacientes_con_saldo(TEXT);
DROP FUNCTION IF EXISTS public.rpc_pacientes_con_entrega_pendiente(TEXT);
DROP FUNCTION IF EXISTS public.rpc_adjust_montura_stock(TEXT, TEXT, INT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.suggest_next_ho(UUID);
DROP FUNCTION IF EXISTS public.sync_snapshot(UUID);
DROP FUNCTION IF EXISTS public.check_rate_limit(TEXT, INT, INT);
DROP FUNCTION IF EXISTS public.paciente_eliminaciones_restantes_hoy(UUID);

-- Evaluar si mantener (potencialmente usado en futuro):
-- rpc_cierre_caja_resumen — keep, marked Active
```

---

### 4.7 Widget: fix key mismatch `"optica_id"` → `"saas_optica_id"`

**Archivo**: `widget/MiNegocioWidgetWorker.kt` línea 86
```kotlin
// ===== ANTES =====
prefs.getString("optica_id", null) ?: ""

// ===== DESPUÉS =====
prefs.getString("saas_optica_id", null) ?: ""
```

**Archivo**: `res/xml/widget_mi_negocio_info.xml`
```xml
<!-- Agregar protección de lockscreen -->
app:widgetCategory="home_screen"
<!-- Si se quiere mostrar en lockscreen, agregar keyguard y redactar datos -->
```

---

### 4.8 Certificate pinning para `*.supabase.co`

**Archivo**: `di/SupabaseModule.kt`

**Paso 1**: Obtener fingerprints:
```bash
openssl s_client -connect tu-proyecto.supabase.co:443 -servername tu-proyecto.supabase.co | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
```

**Paso 2**: Agregar al OkHttp client:
```kotlin
@Provides @Singleton
fun provideSupabaseClient(...): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        httpEngine = {
            OkHttpClient.Builder()
                .certificatePinner(
                    CertificatePinner.Builder()
                        .add("*.supabase.co", "sha256/PEJx17Pek25Z2LhTGYEwBRCNegsTqGdekk6B8eUvFk4=")
                        .add("tu-proyecto.supabase.co", "sha256/...")
                        .build()
                )
                .build()
        }
    }
}
```

---

### 4.9 Autorización en ViewModels — defense-in-depth

**Nuevo helper** (`domain/auth/AuthorizationGuard.kt`):
```kotlin
object AuthorizationGuard {
    fun requireRole(actualRole: String, requiredRoles: Set<String>, operation: String) {
        val normalized = actualRole.trim().lowercase()
        require(normalized in requiredRoles) {
            "Unauthorized: role '$normalized' cannot perform '$operation'. Required: ${requiredRoles.joinToString()}"
        }
    }
}

// Uso en ViewModels:
class MonturasViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    // ...
) : ViewModel() {
    init {
        viewModelScope.launch {
            val role = sessionManager.opticaRol.first()
            AuthorizationGuard.requireRole(
                actualRole = role,
                requiredRoles = AppRoles.ROLES_EDIT_INVENTORY,
                operation = "edit inventory"
            )
        }
    }
}
```

**Aplicar en**: `MonturasViewModel`, `PacienteViewModel`, `EvaluacionViewModel`, `DispensacionViewModel`, `ServiciosViewModel`, `ProveedoresViewModel`, `OrdenesCompraViewModel`.

---

### 4.10 `proximaFechaControl` — completar el mapeo

**Archivos**: `EvaluacionUiState.kt`, `EvaluacionMapping.kt`

```kotlin
// EvaluacionUiState.kt — agregar campo
data class EvaluacionUiState(
    // ... existing fields ...
    val proximaFechaControl: String = "",
)

// EvaluacionMapping.kt — toEvaluacionClinica()
fun EvaluacionUiState.toEvaluacionClinica(id: String, pacienteId: String, opticaId: String, dipParsed: String?): EvaluacionClinica {
    return EvaluacionClinica(
        // ... existing mappings ...
        proximaFechaControl = this.proximaFechaControl,  // ← AGREGAR
    )
}

// EvaluacionMapping.kt — toEvaluacionUiState()
fun EvaluacionClinica.toEvaluacionUiState(): EvaluacionUiState {
    return EvaluacionUiState(
        // ... existing mappings ...
        proximaFechaControl = this.proximaFechaControl,  // ← AGREGAR
    )
}
```

---

### 4.11 `AutoPresbicia`/`AutoAnisometropia` — fix de mapeo

**Archivo**: `EvaluacionMapping.kt` línea 70-76
```kotlin
// Agregar los dos campos faltantes:
autoPresbicia = s.autoPresbicia,           // ← AGREGAR
autoAnisometropia = s.autoAnisometropia,   // ← AGREGAR
autoAmbliopia = s.autoAmbliopia,           // ya existe
```

---

### 4.12 `parseSnellenToLogMar` — fix para notación 6m

**Archivo**: `viewmodel/diagnostico/DiagnosticoCalculator.kt`
```kotlin
fun parseSnellenToLogMar(snellen: String): Double? {
    val regex = Regex("""(\d+)\s*/\s*(\d+)""")
    val m = regex.find(snellen.trim()) ?: return null
    val numerator = m.groupValues[1].toDoubleOrNull() ?: return null
    val denominator = m.groupValues[2].toDoubleOrNull() ?: return null
    require(denominator > 0) { "Denominator must be positive" }
    return -log10(numerator / denominator)  // usa el numerador capturado, no 20.0
}
```

---

### 4.13 `SynStateTracker` transaccional

**Archivo**: `data/SyncStateTracker.kt`

**Fix**: Envolver entity write + markSynced en transacción:
```kotlin
suspend fun markSyncedAtomic(opticaId: String, entityType: String, entityId: String, block: suspend () -> Unit) {
    database.withTransaction {
        block()  // insert/update de la entidad
        markSynced(opticaId, entityType, entityId)  // marca de sync
    }
}
```

---

### 4.14 Widget: `CoroutineScope` con lifecycle

**Archivo**: `widget/MiNegocioWidgetProvider.kt`
```kotlin
class MiNegocioWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            scope.launch {
                try {
                    val opticaId = MiNegocioWidgetWorker.readOpticaId(context)
                    if (opticaId.isBlank()) return@launch
                    val data = fetchWidgetData(context, opticaId)
                    updateWidget(context, appWidgetManager, id, data)
                } catch (e: Exception) {
                    Log.e(TAG, "Widget update failed", e)
                }
            }
        }
    }

    override fun onDisabled(context: Context) {
        scope.cancel()  // limpiar al deshabilitar
    }
}
```

---

### 4.15 `@Update` en DAOs — verificar con `opticaId`

**Fix**: Reemplazar `@Update` con `@Query` que incluya `opticaId`:
```kotlin
// ===== ANTES (PacienteDao) =====
@Update
suspend fun updatePaciente(paciente: Paciente)

// ===== DESPUÉS =====
@Query("""
    UPDATE pacientes SET 
        nombreCompleto = :nombreCompleto,
        telefono = :telefono,
        email = :email,
        dni = :dni,
        direccion = :direccion,
        fechaNacimiento = :fechaNacimiento,
        edad = :edad,
        genero = :genero,
        historiaOptometrica = :historiaOptometrica,
        updatedAt = :updatedAt
    WHERE id = :id AND opticaId = :opticaId
""")
suspend fun updatePaciente(
    id: String, opticaId: String, nombreCompleto: String, telefono: String?,
    email: String?, dni: String?, direccion: String?, fechaNacimiento: String?,
    edad: Int?, genero: String?, historiaOptometrica: String?, updatedAt: String
): Int
```

**Aplicar mismo patrón a**: `MonturaDao`, `ProveedorDao`, `EvaluacionDao`, `DispensacionDao` (todos los que usan `@Update`).

---

## 📋 Checklist Maestro de Despliegue

### Fase 1 (BLOCKERs — 13 fixes)
- [x] 1.1 Service key solo en debug (pendiente)
- [x] 1.2 RPC `assign_optica_role_by_email` re-granted ✅ (db push pendiente)
- [x] 1.3 `rpc_saldo_pendiente` droppeada ✅ (ya existía migración 20260714000002)
- [ ] 1.4 `recalcular_resumen_diario` SECURITY DEFINER + anulado filter (absorbe Fix 2.21)
- [ ] 1.5 RLS `regalos_dispensacion` restrictivo
- [ ] 1.6 `create_optica` DO NOTHING
- [ ] 1.7 `trg_pagos` UPDATE atómico
- [ ] 1.8 `opticas_update` RLS admin/gerente
- [ ] 1.9 Notificaciones con `VISIBILITY_PRIVATE`
- [ ] 1.10 PIN brute-force persistido
- [ ] 1.11 Tokens fuera de logs
- [ ] 1.12 Role default `""` (no `"admin"`)
- [ ] 1.13 Backup SQLite con `backup()` API

### Fase 2 (CRITICALs — 28 fixes)
- [ ] 2.1 15 DAOs `getById` con `opticaId`
- [ ] 2.2 3 cost lookups con `opticaId`
- [ ] 2.3 Métodos deprecated eliminados
- [ ] 2.4 DELETE queries con `opticaId`
- [ ] 2.5 `ConflictRecord` PK compuesta
- [ ] 2.6 `ResumenDiarioEntity` unique constraint
- [ ] 2.7 `cantidad = abs(delta)` en stock helper
- [ ] 2.8 Merge atómico en `SyncFinanzasMerge`
- [ ] 2.9 `save()` con error handling
- [ ] 2.10 Doble-tap guard en save
- [ ] 2.11 `removeRegalo` borra solo uno
- [ ] 2.12 Billing con Edge Function
- [ ] 2.13 `auth.uid()` en RPCs financieros → consolidar con 2.20+2.29 en Fase 2
- [ ] 2.14 `FREE_MAX_PACIENTES` reales
- [ ] 2.15 `CostosBiselado` upload
- [ ] 2.16 `uploadServicios` merge
- [ ] 2.17 `SyncFinanzasUploaders` eliminado
- [ ] 2.18 Fetch reconciliación aborta en error
- [ ] 2.19 Download aislado por entidad
- [ ] 2.20 `proyeccion_caja` con COALESCE → consolidar con 2.13+2.29 en Fase 2
- [ ] 2.21 `recalcular_resumen` excluye anuladas → ABSORBIDO en Fix 1.4
- [ ] 2.22 `costo = 0.0` → costo real
- [ ] 2.23 Regalos en modelo financiero
- [ ] 2.24 PDF incluye anulaciones
- [ ] 2.25 `crearReclamo` valida refund
- [ ] 2.26 Biselado lookup dinámico
- [ ] 2.27 `autoPresbicia`/`autoAnisometropia`
- [ ] 2.28 `parseSnellenToLogMar` 6m

### Fase 3 (HIGH — 12 fixes)
- [ ] 3.1 Schema drift UUID→TEXT (7 tablas)
- [ ] 3.2 Boolean columns alineados
- [ ] 3.3 Migration test extendido
- [ ] 3.4 `NetworkRetryHelper` exponential
- [ ] 3.5 `PostSaveSyncScheduler` TOCTOU
- [ ] 3.6 `acceptAllCloud` order fix
- [ ] 3.7 `resolveKeepMine` mutex
- [ ] 3.8 Banner offline
- [ ] 3.9 Loading indicators
- [ ] 3.10 `contentDescription` en iconos
- [ ] 3.11 Logs sanitizados
- [ ] 3.12 Stubs `assertTrue(true)` eliminados

### Fase 4 (LOW — 5 fixes)
- [ ] 4.1 `BigDecimal` para dinero
- [ ] 4.2 `runBlocking` eliminado (5 sitios)
- [ ] 4.3 `fmt()` centralizado
- [ ] 4.4 Cobertura tests 20%
- [ ] 4.5 `forbidOnly` en CI

---

---

## 6. Recomendaciones Arquitectónicas

### 6.1 God Objects — plan de extracción

| God Object | Responsabilidades | Extraer a |
|------------|-------------------|-----------|
| `SyncViewModel` (850 líneas) | 8 responsabilidades | `ConflictResolutionCoordinator`, `SyncOrchestrator`, `BumpEntityStrategy` |
| `OptoRepository` (578 líneas) | 70+ métodos passthrough | Eliminar — los callers usen DAOs directamente o repos específicos |
| `DispensacionViewModel` (471 líneas) | Save + stock + costos + regalos | `DispensacionStockService`, `DispensacionCostService` |
| `DispensacionRepository` (255 líneas) | 4 entity types | Separar en `PagoRepository`, `ServicioExtraRepository`, `DispensacionItemRepository` |

### 6.2 Clean Architecture — violaciones a corregir

- **`android.util.Log` en `domain/`**: Reemplazar con `SyncLogger` (ya existe). 19 archivos.
- **ViewModels inyectando DAOs**: Interponer Use Cases o repositorios. `SyncViewModel` → `ConflictRepository`, `SyncReportRepository`.
- **`fmt()` duplicado**: Centralizar en `FormatUtils.kt`.

### 6.3 Multi-tenancy — enforcement automático

Crear un **lint rule** que detecte queries Room sin `opticaId`:
```kotlin
// Regla: todo @Query que acceda a una entidad con campo opticaId
// DEBE incluir "opticaId = :opticaId" o "optica_id = :opticaId"
```
Esto previene regresiones.

### 6.4 Testing — umbrales y calidad

- **Cobertura mínima**: Subir de 8.9% → 20% (dominio + data layer)
- **Migration test**: Extender a todas las migraciones
- **`forbidOnly`**: Activar en CI para prevenir regresiones

---

## 7. Preguntas y Decisiones Pendientes

### Preguntas para el equipo

1. **¿Plan de monetización real?** Los límites actuales (`Int.MAX_VALUE`) son placeholders. Necesitamos definir:
   - FREE: ¿50 pacientes? ¿20 dispensaciones/mes?
   - PRO: ¿ilimitado? ¿precio?
   - ¿La validación de plan va en Edge Function o en RPC?

2. **¿Migración `20260713034249` (completar diferidos financieros)?** Reintroduce dependencia de `ventas` (tabla droppeada). ¿Se cancela esta migración o se reescribe con `UNION ALL`?

3. **¿`ventas` — dropear definitivamente o mantener?** La migración `20260710064319` la droppea, pero `20260713034249` la referencia. Definir si el modelo `UNION ALL` es el camino definitivo.

4. **¿`rpc_cierre_caja_resumen` y `rpc_deudores`?** Están marcados como "Active" pero no se llaman desde Android. ¿Se planea usarlos? Si no, droppear.

5. **¿Certificate pinning ahora o después?** Requiere fingerprints de los certificados de Supabase. Si cambian, la app se rompe. ¿Vale la pena el riesgo vs el beneficio de seguridad?

### Decisiones que necesito confirmar

1. **Orden de las Fases 1 y 2**: ¿Priorizamos backend (Supabase) primero porque protege a todos los clientes, o Android primero porque es donde están los BLOCKERs más visibles?

2. **¿Eliminar métodos deprecated de una o gradualmente?** Eliminarlos todos de una puede romper builds si hay callers no detectados. ¿Hacemos PR por DAO?

3. **¿`Double` → `BigDecimal` ahora o postergado?** Es un cambio grande que toca muchas entidades. ¿Lo metemos en Fase 4 o lo dejamos para un proyecto dedicado?

---

*Diagnóstico integral generado el 2026-07-15. Basado exclusivamente en Judgment Day — 34 jueces R1 Security en 17 dominios, ~280 archivos, ~294 hallazgos.*
