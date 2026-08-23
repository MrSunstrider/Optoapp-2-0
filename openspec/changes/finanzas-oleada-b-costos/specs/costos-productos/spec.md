# Delta for costos-productos

## MODIFIED Requirements

### R5: LC Cost Lookup

System SHALL lookup LC cost in `costos_lc` by `tipo_lc`, `material_lc`, `modalidad`, and optional `laboratorio_id` (vigente rows only). Specs SHALL auto-populate from linked evaluation's LC fields mapped into those keys. System MUST NOT use `costos_productos` keys `lente_contacto_cosmetico` / `lente_contacto_medida` for new LC cost fills.

(Previously: Lookup used `costos_productos` where `tipo_lente IN ('lente_contacto_cosmetico','lente_contacto_medida')` by material+laboratorio.)

#### Scenario: LC from costos_lc

- GIVEN LC graduado, material Hydrogel, modalidad mensual, lab Impagurt, evaluacion linkeada
- WHEN cost calculation runs
- THEN lookup returns `costo_unitario` per box from `costos_lc`

#### Scenario: No matching rule

- GIVEN no vigente `costos_lc` row for keys
- WHEN cost calculation runs
- THEN `costo_real_lc` left empty for manual entry
