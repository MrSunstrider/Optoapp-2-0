# Design: VP Toggle Cerca/Intermedio en Refracción

## Technical Approach

Cambio puramente UI en la pantalla de evaluación. Se agrega flag `isVpCerca` al `EvaluacionUiState`, se reestructura el card "Adición (ADD)" existente para incluir un toggle Cerca/Intermedio, y se condiciona el label/valor del segundo campo DIP según el estado del toggle. Sin cambios en entidades, DAOs, mapeos, ni PDF.

## Architecture Decisions

### Decision: Flag UI sin persistencia

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Persistir `isVpCerca` en Room/Supabase | Requiere migración, columna nueva, mapeo, sync — sobreingeniería para un flag de UI temporal | ❌ No persiste |
| Flag solo en UiState con default `true` | Al recargar una evaluación existente arranca en Cerca, el profesional togglea si necesita Intermedio — comportamiento aceptable | ✅ `isVpCerca: Boolean = true` en UiState |

**Rationale**: `isVpCerca` indica qué distancia está ajustando el profesional *ahora*, no es un dato del paciente. Sigue el mismo patrón que flags existentes (`isAddAo`, `autoPresbicia`) que viven solo en UiState.

### Decision: Modificar AddSection in-situ vs extraer nuevo composable

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Crear `VpSection` composable nuevo | Duplica estructura casi idéntica, más archivos, más diff | ❌ |
| Modificar `AddSection` existente | Un solo archivo modificado, diff claro, sigue el patrón actual | ✅ Renombrar conceptualmente la card, no la función |

**Rationale**: La card existente ya contiene los campos ADD. Solo cambia el título, se agrega un toggle arriba, y se mueve el subtitle "Adición" + toggle A/O + campos debajo del nuevo toggle.

### Decision: Pasar `isVpCerca` a DipSection

`DipSection` ya recibe `uiState: EvaluacionUiState`, por lo que `uiState.isVpCerca` es accesible sin cambiar la firma. Se condiciona label y binding interno.

## Data Flow

```
Switch Cerca/Intermedio
       │
       ▼
onUpdate(uiState.copy(isVpCerca = !uiState.isVpCerca))
       │
       ├──► AddSection: título estático "VP Cerca/Intermedio" (no cambia con toggle)
       │
       └──► DipSection:
              isVpCerca=true  → label "DIP Cerca",   value = uiState.dipCerca
              isVpCerca=false → label "DIP Intermedio", value = uiState.dipIntermedio
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `viewmodel/EvaluacionUiState.kt` | Modify | + `val isVpCerca: Boolean = true` |
| `ui/components/evaluacion/RefraccionSection.kt` | Modify | Renombrar título card a "VP Cerca/Intermedio", agregar Switch, reordenar contenido interno, condicionar DipSection |
| `viewmodel/EvaluacionViewModelTest.kt` | Modify | + test assertion para `isVpCerca == true` por defecto |

## Interfaces / Contracts

```kotlin
// En EvaluacionUiState — nuevo flag
val isVpCerca: Boolean = true   // true = Cerca, false = Intermedio
```

Sin cambios en `EvaluacionMapping`, `EvaluacionEntity`, DAOs, ni DTOs de sync.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `EvaluacionUiState` default value | Assert `isVpCerca == true` en `evaluacionUiState_hasDefaultBooleanValues` (test existente) |
| UI | Toggle cambia label/valor DIP | Test de componibilidad con Semantics (verificar label del TextField condicional) — opcional, no hay tests UI actualmente |

No hay cambios en lógica de negocio ni mapeos — solo se agrega una assertion al test existente de defaults.

## Migration / Rollout

No migration required. Rollback: revertir los 3 archivos modificados.

## Open Questions

- Ninguna. El alcance está acotado y los datos ya existen en la entidad.
