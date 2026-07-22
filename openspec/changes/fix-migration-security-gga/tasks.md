# Tasks: Fix Migration Security — GGA

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~200 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Decision needed before apply | No |
| Chain strategy | size-exception |

```
Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low
```

## Phase 1: Sequential Migration Files

5 new DDL-only migrations, applied in order C-3 → C-4 → C-1 → C-2 → W-3.

- [x] 1.1 **M1 — C-3**: Create `20260721000000_fix_c3_exclude_anulacion_cierre_caja.sql` — `CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen` adding `AND tipo IS DISTINCT FROM 'Anulación'` to the WHERE clause (line 311), preserving existing `SECURITY INVOKER`, `SET search_path = public`, membership+role guards, and `REVOKE`/`GRANT EXECUTE` to `authenticated`, `service_role`.
- [x] 1.2 **M2 — C-4**: Create `20260721000001_fix_c4_recalcular_resumen_guard.sql` — `CREATE OR REPLACE FUNCTION public.recalcular_resumen_diario` inserting `IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN RAISE EXCEPTION 'Access denied'; END IF;` after `BEGIN` (line 31). Preserves `SECURITY DEFINER`, `SET search_path = ''`, `OWNER TO postgres`, and existing grants.
- [x] 1.3 **M3 — C-1**: Create `20260721000002_fix_c1_restore_check_rate_limit.sql` — `CREATE OR REPLACE FUNCTION public.check_rate_limit(TEXT, INT, INT)` verbatim from `20260628000000` (lines 16–51), `SECURITY DEFINER`, `SET search_path TO 'public'`, plus `GRANT EXECUTE ON FUNCTION public.check_rate_limit(TEXT, INT, INT) TO anon;`.
- [x] 1.4 **M4 — C-2**: Create `20260721000003_fix_c2_sentinel_do_block_guard.sql` — DO block checking `pg_policies` for `has_optica_role`-based `pacientes_select` policy. If found, raise NOTICE and skip. Otherwise, `DROP POLICY IF EXISTS` + `CREATE POLICY` for basic member policies on `pacientes`, `evaluaciones`, `dispensaciones`, `servicios_extra`, `pagos` (reproducing `20260414120000` lines 90–154).
- [x] 1.5 **M5 — W-3**: Create `20260721000004_fix_w3_app_releases_anon_policy.sql` — `CREATE POLICY "anon_can_read_releases" ON public.app_releases FOR SELECT TO anon USING (true);`.
