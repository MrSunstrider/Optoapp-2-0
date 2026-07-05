# Design: InformacionFinancieraScreen

## Technical Approach

Extract financial management from `NuevaDispensacionScreen` into a dedicated screen. A `DispensacionFinancieraRepository` interface decouples the ViewModel from `OptoRepository` for testability. The screen reuses existing components (`AbonoDialog`, `OptoTextField`, `DropdownField`) and follows the same MVVM pattern as `DispensacionViewModel`.

## Architecture Decisions

| Decision | Option | Tradeoff | Choice |
|----------|--------|----------|--------|
| Repository pattern | Dedicated `DispensacionFinancieraRepository` interface vs inject `OptoRepository` directly | Interface adds one more file but enables unit-test mocking without MockK relaxed stubs. Direct injection follows existing codebase pattern. | Dedicated interface (per proposal — test isolation wins) |
| Venta ID format | Reuse `v_disp_{dispensacionId}` vs create new prefix | Reusing matches `DispensacionViewModel` Venta creation. New prefix would risk duplicate rows for same disp. | Reuse `v_disp_{dispensacionId}` — consistent, idempotent upsert |
| Sticky header | TopAppBar vs Column with card background | TopAppBar is platform-native for back+title. Column card aligns better with scrollable content below. | `OptoTopAppBar` for back/title + Column for context card below it |
| Pagos edit dialog | `AbonoDialog` existing component | Already handles add/edit with montoMaximo constraint. No need to rebuild. | Reuse `AbonoDialog` |
| Save navigation | `onComplete` callback vs direct `navController.popBackStack()` | Callback keeps screen decoupled from nav implementation. | Callback `onComplete` — same pattern as `DispensacionViewModel.saveDispensacion` |

## Data Flow

```
InformacionFinancieraScreen
        │  StateFlow<FinancieraUiState>
        ▼
InformacionFinancieraViewModel
        │  DispensacionFinancieraRepository
        │  SessionManager (opticaId)
        │  PostSaveSyncScheduler
        ▼
DispensacionFinancieraRepositoryImpl
        │  OptoRepository (dispensacion CRUD, pagos CRUD)
        │  VentaDao (upsert)
        ▼
Room DB (dispensaciones, pagos, ventas)
```

### Save Flow Sequence

