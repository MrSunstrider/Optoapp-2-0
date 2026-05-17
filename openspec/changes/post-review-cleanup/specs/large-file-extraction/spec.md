# Large File Extraction Specification

## Purpose

Reduce oversized files (300+ lines) below the threshold by extracting cohesive units into separate files, applying single-responsibility principle without changing external behavior.

## Requirements

### Requirement: All target files under 300 lines

The following files MUST be reduced below 300 lines each via extraction. Extracted code MUST remain in the same package/module.

| File | Current Lines | Target | Extraction Strategy |
|------|--------------|--------|-------------------|
| EvaluacionViewModel.kt | 564 | <300 | Extract helpers/delegates |
| EvaluacionFormSections.kt | 542 | <300 | Extract composable sections |
| MainDrawerScreen.kt | 443 | <300 | Extract drawer sections |
| NuevaDispensacionScreen.kt | 440 | <300 | Extract form sections |
| RecetaPdfBuilder.kt | 435 | <300 | Extract PDF section builders |

#### Scenario: EvaluacionViewModel extracted below threshold

- GIVEN EvaluacionViewModel.kt is 564 lines
- WHEN extraction is complete
- THEN the file MUST be under 300 lines
- AND extracted code MUST be in a new file within the same package (e.g., `EvaluacionHelpers.kt` or `EvaluacionDelegate.kt`)

#### Scenario: Composable sections extracted from EvaluacionFormSections

- GIVEN EvaluacionFormSections.kt is 542 lines
- WHEN extraction is complete
- THEN the original file MUST be under 300 lines
- AND each extracted section MUST be a self-contained composable in its own file

#### Scenario: MainDrawerScreen sections extracted

- GIVEN MainDrawerScreen.kt is 443 lines
- WHEN extraction is complete
- THEN the file MUST be under 300 lines
- AND drawer sections MUST be in separate composable files

### Requirement: No behavioral changes during extraction

Extraction MUST be purely structural. No logic, parameters, or return types SHALL change. The system MUST compile and all existing tests MUST pass without modification.

#### Scenario: Compilation succeeds after extraction

- GIVEN all extractions are complete
- WHEN `./gradlew assembleDebug` is run
- THEN it MUST succeed with zero errors

#### Scenario: Existing tests pass unchanged

- GIVEN all extractions are complete
- WHEN `./gradlew test` is run
- THEN all previously passing tests MUST still pass
- AND no test file was modified as part of extraction

### Requirement: Extracted files follow naming convention

Extracted files MUST follow existing project naming patterns. Helper/delegate files use the `{Feature}Helper` or `{Feature}Delegate` pattern. Composable section files use the `{Feature}{Section}Section` pattern.

#### Scenario: Naming convention followed for new files

- GIVEN a new file is created from extraction
- WHEN the file is reviewed
- THEN its name MUST follow the established pattern for its type
- AND its package declaration MUST match the source file's package
