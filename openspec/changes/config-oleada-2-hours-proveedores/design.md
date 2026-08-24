# Design: config-oleada-2-hours-proveedores

**RDD**: disabled/unmanaged · **Schema**: Room `MIGRATION_46_47` + `20260824160000_proveedores_add_tipo.sql`

## Approach

### A — business_hours

1. `BusinessHoursConfigJson` pure extract/merge on `configJson`.
2. `OpticaSettingsDataSource.upsertOpticaSettingsRemote` — PostgREST upsert (create-if-missing).
3. `MembershipRepository.syncOpticaSettingsFromRemote` — fetch + Room upsert.
4. `BusinessHoursConfigViewModel` — Room flow → draft; save merges JSON → Room + remote.
5. `ConfiguracionScreen` — section under DATOS DE LA ÓPTICA; `LaunchedEffect` sync with fiscal/lab.

### B — Proveedor.tipo

1. Entity field `tipo: String = "monturas"` (Room camelCase).
2. Migration ALTER + OptoDatabase version 47.
3. Supabase column + optional CHECK.
4. `ProveedorRemoto` / `toEntity` / `toRemoto` include `tipo`.
5. Form dropdown `monturas|laboratorio|tecnico`.

## Risks

- Upsert RLS: only admin/gerente (same as Config gate).
- Sync before remote column deploy: upload may fail until migration applied — ship App + SQL together.
- Malformed `configJson`: merge starts from `{}`.
