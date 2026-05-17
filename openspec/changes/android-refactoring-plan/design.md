# Design: Android Codebase Refactoring

## Technical Approach

Refactorización pura — **ninguna tabla/algoritmo cambia**. Cada item sigue: (1) characterization tests, (2) refactor, (3) verify. Los 19 items (A1-A19) se ejecutan en orden de prioridad P0→P1→P2→P3. El diseño documenta la **arquitectura objetivo** post-refactoring.

## Architecture Decisions

### ADR-1: Container/Presenter para EvaluacionScreen (A1)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Mantener saveAction en Screen | Screen sigue con SharedPrefs + NotificationHelper inline | ❌ |
| Extraer a EvaluacionViewModel | ViewModel ya tiene `saveEvaluacion()`; falta notificaciones | ✅ |

**Decisión**: Mover `saveAction` (SharedPreferences + NotificationHelper) al ViewModel como un método `saveAndScheduleReminder()`. La Screen recibe `(pacienteId, evaluacionId) -> Unit` como lambda. Los datepickers, tabs y dialogos locales se quedan en la Screen.

**Consecuencia**: EvaluacionViewModel necesita `NotificationHelper` inyectado por Hilt. Se agrega `@Inject constructor(... private val notificationHelper: NotificationHelper)`.

### ADR-2: Delegados para AuthViewModel (A2)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Un solo ViewModel de 567 líneas | No testeable, mezcla login/PIN/backup | ❌ |
| Delegados separados inyectados | Tests con fakes, SRP | ✅ |

**Decisión**: Extraer tres delegados:
- `AuthDelegate` — login, register, logout, session check, backup
- `PinDelegate` — PIN validation, creation, update (reutiliza SecurityManager)
- `BackupDelegate` — export/restore, duplicate resolution

Cada delegate es una `class` plana inyectable con Hilt. AuthViewModel se queda como orquestador: recibe los 3 delegates y expone un API unificado.

**Consecuencia**: Cambiar `DatabaseModule.kt` para proveer los delegates. AuthViewModel pasa de 567→~100 líneas.

### ADR-3: DAOs separados por archivo (A3, A4)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Status quo: PagoDao, ServicioExtraDao, MonturaDao, MonturaMovimientoDao inline en DispensacionDao.kt | Archivo de 186 líneas con 5 interfaces | ❌ |
| Extraer cada DAO a su propio archivo | Consistencia con PacienteDao, EvaluacionDao | ✅ |

**Decisión**: Mover `PagoDao`, `ServicioExtraDao`, `MonturaDao`, `MonturaMovimientoDao` a archivos individuales en `data/pago/PagoDao.kt`, `data/servicio/ServicioExtraDao.kt`, `data/montura/MonturaDao.kt`. DispensacionDao.kt queda solo con su interface.

**Consecuencia**: `OptoDatabase.kt` se mantiene como coordinador referenciando los DAOs. Migraciones se extraen a `OptoDatabaseMigrations.kt`.

### ADR-4: Builder Pattern para PDF Generator (A6)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Objeto único de 510 líneas | Imposible de testear por separado | ❌ |
| Builder con sub-módulos | Cada sección testeable, reutilizable | ✅ |

**Decisión**: Crear `RecetaPdfBuilder` con métodos `addHeader()`, `addRefraccion()`, `addLentes()`, `addDiagnostico()`, `addFirma()` que devuelven `this` (builder). Cada método recibe solo los datos que necesita. El `build()` produce el `PdfDocument`. Las constantes de estilo (colores, paints) se mantienen en un objeto `PdfStyle`.

### ADR-5: Delegados Sync separados (A5)

Siguiendo el patrón de `SyncFinanzasUseCase` (upload/download separados + merge handler):

**Decisión**: Extraer `uploadEvaluaciones()` y `downloadEvaluaciones()` del cuerpo de `SyncHistorialUseCase` a funciones privadas de nivel superior (mismo archivo o archivo `SyncHistorialMerge.kt`). El `invoke()` orquesta: upload → download.

**Consecuencia**: SyncHistorialUseCase pasa de 149→~80 líneas. La lógica de merge de FK de pacientes se mantiene en upload().

### ADR-6: Parallel awaitAll en OperacionHoyViewModel (A11)

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| flatMapLatest + combine (actual) | Las 5 queries corren EN SERIE dentro del flatMapLatest | ❌ |
| async + awaitAll | Las 5 queries corren EN PARALELO | ✅ |

**Decisión**: Reemplazar `trigger.flatMapLatest { ... combine(5 flows) }` con `async + awaitAll` en `viewModelScope.launch`. El estado se publica con `_uiState.value = OperacionHoyUiState(...)`.

### ADR-7: Caracterización + cambios específicos (varios)

