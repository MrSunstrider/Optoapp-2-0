# Design: Fix Paciente Multi-Tenant WhatsApp Templates

## Technical Approach

Replace two hardcoded strings in `PacienteWhatsAppMenu` composable with tenant-specific values. Optica name flows from existing `OpticaHeaderUi.nombreOptica`. Business hours come from a new Room entity `OpticaSettingsEntity` backed by the existing Supabase `optica_settings` table (jsonb `config_json` → key `business_hours`). Both values are passed as composable parameters from `DetallePacienteScreen`.

## Architecture Decisions

| Decision | Option | Tradeoff | Choice |
|----------|--------|----------|--------|
| Optica name to composable | A: Composable parameter | Explicit deps, testable | **A** — follows existing pattern (PDF generation at L118 reads `nombreOptica` the same way) |
| | B: Read VM inside composable | Implicit coupling | Rejected — anti-pattern, hard to test |
| Business hours source | A: Room entity + DAO | Offline cache, existing pattern | **A** — `optica_settings` table exists in Supabase; Room entity mirrors `ConfiguracionFinancieraEntity` pattern |
| | B: Network-only | No offline support | Rejected — violates offline-first architecture |
| `horarioAtencion` location | Add to `OpticaHeaderUi` | Single source, VM already reacts to opticaId | **Chosen** — avoids new ViewModel for one field; reuses existing reactive pipeline |
| | Separate ViewModel | Isolated concern | Rejected — overkill, adds wiring in screen |

## Data Flow

```
Supabase optica_settings         OpticaSettingsDataSource
  config_json (jsonb) ──fetch──→  fetchOpticaSettings(opticaId)
                                         │
Room OpticaSettingsEntity                  │ opticaSettingsRepo
  opticaId PK + configJson ◄──upsert──────┘  (new method in
         │                                   MembershipRepository)
         │ Flow<OpticaSettingsEntity?>
         ▼
OpticaHeaderViewModel ──→ OpticaHeaderUi(nombreOptica, fiscalEtiqueta, horarioAtencion)
         │
         │ uiState.value
         ▼
DetallePacienteScreen ──nombreOptica + horarioAtencion──→ PacienteWhatsAppMenu
         │
         ▼
  Message: "Hola Juan, te saludamos de Vision Center SAS. ..."
  Delivery: "sus lentes ya están listos, puede venir a recogerlos en este horario: Lunes a Viernes de 9am a 7pm"
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/opticasettings/OpticaSettingsEntity.kt` | Create | `@Entity(tableName="optica_settings")` with `opticaId` PK + `configJson: String` |
| `data/opticasettings/OpticaSettingsDao.kt` | Create | `getByOpticaId()` (Flow), `getByOpticaIdOnce()` (suspend), `upsert()` |
| `data/OptoDatabase.kt` | Modify | v41→v42, register entity + abstract DAO method + MIGRATION_41_42 |
| `data/OptoDatabaseMigrations.kt` | Modify | Add `MIGRATION_41_42` creating `optica_settings` table |
| `data/membership/OpticaSettingsDataSource.kt` | Modify | Add `fetchOpticaSettings(opticaId)` — reads `config_json` from Supabase `optica_settings` |
| `data/MembershipRepository.kt` | Modify | Expose `fetchOpticaSettings()`, `getOpticaSettingsFlow()`, `upsertOpticaSettings()` |
| `viewmodel/OpticaHeaderViewModel.kt` | Modify | Add `horarioAtencion` to `OpticaHeaderUi`; read from `OpticaSettingsDao` reactive flow |
| `ui/components/paciente/PacienteWhatsAppActions.kt` | Modify | Add `nombreOptica: String`, `horarioAtencion: String` params; replace hardcoded strings |
| `ui/screens/DetallePacienteScreen.kt` | Modify | Pass `nombreOptica` + `horarioAtencion` from `opticaHeaderVm.uiState` to menu |
| `di/DatabaseModule.kt` | Modify | Provide `OpticaSettingsDao` |

## Key Contracts

```kotlin
// OpticaSettingsEntity
@Entity(tableName = "optica_settings")
data class OpticaSettingsEntity(
    @PrimaryKey val opticaId: String,
    val configJson: String = "{}"
)

// OpticaSettingsDao
@Dao
interface OpticaSettingsDao {
    @Query("SELECT * FROM optica_settings WHERE opticaId = :opticaId")
    fun getByOpticaId(opticaId: String): Flow<OpticaSettingsEntity?>
    @Query("SELECT * FROM optica_settings WHERE opticaId = :opticaId")
    suspend fun getByOpticaIdOnce(opticaId: String): OpticaSettingsEntity?
    @Upsert
    suspend fun upsert(settings: OpticaSettingsEntity)
}

// OpticaHeaderUi (modified)
data class OpticaHeaderUi(
    val nombreOptica: String = "Óptica",
    val fiscalEtiqueta: String = "Sin documento fiscal",
    val horarioAtencion: String = ""
)

// Composable signature (modified)
@Composable
fun PacienteWhatsAppMenu(
    expanded: Boolean,
    paciente: Paciente,
    evaluaciones: List<EvaluacionClinica>,
    onDismiss: () -> Unit,
    onSendMessage: (mensaje: String) -> Unit,
    nombreOptica: String,        // NEW
    horarioAtencion: String = "", // NEW
)
```

## Fallback Rules

| Field | When Missing | Behavior |
|-------|-------------|----------|
| `nombreOptica` | blank/null | Use `"Su óptica"` — `nombreOptica.ifBlank { "Su óptica" }` |
| `horarioAtencion` | blank/null | Omit hours sentence from "Entrega de Lentes" — `if (horarioAtencion.isNotBlank()) { "...horario: $horarioAtencion" }` |

String building uses Kotlin `buildString` or conditional concatenation — no raw `+` operator for the conditional hours sentence.

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit — DAO | `OpticaSettingsDao` CRUD | `Room.inMemoryDatabaseBuilder(OptoDatabase::class.java).build()` per `ConfiguracionFinancieraDaoTest` pattern |
| Unit — DataSource | `fetchOpticaSettings` deserializes `config_json` | Mock Supabase, verify `business_hours` extraction |
| Unit — ViewModel | `OpticaHeaderUi` reflects per-opticaId `horarioAtencion` | Fake `OpticaSettingsDao`, assert `uiState` emission |
| Unit — Composable | Templates use params, not hardcoded strings | Compose test: render menu with known `nombreOptica`=`"TestOptica"`, assert message contains `"TestOptica"` |
| Unit — Composable | Fallback: empty name → `"Su óptica"`, empty hours → sentence omitted | Parametrized test with null/empty values |
| Migration | `MIGRATION_41_42` creates `optica_settings` table | `MigrationTestHelper` verifying table/schema, following `OptoDatabaseMigrationTest` pattern |

## Migration / Rollout

- Room migration `41→42`: `CREATE TABLE IF NOT EXISTS optica_settings (opticaId TEXT NOT NULL PRIMARY KEY, configJson TEXT NOT NULL DEFAULT '{}')`
- No Supabase migration needed — table already exists
- Zero rows → `horarioAtencion` stays empty → hours sentence omitted (graceful degradation)
- Rollback: revert to v41, delete entity/DAO; composable params can stay (callers pass empty strings)
