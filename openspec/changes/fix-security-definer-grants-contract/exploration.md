# Exploration: security-definer-grants-contract

## Problem

Supabase Security Advisors showed 10 WARN findings (lints 0028/0029 + Auth Pro). Blind revoke of EXECUTE on SECURITY DEFINER RPCs would break Android sync/onboarding and optoweb login/stock/delete quota.

## Call-site inventory

| RPC | Android | optoweb | Required role |
|-----|---------|---------|---------------|
| `check_rate_limit` | none | login/register (pre-auth), pin routes | **anon + authenticated** |
| `paciente_eliminaciones_restantes_hoy` | local quota only (no RPC) | `paciente-delete-audit` after authenticated delete | **authenticated** |
| `create_optica_for_current_user` | OpticaSettingsDataSource | types only | **authenticated** |
| `recalcular_resumen_diario` | DownloadSyncCoordinator | none | **authenticated** |
| `rpc_adjust_montura_stock` | none (finanzas must NOT call) | dispensacion-crud | **authenticated** |
| `recalcular_resumen_diario_admin` | none | none | **service_role only** |

## Applied fix (prod)

Migration `20260913212248_harden_security_definer_grants`:
- Revoke anon from `paciente_eliminaciones_restantes_hoy` (stale grant)
- Lock `recalcular_resumen_diario_admin` to service_role (no membership guard)
- Keep intentional anon on `check_rate_limit`; strip PUBLIC default

## Decision

Leave remaining 0028/0029 advisor WARNs: they flag intentional client-callable SECURITY DEFINER RPCs required for the apps. Do not revoke further. Pro Auth WARNs (leaked password, MFA) require paid plan / product MFA work — out of scope.
