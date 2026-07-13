# Proposal: Migration Guardrails

## Intent

Massive migration drift (99 remote-only, 21 local-only) occurred because Dashboard-first development bypassed migration files, CI never failed, and no local validation existed. The drift is explored and CI is already fixed. This change establishes permanent process and automation guardrails so drift cannot recur.

## Scope

### In Scope
- Pre-commit hook validating migration file integrity on `supabase/migrations/**` changes
- Migration conventions doc (`supabase/migrations/README.md`) with the ONE golden path
- Schema integrity validation via pgTAP or custom SQL tests in CI
- Seed data (`supabase/seed.sql`) so `db reset` works from scratch
- CI env var documentation and deployment path hardening (`db push` only, no Dashboard)

### Out of Scope
- Schema reconciliation of existing drift (exploration covers this — separate Phase 1 work)
- Renaming of existing migration files to match remote timestamps
- Android Room migration changes
- Data backfill replay (one-shot operations, not guardrails)

## Capabilities

### New Capabilities
- `migration-conventions`: Golden path for creating/applying Supabase migrations — naming, rules, Dashboard prohibition, and the git workflow around migration files
- `ci-guardrails`: CI pipeline documentation, required Supabase env vars, `db push` as the only deployment path, and the `db reset` validation step
- `pre-commit-hooks`: Git hooks (`.githooks/pre-commit`) that run `supabase db lint` when `supabase/migrations/` changes, preventing drift before it reaches CI
- `migration-tests`: Schema integrity validation (pgTAP or custom SQL) that runs against a fresh `db reset` to verify expected tables, columns, RLS policies, and functions exist
- `seed-data`: Minimal `supabase/seed.sql` with representative test data so `db reset` environments are functional

### Modified Capabilities
None — no existing specs relate to migrations or CI.

## Approach

1. **Pre-commit hook**: Create `.githooks/pre-commit` that runs `supabase db lint` on staged migration changes. Register path with `git config core.hooksPath`.
2. **Migration conventions**: Rewrite `supabase/migrations/README.md` with the golden path: `supabase migration new` → write SQL → `db lint` → `db reset` → `db diff --linked` → commit. Explicitly forbid Dashboard schema edits.
3. **Migration tests**: Add a SQL test file or pgTAP suite that validates schema invariants (key tables, RLS policies, expected functions). Hook into CI as a post-reset step.
4. **Seed data**: Create `supabase/seed.sql` with minimal representative data (a test optica, some pacientes, products) to make local dev viable.
5. **CI docs**: Add a `### Env Vars` section to the CI workflow explaining required secrets (`SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD`). Document `db push` as the sole deployment path.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `.githooks/pre-commit` | New | Git hook for migration lint |
| `supabase/migrations/README.md` | Modified | Expanded with golden path + rules |
| `supabase/seed.sql` | New | Seed data for local dev |
| `.github/workflows/supabase-ci.yml` | Modified | Add migration test step + env var comments |
| `supabase/tests/` | New | Schema integrity test files |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Pre-commit hook breaks git workflow for non-migration work | Low | Hook only triggers on `supabase/migrations/**` changes |
| Seed data becomes stale or conflicts with real schema | Medium | Seed data follows schema shape; update when schema changes |
| pgTAP requires custom Supabase image | Low | Use plain SQL validation instead if pgTAP setup is complex |

## Rollback Plan

- **Pre-commit hook**: Delete `.githooks/pre-commit` or remove `core.hooksPath` config
- **CI changes**: Revert the workflow file with `git revert`
- **Seed data**: Delete `supabase/seed.sql` and disable seed in `config.toml`
- **Migration tests**: Delete `supabase/tests/` directory

## Dependencies

- `supabase db lint` must succeed locally (requires local Supabase stack)
- `supabase db reset` requires a running local Supabase instance

## Success Criteria

- [ ] `supabase db lint` runs on every commit touching `supabase/migrations/` and blocks on failure
- [ ] Migration README documents the golden path — any dev can follow it without ambiguity
- [ ] CI runs schema integrity validation and fails on drift
- [ ] `supabase db reset` produces a functional local DB with seed data
- [ ] `supabase db diff --linked` is the only accepted deployment synchronization check
