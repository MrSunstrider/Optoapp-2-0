# Sync Conflict Resolution — Full Coverage Specification

## Purpose

Extends download-guard, bump, and three-way merge patterns from 3 protected entity types (servicio_extra, dispensacion, pago) to all 15 entity types. Replaces timestamp-bump trick with field-level snapshot merge.

## PR 1 — Download Guard + DI (FR-01, FR-02)

### FR-01: Download Guard for All Entity Types

All 10 unprotected download methods MUST skip entities with active `conflict_records` using the pattern: `conflictDao.getConflictEntityIds(opticaId, entityType).toSet()` → skip if ID in set.

NOTE: `inventario_fisico` and `inventario_fisico_detalle` are EXCLUDED from download guard because the `InventarioFisico` entity has NO `updatedAt` field — `filterConflicts` never creates `conflict_records` for this type, making the guard a no-op. These entity types are append/sequential in nature (one session at a time) and rarely generate real conflicts.

NOTE: `montura_movimiento` download guard depends on FR-08 (filterConflictMovimientos creating conflict_records). Without FR-08, the guard is a no-op. Apply the guard pattern anyway (it will activate once FR-08 ships).

| # | File | Method | entityType | Applies now? |
|---|------|--------|------------|-------------|
| 1 | SyncPacientesUseCase | `download()` | paciente | ✅ Yes |
| 2 | SyncHistorialUseCase | `downloadEvaluaciones()` | evaluacion | ✅ Yes |
| 3 | SyncInventarioUseCase | `downloadMonturas()` | montura | ✅ Yes |
| 4 | SyncInventarioUseCase | `downloadMovimientos()` | montura_movimiento | ⚠️ Depends on FR-08 |
| 5 | SyncProveedoresUseCase | `downloadProveedores()` | proveedor | ✅ Yes |
| 6 | SyncProveedoresUseCase | `downloadCategorias()` | categoria_montura | ✅ Yes |
| 7 | SyncOrdenesCompraUseCase | `downloadOrdenesCompra()` | orden_compra | ✅ Yes |
| 8 | SyncOrdenesCompraUseCase | `downloadItems()` | orden_compra_item | ✅ Yes |
| 9 | DownloadSyncCoordinator | `downloadDispensacionItems()` | dispensacion_item | ✅ Yes |
| 10 | DownloadSyncCoordinator | `downloadArqueos()` | arqueo_caja | ✅ Yes |

#### Scenario: Conflicted entity skipped during download

- GIVEN active conflict_record for entity X of type `paciente`
- WHEN `SyncPacientesUseCase.download()` runs
- THEN entity X is NOT written to Room
- AND all non-conflicted entities ARE written normally

#### Scenario: No conflicts — download proceeds normally

- GIVEN zero active conflict_records for the optica
- WHEN any download method runs
- THEN all remote entities are written to Room

#### Scenario: Resolved conflict unblocks download

- GIVEN conflict_record for entity X was resolved
- WHEN download runs after resolution
- THEN entity X IS written to Room from remote

#### Scenario: Network error during conflict query

- GIVEN ConflictDao query throws IOException
- WHEN download method runs
- THEN download proceeds without guard (fail-open) and logs error

---

### FR-02: ConflictDao Injection

The following UseCases MUST gain `ConflictDao` as an `@Inject constructor` parameter. Existing Hilt auto-wiring MUST continue to work without module changes.

NOTE: `SyncInventarioFisicoUseCase` is EXCLUDED from injection — `inventario_fisico` has no `updatedAt` field, so `filterConflicts` never creates `conflict_records` for this type. The download guard would be a no-op.

| UseCase | New parameter |
|---------|---------------|
| SyncPacientesUseCase | `conflictDao: ConflictDao` |
| SyncHistorialUseCase | `conflictDao: ConflictDao` |
| SyncInventarioUseCase | `conflictDao: ConflictDao` |
| SyncProveedoresUseCase | `conflictDao: ConflictDao` |
| SyncOrdenesCompraUseCase | `conflictDao: ConflictDao` |

#### Scenario: Hilt provides ConflictDao automatically

- GIVEN all 6 UseCases have `@Inject constructor` with new ConflictDao param
- WHEN the app compiles and runs
- THEN Hilt resolves ConflictDao from OptoDatabase without manual @Provides

#### Scenario: Test construction with fake ConflictDao

- GIVEN a unit test creates SyncPacientesUseCase directly
- WHEN the test passes a fake ConflictDao
- THEN the UseCase constructs without error

---

## PR 2 — Bump Coverage + Upload + UI (FR-03, FR-04, FR-05, FR-06)

