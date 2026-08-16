# Design — ui-form-chrome

## Decisión

Extraer el chrome de wizard a un único componente compartido y corregir el inset del
shell de formularios, en vez de tocar cada pantalla con reglas distintas.

## Componente nuevo: `WizardStepHeader`

Archivo: `ui/components/WizardStepHeader.kt`

Lógica pura (testeable con JUnit, sin Android):

```kotlin
fun wizardStepTitle(labels: List<String>, currentStep: Int): String =
    labels.getOrElse(currentStep) { "" }

fun wizardStepProgress(totalSteps: Int, currentStep: Int): String {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val paso = (currentStep + 1).coerceIn(1, safeTotal)
    return "Paso $paso de $safeTotal"
}
```

Composable: título (`headlineSmall`, primary) + progreso (`labelMedium`, onSurfaceVariant).
Sin `Surface`, sin `CircleShape`, sin números en círculo.

## Fix `OptoFormShell` (gap)

Causa: el `Column` de contenido aplica `navigationBarsPadding()` **y** la `bottomBar`
(FormActions) también → doble inset visible como banda muerta encima de los botones.

Fix homogéneo: aplicar el padding de barra de navegación al contenido **solo** cuando
`bottomBar == null`.

```kotlin
.then(if (bottomBar == null) Modifier.navigationBarsPadding() else Modifier)
```

Esto conserva el comportamiento de Información Financiera (sin bottomBar) y elimina el
gap en Nuevo Paciente (con bottomBar).

## Reorden `PacienteFormSections`

Nueva constante `SECTION_REGISTRO = "Registro"`. La sección "Registro" (Historia
Optométrica + Sugerir HO + botón Fecha de Registro) pasa al inicio; se elimina del final.

## Barra inferior de wizards

Se elimina `Surface(tonalElevation = …)` en `NuevaEvaluacionScreen` y
`NuevaDispensacionScreen`; el `Row` de botones queda como `bottomBar` directa con
`navigationBarsPadding()`.

## Estrategia de testing (TDD)

- **RED→GREEN puro (JUnit)**: `WizardStepHeaderTest` cubre `wizardStepTitle` y
  `wizardStepProgress` (incl. bordes). Se escribe antes de la implementación.
- **Constante**: test de `SECTION_REGISTRO` (presente, no-blank, distinta) en
  `PacienteFormSectionsTest`, alineado con el patrón existente de section headers.
- **Visual (no unit)**: reorden, gap y barra sin recuadro se verifican con build +
  captura en dispositivo (no se añade Robolectric nuevo, según AGENTS.md).

## Alternativas descartadas

- Meter el título del paso en el `TopAppBar`: menos legible en pasos largos y mezcla
  el título de pantalla con el de paso. Se prefiere cabecera dedicada.
