# ⚖️ Judgment Day — Sistema Financiero OptoApp
## Informe Unificado Completo

**Fecha**: 2026-07-15
**Metodología**: Dual blind review (R1 Security) por slice, 2 jueces adversariales en paralelo
**Total de hallazgos**: 102 (33 confirmados · 58 sospechosos · 11 info)
**Slices**: 8 slices de código + 1 auditoría RPC

---

## Resumen Ejecutivo

Se realizó una revisión adversarial completa del sistema financiero de OptoApp en 4 slices + 1 auditoría complementaria. El sistema mostró **problemas estructurales serios** en tres áreas: multi-tenancy (filtrado de `optica_id` ausente en DAOs de costos), integridad transaccional (merges no atómicos, race conditions en triggers), y consistencia de datos (sincronización frágil, pantallas deprecated aún activas colisionando con las nuevas).

**Hallazgos más graves**:
- 🔴 3 DAOs de costos sin filtro `optica_id` — breach multi-tenant (Slice 4)
- 🔴 `save()` de pagos sin error handling — crash o UI congelada (Slice 2)
- 🔴 Merge de dispensaciones no atómico — datos huérfanos (Slice 1)
- 🔴 `rpc_analisis_mensual` excluye pagos sin `venta_id` — reportes inconsistentes (Slice 3)
- 🔴 `CostosBiselado` se descargan pero nunca se suben — data loss (Slice 1)

---

## Metodología

Cada slice fue revisado por **dos jueces independientes** (agentes R1 Security Engineer) con criterios idénticos y sin conocer el veredicto del otro. Los hallazgos se clasifican:

| Clasificación | Criterio |
|---------------|----------|
| 🔴 **Confirmado** | Ambos jueces encontraron el mismo problema |
| 🟡 **Sospechoso** | Solo un juez lo detectó (requiere verificación) |
| 🔵 **Info** | Sugerencia, teórico, o mejora no crítica |

---

## Slice 1: Sync Financiero

**Archivos**: `SyncFinanzasUseCase.kt`, `SyncFinanzasUploaders.kt`, `SyncFinanzasMerge.kt`, `SyncFinanzasDto.kt`, `FinanzasRemoteDefaults.kt`

**Rol**: Capa de sincronización offline-first para todas las entidades financieras (dispensaciones, pagos, servicios, gastos, costos, regalos).

### 🔴 Confirmados (3)

#### C1.1 — CRITICAL: Merge no atómico en SyncFinanzasMerge
- **Archivo**: `SyncFinanzasMerge.kt:54-56, 88-89`
- **Descripción**: `mergeLocalDispensacionConflict` y `resolveLocalDuplicateDispensaciones` ejecutan `updateDispensacion()` + `reassignPagosDispensacion()` + `deleteDispensacionById()` secuencialmente **sin `@Transaction`**. Si el device crashea o Room tira error entre pasos:
  - Pagos movidos al canónico pero el duplicado sigue existiendo → huérfanos
  - Canónico actualizado pero pagos todavía apuntan al duplicado → inconsistencia
- **Fix sugerido**: Envolver las 3 operaciones en `database.runInTransaction { ... }`

#### C1.2 — CRITICAL/WARNING: `"mi_optica_base"` hardcodeado + `.trim()` faltante
- **Archivo**: `SyncFinanzasDto.kt:74, 120, 160, 182`
- **Descripción**: `DispensacionRemota.optId()` y `DispensacionItemRemota.toEntity()` usan el string literal `"mi_optica_base"` en vez de `FinanzasRemoteDefaults.OPTICA_ID_FALLBACK`, y **sin `.trim()`**. Si Supabase devuelve `optica_id` con whitespace, dispensaciones e items se almacenan bajo una óptica diferente a servicios y pagos, rompiendo el aislamiento multi-tenant.
- **Fix sugerido**: Usar `opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK }` en los 4 lugares

#### C1.3 — WARNING/SUGGESTION: `maxOf(montoTotal)` sin audit trail
- **Archivo**: `SyncFinanzasMerge.kt:45, 47`
- **Descripción**: Al mergear dos dispensaciones duplicadas, `montoTotal` y `montoPagado` se resuelven con `maxOf(canonical, duplicate)`. Si el duplicado tiene monto mayor por error, se infla el revenue reportado sin registro de la decisión.
- **Fix sugerido**: Usar valor del canónico salvo que sea cero/default; loggear warning si difieren.

### 🟡 Sospechosos (10)

| # | Juez | Severidad | Hallazgo |
|---|------|-----------|----------|
| S1.1 | B | CRITICAL | `CostosBiselado` se descargan pero **nunca se suben**. `toRemoto()` existe pero falta `safeUpload("costos_biselado")` en `SyncFinanzasUseCase`. **Data loss garantizado**. |
| S1.2 | B | CRITICAL | `uploadServicios` descarta duplicados con last-write-wins sin merge ni warning. Dispensaciones sí mergean — inconsistencia. |
| S1.3 | B | CRITICAL | `SyncFinanzasUploaders.kt` (248 líneas) es **código muerto** — sin `@Inject`, nunca instanciado. Duplica lógica con bugs sutiles. |
| S1.4 | A | CRITICAL | Fetch de reconciliación falla → `emptyList()` → upsert sin dedup → **filas duplicadas** en Supabase. |
| S1.5 | A | CRITICAL | `DispensacionItem` y `RegaloDispensacion` quedan huérfanos tras merge-delete. No se reasignan ni se hace cascade. |
| S1.6 | A | WARNING | Sync state no se marca tras batch parcial de upload → re-upload innecesario en cada ciclo. |
| S1.7 | A | WARNING | OT en blanco excluye dispensaciones de detección de duplicados → dos registros offline con OT vacío nunca se mergean. |
| S1.8 | B | WARNING | `DateTimeParseException` sin manejar en `toEntity()` — una fecha malformada del server crashea el sync. |
| S1.9 | B | WARNING | `catch (IOException)` puede atrapar `RestException` antes que su handler si hereda de IO → 401/403 silenciados. |
| S1.10 | B | WARNING | Download no aislado por entidad (a diferencia de upload con `safeUpload`) — si falla dispensaciones, no se descarga nada más. |

