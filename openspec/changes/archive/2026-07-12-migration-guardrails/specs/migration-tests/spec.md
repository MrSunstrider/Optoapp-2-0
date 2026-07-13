# Migration Tests Specification

## Purpose

Define schema integrity validation that runs after `supabase db reset` in CI, verifying that expected tables, columns, RLS policies, and functions exist and match their specifications.

## Requirements

### Requirement: Schema Invariant Checks

After `supabase db reset`, CI MUST run a suite of SQL-based schema assertions. The suite SHALL verify that every core table exists, every expected column has the correct type, every RLS policy is enabled, and every expected function is defined.

#### Scenario: All core tables exist after reset

- GIVEN a fresh `supabase db reset` on the latest migrations
- WHEN the schema test suite runs a `SELECT` against `information_schema.tables`
- THEN every table listed in the test manifest SHALL be present

#### Scenario: Missing table fails the suite

- GIVEN a migration that removes a core table
- WHEN the schema test suite checks for its existence
- THEN the test SHALL fail AND CI SHALL block the push

### Requirement: RLS Policy Validation

Every table that requires Row-Level Security MUST have at least one RLS policy defined AND policies MUST reference the `optica_id` column for multi-tenant isolation.

#### Scenario: RLS policy present on protected table

- GIVEN a table that requires multi-tenant isolation
- WHEN the test suite queries `pg_policies`
- THEN at least one policy is found referencing `optica_id`

#### Scenario: Table missing RLS fails

- GIVEN a table requiring RLS but with no policies defined
- WHEN the test suite checks `pg_policies`
- THEN the test SHALL fail with a message identifying the unprotected table

### Requirement: Test Suite Execution

The schema test suite SHOULD be written as plain SQL files under `supabase/tests/`. If pgTAP is feasible, the project MAY adopt it. CI MUST exit non-zero if any test assertion fails.

#### Scenario: All assertions pass

- GIVEN schema tests matching the current schema
- WHEN CI runs `supabase db reset` followed by the test suite
- THEN all tests pass AND CI proceeds

#### Scenario: Assertion failure blocks CI

- GIVEN a schema test expects a column type that no longer matches
- WHEN the test suite runs
- THEN the assertion fails AND CI exits non-zero
