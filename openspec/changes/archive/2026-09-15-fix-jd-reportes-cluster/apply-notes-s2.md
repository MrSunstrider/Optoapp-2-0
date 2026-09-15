# S2 apply note — rpc_cierre_caja_resumen bound

## Decision

**Keep exclusive upper bound** (`fecha >= p_from AND fecha < p_to`).

## Why

Sibling repo `optoweb` callers pass exclusive ends:

- `optoweb/src/lib/cierre-caja.ts` — `p_to: period.toExclusive`
- `optoweb/src/lib/dashboard-kpis.ts` — `p_to: toExclusive`
- `optoweb/src/lib/reportes-financieros.ts` — `p_to: range.endExclusive`

Switching to `fecha <= p_to` would silently include an extra day for all web callers.

Android Cierre de Caja does **not** call this RPC (Room/PagoEffect only).

## Mitigation shipped

Migration `20260915085406_document_rpc_cierre_caja_resumen_exclusive.sql` documents the half-open contract via `COMMENT ON FUNCTION`.

Same-day usage: `p_from = D`, `p_to = D + 1 day`.
