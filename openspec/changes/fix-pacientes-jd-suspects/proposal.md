# Proposal: fix-pacientes-jd-suspects

## Intent
Close the two Judgment Day v2 suspects with dual evidence and production-safe fixes.

## Findings (detailed)

### S1 — Local delete quota not per-user
`SessionManager.dailyPacienteDeleteKey` is `paciente_delete_count_${opticaId}_$day`. Server `guard_pacientes_delete` / `paciente_eliminaciones_restantes_hoy` scope by `auth.uid()`.
`saveSession` overwrites email/rol without clearing prior delete keys. On shared device, user A can exhaust the local gate for user B in the same óptica (server would still allow B). Fix: include normalized email in the key.

### S2 — Upload double-failure abort skipped when all `updatedAt` null
`SyncPacientesUseCase` aborts only when `hasCheckable = deduplicated.any { updatedAt != null }`. If every row lacks `updatedAt`, batch fetch failure still uploads all conflict-safe rows without remote timestamp verification — silent overwrite risk. Fix: abort whenever double-failure proof holds, regardless of `updatedAt`.

## Scope
In: SessionManager key + SyncPacientes abort + tests. Out: other sync modules, Supabase.

## Rollback
Revert key format and restore `hasCheckable` gate.
