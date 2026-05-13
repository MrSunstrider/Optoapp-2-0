# Tasks: Post-Review Cleanup

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1800–2200 (1400+ code + 400+ tests) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | W1→W2→S1→S2→W3→S3 (stacked-to-main) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Lines Est. | Notes |
|------|------|-----------|------------|-------|
| 1 | Exception handling (W1) — 7 files | PR 1 | ~150 | Independent; no deps |
| 2 | File extraction W2a — EvaluacionViewModel + FormSections | PR 2 | ~400 | 2 files → 7 files; tests included |
| 3 | File extraction W2b — MainDrawer + NuevaDispensacion | PR 3 | ~350 | 2 files → 4 files |
| 4 | File extraction W2c — RecetaPdfBuilder | PR 4 | ~200 | 1 file → 2 files |
| 5 | ViewModel tests S1 — Settings + Subscription | PR 5 | ~300 | Independent; after extractions for imports |
| 6 | Sync scheduler tests S2 | PR 6 | ~250 | Uses MockK; isolated |
| 7 | Compose BOM W3 + Resource audit S3 | PR 7 | ~100 | BOM single line + docs |

---

## Phase 1: Exception Handling (W1)

All 7 files have independent catch refactors—no cross-dependencies.

- [x] 1.1 `AuthDelegate.kt` — Lines 143, 238: changed to CancellationException rethrow + IOException + Exception fallback, Log.e
- [x] 1.2 `BackupDelegate.kt` — Line 87: changed to CancellationException rethrow + IOException + Exception fallback
- [x] 1.3 `ServiciosViewModel.kt` — Line 208: changed to CancellationException rethrow + IOException + Exception fallback
- [x] 1.4 `MonturasViewModel.kt` — Line 166: changed to CancellationException rethrow + IOException + Exception fallback
- [x] 1.5 `PacienteViewModel.kt` — Line 141: changed to CancellationException rethrow + IOException + Exception fallback
- [x] 1.6 `FiscalConfigViewModel.kt` — Line 244: changed to CancellationException rethrow + IOException + Exception fallback
- [x] 1.7 `EvaluacionViewModel.kt` — Line 496: extracted to companion object, `NumberFormatException` + CancellationException rethrow
- [x] 1.8 Verify: `grep -r "catch (e: Exception)" viewmodel/` — 0 primary generic catches; 3 fallback-only (matching AuthViewModel/SyncViewModel pattern)
- [x] 1.9 Run `./gradlew testDebugUnitTest` — all 33 tests pass (BUILD SUCCESSFUL)

---

## Phase 2: Large File Extraction — Evaluacion (W2a)

Extract EvaluacionViewModel and EvaluacionFormSections first (highest line counts).

- [ ] 2.1 Create `EvaluacionDipHelper.kt` — extract `parseDipOrDnp()`, `formatDipForUi()` from EvaluacionViewModel
- [ ] 2.2 Create `EvaluacionDiagnosticoHelper.kt` — extract `parseRefraction()`, `calcularDiagnostico()`, `parseSnellenToLogMar()`, `normalizeAndTranspose()`
- [ ] 2.3 Update `EvaluacionViewModel.kt` — import helpers, delegate calls, verify <300 lines
- [ ] 2.4 Create `AnamnesisSection.kt` — extract Anamnesis composable from EvaluacionFormSections
- [ ] 2.5 Create `AgudezaVisualSection.kt` — extract AV composable
- [ ] 2.6 Create `RecetaOptometriaSection.kt` — extract receta composable
- [ ] 2.7 Create `EvaluacionOtrosSection.kt` — extract remaining composables
- [ ] 2.8 Update `EvaluacionFormSections.kt` — import sections, delegate, verify <300 lines
- [ ] 2.9 Run `./gradlew assembleDebug` — compilation succeeds
- [ ] 2.10 Run `./gradlew testDebugUnitTest` — all tests pass

---

## Phase 3: Large File Extraction — Drawer & Dispensacion (W2b)

Extract MainDrawerScreen and NuevaDispensacionScreen.

- [ ] 3.1 Create `DrawerSections.kt` — extract drawer composables from MainDrawerScreen
- [ ] 3.2 Update `MainDrawerScreen.kt` — import sections, delegate, verify <300 lines
- [ ] 3.3 Create `DispensacionFormSections.kt` — extract form composables from NuevaDispensacionScreen
- [ ] 3.4 Update `NuevaDispensacionScreen.kt` — import sections, delegate, verify <300 lines
- [ ] 3.5 Run `./gradlew assembleDebug` — compilation succeeds
- [ ] 3.6 Run `./gradlew testDebugUnitTest` — all tests pass