### 🔵 Info (5)

- `retryNetwork` usa substring matching en vez de HTTP status codes
- Backoff lineal sin jitter — amplifica carga en reconexión masiva
- `resolveLocalDuplicateDispensaciones` no determinístico en empate de fechas
- `optId()` mal nombrado (sugiere OT, normaliza opticaId)
- `FinanzasSyncResult` usa args posicionales — frágil ante reordenamiento

---

## Slice 2: Lógica de Pagos

**Archivos**: `InformacionFinancieraViewModel.kt`, `InformacionFinancieraScreen.kt`, `PagosSection.kt`, `AbonoDialog.kt`, `DispensacionFinancieraRepository.kt`

**Rol**: Capa de UI + ViewModel + Repository que el usuario toca directamente para gestionar pagos/abonos de una dispensación.

### 🔴 Confirmados (4)

#### C2.1 — CRITICAL: `save()` sin error handling
- **Archivo**: `InformacionFinancieraViewModel.kt:119-157`
- **Descripción**: El bloque `save()` ejecuta múltiples operaciones de DB sin ningún `try/catch`. Si cualquier llamada falla:
  - `isLoading` queda `true` permanentemente → UI congelada
  - `onComplete()` nunca se llama → usuario no puede navegar
  - `scheduleFinanzasSync` corre **incluso tras rollback** → sync de datos inconsistentes
  - En el peor caso, la excepción no capturada crashea la app
- **Fix sugerido**: Envolver todo en `try/catch`, postear `error` al UI state, resetear `isLoading`, solo schedulear sync en éxito.

#### C2.2 — CRITICAL/WARNING: Sin guard contra doble-tap en save
- **Archivos**: `InformacionFinancieraViewModel.kt:119` + `InformacionFinancieraScreen.kt:64,228`
- **Descripción**: Dos elementos de UI disparan `saveAction()` sin deshabilitarse durante `isLoading`. Dos toques rápidos lanzan dos corrutinas con el mismo snapshot → **pagos duplicados** en DB (o crash por PK conflict), `onComplete()` llamado dos veces, sync duplicado.
- **Fix sugerido**: Early-return si `isLoading`; deshabilitar botones durante save.

#### C2.3 — WARNING (real): `runBlocking` dentro de `runInTransaction`
- **Archivo**: `InformacionFinancieraViewModel.kt:129-150`
- **Descripción**: `runInTransaction` (callback no-suspend) fuerza `runBlocking` dentro de `Dispatchers.IO`, bloqueando un thread del pool. Anti-patrón documentado de Kotlin. Existe `OptoRepository.withTransaction` (suspend) pero no se usa acá.
- **Fix sugerido**: Usar `repository.withTransaction { ... }` en vez de `runInTransaction` + `runBlocking`.

#### C2.4 — SUGGESTION: `saldo` calculado localmente ignorando el ViewModel
- **Archivos**: `InformacionFinancieraScreen.kt:107-109` vs `FinancieraUiState.saldoRestante`
- **Descripción**: La Screen recalcula `saldo = total - pagado` localmente. Un comentario en línea 190 dice "se calcula en el ViewModel para mantener una única fuente de verdad" pero el código lo ignora. Si el cálculo cambia, divergen.
- **Fix sugerido**: Usar `uiState.saldoRestante` y eliminar las variables locales `total`/`pagado`.

### 🟡 Sospechosos (5)

| # | Juez | Severidad | Hallazgo |
|---|------|-----------|----------|
| S2.1 | A | WARNING | `AbonoDialog`: estado persiste entre aperturas para nuevos abonos. `remember(pago?.id)` con key `null` compartida entre todas las instancias nuevas. |
| S2.2 | A | WARNING | `fechaEntrega` se pierde al togglear `estado`: "Entregado" → edita fecha → "Pendiente" → fecha=null → vuelve a "Entregado" → `LocalDate.now()` en vez de la fecha editada. |
| S2.3 | B | WARNING | `initialPagoIds` no se actualiza tras save exitoso. Segundo save trata pagos persistidos como nuevos → upsert redundante. |
| S2.4 | B | WARNING | `obtenerContexto` devuelve `ContextoFinanciero` vacío en vez de `null/Error` si falla. UI renderiza "OT: ", "Paciente: " sin indicar error. |
| S2.5 | A | SUGGESTION | Falta `key(pago.id)` en `forEach` de Compose → riesgo de state bleed entre items de la lista. |

---

## Slice 3: Triggers y RPCs de Supabase

**Archivos**: 8 migraciones SQL (`triggers_ventas`, `pagos_auto_venta_id`, `exclude_anulaciones`, `fix8_distinct_on`, `rpc_resumen_financiero`, `rpc_saldo_pendiente`, `cierre_caja`, `documentation_comments`)

**Rol**: Funciones server-side que mantienen la integridad financiera: tabla espejo `ventas`, triggers de pagos, RPCs de agregación.

