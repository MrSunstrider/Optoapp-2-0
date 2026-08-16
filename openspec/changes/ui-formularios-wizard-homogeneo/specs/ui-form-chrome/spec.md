# Spec — ui-form-chrome

## REQ-1 — Orden de campos en Nuevo Paciente
El formulario de paciente DEBE presentar, como primera sección visible ("Registro"),
el campo "N° Historia Optométrica" (con acción "Sugerir HO") y el botón "Fecha de
Registro", antes de la sección "Identidad".

### Scenario: campos de registro arriba
- **WHEN** se abre "Nuevo Paciente"
- **THEN** "N° Historia Optométrica" y "Fecha de Registro" aparecen antes que "Nombre Completo"

## REQ-2 — Sin gap por doble inset
`OptoFormShell` NO DEBE aplicar `navigationBarsPadding` al contenido cuando la pantalla
provee su propia `bottomBar` (que ya lo aplica). Cuando no hay `bottomBar`, el contenido
DEBE conservar `navigationBarsPadding`.

### Scenario: paciente con bottomBar
- **WHEN** `OptoFormShell` recibe `bottomBar != null`
- **THEN** el inset de barra de navegación se aplica una sola vez (en la bottomBar)

### Scenario: información financiera sin bottomBar
- **WHEN** `OptoFormShell` recibe `bottomBar == null`
- **THEN** el contenido conserva `navigationBarsPadding`

## REQ-3 — Barra de wizard sin recuadro
Las barras inferiores de "Nueva Evaluación" y "Nueva Dispensación" NO DEBEN envolver los
botones en un `Surface` elevado; DEBEN renderizar solo los botones con
`navigationBarsPadding`.

### Scenario: sin contenedor elevado
- **WHEN** se muestra la barra inferior de un wizard
- **THEN** no existe `Surface(tonalElevation > 0)` envolviendo los botones

## REQ-4 — Cabecera de paso con título, sin círculos
Los wizards DEBEN mostrar el título del paso actual mediante `WizardStepHeader`, no el
`StepIndicator` de círculos numerados.

### Scenario: título del paso
- **WHEN** el usuario está en el paso índice 2 de `["Anamnesis","Examen Visual","Refracción",...]`
- **THEN** `wizardStepTitle(labels, 2)` == "Refracción"

### Scenario: progreso legible
- **WHEN** el usuario está en el paso índice 2 de 5
- **THEN** `wizardStepProgress(5, 2)` == "Paso 3 de 5"

### Scenario: índice fuera de rango es seguro
- **WHEN** `currentStep` es negativo o mayor al último
- **THEN** `wizardStepTitle` devuelve "" y `wizardStepProgress` se mantiene dentro de 1..totalSteps

## REQ-5 — Homogeneidad
Evaluación y Dispensación DEBEN usar el MISMO componente `WizardStepHeader` y el MISMO
patrón de barra inferior (sin `Surface`).
