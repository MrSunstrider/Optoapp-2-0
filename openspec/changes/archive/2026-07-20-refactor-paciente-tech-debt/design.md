# Design: Refactor Paciente Tech Debt (Tier 3)

## Architecture Decision

All items are isolated changes within existing classes. No new classes, no schema migrations, no DI graph changes. Each item is independently revertible.

## Group A: Quick Wins (no tests, manual verification)

### A1 — LEGACY_OPTICA_ID constant

**File**: `data/paciente/PacienteEntity.kt`  
**Current**: `val opticaId: String = "mi_optica_base"` (line 42)  
**Target**: Add `companion object { const val LEGACY_OPTICA_ID = "mi_optica_base" }`. Change line 42 + 3 occurrences in `SyncPacientesUseCase.kt` (lines 321, 345, 368).  
**Test strategy**: None. Compile-check + `grep` for residual string in touched files.

### A2 — opticaRol default

**File**: `ui/screens/PacientesListScreen.kt` line 60  
**Current**: `collectAsState(initial = "admin")`  
**Target**: `collectAsState(initial = "")`  
**Test strategy**: None. Manual app launch — no admin UI flashes before role loads.

### A3 — Remove unused import

**File**: `ui/screens/PacientesListScreen.kt` line 27  
**Current**: `import androidx.lifecycle.viewmodel.compose.viewModel`  
**Target**: Delete the line.  
**Test strategy**: None. Compile succeeds.

### A4 — O(n²) → O(1) conflict set

**File**: `domain/SyncPacientesUseCase.kt` line 142  
**Current**:
```kotlin
val finalRows = deduplicated.filter { r -> conflictSafe.any { it.id == r.id } }
```
**Target**:
```kotlin
val conflictIds = conflictSafe.mapTo(mutableSetOf()) { it.id }
val finalRows = deduplicated.filter { it.id in conflictIds }
```
**Test strategy**: None. Manual sync run produces identical results.

### A5 — Locale.US for currency formatting

**Files**: 
- `ui/components/paciente/PacienteInfoHeader.kt` line 64: `Locale.getDefault()` → `Locale.US`
- `ui/components/paciente/PacienteServiciosTab.kt` line 54: `Locale.getDefault()` → `Locale.US`
- `ui/components/paciente/PacienteDispensacionesTab.kt` lines 97, 190-192: `Locale.getDefault()` → `Locale.US`
- `util/FormatUtils.kt` `fmt()`: `Locale.getDefault()` → `Locale.US`

**Test strategy**: None. Manual verify decimal separator is `.` on es-PE device.

## Group B: DAO/Sync (RED→GREEN TDD)

### B1 — @Upsert replaces 18-param updatePaciente

**File**: `data/paciente/PacienteDao.kt`  
**Current** (lines 45-77): 18-param `@Query("UPDATE...") suspend fun updatePaciente(...)`  
**Target**: 
```kotlin
@Upsert
suspend fun upsertPaciente(paciente: Paciente)
```
Remove old `updatePaciente` method.

**File**: `data/PacienteRepository.kt`  
**Current** (lines 53-66): `updatePaciente(paciente)` delegates to `pacienteDao.updatePaciente(18 params)`  
**Target**: `pacienteDao.upsertPaciente(paciente)`

**File**: `data/PacienteRepository.kt` `resolveDuplicatePacientesByHistoria` (lines 180-191)  
**Current**: calls `pacienteDao.updatePaciente(18 params)`  
**Target**: calls `pacienteDao.upsertPaciente(mergedCanonical)`

**Test strategy** (RED→GREEN):
1. RED: Write/update `PacienteRepositoryTest.updatePaciente_modifiesExistingRecord` to verify upsert works for both insert and update.
2. GREEN: Implement the changes and run tests.

### B2 — Flow-driven loading replaces delay(100)

**File**: `viewmodel/PacienteViewModel.kt` init block (lines 56-62)  
**Current**:
```kotlin
init {
    viewModelScope.launch {
        sessionManager.opticaId.first { it.isNotBlank() }
        kotlinx.coroutines.delay(100)
        _isLoading.value = false
    }
}
```
**Target**:
```kotlin
init {
    viewModelScope.launch {
        pacientes.dropWhile { it.isEmpty() }.first()
        _isLoading.value = false
    }
}
```
Then `refresh()` loses `delay(100)` too.