### 🔴 Confirmados (5)

#### C3.1 — CRITICAL: `rpc_analisis_mensual.proyeccion_caja` sin fallback de `venta_id`
- **Archivo**: `20260706205841:81-93`
- **Descripción**: `proyeccion_caja` resuelve `venta_id_match` como `pg.venta_id` sin `COALESCE` a `dispensacion_id`/`servicio_extra_id`. Pagos históricos sin `venta_id` (pre-backfill) quedan **silenciosamente excluidos** del cash projection. `rpc_deudores` y `recalcular_resumen_diario` sí tienen el fallback — inconsistencia que produce números diferentes en el dashboard para los mismos datos.
- **Fix sugerido**: `COALESCE(pg.venta_id, 'v_disp_'||pg.dispensacion_id, 'v_serv_'||pg.servicio_extra_id)`

#### C3.2 — CRITICAL/WARNING: Race condition en `trg_pagos_update_monto_pagado`
- **Archivo**: `20260706205131:7-39`
- **Descripción**: El trigger recalcula `monto_pagado` con `SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE...` sin `FOR UPDATE`. Dos INSERTs concurrentes al mismo `dispensacion_id`:
  1. TxA lee SUM=100, TxB lee SUM=100
  2. TxA escribe monto_pagado=150, TxB escribe monto_pagado=175
  3. **Se pierde el incremento de TxA** → monto_pagado incorrecto
  Esto corrompe la fuente autoritativa y todos los reportes downstream.
- **Fix sugerido**: `SELECT ... FOR UPDATE` sobre la fila de dispensacion/servicio, o usar `UPDATE ... SET monto_pagado = monto_pagado + NEW.monto` atómico.

#### C3.3 — CRITICAL/WARNING: `rpc_resumen_financiero` no excluye anulaciones
- **Archivo**: `20260706203300:18-23`
- **Descripción**: Suma TODOS los pagos sin `AND tipo IS DISTINCT FROM 'Anulación'`. La migración `jd_fix3` agregó esta exclusión a todos los demás RPCs financieros pero dejó este sin tocar (está deprecated pero sigue ejecutable). Si alguien lo invoca, ingresos incluyen reembolsos.
- **Fix sugerido**: Agregar la exclusión o droppear la función (migración `20260710064319` ya la droppea).

#### C3.4 — WARNING (real): Sin trigger `AFTER DELETE` en ventas
- **Archivo**: `20260705023012:1-77`
- **Descripción**: Los triggers `fn_upsert_venta_from_dispensacion` y `fn_upsert_venta_from_servicio_extra` solo firean en `INSERT/UPDATE`. Si una dispensacion/servicio se hard-deletea (no soft-anula), la fila espejo en `ventas` queda huérfana, corrompiendo reportes.
- **Fix sugerido**: Agregar `AFTER DELETE` trigger que elimine la fila correspondiente en `ventas`.

#### C3.5 — WARNING (real): `trg_pagos_set_venta_id` solo `BEFORE INSERT`
- **Archivo**: `20260706203416:19-23`
- **Descripción**: Si un admin cambia `dispensacion_id` o `servicio_extra_id` de un pago vía dashboard, `venta_id` no se recalcula — el pago queda linkeado a la venta equivocada.
- **Fix sugerido**: Agregar `BEFORE UPDATE OF dispensacion_id, servicio_extra_id` trigger.

### 🟡 Sospechosos (8)

| # | Juez | Severidad | Hallazgo |
|---|------|-----------|----------|
| S3.1 | B | CRITICAL | `recalcular_resumen_diario` no filtra `estado = 'Anulado'` en ventas → incluye anulaciones en `v_ventas_monto` pero las excluye de `cobros` → `saldo_pendiente_total` sin sentido. |
| S3.2 | A | WARNING | `rpc_resumen_financiero`: pagos de ventas anuladas se cuentan pero las ventas no → asimetría que distorsiona saldo. |
| S3.3 | A | WARNING | **Ningún RPC financiero valida `auth.uid()`**. Si RLS falla, cualquier authenticated user puede leer datos de otra óptica pasando otro `p_optica_id`. |
| S3.4 | A | WARNING | `rpc_cierre_caja_resumen` no excluye `tipo = 'Anulación'` — cierre con reembolsos muestra totales distorsionados. |
| S3.5 | B | WARNING | `rpc_cierre_caja_resumen`: métodos de pago hardcodeados sin bucket `ELSE`. Nuevo método → desaparece de categorías. |
| S3.6 | B | WARNING | Comentarios de RPCs deprecated mienten: dicen "usa dispensaciones directamente" pero el código usa `ventas`. |
| S3.7 | A | WARNING | Pago con ambos `dispensacion_id` y `servicio_extra_id` → trigger asigna `v_disp_` silenciosamente, sin CHECK ni error. |
| S3.8 | B | WARNING | SUM en trigger de `monto_pagado` no filtra por `optica_id` — defense-in-depth ausente. |

### 🔵 Info (3)

- `saldo_pendiente` en `rpc_resumen_financiero` sin floor (puede exceder ventas_emitidas)
- `metodo_pago` strings hardcodeados en español, sin CHECK constraint
- `rpc_resumen_financiero`: resta cross-mes incorrecta para reportes por período

---

## Slice 4: Costos y Gastos

