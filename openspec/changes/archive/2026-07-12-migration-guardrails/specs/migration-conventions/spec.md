# Migration Conventions Specification

## Purpose

Define the golden path for creating, applying, and committing Supabase database migrations so schema changes are always reproducible, reviewable, and never modified outside the migration file workflow.

## Requirements

### Requirement: Golden Path Workflow

Every schema change MUST follow this exact sequence: `supabase migration new` -> write SQL -> `supabase db lint` -> `supabase db reset` -> `supabase db diff --linked` -> commit. Deviations SHALL be documented in `supabase/migrations/README.md` as violations.

#### Scenario: Golden path succeeds from start to commit

- GIVEN a developer needs to add a new table
- WHEN they run `supabase migration new add_foo`, write the SQL, run `db lint`, `db reset`, and `db diff --linked`
- THEN the resulting migration file exists in `supabase/migrations/` with a valid timestamp prefix AND `db diff --linked` reports zero drift

#### Scenario: Dashboard schema edits are detected

- GIVEN a developer modifies the schema via Supabase Dashboard
- WHEN they run `supabase db diff --linked`
- THEN the diff SHALL show uncommitted changes AND the README SHALL call this a violation

### Requirement: Migration File Naming

Migration file names MUST follow the format `YYYYMMDDHHMMSS_description.sql` (timestamp + snake_case description). Files MUST be immutable after commit — no rewriting, no squashing, no reordering.

#### Scenario: Valid naming convention

- GIVEN a migration file `202607120001_add_pacientes_table.sql`
- WHEN linted by `supabase db lint`
- THEN the file passes without naming-related errors

#### Scenario: Invalid naming is rejected

- GIVEN a migration file `my_migration.sql` without a timestamp prefix
- WHEN linted by `supabase db lint`
- THEN it MUST NOT pass AND the developer MUST rename it before commit

### Requirement: README as Single Source of Truth

The `supabase/migrations/README.md` SHALL document the golden path, the Dashboard prohibition, the migration lifecycle, and a troubleshooting section. Every project onboarding SHALL start by reading this file.

#### Scenario: New developer follows README

- GIVEN a new developer reads `supabase/migrations/README.md`
- WHEN they follow the golden path for a schema change
- THEN all steps succeed AND they commit a valid migration file on first attempt
