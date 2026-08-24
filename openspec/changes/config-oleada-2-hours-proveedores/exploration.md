# Exploration: config-oleada-2-hours-proveedores

**Issue**: #115 · **RDD**: disabled/unmanaged

## Evidence

- `optica_settings.configJson` already stores `business_hours`; `OpticaHeaderViewModel` reads it for `horarioAtencion`.
- No Config UI to edit hours; `OpticaSettingsDataSource` had fetch only (no remote upsert).
- `MembershipRepository` exposed Room upsert + fetch, not `syncOpticaSettingsFromRemote`.
- `Proveedor` entity lacked `tipo`; all proveedores treated as monturas; Lab section soft-deprecated in Oleada 1 pointing to Proveedores tipados.

## Decisions

- Free-text `business_hours` in JSON bag (merge helper preserves sibling keys).
- Remote upsert via PostgREST on `optica_settings` (admin/gerente RLS already).
- Proveedor.tipo ∈ `{monturas, laboratorio, tecnico}` default `monturas`.
- Room 46→47 + Supabase `ADD COLUMN IF NOT EXISTS`.
- Skip configuracion_financiera UI (#112 / Oleada D).

## Out of scope

- PagoEffect / SyncFinanzas order
- Structured weekly hours schema
- Filtering OC/inventario by tipo (consume later)