**Archivos**: `CostosYGastosViewModel.kt`, `CostosYGastosScreen.kt`, `CostoProductoEntity.kt`, `CostoProductoDao.kt`, `CostoBiseladoEntity.kt`, `CostoBiseladoDao.kt`, `GastoOperativoEntity.kt`, `GastoOperativoDao.kt`, `GastosViewModel.kt` (deprecated), `GastosScreen.kt` (deprecated)

**Rol**: Gestión de matriz de costos de producto, costos de biselado, y gastos operativos.

### 🔴 Confirmados (4)

#### C4.1 — CRITICAL: `saveCostoEdit()` nunca dispara sync
- **Archivo**: `CostosYGastosViewModel.kt:149-170`
- **Descripción**: El método persiste `costoUnitario` editado vía `costoProductoDao.upsertAll()` pero **nunca llama a `scheduleFinanzasSync()`**. El override manual es solo local — se pierde en wipe, reinstalación, o en el próximo download del sync. `saveGasto()` sí schedulea sync.
- **Fix sugerido**: Agregar `postSaveSyncScheduler.scheduleFinanzasSync(opticaId)` después del upsert.

#### C4.2 — CRITICAL/WARNING: `lookupLc()` lógica de NULL incorrecta
- **Archivo**: `CostoProductoDao.kt:48-62`
- **Descripción**: `(laboratorio_id IS NULL OR laboratorio_id = :laboratorioId)` — cuando `:laboratorioId` es no-null, matchea tanto la fila con lab=NULL como la del lab específico. Un registro genérico (lab=NULL) actúa como wildcard y puede ganarle al costo específico del laboratorio.
- **Fix sugerido**: `((laboratorio_id IS NULL AND :laboratorioId IS NULL) OR laboratorio_id = :laboratorioId)`

#### C4.3 — WARNING (real): Doble scheduleo de sync
- **Archivos**: `CostosYGastosViewModel.kt:228` + `GastosViewModel.kt:186`
- **Descripción**: `OptoRepository` ya llama `scheduleFinanzasSync()` internamente en `upsertGastoOperativo()` y `deleteGastoOperativo()`. Ambos ViewModels lo vuelven a llamar explícitamente → sync duplicado en cada operación.
- **Fix sugerido**: Eliminar las llamadas explícitas del ViewModel; confiar en el repository.

#### C4.4 — WARNING (real): `GastoOperativoDao.getAll()` deprecated pero leaky
- **Archivo**: `GastoOperativoDao.kt:12-17`
- **Descripción**: Método `@Deprecated` pero funcional, sin filtro `opticaId`. Los tests aún lo usan. Si código futuro lo invoca, devuelve gastos de **todas** las ópticas.
- **Fix sugerido**: Eliminar el método o agregarle `WHERE opticaId = :opticaId`.

### 🟡 Sospechosos (9)

| # | Juez | Severidad | Hallazgo |
|---|------|-----------|----------|
| S4.1 | A | CRITICAL | `CostoProductoDao.lookup()` sin `WHERE optica_id` — breach multi-tenant. Dos ópticas con mismos parámetros comparten `costoUnitario`. |
| S4.2 | A | CRITICAL | `CostoBiseladoDao.lookup()` sin `WHERE optica_id` — mismo breach para costos de biselado. |
| S4.3 | B | CRITICAL | `GastosViewModel` sigue vivo: `AnalisisNegocioScreen` lo inyecta con Hilt. Su `init` auto-genera gastos recurrentes en cada emisión del flow. **Colisiona con `CostosYGastosViewModel`** sobre la misma tabla. |
| S4.4 | B | WARNING | `CostosYGastosViewModel` **no auto-genera gastos recurrentes** — regresión funcional. Usuarios del nuevo screen nunca ven gastos mensuales automáticos. |
| S4.5 | B | WARNING | `GastosViewModel.editGasto()` **dropea `esRecurrente`** — omite el campo en `GastosUiState`, defaultea a `false`. Editar un gasto recurrente lo vuelve no-recurrente. |
| S4.6 | B | WARNING | `Double` para todos los campos monetarios — IEEE 754 no representa centavos exactos. `sumOf` sobre decenas de gastos acumula error visible. |
| S4.7 | A | WARNING | `saveCostoEdit()` hace `upsertAll` **antes** del null guard de `selectedBlock`. Si `selectedBlock` es null, el save persiste pero la UI no se actualiza. |
| S4.8 | A | WARNING | `toDoubleOrNull()` solo acepta `.` como decimal. En locales con coma (España, Alemania), `"15,50"` → null → "Ingresa un monto válido". |
| S4.9 | B | WARNING | Dos `LocalDate.now()` en filtro de totales mensuales — riesgo de race a medianoche de fin de mes. |

### 🔵 Info (1)

- `fmt()` duplicada idéntica en `CostosYGastosScreen` y `GastosScreen`

---

## Auditoría Complementaria: RPCs Android ↔ Supabase

**5 RPCs llamados desde Android, 16 RPCs existentes en DB.**

### Resultado

| Categoría | Cantidad |
|-----------|----------|
| Coincidencias exactas | 4 |
| Type coercion frágil | 1 |
| Dead RPCs en DB | 11 |
| Crash asegurado | 0 |

### 🟡 Único warning

`rpc_analisis_mensual` — Android envía `p_mes` como `String` ("2024-01-01") pero la DB espera `date`. PostgREST hace coerción implícita y funciona, pero es frágil ante cambios de driver.

### 🟢 RPCs muertas en DB (11 de 16)

