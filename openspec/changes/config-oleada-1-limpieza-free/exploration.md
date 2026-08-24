# Exploration: config-oleada-1-limpieza-free

**Issue**: #113 · **RDD**: disabled/unmanaged

## Evidence

- `SubscriptionManager.maxOpticas(FREE)=null`; `FREE_MAX_PACIENTES=50` shown in Config card; `canAddPaciente` unused in screens.
- DB `trg_opticas_limit_guard` allows 2 memberships (`20260627005400_…`).
- `createAdditionalOptica` no plan gate; `friendlyOpticaError` still says “2 ópticas”.
- `ConfigAboutSection` dead; NavHost `Route.Configuracion` ungated; Lab always visible; System+SyncDiag first-class.

## Decisions

- FREE product limit = **1 óptica**; no paciente/disp caps.
- Lab stays editable with legacy banner (Proveedores tipados = Oleada 2).
- System + SyncDiag under “Avanzado” expanded=false by default.
- Trigger hard-cap 1 for all until PRO multi (Oleada 3).
