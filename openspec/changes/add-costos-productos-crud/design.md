# Design: Add Create/Delete to Cost Matrix

## Technical Approach

Add FAB-driven create dialog and row-level delete to the Matriz de Costos tab (Tab 0), mirroring the Gastos Operativos (Tab 1) CRUD pattern. Both operations reuse `costoProductoDao.upsertAll()` — create inserts a new row, delete sets `vigente_hasta = today`. All existing queries already filter `vigente_hasta IS NULL`, so deleted rows vanish from the grid automatically. No DAO, entity, or sync changes needed.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Delete mechanism | Hard-delete (DELETE FROM) | Soft-delete (vigente_hasta) | **B** | `toRemoto()` already maps vigenteHasta; upsert propagates it to Supabase. Existing queries filter `vigente_hasta IS NULL`. Audit trail preserved. |
| Create mutation | New `@Insert` DAO method | Reuse `upsertAll()` | **B** | Single code path for all mutations. The update dialog already uses upsertAll. Simpler. |
| Delete mutation | New `@Query` soft-delete DAO method | Reuse `upsertAll()` | **B** | Copy entity with new vigenteHasta, call upsertAll. Same pattern as saveCostoEdit(). |
| UI pattern | Custom layout | FAB + AlertDialog matching Tab 2 | **B** | Tab 1 (Gastos Operativos) already has FAB + form dialog. Consistent UX expectations. |
| Delete trigger on row | Swipe-to-dismiss | Icon button per row | **B** | Tab 1 uses icon buttons; `CostoProductoRow` already has click handling. Add delete icon alongside. |

## Data Flow

```
User taps FAB (+) ──→ showNewCosto() sets state
                       └─→ AlertDialog renders (dropdowns + text fields)
User fills fields ──→ ViewModel state updaters
User taps Guardar ──→ saveCosto()
                       ├─→ CostoProductoEntity(id=UUID, vigenteDesde=today, vigenteHasta=null)
                       ├─→ costoProductoDao.upsertAll(listOf(entity))
                       ├─→ loadBlock() refresh
                       └─→ postSaveSyncScheduler.scheduleFinanzasSync(opticaId)

User taps delete icon ──→ confirmDeleteCosto() sets state
                           └─→ AlertDialog confirmation renders
User taps Confirmar ──→ deleteCosto()
                          ├─→ entity.copy(vigenteHasta = today)
                          ├─→ costoProductoDao.upsertAll(listOf(updated))
                          ├─→ loadBlock() refresh
                          └─→ postSaveSyncScheduler.scheduleFinanzasSync(opticaId)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../ui/screens/CostosYGastosScreen.kt` | Modify | Add FAB in Tab 0, create dialog (5 dropdowns + 2 text fields), delete icon per `CostoProductoRow`, delete confirmation dialog |
| `optoapp/.../viewmodel/CostosYGastosViewModel.kt` | Modify | Add UI state fields (`isCostoDialogVisible`, create form fields, `deletingCosto`), form updaters, `showNewCosto()`, `dismissCostoDialog()`, `saveCosto()`, `confirmDeleteCosto()`, `dismissDeleteDialog()`, `deleteCosto()` |

## UI State Additions

```kotlin
// New fields in CostosYGastosUiState:
val isCostoDialogVisible: Boolean = false,
val creatingCosto: Boolean = false,   // true = create mode, false = edit mode
val costoMaterial: String = "",
val costoTipoLente: String = "",
val costoStockOFabricacion: String = "",
val costoTratamiento: String = "",
val costoSerie: String = "",
val costoCostoUnitario: String = "",
val costoSaveError: String? = null,
val deletingCosto: CostoProductoEntity? = null,  // row pending delete confirmation
```

## Create Dialog Fields

| Field | Type | Required | Widget |
|-------|------|----------|--------|
| Material | String | Yes | ExposedDropdownMenu (CR39, Policarbonato, Trivex, Alto Indice, ...) |
| Tipo de lente | String | Yes | ExposedDropdownMenu (Monofocal, Bifocal, Progresivo, Ocupacional) |
| Stock o fabricación | String | Yes | Auto-filled from selected block |
| Tratamiento | String? | No | ExposedDropdownMenu (null, Antireflex, Blue Block, Fotocromático) |
| Serie | Int? | No | OutlinedTextField (KeyboardType.Number) |
| Costo unitario | Double | Yes | OutlinedTextField (KeyboardType.Decimal) |
| Laboratorio ID | String? | No | Not in dialog — deferred (future: lab dropdown) |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `saveCosto()` creates valid entity, sets UUID + opticaId + vigenteDesde | ViewModel test with fake DAO |
| Unit | `deleteCosto()` copies entity with vigenteHasta=today, calls upsertAll | ViewModel test with fake DAO |
| Unit | Validation rejects empty material, tipo_lente, costo_unitario ≤ 0 | ViewModel test |
| Integration | Row disappears after soft-delete (Dao `getByBloque` filters vigente_hasta IS NULL) | Room in-memory DB test |

## Migration / Rollout

No migration required — no schema changes, no data migration. Rollback via `git revert`.

## Open Questions

None.
