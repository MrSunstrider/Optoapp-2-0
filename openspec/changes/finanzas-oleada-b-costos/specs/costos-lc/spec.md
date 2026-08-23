# costos-lc Specification

## Purpose

Dedicated contact-lens cost matrix (`costos_lc`): sync, CostosYGastos tab CRUD, and OT snapshot lookup by `tipo_lc` / `material_lc` / `modalidad`. Completes Oleada B (#106). Out: cierre export, P&L, PagoEffect, new schema unless prod missing table.

## Requirements

### Requirement: costos_lc Sync Upload and Download

System SHALL include `costos_lc` in SyncFinanzas upload AND download, immediately after `costos_biselado`. Remote DTO SHALL map snake_case columns. Download SHALL upsert local rows with `skipDeletions = true`. `FinanzasSyncResult` SHALL expose upload/download counters for LC.

#### Scenario: Upload after biselado

- GIVEN pending local `costos_lc` rows
- WHEN SyncFinanzas runs upload
- THEN `costos_lc` uploads after `costos_biselado` and before pagos
- AND result includes uploaded LC count

#### Scenario: Download upsert

- GIVEN remote `costos_lc` for optica
- WHEN download runs
- THEN local Room upserts mapped entities
- AND result includes downloaded LC count

### Requirement: Biselado Tab CRUD

CostosYGastos tab 1 (Biselado) MUST list vigente rows and support create/edit/soft-delete. Soft-delete MUST set `vigenteHasta = today` via upsert (MUST NOT hard DELETE). Post-save MUST schedule finanzas sync. Access MUST remain Oleada A BI/reports role gate.

#### Scenario: Soft-delete hides row

- GIVEN vigente biselado row
- WHEN user confirms delete
- THEN row upserted with `vigenteHasta=today` and disappears from list

#### Scenario: Create then sync

- GIVEN authorized user on tab 1
- WHEN saves new biselado cost
- THEN entity persisted locally and finanzas sync scheduled

### Requirement: LC Tab CRUD

CostosYGastos tab 2 (Lentes Contacto) MUST list vigente `costos_lc` and support create/edit/soft-delete with the same soft-delete and post-save sync rules as Biselado. Catalog constants MUST cover `tipo_lc` ∈ {cosmetico, graduado, terapeutico} and `modalidad` ∈ {diario, quincenal, mensual, anual}.

#### Scenario: Create LC row

- GIVEN authorized user on tab 2
- WHEN saves tipo=cosmetico, material, modalidad=mensual, costo>0
- THEN row appears in list with `vigenteHasta=null`

#### Scenario: Soft-delete LC

- GIVEN vigente LC row
- WHEN user confirms delete
- THEN `vigenteHasta=today` and row hidden from vigente list

### Requirement: OT LC Cost Snapshot from costos_lc

When filling dispensacion item costs, system MUST lookup LC cost via `costos_lc` keys (`tipo_lc`, `material_lc`, `modalidad`, optional `laboratorio_id`). MUST NOT use `costos_productos` `lente_contacto_*` keys for new fills. Existing non-null `costo_real_lc` MUST remain (hybrid `?:` override). If no rule matches, field MAY stay empty for manual entry.

#### Scenario: Snapshot from matrix

- GIVEN matching vigente `costos_lc` rule and null `costo_real_lc`
- WHEN cost snapshot runs
- THEN `costo_real_lc` filled from `costo_unitario`

#### Scenario: Preserve manual override

- GIVEN `costo_real_lc` already set
- WHEN snapshot runs
- THEN value unchanged
