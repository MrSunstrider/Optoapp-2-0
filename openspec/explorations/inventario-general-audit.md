# Exploration: Inventario General — Full Architecture + UX Audit

## Current State

### 1. File Inventory

**Screens & UI:**

| File | Role |
|---|---|
| `ui/screens/MonturasScreen.kt` | Main "Inventario General" screen — search, stock alerts, summary KPIs, list |
| `ui/components/monturas/MonturaList.kt` | `MonturaListSection` (LazyColumn) + `MonturaItem` (ElevatedCard per row) |
| `ui/components/monturas/MonturaForm.kt` | `MonturaEditForm` — AlertDialog content for add/edit |
| `ui/screens/inventariofisico/InventarioFisicoScreen.kt` | Physical inventory count screen (separate flow) |
| `ui/screens/inventariofisico/MonturaScanScreen.kt` | Barcode scanning for physical inventory |
| `ui/components/dispensacion/MonturaForm.kt` | Different form used in dispensacion flow (not inventory mgmt) |

**ViewModel:**

| File | Role |
|---|---|
| `viewmodel/MonturasViewModel.kt` | Single ViewModel for the entire Inventario General screen |
| `viewmodel/MonturaDashboardKpiViewModel.kt` | Dashboard KPIs (separate screen) |
| `viewmodel/InventarioFisicoViewModel.kt` | Physical inventory count VM |

**Data Layer:**

| File | Role |
|---|---|
| `data/dispensacion/DispensacionEntity.kt` | **Contains both `Montura` and `MonturaMovimiento` entities** (lines 162-236) |
| `data/montura/MonturaDao.kt` | Room DAO for monturas — CRUD, search, stock adjust, low-stock queries |
| `data/montura/MonturaMovimientoDao.kt` | Room DAO for movimiento tracking — by optica, by montura, by date |
| `data/montura/MonturaInventoryCoordinator.kt` | Orchestrator — wraps DAOs, adds `updatedAt` timestamps, schedules sync |
| `data/OptoRepository.kt` | God repository — delegates montura calls to `MonturaInventoryCoordinator` |
| `data/proveedor/MonturaProveedorDao.kt` | Provider/Supplier cross-reference |
| `data/proveedor/CategoriaMonturaDao.kt` | Category reference data |
| `data/montura/MonturaDashboardKpiRepository.kt` | Aggregated KPI queries |

**Domain Layer:**

| File | Role |
|---|---|
| `domain/SyncInventarioUseCase.kt` | Upload/download monturas + movimientos to/from Supabase |
| `domain/SyncInventarioFisicoUseCase.kt` | Sync physical inventory counts |

**Utilities:**

| File | Role |
|---|---|
| `util/InventarioMonturasPdfGenerator.kt` | Generates PDF inventory report |
| `util/DispensacionStockHelper.kt` | Helper used by dispensacion flow to decrement stock |

**Navigation:**

| File | Role |
|---|---|
| `ui/screens/DrawerSections.kt` | Drawer item "Inventario" → navigates to `"monturas"` route |
| `ui/screens/MainDrawerScreen.kt` | NavHost route `composable("monturas") { MonturasScreen(navController) }` |

---

### 2. Entity Model — `Montura`

Defined in `data/dispensacion/DispensacionEntity.kt` (line 162):

```kotlin
@Entity(tableName = "monturas")
data class Montura(
    @PrimaryKey val id: String,
    val sku: String,           // Unique per optica
    val marca: String,
    val modelo: String,
    val color: String,
    val talla: String,
    val costo: Double,
    val precio: Double,
    val stockActual: Int,      // ← Denormalized, updated via SQL
    val stockMinimo: Int,      // Alert threshold
    val activo: Boolean,
    val tipoAro: String,
    val materialMontura: String,
    val anchoMm: Double?,
    val puenteMm: Double?,
    val alturaMm: Double?,
    val imagenUri: String?,
    val categoria: String,
    val coleccion: String,
    val temporada: String,
    val estadoComercial: String,
    val genero: String,
    val opticaId: String,      // Multi-tenant key
    val updatedAt: String?,
    val updatedBy: String?
)
```

