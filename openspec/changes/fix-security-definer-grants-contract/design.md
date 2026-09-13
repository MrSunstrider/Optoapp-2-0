# Design: security-definer-grants-contract

## Approach

Fail-closed grants for unused/unguarded admin RPC; keep authenticated EXECUTE for app RPCs that bypass RLS under membership/auth checks; keep anon EXECUTE only for pre-auth rate limit.

## Grant matrix (MUST)

| Function | anon | authenticated | service_role |
|----------|------|---------------|--------------|
| `check_rate_limit(text,int,int)` | YES | YES | YES |
| `paciente_eliminaciones_restantes_hoy(text)` | NO | YES | YES |
| `create_optica_for_current_user(...)` | NO | YES | YES |
| `recalcular_resumen_diario(text,date)` | NO | YES | YES |
| `rpc_adjust_montura_stock(...)` | NO | YES | YES |
| `recalcular_resumen_diario_admin(text,date)` | NO | NO | YES |

## Sequence (optoweb login rate limit)

```
Client → POST /api/auth/login (no JWT)
  → createClient() with anon key
  → rpc check_rate_limit  [requires anon EXECUTE]
  → signInWithPassword
```

## Sequence (Android sync resumen)

```
Authenticated JWT → DownloadSyncCoordinator
  → rpc recalcular_resumen_diario  [requires authenticated EXECUTE + membership guard]
```

## Advisor policy

Lint 0028/0029 findings on the YES cells above are **intentional**. Suppress in Dashboard Advisors settings if desired; do not revoke.
