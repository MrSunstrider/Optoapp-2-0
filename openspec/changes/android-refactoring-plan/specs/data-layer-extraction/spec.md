# Spec: data-layer-extraction

## Behavior to Preserve

### OptoDatabase DAO extraction

- GIVEN OptoDatabase.kt currently defines DAOs inline
- WHEN DAOs are extracted to per-entity files (e.g., EvaluacionDao.kt, PacienteDao.kt)
- THEN each extracted DAO SHALL retain identical method signatures, annotations, and query SQL
- AND Room schema version SHALL NOT change
- AND all @Query, @Insert, @Update, @Delete annotations SHALL produce identical generated implementations

- GIVEN OptoDatabase references all DAOs via abstract methods
- WHEN DAOs are extracted to separate files
- THEN @Database(entities = [...], version = N) SHALL remain unchanged
- AND Hilt module bindings SHALL provide the same DAO instances

### Entity migration from Daos.kt/Entities.kt

- GIVEN entities currently in Daos.kt or Entities.kt
- WHEN migrated to entity-specific packages (dispensacion/, paciente/, evaluacion/)
- THEN @Entity(tableName = "...") SHALL use identical table names
- AND all @ColumnInfo annotations SHALL use identical column names
- AND @PrimaryKey strategies SHALL remain unchanged

- GIVEN code imports entities from old locations
- WHEN entities move to new packages
- THEN all import statements across the codebase SHALL be updated
- AND no compile errors SHALL be introduced

### Room query behavior

- GIVEN existing DAO queries
- WHEN DAOs are extracted to separate files
- THEN query results SHALL be identical for all inputs
- AND Flow return types SHALL emit the same sequences
- AND suspend functions SHALL maintain identical coroutine semantics

## Acceptance Criteria

- [ ] Each extracted DAO compiles with identical annotations and SQL
- [ ] Room schema version stays at current value (no migration needed)
- [ ] All entity table names and column names are unchanged
- [ ] Hilt DI modules provide extracted DAOs correctly
- [ ] Existing Room-generated code compiles without changes
- [ ] `./gradlew assembleDebug` succeeds
