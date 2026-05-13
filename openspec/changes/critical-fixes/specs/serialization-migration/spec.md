# Serialization Migration Specification

## Purpose

Define requirements for migrating 27 `@SerializedName` annotations to `@SerialName` and removing the Gson provider, completing the transition to Kotlinx Serialization.

## Requirements

### Requirement: @SerializedName MUST be replaced with @SerialName

All 27 instances of `@SerializedName` in entity data classes MUST be replaced with `@SerialName` from `kotlinx.serialization`. The serialized field names MUST remain identical after migration.

#### Scenario: PacienteEntity field mapping preserved

- GIVEN `PacienteEntity` has `@SerializedName("nombre_paciente") val nombre: String`
- WHEN migrated to `@SerialName("nombre_paciente")`
- THEN the JSON key MUST remain `"nombre_paciente"`
- AND deserialization from existing API responses MUST produce identical objects

#### Scenario: EvaluacionEntity field mapping preserved

- GIVEN `EvaluacionEntity` has `@SerializedName("evaluacion_id")`
- WHEN migrated to `@SerialName("evaluacion_id")`
- THEN all 27 field mappings MUST produce identical JSON keys

#### Scenario: DispensacionEntity field mapping preserved

- GIVEN `DispensacionEntity` has fields annotated with `@SerializedName`
- WHEN migrated
- THEN each field's serialized name MUST be unchanged

### Requirement: @Serializable annotation MUST be added

All entity data classes affected by the migration MUST have the `@Serializable` annotation added. This is required for Kotlinx Serialization to generate serializers.

#### Scenario: Serializable annotation present

- GIVEN `PacienteEntity` is being migrated
- WHEN the migration is complete
- THEN the class MUST have `@Serializable` annotation

#### Scenario: Nested data classes also annotated

- GIVEN `OptoRepository` contains data classes used for serialization
- WHEN migration is complete
- THEN all data classes participating in serialization MUST have `@Serializable`

### Requirement: Gson provider MUST be removed

`provideGson()` in `DatabaseModule.kt` MUST be removed. No Gson dependency MUST remain in the app module's runtime classpath for these entity files.

#### Scenario: DatabaseModule no longer provides Gson

- GIVEN `DatabaseModule.kt` contains `fun provideGson(): Gson`
- WHEN migration is complete
- THEN the `provideGson()` function MUST be removed
- AND no `@Provides` annotation referencing Gson MUST exist in that module

#### Scenario: build.gradle.kts updated

- GIVEN `app/build.gradle.kts` has kotlinx-serialization dependency
- WHEN migration is complete
- THEN the `kotlinx-serialization-json` dependency MUST be present
- AND Gson dependency SHOULD be removed if no other code references it

### Requirement: Room and Retrofit compatibility preserved

After migration, Room database operations and Retrofit API calls using the migrated entities MUST continue to function correctly.

#### Scenario: Room insert/read round-trip

- GIVEN a migrated `PacienteEntity` with `@SerialName` annotations
- WHEN inserted into Room and read back
- THEN all field values MUST be identical (Room uses its own column mapping, unaffected by serialization annotations)

#### Scenario: Retrofit deserialization works

- GIVEN a Retrofit response body deserializes into a migrated entity
- WHEN the API returns JSON with the original field names
- THEN Kotlinx Serialization MUST map fields correctly using `@SerialName`

## Non-Goals

- Migrating Gson usages in non-entity data classes
- Migrating to a different HTTP client
- Changing Room's column mapping strategy
- Removing Gson dependency entirely (only remove the provider for these entities)
