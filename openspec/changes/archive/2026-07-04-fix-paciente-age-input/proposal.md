# Proposal: Fix age field not editable in patient form

## Intent

The age field (`Edad *`) in the new patient screen renders as editable but discards all input because `onEdadChange = {}` is a no-op. Users typing an age see no change, creating a broken UX. The birth date field correctly auto-calculates age, but users sometimes know the age directly and should be able to enter it.

## Scope

### In Scope
- Fix `onEdadChange` lambda in `NuevoPacienteScreen.kt` to update `edad` state and clear `fechaNacimiento`
- Ensure birth date → age auto-calculation continues working (existing behavior, no regression)
- Ensure age → birth date clear is implemented (new mutual-exclusion behavior)
- Add age input validation in `PacienteFormSections.kt`: max 3 digits, values 0-120 only
- Add birth date validation in `PacienteFormSections.kt`: visual error feedback (isError + supportingText) for day > 31, month > 12, year out of range, or impossible date
- Fix pre-existing compilation error in `ConfigSyncDiagnosticsCard.kt` (Unresolved reference 'setText')

### Out of Scope
- No Supabase schema, RLS, or migration changes
- No ViewModel changes — state is local `remember` in the composable
- No spec-level capability changes (pure bug fix)

## Capabilities

### New Capabilities
None — pure bug fix, no new capability introduced.

### Modified Capabilities
None — no existing spec requirements change.

## Approach

Three changes in a single change set:

1. **Age field fix**: Replace `onEdadChange = {}` with `{ edad = it; fechaNacimiento = "" }` in `NuevoPacienteScreen.kt:138`
2. **Field validation in `PacienteFormSections.kt`**: Age input limited to 3 digits + range 0-120; birth date shows `isError` + `supportingText` for invalid day/month/year (computed via `remember`)
3. **Clipboard fix**: Replace Compose clipboard API (`LocalClipboard`/`LocalClipboardManager`) with Android `ClipboardManager` + `ClipData` in `ConfigSyncDiagnosticsCard.kt`

No ViewModel, repository, or database layer touched.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../ui/screens/NuevoPacienteScreen.kt:138` | Modified | Replace `onEdadChange = {}` with `{ edad = it; fechaNacimiento = "" }` |
| `optoapp/src/androidTest/.../PacienteFlowTest.kt` | Optional (test add) | Add test for edad input updating state and clearing birth date |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Regression in birth date → age calc | Low | Existing auto-calculation code is untouched; only the nuevo lambda added |
| Accidental double-clear on birth-date-edit | Low | Birth date change recalculates age and does NOT touch `onEdadChange` — no conflict |
| Clearing `fechaNacimiento` on age edit after birth date set | Low (desired) | Intended mutual-exclusion behavior |

## Rollback Plan

Revert `NuevoPacienteScreen.kt:138` — set `onEdadChange = {}` back. No database or schema changes to undo.

## Dependencies

None.

## Success Criteria

- [x] User can type digits in the age field and see them reflected in the UI
- [x] Typing a birth date clears any manually entered age and recalculates from birth date
- [x] Typing an age clears any previously entered birth date
- [x] Age input limited to 3 digits; values > 120 rejected
- [x] Birth date shows error text for invalid day/month/year
- [x] `ConfigSyncDiagnosticsCard.kt` compiles without error
- [x] Unit tests pass (`./gradlew :optoapp:testDebugUnitTest --stacktrace`)
- [ ] Android instrumentation tests in `PacienteFlowTest` continue passing (requires emulator/device)