---

## Phase 4: Large File Extraction — PDF Builder (W2c)

Extract RecetaPdfBuilder last (no UI dependencies).

- [ ] 4.1 Create `RecetaPdfSections.kt` — extract PDF page builders from RecetaPdfBuilder
- [ ] 4.2 Update `RecetaPdfBuilder.kt` — import sections, delegate, verify <300 lines
- [ ] 4.3 Run `./gradlew assembleDebug` — compilation succeeds
- [ ] 4.4 Run `./gradlew testDebugUnitTest` — all tests pass

---

## Phase 5: ViewModel Test Coverage (S1)

Add characterization tests following DispensacionViewModel test pattern.

- [ ] 5.1 Create `SettingsViewModelTest.kt` with 5 tests:
  - `initialState_shouldBeLoading()`
  - `loadSettings_success_shouldPopulateState()`
  - `loadSettings_failure_shouldSetErrorState()`
  - `setRemindersEnabled_shouldUpdateOptimistically()`
  - `setUserTimeZone_shouldPersistAndUpdate()`
- [ ] 5.2 Create `SubscriptionViewModelTest.kt` with 5 tests:
  - `initialState_shouldBeLoading()`
  - `refreshPlan_active_shouldIndicateActive()`
  - `refreshPlan_expired_shouldIndicateExpired()`
  - `refreshPlan_error_shouldSetErrorState()`
  - `purchaseAction_shouldTransitionState()`
- [ ] 5.3 Run tests: `./gradlew testDebugUnitTest --tests "*ViewModelTest"`

---

## Phase 6: Sync Scheduler Tests (S2)

Add MockK-based tests for PostSaveSyncScheduler.

- [ ] 6.1 Create `PostSaveSyncSchedulerTest.kt` with test setup (mocks for CoroutineScope, SyncGate, use cases)
- [ ] 6.2 Test: `schedulePacientesSync_shouldEnqueueJob()`
- [ ] 6.3 Test: `transientFailure_shouldRetryWithBackoff()`
- [ ] 6.4 Test: `maxRetriesExceeded_shouldMarkPermanentFailure()`
- [ ] 6.5 Test: `duplicateSaves_shouldDedupSyncJobs()`
- [ ] 6.6 Test: `syncError_shouldPropagateViaCallback()`
- [ ] 6.7 Run tests: `./gradlew testDebugUnitTest --tests "PostSaveSyncSchedulerTest"`

---

## Phase 7: Compose BOM Update (W3)

Single-line change with verification.

- [ ] 7.1 Update `gradle/libs.versions.toml` — change `composeBom = "2024.02.02"` to `"2024.09.00"`
- [ ] 7.2 Verify no hardcoded Compose versions: `grep -r "androidx.compose" gradle/ --include="*.toml" --include="*.kts"`
- [ ] 7.3 Run `./gradlew assembleDebug` — compilation succeeds
- [ ] 7.4 Run `./gradlew testDebugUnitTest` — all tests pass

---

## Phase 8: Resource Audit Documentation (S3)

Audit-only deliverable—no code changes.

- [ ] 8.1 Search codebase: `grep -r "Resource<" --include="*.kt" | wc -l` and document 64 usages
- [ ] 8.2 Categorize usages: Loading+Error state, Error-only, Error+Data wrapper
- [ ] 8.3 Create `openspec/changes/post-review-cleanup/specs/result-type-unification/Resource-audit.md` with:
  - Usage inventory (files + counts)
  - Comparison table (Resource<T> vs kotlin.Result)
  - Decision: Keep Resource<T> (rationale: Loading state, typed errors)
  - Standard usage pattern documentation

---

## Verification Checklist

- [ ] All 7 exception handling files refactored, no generic catches remain
- [ ] All 5 large files <300 lines, extracted files follow naming convention
- [ ] BOM updated, compilation succeeds, no new deprecation warnings
- [ ] 10 ViewModel tests added (Settings + Subscription)
- [ ] 5 PostSaveSyncScheduler tests added
- [ ] Resource audit document created with decision
- [ ] `./gradlew testDebugUnitTest` passes (regression check)
- [ ] `./gradlew assembleDebug` passes (build check)
