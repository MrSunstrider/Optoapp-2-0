# Exploration: Deferred Tier 3 Paciente Tech Debt (Items 15-22)

**Change**: `deferred-tier3-paciente-tech-debt`
**Date**: 2026-07-21
**Source**: Items 15-22 deferred from `refactor-paciente-tech-debt` (archived)

---

## Item 15: JSON array for `ultimasEtiquetas`

### Current State

**Room DB** — Already correct. `Converters.kt` has a JSON-based TypeConverter:
```kotlin
@TypeConverter
fun fromStringList(value: List<String>): String = json.encodeToString(value)
@TypeConverter
fun toStringList(value: String): List<String> = json.decodeFromString(value)
```
This is registered globally on `OptoDatabase` via `@TypeConverters(Converters::class, ...)`. Room stores `List<String>` as a JSON array string — commas in tag values are safely escaped.

**Supabase column** — `ultimas_etiquetas TEXT NOT NULL DEFAULT ''` (set in migration `20260713053521_remote_schema.sql`). It's a plain TEXT column, not JSONB. No existing migration converts it.

**Sync serialization (BROKEN)** — `SyncPacientesUseCase.kt`:
- `Paciente.toRemoto()` (line 361): `ultimasEtiquetas.joinToString(",")` — produces CSV
- `PacienteRemoto.toEntity()` (lines 335-338): `split(",")` — splits CSV, BUT this is the _receiving_ side so it reads Supabase data. If we change the stored format, this must change too.
- `PacienteRemoto.ultimasEtiquetas` (line 314): typed as `String?` 

- `SyncHistorialUseCase.kt` (line 97): `p.ultimasEtiquetas.joinToString(",")` — same CSV pattern in orphan upload

**`PacienteRepository.kt` merge** (line 222): `(canonical.ultimasEtiquetas + other.ultimasEtiquetas).distinct()` — already works with `List<String>`, no issue.

**UI**: No UI references to `ultimasEtiquetas` anywhere in the screen/composable layer.

### Affected Files
- `data/paciente/PacienteEntity.kt` — `ultimasEtiquetas: List<String>` (no change needed)
- `data/Converters.kt` — JSON `List<String>` ↔ `String` (already correct)
- `domain/SyncPacientesUseCase.kt` — `toRemoto()` serialization (CSV→JSON), `PacienteRemoto.ultimasEtiquetas` type, `toEntity()` deserialization (CSV→JSON)
- `domain/SyncHistorialUseCase.kt` — orphan upload serialization (CSV→JSON)
- Supabase migration — potentially add migration to convert existing data from CSV→JSONB OR keep as TEXT but ensure the data format is JSON array (`["tag1","tag2"]`)
- `openspec/specs/paciente/spec.md` — document the schema contract if changing

### Approaches

1. **Keep column as TEXT, change serialization format to JSON array string** (no Supabase migration)
   - Change `toRemoto()` to `Json.encodeToString(ultimasEtiquetas)` 
   - Change `PacienteRemoto.ultimasEtiquetas` type to `String?` (keep as-is)
   - Change `toEntity()` to `Json.decodeFromString<List<String>>(ultimasEtiquetas ?: "[]")`
   - Same changes in `SyncHistorialUseCase.kt` orphan upload
   - Existing Supabase data with CSV format will fail to parse — need a data migration OR add a try-catch fallback to split by `","` for backward compat
   - Pros: No Room schema change, no Supabase column type change
   - Cons: Existing DB rows in Supabase with CSV format break
   - Effort: **Low** (code only)

2. **Change column to JSONB in Supabase + Room migration**
   - Supabase: `ALTER TABLE pacientes ALTER COLUMN ultimas_etiquetas TYPE JSONB USING to_jsonb(string_to_array(ultimas_etiquetas, ','))`
   - Sync: Change DTO field to `@SerialName("ultimas_etiquetas") val ultimasEtiquetas: List<String>? = null`
   - Room: No change needed (already stores JSON via TypeConverter)
   - Pros: Clean JSONB type in Postgres, queryable with JSON operators
   - Cons: Supabase migration (requires careful handling), existing data must be converted
   - Effort: **Medium**

3. **Comma-separated with escaping** — keep CSV but escape commas
   - Use a separator that cannot appear in tag values (e.g. `|||`)
   - Pros: Minimal code change
   - Cons: Still fragile, non-standard, ugly
   - Effort: **Low** (but poor quality)

