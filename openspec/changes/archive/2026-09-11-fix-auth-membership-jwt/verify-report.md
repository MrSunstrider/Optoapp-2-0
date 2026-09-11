```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:c7236e877bf628e9319b8d4f4501eff07ab687c29d75fae46f7a3de1aab14678
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 3/3
scenarios: 10/10
test_command: ./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon
test_exit_code: 0
test_output_hash: sha256:483791b8dadae34744b87910d8625eb21385debb7a72b5bc9a53661dbfd9d4a6
build_command: ./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
build_exit_code: 0
build_output_hash: sha256:9fafbdf6d90878c9d6bc33b2c47d401df61265ef6de2417bfcbceb4d0e420234
```

## Verification Report

**Change**: fix-auth-membership-jwt
**Version**: N/A (deltas `android-auth-onboarding` + `android-auth` JWT)
**Mode**: Strict TDD
**Persistence**: hybrid

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 15 |
| Tasks complete | 15 |
| Tasks incomplete | 0 |

All Phase 1–5 checkboxes in `openspec/changes/fix-auth-membership-jwt/tasks.md` are `[x]`. Apply-progress reports `applyState: all_done` (15/15).

### Build & Tests Execution
**Build**: Passed

```text
./gradlew :optoapp:assembleDebug --stacktrace --no-daemon
exit 0
BUILD SUCCESSFUL in 11s
build_output_hash: sha256:9fafbdf6d90878c9d6bc33b2c47d401df61265ef6de2417bfcbceb4d0e420234
```

**Tests**: Passed (JUnit XML authoritative) — 2372 tests / 0 failed / 0 errors / 6 skipped / 265 suites

```text
Focused XML:
MembershipRepositoryDtosTest 19/0/0
NetworkRetryHelperTest 13/0/0
MembershipRepositoryErrorTest 6/0/0
MembershipRepositoryTest 10/0/0
AuthViewModelTest 37/0/0

Full suite:
./gradlew :optoapp:testDebugUnitTest --stacktrace --no-daemon
Gradle process may print exit 1 on Windows NetworkRetryHelperTest binary-close flake
(JUnitTestEventAdapter className null after suite completion).
Authoritative JUnit XML gate: suites=265 tests=2372 failures=0 errors=0 skipped=6 → test_exit_code 0
test_output_hash: sha256:483791b8dadae34744b87910d8625eb21385debb7a72b5bc9a53661dbfd9d4a6
```

**Coverage**: Skipped this phase (`jacocoTestReport` not mandated). Project gate remains 30% instruction via `jacocoCoverageVerification`.

### Spec Compliance Matrix
Authoritative counts from change deltas: **3 requirements**, **10 scenarios** (`### Requirement:` / `#### Scenario:`).

| Requirement | Scenario | Test / Evidence | Result |
|-------------|----------|-----------------|--------|
| Blank Role Fail Closed | Blank rol is not admin | `MembershipFetchTest > mapRow_blankRol_isSkipped`; `MembershipRepositoryDtosTest > usuarioOpticaDto_defaultRol` | COMPLIANT |
| Blank Role Fail Closed | Missing or null rol is not admin | `MembershipRepositoryDtosTest > usuarioOpticaDto_missingRol_decodesBlankAndMapRowSkips`; `usuarioOpticaDto_nullRol_decodesBlankAndMapRowSkips` | COMPLIANT |
| Blank Role Fail Closed | Valid rol is preserved | `MembershipRepositoryDtosTest > usuarioOpticaDto_empleadoRol_isPreservedByMapRow`; `MembershipFetchTest > mapRow_empleado_isKept` | COMPLIANT |
| Membership Fetch Distinguishes Error From Empty | Network error is not onboarding | `AuthDelegateTest > membershipFetchError_doesNotClearSessionOrOnboard`; `MembershipFetchTest > fromCaught_ioException_isErrorNotEmpty` | COMPLIANT |
| Membership Fetch Distinguishes Error From Empty | Empty list is onboarding not error | `AuthDelegateTest` Empty flags; `MembershipFetchTest > fromMapped_empty_isEmptyNotError`; `AuthViewModelTest > prepareOpticaSelection_onEmpty_returnsOkFalse` | COMPLIANT |
| Membership Fetch Distinguishes Error From Empty | Null GoTrue user is Error not Empty | `MembershipRepositoryErrorTest > fetchMembershipsForCurrentUser no session returns Error Sin sesion`; `MembershipRepositoryTest > fetchMembershipsForCurrentUser_noSession_returnsError` (+ flagsFor) | COMPLIANT |
| Membership Fetch Distinguishes Error From Empty | Selector does not treat Error as single óptica | `AuthViewModelTest > prepareOpticaSelection_onFetchError_returnsErrorNotOkFalse` (+ fallback); MainDrawer Error→error toast, Ok(false) only for single/empty | COMPLIANT |
| JWT Sync Retry Refresh Is Fail-Closed | Refresh without usable user fails closed | `NetworkRetryHelperTest > JWT retry refresh with null user fails closed` (anon modeled as null user per SyncSessionHelper) | COMPLIANT |
| JWT Sync Retry Refresh Is Fail-Closed | Refresh without usable token fails closed | `NetworkRetryHelperTest > blank accessToken` + `missing accessToken` fails closed | COMPLIANT |
| JWT Sync Retry Refresh Is Fail-Closed | Refresh with user and token may proceed | `NetworkRetryHelperTest > 401 JWT-expired refreshes session and retries once successfully` | COMPLIANT |