| Item | Decisión |
|------|----------|
| A7 (ConfiguracionScreen) | Ya tiene secciones extraídas (`ui.components.config.*`). Mover estado persistente (notificaciones, PIN) a SettingsViewModel existente. |
| A8 (DetallePacienteScreen) | Ya tiene sub-composables. Mantener solo coordinación de ViewModels. |
| A9 (AppointmentReminderWorker) | Inyectar `NotificationHelper` via `HiltWorker`. Tests con `TestListenableWorkerBuilder`. |
| A10 (SecurityManager) | Eliminar `migratePinHasBeenSet()`. Simplificar lógica "123456". |
| A12 (NuevaDispensacionScreen) | Extraer adjustStock + movimiento de montura a `DispensacionStockHelper`. |
| A13, A14 (Billing) | `@Deprecated("Sin uso en alpha. Mantener por posible reactivación.")` en clase y companion. NO eliminar. |
| A15 (Type.kt) | Completar Typography con `displayLarge`, `headlineMedium`, `titleMedium`, `bodyMedium`, `labelSmall`. |
| A16 (SyncCancellation + SyncGate) | Mover `rethrowIfCancellation()` a `sync/SyncGate.kt` como función de top-level. Eliminar SyncCancellation.kt. |
| A18 (WhatsAppUtils) | Agregar `shareViaWhatsApp()` a FileShareUtils. WhatsAppUtils.kt se elimina o re-exporta. |
| A19 (OnboardingOpticaScreen) | Agregar `@Deprecated("Flujo migrado a web")` y redirect temprano. |

## Data Flow

```
NuevaEvaluacionScreen (UI only)
    │
    ├── tabs, datepickers, dialogs ──→ estado local (remember)
    │
    └── user taps save ──→ EvaluacionViewModel.saveAndScheduleReminder()
                                │
                                ├── saveEvaluacion() → Room
                                ├── scheduleReminder() → NotificationHelper
                                └── scheduleHistorialSync() → PostSaveSyncScheduler
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/screens/NuevaEvaluacionScreen.kt` | Modify | Eliminar saveAction; recibir callback del VM |
| `viewmodel/EvaluacionViewModel.kt` | Modify | Agregar `saveAndScheduleReminder()`, inyectar NotificationHelper |
| `viewmodel/auth/AuthDelegate.kt` | Create | Login, register, logout, session check |
| `viewmodel/auth/PinDelegate.kt` | Create | PIN creation, validation, update |
| `viewmodel/auth/BackupDelegate.kt` | Create | Export, restore, duplicate resolution |
| `viewmodel/AuthViewModel.kt` | Modify | Orquestar los 3 delegates (~100 líneas) |
| `data/OptoDatabase.kt` | Modify | Mantener solo referencias a DAOs |
| `data/OptoDatabaseMigrations.kt` | Create | Todas las MIGRATION_* const |
| `data/dispensacion/DispensacionDao.kt` | Modify | Solo interface DispensacionDao |
| `data/pago/PagoDao.kt` | Create | Desde inline en DispensacionDao.kt |
| `data/servicio/ServicioExtraDao.kt` | Create | Desde inline |
| `data/montura/MonturaDao.kt` | Create | Desde inline |
| `data/montura/MonturaMovimientoDao.kt` | Create | Desde inline |
| `domain/SyncHistorialUseCase.kt` | Modify | upload/download en funciones separadas |
| `util/RecetaEvaluacionPdfGenerator.kt` | Modify | Constantes a PdfStyle, builder methods |
| `util/RecetaPdfBuilder.kt` | Create | Builder pattern para secciones PDF |
| `ui/screens/OperacionHoyScreen.kt` | Modify | Sin cambios (UI pura) |
| `viewmodel/OperacionHoyViewModel.kt` | Modify | async+awaitAll en vez de combine secuencial |
| `notifications/AppointmentReminderWorker.kt` | Modify | Inyectar NotificationHelper via @HiltWorker |
| `data/SecurityManager.kt` | Modify | Simplificar migratePinHasBeenSet() |
| `util/DispensacionStockHelper.kt` | Create | Lógica de stock movements |
| `ui/screens/NuevaDispensacionScreen.kt` | Modify | Delegar stock al helper |
| `billing/PlayBillingManager.kt` | Modify | @Deprecated + advertencia |
| `subscription/SubscriptionManager.kt` | Modify | @Deprecated + advertencia |
| `ui/theme/Type.kt` | Modify | Completar Typography scale |
| `sync/SyncGate.kt` | Modify | Agregar `rethrowIfCancellation()` |
| `util/SyncCancellation.kt` | Delete | Contenido movido a SyncGate.kt |
| `util/FileShareUtils.kt` | Modify | Agregar `shareViaWhatsApp()` |
| `util/WhatsAppUtils.kt` | Delete | Contenido movido a FileShareUtils |
| `ui/screens/OnboardingOpticaScreen.kt` | Modify | @Deprecated + redirect early |

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | Delegados de AuthViewModel | Fakes para SecurityManager, SessionManager, SupabaseClient |
| Unit | EvaluacionViewModel.saveAndScheduleReminder() | Mock NotificationHelper + OptoRepository |
| Unit | OperacionHoyViewModel | Verificar que async+awaitAll lanza todas las queries |
| Unit | RecetaPdfBuilder | Probar cada sección por separado |
| Unit | DispensacionStockHelper | Verificar adjustStock + movimiento |
| Integration | Workers | TestListenableWorkerBuilder + coroutine rules |
| Integration | DAOs extraídos | Room in-memory database |

## Migration / Rollout

No migration requerida. Cada item se revierte con `git revert <commit>` por archivo. Room schema version no cambia.

## Open Questions

None — todas las decisiones están mapeadas de la propuesta + codebase real.