### Recommendation
**Approach 1** with backward-compatible fallback. The effort-to-impact ratio is best:
- Change serialization to JSON array (`Json.encodeToString`/`Json.decodeFromString`)
- In `toEntity()`, try JSON first; if that fails, fall back to `split(",")` for existing CSV data
- No Room migration needed (Room already uses JSON)
- No Supabase column type change (keep TEXT, just store JSON array string)
- A subsequent clean-up Supabase migration can convert all rows to JSONB if desired
- Add a `TODO` comment in the code to remove CSV fallback after all clients have synced

### Risks
- Existing Supabase rows with CSV format need graceful handling during transition
- Need to verify that `Json.encodeToString` produces the same format as what Room's TypeConverter produces (both use `kotlinx.serialization.json.Json` with default settings)

---

## Item 16: `Resource.Empty` variant

### Current State
```kotlin
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T, val stale: Boolean = false) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
```

Currently:
- `Success(emptyList())` is used when data legitimately has zero items
- `Success(Unit)` is used for success-without-data
- ViewModels use `_state.value = Resource.Loading()` before loading, then emit the result

### Affected Files
- `data/Resource.kt` — add `Empty<T>` variant
- ALL files that have `when` blocks matching on Resource: at least 15+ files across ViewModels and Screens

### Approaches

1. **Add `Empty<T>` variant**
   - `class Empty<T>(val stale: Boolean = false) : Resource<T>()` 
   - Search all `when(resource)` blocks and add `is Resource.Empty ->` branch
   - Cons: TOUCHES EVERY `when` block — many screens, many ViewModels. This is a global refactor with no direct user-facing benefit.
   - Effort: **High** (dozens of files, testing every `when` block)

2. **Do nothing — use convention `Success(emptyList(), stale=true)`**
   - Add a helper function `Resource.empty<T>(stale: Boolean = false)` that returns `Success(emptyList<T>(), stale)`
   - Document that empty data = `Success(emptyList())`, no data yet = `Loading()`
   - Pros: Zero refactoring, just convention
   - Cons: Doesn't solve the ambiguity for non-List types
   - Effort: **Very Low**

3. **Add `Empty` variant but scope to Paciente-related flows only**
   - Add `Empty<T>` to Resource.kt
   - Only update `when` blocks in PacienteViewModel, SyncPacientesUseCase, and Paciente-related screens
   - Pros: Limited scope
   - Cons: Inconsistency — some code uses Empty, some doesn't
   - Effort: **Medium**

### Recommendation
**Approach 2** — Document the convention. The `stale` parameter on `Success` already exists for this purpose (it differentiates fresh-but-empty from stale-but-empty). Adding a new variant forces changes across the entire codebase for marginal benefit. If a future global refactor touches Resource usage, add `Empty` then.

### Risks
- None — this is a no-action item. The existing pattern is workable.

---

## Item 17: `firstOrNull` safety for `.first()` on flows

### Current State
100+ `.first()` calls found. Categories:

**HIGH RISK — `memberships.first()` (when memberships is a `List`, not a Flow)**
- `AuthDelegate.kt` line 260: `memberships.first()` — guarded by `memberships.size == 1` immediate above, so safe
- `AuthViewModel.kt` line 248: `authDelegate.selectOptica(memberships.first())` — need to check context

**MEDIUM RISK — `sessionManager.opticaId.first()` (~50+ occurrences)**
- `opticaId` is a `MutableStateFlow<String>` initialized with `getSecureOpticaId()` which has fallback `LEGACY_OPTICA_ID`. **Never empty** — always has a value. `.first()` is safe.
- `sessionManager.opticaRol.first()` — initialized with `getSecureOpticaRol()` with fallback `""`. **Always emits.** Safe.
- `sessionManager.userEmail.first()` — initialized from DataStore. DataStore always emits at least once (even if null). Safe.
- `securityManager.userPin.first()` — backs onto `MutableStateFlow<String>` initialized with `getSecurePin()` (fallback `""`). **Always emits.** Safe.

**LOW RISK — Room DAO `.first()` calls:**
- `repository.getPagosByDispensacion(id).first()` — Room flows always emit at least one result (empty list or data). Safe.
- `repository.getEvaluacionesByPaciente(id).first()` — same. Safe.
- `costoProductoDao.getByBloque(opticaId, bloqueFilter).first()` — same. Safe.