```
User                Screen              ViewModel              Repository              OptoRepository/DB
 │                    │                     │                      │                         │
 │── tap "Guardar"───▶│                     │                      │                         │
 │                    │── save() ──────────▶│                      │                         │
 │                    │                     │── opticaId ────────▶│                         │
 │                    │                     │── getDispensacion()─▶│── getDispensacionById()─▶│
 │                    │                     │                      │◀── DispensacionOptica ───│
 │                    │                     │◀─────────────────────│                         │
 │                    │                     │                      │                         │
 │                    │                     │── updateDispensacion(montoTotal, estado) ──────▶│
 │                    │                     │                      │                         │
 │                    │                     │── [for each new pago] insertPago() ────────────▶│
 │                    │                     │── [for each modified] updatePago() ────────────▶│
 │                    │                     │── [for each deleted] deletePagoRegistrandoAnulacionEnCaja()─▶│
 │                    │                     │                      │                         │
 │                    │                     │── upsertVenta() ────▶│── ventaDao.upsert() ────▶│
 │                    │                     │                      │◀──────────────────────────│
 │                    │                     │                      │                         │
 │                    │                     │── scheduleFinanzasSync(opticaId)               │
 │                    │                     │                      │                         │
 │                    │◀── onComplete() ────│                      │                         │
 │                    │                     │                      │                         │
 │◀── popBackStack ──│                     │                      │                         │
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `.../data/DispensacionFinancieraRepository.kt` | Create | Interface: `obtenerDispensacion()`, `obtenerContexto()`, `actualizarMontoTotal()`, `agregarPago()`, `editarPago()`, `eliminarPago()`, `actualizarEstado()`. Impl delegates to `OptoRepository` + `VentaDao`. |
| `.../viewmodel/InformacionFinancieraViewModel.kt` | Create | `@HiltViewModel` with `DispensacionFinancieraRepository`, `SessionManager`, `PostSaveSyncScheduler`. Exposes `FinancieraUiState` StateFlow. `save()` persists pagos + Venta + schedules sync. |
| `.../ui/screens/InformacionFinancieraScreen.kt` | Create | Sticky header (OT, paciente, fecha, descripcion), `OptoTextField` for monto total, pagos list via `AbonoDialog`, saldo reactivo, `DropdownField` for estado. Guardar button. |
| `.../ui/screens/NuevaDispensacionScreen.kt` | Modify | Replace `FinancieraInfoSection` call (lines 146-152) with Card summary (monto, saldo, estado) + "Gestionar Pagos" button that navigates to `informacion_financiera/{dispensacionId}`. |
| `.../ui/screens/DispensacionFormSections.kt` | Modify | Mark `FinancieraInfoSection` as `@Deprecated("Use InformacionFinancieraScreen instead")`. Keep for Fase 4 rollback window. |
| `.../ui/screens/MainDrawerScreen.kt` | Modify | Add `composable("informacion_financiera/{dispensacionId}")` in NavHost with null guard (same pattern as `editarDispensacion`). |
| `.../di/DatabaseModule.kt` | Modify | Add `@Provides` for `DispensacionFinancieraRepository` binding to impl. |

## Interfaces / Contracts

```kotlin
// DispensacionFinancieraRepository.kt
interface DispensacionFinancieraRepository {
    suspend fun obtenerDispensacion(dispensacionId: String): Resource<DispensacionOptica>
    suspend fun obtenerContexto(dispensacionId: String): ContextoFinanciero  // OT, paciente, fecha, descripcion
    suspend fun actualizarMontoTotal(dispensacionId: String, montoTotal: Double, opticaId: String)
    suspend fun actualizarEstado(dispensacionId: String, estado: String, fechaEntrega: LocalDate?, opticaId: String)
    suspend fun agregarPago(pago: Pago)
    suspend fun editarPago(pago: Pago)
    suspend fun eliminarPago(pago: Pago, opticaId: String)
    suspend fun upsertVenta(venta: Venta)
}

data class ContextoFinanciero(
    val ot: String,
    val pacienteNombre: String,
    val fecha: LocalDate,
    val descripcion: String
)
```

```kotlin
// FinancieraUiState (in ViewModel file)
data class FinancieraUiState(
    val dispensacionId: String = "",
    val contexto: ContextoFinanciero? = null,
    val montoTotal: String = "",
    val pagos: List<Pago> = emptyList(),
    val estadoEntrega: String = "Pendiente",
    val fechaEntrega: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val saldoRestante: Double
        get() {
            val total = montoTotal.toDoubleOrNull() ?: 0.0
            val pagado = pagos.sumOf { it.monto }
            return total - pagado
        }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `InformacionFinancieraViewModel` — save persiste pagos + Venta, saldo reactivity, estado toggle | MockK + `StandardTestDispatcher` + `runTest` (same as `DispensacionViewModelVentaTest`) |
| Unit | `DispensacionFinancieraRepositoryImpl` delegation correctness | MockK on `OptoRepository` + `VentaDao` |
| Integration | Screen composable renders with mock ViewModel | `createComposeRule()` with fake StateFlow |
| E2E | Full flow: navigate from NuevaDispensacion → edit financials → return | Instrumented test (`androidTest`) |

No `.instrumentation` tests planned — existing pattern uses Robolectric for unit and optional instrumented for UI flow.

## Migration / Rollout

No data migration required. Financiera data persists in existing `dispensaciones`, `pagos`, and `ventas` tables.

## Open Questions

- [ ] None — all dependent methods confirmed in `OptoRepository` (`upsertVenta`, `getPagosByDispensacion`, `insertPago`, `updatePago`, `deletePagoRegistrandoAnulacionEnCaja`, `updateDispensacion`)