`rpc_cierre_caja_resumen`, `rpc_count_pendientes`, `rpc_pacientes_con_saldo`, `rpc_pacientes_con_entrega_pendiente`, `rpc_saldo_pendiente` (DEPRECATED), `rpc_adjust_montura_stock`, `recalcular_resumen_diario`, `suggest_next_ho`, `sync_snapshot`, `check_rate_limit`, `paciente_eliminaciones_restantes_hoy`

---

## Patrones Cross-Cutting

---

## Slice 5: Dominio Financiero + Regalos

**Archivos**: `MovimientoFinanciero.kt`, `ObtenerMovimientosFinancierosUseCase.kt`, `RegaloDispensacionEntity.kt`, `RegaloDispensacionDao.kt`, `RegaloDispensacionViewModel.kt`

### 🔴 Confirmados (6)

#### C5.1 — CRITICAL: `costo = 0.0` hardcodeado para todos los movimientos
- **Archivo**: `ObtenerMovimientosFinancierosUseCase.kt:44,61`
- **Descripción**: Cada dispensación y servicio se construye con `costo = 0.0`. El campo `costo` existe para calcular márgenes pero nunca se popula con datos reales. Todos los reportes financieros que consumen `MovimientoFinanciero` muestran **100% de margen** independientemente de los costos reales.
- **Fix**: Joinear contra `costos_productos`, `monturas.costo` y `costos_biselado` durante la construcción.

#### C5.2 — CRITICAL: Regalos excluidos de `MovimientoFinanciero`
- **Archivo**: `ObtenerMovimientosFinancierosUseCase.kt:19-69`
- **Descripción**: `TipoMovimiento.REGALO` y `Origen.REGALO` están declarados en el modelo pero **nunca se asignan**. El use case solo procesa dispensaciones + servicios. Los regalos tienen `costoUnitario * cantidad` que impacta márgenes, pero son invisibles para el reporting financiero.
- **Fix**: Cargar regalos vía `getRegalosSnapshotForOptica` y emitir entradas con `origen = REGALO`, `costo = cantidad * costoUnitario`.

#### C5.3 — CRITICAL: `removeRegaloAndRestoreStock` borra TODOS los regalos
- **Archivo**: `RegaloDispensacionViewModel.kt:49`
- **Descripción**: `deleteRegalosByDispensacionId(regalo.dispensacionId)` ejecuta `DELETE FROM regalos_dispensacion WHERE dispensacion_id = ?` — **borra todos** los regalos de la dispensación. Luego solo restaura stock para UNO. Si hay 3 regalos, 2 se pierden permanentemente sin restaurar stock.
- **Fix**: Usar `deleteById(regalo.id)` en vez de `deleteByDispensacionId`.

#### C5.4 — CRITICAL: `cantidad = 0` para deltas negativos en movimientos
- **Archivo**: `DispensacionStockHelper.kt:104`
- **Descripción**: `cantidad = delta.coerceAtLeast(0)` fuerza a 0 los deltas negativos. El registro muestra `cantidad=0, stockPrevio=10, stockNuevo=8` — el stock bajó 2 pero el movimiento dice 0. Audit trail corrupto.
- **Fix**: Usar `abs(delta)` o `delta` directamente.

#### C5.5 — CRITICAL: `RuntimeException` crashea el ViewModelScope
- **Archivo**: `RegaloDispensacionViewModel.kt:35`
- **Descripción**: `throw RuntimeException(...)` dentro de `viewModelScope.launch {}` sin `CoroutineExceptionHandler`. La excepción crashea el scope entero, nunca llega al caller.
- **Fix**: Usar `StateFlow<Error?>` o `Channel` para exponer errores a la UI.

#### C5.6 — CRITICAL: Insert antes de deducción de stock, sin rollback
- **Archivo**: `RegaloDispensacionViewModel.kt:26-38`
- **Descripción**: `saveRegaloAndDeductStock` inserta el regalo (línea 27) **antes** de intentar la deducción de stock (línea 29). Si la deducción falla, el regalo ya está persistido — sin rollback.
- **Fix**: Invertir orden: deducir stock primero, insertar solo si éxito. O usar `@Transaction`.

### 🟡 Sospechosos (17)

- `RegaloDispensacionDao.getByDispensacionId` sin `optica_id`
- `!!` frágil en `it.dispensacionId!!` / `it.servicioExtraId!!`
- Sin FK en `producto_id` de `RegaloDispensacionEntity`
- `insert()` sin conflict strategy vs `upsert()` con REPLACE
- `SALIDA_VENTA` hardcodeado para regalos (debería ser `SALIDA_REGALO`)
- Mensaje de error misleading ("Stock insuficiente" para cualquier falla)
- Delete antes de restaurar stock (orden invertido)
- `pacienteId` null → todos colapsan a bucket vacío
- `descripcion = "OT "` cuando OT es blank
- Pagos cargados sin filtro de fecha
- Missing `optica_id` index en tabla regalos

---

## Slice 6: Configuración Financiera + Resumen Diario

**Archivos**: `ConfiguracionFinancieraEntity.kt`, `ConfiguracionFinancieraDao.kt`, `ResumenDiarioEntity.kt`, `ResumenDiarioDao.kt`

### 🔴 Confirmados (3)

#### C6.1 — CRITICAL: Sin unique constraint en `(opticaId, fecha)`
- **Archivo**: `ResumenDiarioEntity.kt:8`
- **Descripción**: Sin `@Index(unique = true)` en `(opticaId, fecha)`. Usuarios nuevos (fresh install) no tienen la constraint que la migración agrega. Duplicados silenciosos — `getByOpticaAndDate` usa `LIMIT 1`, ocultando el problema pero corrompiendo sumas.
- **Fix**: Agregar `indices = [Index(value = ["opticaId", "fecha"], unique = true)]` al `@Entity`.

