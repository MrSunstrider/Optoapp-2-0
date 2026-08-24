# Spec: horario-atencion (NEW)

## Requirement

Admin/gerente can view and save free-text business hours for the active óptica.

## Scenarios

### Load from Room after remote sync

- GIVEN remote `optica_settings` has `config_json` with `business_hours`
- WHEN Config opens and sync runs
- THEN the horario field shows that value from Room

### Save merges without wiping siblings

- GIVEN existing `configJson` contains other keys
- WHEN user saves a new horario
- THEN `business_hours` is updated and other keys remain

### Upsert creates missing remote row

- GIVEN no remote `optica_settings` row
- WHEN user saves horario
- THEN PostgREST upsert creates the row and Room stores the merged JSON
