# Proposal — UI de formularios homogénea (paciente + wizards)

## Intent

Corregir cuatro fricciones visuales reportadas por el usuario en las pantallas de
alta/edición, aplicando una **regla homogénea** al chrome de formularios en lugar de
parches por pantalla.

## Evidence (reporte del usuario)

1. **Nuevo Paciente**: "Fecha de Registro" e "N° Historia Optométrica" están al final
   del formulario; deberían ir **arriba de todo**.
2. **Nuevo Paciente**: hay un **espaciado muerto encima de los botones Cancelar/Guardar**
   que tapa la pantalla y "no tiene sentido".
3. **Nueva Evaluación / Nueva Dispensación**: la barra inferior con "Anterior/Siguiente"
   dibuja un **recuadro (Surface elevado)** que obstruye la vista; se quieren solo los
   botones sin el cuadro.
4. **Nueva Evaluación / Nueva Dispensación**: los **números redondos** (StepIndicator con
   círculos 1-2-3) no gustan; se prefiere ver el **título del paso** (estilo título de
   ventana).

## Scope

- **IN**: `PacienteFormSections`, `OptoFormShell`, `NuevaEvaluacionScreen`,
  `NuevaDispensacionScreen`, nuevo componente compartido `WizardStepHeader`.
- **OUT**: lógica de negocio, ViewModels, esquema de datos, navegación.

## Capabilities

- `ui-form-chrome`: chrome homogéneo de formularios y wizards (orden de campos,
  padding de barra inferior, cabecera de paso).

## Approach

- Un único componente `WizardStepHeader` (título del paso + "Paso N de M") reemplaza al
  `StepIndicator` de círculos en **ambos** wizards.
- La barra inferior de ambos wizards pierde el `Surface` elevado: quedan los botones
  con `navigationBarsPadding`, sin recuadro.
- `OptoFormShell` aplica `navigationBarsPadding` **solo** cuando no hay `bottomBar`
  propia (la barra ya lo aplica), eliminando el doble inset que genera el gap.
- `PacienteFormSections` mueve Historia Optométrica + Fecha de Registro a una sección
  "Registro" al inicio.

## Causal invariants

- INV-1: El inset de barra de navegación se aplica **exactamente una vez** por pantalla.
- INV-2: Los dos wizards usan **el mismo** componente de cabecera de paso (sin círculos).
- INV-3: La barra de acciones inferior no dibuja contenedor elevado en los wizards.

## Risks

- Bajo. Cambios declarativos de Compose; sin cambios de datos ni de contrato.
- `OptoFormShell` es compartido (Paciente + Información Financiera): el fix condicional
  preserva el padding para pantallas sin `bottomBar` (IF).
