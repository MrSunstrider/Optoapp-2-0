# CI Pipeline for androidTest Specification

## Purpose

Define the CI pipeline configuration for executing `connectedAndroidTest` on an Android emulator. Covers when tests run, emulator setup, credential management, and failure handling.

## Requirements

### Requirement: Unit Tests on Every Push/PR

CI MUST run `testDebugUnitTest` on every push and every pull request targeting `main` or `version-saas`. This is the existing behavior and MUST NOT regress.

#### Scenario: Push to main triggers unit tests

- GIVEN a developer pushes a commit to `main`
- WHEN the CI workflow starts
- THEN `testDebugUnitTest` MUST execute and pass before merge

#### Scenario: PR to version-saas triggers unit tests

- GIVEN a PR targets `version-saas`
- WHEN the CI workflow starts
- THEN `testDebugUnitTest` MUST execute and report status on the PR

### Requirement: androidTest on Main/Version-Saas Only

CI SHOULD run `connectedAndroidTest` only on pushes to `main` or `version-saas`. CI SHALL NOT run `connectedAndroidTest` on pull requests to avoid slow feedback loops from emulator setup.

#### Scenario: Push to main triggers androidTest

- GIVEN a developer pushes to `main`
- WHEN the CI workflow reaches the androidTest job
- THEN `connectedAndroidTest` MUST execute on an Android emulator

#### Scenario: PR does NOT trigger androidTest

- GIVEN a PR targets `main`
- WHEN the CI workflow runs
- THEN the `connectedAndroidTest` step MUST be skipped (not queued, not failed — skipped)

### Requirement: Android Emulator Configuration

CI MUST configure a GitHub-hosted Android emulator for `connectedAndroidTest`. The emulator SHALL target API 34 (Android 14). CI SHALL use the `reactivecircus/android-emulator-runner` action or equivalent for reliable emulator lifecycle management.

| Config | Value |
|--------|-------|
| Runner | `ubuntu-latest` |
| API level | 34 |
| Target | `google_apis` |
| Architecture | `x86_64` |
| Timeout | 20 minutes max |
| Headless | Yes (`-no-window`) |

#### Scenario: Emulator boots successfully

- GIVEN the CI job starts on `ubuntu-latest`
- WHEN the emulator setup action runs with API 34
- THEN the emulator MUST reach `booted` state within 10 minutes

#### Scenario: Emulator setup timeout

- GIVEN the emulator has not booted within 15 minutes
- WHEN the timeout is reached
- THEN the CI job MUST fail with a clear error message indicating emulator timeout

### Requirement: Supabase Test Credentials in CI

CI SHALL store Supabase test credentials (`SUPABASE_TEST_URL`, `SUPABASE_TEST_ANON_KEY`) as GitHub Secrets. The `connectedAndroidTest` step MUST inject these as environment variables or `BuildConfig` fields. Credentials MUST NOT be logged or printed in CI output.

| Secret | Purpose |
|--------|---------|
| `SUPABASE_TEST_URL` | Test Supabase project URL |
| `SUPABASE_TEST_ANON_KEY` | Test Supabase anonymous key |
| `SUPABASE_TEST_SERVICE_KEY` | (Optional) For test data cleanup |

#### Scenario: Credentials available to androidTest

- GIVEN GitHub Secrets are configured with test Supabase credentials
- WHEN the `connectedAndroidTest` step runs
- THEN the test APK MUST be able to read `BuildConfig.SUPABASE_TEST_URL` and `BuildConfig.SUPABASE_TEST_ANON_KEY`

#### Scenario: Credentials not exposed in logs

- GIVEN the androidTest step is running
- WHEN any CI step outputs environment variables
- THEN Supabase test keys MUST NOT appear in plain text in CI logs

### Requirement: Test Failure Handling

If `connectedAndroidTest` fails, CI MUST report the failure as a check status on the commit. CI MAY continue with `assembleDebug` even if androidTest fails (non-blocking initially). CI SHALL collect emulator logs and test reports as artifacts for debugging.

#### Scenario: androidTest failure reported

- GIVEN a test fails during `connectedAndroidTest`
- WHEN the CI step completes
- THEN the failure MUST be reported as a failed check on the commit
- AND test reports MUST be uploaded as GitHub Actions artifacts

#### Scenario: androidTest artifacts available for debugging

- GIVEN `connectedAndroidTest` completed (pass or fail)
- WHEN a developer inspects the CI run
- THEN `build/outputs/androidTest-results/` and emulator logcat MUST be downloadable as artifacts

### Requirement: Skip on Emulator Failure

CI MAY skip `connectedAndroidTest` if emulator setup fails, to avoid blocking the pipeline. This SHALL only apply to the androidTest job — unit tests and build MUST still run regardless.

#### Scenario: Emulator failure does not block build

- GIVEN the emulator fails to boot within timeout
- WHEN the androidTest job reports failure
- THEN the build job (`assembleDebug`) MUST still run and report its own status independently

### Requirement: Execution Time Budget

The `connectedAndroidTest` step SHOULD complete within 15 minutes. Tests that consistently exceed this budget SHOULD be flagged for optimization or moved to a nightly schedule.

#### Scenario: androidTest within time budget

- GIVEN the androidTest job starts
- WHEN all tests execute on the emulator
- THEN the total job time (excluding emulator boot) SHOULD NOT exceed 15 minutes

#### Scenario: Exceeding budget flags for review

- GIVEN `connectedAndroidTest` takes longer than 15 minutes on 3 consecutive runs
- WHEN the CI workflow completes
- THEN a warning SHOULD be logged (or a GitHub issue created) for test optimization review
