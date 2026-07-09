# Design: fix-dispensacion-issues

## Technical Approach

Three independent bug fixes in the Android app, each self-contained to its module. No Supabase schema/RLS changes, no data migration. All changes follow existing patterns: ViewModel+StateFlow, Room DAOs, ReorderList/SyncReconcile markers.

## Bug 1 — Remove stock filter from montura dropdown

| File | Old Code | New Code |
|------|----------|----------|
| `LenteForm.kt:164-171` | `val filteredMonturas = if (isSelected) emptyList() else if (monturaQuery.isBlank()) monturasActivas.filter { it.stockActual > 0 } else monturasActivas.filter { it.stockActual > 0 }.filter { ... }` | `val filteredMonturas = if (monturaQuery.isBlank()) monturasActivas else monturasActivas.filter { it.marca.contains(monturaQuery, ignoreCase = true) \|\| it.modelo.contains(monturaQuery, ignoreCase = true) \|\| it.sku.contains(monturaQuery, ignoreCase = true) }` |
| `DispensacionFormSections.kt:56-68` | Same pattern with `isSelected` → `emptyList()` + `.filter { it.stockActual > 0 }` | Same transformation — remove `isSelected` guard, remove stock filter |

**Rationale**: Save-time validation via `checkMonturaStock` already catches insufficient stock. The dropdown should show all active monturas so users can search and select any frame. The `if (isSelected) emptyList()` branch hid the list after selection, preventing re-selection without clearing the query.

**Edge case**: User selects a zero-stock montura → save fails with "Stock insuficiente" (existing `adjustStockAndRegistrarMovimiento` returns `Result.failure`). Same UX as before for other validation errors.

## Bug 2 — Stabilize generatedId and prevent crash

| File | Old Code | New Code |
|------|----------|----------|
| `DispensacionViewModel.kt:55` | `val generatedId: String = UUID.randomUUID().toString()` | `val generatedId: String = ""` |
| `DispensacionViewModel.kt:101-109` (init) | Initializes `monturasActivas` and `ultimaEvaluacionTicket` only | Add: `_uiState.update { it.copy(generatedId = UUID.randomUUID().toString()) }` |
| `DispensacionViewModel.kt:288` | `val montoTotal = s.montoTotal.toDoubleOrNull()` | `val montoTotal = s.montoTotal.replace(",", ".").toDoubleOrNull()` |
| `DispensacionViewModel.kt:440` | `catch (e: RuntimeException)` | `catch (e: Exception)` |

**Rationale**: `UUID.randomUUID()` in a data class default regenerates on every `copy()` if not explicitly passed, making `generatedId` unstable. Moving initialization to ViewModel `init` guarantees a single assignment. The `montoTotal` comma replacement handles Spanish locale keyboards (`,` as decimal separator). Widening the catch block prevents UI crashes from unexpected exceptions during save.

**Edge cases**:
- Edit mode: `loadDispensacion()` already sets `generatedId = dispensacionId` at line 134 → overrides the init value. Correct.
- Rapid save: `s.generatedId` at line 304 is captured from the state snapshot, not re-read from flow. Stable.

## Bug 3 — Fix montura_movimientos dedup for composite key

### Data Flow

```
Room (local)                          Supabase (remote)
┌────────────────┐                   ┌────────────────────┐
│ MonturaMovimiento                  │ montura_movimientos │
│ id=UUID-new     │                   │ id=UUID-old         │
│ referenciaId=X  │                   │ referenciaId=X      │
│ tipo=SALIDA_VENTA│  ── upload ──→  │ tipo=SALIDA_VENTA   │
│ monturaId=M     │                   │ monturaId=M         │
└────────────────┘                   └────────────────────┘
                                           │
                              idx_movimientos_conflict UNIQUE
                              ON (referencia_id, tipo, montura_id)
                                           │
                              Error 23505: duplicate key
```

Old dedup `distinctBy { it.id }` is PK-based — permits duplicate composite keys in the same batch. Supabase's PK-based upsert doesn't match `idx_movimientos_conflict`, so duplicates hit error 23505.

### Changes

| File | Old Code | New Code |
|------|----------|----------|
| `SyncInventarioUseCase.kt:118` | `.distinctBy { it.id }` | `.distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }` |
| `SyncInventarioUseCase.kt:118-141` | No conflict detection before upload | Add `conflictHelper.filterConflictMovimientos()` call + ID reconciliation before upsert |

### New uploadMovimientos Flow

```
1. Fetch local movimientos from Room
2. Dedup by Triple(referenciaId, tipo, monturaId)
3. Call conflictHelper.filterConflictMovimientos(opticaId, localEntities)
   → returns safeIds (movements matching remote, or only-local)
4. Fetch remote movimientos by opticaId
5. For each safe movement with a composite key match in remote:
     → adopt remote.id locally (reconciliate via repository.upsertMonturaMovimiento)
6. Filter to movements that are NEW (no remote match) only
7. Upsert filtered batch to Supabase
8. Download fresh state from Supabase
```

**Key insight**: Movements where local stockNuevo == remote stockNuevo are already consistent — no upload needed, but local ID must adopt remote ID to prevent future mismatch. Only true "new" movements (composite key not in remote) are uploaded.

### Reconciliation helper function (new private method)

```kotlin
private suspend fun reconcileMovimientoIds(
    opticaId: String,
    safeIds: Set<String>,
    remoteByKey: Map<Triple<String, String, String>, MonturaMovimientoRemoto>
) {
    // Adopt remote IDs for local movements that match existing composite keys
    val toReconcile = repository.getMovimientosMonturaSnapshotForOptica(opticaId)
        .filter { it.id in safeIds }
    for (local in toReconcile) {
        val remote = remoteByKey[Triple(local.referenciaId, local.tipo, local.monturaId)]
        if (remote != null && remote.id != local.id) {
            repository.upsertMonturaMovimiento(local.copy(id = remote.id))
        }
    }
}
```

**Edge cases**:
- Multiple locals with same composite key → dedup by `distinctBy` before processing
- Remote has different stockNuevo → filtered as conflicted, not uploaded, ID not reconciled
- Network failure during reconcile → partial reconcile, next sync cycle retries

## Testing Strategy

| Bug | Layer | Approach |
|-----|-------|----------|
| 1 | UI unit (Robolectric) | Verify dropdown shows zero-stock monturas; verify save validation still rejects insufficient stock |
| 2 | ViewModel unit | Verify `generatedId` is stable across `copy()`; verify `montoTotal` with `","` separator parses correctly; verify exception in save doesn't crash |
| 3 | UseCase unit (Robolectric) | Verify dedup by composite key; verify reconciliation adopts remote IDs; verify error 23505 no longer occurs |
| 3 | Integration | End-to-end sync test with pre-existing remote movements matching composite key |

## Verification Steps

1. Build: `./gradlew :optoapp:assembleDebug`
2. Unit tests: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
3. Manual (Bug 1): Create montura with `stockActual=0` → open dispensation form → searchable in dropdown
4. Manual (Bug 2): Type "1500,50" in monto-total → save → converts to 1500.50
5. Manual (Bug 3): Edit a dispensation with existing movements → sync → no error 23505 in logs

## Open Questions

None. All three bugs have precise root causes and isolated fixes.
