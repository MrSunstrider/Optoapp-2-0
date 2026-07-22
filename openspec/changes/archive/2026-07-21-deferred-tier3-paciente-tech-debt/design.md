# Design: Deferred Tier-3 Paciente Tech Debt (Items 15, 18-22)

## Technical Approach

Six items across two independent groups. Group A (items 15, 18, 21) touches data/domain layers — sync serialization, SQL optimization, sort relocation. Group B (items 19, 20, 22) is pure UI — new composable, display chips, spinner fix. No Supabase schema changes. No new capabilities. Strict TDD: write failing tests before every implementation step.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|---|---|---|---|---|
| **Item 15**: Sync serialization format | CSV `joinToString(",")` → JSON `Json.encodeToString` | CSV with escaped separators | JSON (A) | Resistente a comas en valores. Consistente con Room TypeConverter que ya usa JSON. El fallback CSV en `toEntity()` maneja filas existentes por compatibilidad. |
| **Item 18**: MAX calculation | SQL `MAX(CAST(SUBSTR(historiaOptometrica, 8) AS INTEGER))` | In-memory loop (actual) | SQL MAX (A) | O(1) memoria, usa indice de opticaId. El formato `HO-YYYY-NNNN` garantiza offset=8 estable. |
| **Item 21**: Sort location | ViewModel (derived StateFlow) | DAO `ORDER BY` per column | ViewModel (A) | Dynamic user-selectable sort. DAO-level ORDER BY for every permutation over-engineered para volumenes tipicos. |
| **Item 19**: Empty state pattern | Reusable `EmptyState` composable in `ui/components/common/` | Inline `if(empty)` per screen | Reusable composable (A) | DRY, consistencia visual, facil de testear con Compose UI tests. Aplica inicialmente a 3 tabs de DetallePacienteScreen. |
| **Item 20**: Tag chips display | Display-only chips in PacienteInfoHeader | Full CRUD tag management | Display-only (A) | Las etiquetas son datos existentes que nunca se muestran. CRUD completo es feature separado. |
| **Item 22**: Spinner fix | Timeout `LaunchedEffect` + error/retry state | Global `LoadingContent` wrapper composable | Scoped fix (A) | Solo DetallePacienteScreen tiene spinner infinito real (paciente nulo sin error). Otros spinners tienen manejo de error separado. |

## Data Flow — Item 15 (Sync Serialization)

```
┌──────────┐    toRemoto()     ┌────────────────┐    upsert    ┌───────────┐
│ Paciente │ ─────────────────→│ PacienteRemoto  │ ──────────→ │ Supabase  │
│ (Room)   │  Json.encodeToString │ .ultimasEtiquetas│            │ TEXT col  │
│ List<String>│ ←────────────── │ String (JSON array)│ ←──────── │           │
└──────────┘    toEntity()      └────────────────┘  download   └───────────┘
                Json.decodeFromString
                + CSV fallback
```

**Format change**: `"tag1,tag2"` (CSV actual) → `"[\"tag1\",\"tag2\"]"` (JSON array). Room (via Converters) ya usa JSON — solo la capa sync remota estaba desincronizada. El fallback CSV en `toEntity()` maneja filas existentes en Supabase con formato antiguo; se agrega `TODO` para remover tras migracion completa.

Mismo cambio en `SyncHistorialUseCase.kt` linea 97 (orphan patient upload).

## File Changes

| File | Action | Description |
|---|---|---|
| `ui/components/common/EmptyState.kt` | **Create** | Reusable composable: icono, titulo, subtitulo opcional, accion opcional |
| `domain/SyncPacientesUseCase.kt` | Modify | `toRemoto()`: `joinToString(",")` → `Json.encodeToString()`. `toEntity()`: `split(",")` → `Json.decodeFromString` + CSV fallback |
| `domain/SyncHistorialUseCase.kt` | Modify | Linea 97: `joinToString(",")` → `Json.encodeToString()` |
| `data/PacienteDao.kt` | Modify | Add `getMaxHistoriaNum(opticaId, year): Int?` SQL query |
| `data/PacienteRepository.kt` | Modify | `suggestNextHistoriaOptometrica`: replace loop with DAO call |
| `viewmodel/GastosViewModel.kt` | Modify | Add `sortedByDescending { it.fecha }` in flow collection or UiState derivation |
| `viewmodel/MonturasViewModel.kt` | Modify | Move `sortedFiltradas`/`porReponer` computation from composable to VM as derived state |
| `ui/screens/GastosScreen.kt` | Modify | Remove `sortedByDescending` from `items()` call |
| `ui/screens/MonturasScreen.kt` | Modify | Remove inline sort/filter logic, consume from VM |
| `ui/screens/DetallePacienteScreen.kt` | Modify | Add EmptyState in 3 tabs, replace infinite spinner with timeout+error+retry |
| `ui/components/paciente/PacienteInfoHeader.kt` | Modify | Add tag chip row via `FilterChip`/`SuggestionChip` when `ultimasEtiquetas` non-empty |

## Testing Strategy

| Item | Layer | Approach | Test File |
|---|---|---|---|
| 15 | Unit | `SyncPacientesUseCase` test: `toRemoto()` produce JSON array, `toEntity()` decode JSON + fallback CSV. Mock Supabase. | `SyncPacientesUseCaseTest.kt` (existing, extend) |
| 15 | Unit | `SyncHistorialUseCase` orphan upload: verify JSON format in built `PacienteRemoto`. | `SyncHistorialUseCaseTest.kt` (existing, extend) |
| 18 | DAO | Room in-memory: insert pacientes with `HO-2026-0001` through `HO-2026-0042`, verify MAX returns 42. | `PacienteRepositoryTest.kt` (extend) or new DAO test |
| 19 | UI | Compose test: render `EmptyState` with icon+title, verify content visible. | New `EmptyStateTest.kt` |
| 21 | Unit | `GastosViewModel`: verify emitted gastos are sorted by fecha descending. | `GastosViewModelTest.kt` (extend) |
| 21 | Unit | `MonturasViewModel`: verify derived state respects `sortBy` field. | `MonturasViewModelCatchRefactorTest.kt` (extend) |
| 22 | UI | Compose test: DetallePacienteScreen with null paciente → shows error state after timeout, retry triggers reload. | New `DetallePacienteScreenErrorTest.kt` or extend existing screen test |

Estrategia global: TDD estricto (`strict_tdd: true`). Cada item: write failing test → implement → verify green. Items 15/18/21 tienen tests unitarios existentes. Items 19/20/22 requieren nuevos tests Compose UI.

## Migration / Rollout

No data migration. Item 15 mantiene el tipo de columna Supabase como TEXT — solo cambia el formato del string. El fallback CSV en `toEntity()` garantiza compatibilidad hacia atras. No se requiere feature flag ni phased rollout. Rollback: `git revert`.

## Open Questions

None. All ambiguity resolved in exploration phase.
