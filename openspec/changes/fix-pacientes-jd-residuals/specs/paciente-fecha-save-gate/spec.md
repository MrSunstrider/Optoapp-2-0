# Paciente Fecha Save Gate Specification

## Purpose

Ensure birth-date (`fechaNacimiento`) validation shown in the patient form is enforced at save time and that persistence uses strict calendar parsing consistent with validation — preventing lenient date adjustment or silent null persistence when the UI shows an error.

## ADDED Requirements

### Requirement: Save MUST block invalid non-blank birth date

When saving a patient from the new/edit patient screen, if `fechaNacimiento` input is non-blank and `validateFechaNacimiento(fechaNacimiento)` returns a non-null error message, the system MUST NOT invoke persistence. The user MUST receive visible feedback (e.g. toast) indicating the date is invalid. Required fields (`nombreCompleto`, `edad`, `telefono`) gating MUST remain unchanged.

#### Scenario: Partial birth date blocks save

- GIVEN `fechaNacimiento = "3102202"` (fewer than 8 digits)
- AND `validateFechaNacimiento` returns an error for incomplete input
- WHEN the user triggers save
- THEN `savePaciente` is NOT called
- AND the user sees invalid-date feedback

#### Scenario: Invalid calendar date blocks save

- GIVEN `fechaNacimiento = "31022020"` (invalid day for February)
- AND `validateFechaNacimiento` returns "Fecha inválida" or equivalent
- WHEN the user triggers save
- THEN `savePaciente` is NOT called
- AND no patient row is written with a leniently adjusted date

#### Scenario: Blank birth date allows save

- GIVEN `fechaNacimiento` is blank or whitespace-only
- AND required fields are valid
- WHEN the user triggers save
- THEN save proceeds
- AND `fechaNacimiento` is persisted as null/absent

#### Scenario: Valid birth date allows save

- GIVEN `fechaNacimiento = "15061990"` (8 valid digits)
- AND `validateFechaNacimiento` returns null
- WHEN the user triggers save
- THEN save proceeds
- AND the persisted date matches the strict calendar interpretation of the input

### Requirement: Birth-date parse MUST be strict and shared with validation

Birth-date persistence for patient save MUST use the same strict calendar rules as `validateFechaNacimiento`. Parsing MUST reject invalid month/day combinations and MUST NOT use lenient resolver behavior (e.g. adjusting `31022020` to a nearby valid date). A shared strict parse path MUST be used for both validation display and save persistence.

#### Scenario: Strict parse rejects lenient-adjustment candidates

- GIVEN digit input `"31022020"`
- WHEN strict birth-date parse runs for save
- THEN parse returns failure/null
- AND no `LocalDate` with March 2 2020 is produced via lenient adjustment

#### Scenario: Strict parse accepts valid input

- GIVEN digit input `"15061990"`
- WHEN strict birth-date parse runs
- THEN parse succeeds with calendar date 1990-06-15

#### Scenario: UI error and save gate stay aligned

- GIVEN input that produces a visible fecha error in the form
- WHEN save is attempted
- THEN save is blocked
- AND the persisted value MUST NOT bypass the displayed error state