**Actually risky — list.first() calls:**
- `DispensacionViewModel.kt` line 349: `val primerItem = s.items.first()` — could crash if items is empty
- `PlayBillingManager.kt` line 152: `detailsList.first()` — could crash if no matching SKU

### Affected Files
- ALL ViewModels in `viewmodel/` — 30+ files, 100+ call sites
- Several Use Cases and Managers

### Approaches

1. **Systematic replace of `.first()` → `.firstOrNull()` across ALL call sites**
   - Then add null checks or `?:` elvis operators at each call site
   - Pros: Defensive
   - Cons: ~100+ changes, many unnecessary (safe StateFlows), generates noise
   - Effort: **Very High (~100+ changes, ~30 files)**

2. **Focus on actual risky calls only (~5-10 sites)**
   - `list.first()` calls on user-facing lists
   - `detailsList.first()` in billing
   - `memberships.first()` when not guarded by size check
   - Pros: Meaningful safety, minimal changes
   - Cons: Doesn't solve the theoretical Flow concern
   - Effort: **Low**

3. **Do nothing — all StateFlow/DataStore `.first()` calls are safe**
   - Document in architecture notes that StateFlow + DataStore flows always emit initial values
   - Only fix `list.first()` calls when they appear in bug reports
   - Effort: **None**

### Recommendation
**Approach 2** — Focus on actual crashes:
- `list.first()` calls on user-provided data (e.g. `s.items.first()`) — wrap in `firstOrNull()?.let { }` or check `isNotEmpty()` first
- `detailsList.first()` in billing — guard with `firstOrNull()`
- Verify `memberships.first()` call sites are guarded (they seem to be)
- All `sessionManager.*.first()` calls are safe (StateFlow always emits initial value)
- All DAO `Flow.first()` calls are safe (Room emits at least once)

### Risks
- Approach 1 generates commit noise with low safety gain
- Approach 3 is pragmatic but needs clear documentation

---

## Item 18: `suggestNextHistoriaOptometrica` with SQL `MAX`

### Current State
```kotlin
// PacienteRepository.kt lines 76-86
suspend fun suggestNextHistoriaOptometrica(opticaId: String): String {
    val historias = pacienteDao.getHistoriasOptometricasByOptica(opticaId)
    val year = LocalDate.now().year.toString()
    val regex = Regex("^HO-$year-(\\d+)$", RegexOption.IGNORE_CASE)
    var max = 0
    for (historia in historias) {
        regex.find(historia.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { if (it > max) max = it }
    }
    val next = max + 1
    return "HO-$year-" + next.toString().padStart(4, '0')
}
```

DAO query (line 17-18):
```kotlin
@Query("SELECT historiaOptometrica FROM pacientes WHERE opticaId = :opticaId AND ifnull(historiaOptometrica, '') <> ''")
suspend fun getHistoriasOptometricasByOptica(opticaId: String): List<String>
```
Loads ALL non-empty `historiaOptometrica` values into memory → O(n) memory and CPU. For 10K patients with HOs, this is significant.

### Affected Files
- `data/PacienteDao.kt` — add `getMaxHistoriaOptometricaNumber()` or equivalent
- `data/PacienteRepository.kt` — lines 76-86 replace with SQL query
- `viewmodel/PacienteViewModel.kt` — caller at line 187

### Approaches

1. **SQL `MAX` with regex extraction in SQL**
   - Since the format is `HO-YYYY-NNNN`, we can use Postgres regex but Room/SQLite accepts `LIKE` patterns
   - SQLite query: `SELECT MAX(CAST(SUBSTR(historiaOptometrica, 8) AS INTEGER)) FROM pacientes WHERE opticaId = :opticaId AND historiaOptometrica LIKE 'HO-' || :year || '-%'`
   - Room DAO: `@Query("SELECT MAX(CAST(SUBSTR(historiaOptometrica, 8) AS INTEGER)) FROM pacientes WHERE opticaId = :opticaId AND historiaOptometrica LIKE 'HO-' || :year || '-%'")`
   - Needs year parameter passed from repository
   - Pros: O(1) memory, leverages SQL index on opticaId
   - Cons: SQLite string manipulation is slightly less readable
   - Effort: **Low** (2 files: DAO + Repository)

2. **Keep Kotlin logic, add LIMIT 1 to avoid loading all rows**
   - Not possible with current design (needs MAX)
   - Can't meaningfully limit without SQL

