# Proposal: Fix Paciente Multi-Tenant WhatsApp Templates

## Intent

WhatsApp message templates in `PacienteWhatsAppActions.kt` hardcode business name `"Óptica Sersa Visual y Preventiva"` and hours `"Martes a Sábado de 10am a 6:30pm y Domingos de 10am a 2pm"`. Since the app is multi-tenant by `optica_id`, every tenant sends messages with another optica's identity — a legal/compliance impersonation issue.

## Scope

### In Scope
- Extract hardcoded strings from `PacienteWhatsAppMenu` composable, accept `nombreOptica` and `horarioAtencion` as parameters
- Wire `nombreOptica` from existing `OpticaHeaderViewModel` in `DetallePacienteScreen`
- Create `optica_settings` Room entity + DAO to read business hours from `optica_settings.config_json -> business_hours`
- Add `horarioAtencion` to `OpticaHeaderUi` with read from `optica_settings`
- Unit tests verifying per-tenant values propagate to message templates

### Out of Scope
- UI for editing business hours (deferred to future config screen)
- Migrating other hardcoded strings outside WhatsApp templates
- Supabase migration — `optica_settings` table already exists

## Capabilities

### New Capabilities
- `optica-config-settings`: Reading per-tenant configuration from `optica_settings` table via Room, covering business hours and other future key-value settings

### Modified Capabilities
- None (no existing spec covers WhatsApp behavior or tenant messaging)

## Approach

1. **Add `horarioAtencion` field** to `OpticaHeaderUi` data class, read from `optica_settings.config_json->>'business_hours'` via new Room entity `OpticaSettingsEntity` + DAO (`OpticaSettingsDao`)
2. **Add parameters** `nombreOptica: String` and `horarioAtencion: String` to `PacienteWhatsAppMenu` composable; replace hardcoded strings with parameter values
3. **Wire in `DetallePacienteScreen`**: read `opti caHeaderVm.uiState.value` (already sourced) + new `opticaSettingsVm` for hours, pass both to `PacienteWhatsAppMenu`
4. **Fallback**: if `horarioAtencion` is blank, omit hours from "Entrega de Lentes" message instead of using wrong data

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/components/paciente/PacienteWhatsAppActions.kt` | Modified | Accept params, remove hardcoded strings |
| `viewmodel/OpticaHeaderViewModel.kt` | Modified | Add `horarioAtencion` to `OpticaHeaderUi` |
| `ui/screens/DetallePacienteScreen.kt` | Modified | Wire optica name + hours to menu |
| `data/membership/OpticaSettingsDataSource.kt` | Modified | Add `fetchOpticaSettings` reading `config_json` |
| `data/MembershipRepository.kt` | Modified | Expose `fetchOpticaSettings` |
| `data/` (new entity + DAO) | New | `OpticaSettingsEntity` + `OpticaSettingsDao` |
| `database/OptoDatabase.kt` | Modified | Register new entity/DAO |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `optica_settings` row doesn't exist → hours empty | Med | Fallback: omit hours line, don't fail silently |
| RLS blocks `optica_settings` read offline | Low | Room caches last synced value; offline reads local |
| Existing users see empty hours until configured | Med | Acceptable — wrong hours is worse than no hours |

## Rollback Plan

Revert changes to `PacienteWhatsAppActions.kt`, `DetallePacienteScreen.kt`, and `OpticaHeaderViewModel.kt`. New Room entity/DAO can remain (zero rows → no behavior change). Old hardcoded values return.

## Dependencies

- `optica_settings` table exists in Supabase (migration `20260429214000`)
- No new Supabase migration needed

## Success Criteria

- [ ] Every optica sends WhatsApp messages with its own `nombre` (from `opticas` table) not "Óptica Sersa Visual y Preventiva"
- [ ] "Entrega de Lentes" message uses per-tenant hours from `optica_settings.config_json->>'business_hours'` or omits them if unset
- [ ] Unit tests verify 2+ different optica IDs produce different message text
- [ ] Offline: messages use cached values from Room
