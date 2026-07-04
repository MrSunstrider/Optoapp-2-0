# Tasks: Login Row Layout

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~30 lines |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: stacked-to-main
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Update LoginFlowTest.kt to verify merged row layout | PR 1 | Align new row assertions with existing test pattern |

## Phase 1: Test Analysis

- [ ] 1.1 Review existing login flow tests and understand the current layout expectations
- [ ] 1.2 Identify which tests need to be updated for the merged row layout
- [ ] 1.3 Create test assertions for:
      - Both elements display on same vertical level
      - Recordar Cuenta is left-aligned
      - ¿Olvidaste? is right-aligned using weight(1f) spacer

## Phase 2: Test Updates

- [ ] 2.1 Update `LoginFlowTest.kt`: modify rememberAccountCheckbox test to verify it renders within merged row structure with right padding to accommodate weight spacer
- [ ] 2.2 Update `LoginFlowTest.kt`: modify olvidasteButton test to assert it's on same row with rememberAccount, positioned at right edge
- [ ] 2.3 Verify that existing tests still pass with the merged row layout

## Phase 3: Verification

- [ ] 3.1 Run unit tests: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [ ] 3.2 Run android tests to verify UI assertions
- [ ] 3.3 Build APK: `./gradlew :optoapp:assembleDebug`

## Phase 4: Documentation

- [ ] 4.1 Update test documentation if any
- [ ] 4.2 Ensure tests clearly document the new row layout expectations
