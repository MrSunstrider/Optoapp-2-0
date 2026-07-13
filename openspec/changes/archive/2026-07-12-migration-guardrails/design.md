# Design: Migration Guardrails

## Technical Approach

Five independent capabilities that together prevent migration drift:

1. **Pre-commit hook** — shell script in `.githooks/pre-commit` that selectively runs `supabase db lint` when staged files touch `supabase/migrations/`
2. **Migration conventions** — rewrite `supabase/migrations/README.md` with the golden path
3. **CI guardrails** — document required env vars, add schema test step to existing CI pipeline
4. **Migration tests** — plain SQL schema-integrity assertions in `supabase/tests/` following the existing pattern
5. **Seed data** — `supabase/seed.sql` with minimal representative entities

No changes to Android code or Supabase functions. All changes are within `supabase/` and `.githooks/`.

## Architecture Decisions

### Decision: Pre-commit hook mechanism

| Option | Tradeoff |
|--------|----------|
| **Husky** (Node) | Requires `package.json` + npm install — no existing Node in project |
| **Lefthook** (Go binary) | Adds tooling dependency, not project-wide |
| **.githooks/ + core.hooksPath** ✅ | Zero dependencies, POSIX shell, no runtime, works on GitHub Codespaces |

**Choice**: `.githooks/pre-commit` registered via `git config core.hooksPath .githooks`.

**Rationale**: The project has no Node.js dependency. This is a pure shell script that runs `supabase db lint` only when `git diff --cached --name-only` matches `supabase/migrations/*`. It's the simplest, most portable option. The hook checks for Supabase CLI availability and exits gracefully if absent (developer without Supabase can still commit other code).

### Decision: Migration test framework

| Option | Tradeoff |
|--------|----------|
| **pgTAP** | Requires pgTAP extension installed in Supabase image — extra setup, fragile |
| **Custom SQL assertions** ✅ | `DO $$ ... ASSERT ... $$` blocks — matches existing pattern in `supabase/tests/`, no deps |

**Choice**: Plain PL/pgSQL `DO` blocks with `ASSERT`. One file `test_schema_integrity.sql` that validates core tables, columns, RLS policies, and functions via `information_schema` and `pg_policies`. CI runs them against the reset database via `psql -f` or embedded in a migration-like step.

**Rationale**: The project already has 5 tests in `supabase/tests/` using this exact pattern. Adding pgTAP would require modifying the Docker image and learning a new DSL. Plain SQL assert works with zero configuration.

### Decision: Migration naming convention

**Choice**: `YYYYMMDDHHmmSS_description.sql` — 14-digit UTC timestamp + snake_case. Let `supabase migration new` generate the prefix.

**Rationale**: Already the existing convention in the repo. `supabase migration new` auto-generates the timestamp. Manual naming risks collisions. The README will codify this rule.

### Decision: Seed data idempotency

**Choice**: `ON CONFLICT DO NOTHING` for all inserts, with a header comment stating "Development only — not for production."

**Rationale**: The spec requires idempotency. `DELETE FROM ... ; INSERT` is unsafe (can cascade). `ON CONFLICT` on PK or unique constraint is the standard Postgres pattern. Seed includes: 1 optica, 3 pacientes, 2+ products, 1 dispensación, 1 service entry.

## Data Flow

```text
Developer commit
    │
    ▼
.git/pre-commit ──→ git diff --cached --name-only
    │                      │
    │             supabase/migrations/?      No  → exit 0
    │                      │
    │                     Yes
    │                      ▼
    │              supabase db lint
    │                      │
    │                 Fail? → abort commit
    │                      │
    │                     Pass → commit proceeds
    │
    ▼
Git push
    │
    ▼
GitHub CI (supabase-ci.yml)
    │
    ├── supabase start
    ├── supabase db lint         (naming + SQL syntax)
    ├── supabase db reset        (apply all migrations + seed)
    ├── Run schema tests         (plain SQL asserts via psql)
    └── supabase db diff --linked (detect remote drift)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `.githooks/pre-commit` | Create | Shell hook: selective `supabase db lint` on migration changes |
| `supabase/migrations/README.md` | Modify | Rewrite with golden path, Dashboard prohibition, troubleshooting |
| `supabase/seed.sql` | Create | Idempotent seed data (test optica, pacientes, products) |
| `supabase/tests/test_schema_integrity.sql` | Create | Schema assertions: core tables, RLS policies, functions |
| `.github/workflows/supabase-ci.yml` | Modify | Add test step after `db reset`, env var comments at top |
| `supabase/config.toml` | No change | `[db.seed]` already points to `./seed.sql` |

## Interfaces / Contracts

**Pre-commit hook contract**: Bash script, executable (`chmod +x`), POSIX-compatible.
- Exit 0 if no migration files staged
- Exit 0 if all staged migrations pass lint
- Exit 1 and print `supabase db lint` output on failure

**Schema test contract**: SQL file runnable via `psql -f` against a reset database.
- Uses `DO $$ ... $$` blocks with `ASSERT`
- Exits with non-zero error code on any failed assertion
- Queries `information_schema.tables`, `information_schema.columns`, `pg_policies`, `pg_proc`

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Pre-commit hook | Selective trigger, lint pass/fail | Bash unit test (mock `supabase` binary) |
| Schema tests | Core tables exist, RLS on all tables, expected functions | SQL assertions in `test_schema_integrity.sql` |
| Seed data | Idempotency, FK consistency | Manual `supabase db reset` + verify rows |
| CI pipeline | Full workflow passes | PR to `main` triggering `supabase-ci.yml` |

## Migration / Rollout

No migration required. All artifacts are new files or rewrites of non-critical docs.

**Rollback**: Delete `.githooks/`, revert `supabase-ci.yml`, revert `README.md` via git, delete `seed.sql` and `test_schema_integrity.sql`.

## Open Questions

- [ ] Does every developer already have `supabase` CLI installed locally? If not, the pre-commit hook should warn gracefully rather than crash.
- [ ] Should we add a `setup.sh` or `init` script that runs `git config core.hooksPath .githooks` automatically?
