# Design: Critical Fixes

## Technical Approach

Three independent concerns refactored in parallel: (C1) replace 57+ generic `catch (e: Exception)` with typed catches + structured logging + Result propagation, (C2) migrate 27 `@SerializedName` → `@SerialName` + remove Gson provider, (C3) add ≥5 unit tests per repository. Each critical is self-contained; ordering matters only for build integrity (C2 first or alongside C1, C3 last).

## Architecture Decisions

### Decision: C1 catch pattern — rethrowIfCancellation + typed catches + Log.e + Result.failure

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Wrap all in top-level try/catch | Loses per-operation error granularity | Rejected |
| Per-method typed catches + consistent logging | Slightly more lines, clear error boundaries | **Adopted** |
| Introduce Timber | Not in codebase, would add deps | Rejected — use `Log.e(TAG, ...)` (existing pattern) |

Pattern: `rethrowIfCancellation(e)` first, then matching typed catch (IOException, PostgrestException, etc.), then `Log.e(TAG, "operation: context", e)`, then `Result.failure(e)`.

### Decision: C2 BackupData alternate names — custom kotlinx serializer

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Drop alternate names | Breaks deserialization of old backup JSONs | Rejected |
| `@SerialName` only for primary name | Only reads the primary key, loses "ordenes"/"ventas" | Rejected |
| Custom `JsonTransformingSerializer` | Preserves backward compat, adds ~20 LOC | **Adopted** |

BackupData fields with alternate names (`dispensaciones` ← `ordenes`, `ventas`; `serviciosExtra` ← `servicios`, `otrosServicios`) get a custom serializer that tries each name in order.

### Decision: C3 test strategy — per-repository approach

| Repository | Dependencies | Approach |
|------------|-------------|----------|
| MembershipRepository | SupabaseClient (mock) | MockK for SupabaseClient |
| PacienteRepository | PacienteDao, EvaluacionDao | Room in-memory (existing PagoDaoTest pattern) |
| DispensacionRepository | DispensacionDao, PagoDao, ServicioExtraDao | Room in-memory |
| OptoRepository | 8 DAOs, SyncStateTracker, Lazy<>, 3 repos | MockK for all deps |

Add MockK (`io.mockk:mockk`) to test dependencies in `build.gradle.kts`.

## Data Flow

### C1 Exception flow

```
Repository/UseCase method
  → try { operation() }
  → catch (e: CancellationException) { throw e }
  → catch (e: IOException) { Log.e(TAG, "op: ctx", e); Result.failure(e) }
  → catch (e: PostgrestException) { Log.e(TAG, "op: ctx", e); Result.failure(e) }
```

### C2 Serialization flow

```
BackupDelegate (before): Gson.toJson(BackupData) → JSON
BackupDelegate (after):  kotlinx.Json.encodeToString(BackupData) → JSON
BackupDelegate (after):  kotlinx.Json.decodeFromString(BackupData) ← JSON (handles old names via custom serializer)
```

## File Changes

### C1: Exception Handling