**Stock is denormalized** — `stockActual` is a direct field on `Montura`, updated atomically via:
```sql
UPDATE monturas SET stockActual = stockActual + :delta WHERE id = :id AND opticaId = :oid AND (stockActual + :delta) >= 0
```

### 3. Entity Model — `MonturaMovimiento`

```kotlin
@Entity(tableName = "montura_movimientos")
data class MonturaMovimiento(
    @PrimaryKey val id: String,
    val monturaId: String,     // FK → Montura
    val fecha: LocalDate,
    val tipo: String,          // "ENTRADA", "SALIDA_VENTA", "AJUSTE"
    val cantidad: Int,
    val stockPrevio: Int,
    val stockNuevo: Int,
    val referenciaId: String,  // Links to OT, order, etc.
    val nota: String,
    val opticaId: String,
    val userId: String,
    val costoUnitario: Double,
    val tipoDocumento: String
)
```

Movements are historical/audit trail only — **stock is NOT calculated from movements**, it's directly stored on `Montura`. However, `syncStockFromMovimientos()` can rebuild stock from movements as a reconciliation mechanism.

---

### 4. Data Flow

```
User taps "+" (Entrada)
  → MonturaItem.onEntrada()
    → MonturasScreen calls viewModel.registrarEntrada(montura, 1)
      → MonturasViewModel:
          1. repository.adjustMonturaStock(id, opticaId, +1)  ← SQL UPDATE
          2. repository.insertMonturaMovimiento(...)           ← INSERT movimiento
          3. postSaveSyncScheduler.scheduleInventarioSync()    ← Triggers cloud sync
```

```
User taps edit icon
  → MonturaItem.onEdit()
    → viewModel.startEdit(montura)  ← Populates MonturaFormState
    → AlertDialog with MonturaEditForm appears
    → User edits, taps "Guardar"
      → viewModel.save():
          1. Validates fields (SKU, marca, modelo, tipoAro, materialMontura)
          2. Builds Montura object
          3. Checks if exists → updateMontura() or insertMontura()
          4. Closes dialog, shows success message
```

```
User taps delete icon
  → Confirm dialog → viewModel.delete(montura)
    → repository.deleteMontura(montura)
```

---

### 5. Scroll Bug — Root Cause Analysis

**File: `MonturasScreen.kt`, lines 123-173**

```kotlin
Column(                                    // ← NO verticalScroll()
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = ..., vertical = ...),
    verticalArrangement = Arrangement.spacedBy(...)
) {
    OptoTextField(...)                     // Search bar
    StockAlertCard(...)                     // ~120-200dp (variable)
    SummaryCard(...)                        // ~200dp
    Text(error/success)                     // conditional, ~30-60dp
    MonturaListSection(...)                 // ← LazyColumn INSIDE Column
}
```

**Root cause: Nested `LazyColumn` inside non-scrollable `Column` without `Modifier.weight(1f)`.**

The `MonturaListSection` returns a `LazyColumn` directly. When a `LazyColumn` is a direct child of a `Column`:
1. The `Column` measures all non-weight children first (search, alert card, summary card, error text) with their intrinsic height
2. Without `Modifier.weight(1f)`, the `LazyColumn` gets **measured with the full remaining height constraint**, which IS correct
3. BUT — the `LazyColumn` has no `Modifier.weight(1f)` so it expands to fill the remaining space

**The actual bug might be more subtle** — there's a well-known Compose issue where `LazyColumn` inside `Column` causes measurement problems because `Column` can't properly constrain the `LazyColumn`'s height without explicit `weight()`. On some devices/configurations, this results in the `LazyColumn` items not rendering or the list not scrolling beyond its initial visible items.

**The proper fix** is to restructure so the entire content lives inside a SINGLE `LazyColumn` using `item {}` for static content:

```kotlin
LazyColumn(...) {
    item { OptoTextField(...) }
    item { StockAlertCard(...) }
    item { SummaryCard(...) }
    item { if (error) Text(...) }
    // low-stock items
    if (porReponer.isNotEmpty()) {
        item { Text("Por reponer") }
        items(porReponer) { MonturaItem(...) }
        item { Text("Todos los productos") }
    }
    items(restantes) { MonturaItem(...) }
}
```