**Test strategy** (RED→GREEN):
1. RED: Test that `isLoading` transitions to `false` when `pacientes` emits non-empty list.
2. GREEN: Replace delay with flow-driven approach.

### B3 — download returns actual upserted count

**File**: `domain/SyncPacientesUseCase.kt` download() (lines 246-275)  
**Current**: `return remotos.size` at line 274  
**Target**: Track `var upserted = 0` inside forEach, increment on success, return `upserted`.

**Test strategy** (RED→GREEN):
1. RED: Test `download()` returns the count of successfully upserted rows when some are skipped.
2. GREEN: Count actual upserts instead of `remotos.size`.

### B4 — deletePaciente returns Int

**File**: `data/paciente/PacienteDao.kt` line 80  
**Current**: `suspend fun deletePaciente(id: String, opticaId: String)` (returns Unit)  
**Target**: `suspend fun deletePaciente(id: String, opticaId: String): Int`  
Note: `deletePacienteById` (line 95) already returns `Int` — same SQL.

**Test strategy** (RED→GREEN):
1. RED: Test that `deletePaciente` returns `1` for existing record, `0` for non-existent.
2. GREEN: Add return type `: Int`.

### B5 — CancellationException propagation (verify)

**File**: `domain/SyncPacientesUseCase.kt` download() Phase 1  
**Status**: Already correct. Lines 208-209 rethrow inside forEach, lines 220-221 rethrow in outer catch.  
**Action**: Verify via code review. Add test proving CancellationException propagates and does NOT get caught by `catch (e: Exception)`.

**Test strategy** (GREEN only):
1. Write test: mock supabase to throw `CancellationException`, verify it propagates (not wrapped in Resource.Error).

### B6 — Fix misleading log message

**File**: `domain/SyncPacientesUseCase.kt` line 223  
**Current**: `"Error querying pending deletions for paciente type: ${e.message}"`  
**Target**: `"Error during Phase 1 pending-delete retry for paciente type: ${e.message}"`

**Test strategy**: None (string change). Verification via code review.

### B7 — Use TABLE constant in download Phase 1

**File**: `domain/SyncPacientesUseCase.kt` line 200  
**Current**: `supabase.postgrest["pacientes"].delete {`  
**Target**: `supabase.postgrest[TABLE].delete {`

**Test strategy**: None. Verification via code review — no new test needed since TABLE is already tested elsewhere.

## Group C: ViewModel/UI (RED→GREEN TDD)

### C1 — fechaNacimiento validation for intermediate lengths

**File**: `ui/components/paciente/PacienteFormSections.kt` lines 141-164  
**Current**: Only validates when `length == 8`. Returns `null` (no error) for 1-7 chars.  
**Target**: Show contextual error messages for all lengths:
- 0 chars: no error (field is optional)
- 1-7 chars: "Fecha completa requerida (8 dígitos)"
- 8 chars: existing validation (day/month/year checks)

**Test strategy** (RED→GREEN):
1. RED: Compose UI test that types "12" (2 chars) into the field and asserts error text is visible.
2. GREEN: Modify `fechaNacError` logic to show error for non-empty non-8-digit input.

### C2 — Remove duplicate HO check from screen

**File**: `ui/screens/NuevoPacienteScreen.kt` lines 219-230  
**Current**: Screen calls `viewModel.existsDuplicateHistoriaOptometrica()` before save.  
**Target**: Remove the screen-side check. Only the VM `savePaciente` validates.

**Test strategy** (RED→GREEN):
1. RED: Test that `savePaciente` in VM throws `IllegalArgumentException` for duplicate HO (already tested in `PacienteViewModelTest`). Verify screen save path does NOT call `existsDuplicateHistoriaOptometrica` independently.
2. GREEN: Remove the duplicate check block from NuevoPacienteScreen. Remove `showDuplicateHoWarning` state and dialog if no longer needed.
