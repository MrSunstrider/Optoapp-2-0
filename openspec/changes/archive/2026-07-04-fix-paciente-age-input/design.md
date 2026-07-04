# Design: Fix age field and add validation in patient form

## Technical Approach

Three changes in a single change set:

1. **Age field fix**: Replace `onEdadChange = {}` (no-op) with `onEdadChange = { edad = it; fechaNacimiento = "" }` in `NuevoPacienteScreen.kt`. This wires the callback so user typing updates state, and implements mutual exclusion — age and birth date are alternative sources of truth.

2. **Age validation (0-120)**: In `PacienteFormSections.kt`, the age field's `onValueChange` now limits input to 3 digits and rejects values > 120. Users cannot type "551" or any unrealistic age.

3. **Birth date validation**: In `PacienteFormSections.kt`, a `remember(fechaNacimiento)` block computes a validation error string when all 8 digits are entered. Errors: month not 1-12, day not 1-31, year outside 1900-2100, impossible date (e.g. Feb 30). Error is shown via `isError` + `supportingText` on the field.

4. **Clipboard fix**: `ConfigSyncDiagnosticsCard.kt` had `Unresolved reference 'setText'` with the Compose `LocalClipboard`/`LocalClipboardManager` API. Replaced with direct Android `ClipboardManager` + `ClipData` via `ctx.getSystemService`.

## Architecture Decisions

### Decision: Client-side validation in form component

**Choice**: Validate age and birth date directly inside `PacienteFormSections` using `remember` blocks, not in the ViewModel.
**Rationale**: These are pure UI constraints (max digits, range checks, date validity). They don't require business logic, database access, or async operations. Keeping validation in the composable avoids adding complexity to the ViewModel and keeps the form self-contained.

### Decision: Inline date validation (no new utility)

**Choice**: Compute birth date error directly in `PacienteFormSections` rather than creating a `DateValidationResult` sealed class in `DateUtils`.
**Rationale**: The logic is straightforward (extract digits, check ranges, try LocalDate.of). A separate sealed class would add abstraction without current reuse value. Can be extracted later if needed.

### Decision: Android clipboard API over Compose

**Choice**: Use `ctx.getSystemService(Context.CLIPBOARD_SERVICE)` instead of `LocalClipboard`/`LocalClipboardManager`.
**Rationale**: The Compose clipboard API (`setText`) had an unresolved reference error with this Kotlin/Compose version combination. The Android API is stable, well-documented, and avoids the abstraction issue.

## Data Flow

```
Age input:
  User types digits
  → PacienteFormSections: length ≤ 3 AND digits only AND (empty or 0-120)
  → onEdadChange(digits)
  → NuevoPacienteScreen: edad = digits; fechaNacimiento = ""
  → UI recomposes

Birth date input:
  User types 8 digits
  → PacienteFormSections: filter digits, take(8)
  → onFechaNacimientoChange(digits)
  → NuevoPacienteScreen: fechaNacimiento = digits; edad = calculated
  → UI recomposes with validation error (if any) via supportingText
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/.../ui/screens/NuevoPacienteScreen.kt:138` | Modify | Replace `onEdadChange = {}` with `onEdadChange = { edad = it; fechaNacimiento = "" }` |
| `optoapp/.../ui/components/paciente/PacienteFormSections.kt` | Modify | Age: max 3 digits + range 0-120; Birth date: isError + supportingText for invalid day/month/year |
| `optoapp/.../ui/components/config/ConfigSyncDiagnosticsCard.kt` | Modify | Replace Compose clipboard API with Android `ClipboardManager` + `ClipData` |
| `optoapp/src/androidTest/.../PacienteFlowTest.kt` | Modify | Add tests for age validation, birth date validation, clipboard fix |

## Interfaces / Contracts

No interface changes. `PacienteFormSections` parameters unchanged — validation is computed internally via `remember`. `ConfigSyncDiagnosticsCard` signature unchanged.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (new) | `edadField_rejectsValueOver120` | Type "150", assert captured value is not "150" (filtered) |
| Unit (new) | `edadField_limitsToThreeDigits` | Type "12345", assert captured ≤ 3 chars |
| Unit (new) | `fechaNacField_showsErrorForInvalidMonth` | Type "01131990" (month 13), assert supporting text "Mes debe ser 1-12" |
| Unit (new) | `fechaNacField_showsErrorForInvalidDay` | Type "32011990" (day 32), assert supporting text "Día debe ser 1-31" |
| Unit (new) | `fechaNacField_showsNoErrorForValidDate` | Type "15061990", assert formatted display shows correctly |
| Existing | All prior tests | No regression |

## Migration / Rollout

No migration required. Pure UI bug fix with no data or schema changes. The clipboard fix resolves a pre-existing compilation error unrelated to the patient form.