This approach:
- Eliminates the nested scrolling surface
- The ENTIRE screen scrolls as one unit (search + cards + list)
- Users can always reach every item
- Uses Compose's lazy layout correctly

---

### 6. Missing "-" Button — Root Cause

**File: `MonturaList.kt`, line 88**

```kotlin
Row {
    IconButton(onClick = onEntrada) { Icon(Icons.Default.Add, ...) }  // Only "+"
    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, ...) }
    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, ...) }
}
```

There is ONLY a `+1` (Entrada) button. There is NO `-1` (Salida) button.

**Why:**

1. **ViewModel gap**: `MonturasViewModel` has `registrarEntrada()` but NO `registrarSalida()` method.
2. **UI gap**: `MonturaItem` doesn't pass `onSalida` callback, and `MonturaListSection` doesn't provide one.
3. **Infrastructure exists**: `MonturaDao.adjustStock()` accepts negative deltas. `MonturaInventoryCoordinator.registrarSalida()` exists and is used by dispensacion flow. `DispensacionStockHelper.adjustStockAndRegistrarMovimiento()` handles the full flow.

The ViewModel would need:

```kotlin
fun registrarSalida(montura: Montura, cantidad: Int = 1) {
    viewModelScope.launch {
        val opticaId = sessionManager.opticaId.first()
        val updated = repository.adjustMonturaStock(montura.id, opticaId, -cantidad)
        if (updated > 0) {
            repository.insertMonturaMovimiento(
                MonturaMovimiento(
                    id = UUID.randomUUID().toString(),
                    monturaId = montura.id,
                    tipo = "SALIDA",
                    cantidad = cantidad,
                    stockPrevio = montura.stockActual,
                    stockNuevo = montura.stockActual - cantidad,
                    referenciaId = "",
                    nota = "Salida manual desde inventario",
                    opticaId = opticaId
                )
            )
        }
    }
}
```

And the UI would add in `MonturaItem`:
```kotlin
IconButton(onClick = onSalida) { Icon(Icons.Default.Remove, ...) }
```

---

### 7. Add/Edit Flow

Defined in `MonturasScreen.kt` lines 88-102:

```
"+" (top bar icon) → viewModel.startCreate() → AlertDialog appears
  → MonturaEditForm rendered inside dialog
  → Fields: SKU*, Marca*, Modelo*, Color, Talla, TipoAro*, Material*,
      Costo, Precio, Stock inicial, Stock mínimo,
      Ancho/Puente/Altura mm, Imagen URI,
      Categoría, Colección, Temporada, Estado comercial, Género,
      Costo proveedor, Precio sugerido
  → "Guardar" → viewModel.save() → validate → upsert → close dialog

Edit icon (per-row) → viewModel.startEdit(m) → same dialog pre-filled
```

**Key observation**: This is a VERY long form for a dialog — ~20 fields crammed into an `AlertDialog`. On phones this must be painful to scroll through, and there's no way to collapse sections.

---

### 8. Offline Sync

Sync uses the standard app pattern: `PostSaveSyncScheduler` → `SyncInventarioUseCase`:
1. After every insert/update/delete/adjustment, `scheduleInventarioSync()` is called
2. `SyncInventarioUseCase` uploads local changes to Supabase, then downloads remote changes
3. Conflict resolution via `ConflictHelper` + `ConflictDao`
4. `syncStockFromMovimientos()` can rebuild `stockActual` from the movement trail

Sync order respects dependencies: monturas have no FK dependencies, so they upload first.

---

## UX/UI Analysis

### Pain Points (Prioritized)

#### 🔴 CRITICAL

**P1. Screen does not scroll properly**
- `<vulnerability>` The `Column` wrapping the entire screen has no `.verticalScroll()`, and the `LazyColumn` inside has no `.weight(1f)`. On smaller screens or with many products, search + alert card + summary card consume most of the visible area, leaving the list barely visible.
- **Fix**: Restructure to a single `LazyColumn` with `item{}` for static content.

