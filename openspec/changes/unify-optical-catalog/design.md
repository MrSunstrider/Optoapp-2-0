# Design: Unify Optical Catalog

## Technical Approach

Create `OpticalCatalog.kt` under `domain/` as a Kotlin `object` with 5 canonical properties. Replace 3 companion-object constant lists in `CostosYGastosViewModel.kt` and 3 inline `listOf(...)` calls in `LenteForm.kt` with `OpticalCatalog` references. No DB changes, no logic changes — purely repointing where dropdown options come from.

## Architecture Decisions

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `object` vs `class` | `object` is idiomatic for stateless singleton constants; `class` implies instantiation that never happens | **`object`** |
| `domain/` vs `viewmodel/` | ViewModels are presentation layer; optical values are domain knowledge per Clean Architecture | **`domain/`** |
| `mapOf` vs `val` for TIPO_ARO / SERIES | Display labels are user-facing Spanish; internal values are snake_case (DB) or integers (Room); map provides translation layer | **`mapOf`** for both; follows existing `blockToFilter` pattern |
| Plain list vs map for MATERIALES / TIPO_LENTE / TRATAMIENTOS | These are displayed as-is with no translation to internal IDs | **Plain `listOf`** |

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/OpticalCatalog.kt` | **Create** | Canonical constants: `MATERIALES`, `TIPO_LENTE`, `TRATAMIENTOS`, `TIPO_ARO`, `SERIES` |
| `viewmodel/CostosYGastosViewModel.kt` | **Modify** | Remove companion vals `MATERIALES_OPTICOS`, `TIPOS_LENTE`, `TRATAMIENTOS`; repoint public properties to `OpticalCatalog.*` |
| `ui/components/dispensacion/LenteForm.kt` | **Modify** | Replace inline `listOf(...)` on lines 67, 98, 103, 221 with `OpticalCatalog.*` references |

## OpticalCatalog Properties

| Property | Type | Values |
|----------|------|--------|
| `MATERIALES` | `List<String>` | `["Resina", "Cristal", "Policarbonato", "Trivex"]` |
| `TIPO_LENTE` | `List<String>` | `["Monofocal", "Bifocal", "Multifocal", "Ocupacional", "Lentes de Contacto"]` |
| `TRATAMIENTOS` | `List<String>` | 13 entries ordered: UV 400 → antirayas → antireflejo → B Defense → fotocromático → polarizado → discromatopsia → coloreado → reducción diámetro → alto índice Rose 1.7 → alto índice Blanco 1.7 → alto índice Blanco 1.8 → Circadian |
| `TIPO_ARO` | `Map<String, String>` | Display label → snake_case value (3 entries) |
| `SERIES` | `Map<String, Int?>` | Display label → integer (1, 2, 3, null) |

## Key Normalizations

- **"Progresivo" → "Multifocal"**: Existing DB values use "Multifocal". All dropdowns now show "Multifocal" consistently.
- **"CR39" → "Resina"**: LenteForm was already using "Resina". VM used "CR39". Catalog chooses the user-facing name.
- **"Antireflex" → "Antireflejo"**: LenteForm already uses "Antireflejo". Phase 2 will fix cost matrix lookup to match.
- **VM drops "Alto Índice" / "Policarbonato Alto Índice" / "Cristal Mineral" as materials**: These are treatments and substrate combinations, not fundamental materials. Handled in Phase 2 categorization.

## ViewModel Repointing

```kotlin
// REMOVED from companion object:
// MATERIALES_OPTICOS, TIPOS_LENTE, TRATAMIENTOS

// REPLACED public properties:
val materialesOpticos = OpticalCatalog.MATERIALES
val tiposLente = OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }
val tratamientos = OpticalCatalog.TRATAMIENTOS
```

`tiposLente` filters out "Lentes de Contacto" because LC has its own cost block tab (Phase 4).

## LenteForm Repointing

- **Line 67** (Tipo de Lente): `listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")` → `OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }`
- **Line 98** (Material): `listOf("Resina", "Policarbonato", "Cristal", "Trivex")` → `OpticalCatalog.MATERIALES`
- **Line 103** (Tratamientos): hardcoded `listOf("Ninguno", ...)` → `listOf("Ninguno") + OpticalCatalog.TRATAMIENTOS`
- **Line 221** (Tipo de Aro): `listOf("Aro Completo", "Semi al aire", "Al aire")` → `OpticalCatalog.TIPO_ARO.keys.toList()`

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `OpticalCatalog` property integrity | Plain JUnit. Assert list sizes and non-empty values. No Robolectric. |
| Regression | Existing tests still pass | Run `testDebugUnitTest` after changes. No test modifications expected. |

## Migration / Rollout

No migration required. Constants are compile-time. Rollback: `git checkout` on modified files, delete `OpticalCatalog.kt`.

## Open Questions

- [ ] Are the user-provided treatment names ("UV 400", "Antireflejo B Defense", "Circadian") the final canonical names, or should they align with the spec values ("Filtro UV 400", "AR Blue Defense", "Circadian NK55")?
- [ ] Does the VM treatment list need the empty-string entry `""` preserved for "no treatment selected" in the cost matrix dialog, or is it handled differently?

## Not in This Design

- No DB migrations (values are stored as user-selected strings)
- No DAO or entity changes
- No `determineTipoLente` / `determineSerie` / `normalizeTipoAro` fixes (Phase 2)
- No cost lookup fixes (Phase 2)
