# Proposal: Add CRUD (Create/Delete) to Cost Matrix

## Intent

Users managing the cost matrix in "Matriz de Costos" (Tab 1 of CostosYGastosScreen) can only edit existing entries. Adding new cost rules or removing outdated ones requires direct database access. This gap blocks normal operations when costs change by region or over time.

## Scope

### In Scope
1. **Create**: AlertDialog form to add new `CostoProductoEntity` entries — user selects block via dropdown, then fills material/tipo_lente/tratamiento/serie/costo_unitario, with optional laboratorio_id
2. **Delete**: Delete button per row + confirmation dialog — soft-delete via `vigente_hasta = today`
3. **DAO**: Add soft-delete query on `CostoProductoDao` (set `vigente_hasta`)
4. **ViewModel**: `createCosto()` and `deleteCosto()` methods matching existing `saveGasto()`/`deleteGasto()` pattern
5. **Sync**: No changes — soft-delete propagates via existing upsert sync (`vigente_hasta` is already mapped in `toRemoto()`)
6. **Spec delta**: Add R9 (Create) and R10 (Delete) to `openspec/specs/costos-productos/spec.md`

### Out of Scope
- Delete for biselado costs (`CostoBiseladoEntity`) — same patterns apply, deferred
- Hard-delete / physical row removal — soft-delete via `vigente_hasta` is the established pattern
- Robolectric tests — follow project's standard testing approach

## Capabilities

### New Capabilities
None — all changes modify the existing `costos-productos` capability.

### Modified Capabilities
- `costos-productos`: Add Create requirement (R9) and Delete requirement (R10) to the existing spec

## Approach

Follow the exact UX pattern already implemented in Tab 2 ("Gastos Operativos") of the same screen:

- **Create**: FAB in the cost matrix tab → AlertDialog form with fields matching `CostoProductoEntity` constructor. Block is auto-filled from dropdown selection. On save, create entity with `UUID.randomUUID().toString()`, `opticaId` from session, `vigenteDesde = today`, call `costoProductoDao.upsertAll(listOf(newCosto))`, then refresh block.
- **Delete**: Delete icon button on each `CostoProductoRow` (same as `GastoOperativoCard`). Confirmation dialog. On confirm, set `vigenteHasta = today` and upsert.
- **DAO**: Add `@Query("UPDATE costos_productos SET vigente_hasta = :hasta WHERE id = :id") suspend fun softDelete(id: String, hasta: String)` — no sync state tracking needed since upsert propagates the timestamp.
- **ViewModel**: `showNewCosto()`, `dismissCostoDialog()`, form field updaters, `saveNewCosto()`, `confirmDeleteCosto()`, `dismissDeleteDialog()` — all matching the gastos operativos CRUD pattern.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `CostoProductoDao.kt` | Modified | Add `softDelete()` query |
| `CostosYGastosViewModel.kt` | Modified | Add create/delete methods + UI state fields |
| `CostosYGastosScreen.kt` | Modified | FAB, create dialog, delete button, delete confirmation |
| `openspec/specs/costos-productos/spec.md` | Modified | Add R9, R10 requirements |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Soft-delete collision with active lookups | Low | All queries already filter `vigente_hasta IS NULL` — no impact on active lookups |
| User edit of deleted row in edit dialog | Low | Dismiss dialog before delete; row removed from flow after refresh |

## Rollback Plan

Revert: `git revert` the merge commit. Since no schema or sync changes, rollback is zero-downtime.

## Dependencies

None.

## Success Criteria

- [ ] User can open a create dialog from the cost matrix tab
- [ ] New cost entries appear in the matrix after saving
- [ ] New cost entries sync to Supabase via existing upsert flow
- [ ] User can delete a cost entry with confirmation
- [ ] Deleted entries have `vigente_hasta` set locally and propagate to Supabase
- [ ] Deleted entries no longer appear in the matrix
