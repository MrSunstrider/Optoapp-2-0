# Design: Fix Gastos Categoria Constraint Mismatch

## Technical Approach

Replace Android-side category string values (`"Local"`, `"Sueldos"`, etc.) with exact PostgreSQL CHECK constraint values (`"alquiler"`, `"personal"`, etc.) across the ViewModel and tests. No schema, migration, or type changes — `categoria` is a plain `String` in Room and the DB already enforces the correct constraint.

## Architecture Decisions

### Decision: Category Value Mapping

| DB CHECK Value | Old Android Value | Rationale |
|----------------|-------------------|-----------|
| `alquiler` | `Local` | Direct rename |
| `servicios` | `Servicios` | Direct rename |
| `personal` | `Sueldos`, `Planilla` | Single DB value covers both old labels. Tests using `"Planilla"` migrate to `"personal"` |
| `proveedores` | `Mantenimiento` | Direct rename |
| `insumos` | `Insumos` | Direct rename |
| `marketing` | `Marketing` | Direct rename |
| `impuestos` | `Impuestos` | Direct rename |
| `otro` | `Otro`, `Temporal` | Catch-all. Tests using `"Temporal"` migrate here |

Rationale: There is exactly one correct set of values — the DB CHECK constraint. The app **must** produce these values or sync fails. No mapping layer or type-safe enum is introduced because `categoria` is just a `String` in `GastoOperativoEntity` and the UI dropdown already reads from the `categorias` list. Adding an enum would be scope creep with no user-facing benefit.

### Decision: No Display-Name Layer

**Choice**: UI displays the lowercase DB values directly (`"alquiler"`, `"servicios"`).
**Alternatives considered**: Add a `displayName` map (`alquiler → "Alquiler/Alquiler"`, etc.)
**Rationale**: The current code uses the category string directly as both payload and label. A display name layer would be a new capability and is explicitly out of scope per the proposal. The user will see lowercase labels in the dropdown — acceptable given this aligns the app to the DB constraint. A future change can add `categoriaDisplayName` if UX demands it.

## Data Flow

No data flow change. The same path applies:
```
User selects category → ViewModel.categorias list → GastosUiState.categoria → GastoOperativoEntity.categoria → Room INSERT → UploadSyncCoordinator → Supabase INSERT (no more CHECK failure)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/GastosViewModel.kt` | Modify | Line 22: default `categoria` → `"alquiler"`. Line 97: `categorias` list → DB CHECK values |
| `optoapp/.../viewmodel/GastosRecurrentesTest.kt` | Modify | Replace `"Local"` → `"alquiler"`, `"Planilla"` → `"personal"`, `"Reparacion"` → `"servicios"` in test fixtures |
| `optoapp/.../data/OptoRepositoryFinanzasTest.kt` | Modify | Replace `"Alquiler"` → `"alquiler"` (lowercase), `"Servicios"` → `"servicios"`, `"Sueldos"` → `"personal"`, `"Marketing"` → `"marketing"`, `"Temporal"` → `"otro"`, `"Internet"` → `"servicios"` in test fixtures |

## Interfaces / Contracts

No new interfaces. `categoria` remains a `String` field. Contract change: the legal values for `GastosViewModel.categorias` and `GastosUiState.categoria` are now bound to the DB CHECK constraint set.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `GastosViewModel.categorias` contains correct values | Static assertion (list equality) |
| Unit | Category matching in `autoGenerarRecurrentes` | Existing tests updated to use DB CHECK values — verifying dedup logic still works |
| Integration | `OptoRepository` local write/read with valid categories | Test fixtures use DB CHECK values; assertions updated |
| E2E | Sync upload succeeds with valid category | Verified by existing test infrastructure (no new test needed — category string passes through unchanged) |

## Migration / Rollout

No migration required. Existing records in Room with old category values will fail sync upload (same as before). Users must edit those records to select a valid category for them to sync.

## Open Questions

None. Mapping is fully determined by the DB CHECK constraint.
