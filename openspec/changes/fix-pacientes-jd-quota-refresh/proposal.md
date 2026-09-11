# Proposal: fix-pacientes-jd-quota-refresh

## Intent

Judgment Day (pacientes full stack) confirmed three severe defects. This change records the bounded fixes already applied under JD Round 1 and the remote RPC restore.

## Scope

### In Scope
1. Restore `paciente_eliminaciones_restantes_hoy` (SECURITY DEFINER, per-user UTC day, `usuario_optica` membership) after `20260621033310` regression.
2. Consume daily local delete quota immediately after local delete+tombstone (before remote), so remote failure cannot bypass the 10/day gate.
3. Add `withTimeout(5_000)` to `PacienteViewModel.refresh()` so empty listas clear loading.

### Out of Scope
- SyncPacientes conflictDao fail-open (JD SUSPECT — not dual-confirmed)
- Fecha nacimiento save gating (INFO)
- Aligning SessionManager LocalDate key to UTC (INFO)

## Supabase
Yes — forward migration `20260911000000_fix_paciente_eliminaciones_restantes_hoy.sql`.

## Rollback
- Drop/replace RPC with prior body from `20260621033310` (not recommended) or re-apply `20260429100200` semantics without membership.
- Revert `PacienteViewModel` delete increment order and refresh timeout; remove added unit tests.
