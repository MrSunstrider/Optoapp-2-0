# Proposal: fix-security-definer-grants-contract

## Why

Advisor WARN noise must not drive unsafe revokes. Two real grant bugs were fixed; intentional SECURITY DEFINER exposure stays for app function.

## Scope

- Supabase EXECUTE matrix for six SECURITY DEFINER RPCs
- Catalog SQL contract test (TDD)
- Explicit non-goals: silence 0028/0029 by breaking clients; enable Pro Auth features

## Supabase impact

Yes — grants only (already applied as `20260913212248_harden_security_definer_grants`). No function body changes.

## Rollback

Re-grant prior EXECUTE if a live caller fails with `42501`:
- `GRANT EXECUTE ON FUNCTION public.paciente_eliminaciones_restantes_hoy(text) TO anon;` (only if proven anon caller)
- `GRANT EXECUTE ON FUNCTION public.recalcular_resumen_diario_admin(text, date) TO authenticated;` (only if proven authenticated admin UI)

## RDD

`rdd_mode=disabled/unmanaged` — principles only; no receipt authority.

## GGA

R1 Risk: CLEAN (live). R3/R4 native binding unavailable; parent live analysis CLEAN (see `gga-report.md`).
