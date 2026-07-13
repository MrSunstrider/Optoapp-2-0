# CI Guardrails Specification

## Purpose

Define the CI pipeline requirements for Supabase schema validation so that every push to `main` touching migrations is automatically linted, reset-tested, and diff-verified against the linked project, with no deployment path bypassing `supabase db push`.

## Requirements

### Requirement: CI Env Vars

The `supabase-ci.yml` workflow MUST declare `SUPABASE_ACCESS_TOKEN` and `SUPABASE_DB_PASSWORD` as required secrets. The workflow SHALL fail fast with a clear message if either is missing.

#### Scenario: Missing env var fails CI early

- GIVEN a push to `main` that modifies `supabase/migrations/`
- WHEN `SUPABASE_ACCESS_TOKEN` is not set in the CI environment
- THEN the workflow SHALL fail at the setup step with an error indicating the missing secret

#### Scenario: All env vars present

- GIVEN a push to `main` that modifies `supabase/migrations/`
- WHEN all required secrets are configured
- THEN CI proceeds to the validation steps

### Requirement: CI Validation Pipeline

On every push/PR to `main` touching `supabase/migrations/`, CI MUST run `supabase db lint`, then `supabase db reset`, then `supabase db diff --linked`. If any step fails, the pipeline MUST NOT pass.

#### Scenario: Lint failure blocks CI

- GIVEN a migration with invalid SQL syntax
- WHEN CI runs `supabase db lint`
- THEN the pipeline fails and the migration is not applied

#### Scenario: Drift detected in diff

- GIVEN local migrations are out of sync with the linked project
- WHEN CI runs `supabase db diff --linked`
- THEN the diff SHALL report drift AND the pipeline SHALL fail

### Requirement: Sole Deployment Path

`supabase db push` SHALL be the only accepted method for deploying migrations to the linked project. The README and CI docs MUST explicitly forbid using the Supabase Dashboard for schema changes.

#### Scenario: Dashboard change detected after deploy

- GIVEN a schema change was applied via Dashboard
- WHEN `supabase db diff --linked` runs in CI on the next push
- THEN CI fails, flagging the drift introduced outside the golden path