### FR-03: bumpEntityUpdatedAt Coverage

`bumpEntityUpdatedAt()` MUST handle all entity types with `updatedAt` + an update method:

NOTE: `inventario_fisico` is EXCLUDED from bump — the entity has NO `updatedAt` field. Adding one would require Room migration + Supabase schema change, which is out of scope for this change. `inventario_fisico` entries are sequential (one active session per optica at a time), so conflicts are unlikely in practice.

| entityType | Repository method | Notes |
|------------|-------------------|-------|
| paciente | `repository.updatePaciente()` | auto-stamps updatedAt |
| evaluacion | `repository.updateEvaluacion()` | auto-stamps |
| montura | `MonturaInventoryCoordinator.updateMontura()` | auto-stamps |
| proveedor | `ProveedorRepository.update()` | needs updatedAt wrapping |
| orden_compra | `OrdenCompraRepository.update()` | needs updatedAt wrapping |
| arqueo_caja | `repository.updateArqueo()` | needs stamp wrapping |

#### Scenario: Bump paciente updates timestamp

- GIVEN paciente X with `updatedAt = T_old`
- WHEN `bumpEntityUpdatedAt(X, "paciente")` runs
- THEN `repository.updatePaciente()` is called with `updatedAt = T_now > T_old`

#### Scenario: Bump unknown type logs and skips

- GIVEN entityType = "unknown_type"
- WHEN `bumpEntityUpdatedAt()` runs
- THEN no repository call is made
- AND a debug log is emitted

#### Scenario: Entity not found in Room

- GIVEN entityId not found by repository
- WHEN bump runs
- THEN a warning is logged, no crash

---

### FR-04: Child Entity Parent Bump

Entity types without their own `updatedAt` SHALL bump their parent entity:

| Child entityType | Parent entityType | Parent bump method |
|-----------------|-------------------|---------------------|
| montura_movimiento | montura | `MonturaInventoryCoordinator.updateMontura()` |
| orden_compra_item | orden_compra | `OrdenCompraRepository.update()` |
| dispensacion_item | dispensacion | `repository.updateDispensacion()` |
| categoria_montura | (none) | Log warning, skip bump |

NOTE: `inventario_fisico_detalle` is EXCLUDED from child→parent bump. The parent entity `inventario_fisico` has no `updatedAt` field, so bumping it would have no effect on conflict detection. `inventario_fisico_detalle` conflicts cannot occur because the parent never generates `conflict_records`.

When a child conflict is resolved with "keep mine", the parent entity MUST be bumped and synced.

#### Scenario: Child montura_movimiento bumps parent montura

- GIVEN conflict on montura_movimiento with ID X
- WHEN `resolveKeepMine` runs for X
- THEN parent montura is bumped via `updateMontura()`
- AND the conflict record for X is cleared on success

#### Scenario: categoria_montura has no parent

- GIVEN conflict on categoria_montura
- WHEN bump is attempted
- THEN a warning is logged and bump is skipped

#### Scenario: Parent entity not found

- GIVEN child conflict references parent ID that no longer exists
- WHEN bump runs
- THEN warning logged, child conflict still cleared

---

### FR-05: Upload Flow Enhancement

Upload methods for entity types without `filterConflicts` SHOULD gain conflict detection where applicable:

| Method | Action |
|--------|--------|
| `uploadMovimientos()` | Already uses `filterConflictMovimientos` — no change |
| `uploadDispensacionItems()` | SHOULD add `filterConflicts` (has updatedAt) |
| `uploadArqueos()` | SHOULD add `filterConflicts` (has updatedAt) |
| `uploadCategorias()` | MAY add `filterConflicts` or remain blind (append-only) |
| `uploadItems()` | MAY add `filterConflicts` (has updatedAt) |
| `uploadDetalles()` | MAY add `filterConflicts` (has updatedAt) |

#### Scenario: uploadDispensacionItems filters conflicts

- GIVEN dispensacion_item X has local < remote updatedAt
- WHEN `uploadDispensacionItems()` runs
- THEN X is excluded from upsert and a conflict_record is created

#### Scenario: Pure-append log uploads without filter

- GIVEN categoria_montura entities
- WHEN `uploadCategorias()` runs
- THEN all entities are uploaded without conflict filtering

---

### FR-05b: filterConflictMovimientos MUST Create conflict_records

The existing `ConflictHelper.filterConflictMovimientos()` detects stock-level conflicts by comparing `stockNuevo` between local and remote (composite key: `referenciaId + tipo + monturaId`), but it does NOT persist conflicts to `conflict_records`. It only marks conflicted IDs in `SyncStateTracker`.

