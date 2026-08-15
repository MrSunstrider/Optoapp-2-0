# Delta: paciente-ux (PR-U3)

## MODIFIED Requirements

### Requirement: Paciente form uses OptoFormShell

`NuevoPacienteScreen` MUST render through `OptoFormShell` with Cancel/Guardar bottom actions (`FormActions`). Ad-hoc Scaffold + Toast-only error UX MAY remain for load failures until FormErrorHandler lands; save path SHOULD prefer Snackbar when shell exposes it.

#### Scenario: Shell wraps create and edit

- GIVEN user opens nuevo paciente or editar paciente
- WHEN the screen renders
- THEN title and back navigation come from `OptoFormShell`
- AND bottom bar shows Cancel + Guardar

### Requirement: Paciente form three sections

Form fields MUST be grouped into three labeled sections:

1. **Identidad** — nombre, DNI, sexo, fecha nacimiento, edad
2. **Contacto** — teléfono, email, dirección, distrito
3. **Clínica / contexto** — ocupación, acompañante, hobbies, historia optométrica, fecha creación

#### Scenario: Section headers visible

- GIVEN create or edit paciente
- WHEN scrolling the form
- THEN the three section titles are present
