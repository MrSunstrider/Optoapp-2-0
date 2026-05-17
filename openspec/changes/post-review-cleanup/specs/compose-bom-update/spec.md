# Compose BOM Update Specification

## Purpose

Update Jetpack Compose BOM from 2024.02.02 to the latest stable version, ensuring compilation and test compatibility across the Android app.

## Requirements

### Requirement: BOM version updated to latest stable

The Compose BOM version in `build.gradle.kts` (app) MUST be updated from `2024.02.02` to the latest stable release (`2024.09.00` or newer stable at time of implementation).

#### Scenario: BOM version string updated

- GIVEN the current Compose BOM is 2024.02.02
- WHEN build.gradle.kts is updated
- THEN the BOM version MUST be the latest stable release
- AND all Compose dependency versions MUST be resolved through the BOM (no hardcoded Compose versions)

#### Scenario: No hardcoded Compose versions bypass BOM

- GIVEN the BOM is updated
- WHEN a search for hardcoded `androidx.compose` versions is performed in build files
- THEN zero hardcoded Compose library versions MUST exist (BOM manages all)

### Requirement: Compilation succeeds

The Android project MUST compile without errors after the BOM update.

#### Scenario: Debug build succeeds

- GIVEN the BOM is updated to latest stable
- WHEN `./gradlew assembleDebug` is run
- THEN the build MUST complete successfully

#### Scenario: No deprecated API warnings introduced

- GIVEN the BOM is updated
- WHEN the build completes
- THEN no NEW deprecation warnings related to Compose APIs MUST be introduced

### Requirement: Existing tests pass

All existing Android unit tests MUST pass after the BOM update. No test modifications should be needed unless Compose test APIs changed in the new version.

#### Scenario: Unit tests pass after update

- GIVEN the BOM is updated
- WHEN `./gradlew testDebugUnitTest` is run
- THEN all tests MUST pass
- AND test count MUST remain the same (no tests silently dropped)

### Requirement: Rollback plan

If the update causes compilation failures or test regressions that cannot be resolved within the change scope, the BOM MUST be reverted to 2024.02.02.

#### Scenario: Revert on unrecoverable failure

- GIVEN the BOM update causes build failures
- WHEN the failures cannot be fixed without scope expansion
- THEN the BOM version MUST be reverted to 2024.02.02
- AND the change MUST document which failures prevented the update
