# Delta for costos-productos

## ADDED Requirements

### R9: Create Cost Matrix Entry

User MUST be able to create a new `costos_productos` entry via a dialog. The dialog SHALL present fields matching the cost matrix block columns: material, tipo_lente, stock_o_fabricacion, tratamiento, serie, costo_unitario, and laboratorio_id (optional). On save, the system SHALL insert the entry scoped to the session's optica_id via the existing upsert, and the new entry MUST appear in the grid after refresh. The entry SHALL sync to Supabase through the existing upsert pipeline.

#### Scenario: User creates a cost entry

- GIVEN user is in CostosYGastosScreen Tab 1 with a block selected
- WHEN user opens create dialog, fills all required fields, and confirms save
- THEN the new entry appears in the cost matrix grid
- AND the entry syncs to Supabase via the existing upsert flow

#### Scenario: Missing required fields

- GIVEN user opens the create dialog
- WHEN user attempts to save with empty required fields
- THEN the system MUST show a validation error and prevent save

### R10: Delete Cost Matrix Entry

User MUST be able to soft-delete a `costos_productos` entry. The system SHALL prompt a confirmation dialog before deletion. On confirm, the system SHALL set `vigente_hasta` to the current date and upsert the entry. After deletion, the entry MUST NOT appear in the grid or in auto-cost calculations. All operations SHALL be scoped to the session's optica_id.

#### Scenario: User confirms deletion

- GIVEN a cost entry exists in the matrix grid
- WHEN user taps delete on the entry and confirms the deletion dialog
- THEN the entry disappears from the grid
- AND vigente_hasta is set to today's date locally and propagated to Supabase

#### Scenario: User cancels deletion

- GIVEN user taps delete on a cost entry
- WHEN user cancels the confirmation dialog
- THEN the entry remains unchanged in the grid
