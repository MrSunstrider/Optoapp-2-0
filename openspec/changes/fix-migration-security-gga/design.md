# Design: Fix Migration Security — GGA

## Technical Approach

5 sequential, idempotent DDL-only migrations. Each migration is self-contained (CREATE OR REPLACE + DROP IF EXISTS + CREATE POLICY pattern). No data migration, no app code changes. Order: C-3 → C-4 → C-1 → C-2 → W-3 (least risky first).

## Architecture Decisions

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| D1 | Migration naming | `YYYYMMDDHHMMSS_descriptive.sql` | Matches existing 54 migrations |
| D2 | Idempotency pattern | `CREATE OR REPLACE` for functions; `DROP POLICY IF EXISTS` + `CREATE POLICY` for policies; sentinel guards for DO blocks | Each migration safe to re-run |
| D3 | C-3 fix approach | Add `AND tipo IS DISTINCT FROM 'Anulación'` to existing WHERE clause in function body | One-line change, consistent with `rpc_deudores` and `rpc_analisis_mensual` in same source migration |
| D4 | C-4 fix approach | Add `is_optica_member` guard at function top (membership only, no role check) | Recalculation utility accessible to any member; matches pattern in `rpc_deudores` |
| D5 | C-1 fix approach | `CREATE OR REPLACE FUNCTION` verbatim from `20260628000000`, plus `GRANT EXECUTE TO anon` | Web companion needs anon pre-auth access; function is SECURITY DEFINER, only touches `pin_attempts` |
| D6 | C-2 fix approach | Sentinel DO block checking for `has_optica_role`-based policies before allowing rerun | Guards disaster recovery without modifying old migrations |
| D7 | W-3 fix approach | Add `anon_can_read_releases` policy (`FOR SELECT TO anon USING (true)`) | Aligns with intent of Block 3 in `20260714000005`; `app_releases` contains no tenant data |

## Migration Sequence

```
M1: 20260721000000_fix_c3_exclude_anulacion_cierre_caja.sql   — C-3
M2: 20260721000001_fix_c4_recalcular_resumen_guard.sql         — C-4
M3: 20260721000002_fix_c1_restore_check_rate_limit.sql         — C-1
M4: 20260721000003_fix_c2_sentinel_do_block_guard.sql          — C-2
M5: 20260721000004_fix_w3_app_releases_anon_policy.sql         — W-3
```

## Per-Fix Migration Content

### M1 — C-3: rpc_cierre_caja_resumen Anulación filter
Source body: `20260716045310_fix_financial_rpcs_security.sql` lines 275–320.
Change: line 311 `AND fecha < p_to;` → `AND fecha < p_to AND tipo IS DISTINCT FROM 'Anulación';`.
Includes full `CREATE OR REPLACE FUNCTION` + `REVOKE`/`GRANT EXECUTE` to authenticated, service_role.

### M2 — C-4: recalcular_resumen_diario membership guard
Source body: `20260716012521_fix_recalcular_resumen_security.sql` lines 14–139.
Change: insert after `BEGIN` (line 31): `IF NOT app_private.is_optica_member(auth.uid(), p_optica_id) THEN RAISE EXCEPTION 'Access denied'; END IF;`.
Preserves `SECURITY DEFINER`, `SET search_path = ''`, `OWNER TO postgres`, and grants.

### M3 — C-1: Restore check_rate_limit
Source body: `20260628000000_grant_anon_check_rate_limit.sql` lines 16–51.
`CREATE OR REPLACE FUNCTION public.check_rate_limit(TEXT, INT, INT)` verbatim (`SECURITY DEFINER`, `SET search_path TO 'public'`).
Plus: `GRANT EXECUTE ON FUNCTION public.check_rate_limit(TEXT, INT, INT) TO anon;`.

### M4 — C-2: Sentinel guard for foundational DO block
DO block checking: `IF EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'pacientes' AND policyname = 'pacientes_select' AND pg_get_expr(polqual, polrelid)::text LIKE '%has_optica_role%') THEN RAISE NOTICE 'Restrictive policies exist — skipping'; ELSE` ... re-run `DROP POLICY IF EXISTS` + `CREATE POLICY` for basic member policies on the 5 tables (pacientes, evaluaciones, dispensaciones, servicios_extra, pagos). `END IF;`

### M5 — W-3: anon SELECT policy on app_releases
```sql
CREATE POLICY "anon_can_read_releases" ON public.app_releases
  FOR SELECT TO anon USING (true);
```

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| M1 changes financial reports downward | Correct: voided payments should not count as revenue. Consistent with all other RPCs. |
| M2 guard rejects legitimate callers | Checks optica membership, not role. Any member can recalculate. |
| M3 exposes rate-limit logic | Function only touches `pin_attempts` table. No PII. |
| M4 DO block reruns unnecessarily | Sentinel prevents rerun if restrictive policies already exist. |
| M5 exposes version metadata without auth | `app_releases` has no tenant data; APK URLs already public. |
