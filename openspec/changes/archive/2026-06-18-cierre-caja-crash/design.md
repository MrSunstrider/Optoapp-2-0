# Design: Fix CierreCaja Crash and Related Cash-Close Bugs

## Technical Approach

Six isolated defect fixes on the cash-close vertical slice (`CierreCajaScreen` + two ViewModels + DAO + DI). No schema migration, no cross-cutting refactor. Each fix is local to one file and reverts cleanly. Order: crash (1) → correctness (2-4) → lifecycle/robustness (5-6). The only structural change is moving the blocking DataStore read out of DI (`@CurrentUserId`) and into `ArqueoCajaViewModel.init {}` as an async resolution, which removes a main-thread `runBlocking` and an unnecessary Hilt qualifier binding.

## Architecture Decisions

| # | Decision | Alternatives rejected | Rationale |
|---|----------|----------------------|-----------|
| 1 | Replace nested `LazyColumn` with `Column { forEach }` | Give the parent a fixed-height `LazyColumn`; nested scroll modifiers | Parent already uses `verticalScroll`; a child lazy list with unbounded height throws `IllegalStateException`. Transaction lists are small (one day), so `Column` is correct and cheap. |
| 3 | Resolve user email async in `ArqueoCajaViewModel.init {}`; drop `@CurrentUserId` | Keep qualifier but make it `suspend`/`Flow`; `Dispatchers.IO` runBlocking | DI providers run on the caller thread (main during graph init) — `runBlocking` on DataStore risks ANR. `viewModelScope` + `userEmail.first()` defers I/O off the critical path. Removing the qualifier deletes the only consumer of the blocking provider. |
| 4 | Align `cerrarDia()` lookup keys to stored title-case values | Lowercase both sides via `.lowercase()` | Stored `Pago.metodoPago` is title-case everywhere (`"Efectivo"`, `"Tarjeta"`, `"Transferencia"`, `"Móvil"`; default `METODO_PAGO_VACIO = "Efectivo"`). `getTotalesPorMetodo()` keys by raw value. Matching the existing canonical casing is the smallest, least surprising fix and keeps the map readable. |
| 5 | `flatMapLatest` over a fecha/optica `StateFlow` for the arqueo observer | `Job?` field + manual cancel | `observePagos()` already uses the `combine + distinctUntilChanged + flatMapLatest + launchIn` pattern in this same ViewModel. Reusing it guarantees a single active collector and matches house style; a manual `Job` would be a second, inconsistent idiom. |
| 6 | Switch `insertArqueo` to `OnConflictStrategy.REPLACE` | try/catch + error UI state; route callers to existing `upsertArqueo` | Business rule: re-closing the same date must update, never throw. `REPLACE` makes the insert idempotent with no caller change. A sibling `upsertArqueo(REPLACE)` already exists, confirming REPLACE is the intended idempotent path. |

Bug #2 (Hilt import) is a one-line correction — no decision needed.

## Data Flow

    SessionManager.userEmail (DataStore, async)
            │  init {} viewModelScope.launch { .first() }
            ▼
    ArqueoCajaViewModel.currentUserId ──► cerrarDia() ──► repo.insertArqueo (REPLACE)

    AuthVM.opticaId ─┐
    uiState.fecha ───┼─► CierreCajaVM: fechaOptica StateFlow
                     └─► flatMapLatest ─► getArqueoByFecha ─► uiState.arqueoForFecha
                                          (single collector)

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/screens/CierreCajaScreen.kt` | Modify | L26 fix import to `androidx.hilt.navigation.compose.hiltViewModel`; L259-266 swap `LazyColumn{items}` → `Column(verticalArrangement = spacedBy(8.dp)){ uiState.pagos.forEach { TransactionItem(it) } }`; drop now-unused `LazyColumn`/`items` imports (L6-7). |
| `viewmodel/ArqueoCajaViewModel.kt` | Modify | Remove `@CurrentUserId` ctor param and import; inject `SessionManager`; add `private var currentUserId: String = ""`; `init { viewModelScope.launch { currentUserId = sessionManager.userEmail.first() } }`; align `systemTotals` keys to `"Efectivo"/"Tarjeta"/"Transferencia"/"Móvil"`. |
| `viewmodel/CierreCajaViewModel.kt` | Modify | Convert `observeArqueoForDate` to drive a `MutableStateFlow<Pair<LocalDate,String>?>` consumed once via `flatMapLatest{ getArqueoByFecha }.onEach{ update arqueoForFecha }.launchIn(viewModelScope)` set up in `init`. |
| `data/arqueo/ArqueoCajaDao.kt` | Modify | L26 `OnConflictStrategy.ABORT` → `OnConflictStrategy.REPLACE`. |
| `di/DatabaseModule.kt` | Modify | Delete `provideCurrentUserId` (L247-253); remove `runBlocking`/`first` imports if now unused. |

## Interfaces / Contracts

```kotlin
// ArqueoCajaViewModel — new constructor shape
@HiltViewModel
class ArqueoCajaViewModel @Inject constructor(
    private val repo: IArqueoCajaRepo,
    private val sessionManager: SessionManager
) : ViewModel() {
    private var currentUserId: String = ""   // populated async in init
}

// cerrarDia key lookup — canonical title-case
val efCobrado    = systemTotals["Efectivo"]      ?: 0.0
val tarCobrado   = systemTotals["Tarjeta"]       ?: 0.0
val transCobrado = systemTotals["Transferencia"] ?: 0.0
val movCobrado   = systemTotals["Móvil"]         ?: 0.0
```

No public API or DAO signature changes (only the conflict strategy annotation).

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `cerrarDia` builds `ArqueoCaja` with correct per-method cobrado from title-case `systemTotals`; diferencias computed right | Fake `IArqueoCajaRepo`, `runTest`, assert captured arqueo; assert non-zero when payments exist |
| Unit | `getTotalesPorMetodo` returns title-case keys matching `cerrarDia` lookups | Seed `uiState.pagos`, assert key set |
| Unit | Re-`cerrarDia` same date → `insertArqueo` called twice without throwing (REPLACE behavior at repo boundary) | Fake repo records calls; assert no exception |
| Unit | `observeArqueoForDate` emits only latest on rapid fecha changes (single collector) | `flatMapLatest` over test flow, assert no stale emission |
| Instrumentation | `ArqueoCajaDao.insertArqueo` REPLACE upserts same PK row | Room in-memory DB, insert twice same id/fecha, assert row count 1 |
| Instrumentation/UI | `CierreCajaScreen` renders non-empty payment list without `IllegalStateException` | Compose UI test with seeded `uiState.pagos` |

Mock strategy: ViewModels take interfaces (`IArqueoCajaRepo`, `SessionManager`) — substitute fakes returning controlled flows; no Hilt needed in unit tests. DAO REPLACE needs real Room (in-memory) since conflict resolution is a DB concern.

## Migration / Rollout

No migration required. No DB version bump, no persisted-state format change. REPLACE only affects runtime conflict handling on the existing `arqueo_caja` table.

## Open Questions

- [ ] Confirm `SessionManager.userEmail` first emission is non-empty at the moment `cerrarDia` may run; if a user can press "Cerrar Día" before `init` resolves email, `cerradoPor` would be `""`. Mitigation: guard the button or fall back to a stored value. (Low risk — see Risks.)
- [ ] REPLACE silently overwrites a prior same-date arqueo (intended per business rule). Confirm no audit requirement to preserve the superseded record.