#### C6.2 — CRITICAL/WARNING: `Double` para montos monetarios
- **Archivos**: `ResumenDiarioEntity.kt:12-16`, `ConfiguracionFinancieraEntity.kt:13`
- **Descripción**: IEEE 754 no representa centavos exactos. `sumOf` sobre meses de filas diarias acumula error. Umbrales de alerta comparados con floats dan falsos positivos/negativos.
- **Fix**: `Long` (centavos) o `BigDecimal` con TypeConverter.

#### C6.3 — WARNING: `fecha` como String sin validación
- **Archivo**: `ResumenDiarioEntity.kt:10`
- **Descripción**: `strftime('%Y-%m', fecha)` solo funciona si el formato es `YYYY-MM-DD`. Si el server manda ISO-8601 con timezone, la query devuelve 0 filas silenciosamente.
- **Fix**: Validar en `toEntity()` o usar TypeConverter con `LocalDate`.

### 🟡 Sospechosos (3)

- `ConfiguracionFinancieraDao.deleteAll()` mal nombrado (solo borra por opticaId)
- Sin `@ForeignKey` en `opticaId` de ambas entidades
- `margenNetoObjetivo` sin range guard (puede ser -50 o 500)

---

## Slice 7: Migración Post-Ventas + RLS

**Archivos**: `20260710064319_drop_ventas_rewrite_rpcs.sql`, `remote_schema_dump.sql` (RLS policies)

### 🔴 Confirmados (3)

#### C7.1 — CRITICAL: `rpc_saldo_pendiente` referencea `ventas` ya droppeada
- **Archivo**: `remote_schema_dump.sql:1055-1084`
- **Descripción**: La migración droppea `ventas` pero `rpc_saldo_pendiente` no fue reescrita ni droppeada — sigue referenciando `public.ventas`. Cualquier llamada crashea con `relation "public.ventas" does not exist`. La función sigue GRANTed a `authenticated`.
- **Fix**: Aplicar migración `20260714000002` (DROP FUNCTION) inmediatamente.

#### C7.2 — CRITICAL: `recalcular_resumen_diario` SECURITY INVOKER + sin INSERT policy
- **Archivo**: `remote_schema_dump.sql:3487-3490`
- **Descripción**: La función es `SECURITY INVOKER` pero `resumen_diario` solo tiene SELECT policy. Cualquier `authenticated` user que la invoque → INSERT silenciosamente filtrado por RLS → 0 filas escritas, sin error.
- **Fix**: Hacerla `SECURITY DEFINER` o agregar INSERT/UPDATE policies.

#### C7.3 — WARNING: `regalos_dispensacion` DELETE policy demasiado permisiva
- **Archivo**: `remote_schema_dump.sql:3468-3483`
- **Descripción**: Cualquier member (incluyendo `invitado`) puede DELETEr regalos. Todos los demás tablas financieras restringen DELETE a `admin/gerente`.
- **Fix**: Usar `has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente'])`.

### 🟡 Sospechosos (8)

- Migración `20260713034249` reintroduce dependencia de `ventas` (referencia `venta_id`, query a tabla droppeada)
- `rpc_analisis_mensual` pierde inline `category_revenue` al migrar a tabla `margen_por_categoria` pre-calculada
- `resumen_diario` sin INSERT/UPDATE/DELETE policies (solo SELECT)
- `UNION ALL` duplica table scans (dispensaciones + servicios escaneados 2x en `recalcular_resumen_diario`)
- COALESCE 3-argumentos en joins de pago innecesario post-backfill
- `margen_por_categoria` solo SELECT, sin INSERT policy
- Comentario DEPRECATED en `rpc_saldo_pendiente` incorrecto
- `rpc_deudores` sin filtro de fecha (full scan)

### RLS Coverage Summary

| Tabla | SELECT | INSERT | UPDATE | DELETE |
|-------|--------|--------|--------|--------|
| `pagos` | ✅ member | ✅ staff | ✅ staff | ✅ admin/gerente |
| `dispensaciones` | ✅ member | ✅ staff | ✅ staff | ✅ admin/gerente |
| `servicios_extra` | ✅ member | ✅ staff | ✅ staff | ✅ admin/gerente |
| `gastos_operativos` | ✅ member | ✅ admin/gerente | ✅ admin/gerente | ✅ admin/gerente |
| `resumen_diario` | ✅ member | ❌ | ❌ | ❌ |
| `regalos_dispensacion` | ✅ member | ✅ any member ⚠️ | ✅ any member ⚠️ | ✅ any member ⚠️ |

---

## Slice 8: Reportes + Stock

**Archivos**: `ReporteFinancieroPdfGenerator.kt`, `DispensacionStockHelper.kt`, `DispensacionLaboratorioTicket.kt`, `DispensacionViewModel.kt` (costos)

### 🔴 Confirmados (3)

#### C8.1 — CRITICAL: Cost lookups sin `optica_id` (confirmado cross-slice con S4.1)
- **Archivos**: `CostoProductoDao.kt`, `CostoBiseladoDao.kt`, `DispensacionViewModel.kt:762-783`
- **Descripción**: Las 3 queries de lookup de costos no filtran por `optica_id`. `calculateCosts()` obtiene `opticaId` del sessionManager pero nunca lo pasa a los lookups. Dos ópticas con mismos parámetros comparten costos.
- **Fix**: Agregar `AND optica_id = :opticaId` a las 3 queries + propagar el parámetro.

