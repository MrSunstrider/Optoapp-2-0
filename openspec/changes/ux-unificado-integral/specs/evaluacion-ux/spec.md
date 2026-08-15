# Delta: evaluacion-ux (PR-U3)

## MODIFIED Requirements

### Requirement: Evaluación shows PatientContextCard

`NuevaEvaluacionScreen` MUST show `PatientContextCard` with paciente nombre (and fecha when available) above the step indicator. The existing 5-step wizard (Anamnesis → Examen Visual → Refracción → Contactología → Cierre) MUST remain intact.

#### Scenario: Context card on create and edit

- GIVEN evaluación for a paciente
- WHEN the screen renders at any wizard step
- THEN `PatientContextCard` is visible with the patient name

### Requirement: Unified CTAs

Save/finalize MUST remain reachable via top Check and bottom wizard controls (Anterior/Siguiente/Finalizar). Prefer migrating the Scaffold shell to `OptoFormShell` when it does not break `StepIndicator` layout.
