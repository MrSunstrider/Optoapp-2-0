# Exploration: Supabase Migration Drift

## Current State

The project has a **critical drift** between local migration files (`supabase/migrations/`) and the remote Supabase database's `_supabase_migrations` tracking table.

| Metric | Local | Remote | Delta |
|--------|-------|--------|-------|
| Migration files (.sql) | 103 | — | — |
| Applied migrations | — | 183 | — |
| Versions in common | — | — | 82 |
| Local-only (not applied remotely) | 21 | — | ✗ |
| Remote-only (no local file) | — | 99 | ✗ |

The CI workflow (`supabase-ci.yml`) previously masked this entirely with `continue-on-error: true` on both `supabase db lint` and `supabase db diff --linked`. The CI has **already been fixed** (commit `1cc79c2`) — `continue-on-error` was removed, `supabase db reset` was added, and the diff step now fails on mismatch.

## Root Cause Analysis

### How Did the Drift Happen?

**Root cause chain:**

1. **Dashboard-first development** (primary factor): Between ~2026-06-17 and 2026-07-12, SQL changes were applied directly to the remote Supabase database through:
   - The Supabase Dashboard SQL editor (fast iteration on financial features)
   - Edge function deployments that may have included schema changes
   - Direct `supabase db push` (which applies local migrations to remote but was used out of sync)

   Evidence: The remote has 99 migrations with NO corresponding local file. The names clearly show iterative development phases (backfills, judgment-day fixes, p1-p7 series, cancel_X, etc.) that were never committed as migration files.

2. **Timestamp fragmentation**: At least 21 local migrations (e.g., `20260704000000_create_ventas_table.sql`) have a DIFFERENT timestamp than their remote counterpart (e.g., remote `20260705022952` for the same `create_ventas_table` name). This happens because:
   - The migration was applied remotely first (generating a timestamp at apply time)
   - Later, a local file was created via `supabase migration new` (generating a NEW timestamp)
   - Both exist independently, and Supabase treats them as DIFFERENT migrations

   Only 82 of 103 local versions have an exact match in the remote `_supabase_migrations` table.

3. **`continue-on-error: true` in CI** (enabler): The original CI workflow had both lint and diff steps configured with `continue-on-error: true`, meaning they NEVER failed. No PR or push was ever blocked by migration drift. The drift accumulated over weeks undetected.

   ```yaml
   # ORIGINAL CI (before fix) — never failed
   - name: Lint migrations
     run: supabase db lint
     continue-on-error: true     # ← SILENT PASS
   - name: Check migrations are consistent
     run: supabase db diff --linked
     continue-on-error: true     # ← SILENT PASS
   ```

4. **No `db reset` validation**: The original CI never ran `supabase db reset` to verify that migrations apply cleanly from scratch. This meant even basic migration failures (e.g., dependency ordering, missing `IF NOT EXISTS`) were invisible.

5. **No pre-commit enforcement**: There are no git hooks that verify migration consistency before allowing commits that touch `supabase/migrations/`.

6. **Missing `seed.sql`**: The config.toml references `[db.seed] sql_paths = ["./seed.sql"]` but the file does not exist. A `db reset` would crash or silently skip seeding.

### Remote-Only Migration Categories

Of the 99 remote-only migrations:

| Category | Count | Examples |
|----------|-------|----------|
| Data backfills (venta_id, costos, pagos) | ~35 | `backfill_venta_id_remedial`, `p1-p7` series, `cancel_*` |
| Schema changes with local equivalents (different timestamp) | ~15 | `create_ventas_table` (remote `20260705022952` vs local `20260704000000`) |
| Schema-only (no local equivalent) | ~15 | `add_arqueo_caja`, `add_arqueo_diferencia_columns`, `add_gastos_recurrentes_columns` |
| Security/hardening fixes | ~10 | `fix_security_advisors`, `revoke_from_public`, `anon_policy_pin_attempts` |
| Judgment Day fixes | ~14 | `jd_fix3_exclude_anulaciones`, `jd_r3_*` series |
| RPC refactors (financial) | ~10 | `fix_rpc_deudores_dedup`, `fix_rpc_analisis_mensual_proyeccion` |

### Local-Only Migration Categories

Of the 21 local-only:

