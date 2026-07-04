# Tasks: Fix age field and add validation in patient form

## Phase 1: Core Fix (Age field wiring)
- [x] 1.1 Modify `NuevoPacienteScreen.kt:138` — Replace `onEdadChange = {}` with `onEdadChange = { edad = it; fechaNacimiento = "" }`

## Phase 2: Validation
- [x] 2.1 `PacienteFormSections.kt` — Age field: limit to 3 digits, reject values > 120
- [x] 2.2 `PacienteFormSections.kt` — Birth date field: add `isError` + `supportingText` for invalid day/month/year

## Phase 3: Clipboard Compilation Fix
- [x] 3.1 `ConfigSyncDiagnosticsCard.kt` — Replace Compose clipboard API with Android `ClipboardManager` + `ClipData`

## Phase 4: Tests
- [x] 4.1 Add `edadField_rejectsValueOver120` — verify filter blocks > 120
- [x] 4.2 Add `edadField_limitsToThreeDigits` — verify max 3 chars
- [x] 4.3 Add `fechaNacField_showsErrorForInvalidMonth` — verify "Mes debe ser 1-12"
- [x] 4.4 Add `fechaNacField_showsErrorForInvalidDay` — verify "Día debe ser 1-31"
- [x] 4.5 Add `fechaNacField_showsNoErrorForValidDate` — verify valid input accepted

## Phase 5: Verification
- [x] 5.1 `./gradlew :optoapp:compileDebugKotlin` — compilation passes
- [x] 5.2 `./gradlew :optoapp:testDebugUnitTest` — unit tests pass
