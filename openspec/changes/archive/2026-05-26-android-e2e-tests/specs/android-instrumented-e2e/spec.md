# Supabase Instrumented E2E Tests Specification

## Purpose

Define instrumented tests that verify real Supabase integration for Auth and Sync flows. These tests hit a dedicated test Supabase project, validating authentication, session persistence, and data synchronization end-to-end.

## Requirements

### Requirement: Test Supabase Infrastructure

Tests MUST use a dedicated Supabase project separate from dev/prod. Credentials SHALL be provided via `BuildConfig` fields populated from `local.properties` (gitignored) or GitHub Secrets in CI. No test MUST hardcode Supabase URLs or keys.

| Concern | Rule |
|---------|------|
| Project | Dedicated test project (free tier acceptable) |
| Credentials source | `BuildConfig.SUPABASE_TEST_URL`, `BuildConfig.SUPABASE_TEST_ANON_KEY` |
| Storage | `local.properties` (local), GitHub Secrets (CI) |
| Isolation | MUST NOT share data with dev/prod projects |

#### Scenario: Credentials loaded from BuildConfig

- GIVEN `local.properties` contains `supabase.test.url` and `supabase.test.anon.key`
- WHEN the test APK is built with `assembleDebugAndroidTest`
- THEN `BuildConfig.SUPABASE_TEST_URL` and `BuildConfig.SUPABASE_TEST_ANON_KEY` MUST be populated

#### Scenario: Missing credentials fail fast

- GIVEN no test Supabase credentials are configured
- WHEN a Supabase instrumented test starts
- THEN the test MUST fail with a clear assertion message (not a null pointer)

### Requirement: Auth Flow Tests

Tests MUST verify register, login, and session persistence against real Supabase Auth. Each test run SHALL use a unique test user (timestamp-suffixed email) to avoid collisions.

#### Scenario: Register new user

- GIVEN a unique email not yet registered in the test project
- WHEN the test calls Supabase Auth signup with valid credentials
- THEN a session MUST be returned and `currentSession` MUST be non-null

#### Scenario: Login with existing user

- GIVEN a test user already registered in the test project
- WHEN the test calls Supabase Auth login with correct credentials
- THEN a session MUST be returned and `currentSession.user.id` MUST match the registered user

#### Scenario: Login with wrong password fails

- GIVEN a registered test user
- WHEN the test calls Supabase Auth login with an incorrect password
- THEN an auth error MUST be thrown and `currentSession` MUST remain null

#### Scenario: Session persists across restarts

- GIVEN a user has logged in and a session exists
- WHEN the test calls `supabase.auth.retrieveSession()`
- THEN the session MUST still be valid (not expired) if within the token lifetime

### Requirement: Google OAuth Flow

Tests SHOULD verify the OAuth redirect flow. Since automated Google sign-in is non-trivial, tests SHALL at minimum verify that `signInWith(Provider.GOOGLE)` initiates without error and the correct redirect URL is constructed.

#### Scenario: OAuth redirect URL is correct

- GIVEN the Supabase client is configured with the test project
- WHEN `signInWith(Provider.GOOGLE)` is called
- THEN the constructed redirect URL MUST contain the configured `SUPABASE_REDIRECT_SCHEME` and `SUPABASE_REDIRECT_HOST`

### Requirement: Sync Flow Tests

Tests MUST verify that locally created entities sync to Supabase. Tests SHALL create data in Room, trigger sync, and verify Supabase state via PostgREST queries.

#### Scenario: Patient syncs to Supabase

- GIVEN a patient is inserted into Room with `syncStatus = PENDING`
- WHEN the sync coordinator uploads pending patients
- THEN the patient MUST appear in the Supabase `pacientes` table within 30 seconds

#### Scenario: Evaluation syncs to Supabase

- GIVEN an evaluation is inserted into Room linked to a synced patient
- WHEN the sync coordinator uploads pending evaluations
- THEN the evaluation MUST appear in the Supabase `historial` table

#### Scenario: Network failure leaves data in Room

- GIVEN a patient is inserted into Room with `syncStatus = PENDING`
- WHEN the sync attempt fails due to network error
- THEN the patient MUST remain in Room with `syncStatus = PENDING` and MUST NOT be lost

### Requirement: Test Data Cleanup

Tests MUST clean up all test data after each run. Auth users SHALL be deleted via Supabase Admin API (service role key) or marked with a test prefix for bulk cleanup. Synced rows SHALL be deleted by matching test-specific identifiers.

#### Scenario: Test user cleaned up after run

- GIVEN a test created a new Supabase Auth user
- WHEN the test teardown runs
- THEN the user MUST be deleted from the test Supabase project

#### Scenario: Synced test data cleaned up

- GIVEN a test synced patients and evaluations to Supabase
- WHEN the test teardown runs
- THEN all rows with the test identifier prefix MUST be deleted from Supabase tables

### Requirement: Test Isolation

Tests MUST NOT run alongside unit tests in the same Gradle task. The `connectedAndroidTest` task SHALL execute separately from `testDebugUnitTest`. Tests SHOULD be annotated with a custom test suite marker for selective execution.

#### Scenario: Unit tests and instrumented tests are separate tasks

- GIVEN both `testDebugUnitTest` and `connectedAndroidTest` are available
- WHEN CI runs unit tests
- THEN `connectedAndroidTest` MUST NOT be triggered (and vice versa)
