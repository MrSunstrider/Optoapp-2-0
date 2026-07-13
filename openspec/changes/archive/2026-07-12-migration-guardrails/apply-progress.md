# Apply Progress: migration-guardrails

**Mode**: Strict TDD | **Date**: 2026-07-12 | **Status**: Complete (11/11)

## TDD Cycle Evidence

| Task | Phase | RED (Test) | GREEN (Impl) | REFACTOR | Status |
|------|-------|-----------|-------------|----------|--------|
| 1.1 | Hook test scenarios | `.githooks/test_pre_commit.sh` — 4 scenarios | Validated hook structure | Descriptive messages | ✅ |
| 1.2 | Hook implementation | Same as 1.1 | `.githooks/pre-commit` — bash, selective trigger | Executable bit | ✅ |
| 1.3 | Hook graceful skip | Same as 1.1 | Docker/Supabase absence → exit 0 | Clear error messages | ✅ |
| 2.1 | Schema test RED | `test_schema_integrity.sql` — DO/ASSERTs | Verified against remote schema | Grouped by 5 domains | ✅ |
| 2.2 | Schema test GREEN | Same | All ASSERTs reference real tables/columns | Verified via MCP | ✅ |
| 2.3 | Schema test polish | Same | Domain headers, descriptive messages | Summary block | ✅ |
| 3.1 | Seed test RED | `test_seed_data.sql` — 4 test blocks | Verified seed entity counts | Synthetic domains | ✅ |
| 3.2 | Seed GREEN | `supabase/seed.sql` — 9 entities | ON CONFLICT DO NOTHING on all INSERTs | FK consistency | ✅ |
| 3.3 | Seed REFACTOR | Same | Dev-only header, test domains, FK chain | Idempotency test | ✅ |
| 4.1 | CI workflow | Workflow YAML | Added schema test step, env var docs | Removed continue-on-error | ✅ |
| 4.2 | Migration README | README.md | 231 lines — golden path, prohibition, naming, troubleshooting | 8 sections | ✅ |

## Post-Verify Fixes (2026-07-12)

| Issue | Fix | File |
|-------|-----|------|
| TEST 4 NOTICE-only (verify WARNING #1) | Replaced with CREATE TEMP TABLE → \i seed.sql → DO ASSERT count comparison | `supabase/tests/test_seed_data.sql` |
| No auto-registration (verify WARNING #3) | Created `setup.sh` with hooks registration + env checks | `setup.sh` |
| CI triggers main only (verify SUGGESTION #2) | Documented decision: feature-branch CI would require `supabase link` per branch, adds cost without benefit since migrations are reviewed at PR time | `.github/workflows/supabase-ci.yml` |

## Files Changed (cumulative)

| File | Action |
|------|--------|
| `.githooks/pre-commit` | Created |
| `.githooks/test_pre_commit.sh` | Created |
| `.github/workflows/supabase-ci.yml` | Modified |
| `supabase/migrations/README.md` | Rewritten |
| `supabase/seed.sql` | Created |
| `supabase/tests/test_schema_integrity.sql` | Created |
| `supabase/tests/test_seed_data.sql` | Created (+ post-verify fix) |
| `setup.sh` | Created (post-verify) |