**P2. No way to decrement stock**
- Users can accidentally tap "+1" and have no way to undo it from the inventory screen.
- **Fix**: Add a "-1" button + `registrarSalida()` in ViewModel.
- **Alternative fix**: Add "Ajuste de stock" dialog when tapping the stock number directly.

**P3. Edit form inside AlertDialog is too long**
- ~20 fields in a dialog on a phone screen = terrible UX. Users must scroll within the dialog, and the dialog height is constrained.
- **Fix**: Move to a full-screen composable route (like other screens do), or at minimum make the dialog full-screen on phones.

#### 🟡 HIGH

**P4. No stock movement history visible**
- Movements are tracked (`MonturaMovimiento` table) but NOT displayed anywhere in the Inventario General screen.
- Users can't see WHO added stock, WHEN, or WHY.
- **Fix**: Show a movement history section in the edit dialog or as a detail screen.

**P5. No confirmation on "+1"**
- One tap = immediate stock increment with no undo mechanism. The success toast helps but is transient.
- **Fix**: Add a brief Snackbar with "Undo" action (calls `registrarSalida`).

**P6. Filters exist in ViewModel but are NOT exposed in UI**
- `MonturasUiState` has `filterMarca`, `filterMaterial`, `filterCategoria`, `filterPrecioMin/Max`, `filterStockBajo` — all with corresponding ViewModel methods (`setFilterMarca`, `toggleFilterStockBajo`, etc.)
- **But MonturasScreen.kt does NOT use any of these**. The only filter is the search text field.
- **Fix**: Add filter chips/buttons below the search bar.

**P7. Stock alert card takes too much space**
- The `StockAlertCard` and `SummaryCard` are both `ElevatedCard`s with full width, padding, and multiple rows. Together they take ~250-350dp of vertical space before the list even starts.
- **Fix**: Merge into a single compact header, or make collapsible.

#### 🔵 MEDIUM

**P8. Cannot filter by "low stock only"**
- The `StockAlertCard` shows low-stock items separately AND in the main list, but there's no toggle to see ONLY low-stock items.
- **Fix**: Add a filter chip for stock bajo (ViewModel already has `toggleFilterStockBajo()`).

**P9. Edit form sections not collapsible**
- The "Catálogo extendido" and "Proveedor" sections have no visual grouping or collapse.
- **Fix**: Use `CollapsibleCard` or section headers.

**P10. No column sorting (by stock, precio, marca)**
- Items sort only by `marca ASC, modelo ASC`.
- **Fix**: Allow tapping column headers or add a sort dropdown.

**P11. Stock value shown but no margin %**
- The summary shows "Valor costo" and "Valor venta" but not the margin.
- **Fix**: Add `Margen: S/. X (Y%)` to the summary.

### Comparison with Best Practices

| Feature | Shopify | Square POS | Simple POS | OptoApp |
|---|---|---|---|---|
| Stock +/- from list | ✅ | ✅ | ✅ | ❌ (only +) |
| Search by name/SKU | ✅ | ✅ | ✅ | ✅ |
| Filter by category | ✅ | ✅ | ✅ | ❌ (hidden) |
| Low stock badge | ✅ | ✅ | ✅ | ✅ (alert card) |
| Stock history | ✅ | ✅ | ✅ | ❌ (not exposed) |
| Barcode scan | ✅ | ✅ | ✅ | ✅ (separate screen) |
| Bulk stock adjust | ✅ | ✅ | ❌ | ❌ |
| Sort column | ✅ | ✅ | ✅ | ❌ (hardcoded) |
| Edit from list | ✅ | ✅ | ✅ | ✅ (dialog) |
| Delete confirmation | ✅ | ✅ | ✅ | ✅ |

---

## Test Coverage

