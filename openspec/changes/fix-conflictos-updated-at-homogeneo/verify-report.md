# Verify Report: fix-conflictos-updated-at-homogeneo

**Verdict**: PASS (pending device confirmation of task 3.4)
**Date**: 2026-08-15
**Target**: production `sflhtihqdhrlryeyrzdo`

## Requirements coverage

| Req | Statement | Evidence | Result |
|-----|-----------|----------|--------|
| REQ-1 | Exactly one timestamp trigger per sync table | Post-migration trigger inventory | PASS |
| REQ-2 | Client `updated_at` preserved | Preserve probe: all five `true` | PASS |
| REQ-3 | Server stamps only on NULL | `set_updated_audit_fields` body unchanged | PASS |
| REQ-4 | No `update_updated_at` on sync tables | Inventory shows none | PASS |
| REQ-5 | Settings exception only | Only `cierres_caja`, `optica_settings` | PASS |
| REQ-6 | Homogeneous enforcement in tests | DOMAIN A/B + DOMAIN 6 asserts all sync tables | PASS |
| REQ-7 | Shared conflict recovery, no per-entity path | No Android code changed by this change | PASS |

## TDD evidence

RED (before migration), self-reverting probe on production:

```
PROBE_RESULT preserved: pacientes=false evaluaciones=false dispensaciones=false
pagos=false servicios_extra=false  (intentional rollback, nothing persisted)
```

GREEN (after migration), same probe:

```
PROBE_RESULT preserved: pacientes=true evaluaciones=true dispensaciones=true
pagos=true servicios_extra=true  (intentional rollback, nothing persisted)
```

The probe ends in `RAISE EXCEPTION`, so the whole statement rolls back; no
fixture row was ever committed to production.

## Post-migration trigger inventory

| Table | Trigger | Function |
|-------|---------|----------|
| pacientes | trg_pacientes_set_updated_audit | set_updated_audit_fields |
| evaluaciones | trg_evaluaciones_set_updated_audit | set_updated_audit_fields |
| dispensaciones | trg_dispensaciones_set_updated_audit | set_updated_audit_fields |
| pagos | trg_pagos_set_updated_audit | set_updated_audit_fields |
| servicios_extra | trg_servicios_extra_set_updated_audit | set_updated_audit_fields |
| monturas | trg_monturas_set_updated_audit | set_updated_audit_fields |
| montura_movimientos | trg_montura_movimientos_set_updated_audit | set_updated_audit_fields |
| cierres_caja | trg_cierres_caja_updated_at | update_updated_at (documented exception) |
| optica_settings | trg_optica_settings_updated_at | update_updated_at (documented exception) |

Zero legacy `*_updated_at` triggers remain on sync tables.

## Review gate

GGA v2.10.1 could not run: provider `opencode` resolved to `deepseek-v4-pro`,
which now requires an explicit China-hosted opt-in. Substituted Cursor Bugbot.

| Round | Result |
|-------|--------|
| 1 | 1 medium finding — DOMAIN B fixtures violated NOT NULL / CHECK constraints |
| Fix | Added `pacientes.fecha_creacion`, `servicios_extra.descripcion`, `servicios_extra.metodo_pago`, `pagos.metodo_pago`; changed `pagos.tipo` to `Abono` per `chk_pagos_tipo` |
| 2 | No bugs found |

## Residual work

Task 3.4: the 406 existing conflict records live in the device's Room database.
They must be cleared once from Conflictos → "Usar el mío para todos". With the
preserve policy live, they must not regenerate on subsequent syncs.

## Rollback

Recreate the five dropped triggers pointing at `update_updated_at()`.
