# Tasks: Fix Analisis Financiero Categorias

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~150 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single-pr |
| Delivery strategy | single-pr |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write failing SQL tests (before migration)

- [x] 1.1 Create `supabase/tests/test_case_mapping.sql` — unit test verifying CASE maps `(Monofocal,Resina)→lente_monofocal`, `(Progresivo,*)→lente_progresivo`, etc. (spec: R23 scenario — margen_por_categoria mapping)
- [x] 1.2 Create `supabase/tests/test_margen_categoria.sql` — integration test calling `rpc_analisis_mensual('o1','2026-07-01')` verifying `margen_por_categoria` has non-zero `ventas` (spec: R23 scenario)
- [x] 1.3 Create `supabase/tests/test_stock_estancado.sql` — integration test verifying sold monturas show real `ultima_venta`, never-sold show `dias_sin_venta=999` (spec: R23 scenarios 2 & 3)
- [x] 1.4 Create `supabase/tests/test_json_structure.sql` — integration test validating 9 categories present, all fields exist in JSON (spec: R23 scenario — full coverage)

## Phase 2: GREEN — Implement the migration

- [x] 2.1 Create `supabase/migrations/20260709000003_fix_analisis_mensual_categorias.sql` with `CREATE OR REPLACE FUNCTION` — inline `margen_por_categoria` CTE + fixed `stock_estancado` CTE (design: Decision 1 & 2), plus `REVOKE/GRANT EXECUTE`
- [x] 2.2 Apply migration, then re-run T1–T4 — all must PASS

## Phase 3: VERIFY — Android regression

- [x] 3.1 Run `./gradlew :optoapp:testDebugUnitTest` — confirm `AnalisisMensualTest` and `AnalisisDetalleScreenTest` pass unchanged (T5)