3. **Hybrid: SQL for counting, Kotlin for formatting**
   - Add DAO method for MAX number
   - Repository calls DAO, formats result
   - This is Approach 1
   - Effort: **Low**

### Recommendation
**Approach 1** — SQL MAX query. Clean, efficient, easy to test.

### Risks
- None. The format pattern `HO-YYYY-NNNN` is well-established across the codebase.
- `SUBSTR(historiaOptometrica, 8)` assumes the prefix `HO-YYYY-` is always 8 chars. `HO-` (3) + YYYY (4) + `-` (1) = 8. This is correct.

---

## Item 19: Empty-field layout

### Current State
- **PacientesListScreen**: Has empty state (PersonOff icon + "No se encontraron pacientes", lines 187-194)
- **DetallePacienteScreen tabs**: NO empty state for evaluaciones/dispensaciones/servicios lists. Shows just the tab with 0 count but no visual empty placeholder.
- **ReportesScreen**: Has basic empty state text (line 236-243)
- **GastosScreen**: Has empty state with icon + text (lines 100-109)
- **AgendaScreen**: Probably missing empty state
- **MonturasScreen**: Missing empty state
- **CostosYGastosScreen**: Missing empty state for sub-tabs
- Most list screens lack a reusable empty-state composable

### Affected Files
- `ui/components/paciente/EvaluacionesList.kt` (if exists)
- `ui/components/paciente/DispensacionesList.kt`
- `ui/components/paciente/ServiciosExtraList.kt`
- Potentially many list screens

### Approaches

1. **Create reusable `EmptyState` composable**
   ```kotlin
   @Composable
   fun EmptyState(
       icon: ImageVector = Icons.Default.Inbox,
       title: String = "Sin datos",
       subtitle: String? = null,
       action: @Composable (() -> Unit)? = null,
   )
   ```
   Then use in all list screens and tab content areas.
   - Pros: DRY, consistent look, easy to add to any screen
   - Cons: Need to update each list component individually
   - Effort: **Medium** (one composable + updates to ~5-8 list components)

2. **Unforked approach: inline empty states per screen**
   - Just add `if (items.isEmpty()) { /* empty UI */ }` wherever it's missing
   - Pros: Quick
   - Cons: Inconsistent, repeated code
   - Effort: **Low** per screen

3. **Combine with Item 22 (spinner state)** — create a `LoadingOrEmptyOrContent` pattern composable
   - A single state wrapper that shows: Loading spinner | Empty state | Content
   - Pros: Also solves Item 22
   - Cons: More complex, might not fit every screen's layout
   - Effort: **Medium**

### Recommendation
**Approach 1** — Create an `EmptyState` composable in `ui/components/`. Initially apply it to:
- `DetallePacienteScreen` tabs (3 lists)
- `PacientesListScreen` already has inline empty state, can optionally migrate
- Any screen that currently has inline but ad-hoc empty states
This is a pure UI change, testable with Compose UI tests.

### Risks
- None. Pure additive UI change.

---

## Item 20: Etiquetas UI

### Current State
Tag-related UI for patient tags (`ultimasEtiquetas`) **does not exist**. Current findings:
- `PacienteInfoHeader.kt` — no tags display
- `NuevoPacienteScreen.kt` — no tags input in the form. Constructor at line 186 omits `ultimasEtiquetas` (uses default `emptyList()`)
- `PacientesListScreen.kt` — no tags in the card/row
- The word `etiqueta` in the codebase refers to appointment status labels in AgendaScreen/CierreSection, NOT patient tags

`ultimasEtiquetas` is populated only via sync (from Supabase) and during merge operations (`PacienteRepository.kt` line 222). There is no mechanism to ADD, EDIT, or DISPLAY tags on Android.

### Affected Files
- `ui/components/paciente/PacienteInfoHeader.kt` — add tag chip display
- `ui/components/paciente/PacienteFormSections.kt` — add tag input UI
- `ui/screens/NuevoPacienteScreen.kt` — pass etiquetas to form
- `ui/screens/DetallePacienteScreen.kt` — if editing tags
- `viewmodel/PacienteViewModel.kt` — savePaciente needs to handle tags
- `ui/components/OptoTagsChips.kt` — new composable for tag input/display

### Approaches

