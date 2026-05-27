# Tasks: Android E2E Tests

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1200–1500 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Infra) → PR 2 (Compose UI) → PR 3 (Supabase) → PR 4 (CI) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Test infrastructure + production `@TestTag` annotations | PR 1 | Targets `main`; no tests yet |
| 2 | Compose UI flow tests for 4 P0 screens | PR 2 | Targets `main`; needs PR 1 merged first |
| 3 | Supabase instrumented auth & sync tests | PR 3 | Targets `main`; needs PR 1 merged first |
| 4 | CI pipeline with emulator and credentials | PR 4 | Targets `main`; needs PR 2 & 3 merged first |

## Phase 1: Test Infrastructure

- [x] 1.1 Create `optoapp/src/main/java/com/example/optoapp/testing/TestTags.kt` with constants for Login, PIN, Paciente, Evaluación, Dispensación, and Navigation screens
- [x] 1.2 Add `@TestTag` annotations to production composables: `LoginScreen`, `NuevoPacienteScreen`, `NuevaEvaluacionScreen`, `NuevaDispensacionScreen`, and navigation bars
- [x] 1.3 Create `optoapp/src/androidTest/java/com/example/optoapp/factories/TestDataFactory.kt` with inline factories for `Paciente`, `EvaluacionClinica`, `DispensacionOptica`
- [x] 1.4 Create `optoapp/src/androidTest/java/com/example/optoapp/rules/TestDatabaseRule.kt` — Room in-memory DB with `allowMainThreadQueries()`, auto-create in `@Before`, close in `@After`
- [x] 1.5 Create fakes in `optoapp/src/androidTest/java/com/example/optoapp/fakes/`: `FakePacienteRepository`, `FakeEvaluacionRepository`, `FakeDispensacionRepository` with in-memory storage for P0 flows
- [x] 1.6 Update `optoapp/build.gradle.kts` — add `espresso-idling-resource`, `test-rules`, and `hilt-android-testing` dependencies

## Phase 2: Compose UI Flow Tests

- [x] 2.1 Write `optoapp/src/androidTest/java/com/example/optoapp/ui/LoginFlowTest.kt` — valid login→PIN navigation, wrong password error, empty fields validation using `createAndroidComposeRule<MainActivity>()`
- [x] 2.2 Write `optoapp/src/androidTest/java/com/example/optoapp/ui/PacienteFlowTest.kt` — create patient with all fields, required field validation, and list display
- [x] 2.3 Write `optoapp/src/androidTest/java/com/example/optoapp/ui/EvaluacionFlowTest.kt` — complete evaluation with auto-diagnosis, partial evaluation blocks save
- [x] 2.4 Write `optoapp/src/androidTest/java/com/example/optoapp/ui/DispensacionFlowTest.kt` — add items, configure OT/lens/mounture, enter payment, save; empty items blocks save
- [x] 2.5 Write `optoapp/src/androidTest/java/com/example/optoapp/ui/NavigationTest.kt` — bottom nav switches sections, drawer menu opens and navigates

## Phase 3: Supabase Instrumented Tests

- [x] 3.1 Write `optoapp/src/androidTest/java/com/example/optoapp/data/SupabaseAuthTest.kt` — register unique user, login existing/wrong password, session persists; uses `BuildConfig` credentials and `@After` cleanup via Admin API
- [x] 3.2 Write `optoapp/src/androidTest/java/com/example/optoapp/data/SyncFlowTest.kt` — insert patient/evaluation into Room with `syncStatus=PENDING`, trigger sync coordinator, verify rows appear in Supabase `pacientes`/`historial` within 30s
- [x] 3.3 Write `optoapp/src/androidTest/java/com/example/optoapp/data/OfflineSyncTest.kt` — network failure keeps data in Room with `PENDING` status; queue resumes and syncs on reconnect

## Phase 4: CI Pipeline

- [x] 4.1 Update `.github/workflows/android-ci.yml` — add `android-test` job running on `ubuntu-latest` with `reactivecircus/android-emulator-runner` (API 34, google_apis, x86_64, pixel_6), inject `SUPABASE_TEST_*` secrets into `local.properties`, upload `androidTest-results/` and emulator logs as artifacts, skip on PRs