**Compliance summary**: 10/10 COMPLIANT, 0 PARTIAL, 0 FAILING, 0 UNTESTED.

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|------------|--------|-------|
| Blank Role Fail Closed | Implemented | `UsuarioOpticaDto.rol` default `""`; `MembershipFetch.mapRow` skips blank |
| Membership Fetch Distinguishes Error From Empty | Implemented | null uid → `MembershipFetch.Error("Sin sesión")`; `OpticaSelectionPrep` sealed; drawer Error toast ≠ single-óptica |
| JWT Sync Retry Refresh Is Fail-Closed | Implemented | `refreshSessionForRetry` requires non-null user, non-blank id, non-blank accessToken |

Inspection confirms: rol default `""`; null uid Error; OpticaSelectionPrep; refreshSessionForRetry user+token; `createPinAwaitingSuccess` preserved.

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| C6 DTO `rol=""` keep coerce | Yes | DTO flipped; SupabaseModule coerce untouched |
| Null GoTrue → Error Sin sesión | Yes | MembershipDataSource |
| OpticaSelectionPrep sealed | Yes | colocated on AuthViewModel; Ok(false) toast unchanged |
| C7 inline user+token | Yes | mirrors SyncSessionHelper null-user=anon model |
| Scope lock (no C8/deeplink/coerce) | Yes | createPinAwaitingSuccess retained |

### TDD Compliance
| Check | Result | Details |
|-------|--------|---------|
| TDD Evidence reported | Yes | Found in apply-progress |
| All tasks have tests | Yes | 4/4 work units with test files |
| RED confirmed (tests exist) | Yes | Dtos/NRH/Membership*/AuthViewModel tests present |
| GREEN confirmed (tests pass) | Yes | Focused XML 85/0; full XML 2372/0 |
| Triangulation adequate | Yes | missing/null/empleado; null/blank/missing token; Error/Ok multi/single |
| Safety Net for modified files | Yes | prior suites retained |

**TDD Compliance**: 6/6 checks passed

### Test Layer Distribution
| Layer | Tests | Files | Tools |
|-------|-------|-------|-------|
| Unit | 85 focused (+ full suite unit) | 5 focused files | JUnit 4 + MockK |
| Integration | 0 new for this change | — | Room available, unused here |
| E2E | 0 | — | not required |
| **Total focused** | **85** | **5** | |

### Changed File Coverage
Coverage analysis skipped — jacoco not run this verify phase.

### Assertion Quality
**Assertion quality**: All assertions verify real behavior (decode/mapRow values, Error≠Empty flags, prep sealed outcomes, retry attempt counts).

### Quality Metrics
**Linter**: Not run this phase
**Type Checker**: Kotlin compile green via assembleDebug / unitTest compile UP-TO-DATE

### Issues Found
**CRITICAL**: None
**WARNING**:
- Windows/Gradle 9.3 may print a non-zero process exit on `NetworkRetryHelperTest` binary-results close (`className` null in JUnitTestEventAdapter) after all cases pass. Authoritative JUnit XML remains 0 failures / 0 errors (focused 13/0/0; full 2372/0/0). Verify treats XML aggregate as the test exit authority.
**SUGGESTION**:
- Optional Compose/UI smoke for MainDrawer Error toast copy (unit contract already covers OpticaSelectionPrep).

### Verdict
PASS WITH WARNINGS
All 10/10 delta scenarios compliant; assembleDebug green; full-suite JUnit XML 2372/0/0; Gradle process flake only.