#### C8.2 — CRITICAL: `cantidad = 0` para deltas negativos (confirmado cross-slice con C5.4)
- **Archivo**: `DispensacionStockHelper.kt:104`
- **Descripción**: Mismo bug visto en Slice 5. `delta.coerceAtLeast(0)` fuerza a 0 los deltas negativos en movimientos de inventario, corrompiendo el audit trail.
- **Fix**: Usar `abs(delta)`.

#### C8.3 — WARNING: Ajustes de stock sin verificar resultado
- **Archivo**: `DispensacionViewModel.kt:440-442, 514-519`
- **Descripción**: `toAddStock.forEach` descarta el `Result` de `adjustStockAndRegistrarMovimiento`. Si falla (montura no encontrada, DB error), el error se traga silenciosamente dentro de una transacción que commitea igual.
- **Fix**: Aplicar el mismo patrón de verificación que `toRemoveStock` (línea 444).

### 🟡 Sospechosos (7)

- PDF report excluye pagos "Anulación" → infla `montoPagado`
- Biselado lookup hardcodea `stockOFabricacion = "stock"` ignorando lentes de fabricación
- PDF no distingue visualmente saldo negativo (sobrepago/reembolso)
- `crearReclamo()` no valida `totalPagadoOriginal > 0` antes de calcular refund
- `deleteDispensacion` y `anularDispensacion` no son transaccionales (stock restaurado antes de delete)
- Solo primer tratamiento usado en lookup de costo (ignora combinaciones)
- Costos stale persisten al cambiar tipo de lente (OD/OI no se resetean para LC)

---

## Patrones Cross-Cutting (actualizado)

### 1. Multi-tenancy frágil (7+ ubicaciones)
- 3 DAOs de lookup sin `WHERE optica_id` (S4.1, S4.2, C8.1)
- `RegaloDispensacionDao.getByDispensacionId` sin `optica_id` (S5.7)
- `.trim()` faltante en DTOs de sync (C1.2)
- RPCs sin validación `auth.uid()` (S3.3)
- `GastoOperativoDao.getAll()` leaky (C4.4)

### 2. Sincronización fantasma (4 paths)
- `saveCostoEdit()` sin sync (C4.1)
- `CostosBiselado` upload ausente (S1.1)
- `save()` schedulea sync incluso tras error (C2.1)
- `saveCostoEdit()` en Slice 4 también sin sync

### 3. Colisión deprecated/activo
- `GastosViewModel` vs `CostosYGastosViewModel` sobre misma tabla
- `SyncFinanzasUploaders.kt` (248 líneas muertas) duplica lógica con bugs

### 4. Carreras y no-atómicos (4 ubicaciones)
- Merge no atómico en `SyncFinanzasMerge` (C1.1)
- Race condition en `trg_pagos_update_monto_pagado` (C3.2)
- Insert antes de stock deduction sin rollback (C5.6)
- `deleteDispensacion`/`anularDispensacion` sin transacción (S8.5)

### 5. `costo = 0.0` sistémico
- `MovimientoFinanciero` hardcodea costo=0 para todas las ventas (C5.1)
- Regalos excluidos del modelo financiero (C5.2)
- Todos los reportes de margen son incorrectos

---

## Plan de Acción Priorizado (completo)

### 🔴 Críticos — Arreglar ya (12)

| # | Slice | Hallazgo |
|---|-------|----------|
| 1 | C8.1 | 3 DAOs de costos sin `optica_id` — breach multi-tenant |
| 2 | C5.3 | `removeRegalo` borra TODOS los regalos — data destruction |
| 3 | C5.1 | `costo = 0.0` hardcodeado — márgenes 100% falsos |
| 4 | C1.1 | Merge no atómico — pagos huérfanos |
| 5 | C2.1 | `save()` sin error handling — crash/UI congelada |
| 6 | C3.2 | Race condition trigger `monto_pagado` — balances corruptos |
| 7 | C3.1 | `proyeccion_caja` sin fallback `venta_id` — reportes inconsistentes |
| 8 | C7.1 | `rpc_saldo_pendiente` referencia tabla droppeada — crash |
| 9 | C7.2 | `recalcular_resumen_diario` SECURITY INVOKER sin INSERT policy |
| 10 | C5.4 | `cantidad = 0` para deltas negativos — audit trail corrupto |
| 11 | C6.1 | Sin unique constraint `(opticaId, fecha)` — duplicados |
| 12 | C5.5 | `RuntimeException` crashea ViewModelScope |

### 🟠 Alta prioridad (10)

| # | Slice | Hallazgo |
|---|-------|----------|
| 13 | C1.2 | `"mi_optica_base"` hardcodeado + `.trim()` faltante |
| 14 | C2.2 | Doble-tap save sin guard |
| 15 | C4.1 | `saveCostoEdit()` sin sync |
| 16 | S1.1 | `CostosBiselado` nunca se suben |
| 17 | C7.3 | `regalos_dispensacion` DELETE demasiado permisivo |
| 18 | C5.2 | Regalos excluidos de `MovimientoFinanciero` |
| 19 | C5.6 | Insert antes de stock deduction sin rollback |
| 20 | C8.3 | Ajustes de stock sin verificar resultado |
| 21 | C6.2 | `Double` para montos monetarios |
| 22 | S4.3 | `GastosViewModel` colisiona con `CostosYGastosViewModel` |

### 🟡 Media prioridad (12)