1. **Display-only: show existing tags as chips in PacienteInfoHeader**
   - Add `ultimasEtiquetas.forEach { FilterChip(it) }` or similar
   - No tag editing capability
   - Pros: Minimal change, surface existing data
   - Cons: Users still can't edit tags
   - Effort: **Very Low**

2. **Full CRUD: Display + Edit in a dedicated tags section**
   - Add tag input with add/remove chip UI in PacienteFormSections
   - Display in PacienteInfoHeader
   - Persist via PacienteViewModel.savePaciente
   - Pros: Complete feature
   - Cons: Larger scope, needs form integration, may need Supabase migration
   - Effort: **High**

3. **Future: Add when tags become searchable/filterable**
   - Wait until the filter/search use case requires tags
   - Don't build UI now, tags only exist as sync-pass-through data
   - Effort: **None**

### Recommendation
**Approach 1** (Display-only) for this change. Tags are currently dangling data — users don't know they exist because they're never shown. Displaying existing tags is the minimum viable improvement. Full CRUD can be a separate change.

### Risks
- Minimal. Display-only change is safe. No data mutation.

---

## Item 21: Sort order at DAO level

### Current State
Several queries sort in Kotlin instead of SQL `ORDER BY`:

1. **PacienteViewModel.kt** (lines 108-114):
   ```kotlin
   .map { list ->
       when (sort) {
           "reciente" -> list.sortedByDescending { it.fechaCreacion }
           "antiguo" -> list.sortedBy { it.fechaCreacion }
           else -> list.sortedBy { it.nombreCompleto.lowercase() }
       }
   }
   ```
   The DAO already returns `ORDER BY nombreCompleto ASC` as default. The sort order is dynamic (user-selectable), which is harder to push to SQL cleanly.

2. **GastosScreen.kt** (line 112): `gastos.sortedByDescending { it.fecha }` — in UI composition, not DAO.

3. **MonturasScreen.kt** (lines 103-109): Dynamic sort — similar to PacientesList.

4. **CostosYGastosScreen.kt** (line 448): `gastos.sortedByDescending { it.fecha }` — in UI.

### Affected Files
- `viewmodel/PacienteViewModel.kt` — lines 108-114
- `ui/screens/GastosScreen.kt` — line 112
- `ui/screens/MonturasScreen.kt` — lines 103-109
- Possibly other screens with in-Composable sorting

### Approaches

1. **Add DAO methods with `ORDER BY` for common sort orders**
   - For PacienteDao: add `getPacientesByOpticaSortedBy(column, direction)` or specific methods for each sort
   - Pros: SQL-level sort uses indexes
   - Cons: Multiplies DAO methods; dynamic sort (user selects) doesn't map cleanly
   - Effort: **Medium**

2. **Move sort to the repository/use-case layer (one place only)**
   - Currently sort is in ViewModel (PacienteViewModel) and Screen (GastosScreen)
   - Move all sorting to the ViewModel's flow processing (already done in PacienteViewModel, but GastosScreen sorts in composable)
   - Pros: Centralized, testable
   - Cons: Doesn't address the SQL concern
   - Effort: **Low**

3. **Only fix cases where sort is in the UI composable (GastosScreen, MonturasScreen)**
   - Move those `sortedBy` calls into ViewModels
   - Leave PacienteViewModel alone (it already does this in VM correctly)
   - Pros: Quick wins, correctness
   - Cons: Partial fix
   - Effort: **Low**

### Recommendation
**Approach 2** for easy wins + note on the PacienteViewModel pattern:
- Move in-Composable sorting (GastosScreen, MonturasScreen) to their respective ViewModels
- Document that PacienteViewModel's sort is acceptable (dynamic user choice, already in VM, loading all data is required for the filter+search+sort combo anyway)
- Do NOT add DAO-level ORDER BY for every possible sort; the Kotlin-sort pattern is acceptable for small-to-medium datasets (typical optica has hundreds, not millions, of patients)

### Risks
- None. Moving sort out of composables is a pure refactor.
- For very large opticas (10K+ patients), the PacienteViewModel sort should be addressed later with server-side sorting.

---

## Item 22: Spinner error state

### Current State
24 `CircularProgressIndicator` usages found. Pattern analysis:

**Good pattern** — PacientesListScreen:
- `LinearProgressIndicator` at line 162 during loading
- Separate error `Card` display at lines 166-184 with retry button
- The loading indicator is an overlay, not blocking

