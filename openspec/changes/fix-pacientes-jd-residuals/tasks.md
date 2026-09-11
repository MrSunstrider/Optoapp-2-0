# Tasks: fix-pacientes-jd-residuals

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~80–150 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Fail-closed pacientes download guards | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.domain.SyncPacientesUseCaseDownloadGuardTest` | N/A — unit tests only | `SyncPacientesUseCase.kt`, guard tests, `FakeConflictDao.kt` |
| 2 | Fecha save gate + strict parse | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.util.DateUtilsTest,com.example.optoapp.ui.components.paciente.PacienteFormSectionsTest` | N/A — unit tests only | `DateUtils.kt`, `PacienteFormSections.kt`, `NuevoPacienteScreen.kt` |
| 3 | UTC daily delete key | PR 1 | `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.util.DateUtilsTest,com.example.optoapp.viewmodel.PacienteViewModelTest` | N/A — unit tests only | `SessionManager.kt`, UTC test fixtures |
| 4 | Full verify | PR 1 | `./gradlew :optoapp:testDebugUnitTest --stacktrace` | N/A — CI-equivalent suite | N/A |

## Phase 1: SyncPacientes Fail-Closed (Unit 1)

- [x] 1.1 Add `throwOnGetConflictEntityIds` hook to `optoapp/src/test/java/com/example/optoapp/test/FakeConflictDao.kt`
- [x] 1.2 RED: In `SyncPacientesUseCaseDownloadGuardTest.kt`, `getConflictEntityIds` throws → 0 upserts, no `upsertPaciente`, `markError(download_pacientes, batch)`
- [x] 1.3 GREEN: In `domain/SyncPacientesUseCase.kt`, extract `buildPacienteSkipIds()` (one retry per query); null → return 0 before `fetchRemotePacientesForDownload`
- [x] 1.4 RED: Same test class — Phase 2 `syncStateDao.getPendingDeletions` throws → same fail-closed contract (zero upserts, `markError`, no fetch)
- [x] 1.5 GREEN: Wire pending-deletions failure into `buildPacienteSkipIds()`; call `markError` on guard failure
- [x] 1.6 RED: Guard query throws `CancellationException` → propagates; no `markError`, no fail-closed swallow
- [x] 1.7 GREEN: Re-throw `CancellationException` in guard paths; confirm happy-path conflict-skip tests still pass

## Phase 2: Fecha Nacimiento Save Gate (Unit 2)

- [x] 2.1 RED: In `DateUtilsTest.kt`, `parseBirthDateFromDigits("31022020")` → null; `"15061990"` → 1990-06-15; blank → null
- [x] 2.2 GREEN: Add `parseBirthDateFromDigits(digits): LocalDate?` and `utcToday(): LocalDate` to `util/DateUtils.kt` (strict `LocalDate.of`, no lenient adjust)
- [x] 2.3 Refactor `validateFechaNacimiento` in `ui/components/paciente/PacienteFormSections.kt` to delegate to `parseBirthDateFromDigits`; run `PacienteFormSectionsTest` — must stay green
- [x] 2.4 RED: Add test — non-blank fecha with validation error blocks save (`savePaciente` not invoked); prefer pure predicate over Compose if possible
- [x] 2.5 GREEN: In `ui/screens/NuevoPacienteScreen.kt`, gate `saveAction`: toast + return when `fechaNacimiento.isNotBlank() && validateFechaNacimiento(...) != null`; persist via `parseBirthDateFromDigits`

## Phase 3: UTC Daily Delete Key (Unit 3)

- [x] 3.1 RED: In `DateUtilsTest.kt`, assert `utcToday()` uses UTC calendar day when local date differs (fixed instant or zone override)
- [x] 3.2 GREEN: Change `SessionManager.dailyPacienteDeleteKey` in `data/SessionManager.kt` to `DateUtils.utcToday()` formatted `BASIC_ISO_DATE`
- [x] 3.3 Update `PacienteViewModelTest.kt` delete-quota fixtures to expect UTC-keyed preference key when local ≠ UTC

## Phase 4: Verification (Unit 4)

- [x] 4.1 Run `./gradlew :optoapp:testDebugUnitTest --max-workers=1` — full suite BUILD SUCCESSFUL
- [x] 4.2 Spec scenarios covered; Fresh JD Judges A+B findings=[] → APPROVED