| File | Action | Description |
|------|--------|-------------|
| `data/MembershipRepository.kt` | Modify | 7+ generic catches → typed + rethrowIfCancellation + Log.e + Result.failure |
| `data/PacienteRepository.kt` | Modify | 2 generic catches → typed (likely DatabaseException/IOException) |
| `data/DispensacionRepository.kt` | Modify | 2 generic catches → typed |
| `data/OptoRepository.kt` | Modify | 6 generic catches in `restoreBackup` → typed + Log.e (no Result needed, it's a fire-and-forget restore loop) |
| `domain/SyncFinanzasUseCase.kt` | Modify | Already has pattern for top-level; refine inner catches (14 locations) with specific types |
| `domain/SyncInventarioUseCase.kt` | Modify | 6 generic catches → typed |
| `domain/SyncPacientesUseCase.kt` | Modify | 4 generic catches → typed |
| `domain/SyncHistorialUseCase.kt` | Modify | 4 generic catches → typed |
| `domain/SyncSessionHelper.kt` | Modify | 1 generic catch → typed |
| `domain/command/CommandPatterns.kt` | Modify | 1 generic catch → typed |
| `domain/sync/strategies/DefaultStrategies.kt` | Modify | 1 generic catch → typed |

### C2: Serialization Migration

| File | Action | Description |
|------|--------|-------------|
| `data/paciente/PacienteEntity.kt` | Modify | `@SerializedName` → `@SerialName`, add `@Serializable` (3 annotations) |
| `data/evaluacion/EvaluacionEntity.kt` | Modify | `@SerializedName` → `@SerialName`, add `@Serializable` (4 annotations) |
| `data/dispensacion/DispensacionEntity.kt` | Modify | `@SerializedName` → `@SerialName`, add `@Serializable` on 5 classes (14 annotations) |
| `data/OptoRepository.kt` | Modify | `@SerializedName` → `@SerialName` on BackupData (6 annotations); add custom serializer for alternate names |
| `di/DatabaseModule.kt` | Modify | Remove `provideGson()` (line 133-135) |
| `viewmodel/auth/BackupDelegate.kt` | Modify | Replace `gson.toJson(...)` with `Json.encodeToString(...)`; replace `gson` constructor param with `Json` |
| `app/build.gradle.kts` | Modify | Remove `implementation(libs.gson)`, verify `kotlinx-serialization-json` dependency exists |
| `gradle/libs.versions.toml` | Modify | Add `kotlinx-serialization-json` version entry (if missing) |

### C3: Repository Test Coverage

| File | Action | Description |
|------|--------|-------------|
| `data/MembershipRepositoryTest.kt` | **Create** | ≥5 tests, MockK for SupabaseClient |
| `data/OptoRepositoryTest.kt` | **Create** | ≥5 tests, MockK for all deps |
| `data/PacienteRepositoryTest.kt` | **Create** | ≥5 tests, Room in-memory |
| `data/DispensacionRepositoryTest.kt` | **Create** | ≥5 tests, Room in-memory |
| `app/build.gradle.kts` | Modify | Add `testImplementation(libs.mockk)` |
| `gradle/libs.versions.toml` | Modify | Add mockk version entry |

## Interfaces / Contracts

### BackupData custom serializer (key new type)

```kotlin
@Serializable(BackupDataSerializer::class)
data class BackupData(
    val version: Int = 3,
    val dateExported: Long = System.currentTimeMillis(),
    val appIdentifier: String = "OptoApp-2.0",
    @SerialName("source_optica_id")
    val sourceOpticaId: String? = null,
    val pacientes: List<Paciente>? = emptyList(),
    val evaluaciones: List<EvaluacionClinica>? = emptyList(),
    val dispensaciones: List<DispensacionOptica>? = emptyList(),
    val pagos: List<Pago>? = emptyList(),
    val serviciosExtra: List<ServicioExtra>? = emptyList()
)

object BackupDataSerializer : KSerializer<BackupData> {
    // Tries primary name first, then alternates for dispensaciones/serviciosExtra
}
```

### Json instance for BackupDelegate

```kotlin
val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit — C1 | Each refactored catch block | Manual verification via code review; existing behaviour tests still pass |
| Unit — C3 | MembershipRepository CRUD + sync | MockK for SupabaseClient, `runTest`, assert Result types |
| Unit — C3 | OptoRepository DB + sync | MockK for all deps, verify DAO calls + sync state updates |
| Unit — C3 | PacienteRepository CRUD | Room in-memory (Robolectric), Flow observation via `.first()` |
| Unit — C3 | DispensacionRepository CRUD + stock | Room in-memory (Robolectric), transactional patterns |
| Unit — C2 | BackupData serialization roundtrip | Pure kotlin test, encode → decode, verify old name compatibility |

## Migration / Rollout

**No data migration required.** Room column names are unchanged (Room uses its own `@ColumnInfo`, not serialization annotations). Retrofit endpoints remain identical (`@SerialName` values match the original `@SerializedName` values).

**BackupData backward compatibility**: the custom serializer reads old JSON files with `ordenes`/`ventas` keys for `dispensaciones`, and `servicios`/`otrosServicios` for `serviciosExtra`. New exports will use the primary `@SerialName` values only.

**Rollback plan**:
1. Revert `build.gradle.kts` and `libs.versions.toml` changes
2. Restore `@SerializedName` + remove `@Serializable` in entity files
3. Re-add `provideGson()` in `DatabaseModule.kt`
4. Delete test files
5. Revert catch blocks manually if behavioral bugs appear

## Open Questions

- None — all blocking decisions resolved above.
