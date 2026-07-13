# Seed Data Specification

## Purpose

Define the seed data requirements in `supabase/seed.sql` so that `supabase db reset` produces a functional local development environment with representative data.

## Requirements

### Requirement: Seed SQL File

The project MUST have a `supabase/seed.sql` file that populates tables with minimal representative data after migration reset. The file MUST be idempotent — safe to run multiple times without errors or duplicate rows.

#### Scenario: Seed runs after db reset

- GIVEN a fresh `supabase db reset`
- WHEN `supabase/seed.sql` executes as part of the reset
- THEN the database contains at least one test optica with related pacientes, products, and dispensaciones

#### Scenario: Seed is idempotent

- GIVEN a database that was already seeded
- WHEN `supabase/seed.sql` runs again (e.g. via manual `psql` import)
- THEN no errors occur AND no duplicate primary key violations are raised

### Requirement: Representative Data Coverage

Seed data MUST include at least: one optica (tenant), three pacientes with varied profiles, two products with prices, one dispensación linked to a paciente, and one service entry. Foreign key relationships MUST be consistent.

#### Scenario: All seed entities present

- GIVEN a freshly reset and seeded database
- WHEN querying each core table
- THEN each table returns at least one row AND all foreign key references resolve

#### Scenario: Inconsistent seed fails

- GIVEN seed data with a foreign key referencing a non-existent parent
- WHEN `supabase/seed.sql` executes
- THEN it SHALL fail with a foreign key violation error

### Requirement: Development vs Production

Seed data MUST NOT contain real customer data. All names, emails, and identifiers SHALL be obviously synthetic (e.g. `paciente-a@test.com`, `Optica Demo S.A.S.`). The seed SHALL be clearly documented as development-only in the file header.

#### Scenario: Seed data is clearly synthetic

- GIVEN a developer reviews `supabase/seed.sql`
- WHEN they read the header comment and inspect values
- THEN all data uses test domains (`@test.com`, `@ejemplo.com`) AND synthetic names