For the `downloadMovimientos` guard (FR-01 row 4) to function, `filterConflictMovimientos` MUST also create `conflict_records` rows for conflicted movimiento IDs, using the same pattern as `filterConflicts()`.

Note: `montura_movimiento` has no `updatedAt` field, so snapshot-based three-way merge does NOT apply. The conflict record will only contain timestamp values (legacy format), and resolution falls back to the bump behavior.

#### Scenario: filterConflictMovimientos creates a conflict_record

- GIVEN local movimiento with `stockNuevo=5`, remote movimiento with `stockNuevo=10`
- GIVEN the composite key (`referenciaId`, `tipo`, `monturaId`) matches
- WHEN `filterConflictMovimientos()` runs
- THEN `conflictDao.upsertConflict()` is called with the movimiento ID
- AND `SyncStateTracker.markConflicted()` is also called

#### Scenario: filterConflictMovimientos skips non-conflicted entities

- GIVEN local and remote have matching `stockNuevo` values
- WHEN `filterConflictMovimientos()` runs
- THEN no `conflict_records` row is created

---

### FR-06: UI Entity Type Labels

`TYPE_LABELS` in `ConflictosScreen.kt` MUST include all 15 entity types:

| entityType | Label |
|------------|-------|
| paciente | Paciente |
| evaluacion | Evaluación |
| dispensacion | Dispensación |
| servicio_extra | Servicio extra |
| pago | Pago |
| montura | Montura |
| montura_movimiento | Movimiento de montura |
| proveedor | Proveedor |
| categoria_montura | Categoría de montura |
| orden_compra | Orden de compra |
| orden_compra_item | Item de orden de compra |
| inventario_fisico | Inventario físico |
| inventario_fisico_detalle | Detalle de inventario |
| dispensacion_item | Item de dispensación |
| arqueo_caja | Arqueo de caja |

#### Scenario: Unknown entity type shows raw type string

- GIVEN a conflict with entityType = "future_type"
- WHEN the conflict card renders
- THEN the label falls back to "future_type" (current behavior preserved)

---

## PR 3 — Three-Way Merge (FR-07 through FR-12)

### FR-07: Conflict Record Schema Migration

Room migration v27→v28 SHALL add three columns to `conflict_records`:

| Column | Type | Default |
|--------|------|---------|
| `baseSnapshot` | TEXT NOT NULL | `'{}'` |
| `localData` | TEXT NOT NULL | `'{}'` |
| `remoteData` | TEXT NOT NULL | `'{}'` |

Existing rows SHALL receive `'{}'` defaults (no data migration).

#### Scenario: Migration preserves existing rows

- GIVEN 5 existing conflict_records with v27 schema
- WHEN migration v28 runs
- THEN all 5 rows exist with `baseSnapshot = '{}'`, `localData = '{}'`, `remoteData = '{}'`

#### Scenario: Fresh insert includes snapshot columns

- GIVEN migration v28 applied
- WHEN `upsertConflict()` is called with snapshot params
- THEN the new row contains the provided JSON values

#### Scenario: Migration on empty database

- GIVEN no existing conflict_records
- WHEN migration v28 runs
- THEN the table is created with all 3 new columns, zero rows

---

### FR-08: Snapshot Capture at Conflict Detection

When `ConflictHelper.filterConflicts()` detects a conflict (local < remote), it MUST:
1. Query the full remote entity row from Supabase (not just timestamp)
2. Serialize the full local entity to JSON (`localData`)
3. Serialize the full remote entity to JSON (`remoteData`)
4. Determine base snapshot: last-synced state from Room, or `'{}'` if unavailable

Serialization MUST use `kotlinx.serialization`.

#### Scenario: Conflict detected — snapshots captured

- GIVEN local entity with `updatedAt = T_old`, remote with `T_remote > T_old`
- WHEN `filterConflicts()` processes this entity
- THEN `conflict_records` row includes non-empty `localData` and `remoteData` JSON

#### Scenario: Base snapshot unavailable

- GIVEN entity has no prior sync state in Room
- WHEN conflict is detected
- THEN `baseSnapshot = '{}'`

#### Scenario: Remote fetch fails during snapshot capture

- GIVEN Supabase query for full remote row throws
- WHEN conflict detection runs
- THEN `remoteData = '{}'`, conflict still recorded with timestamp-only data

---

### FR-09: Field-Level Three-Way Merge Logic

A new class `ThreeWayMerge` SHALL implement:

```
For each field:
  local == base AND remote == base → no change
  local != base AND remote == base → apply local (auto-merge)
  local == base AND remote != base → apply remote (auto-merge)
  local != base AND remote != base → CONFLICT (mark for user)
```

Result SHALL include: merged entity + list of conflicting field names.

#### Scenario: Non-overlapping changes auto-merge

- GIVEN base={a:1, b:2}, local={a:10, b:2}, remote={a:1, b:20}
- WHEN three-way merge runs
- THEN result = {a:10, b:20}, conflictingFields = []

#### Scenario: Overlapping changes produce conflict

- GIVEN base={a:1, b:2}, local={a:10, b:20}, remote={a:100, b:200}
- WHEN three-way merge runs
- THEN conflictingFields = ["a", "b"]

#### Scenario: No changes from either side

- GIVEN base={a:1}, local={a:1}, remote={a:1}
- WHEN merge runs
- THEN result = {a:1}, conflictingFields = []

#### Scenario: Missing snapshot fields treated as no-change

- GIVEN baseSnapshot = '{}' (empty), local and remote have data
- WHEN merge runs
- THEN all fields present in both local and remote are treated as conflicting

---

### FR-10: "Usar el mío" with Three-Way Merge

`resolveKeepMine` SHALL:
1. Perform three-way merge if snapshot data exists (non-empty)
2. Apply auto-merged fields + local values for conflicting fields
3. Upload merged result
4. Clear conflict record only on success
5. Fall back to old bump behavior if `baseSnapshot = '{}'`

#### Scenario: Keep-mine with snapshot data

- GIVEN conflict with valid baseSnapshot, localData, remoteData
- WHEN user taps "Usar el mío"
- THEN merged entity (local wins on conflicts) is uploaded
- AND conflict record cleared on success

#### Scenario: Keep-mine without snapshot falls back to bump

- GIVEN conflict with `baseSnapshot = '{}'`
- WHEN user taps "Usar el mío"
- THEN old bump behavior: updatedAt → now(), upload, resolve

#### Scenario: Upload fails — conflict retained

- GIVEN valid snapshot data, upload throws IOException
- WHEN keep-mine runs
- THEN conflict record remains active

---

### FR-11: "Usar la nube" with Three-Way Merge

`resolveAcceptTheirs` SHALL:
1. Perform three-way merge if snapshot data exists
2. Apply auto-merged fields + remote values for conflicting fields
3. Write merged result to Room
4. Clear conflict record
5. Fall back to old behavior if `baseSnapshot = '{}'`

#### Scenario: Accept-theirs with snapshot data

- GIVEN conflict with valid snapshots
- WHEN user taps "Usar nube"
- THEN merged entity (remote wins on conflicts) is written to Room
- AND conflict record cleared

#### Scenario: Accept-theirs without snapshot

- GIVEN `baseSnapshot = '{}'`
- WHEN user taps "Usar nube"
- THEN old behavior: clear conflict, force-download entity

#### Design Decision: resolveAcceptTheirs writes to Room, does NOT upload

Writing the merged result (remote wins) directly to Room is intentional:
- The merged entity is the authoritative state (remote version for conflicting fields, merged auto-fields)
- The next sync cycle will detect no diff between Room and Supabase (the merged entity matches server state)
- Uploading the merged result would bump `updatedAt` locally, potentially re-triggering unwanted conflicts
- This mirrors the current `skipUpload = true` behavior in `resolveAcceptTheirs`

---

### FR-12: Field-Level Conflict UI

`ConflictosScreen` SHALL show:
1. For conflicts WITH snapshot data: list of conflicting field names with local vs remote values
2. For conflicts WITHOUT snapshot data: current timestamp-based display (backward compatible)
3. Auto-merged fields SHALL NOT appear in the conflict list
4. User SHALL have only two global decisions: "Usar el mío" / "Usar la nube" (no per-field decision)

#### Scenario: Snapshot-based conflict shows field diffs

- GIVEN conflict with non-empty snapshots and conflictingFields = ["nombre", "telefono"]
- WHEN conflict card renders
- THEN card shows "nombre: local=Juan vs nube=Pedro" and "telefono: local=555 vs nube=666"

#### Scenario: Pre-migration conflict shows timestamps

- GIVEN conflict with `baseSnapshot = '{}'`
- WHEN conflict card renders
- THEN card shows timestamp-based display (current behavior)

#### Scenario: Auto-merged fields hidden from UI

- GIVEN merge result with 2 auto-merged fields and 1 conflicting field
- WHEN conflict card renders
- THEN only the 1 conflicting field is shown
