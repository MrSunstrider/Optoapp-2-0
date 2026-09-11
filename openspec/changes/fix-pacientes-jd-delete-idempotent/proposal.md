# Proposal: fix-pacientes-jd-delete-idempotent

## Intent
Close Judgment Day v2 confirmed CRITICAL: retrying patient delete after remote failure re-charges the daily quota and leaves the detail screen delete-enabled.

## Scope
### In
1. Idempotent local delete quota: increment at most once per patient tombstone / successful local delete for the UTC day.
2. UI: after local delete succeeded (remote pending/failed), leave detail screen (close dialog + pop) so user cannot re-confirm and burn quota.
3. TDD coverage for retry / already-tombstoned paths.

### Out
- SessionManager per-user quota key (JD SUSPECT — Judge B only)
- SyncPacientes null-updatedAt upload abort (JD SUSPECT — Judge B only)
- Broad sync-module fail-closed rollout

## Related
Builds on `fix-pacientes-jd-quota-refresh` and `fix-pacientes-jd-residuals`.

## Rollback
Revert ViewModel/Repository/DetallePacienteScreen + tests to prior increment-always-after-local behavior.
