# Proposal: VP Toggle Cerca/Intermedio en Refracción

## Intent

En la pantalla de evaluación, la sección de Adición (ADD) necesita un toggle
Cerca/Intermedio que actualice los labels del card y del campo DIP, permitiendo
al profesional optométrico indicar si está trabajando en visión próxima (cerca)
o distancia intermedia. El toggle persiste visualmente y repercute en el label
y valor del campo DIP.

## Scope

### In Scope
- Renombrar card "Adición (ADD)" → "VP Cerca/Intermedio"
- Agregar toggle Cerca/Intermedio (Switch) con default "Cerca"
- Mover "Adición" y su toggle A/O debajo del nuevo toggle
- Cambiar label "DIP Cerca" → "DIP Intermedio" según toggle
- Cambiar valor mostrado en DIP entre `dipCerca` ↔ `dipIntermedio` según toggle
- Agregar flag `isVpCerca` al `EvaluacionUiState`
- Pasar flag a DIP label y value binding

### Out of Scope
- No se agregan nuevos campos de BD (addIntermediaOd/Oi ya existen)
- No se cambia el PDF de receta
- No se modifica AV VP ni Add OD/OI
- Sin cambios en Web ni Supabase

## Capabilities

### New Capabilities
- `vp-cerca-intermedio-toggle`: Toggle visual para elegir entre VP Cerca e
  Intermedio, con impacto en labels y DIP

### Modified Capabilities
- None — cambio puramente UI, no altera requirements de specs existentes

## Approach

1. Agregar `isVpCerca: Boolean = true` a `EvaluacionUiState`
2. En `RefraccionSection.kt`:
   - Cambiar título del card a "VP Cerca/Intermedio"
   - Agregar Switch Cerca/Intermedio debajo del título
   - Mover sección "Adición" con su A/O toggle debajo del nuevo toggle
   - Pasar `isVpCerca` a `DipSection`
3. En `DipSection`: label del segundo campo condicional según `isVpCerca`
   - `isVpCerca = true` → label "DIP Cerca", value `dipCerca`
   - `isVpCerca = false` → label "DIP Intermedio", value `dipIntermedio`

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `viewmodel/EvaluacionUiState.kt` | Modified | + `isVpCerca` field |
| `ui/components/evaluacion/RefraccionSection.kt` | Modified | Reestructurar card, toggle, pasar flag |
| `util/LaboratorioTicketText.kt` | Minor | Si usa `dipCerca` hardcoded, verificar |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|-------------|
| `isVpCerca` no se persiste en BD | High | Es flag de UI, no necesita persistencia. Default `true` al cargar. |

## Rollback Plan

Revertir cambios en `RefraccionSection.kt` y `EvaluacionUiState.kt`.
No hay migración de BD que revertir.

## Dependencies

- Ninguna

## Success Criteria

- [ ] Toggle "Cerca/Intermedio" visible y funcional en el card VP
- [ ] Al togglear a Intermedio, label DIP cambia a "DIP Intermedio" y muestra `dipIntermedio`
- [ ] Al volver a Cerca, label vuelve a "DIP Cerca" y muestra `dipCerca`
- [ ] A/O toggle sigue funcionando independientemente
- [ ] AV VP y Add OD/OI no se ven afectados
- [ ] Tests unitarios existentes siguen pasando