| Category | Count | Examples |
|----------|-------|----------|
| Same name, different timestamp (should match remote but doesn't) | 21 | ALL of them — every local migration from `20260620000000` onward has a different timestamp than its remote counterpart |

This means **21 local migration files need reconciliation** — they exist locally but the remote already has their content (or something equivalent) under a different version number.

## Impact Assessment

| Impact | Severity | Description |
|--------|----------|-------------|
| **Local dev is broken** | 🔴 Critical | `supabase db reset` applies 103 local migrations, producing a DB that does NOT match remote. Any dev relying on `db reset` gets a wrong schema. |
| **New migrations can't be safely written** | 🔴 Critical | A migration written against local state may attempt to CREATE tables that already exist remotely, or fail to account for remote-only columns/functions. |
| **CI drift check fails** | 🟡 High | Even with the fix, `supabase db diff --linked` will FAIL because local ≠ remote. The CI is now correct but the PROJECT isn't. |
| **Data loss risk** | 🟡 High | Running `supabase db push` with local-only migrations could drop remote-only objects. Running remote-only migrations locally would change the schema unpredictably. |
| **No seed data** | 🟡 Medium | `seed.sql` is missing entirely. Fresh local databases have no test/seed data. |
| **Sync/Android integration risk** | 🟡 Medium | If the Android app depends on specific RPC functions or schema that exist only in one place, sync behavior diverges between local dev and production. |
| **Migration history opacity** | 🟢 Low | The remote migration history is recoverable from `_supabase_migrations` but the data backfill operations cannot be replayed deterministically. |

## Recommended Sync Approach

### Approach: Schema Dump + Reconciliation (Recommended)

```
1. Dump remote schema     → Get current remote truth
2. Diff local vs remote   → Identify all deltas
3. Create reconciliation  → Single "catch-up" migration
4. Reconcile versions     → Align local file names with remote versions
5. Verify with db reset   → Confirm local == remote
6. Lock down process      → Guardrails
```

| Step | Action | Effort | Risk |
|------|--------|--------|------|
| 1 | `supabase db dump --linked -f supabase/schema_dump.sql` | Low | None |
| 2 | Compare dump vs local migrations to generate missing SQL | High | Needs careful review of 99 remote-only migration contents |
| 3 | Create `supabase/migrations/YYYYMMDDHHMMSS_reconcile_drift.sql` | Medium | MUST use `IF NOT EXISTS` / `CREATE OR REPLACE` — data backfills can't be replayed |
| 4 | Rename local files to match remote timestamps where name matches | Medium | Git history rename tracking works; 21 files affected |
| 5 | `supabase db reset` then `supabase db diff --linked` | Low | Must show no diff |
| 6 | CI + pre-commit hooks + migration convention docs | Low-Medium | Convention enforcement |

### Alternative: Blow Away Local Migrations, Re-export from Remote

```
1. Delete all local migration files
2. supabase db dump --linked to get current schema
3. Create single baseline migration
4. Continue from there
```
- **Pro**: Clean slate, no reconciliation needed
- **Con**: Loses ALL migration history. The entire `supabase/migrations/` directory is gone. Android Room migrations may depend on specific migration version order.

### Alternative: Selective Backfill (Only Schema, Skip Data)

```
1. For each remote-only migration: reconstruct SQL into local file
2. Skip data-only backfills (they're one-shot)
3. Keep existing local files where they match remote in content
```
- **Pro**: Preserves migration history, selective
- **Con**: Labor-intensive to reconstruct 99 migrations from a schema dump alone. Need to distinguish schema vs data changes.

## Guardrail Recommendations

### 1. CI Guard (ALREADY FIXED)
- ✅ Remove `continue-on-error: true` — DONE
- ✅ Add `supabase db reset` step — DONE
- ✅ Keep `supabase db diff --linked` as a blocking step — DONE

### 2. Pre-Commit Hook (RECOMMENDED — HIGH PRIORITY)

Add a pre-commit git hook (`.husky/pre-commit` or `.githooks/pre-commit`) that runs when `supabase/migrations/` changes:

```bash
# .githooks/pre-commit — check that migrations are consistent
if git diff --cached --name-only | grep -q "^supabase/migrations/"; then
  supabase db lint || exit 1
fi
```

This catches drift BEFORE it reaches the CI.

### 3. Migration Convention (RECOMMENDED — MEDIUM PRIORITY)

Update `supabase/migrations/README.md` to clarify the process:

> **Rules**:
> 1. ALL schema changes MUST go through migration files. NEVER use the Supabase Dashboard SQL editor for production schema changes.
> 2. Run `supabase db diff --linked` BEFORE writing a new migration to check for existing drift.
> 3. Run `supabase db reset` AFTER creating a migration to verify it applies cleanly.
> 4. Data backfills are one-shot operations — document them in migration files even if irreversible.
> 5. Migration files are immutable once applied. Never rename or renumber a migration that has been applied remotely.

### 4. Migration Review Checklist (RECOMMENDED — LOW EFFORT)

Add to PR template a checklist for Supabase changes:
- [ ] `supabase db lint` passes locally
- [ ] `supabase db reset` applies cleanly from scratch
- [ ] `supabase db diff --linked` shows no drift (or drift is intentional and documented)
- [ ] Migration uses `IF NOT EXISTS` / `CREATE OR REPLACE` where safe
- [ ] No `continue-on-error` on migration checks in CI

### 5. Seed Data (RECOMMENDED — MEDIUM PRIORITY)

Create `supabase/seed.sql` with minimal seed data so local dev environments have functional test data.

### 6. Migration CI Runners (OPTIONAL)

Consider running `supabase db reset` in a matrix across past migration snapshots to verify forward/backward compatibility (overkill for now — revisit if migration count exceeds 200).

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Reconciliation migration conflicts with existing data | Medium | High | Use `IF NOT EXISTS` / `CREATE OR REPLACE` on all DDL. Skip data backfills. |
| Local dev env diverges from remote after reconciliation | Low | Medium | Lock down with CI + pre-commit. |
| Android Room migration version depends on specific Supabase schema version | Low | High | Check `IMPROVEMENT-PLAN.md` for known issues. Room migrations are sequential and independent of Supabase migration versions. |
| Git history becomes confusing after file renames | Medium | Low | `git mv` is a rename. Keep `git log --follow` for each file. |
| Reconciliation migration is too large and hard to review | High | Medium | Split into logical groups (schema, RPCs, security, indexes) across 2-3 migrations. |
| Data-only remote migrations can never be replayed locally | High | Medium | Accept this — document which backfills are one-shot. The schema dump covers what exists. |

## Ready for Proposal

**Yes.** The exploration is complete and the path forward is clear:

1. **Phase 1** — Dump remote schema, diff, and reconcile. This produces a "catch-up" migration and aligned file names.
2. **Phase 2** — Add pre-commit hook and update migration conventions.
3. **Phase 3** — Create seed data and finalize guardrails.

The CI has already been hardened (the `continue-on-error` fix is in place). What remains is schema reconciliation and process guardrails. The biggest effort item is reconstructing the remote-only migrations into local files — estimated at 2-3 hours of careful SQL work.