| Test File | Tests | What it covers |
|---|---|---|
| `MonturaDaoTest.kt` | ~10 tests | CRUD operations, flow emissions |
| `MonturaDaoAdvancedSearchTest.kt` | ~8 tests | Advanced search with filters |
| `MonturaMovimientoDaoTest.kt` | ~8 tests | Movement insert, query by optica/montura/date |
| `MonturaInventoryCoordinatorSyncStockTest.kt` | ~5 tests | Stock rebuild from movements |
| `MonturasViewModelCatchRefactorTest.kt` | ~8 tests | Error handling in save() catch block |
| `MonturaDashboardKpiViewModelTest.kt` | ~5 tests | KPI calculations |

**Total: ~44 tests** across 6 test files.

**Gaps**:
- No UI tests for `MonturasScreen` or `MonturaList` (Compose UI tests)
- No ViewModel integration tests for `registrarEntrada`, `delete`, `save` with real DAO
- No tests for the scroll behavior
- No tests for sync conflict resolution specific to monturas

---

## Risk Assessment

### Scroll behavior change risks

| Risk | Impact | Mitigation |
|---|---|---|
| Restructuring to single LazyColumn changes item key behavior | Medium — item animations, recomposition | Use stable keys, test with many items |
| Moving search/cards into LazyColumn `item {}` changes their recomposition behavior | Low — static content doesn't need keys | Wrap in `item(key = "search")` for stability |
| `Modifier.weight(1f)` on LazyColumn may change empty-state layout | Low | Test with empty list |
| Nested scrolling warnings in Logcat | Low | Only applies if outer scrollable + inner LazyColumn; our fix removes the outer scrollable |

### Missing "-" button risks

| Risk | Impact | Mitigation |
|---|---|---|
| Accidental stock going negative | Low — DAO has `(stockActual + :delta) >= 0` guard | SQL-level protection already in place |
| No movement record if user misclicks | Medium | Add confirmation dialog before decrement |

### What could break

1. **Changing the Column to LazyColumn** changes Compose recomposition scopes. Cards that use `viewModel.uiState.collectAsState()` will recompose differently. **Risk: Low** — they'll just recompose more efficiently inside `item {}`.
2. **Removing the outer Column** means the `verticalArrangement = spacedBy(...)` is lost. **Risk: Low** — can be moved to `LazyColumn`'s `verticalArrangement`.
3. **Adding `registrarSalida()`** is a pure additive change. **Risk: Very Low**.

---

## Ready for Proposal

**Yes.** This exploration identifies clear, isolated issues with well-understood fixes. The scroll bug and missing "-" button are quick wins. Filters exposed in ViewModel but not in UI are another easy improvement. Recommend proposing as a single change with 3-4 tasks.

### Summary for User

```
ARCHITECTURE FINDINGS
├── 15 files comprise the inventory module
├── Montura entity: 26 fields, stockActual is denormalized (stored, not calculated)
├── MonturaMovimiento: audit trail for stock changes
├── Data flow: Screen → ViewModel → OptoRepository → MonturaInventoryCoordinator → DAO → Room
├── Offline sync: PostSaveSyncScheduler → SyncInventarioUseCase (upload + download)
└── 44 tests across 6 files (Room DAO + ViewModel + Coordinator)

SCROLL BUG — ROOT CAUSE
Column wrapping the entire screen has no verticalScroll,
and LazyColumn (inside MonturaListSection) has no Modifier.weight(1f).
→ Fix: Restructure to single LazyColumn using item {} for static content.

MISSING "-" BUTTON — ROOT CAUSE
ViewModel has registrarEntrada() but no registrarSalida().
DAO and Coordinator support negative deltas (infrastructure exists).
MonturaItem only renders "+" icon, no "-" icon.
→ Fix: Add registrarSalida() to ViewModel, "-" button to MonturaItem.

HIDDEN FILTERS
ViewModel has filterMarca, filterCategoria, filterStockBajo, filterPrecio
with corresponding methods — but the Screen never uses them.
Search is the only active filter.

UX PRIORITY
P1 [CRITICAL] Screen doesn't scroll → single LazyColumn
P2 [CRITICAL] No stock decrement → add "-" button
P3 [HIGH] Edit form in AlertDialog too long → full-screen form
P4 [HIGH] Filters not exposed → add filter chips
P5 [MEDIUM] No stock movement history → show in detail view
P6 [MEDIUM] No sort → add sort options
```
