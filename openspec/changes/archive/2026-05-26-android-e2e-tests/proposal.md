# Proposal: android-e2e-tests

## Intent

Add comprehensive E2E testing coverage for OptoApp Android using Compose UI Tests and instrumented Supabase integration tests. Goal: verify the 4 P0 user flows (Login+PIN, Patient Creation, Evaluation, Dispensación) end-to-end without relying solely on unit tests.

## Scope

### In Scope
- Level 1 Compose UI Tests for 4 P0 flows (Login+PIN, Patient Creation, Evaluation, Dispensación)
- Level 2 Instrumented Tests for Auth + Sync flows (real Supabase)
- CI pipeline update to run `androidTest` on emulator
- Test data factories for reproducible test state

### Out of Scope
- Web E2E tests (future work)
- Performance/benchmark tests
- Visual regression tests
- Unit test additions (already covered by `test-coverage` spec)

## Capabilities

### New Capabilities
- `android-compose-e2e`: Compose UI tests for 4 core flows using `androidx.compose.ui.test.junit4`
- `android-instrumented-e2e`: Supabase-backed instrumented tests for Auth and Sync flows
- `android-ci-androidtest`: CI step executing `connectedAndroidTest` on emulator

### Modified Capabilities
- None — test infrastructure is additive

## Approach

**Phase 1 — Compose UI Tests (Level 1)**

1. Add `@TestTag` annotations to key composables (LoginScreen, NuevoPacienteScreen, NuevaEvaluacionScreen, NuevaDispensacionScreen)
2. Write Compose UI tests using `createAndroidComposeRule<MainActivity>()` for each P0 flow
3. Use inline seed data (hardcoded test patients/payments) — no Supabase calls
4. Pattern: semantic node queries (`onNodeWithText`, `onNodeWithTag`) over coordinates

**Phase 2 — Instrumented Supabase Tests (Level 2)**

1. Provision separate Supabase test project (free tier)
2. Create `SupabaseTestCredentials` via `local.properties` (gitignored)
3. Add `HiltAndroidRule` + `@UninstallModules(SupabaseModule::class)` for auth flow tests
4. Test data teardown via Supabase Admin API post-test

**Phase 3 — CI Pipeline**

1. Add `connectedAndroidTest` step to `.github/workflows/android-ci.yml`
2. Use GitHub-hosted Android emulator (API 34)
3. Run only on pushes to `main`/`version-saas` — skip PRs (emulator is slow)

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Test data strategy | Inline factories | No seed scripts, no external DB; avoids Supabase dependency |
| Supabase test project | Separate free-tier project | Isolates test data from dev/prod RLS |
| Hilt in tests | `@UninstallModules` approach | Existing androidTest doesn't use Hilt; gradual migration |
| Compose test pattern | Semantic nodes + test tags | More robust than text matching; survives UI copy changes |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/src/androidTest/` | New | 6–8 test files: 4 UI flows + 2 Supabase flows |
| `optoapp/build.gradle.kts` | Modified | Add `testTag` resource, possible IdlingResource dep |
| `optoapp/src/main/java/com/example/optoapp/ui/**/` | Modified | Add `@TestTag` strings to key composables |
| `.github/workflows/android-ci.yml` | Modified | Add emulator + `connectedAndroidTest` step |
| `local.properties` | Modified | Add `SUPABASE_TEST_URL`, `SUPABASE_TEST_ANON_KEY` (gitignored) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| No Hilt test infrastructure yet | High | Phase 1 uses direct instantiation; Hilt added incrementally |
| CI doesn't have emulator configured | High | Use GitHub-hosted `runs-on: ubuntu-latest` + `setup-android` action |
| Supabase test credentials leak | Medium | Store in GitHub Secrets; never commit `local.properties` |
| Flaky tests from async operations | Medium | Use `waitUntil` and `runOnUiThread` in Compose tests; generous timeouts |
| Test data pollution | Low | Supabase test project has no production data; teardown after each test |

## Rollback Plan

All changes are **additive** — no production code is modified. If tests break CI:
1. Temporarily disable the `connectedAndroidTest` step in `android-ci.yml`
2. Tests in `androidTest/` remain on disk for local execution
3. No rollback of production code needed

## Dependencies

- Supabase test project provisioned and credentials available
- GitHub Secrets configured for `SUPABASE_TEST_ANON_KEY`
- Android emulator available in CI (API 34)

## Success Criteria

- [ ] 4 Compose UI tests pass (`LoginFlowTest`, `PatientCreationFlowTest`, `EvaluationFlowTest`, `DispensacionFlowTest`)
- [ ] 2 Supabase instrumented tests pass (`AuthSyncTest`, `OfflineSyncTest`)
- [ ] `connectedAndroidTest` step runs in CI on `main` branch
- [ ] All tests execute in < 15 minutes locally (emulator)
- [ ] Zero new production code changes beyond test files and `@TestTag` annotations
