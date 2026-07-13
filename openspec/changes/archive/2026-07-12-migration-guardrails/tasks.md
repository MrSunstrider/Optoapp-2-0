# Tasks: Migration Guardrails

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~250-300 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All guardrails | Single PR | 5 files, ~250-300 lines, independent scope |

## Phase 1: Pre-commit Hook (TDD)

- [x] 1.1 [RED] Write scenarios validating hook behavior: skip on non-migration changes, block on lint failure, pass on clean migration
- [x] 1.2 [GREEN] Create `.githooks/pre-commit` with `git diff --cached --name-only` filter + `supabase db lint` on match
- [x] 1.3 [REFACTOR] Add `command -v supabase` graceful skip, descriptive error messages, ensure executable (`chmod +x`)

## Phase 2: Schema Integrity Tests (TDD)

- [x] 2.1 [RED] Write `supabase/tests/test_schema_integrity.sql` with `DO $$ ASSERT` blocks for core tables, RLS on `optica_id`, expected functions
- [x] 2.2 [GREEN] Run `supabase db reset` + `psql -f` against local DB — verify all assertions pass on current schema
- [x] 2.3 [REFACTOR] Group assertions by domain (tables, columns, RLS, functions), add descriptive failure messages per assertion

## Phase 3: Seed Data (TDD)

- [x] 3.1 [RED] Verify `db reset` produces empty core tables (no development data before seed)
- [x] 3.2 [GREEN] Create `supabase/seed.sql` with `ON CONFLICT DO NOTHING` inserts (1 optica, 3 pacientes, products, dispensación, service entry)
- [x] 3.3 [REFACTOR] Add dev-only header comment, synthetic test domains (`@test.com`), verify FK consistency across all inserts

## Phase 4: CI & Documentation

- [x] 4.1 Modify `.github/workflows/supabase-ci.yml` — add schema test step after `db reset`, document required secrets (`SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD`) in comments
- [x] 4.2 Rewrite `supabase/migrations/README.md` with golden path workflow, Dashboard prohibition, migration lifecycle rules, and troubleshooting section
