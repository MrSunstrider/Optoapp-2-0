# Spec: evaluacion-screen

## Behavior to Preserve

### NuevaEvaluacionScreen navigation and form

- GIVEN user is on NuevaEvaluacionScreen
- WHEN user fills form fields (paciente, optometra, observaciones)
- THEN form state SHALL be preserved in ViewModel StateFlow without data loss
- AND navigation callbacks SHALL fire with identical parameters as before

- GIVEN user taps "Generar PDF"
- WHEN form is valid
- THEN the system SHALL invoke RecetaEvaluacionPdfGenerator with the same parameters (evaluacion data, receta sections)
- AND the generated PDF SHALL contain identical content and formatting

### PDF Generation sections

- GIVEN evaluacion data with multiple receta sections
- WHEN RecetaEvaluacionPdfGenerator builds the PDF
- THEN each section (titulo, diagnostico, tratamiento, observaciones) SHALL render in the same order and format
- AND the output file name pattern SHALL remain unchanged

- GIVEN empty or partial evaluacion fields
- WHEN PDF generation is triggered
- THEN the system SHALL handle missing fields identically to current behavior (show placeholders or skip sections)

## Acceptance Criteria

- [ ] NuevaEvaluacionScreen composable extracts logic to presenter but renders identically
- [ ] ViewModel exposes same StateFlow states; no new or removed states
- [ ] Form validation rules produce same valid/invalid outcomes for all field combinations
- [ ] RecetaEvaluacionPdfGenerator refactored into builder modules produces byte-identical PDF for same input
- [ ] Navigation callbacks (onNavigate, onBack) receive same arguments
- [ ] All existing characterization tests pass unchanged