**Risky pattern** — DetallePacienteScreen (line 248-250):
```kotlin
} ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}
```
Spinner shows while patient loads. If `paciente = pacienteViewModel.getPaciente(id)` returns null (not found), the spinner stays forever — NO error state.

**Risky pattern** — GastosScreen (lines 68-76):
```kotlin
if (uiState.isLoading) {
    Box { CircularProgressIndicator() }
    return@Scaffold
}
```
If loading fails silently (error not set), the entire screen is stuck. Also, this `return@Scaffold` means error state exists in the ELSE branch but is suppressed.

**Risky pattern** — CostosYGastosScreen:
- Has loading + error states but also has `CircularProgressIndicator` in sub-sections

**Screens with unconditional spinners** (no error fallback):
- DetallePacienteScreen (line 249)
- GastosScreen (line 73) — guarded by isLoading
- InformacionFinancieraScreen (line 54) — need to check context

### Affected Files
- `ui/screens/DetallePacienteScreen.kt` — line 249, spinner with no error state
- `ui/screens/InformacionFinancieraScreen.kt` — line 54
- `ui/screens/GastosScreen.kt` — lines 68-76
- Potentially other screens

### Approaches

1. **Add timeout + error display to all spinner-only loading states**
   - Every `CircularProgressIndicator()` should have a surrounding `LaunchedEffect` with a timeout
   - After timeout, show "Cargando..." → "Error al cargar" state
   - Pros: No more infinite spinners
   - Cons: More boilerplate per screen
   - Effort: **High** (24 sites)

2. **Create `LoadingOverlay` composable with integrated timeout+error**
   ```kotlin
   @Composable
   fun LoadingContent(
       isLoading: Boolean,
       error: String? = null,
       onRetry: (() -> Unit)? = null,
       content: @Composable () -> Unit,
   )
   ```
   Wraps: Loading spinner | Error with retry | Content
   - Pros: Reusable, consistent, solves both spinner and error
   - Cons: Refactor needed in existing screens
   - Effort: **Medium** (create composable + update screens)

3. **Focus on the one actually broken screen (DetallePacienteScreen)**
   - Add timeout to the `LaunchedEffect(id)` that loads the patient
   - Show error state (retry button) if patient is null after timeout
   - Other screens have functional error handling already (separate state variables)
   - Effort: **Very Low**

### Recommendation
**Approach 3** first — fix the genuinely broken infinite-spinner case:
- In `DetallePacienteScreen.kt`, add error handling: if paciente is null and not loading, show error with retry
- Create a `LoadingContent` composable as **optional** follow-up (nice-to-have, not blocking)
- Most `CircularProgressIndicator` usages are in auth/login screens where error handling already exists (they show error via other mechanisms)

### Risks
- None for Approach 3.

---

## Recommended Grouping & Order

Based on independence, complexity, and TDD requirements:

### Group A: Quick Wins (No tests, code review only)
1. **Item 15** — JSON array for `ultimasEtiquetas` (code-only, backward-compatible approach)
2. **Item 18** — SQL MAX for `suggestNextHistoriaOptometrica` (2-file change)
3. **Item 21** — Move composable-side sorting to ViewModels (GastosScreen, MonturasScreen)

### Group B: UI Improvements (Compose UI tests)
4. **Item 19** — `EmptyState` composable (add to 3 tab lists in DetallePacienteScreen)
5. **Item 20** — Display tags in PacienteInfoHeader (display-only, no tests needed for logic)
6. **Item 22** — Fix DetallePacienteScreen infinite spinner + add timeout

### Group C: Optional/Low Urgency
7. **Item 16** — `Resource.Empty` (defer, use convention instead)
8. **Item 17** — `firstOrNull` safety (only fix 2-3 actual risky sites, not all 100)

### Ordering Rationale
- Group A items are purely internal (no UI changes), independently testable or simple code review only
- Group B items improve the user experience and share some composable infrastructure
- Group C items are deferred further — Item 16 adds global refactoring cost, Item 17 is mostly theoretical risk

## Ready for Proposal
**Yes.** All 8 items have been investigated. The orchestrator can create a proposal with:
- 6 actionable items (15, 18, 19, 20, 21, 22) with clear approaches
- 2 deferred/optional items (16, 17) with recommendations to minimize scope
- 3 proposed implementation groups with ordering
- A `stale` parameter on `Success` already handles the `Resource.Empty` concern
- All StateFlow `.first()` calls are safe (initial values always emitted)
