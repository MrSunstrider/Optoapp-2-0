# Delta for Optical Catalog

## ADDED Requirements

### Requirement: OpticalCatalog Canonical Lists (R1)

The `OpticalCatalog` object SHALL be the single source of truth for optical dropdown options. It MUST define canonical ordered lists for the following five categories. No other file SHALL define duplicate hardcoded optical constants.

| Property | Canonical Values | Normalization |
|----------|-----------------|---------------|
| `MATERIALES` | `["Resina", "Cristal", "Policarbonato", "Trivex"]` | "CR39" → "Resina"; "Alto Índice" → treatments |
| `TIPO_LENTE` | `["Monofocal", "Bifocal", "Multifocal", "Ocupacional", "Lentes de Contacto"]` | "Progresivo" → "Multifocal"; adds Lentes de Contacto |
| `TRATAMIENTOS` | `["Antireflejo", "Antirayas", "Filtro UV 400", "Fotocromatico", "Polarizado", "AR Blue Defense", "Circadian NK55", "Filtro Discromatopsia", "Blue Block"]` | "Antireflex" → "Antireflejo"; merged LenteForm + VM lists |
| `TIPO_ARO` | `["Aro Completo", "Semi al aire", "Al aire"]` | Values unchanged from existing LenteForm/MonturaForm |
| `SERIES` | `["1ra", "2da", "3ra"]` | New centralized list (previously not extracted) |

TRATAMIENTOS SHALL NOT contain a blank or empty entry. Absence of treatment selection is represented as `null` or an empty list, not a sentinel string.

#### Scenario: Material list matches canonical values
- GIVEN `OpticalCatalog.MATERIALES`
- WHEN read
- THEN returns exactly 4 values: `["Resina", "Cristal", "Policarbonato", "Trivex"]`
- AND does NOT include "CR39" or "Alto Indice"

#### Scenario: Lens type includes Multifocal and contact lenses
- GIVEN `OpticalCatalog.TIPO_LENTE`
- WHEN read
- THEN "Multifocal" is present and "Progresivo" is absent
- AND "Lentes de Contacto" is the last value

#### Scenario: Treatments exclude empty sentinel
- GIVEN `OpticalCatalog.TRATAMIENTOS`
- WHEN read
- THEN no entry is blank or empty string
- AND "Antireflejo" is present and "Antireflex" is absent

### Requirement: CostosYGastosViewModel Uses OpticalCatalog (R2)

`CostosYGastosViewModel` SHALL expose material, lens type, and treatment lists exclusively from `OpticalCatalog`. The companion object constants `MATERIALES_OPTICOS`, `TIPOS_LENTE`, and `TRATAMIENTOS` MUST be removed.

#### Scenario: ViewModel exposes catalog material list
- GIVEN `CostosYGastosViewModel` is initialized
- WHEN `materialesOpticos` is read
- THEN it returns `OpticalCatalog.MATERIALES`

#### Scenario: ViewModel exposes catalog lens types
- GIVEN `CostosYGastosViewModel` is initialized
- WHEN `tiposLente` is read
- THEN it returns `OpticalCatalog.TIPO_LENTE`

#### Scenario: ViewModel exposes catalog treatments
- GIVEN `CostosYGastosViewModel` is initialized
- WHEN `tratamientos` is read
- THEN it returns `OpticalCatalog.TRATAMIENTOS`

### Requirement: LenteForm Uses OpticalCatalog (R3)

`LenteForm` SHALL reference `OpticalCatalog` properties for all hardcoded `listOf(...)` dropdown options. No inline string lists for material, lens type, treatments, or aro type SHALL remain.

#### Scenario: Material dropdown from catalog
- GIVEN `LenteForm` renders the "Material del Lente" dropdown
- WHEN the dropdown options are populated
- THEN options are `OpticalCatalog.MATERIALES`

#### Scenario: Lens type dropdown from catalog
- GIVEN `LenteForm` renders the "Tipo de Lente" dropdown
- WHEN the dropdown options are populated
- THEN options are `OpticalCatalog.TIPO_LENTE`

#### Scenario: Treatment dropdowns from catalog
- GIVEN `LenteForm` renders treatment dropdowns
- WHEN the dropdown options are populated
- THEN options are `OpticalCatalog.TRATAMIENTOS`
- AND a "Ninguno" option MAY be prepended as a UI-only sentinel (not part of the catalog list itself)

#### Scenario: Aro type dropdown from catalog
- GIVEN `LenteForm` renders the "Tipo de Aro" dropdown
- WHEN the dropdown options are populated
- THEN options are `OpticalCatalog.TIPO_ARO`
