# Delta specs: UX Unificado Integral

Parent change. Domain deltas live in subfolders as phases complete.

## Included domains

| Domain | Phase | Delta path |
|--------|-------|------------|
| cierre-caja | P2 | `../ux-cierre-caja-v2/spec.md` (merge to base) |
| paciente | P3 | TBD `specs/paciente-ux/` |
| evaluacion | P3 | TBD |
| dispensacion + IF | P4 | TBD |
| servicio-extra | P5 | TBD |

## Cross-cutting requirements

### REQ-UX-INT-001: PagoEffect Everywhere

All UX surfaces showing payment totals MUST use `PagoEffect.signedAmount(tipo, monto)`. MUST NOT use raw `sum(monto)` nor exclude-only-Anulación filters.

### REQ-UX-INT-002: FinancieraPagosSection Single Source

Payment history UI (abonos list, add/edit/delete, saldo) MUST render via shared `FinancieraPagosSection` in IF dispensación and ServicioForm. Duplicated inline payment blocks MUST NOT remain after Phase 1.

### REQ-UX-INT-003: PatientContextCard

Screens editing entity for a patient (evaluación, dispensación, IF, servicio from paciente tab) MUST show `PatientContextCard` with OT (if any), paciente, fecha.

### REQ-UX-INT-004: Form Shell Consistency

Form screens MUST use `OptoFormShell` + bottom bar Cancel/Guardar (or wizard equivalent). Ad-hoc Scaffold patterns MUST be migrated per phase.

### REQ-UX-INT-005: Cierre v2 Integrated

Cierre de Caja MUST satisfy full delta in `ux-cierre-caja-v2/spec.md` as Phase 2 of this change — not a separate product track.

### REQ-UX-INT-006: Regalos IF-Only

Regalos dispensación MUST be editable only in InformacionFinanciera hub. NuevaDispensacion wizard MUST NOT include RegalosSection after Phase 4.
