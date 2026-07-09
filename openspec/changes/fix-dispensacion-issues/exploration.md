## Exploration: fix-dispensacion-issues

### Bug 1: Montura stock not found from dispensations
**Root cause**: In `LenteForm.kt` (lines 164-171) and `DispensacionFormSections.MonturaInfoSection` (lines 56-68), the frame search dropdown filters by `monturasActivas.filter { it.stockActual > 0 }`. Monturas with `stockActual = 0` are excluded from the dropdown entirely. When a user adds a montura via the inventory screen, `MonturasViewModel.save()` line 165 does `form.stockActual.toIntOrNull() ?: 0` — if the user leaves the "Stock inicial" field blank, stock defaults to 0. The montura is saved but invisible in the dispensation dropdown.

Additionally, the `DispensacionViewModel` `init` block (lines 101-108) uses a nested `collect` pattern from `sessionManager.opticaId` → `repository.getMonturasByOptica()`. Room Flow re-emits on table changes, so newly added monturas should appear. But the `stockActual > 0` filter is the gate that prevents zero-stock items from being selectable.

**Files involved**:
- `optoapp/src/main/java/com/example/optoapp/ui/components/dispensacion/LenteForm.kt` lines 164-171 — filter `it.stockActual > 0`
- `optoapp/src/main/java/com/example/optoapp/ui/screens/DispensacionFormSections.kt` lines 56-68 — same filter in deprecated MonturaInfoSection
- `optoapp/src/main/java/com/example/optoapp/viewmodel/DispensacionViewModel.kt` lines 101-108 — monturasActivas population
- `optoapp/src/main/java/com/example/optoapp/viewmodel/MonturasViewModel.kt` line 165 — stock defaults to `0` when field is blank

**Fix approach**: 
1. Remove the `stockActual > 0` filter from the dropdown or change it to `stockActual > 0 || hasBeenSelected` so zero-stock items can still be found/searchable
2. Or keep the filter but show a clear message when no results match (inform user to check stock)
3. At minimum, remove the `if (isSelected) emptyList()` branch (lines 156-158) which hides the list once an item is selected, making re-selection impossible without clearing the query

### Bug 2: App crash on monto total
**Root cause**: Not conclusively determined from static analysis alone — crash logs are needed. However, looking at the code patterns, the most suspicious area is:

1. `DispensacionUiState.generatedId` uses `UUID.randomUUID().toString()` as a default value (line 55). If the `DispensacionUiState` is recreated (e.g., during recomposition) or if the state copy somehow loses it, the `generatedId` used for navigation to `InformacionFinancieraScreen` would differ from the ID used inside the transaction (`finalId` in saveDispensacion), causing navigation to a non-existent dispensacion.

2. The `montoTotal` is a `String` field (line 44) converted to Double via `.toDoubleOrNull()` on save (line 288). All parsing uses safe methods — no NumberFormatException path found. However, the Decimal keyboard on some locale configurations could produce comma-separated values that silently parse to null (not a crash, but confusing UX).

3. The deprecated `FinancieraInfoSection` in `DispensacionFormSections.kt` is not referenced from any current screen — confirmed dead code.

**Files involved**:
- `optoapp/src/main/java/com/example/optoapp/ui/screens/NuevaDispensacionScreen.kt` lines 153-159 — montoTotal TextField
- `optoapp/src/main/java/com/example/optoapp/viewmodel/DispensacionViewModel.kt` lines 44, 162, 288 — montoTotal as String with toString/toDoubleOrNull
- `optoapp/src/main/java/com/example/optoapp/ui/screens/InformacionFinancieraScreen.kt` lines 95-99 — montoTotal TextField (second screen)
- `optoapp/src/main/java/com/example/optoapp/ui/screens/DispensacionFormSections.kt` lines 153-158 — deprecated FinancieraInfoSection (dead code)

**Fix approach**:
1. Persist `generatedId` as a stable ID (not `UUID.randomUUID()` default in constructor) to survive any state recreation
2. Add decimal-comma replacement (`value.replace(",", ".")`) in the onValueChange handler to handle Spanish locale keyboards gracefully
3. Collect crash logs (Firebase Crashlytics or user-provided stack trace) for exact crash point
4. Consider moving `montoTotal` to a `Double` backing field with a formatter in the UI layer

### Bug 3: Duplicate key on montura_movimientos sync
**Root cause**: The `uploadMovimientos` method in `SyncInventarioUseCase.kt` (lines 115-141) uploads movements but has two gaps:

1. **No conflict detection before upload**: Unlike `uploadMonturas` (line 84) which uses `conflictHelper.filterConflicts()`, `uploadMovimientos` does NOT call any conflict detection before pushing to Supabase. The `ConflictHelper.detectConflictMovimientos()` method exists (lines 73-95) but is never invoked.

2. **Deduplication by `id` instead of composite key**: `distinctBy { it.id }` deduplicates by primary key, but the Supabase unique constraint `idx_movimientos_conflict` is on `(referencia_id, tipo, montura_id)`. When a dispensation is edited, Room's `ON CONFLICT REPLACE` replaces old movements with new UUIDs. The upload sends new IDs with the same composite key. Supabase PK-based upsert doesn't catch this — only the unique index does, raising error 23505.

3. **No `onConflict` parameter on upsert**: `supabase.postgrest[TABLE_MOVIMIENTOS].upsert(chunk)` uses default PK conflict resolution. There's no `onConflict` parameter specifying the composite index, so Supabase tries INSERT → hits the unique index violation.

**Files involved**:
- `optoapp/src/main/java/com/example/optoapp/domain/SyncInventarioUseCase.kt` lines 115-141 — upload without conflict detection and with wrong dedup
- `optoapp/src/main/java/com/example/optoapp/domain/sync/ConflictHelper.kt` lines 73-95 — `detectConflictMovimientos` exists but unused in upload
- `supabase/migrations/20260617020000_inventario_v2.sql` lines 17-18 — `idx_movimientos_conflict` unique index
- `optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt` lines 199-206 — Room entity with same unique index
- `optoapp/src/main/java/com/example/optoapp/util/DispensacionStockHelper.kt` lines 68-113 — creates movements with UUID-based IDs
- `optoapp/src/main/java/com/example/optoapp/viewmodel/DispensacionViewModel.kt` lines 344-363 — creates stock movements on dispensation save

**Fix approach**:
Several options, ordered from simplest to most robust:

1. **Use `onConflict` on the upsert** (simplest): Specify `onConflict = "referencia_id,tipo,montura_id"` in the upsert call so Supabase uses the composite index for conflict resolution instead of the primary key.

2. **Add conflict dedup pre-upload**: Change `distinctBy { it.id }` to `distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }` to deduplicate by composite key before upload. Also apply `ConflictHelper.detectConflictMovimientos()` similar to how `uploadMonturas` handles conflicts.

3. **Reconcile IDs**: Before upload, match local movements by composite key against a remote lookup and adopt existing remote IDs so PK-based upsert succeeds.

### Risks
- **Bug 1**: Removing the `stockActual > 0` filter could let users select zero-stock frames, which would fail at save time with "Stock insuficiente". The UX should guide the user to check stock levels.
- **Bug 2**: Without crash logs, fix is speculative. Deploy crash reporting first.
- **Bug 3**: Changing upsert conflict behavior could cause unintended data loss if resolved incorrectly. Test with a replica of production data.

### Ready for Proposal
Yes
