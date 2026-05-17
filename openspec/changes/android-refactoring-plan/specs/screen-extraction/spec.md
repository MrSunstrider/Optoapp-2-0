# Spec: screen-extraction

## Behavior to Preserve

### ConfiguracionScreen sections

- GIVEN ConfiguracionScreen.kt currently renders all configuration sections in one composable
- WHEN sections are extracted into separate composables (e.g., ProfileSection, NotificationsSection, ThemeSection)
- THEN each section SHALL display identical UI elements with identical data
- AND section navigation/visibility logic SHALL produce identical results
- AND all user interactions (toggles, inputs, saves) SHALL function identically

### DetallePacienteScreen tabs and components

- GIVEN DetallePacienteScreen.kt renders patient details with tabs
- WHEN tabs and components are extracted into separate composables
- THEN each tab SHALL display identical data for the same patient
- AND tab switching SHALL preserve the same scroll position and state
- AND action buttons SHALL trigger identical ViewModel calls

### NuevaDispensacionScreen stock logic

- GIVEN NuevaDispensacionScreen.kt mixes stock logic with UI
- WHEN stock logic is extracted to a separate layer (presenter/use-case)
- THEN stock calculations SHALL produce identical results for the same inputs
- AND stock display (available, reserved, dispensed) SHALL show identical values
- AND dispensacion submission SHALL pass identical parameters to the repository

### User interaction preservation

- GIVEN any extracted screen
- WHEN user performs a sequence of interactions (scroll, tap, input, navigate back)
- THEN the complete interaction flow SHALL produce identical side effects and final state

## Acceptance Criteria

- [ ] ConfiguracionScreen extracted sections render identical UI for same data
- [ ] DetallePacienteScreen tabs show identical content and support identical actions
- [ ] NuevaDispensacionScreen stock calculations produce identical values
- [ ] All navigation callbacks receive identical arguments
- [ ] Scroll positions and focus states are preserved
- [ ] No new recompositions introduced compared to baseline
