# Delta for Paciente Tech Debt (No Spec-Level Changes)

This change is pure refactoring and UI polish — no new or modified capabilities. All items are
implementation-level improvements that preserve existing behavior contracts.

## ADDED Requirements

None.

## MODIFIED Requirements

None. Each item changes only implementation, not observable behavior:

| Item | Change | Why Not a Spec Change |
|------|--------|----------------------|
| 15 | CSV→JSON for `ultimasEtiquetas` sync serialization | Supabase column stays `TEXT`; API contract unchanged; existing rows handled via CSV fallback |
| 18 | SQL `MAX(SUBSTR(...))` replacing in-memory loop | Produces identical output (`HO-YYYY-NNNN`); faster path, same result |
| 19 | Reusable `EmptyState` composable | Additive UI component — no existing requirement modified |
| 20 | Tag chips in `PacienteInfoHeader` | Display-only; data already surfaced via `ultimasEtiquetas` field |
| 21 | Sort moved from composables to ViewModels | Preserves identical sort order (`sortedByDescending`) |
| 22 | Timeout + error state replacing infinite spinner | Fixes a bug (stuck loading state); no new behavior requirement |

## REMOVED Requirements

None.

## RENAMED Requirements

None.