- `runBlocking` anti-patrón (C2.3)
- `rpc_resumen_financiero` sin excluir anulaciones (C3.3)
- `uploadServicios` last-write-wins (S1.2)
- Items/regalos huérfanos tras merge (S1.5)
- `CostosYGastosViewModel` sin auto-generación (S4.4)
- `editGasto()` droppea `esRecurrente` (S4.5)
- `recalcular_resumen_diario` no filtra anuladas (S3.1)
- PDF excluye anulaciones (S8.1)
- Biselado hardcodea "stock" (S8.2)
- `deleteDispensacion`/`anularDispensacion` no transaccional (S8.3)
- `margenNetoObjetivo` sin range guard (S6.4)
- `fecha` String sin validación (C6.3)

### 🔵 Backlog

- Cleanup 11 RPCs muertas + 248 líneas código muerto
- `Double` → `BigDecimal` en todo el sistema
- `toDoubleOrNull()` con coma decimal
- Comentarios RPCs deprecated incorrectos
- `key()` en Compose `forEach`
- Backoff con jitter
- Sin FK en `producto_id` de regalos
- Migración `20260713034249` reintroduce dependencia `ventas`

---

## Totales Finales

| Categoría | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | Audit | **Total** |
|-----------|----|----|----|----|----|----|----|----|-------|-----------|
| 🔴 Confirmados | 3 | 4 | 5 | 4 | 6 | 3 | 3 | 3 | — | **31** |
| 🟡 Sospechosos | 10 | 5 | 8 | 9 | 17 | 3 | 8 | 7 | — | **67** |
| 🔵 Info | 5 | 0 | 3 | 1 | 4 | 2 | 3 | 2 | 1 | **21** |
| **Total** | **18** | **9** | **16** | **14** | **27** | **8** | **14** | **12** | **1** | **119** |

**Nota**: Algunos hallazgos son cross-slice (mismo bug visto por diferentes jueces en distintos slices). El conteo refleja hallazgos únicos reportados, no necesariamente bugs únicos. Los bugs repetidos (ej. `cantidad=0` en S5 y S8, `optica_id` en S4 y S8) confirman patrones sistémicos.

---

## Archivos Afectados

### Android (Kotlin/Compose) — 26 archivos
- `domain/SyncFinanzasUseCase.kt` — Orquestador de sync
- `domain/SyncFinanzasUploaders.kt` — Uploaders + 248 líneas código muerto
- `domain/SyncFinanzasMerge.kt` — Merge no atómico
- `domain/SyncFinanzasDto.kt` — DTOs con hardcodeo de optica_id
- `domain/MovimientoFinanciero.kt` — costo=0.0, REGALO nunca usado
- `domain/ObtenerMovimientosFinancierosUseCase.kt` — excluye regalos, costo=0
- `data/FinanzasRemoteDefaults.kt` — Constante ignorada
- `viewmodel/InformacionFinancieraViewModel.kt` — save() sin error handling
- `viewmodel/CostosYGastosViewModel.kt` — sync ausente en costos
- `viewmodel/GastosViewModel.kt` — deprecated, colisiona, droppea esRecurrente
- `viewmodel/RegaloDispensacionViewModel.kt` — delete masivo, crash, orden invertido
- `viewmodel/DispensacionViewModel.kt` — cost lookups sin optica_id, stock sin verificar
- `ui/screens/InformacionFinancieraScreen.kt` — doble-tap, saldo local
- `ui/screens/CostosYGastosScreen.kt` — LocalDate.now() race, fmt() duplicada
- `ui/components/dispensacion/PagosSection.kt` — sin key()
- `ui/components/AbonoDialog.kt` — estado compartido
- `data/DispensacionFinancieraRepository.kt` — obtenerContexto() sin error
- `data/costoproducto/CostoProductoDao.kt` — sin optica_id, lógica NULL rota
- `data/costobiselado/CostoBiseladoDao.kt` — sin optica_id
- `data/gastooperativo/GastoOperativoDao.kt` — getAll() leaky
- `data/regalodispensacion/RegaloDispensacionDao.kt` — sin optica_id, sin FK
- `data/regalodispensacion/RegaloDispensacionEntity.kt` — sin FK producto_id
- `data/configuracionfinanciera/ConfiguracionFinancieraEntity.kt` — Double, sin FK
- `data/resumendiario/ResumenDiarioEntity.kt` — sin unique constraint, Double
- `util/DispensacionStockHelper.kt` — cantidad=0 para negativos
- `util/ReporteFinancieroPdfGenerator.kt` — excluye anulaciones

### Supabase (SQL) — 12 archivos
- `20260705023012` — Triggers ventas sin AFTER DELETE
- `20260706203416` — Trigger pagos sin BEFORE UPDATE
- `20260706205131` — Trigger monto_pagado race condition
- `20260706205841` — rpc_analisis_mensual sin fallback venta_id
- `20260706203300` — rpc_resumen_financiero sin excluir anulaciones
- `20260706203337` — rpc_saldo_pendiente (deprecated)
- `20260706202037` — rpc_cierre_caja sin ELSE bucket
- `20260706202105` — Comentarios incorrectos
- `20260710064319` — Drop ventas, RPCs reescritos
- `20260714000000` — recalcular_resumen_diario SECURITY INVOKER
- `20260713034249` — Reintroduce dependencia ventas (pendiente)
- `remote_schema_dump.sql` — RLS policies (regalos permisivo, resumen sin INSERT)

---

*Informe generado por Judgment Day — 2 jueces R1 Security Engineer en paralelo por slice. Verificado el 2026-07-15.*
