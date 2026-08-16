# Tasks — ui-form-chrome

## 1. RED (tests primero)
- [x] 1.1 `WizardStepHeaderTest` (JUnit puro): `wizardStepTitle` normal + fuera de rango
- [x] 1.2 `WizardStepHeaderTest`: `wizardStepProgress` normal + bordes (neg, >total, total 0)
- [x] 1.3 `PacienteFormSectionsTest`: `SECTION_REGISTRO` presente/no-blank/distinta
- [x] 1.4 Confirmar RED (no compila / falla): helpers y constante aún no existen

## 2. GREEN (implementación)
- [x] 2.1 Crear `WizardStepHeader.kt` (helpers puros + composable)
- [x] 2.2 `PacienteFormSections`: añadir `SECTION_REGISTRO`, mover Historia+Fecha arriba
- [x] 2.3 `OptoFormShell`: `navigationBarsPadding` condicional (solo sin bottomBar)
- [x] 2.4 `NuevaEvaluacionScreen`: `WizardStepHeader` + quitar `Surface` de bottomBar
- [x] 2.5 `NuevaDispensacionScreen`: `WizardStepHeader` + quitar `Surface` de bottomBar
- [x] 2.6 `NuevoServicioScreen` (homogeneidad) + eliminar `StepIndicator.kt` muerto

## 3. VERIFY
- [x] 3.1 `:optoapp:testDebugUnitTest` verde
- [x] 3.2 `:optoapp:assembleDebug` compila
- [~] 3.3 Smoke en dispositivo (arranca sin crash); confirmación visual pendiente del usuario
- [x] 3.4 `verify-report.md`
