# Tasks: fix-pacientes-jd-suspects

- [x] 1.1 Extract pure `pacienteDeleteQuotaKey(opticaId, email, utcDay)` + unit tests (two emails → distinct keys)
- [x] 1.2 Wire SessionManager.dailyPacienteDeleteKey to use email + utcToday
- [x] 2.1 RED: upload with all null updatedAt + batchFetchFailed aborts (markError Double fetch failure)
- [x] 2.2 GREEN: remove hasCheckable gate in SyncPacientesUseCase upload
- [x] 2.3 Confirm existing double-fetch test with non-null updatedAt still passes
- [x] 3.1 Focused tests green (30/30); Judgment Day next
