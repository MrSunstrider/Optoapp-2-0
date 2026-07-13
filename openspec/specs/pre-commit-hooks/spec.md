# Pre-Commit Hooks Specification

## Purpose

Define the behavior of the `pre-commit` git hook that validates migration file integrity before commits, preventing drift from reaching CI.

## Requirements

### Requirement: Selective Hook Trigger

The pre-commit hook MUST run `supabase db lint` ONLY when staged files include paths under `supabase/migrations/`. If no staged paths match, the hook MUST exit zero without running any Supabase commands.

#### Scenario: Hook skips on non-migration changes

- GIVEN a developer stages changes to `optoapp/src/main/...` only
- WHEN they run `git commit`
- THEN the hook exits with code 0 without invoking `supabase`

#### Scenario: Hook triggers on migration changes

- GIVEN a developer stages a new file under `supabase/migrations/`
- WHEN they run `git commit`
- THEN the hook runs `supabase db lint` on the staged migration files

### Requirement: Lint Failure Blocks Commit

If `supabase db lint` returns a non-zero exit code, the pre-commit hook MUST abort the commit with an error message identifying the failing migration file.

#### Scenario: Invalid migration blocks commit

- GIVEN a staged migration file with invalid SQL
- WHEN `supabase db lint` returns a non-zero exit
- THEN the commit is aborted AND the error output indicates which file failed

#### Scenario: Valid migration allows commit

- GIVEN a staged migration file that passes all lint rules
- WHEN `supabase db lint` returns exit code 0
- THEN the commit proceeds normally

### Requirement: Hook Registration

The project SHALL use `git config core.hooksPath .githooks` to register the hook directory. The `.githooks/pre-commit` file MUST be committed to the repository and executable on POSIX systems.

#### Scenario: Fresh clone activates hooks

- GIVEN a fresh clone of the repository
- WHEN the developer runs `git config core.hooksPath .githooks`
- THEN `git commit` SHALL execute `.githooks/pre-commit`
